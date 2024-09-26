#!/bin/bash

# mkdir /virtual/$USER
# rm /virtual/$USER/example.db
rm database.db
sqlite3 database.db < schema.sql

javac URLShortner.java
java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortner &



