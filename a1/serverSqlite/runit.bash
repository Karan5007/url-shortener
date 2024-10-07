#!/bin/bash

mkdir -p /virtual/$USER
rm -f /virtual/$USER/database.db

sqlite3 /virtual/$USER/database.db < schema.sql
ipAddr=$(ifconfig eno1 | grep 'inet ' | awk '{print $2}')
javac URLShortner.java
java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortner $ipAddr &
