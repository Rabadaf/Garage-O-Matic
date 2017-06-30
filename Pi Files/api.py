import time
from flask import Flask, jsonify, request, make_response
from flask_httpauth import HTTPBasicAuth
import RPi.GPIO as GPIO
import os
import json
import logging

# Get the current pin value: GET http://arkf.duckdns.org:8000/api/GPIO/<num>/value
# Set the current pin value: POST http://arkf.duckdns.org:8000/api/GPIO/<num>/value/<0|1>
# Run a sequence on a pin: POST http://arkf.duckdns.org:8000/api/GPIO/<num>/sequence/<delay,sequence>
# Register new ID: POST http://arkf.duckdns.org:8000/api/id/<id>

# TODO: limit access to only setup pins
# TODO: error handling
# TODO: put regID code in script.py, import here

app = Flask(__name__)
auth = HTTPBasicAuth()

GPIO.setmode(GPIO.BCM)
RELAY = 17
DOOR = 8
GPIO.setup(RELAY, GPIO.OUT, initial=GPIO.HIGH)
GPIO.setup(DOOR, GPIO.IN, pull_up_down=GPIO.PUD_UP)

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger('Garage-O-Matic API')
handler = logging.FileHandler(os.path.join(os.path.dirname(os.path.realpath(__file__)), "GarageOMaticAPI.log"))
formatter = logging.Formatter("%(asctime)s - %(levelname)s - %(message)s")
handler.setFormatter(formatter)
logger.addHandler(handler)


@app.route('/')
def hello():
    return "Hello World! Boom!"


@auth.get_password
def get_password(username):
    # TODO: save a hash in a file, check hashes for equality
    logger.info('Authorization header: {}'.format(request.authorization))
    if username == 'ArkF':
        logger.info('Username {} is good'.format(username))
        return '12345'
    logger.info('Username {} is bad'.format(username))
    return None


@auth.error_handler
def unauthorized():
    return make_response(jsonify({'error': 'Unauthorized access'}), 401)


@app.route('/api/GPIO/<int:pin>/value', methods=['GET'])
@auth.login_required
def get_value(pin):
    ip = request.remote_addr
    logger.info('IP {} requested value of pin {} which is {}'.format(ip, pin, GPIO.input(pin)))
    return str(GPIO.input(pin))


@app.route('/api/GPIO/<int:pin>/value/<int:value>', methods=['POST'])
@auth.login_required
def set_value(pin, value):
    ip = request.remote_addr
    logger.info('IP {} set value of pin {} to {}'.format(ip, pin, value))
    GPIO.output(pin, value)
    return jsonify({'value': bool(GPIO.input(pin))})


@app.route('/api/GPIO/<int:pin>/sequence/<int:delay>,<sequence>', methods=['POST'])
@auth.login_required
def sequence(pin, delay, sequence):
    ip = request.remote_addr
    logger.info('IP {} ran sequence {} with delay of {} ms on pin {}'.format(ip, sequence, delay, pin))
    intSeq = [int(char) for char in sequence]
    # return jsonify({'intSeq': intSeq, 'delay': delay, 'pin': pin})
    for value in intSeq:
        GPIO.output(pin, value)
        time.sleep(delay / 1000.0)
    return jsonify({'value': bool(GPIO.input(pin)), 'intSeq': intSeq, 'delay': delay, 'pin': pin})


@app.route('/api/id/<regIDandUserID>', methods=['POST'])
@auth.login_required
def registerID(regIDandUserID):
    regID = regIDandUserID.split(',')[0]
    userID = regIDandUserID.split(',')[1]
    ip = request.remote_addr
    logger.info('IP {} submitted regID {} with User ID {} for addition to database'.format(ip, regID, userID))
    regIDs = readJsonIDs()
    newRegIDFull = [regID, userID]
    if newRegIDFull not in regIDs:
        logger.info("Writing new registration ID to file: " + regID + " with User ID: " + userID)
        regUserIDs = (regID, userID)
        regIDs.append(regUserIDs)
        writeJsonIDs(regIDs)
        return jsonify({'new ID': True, 'ID': regID})
    logger.info('RegID {} was already in database'.format(regID))
    return jsonify({'new ID': False})


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


if __name__ == "__main__":
    try:
        app.run(host='0.0.0.0', debug=True)
    except KeyboardInterrupt:
        pass
    finally:
        print('Cleaning up GPIO...')
        GPIO.cleanup()
