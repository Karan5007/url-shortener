#!/bin/bash

HOSTS_FILE="hosts.txt"
[ ! -f "$HOSTS_FILE" ] && { echo "Hosts file not found"; exit 1; }

for host in $(cat "$HOSTS_FILE"); do
    echo "Shutting down server on $host..."
    # sleep 10
    ssh -o StrictHostKeyChecking=no $host "./a1group05/a1/serverSqlite/shutdown_local.bash"
    # ssh $host "./CSC409/a1group05/a1/serverSqlite/shutdown_local.bash" # for karan! 
done