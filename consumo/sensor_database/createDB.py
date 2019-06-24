import sqlite3

sqlite_file = 'sensorData.db'
conn = sqlite3.connect(sqlite_file)
c = conn.cursor()
conn.execute("CREATE TABLE measures (timestamp DATETIME, t_int REAL, t_ext REAL, co2_int REAL, co2_ext REAL, hum_int REAL, hum_ext REAL, consumo1 REAL, consumo2 REAL)")
conn.commit()
conn.close
