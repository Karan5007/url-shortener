#!/bin/bash 
javac LoadTest.java 
echo $SECONDS 
java LoadTest 127.0.0.1 8081 11 PUT 1000 &
java LoadTest 127.0.0.1 8081 12 PUT 1000 & 
java LoadTest 127.0.0.1 8081 13 PUT 1000 & 
java LoadTest 127.0.0.1 8081 14 PUT 1000 & 
wait $(jobs -p) 
echo $SECONDS
