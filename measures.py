#!/usr/bin/python
from time import sleep
import serial
import http.client
import urllib.parse
import time
import sqlite3
from datetime import datetime

import pytz # $ pip install pytz
from tzlocal import get_localzone

key = "V2HLHDG36J490TTW"

# Establish the connection on a specific port
ser = serial.Serial('/dev/ttyUSB0', 9600) 
sendCO2 = []
sendH = []
sendT = []
insert = True
while True:
	output=ser.readline().decode('ascii') # Read the newest output
	#print(output)
	data = output.split(' ')
	
	if len(data) > 8:
		try:
			#print(data)
			# Leer datos sensor
			humidity = float(data[2].split('\r')[0])/10
			temp = (float(data[4].split('\r')[0])-1000)/10
			co2 = int(data[6].split('\r')[0])
			co2_instant = int(data[8].split('\r')[0])
			
			
			#print("Humedad: " + str(humidity))
			#print("Temperatura: " + str(temp))
			#print("CO2: " + str(co2))
			#print("CO2 (sin filtrar): " + str(co2_instant))

			sendCO2.append(co2)
			sendH.append(humidity)
			sendT.append(temp)
			
			if not insert:
				conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
				cursor = conn.cursor()
				cursor.execute("SELECT * FROM measures")
				results = cursor.fetchall()
				conn.close()
			
				if(len(results) == 0):
					insert = True
			
			if(len(sendCO2) == 15 or insert):
				insert = False
				# Calcular media
				aux_co2 = 0
				aux_h = 0
				aux_t = 0
				for i in range(0, len(sendCO2)):
					aux_co2 = aux_co2 + sendCO2[i]
					aux_h = aux_h + sendH[i]
					aux_t = aux_t + sendT[i]
				co2 = aux_co2/len(sendCO2)
				humidity = aux_h/len(sendH)
				temp = aux_t/len(sendT)

				print("Humedad media: " + str(humidity))
				print("Temperatura media: " + str(temp))
				print("CO2 medio: " + str(co2))
				
				# Guardar en base de datos
				sql_insert = "INSERT INTO measures(timestamp, t, hum, co2) VALUES (?,?,?,?)"
				conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorDataInt.db")
				cursor = conn.cursor()
				cursor.execute(sql_insert, (time.time()+7200, temp, humidity, co2))
				conn.commit()
				conn.close()
				
				# Enviarlos a Thingspeak
				'''
				params = urllib.parse.urlencode({'field1': co2, 'field2': humidity, 'field3': temp, 'key':key })
				headers = {"Content-typZZe": "application/x-www-form-urlencoded","Accept": "text/plain"}
				conn = http.client.HTTPConnection("api.thingspeak.com:80")
				try:
					conn.request("POST", "/update", params, headers)
					response = conn.getresponse()
					conn.close()
					#sleep(20)
				except:
        	    			print("Fallo en la conexion con ThingSpeak")
				'''
				sendCO2 = []
				sendH = []
				sendT = []
			sleep(20)

		except Exception as e:
			print(e)
			print("Error al recoger los datos")
