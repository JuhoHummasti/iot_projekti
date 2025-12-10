***This project is a weather application that collects data from a sensor (BMP280) using
Raspberry Pi Pico W circuitry. The Pico W uses wireless connectivity to send
the data into the cloud and then is forwarded to the database. The two data
points are then retrieved from the database and visualized in the mobile appli-
cation. Additionally, the temperature reading is used as a control for an LED.
A temperature that is too low will turn on the LED***

## Getting Started 

# Step 1 (Thonny):
Download thonny.
https://micropython.org/download/RPI_PICO_W/

Download necessary packages into thonny. "Tools" > "Manage packages" > "umqtt.robust" and "umqtt.simple"

![alt text](image-1.png)

![alt text](image-2.png)

Change the parameters of MQTT CONFIG and the Wifi connection.

![alt text](image-3.png) 

NOTE! MQTT_BROKER, PORT, USER and PASSWORD will get from HIVEMQ.

# Step 2 (HiveMQ):
Login to HiveMQ or create an account.

Create a new cluster and select free.

Manage cluster and copy the URL and the port into your main.py

![alt text](image.png)

Go to access management in your service and add credentials. These credentials are added to the code.

![alt text](image-8.png)

Then go to web client and start the cloud with your credentials.

![alt text](image-9.png)

You can try to connect to the wifi and MQTT broker. Subscribe on web client and you can see the data.

![alt text](image-4.png)

![alt text](image-6.png)

# Step 3 (InfluxDB):
Install InfluxDB. https://docs.influxdata.com/influxdb/v2/install/?t=Windows+Powershell

Unextract the files. Open CMD and move to the directory, where the files are located.

Then run command `.\influxd.exe`

![alt text](image-10.png)

Login or create an account to InfluxDB Cloud 2.0.


# Step 4 (Nodejs):
Install nodejs. https://nodejs.org/en

Open CMD as administrator and run the following commands.

`npm install -g --unsafe-perm node-red`

`node-red` < start node

![alt text](image-7.png)

Go to http://localhost:1880

Install required packages: 
Go to the menu (top right corner) > "Manage palette" > "Install" > "node-red-contrib-influxdb"

Import the "flowsModule.json" from the menu.  