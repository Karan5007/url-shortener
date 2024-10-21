#!/bin/bash

PORT=8081

PID=$(lsof -t -i:$PORT)

if [ -n "$PID" ]; then
    # echo "Shutting down Server (PID: $PID) on $(hostname)..."
    kill -15 $PID
    # echo "Server on $(hostname) terminated."
fi


javac SimpleProxyServer.java
javac ConsistentHashing.java 
javac HostFileManager.java
java SimpleProxyServer  &


