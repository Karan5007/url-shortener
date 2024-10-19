#!/bin/bash
# remote_dir="/student/$USER/a1group05/a1/serverSqlite"
remote_dir="/student/sing1871/CSC409/a1group05/a1/serverSqlite" # for karan! 
hosts_file="hosts.txt"

[ ! -f "$hosts_file" ] && { echo "Hosts file not found"; exit 1; }

for host in $(cat "$hosts_file"); do
    echo "Starting server on $host"

    ssh $host "cd $remote_dir && nohup ./runit.bash > server_output.log 2>&1 &"
    
    echo "Started server on $host"
done

echo "All hosts have been started."