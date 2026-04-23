#!/bin/bash
cd /mnt/d/cours/jobfinder
setsid java -jar server/target/server-1.0-SNAPSHOT.jar > server.log 2>&1 &
echo "Server starting..."
sleep 3
echo "Checking port..."
ss -tlnp | grep 8082 || echo "Port not found"
tail -5 server.log