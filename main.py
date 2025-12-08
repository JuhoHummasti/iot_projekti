from machine import Pin, I2C
import network
import utime
from bmp280 import BMP280
import ssl
from umqtt.simple import MQTTClient

# ---------- MQTT CONFIG ----------
MQTT_CLIENT_ID   = "AnyNameWillDo"                # any ID you like 
MQTT_BROKER      = "URL_OF_YOUR_BROKER"           # <---CHANGE ME
MQTT_PORT        = 8883                           # 8883 for HiveMQ Cloud with TLS
MQTT_USER        = "MQ_USER_NAMR"                  # MQTT Username <---CHANGE ME
MQTT_PASSWORD    = "MQ_PASSWORD"                     # MQTT PASSWORD <---CHANGE ME
MQTT_TOPIC_TEMP  = b"TOPIC_NAME"                   # matches your Node-RED topic <---CHANGE ME
MQTT_TOPIC_PRESS = b"TOPIC_NAME"               # optional, Topic name for pressure <---CHANGE ME
MQTT_CONTROL     = b"TOPIC_NAME"                # Topic name for LED ON/OFF from Node-RED <---CHANGE ME
# ----------------------------------

# Small helper to make sure we pass bytes to MQTT client
def to_bytes(s):
    return s if isinstance(s, bytes) else str(s).encode()

# ---------- WiFi CONNECTION ----------
wlan = network.WLAN(network.STA_IF)
wlan.active(True)
wlan.config(pm=0xa11140)  # Disable powersave mode (good for MQTT)
wlan.connect('wifiname', 'wifipassword') #<---CHANGE ME

max_wait = 10
while max_wait > 0:
    if wlan.status() < 0 or wlan.status() >= 3:
        break
    max_wait -= 1
    print("waiting for connection...")
    utime.sleep(1)

if wlan.status() != 3:
    raise RuntimeError("wifi connection failed")
else:
    print("connected")
    status = wlan.ifconfig()
    print("ip =", status[0])

# ---------- SENSOR SETUP ----------
i2c = I2C(0, sda=Pin(0), scl=Pin(1), freq=100000)
bmp = BMP280(i2c)

led_pin = Pin("LED", Pin.OUT)

# ---------- MQTT SETUP ----------
context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
context.verify_mode = ssl.CERT_NONE  # ok for lab / HiveMQ Cloud without CA

def connect_mqtt():
    print("Connecting to MQTT broker...")
    client = MQTTClient(
        client_id=to_bytes(MQTT_CLIENT_ID),
        server=MQTT_BROKER,           # str is OK, lib converts
        port=MQTT_PORT,
        user=to_bytes(MQTT_USER),
        password=to_bytes(MQTT_PASSWORD),
        keepalive=7200,
        ssl=context,
    )
    client.connect()
    print("Connected to MQTT!")
    return client

def on_message(topic, msg):
    print("Received message:", msg, "on topic:", topic)
    if msg == b"ON":
        led_pin.on()
        print("LED ON")
    elif msg == b"OFF":
        led_pin.off()
        print("LED OFF")

client = connect_mqtt()
client.set_callback(on_message)
client.subscribe(MQTT_CONTROL)  # listens for ON/OFF from Node-RED

def publish(topic, value):
    if isinstance(value, str):
        value = value.encode()
    print("Publishing:", topic, value)
    client.publish(topic, value)
    print("publish done")

# ---------- MAIN LOOP ----------
while True:
    # Read sensor data
    temperature = bmp.temperature
    pressure = bmp.pressure
    
    print("\n")
    print("Temperature:", temperature, "C")
    print("Pressure:", pressure, "hPa")
    print("\n")
    
    # Publish to topics that Node-RED listens to
    publish(MQTT_TOPIC_TEMP, f"{temperature:.2f}")
    publish(MQTT_TOPIC_PRESS, f"{pressure:.2f}")

    # Check if any control messages ("ON"/"OFF") arrived
    client.check_msg()

    utime.sleep(30)


