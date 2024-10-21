#!/bin/bash

CWD = 'pwd'


./$CWD/serverSqlite/shutdown_all.bash 
echo "Starting all hosts..."

sleep 2
# destory state file
rm monitor/hosts.properties
# Assuming that pc22 is our home PC
for i in {1..5}
do
    ssh -o StrictHostKeyChecking=no dh2026pc23 "pkill -f 'java MonitorApp' ; echo $?"
    ./$CWD/a1/proxyServer/shutdownProxyLocal.bash
    sleep 0.2
    echo "Monitor stopped and proxy shut down with on dh2026pc23 $i times"
done

rm $CWD/proxyServer/savedConsistentHashing