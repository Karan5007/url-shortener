#!/bin/bash

# mkdir /virtual/$USER
# rm /virtual/$USER/example.db
IP_ADDRESS="142.1.46.25"
PORT="8080"

rm database.db
sqlite3 database.db < schema.sql

javac URLShortner.java
java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortner "$IP_ADDRESS" "$PORT" &



