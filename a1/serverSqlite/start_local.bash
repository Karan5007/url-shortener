rm -r /virtual/409a1dbk 
mkdir /virtual/409a1dbk
rm -f /virtual/409a1dbk/main.db
rm -f /virtual/409a1dbk/replica.db

chmod 777 /virtual/409a1dbk
sqlite3 /virtual/409a1dbk/main.db < schema.sql
sqlite3 /virtual/409a1dbk/replica.db < schema.sql

chmod 777 /virtual/409a1dbk/main.db
chmod 777 /virtual/409a1dbk/replica.db

ipAddr=$(ifconfig eno1 | grep 'inet ' | awk '{print $2}')
hostAdder=$1
javac URLShortner.java
java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortner $ipAddr $hostAdder &