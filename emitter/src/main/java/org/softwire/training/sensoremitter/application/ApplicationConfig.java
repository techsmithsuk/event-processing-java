package org.softwire.training.sensoremitter.application;

import com.typesafe.config.Config;

import java.time.Duration;
import java.time.Instant;

public class ApplicationConfig {

    public final int universeWidth;

    public final int xIncrements;
    public final int yIncrements;
    public final int timeIncrements;

    public final int meanMillisToNextEvent;
    public final int stdDeviationMillisToNextEvent;
    public final int minimumMillisToNextEvent;
    public final int numValidLocations;
    public final int numUnknownLocations;


    public final Duration duration;
    public final Instant beginningOfTime;
    public final Instant endOfTime;

    public final double duplicationProbability;
    public final double delayProbability;
    public final Duration delayMax;

    public final String topicArn;

    public final String s3Bucket;
    public final String s3Key;

    public final double jsonDestroyerProbability;
    public final int threadCount;

    public final boolean skipAws;

    public final double clockSkewProbability;
    public final Duration clockSkewMax;

    public ApplicationConfig(Config config) {
        duration = config.getDuration("duration");
        beginningOfTime = Instant.now();
        endOfTime = beginningOfTime.plus(config.getDuration("duration"));

        universeWidth = config.getInt("universeWidth");

        timeIncrements = config.getInt("timeIncrements");
        xIncrements = config.getInt("xIncrements");
        yIncrements = config.getInt("yIncrements");

        meanMillisToNextEvent = config.getInt("meanMillisToNextEvent");
        stdDeviationMillisToNextEvent = config.getInt("stdDeviationMillisToNextEvent");
        minimumMillisToNextEvent = config.getInt("minimumMillisToNextEvent");
        numValidLocations = config.getInt("numValidLocations");
        numUnknownLocations = config.getInt("numUnknownLocations");


        duplicationProbability = config.getDouble("duplicationProbability");
        delayProbability = config.getDouble("delayProbability");
        delayMax = config.getDuration("delayMax");

        topicArn = config.getString("topicArn");
        threadCount = config.getInt("threadCount");

        s3Bucket = config.getString("s3Bucket");
        s3Key = config.getString("s3Key");

        jsonDestroyerProbability = config.getDouble("jsonDestroyerProbability");

        skipAws = config.getBoolean("skipAws");

        clockSkewProbability = config.getDouble("clockSkewProbability");
        clockSkewMax = config.getDuration("clockSkewMax");
    }
}
