#!/bin/bash

# Set your remote node and application details
REMOTE_NODE=$1
APP_NAME="MonitorApp"

ssh -o StrictHostKeyChecking=no "$REMOTE_NODE" "jps | grep $APP_NAME" > /dev/null

# exit code 0 = app is running, exit code 1 = app not running, exit code not 1 or zero, ssh failure