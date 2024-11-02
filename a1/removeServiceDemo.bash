#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <hostname>"
  exit 1
fi
CWD=`pwd`

HOSTNAME=$1

hostAddr=$(ifconfig eno1 | grep 'inet ' | awk '{print $2}') 

ssh -o StrictHostKeyChecking=no "$HOSTNAME" "cd $CWD/serverSqlite && ./shutdown_local.bash $hostAddr > server_output.log 2>&1 &"

# REMOTE_IP=$(ssh "$HOSTNAME" "ifconfig eno1 | grep 'inet ' | awk '{print \$2}'")

# curl -i -X PUT "localhost:8081/?method=addedNode&ipAddr=$REMOTE_IP"
