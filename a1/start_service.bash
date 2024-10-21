#!/bin/bash

CWD = 'pwd'

rm monitor/hosts.properties
rm proxyServer/savedConsistentHashing
# Assuming that pc22 is our home PC
ssh -o StrictHostKeyChecking=no dh2026pc23 "cd $CWD/monitor && ./startMonitorFirstTime.bash;

sleep 3

echo "Monitor started with on dh2026pc23

cd proxyServer
echo "Starting proxy server..."
./runProxyServer.bash &
PROXY_PID=$!
echo "Proxy server started with PID $PROXY_PID"
sleep 3

cd ../serverSqlite
echo "Starting all hosts..."
./start_all.bash &
HOSTS_PID=$!
echo "All hosts started with PID $HOSTS_PID"
sleep 3



echo "Processes running:"
echo "Proxy PID: $PROXY_PID"
echo "Hosts PID: $HOSTS_PID"

wait $PROXY_PID $HOSTS_PID 
