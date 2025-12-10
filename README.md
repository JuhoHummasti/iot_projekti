***This project is a weather application that collects data from a sensor (BMP280) using
Raspberry Pi Pico W circuitry. The Pico W uses wireless connectivity to send
the data into the cloud and then is forwarded to the database. The two data
points are then retrieved from the database and visualized in the mobile appli-
cation. Additionally, the temperature reading is used as a control for an LED.
A temperature that is too low will turn on the LED***

## Getting Started 

# STEP 1 (thonny):
Download thonny.
https://micropython.org/download/RPI_PICO_W/

![alt text](image.png)

Download necessary packages into thonny. Tools -> Manage packages -> umqtt.robust and umqtt.simple

![alt text](image-1.png)

![alt text](image-2.png)

Change the parameters of MQTT CONFIG and the Wifi connection.

![alt text](image-3.png) 

NOTE! MQTT_BROKER, PORT, USER and PASSWORD will get from HIVEMQ.

# STEP 2 (HiveMQ):
Login to HiveMQ or create an account.

![alt text](image-4.png)

Create a new cluster and select free

![alt text](image-6.png)

Manage cluster and copy the URL and the port into your main.py

![alt text](image-7.png)

Go to access management in your service and add credentials.

![alt text](image-8.png)

Then go to web client and start the cloud with your credentials.

![alt text](image-9.png)