#!/bin/bash
remote_dir="/student/abdul322/a1group05/a1/serverSqlite"
hosts_file="hosts.txt"

[ ! -f "$hosts_file" ] && { echo "Hosts file not found"; exit 1; }

for host in $(cat "$hosts_file"); do
    echo "Starting URLShortner on $host"

    ssh $host "source ~/.bashrc && cd $remote_dir && ./runit.bash"
    
    echo "Started URLShortner on $host"
done

echo "All hosts have been started."