import sqlite3
import sys

con = sqlite3.connect(sys.argv[1])
cur = con.cursor()
print("sessions:", cur.execute(
    "SELECT sessionId, studentId, sessionDate, sessionStartTime FROM sessions ORDER BY sessionStartTime DESC LIMIT 5"
).fetchall())
print("students:", cur.execute(
    "SELECT studentId, email FROM students LIMIT 3"
).fetchall())
print("simulation analytics:", cur.execute(
    "SELECT screenName, conceptId, interactionType, eventType, timestamp FROM app_analytics "
    "WHERE screenName='SIMULATION' ORDER BY analyticsId DESC LIMIT 10"
).fetchall())
print("interactions:", cur.execute(
    "SELECT simulationTitle, elementClicked, elementType, isCorrect FROM simulation_interactions LIMIT 10"
).fetchall())
con.close()
