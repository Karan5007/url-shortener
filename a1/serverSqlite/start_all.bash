#!/bin/bash
remote_dir="/student/$USER/a1group05/a1/serverSqlite"
# remote_dir="/student/sing1871/CSC409/a1group05/a1/serverSqlite" # for karan! 
# remote_dir="/shared/a1group05/a1/serverSqlite" # for docker! 
hosts_file="hosts.txt"

[ ! -f "$hosts_file" ] && { echo "Hosts file not found"; exit 1; }

hostAddr=$(ifconfig eno1 | grep 'inet ' | awk '{print $2}') # takes IPAdder of machine running the bash command. 
# Assuming that this is only run from PC running ProxyServer

for host in $(cat "$hosts_file"); do
    echo "Starting server on $host"
    
    ssh -o StrictHostKeyChecking=no $host "cd $remote_dir && nohup ./start_local.bash $hostAddr > server_output.log 2>&1 &"
    
    echo "Started server on $host"
done

echo "All hosts have been started."