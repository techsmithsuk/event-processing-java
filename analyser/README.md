Event Processing - Example Client
=================================

# Setup

* Update S3 and SNS locations in `src/main/resources/application.conf`.

* Add AWS credentials in `src/main/resources/AwsCredentials.properties`, in the form:

      accessKey=<access key>
      secretKey=<secret key>

# Run

```
mvn compile exec:java
```

# Test

```
mvn test
```