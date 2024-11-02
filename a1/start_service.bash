#!/bin/bash

CWD=`pwd`

rm "$CWD/monitor/hosts.properties"
rm "$CWD/proxyServer/savedConsistentHashing"
# Assuming that pc22 is our home PC
ssh -o StrictHostKeyChecking=no dh2010pc13 "cd $CWD/monitor && ./startMonitorFirstTime.bash"

sleep 3

echo "Monitor started with on dh2010pc13"

echo "Starting proxy server..."
(cd "$CWD/proxyServer" && ./runProxyServer.bash) &
PROXY_PID=$!
echo "Proxy server started with PID $PROXY_PID"
sleep 3

echo "Starting all hosts..."
(cd "$CWD/serverSqlite" && ./start_all.bash) &
HOSTS_PID=$!
echo "All hosts started with PID $HOSTS_PID"

sleep 3



echo "Processes running:"
echo "Proxy PID: $PROXY_PID"
echo "Hosts PID: $HOSTS_PID"

wait $PROXY_PID $HOSTS_PID 
