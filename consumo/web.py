
from flask import Flask, render_template, request, redirect
import sqlite3
import json
import time

app = Flask(__name__)

@app.route('/')
def index():
	return 'This is my flask website and it is so cool'

@app.route("/dataCO2")
def dataCO2():
	conn = sqlite3.connect("sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, co2_int FROM measures")
	results = cursor.fetchall()
	conn.close()
	return json.dumps(results)

@app.route("/dataTint")
def dataTint():
	conn = sqlite3.connect("sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, t_int FROM measures")
	results = cursor.fetchall()
	conn.close()
	return json.dumps(results)

@app.route("/dataText")
def dataText():
	conn = sqlite3.connect("sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, t_ext FROM measures")
	results = cursor.fetchall()
	conn.close()
	return json.dumps(results)
	
@app.route("/dataHint")
def dataHint():
	conn = sqlite3.connect("sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, hum_int FROM measures")
	results = cursor.fetchall()
	conn.close()
	return json.dumps(results)

@app.route("/dataHext")
def dataHext():
	conn = sqlite3.connect("sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, hum_ext FROM measures")
	results = cursor.fetchall()
	conn.close()
	return json.dumps(results)
	
@app.route("/dataTdiff")
def dataTdiff():
	conn = sqlite3.connect("sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, t_ext, t_int FROM measures")
	results = cursor.fetchall()
	conn.close()
	return json.dumps(results)

@app.route("/dataConsumo1")
def dataConsumo1():
	conn = sqlite3.connect("sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, consumo1 FROM measures")
	results = cursor.fetchall()
	conn.close()
	return json.dumps(results)

@app.route("/dataConsumo2")
def dataConsumo2():
	conn = sqlite3.connect("sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute("SELECT 1000*timestamp, consumo2 FROM measures")
	results = cursor.fetchall()
	conn.close()
	return json.dumps(results)	

@app.route("/postData", methods=['POST'])
def saveData():	
	sql_insert = "INSERT INTO measures(timestamp, t_int, t_ext, hum_int, hum_ext, co2_int, consumo1, consumo2) VALUES (?,?,?,?,?,?,?,?)"
	conn = sqlite3.connect("sensor_database/sensorData.db")
	cursor = conn.cursor()
	cursor.execute(sql_insert, (time.time()+7200, request.form['tempInt'], request.form['tempExt'], request.form['humInt'], request.form['humExt'], request.form['co2'], request.form['consumo1'], request.form['consumo2']))
	conn.commit()
	conn.close()
	return "OK"

@app.route("/getData", methods=['GET'])
def getData():	
	conn = sqlite3.connect("sensor_database/sensorDataInt.db")
	cursor = conn.cursor()
	cursor.execute("SELECT t, hum, co2 FROM measures ORDER BY timestamp DESC LIMIT 1;")
	results = cursor.fetchall()
	conn.close()
	return json.dumps(results)

@app.route("/deleteData", methods=['GET'])
def deleteData():
	conn = sqlite3.connect("sensor_database/sensorDataInt.db")
	sql = 'DELETE FROM measures'
	cur = conn.cursor()
	cur.execute(sql)
	conn.commit()
	conn.close()
	
	conn = sqlite3.connect("sensor_database/sensorData.db")
	sql = 'DELETE FROM measures'
	cur = conn.cursor()
	cur.execute(sql)
	conn.commit()
	conn.close()
	
	return redirect("http://192.168.43.42:5000/graph", code=302)
	
@app.route("/graph")
def graph():
	return render_template('graph.html')


if __name__ == '__main__':
	app.run(debug=True, host='0.0.0.0')
