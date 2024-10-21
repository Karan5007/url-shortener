# !/bin/bash
# $1 = new IP, 
ssh -o StrictHostKeyChecking=no $1 "cd $HOME/a1group05/a1/proxyServer && ./restartProxyServer.bash > janky-proxy-logging.txt 2>&1 & disown"

if [ $? -eq 0 ]; then
    exit 0
else
    exit 1
fi