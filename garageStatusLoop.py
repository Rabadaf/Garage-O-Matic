from datetime import datetime
from datetime import timedelta
import requests
from requests.packages.urllib3.util.retry import Retry
from requests.adapters import HTTPAdapter
import os
import logging
import json
import RPi.GPIO as GPIO
import sys
import time

#TODO: Add status info to display like "Open since xx:xx" or "last closed xx:xx" or "Open for xx minutes"

RELAY = 17
DOOR = 8

API_KEY = 'AIzaSyDzvp67Aa5CY5MU5j9FdFhZnH77Lyk81co'
regIDs = {}

doorStatus = "closed"
openTime = datetime(2000, 1, 1)
closedTime = datetime(2000, 1, 1)
# Set the time the door must be open to send a notification
notifyDuration = timedelta(minutes=30, seconds=5)
tooLongMessageSent = False
# Set the time (date doesn't matter) after which it should close automatically
autoCloseTime = datetime(2000, 1, 1, 22, 0, 0)
# Set how long the door must have been open before it closes automatically
autoCloseDuration = timedelta(minutes=60, seconds=10)
autoCloseMessageSent = False

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger('Garage-O-Matic')
# logger.addHandler(logging.StreamHandler())
handler = logging.FileHandler(os.path.join(os.path.dirname(os.path.realpath(__file__)), "GarageOMatic.log"))
formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
handler.setFormatter(formatter)
logger.addHandler(handler)


def setup():
    global regIDs
    # Setup the GPIO pins
    GPIO.setmode(GPIO.BCM)
    GPIO.setup(RELAY, GPIO.OUT, initial=GPIO.HIGH)
    GPIO.setup(DOOR, GPIO.IN, pull_up_down=GPIO.PUD_UP)
    # Read in the GCM registered ids
    regIDs = readJsonIDs()
    try:
        loop()
    finally:
        destroy()


def loop():
    global doorStatus
    global openTime
    global closedTime
    global tooLongMessageSent
    global autoCloseMessageSent

    while True:
        # Keep track of the previous status, to tell if it changes
        oldDoorStatus = doorStatus
        doorStatus = getDoorStatus()
        # logger.debug("oldDoorStatus: " + oldDoorStatus)
        # logger.debug("DoorStatus: " + doorStatus)
        nowTime = datetime.now()

        # Send a GCM message if the door status changes
        if oldDoorStatus == "closed" and doorStatus == "open":
            openTime = nowTime
            closedTime = None
            logger.info("Door opened")
            sendMessage("door_status_changed", openTime)
        if doorStatus == "closed" and oldDoorStatus == "open":
            closedTime = nowTime
            openTime = None
            tooLongMessageSent = False
            autoCloseMessageSent = False
            logger.info("Door closed")
            sendMessage("door_status_changed", closedTime)

        # If the door is open, check to see how long it has been so
        if doorStatus == "open":
            openDuration = nowTime - openTime
            if openDuration > notifyDuration and not tooLongMessageSent:
                logger.warning("Door open too long, open for: {}".format(openDuration))
                sendMessage("door_open_too_long", nowTime, openTime=openDuration)
                tooLongMessageSent = True
            # TODO: fix when door open too long after midnight
            if openDuration > autoCloseDuration and nowTime.time() > autoCloseTime.time() and not autoCloseMessageSent:
                logger.warning("Door closed automatically")
                sendMessage("door_closed_automatically", nowTime)
                autoCloseMessageSent = True
                # TODO: put into own function
                GPIO.output(RELAY, GPIO.LOW)
                time.sleep(.1)
                GPIO.output(RELAY, GPIO.HIGH)

        time.sleep(0.1)


