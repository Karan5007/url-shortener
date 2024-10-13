#!/bin/bash

mkdir -p /virtual/409a1db
rm -f /virtual/409a1db/database.db
rm -f /virtual/409a1db/replica-database.db

sqlite3 /virtual/409a1db/database.db < schema.sql
sqlite3 /virtual/409a1db/replica-database.db < schema.sql

ipAddr=$(ifconfig eno1 | grep 'inet ' | awk '{print $2}')
javac URLShortner.java
java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortner $ipAddr &
