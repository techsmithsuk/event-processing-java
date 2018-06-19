Emitter
=======

# Deployment

### Create an Access Key
* `AWS` -> `Services` -> `EC2` -> `Key Pairs` -> `Create Key Pair`

### Create Cloudformation Stacks
* `AWS` -> `Services` -> `CloudFormation` 
* Ensure there are no pre-existing EventProcessing stacks, if there are then go to `Services`
  -> `S3` and empty and delete the EventProcessing S3 bucket.  Then delete the pre-existing
  stack.
* `Create Stack` -> `Upload a Template` and choose the template in 
  `infrastructure/cloudformation-template.json`.
  * Stack Name - "EventProcessing-Stack1" or similar
  * Key Name - Access Key name set up earlier
  * VpcId - Set this to the default VPC Id, visible on `Services` -> `VPC` -> `Your VPCs`, 
    eg. `vpc-f3f95896`.  Look for the column `Default VPC`.
  * If you like, you can assign names to the users which the students will use to access the
    Topic, there are 7 such users created by default.  
* `Next`, then leave all options on `Options` page on their defaults, and `Next` again.
* On `Review` page, check the `I acknowledge that ...` box and click `Create`.
* Wait for the stack status to be `CREATE_COMPLETED` (you can refresh via a button near the top
  right).  This will take a couple of minutes.
* Click on the stack name, expand the `Outputs` section and note them down.

### Update App Configuration

* Update `src/main/resources/part1.conf` with Outputs from previous section:
  * `s3Bucket = <LocationsS3Bucket>` 
  * `topicArn = <EmitterTopicArnPartPart1>`
* Similarly, update `src/main/resources/part1.conf`:
  * `s3Bucket = <LocationsS3Bucket>` 
  * `topicArn = <EmitterTopicArnPartPart2>`

### Run the Emitter

`infrastructure/deploy.sh <Access Key PEM> <EmitterPublicDNS> <Profile>` 
Where profile is `PART1` or `PART2`, depending on whether you want to deploy the emitter
for the first task, or the second.  

You can (and should) deploy both to the same machine at the same time when necessary.

#### Troubleshooting

* If `deploy.sh` complains about the wrong Java version, or of Java missing entirely - 
  this is caused by AWS' cfn-init failing, which seems to just happen sometimes.  Run
  `yum install java-1.8.0-openjdk`, followed by `sudo update-alternatives --config java`
  and select Java 8.

### Check it's working

Go to `AWS` -> `Services` -> `SQS` -> `<Example Queue Name>`

Messages available should be > 0 and increasing, you can also inspect the messages the 
emitter is sending out.

# Development

To run locally without connecting to any AWS services (eg. for debugging), set 
`skipAws = true` in `defaults.conf` and then run:
        
        mvn compile exec:java -Dexec.args="<profile>"
 
where `<profile>` is `PART1` or `PART2`.   

# Command line arguments and configuration

If the command line argument is `PART1`, the config will be read from 
`src/main/resources/part1.conf`, similarly for `PART2`.  The values in these files override 
those in the fallback file `src/main/resources/defaults.conf`.

While the emitter is relatively configurable, you shouldn't need to change any config 
settings other than `topicArn` and `s3Bucket`.

# Architecture

On startup the application randomly generates a set of locations and some trends which the 
data will follow.  The locations are exported to S3 and the trends are printed to the console.  

Then the application enters the main event loop, which polls the `emitter` for new events, 
passes it through the `pipeline`, and into the `consumer` which sends the event to an SNS 
topic.

    DataSource 
        |
        v
    Emitter -> Pipeline -> Consumer

`DataSources` have the job of providing realistic looking data to the emitter, containing 
the necessary trends and some random noise, to the emitter.  

The pipeline is a series of distinct components such as delay, duplication, etc, which 
events are passed through in turn.
 