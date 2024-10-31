#!/bin/bash

CWD=`pwd`

sleep 2
# destory state file
rm $CWD/monitor/hosts.properties
# Assuming that pc22 is our home PC
for i in {1..5}
do
    ssh -o StrictHostKeyChecking=no dh2010pc13 "pkill -f 'java MonitorApp' ; echo $?"
    "$CWD/proxyServer/shutdownProxyLocal.bash"
    sleep 0.2
    echo "Monitor stopped and proxy shut down with on dh2010pc13 $i times"
done

"$CWD/serverSqlite/shutdown_all.bash" 
echo "shutdown all hosts..."

rm $CWD/proxyServer/savedConsistentHashing