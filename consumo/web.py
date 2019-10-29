
from flask import Flask, render_template, request, redirect, send_file
import sqlite3
import json
import time

from time import sleep
import serial
import datetime
from openpyxl import Workbook

import _thread as thread

app = Flask(__name__)

def delete_empty(results):
	final_result = []
	for r in results:
		if r[1] != '':
			final_result.append(r)
	return final_result

def measures():

	# Establish the connection on a specific port
	ser = serial.Serial('/dev/ttyUSB0', 9600)
	sendCO2 = []
	sendH = []
	sendT = []
	insert = True
	while True:
		output=ser.readline().decode('ascii') # Read the newest output
		data = output.split(' ')

		if len(data) > 8:
			try:
				# Leer datos sensor
				humidity = float(data[2].split('\r')[0])/10
				temp = (float(data[4].split('\r')[0])-1000)/10
				co2 = int(data[6].split('\r')[0])
				co2_instant = int(data[8].split('\r')[0])

				sendCO2.append(co2)
				sendH.append(humidity)
				sendT.append(temp)

				# Insertar inmediatamente si es el primer dato de la bbdd
				if not insert:
					conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
					cursor = conn.cursor()
					cursor.execute("SELECT * FROM measures")
					results = cursor.fetchall()
					conn.close()

					if(len(results) == 0):
						insert = True

				if(len(sendCO2) == 6 or insert):
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
					sql_insert = "INSERT INTO measures(timestamp, t_int, t_ext, hum_int, hum_ext, co2_int, consumo1, consumo2) VALUES (?,?,?,?,?,?,?,?)"
					conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
					cursor = conn.cursor()
					cursor.execute(sql_insert, (time.time(), temp, '', humidity, '', co2, '', ''))
					conn.commit()
					conn.close()

					# Mirar el resto de datos

					sendCO2 = []
					sendH = []
					sendT = []
				sleep(10)

			except Exception as e:
				print(e)
				print("Error al recoger los datos")


@app.route("/dataCO2")
def dataCO2():
	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, co2_int FROM measures")
	results = delete_empty(cursor.fetchall())
	conn.close()
	return json.dumps(results)

@app.route("/dataTint")
def dataTint():
	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, t_int FROM measures")
	results = delete_empty(cursor.fetchall())
	conn.close()
	return json.dumps(results)

@app.route("/dataText")
def dataText():
	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, t_ext FROM measures")
	results = delete_empty(cursor.fetchall())
	conn.close()
	return json.dumps(results)

@app.route("/dataHint")
def dataHint():
	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, hum_int FROM measures")
	results = delete_empty(cursor.fetchall())
	conn.close()
	return json.dumps(results)

@app.route("/dataHext")
def dataHext():
	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, hum_ext FROM measures")
	results = delete_empty(cursor.fetchall())
	conn.close()
	return json.dumps(results)

@app.route("/dataTdiff")
def dataTdiff():
	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, t_ext, t_int FROM measures")
	results = delete_empty(cursor.fetchall())
	conn.close()
	return json.dumps(results)

@app.route("/dataConsumo1")
def dataConsumo1():
	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, consumo1 FROM measures")
	results = delete_empty(cursor.fetchall())
	conn.close()
	return json.dumps(results)

@app.route("/dataConsumo2")
def dataConsumo2():
	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, consumo2 FROM measures")
	results = delete_empty(cursor.fetchall())
	conn.close()
	return json.dumps(results)

@app.route("/postData", methods=['POST'])
def saveData():
	sql_insert = "INSERT INTO measures(timestamp, t_int, t_ext, hum_int, hum_ext, co2_int, consumo1, consumo2) VALUES (?,?,?,?,?,?,?,?)"
	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute(sql_insert, (time.time(), request.form['tempInt'], request.form['tempExt'], request.form['humInt'], request.form['humExt'], request.form['co2'], request.form['consumo1'], request.form['consumo2']))
	conn.commit()
	conn.close()
	return redirect("http://192.168.43.42:5000/graph", code=302)

@app.route("/getData", methods=['GET'])
def getData():
	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT t_int, hum_int, co2_int FROM measures ORDER BY timestamp DESC LIMIT 1;")
	results = cursor.fetchall()
	conn.close()
	return json.dumps(results)

@app.route("/deleteData", methods=['GET'])
def deleteData():

	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	sql = 'DELETE FROM measures'
	cur = conn.cursor()
	cur.execute(sql)
	conn.commit()
	conn.close()
	return redirect("http://192.168.43.42:5000/graph", code=302)

@app.route('/download')
def downloadFile ():

	conn = sqlite3.connect("/home/pi/consumo/sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT * FROM measures ORDER BY timestamp DESC;")

	filename = 'outfile.xlsx'
	book = Workbook()
	sheet = book['Sheet']
	sheet.title = "Datos"

	sheet.append(['Dia', 'Hora', 'Temperatura Interior', 'Temperatura Exterior', 'CO2', 'Humedad Interior', 'Humedad Exterior', 'Consumo Enchufe 1', 'Consumo Enchufe 2'])
	for row in cursor.fetchall():
		ts = int(row[0])
		day = datetime.datetime.utcfromtimestamp(ts).strftime('%Y-%m-%d')
		hour = datetime.datetime.utcfromtimestamp(ts).strftime('%H:%M:%S')
		data = [day, hour]
		for i in range(1, len(row)):
			data.append(row[i])
		sheet.append(data)

	book.save(filename)

	conn.close()
	print("filename " + str(filename))
	return send_file(filename, as_attachment=True)

@app.route("/graph")
def graph():
	return render_template('graph.html')


if __name__ == '__main__':
	thread.start_new_thread(measures, () )
	app.run(host='0.0.0.0')
