import sqlite3

sqlite_file = 'sensorDataInt.db'
conn = sqlite3.connect(sqlite_file)
c = conn.cursor()
conn.execute("CREATE TABLE measures (timestamp DATETIME, t REAL, co2 REAL, hum REAL)")
conn.commit()
conn.close
