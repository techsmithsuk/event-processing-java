package org.softwire.training.analyzer.application;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;

public class AwsClientFactory {

    private static final ClasspathPropertiesFileCredentialsProvider CREDENTIALS_PROVIDER =
            new ClasspathPropertiesFileCredentialsProvider();
    private static final Regions REGION = Regions.EU_WEST_1;

    public AmazonS3 s3() {
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(CREDENTIALS_PROVIDER)
                .withRegion(REGION)
                .build();
    }

    public AmazonSQS sqs() {
        return AmazonSQSAsyncClientBuilder
                .standard()
                .withCredentials(CREDENTIALS_PROVIDER)
                    .withRegion(REGION)
                .build();
    }

    public AmazonSNS sns() {
        return AmazonSNSClientBuilder
                .standard()
                .withCredentials(CREDENTIALS_PROVIDER)
                .withRegion(REGION)
                .build();
    }
}
