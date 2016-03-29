import webiopi
from datetime import datetime
from datetime import timedelta
import requests
from requests.packages.urllib3.util.retry import Retry
from requests.adapters import HTTPAdapter
import os
from gcm import GCM
import logging
import json


GPIO = webiopi.GPIO
RELAY = 17
DOOR = 8

API_KEY = 'AIzaSyDzvp67Aa5CY5MU5j9FdFhZnH77Lyk81co'
gcm = GCM(API_KEY, debug=False)
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
    GPIO.setFunction(RELAY, GPIO.OUT)
    GPIO.digitalWrite(RELAY, GPIO.HIGH)
    GPIO.setFunction(DOOR, GPIO.IN, GPIO.PUD_UP)

    regIDs = readJsonIDs()


def loop():
    global doorStatus
    global openTime
    global closedTime
    global tooLongMessageSent
    global autoCloseMessageSent

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
        if openDuration > autoCloseDuration and nowTime.time() > autoCloseTime.time() and not autoCloseMessageSent:
            logger.warning("Door closed automatically")
            sendMessage("door_closed_automatically", nowTime)
            autoCloseMessageSent = True
            GPIO.digitalWrite(RELAY, GPIO.LOW)
            webiopi.sleep(.1)
            GPIO.digitalWrite(RELAY, GPIO.HIGH)

    webiopi.sleep(1)


def sendMessage(messageType, messageTime, openTime=None):
    global doorStatus
    global regIDs

    gcmSendURL = 'https://gcm-http.googleapis.com/gcm/send'
    gcmHeaders = {'Authorization': 'key=' + API_KEY, 'Content-Type': 'application/json'}
    if messageType == "door_status_changed":
        gcmDataPayload = {'type': messageType, 'doorStatus': doorStatus, 'time': messageTime.strftime('%c')}
    elif messageType == "door_open_too_long":
        openTimeString = str(openTime).split(':')[-3] + " hours " + str(openTime).split(':')[-2] + " minutes " + str(openTime).split(':')[-1].split('.')[0] + " seconds"
        gcmDataPayload = {'type': messageType, 'doorStatus': doorStatus, 'time': messageTime.strftime('%c'),  'openTime': openTimeString}
    elif messageType == "door_closed_automatically":
        gcmDataPayload = {'type': messageType, 'doorStatus': doorStatus, 'time': messageTime.strftime('%c')}
    else:
        logger.error("Incorrect message type: {}".format(messageType))
    # gcmNotificationPayload = {'title': 'Garage Door Has Opened', 'body': 'Garage door opened at {}'.format(messageTime.strftime('%c')), 'icon': 'ic_door_open_notification2'}
    gcmPayload = {'registration_ids': regIDs, 'time_to_live': 86400, 'data': gcmDataPayload}
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
                logger.warning('Replacing reg_id {0} with {1}'.format(regIDs[i], result['registration_id']))
                regIDs.pop(i)
                regIDs.append(result['registration_id'])
                writeJsonIDs(regIDs)
            if 'error' not in result:
                logger.info('Successfully sent notification for reg_id {0}'.format(regIDs[i]))

    # Handling errors
    if responseDict['failure']:
        for i, result in enumerate(responseDict['results']):
            # Check for errors and act accordingly
            if 'error' in result and result['error'] in ['NotRegistered', 'InvalidRegistration']:
                # Remove reg_ids from database
                logger.warning("Removing reg_id: {0} from db".format(regIDs[i]))
                regIDs.pop(i)
                writeJsonIDs(regIDs)


def readJsonIDs():
    cwd = os.path.dirname(os.path.realpath(__file__))
    regIDFilePath = os.path.join(cwd, 'registrationIDs.txt')
    try:
        with open(regIDFilePath, 'r') as f:
            try:
                regIDsJson = json.load(f)
                logger.debug("Read registration IDs from file: " + str(regIDs))
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


@webiopi.macro
def getDoorStatus():
    doorOpen = GPIO.digitalRead(DOOR)
    if doorOpen:
        return "closed"
    else:
        return "open"


@webiopi.macro
def saveRegID(token):
    global regIDs

    if token not in regIDs:
        logger.info("Writing new token to file: " + token)
        regIDs.append(token)
        writeJsonIDs(regIDs)


def destroy():
    GPIO.digitalWrite(RELAY, GPIO.HIGH)
