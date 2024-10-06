#!/bin/bash

mkdir -p /virtual/abdul322
rm -f /virtual/abdul322/database.db

sqlite3 /virtual/abdul322/database.db < schema.sql

javac URLShortner.java
java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortner &
