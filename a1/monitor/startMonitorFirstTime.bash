# !/bin/bash

javac HostFileManager.java 
javac MonitorApp.java
date +"%A, %B %d, %Y %H:%M:%S.%3N" >> monitor-logging.txt
echo 'New Session Launched' >> monitor-logging.txt
java MonitorApp true #>> monitor-logging.txt 2>&1 & disown
# Literally 1984.


# # Compile HostFileManager first (assumes you're running this script from the 'monitor' directory)
# javac -cp ../hostfilemanager ../hostfilemanager/HostFileManager.java

# # Compile MonitorApp, including the classpath to find HostFileManager.class
# javac -cp ../hostfilemanager:. MonitorApp.java

# # Run MonitorApp, again specifying the classpath to find HostFileManager.class
# java -cp ../hostfilemanager:. MonitorApp true
