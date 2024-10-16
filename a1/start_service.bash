#!/bin/bash
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

cd ../MonitorApp
echo "Starting monitor..."
./startMonitor.bash &
MONITOR_PID=$!
echo "Monitor started with PID $MONITOR_PID"

echo "Processes running:"
echo "Proxy PID: $PROXY_PID"
echo "Hosts PID: $HOSTS_PID"
echo "Monitor PID: $MONITOR_PID"

wait $PROXY_PID $HOSTS_PID $MONITOR_PID
