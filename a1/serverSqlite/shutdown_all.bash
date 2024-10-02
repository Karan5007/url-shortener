#!/bin/bash

HOSTS_FILE="hosts.txt"
[ ! -f "$HOSTS_FILE" ] && { echo "Hosts file not found"; exit 1; }

for host in $(cat "$HOSTS_FILE"); do
    echo "Shutting down server on $host..."
    ssh $host "./a1group05/a1/serverSqlite/shutdown_local.bash"
done