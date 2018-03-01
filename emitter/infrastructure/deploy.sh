#!/usr/bin/env bash

##
## Deploy and run the emitter
##

set -e

if [ "$#" -ne 3 ] || ([ "$3" != "PART1"  ] && [ "$3" != "PART2" ]); then
    echo "usage: $0 <ssh key> <ec2 instance> <profile>"
    echo "    where <profile> is PART1 or PART2"
    echo "    eg. $0 eventProcessing-awskey.pem ec2-34-243-251-193.eu-west-1.compute.amazonaws.com PART1"
    exit 2
fi

KEY=$1 #  eg. "my-aws-key.pem"
SERVER=$2  # eg. ec2-34-243-88-182.eu-west-1.compute.amazonaws.com
PROFILE=$3
COMMAND="java -jar emitter-1.0.jar $PROFILE"

echo "Testing to see if emitter is running on the remote server"
ssh -i ${KEY} ec2-user@${SERVER} "! ps ax | grep \"$COMMAND\" | grep -v grep"

echo "Building package"
mvn -q clean package

echo "Copy jar to server"
scp -q -i ${KEY} $(dirname $0)/../target/emitter-1.0.jar ec2-user@${SERVER}:~

# Start the emitter
echo "Starting the emitter, you can safely disconnect with ^C and it will"
echo " keep running. if you would like to stop the emitter, then ssh to"
echo " the server and run: "
echo " ps ax | grep \"$COMMAND\" | grep -v grep | awk '{print \$1}' | xargs -r kill"

ssh -i ${KEY} ec2-user@${SERVER} "$COMMAND | tee emitter-log-$PROFILE"
