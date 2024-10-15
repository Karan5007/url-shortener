#!/bin/bash
rm -r /virtual/409a1dba
mkdir /virtual/409a1dba
rm -f /virtual/409a1dba/database.db
rm -f /virtual/409a1dba/replica-database.db

sqlite3 /virtual/409a1dba/database.db < schema.sql
sqlite3 /virtual/409a1dba/replica-database.db < schema.sql

ipAddr=$(ifconfig eno1 | grep 'inet ' | awk '{print $2}')
javac URLShortner.java
java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortner $ipAddr &
