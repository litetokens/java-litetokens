#!/bin/bash
kill -9 `cat /home/litetokens/pid.txt`
nohup  java -jar /home/litetokens/java-litetokens/java-litetokens.jar -p $LOCAL_WITNESS_PRIVATE_KEY --witness -c /home/litetokens/config.conf > /home/litetokens/litetokens-shell.log 2>&1 & echo $! >/home/litetokens/pid.txt