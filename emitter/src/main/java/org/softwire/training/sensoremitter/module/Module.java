package org.softwire.training.sensoremitter.module;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.google.inject.Provides;
import org.softwire.training.sensoremitter.application.ApplicationConfig;
import org.softwire.training.sensoremitter.consumer.Consumer;
import org.softwire.training.sensoremitter.consumer.SnsTopic;
import org.softwire.training.sensoremitter.exporter.Exporter;
import org.softwire.training.sensoremitter.exporter.S3Exporter;

public class Module extends BaseModule {
    private static final Regions AWS_REGION = Regions.EU_WEST_1;

    public Module(ApplicationConfig config) {
        super(config);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(Consumer.class).to(SnsTopic.class);
        bind(Exporter.class).to(S3Exporter.class);
    }

    @Provides
    AmazonSNS sns() {
        return AmazonSNSClientBuilder
                .standard()
                .withCredentials(InstanceProfileCredentialsProvider.getInstance())
                .withRegion(AWS_REGION)
                .build();
    }

    @Provides
    AmazonSQS sqs() {
        return AmazonSQSClientBuilder
                .standard()
                .withCredentials(InstanceProfileCredentialsProvider.getInstance())
                .withRegion(AWS_REGION)
                .build();
    }

    @Provides
    AmazonS3 s3() {
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(InstanceProfileCredentialsProvider.getInstance())
                .withRegion(AWS_REGION)
                .build();
    }
}