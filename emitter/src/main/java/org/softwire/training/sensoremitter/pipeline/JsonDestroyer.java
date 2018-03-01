package org.softwire.training.sensoremitter.pipeline;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.application.ApplicationConfig;

import java.time.Instant;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Replace random characters with something which will never be valid (looking at the characters allowed in the event
 * type) - the higher planes of UTF-8 should do it.
 */
public class JsonDestroyer implements Pipeline<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(JsonDestroyer.class);

    private static final int MIN_CODE_POINT = 0x80;
    private static final int MAX_CODE_POINT = 0xFFFF;
    private final double probability;
    private final Random random;

    @Inject
    JsonDestroyer(ApplicationConfig config,
                  Random random) {
        this.probability = config.jsonDestroyerProbability;
        this.random = random;
    }

    private String replaceRandomChar(String original) {
        String badCharacter = String.valueOf(Character.toChars(random.nextInt(MAX_CODE_POINT - MIN_CODE_POINT)));
        int location = random.nextInt(original.length() - 1);
        return original.substring(0, location) + badCharacter + original.substring(location + 1);
    }

    @Override
    public Stream<String> handle(Instant now, String json) {
        if (random.nextDouble() < probability) {
            String destroyedJson = replaceRandomChar(json);
            LOG.info("Destroyed JSON: {}", destroyedJson);
            return Stream.of(destroyedJson);
        } else {
            return Stream.of(json);
        }
    }
}
