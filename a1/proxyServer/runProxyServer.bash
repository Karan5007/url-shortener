#!/bin/bash

javac SimpleProxyServer.java
javac ConsistentHashing.java 
javac HostFileManager.java
java SimpleProxyServer true &


