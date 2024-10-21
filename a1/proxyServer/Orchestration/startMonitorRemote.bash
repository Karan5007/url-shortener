#!/bin/bash
# $1 = new IP

# Start the monitor and detach the process to avoid hanging
# ssh -o StrictHostKeyChecking=no $1 "cd /student/sing1871/CSC409/a1group05/a1/monitor/ && ./startMonitor.bash > janky-logging.txt 2>&1 & disown"
ssh -o StrictHostKeyChecking=no $1 "cd /student/$USER/a1group05/a1/monitor/ && ./startMonitor.bash > janky-logging.txt 2>&1 & disown"
# Check if the command was successful 
if [ $? -eq 0 ]; then
    exit 0
else
    exit 1
fi
