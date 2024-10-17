#!/bin/bash
# rm -r /virtual/genericdb
# mkdir /virtual/genericdb
# rm -f /virtual/genericdb/main.db
# rm -f /virtual/genericdb/replica.db

# sqlite3 /virtual/genericdb/main.db < schema.sql
# sqlite3 /virtual/genericdb/replica.db < schema.sql

# ipAddr=$(ifconfig eno1 | grep 'inet ' | awk '{print $2}')
# javac URLShortner.java
# java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortner $ipAddr &

# KARAN:
rm -r /virtual/409a1dbk 
mkdir /virtual/409a1dbk
rm -f /virtual/409a1dbk/main.db
rm -f /virtual/409a1dbk/replica.db

sqlite3 /virtual/409a1dbk/main.db < schema.sql
sqlite3 /virtual/409a1dbk/replica.db < schema.sql

ipAddr=$(ifconfig eno1 | grep 'inet ' | awk '{print $2}')
javac URLShortner.java
java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortner $ipAddr &