def sendMessage(messageType, messageTime, openTime=None):
    global doorStatus
    global regIDs

    gcmSendURL = 'https://gcm-http.googleapis.com/gcm/send'
    gcmHeaders = {'Authorization': 'key=' + API_KEY, 'Content-Type': 'application/json'}
    if messageType == "door_status_changed":
        gcmDataPayload = {'type': messageType, 'doorStatus': doorStatus, 'time': messageTime.strftime('%c')}
    elif messageType == "door_open_too_long":
        openTimeString = str(openTime).split(':')[-3] + " hours " + str(openTime).split(':')[-2] + " minutes " + str(openTime).split(':')[-1].split('.')[0] + " seconds"
        gcmDataPayload = {'type': messageType, 'doorStatus': doorStatus, 'time': messageTime.strftime('%c'), 'openTime': openTimeString}
    elif messageType == "door_closed_automatically":
        gcmDataPayload = {'type': messageType, 'doorStatus': doorStatus, 'time': messageTime.strftime('%c')}
    else:
        logger.error("Incorrect message type: {}".format(messageType))
    # gcmNotificationPayload = {'title': 'Garage Door Has Opened', 'body': 'Garage door opened at {}'.format(messageTime.strftime('%c')), 'icon': 'ic_door_open_notification2'}
    regIDsOnly = [item[0] for item in regIDs]
    logger.debug("Reg IDs: {}".format(regIDs))
    logger.debug("Reg IDs only: {}".format(regIDsOnly))
    gcmPayload = {'registration_ids': regIDsOnly, 'time_to_live': 86400, 'data': gcmDataPayload}
    # gcmPayload = {'to': regIDs[0].strip(), 'priority': 'high', 'delay_while_idle': False, 'time_to_live': 86400, 'notification': gcmNotificationPayload}
    # gcmPayload = {'to': regIDs[0].strip(), 'priority': 'high', 'delay_while_idle': False, 'time_to_live': 86400, 'content_available': True, 'data': gcmDataPayload, 'notification': gcmNotificationPayload}
    logger.debug("Payload: {}".format(gcmPayload))

    response = requests.Response()
    with requests.Session() as s:
        retries = Retry(total=5, backoff_factor=1)
        s.mount('http://', HTTPAdapter(max_retries=retries))
        try:
            response = s.post(gcmSendURL, headers=gcmHeaders, json=gcmPayload, timeout=10)
            response.raise_for_status()
            logger.debug('GCM response: ' + response.text)
        except (requests.exceptions.ConnectionError, requests.exceptions.HTTPError, requests.exceptions.Timeout, requests.exceptions.TooManyRedirects) as e:
            logger.error('Error sending GCM message')
            logger.error(e)

    responseDict = response.json()
    if responseDict['success'] or responseDict['canonical_ids']:
        for i, result in enumerate(responseDict['results']):
            if 'registration_id' in result:
                logger.info('Replacing reg_id {0} with {1}'.format(regIDs[i], result['registration_id']))
                userID = regIDs[i][1]
                regIDs.pop(i)
                regIDs.append((result['registration_id'], userID))
                writeJsonIDs(regIDs)
            if 'error' not in result:
                logger.debug('Successfully sent notification for reg_id {0}'.format(regIDs[i]))

    # Handling errors
    if responseDict['failure']:
        # Check each regID to see if it is valid or not
        regIDs[:] = [regID for regID in regIDs if regIDValid(regID, responseDict)]
        # Write just the valid IDs back to the file
        writeJsonIDs(regIDs)


def regIDValid(regID, responseDict):
    index = regIDs.index(regID)
    result = responseDict['results'][index]
    if 'error' in result and result['error'] in ['NotRegistered', 'InvalidRegistration']:
        # regID not valid
        logger.info("Removing reg_id: {0} from db".format(regID))
        return False
    # regID didn't have an error (or had some other error besides registration error), must be valid
    return True


def readJsonIDs():
    cwd = os.path.dirname(os.path.realpath(__file__))
    regIDFilePath = os.path.join(cwd, 'registrationIDs.txt')
    try:
        with open(regIDFilePath, 'r') as f:
            try:
                regIDsJson = json.load(f)
                logger.debug("Read registration IDs from file: " + str(regIDsJson))
            except ValueError:
                regIDsJson = {'IDs': []}
    except IOError:
        regIDsJson = {'IDs': []}
    return regIDsJson['IDs']


def writeJsonIDs(regIDs):
    cwd = os.path.dirname(os.path.realpath(__file__))
    regIDFilePath = os.path.join(cwd, 'registrationIDs.txt')
    with open(regIDFilePath, 'w') as f:
        regIDsJson = {'IDs': regIDs}
        json.dump(regIDsJson, f)
        logger.debug('Wrote registration IDs to file: ' + str(regIDsJson))


def getDoorStatus():
    doorOpen = GPIO.input(DOOR)
    if doorOpen:
        return "closed"
    else:
        return "open"


def destroy():
    logger.info('Exiting')
    GPIO.output(RELAY, GPIO.HIGH)
    sys.exit(1)


if __name__ == "__main__":
    setup()
