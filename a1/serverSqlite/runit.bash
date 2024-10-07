#!/bin/bash

mkdir -p /virtual/abdul322
rm -f /virtual/abdul322/database.db

sqlite3 /virtual/abdul322/database.db < schema.sql
ipAddr=$(ifconfig eno1 | grep 'inet ' | awk '{print $2}')
javac URLShortner.java
java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortner $ipAddr &
