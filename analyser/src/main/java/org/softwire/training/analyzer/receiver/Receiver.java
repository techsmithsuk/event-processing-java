package org.softwire.training.analyzer.receiver;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.analyzer.model.Event;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * This class wraps all the threaded aspects of the receiver.
 * <p>
 * We spin up {@link TypedConfig#numThreads} polling loops, each of which uses {@link SqsEventRetriever#get()} to
 * retrieve events from SQS and puts them into the outboundQueue.  If any thread encounters an error then it's logged
 * and fatalErrorInThread is set, and then next call to {@link #get()} will throw.
 * <p>
 * It's safe to use a single instance of {@link SqsEventRetriever} as it's thread-safe.
 * <p>
 * Instead of managing the threads ourselves, an alternative solution would be to use some kinda of ExecutorService, as
 * we do in the SNS publisher in the emitter.
 */
public class Receiver implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Receiver.class);

    private final AtomicReference<Throwable> fatalErrorInThread = new AtomicReference<>();
    private final AtomicBoolean threadShutdownRequested = new AtomicBoolean(false);
    private final LinkedBlockingQueue<Event> outboundQueue = new LinkedBlockingQueue<>();

    private final SqsEventRetriever sqsEventRetriever;
    private final List<Thread> threads;

    public Receiver(SqsEventRetriever sqsEventRetriever,
                    Receiver.TypedConfig config) {
        this.sqsEventRetriever = sqsEventRetriever;

        threads = IntStream.range(0, config.numThreads).mapToObj(i -> {
            Thread thread = new Thread(this::runPollingLoop, "Receiver Thread " + i);
            thread.start();
            return thread;
        }).collect(Collectors.toList());
    }

    @Override
    public void close() throws InterruptedException {
        LOG.info("Shutting down worker threads");
        threads.forEach(Thread::interrupt);
        for (Thread thread : threads) {
            thread.join();
        }
    }

    public Stream<Event> get() throws InterruptedException {
        Event event = outboundQueue.poll(1, TimeUnit.SECONDS);
        if (fatalErrorInThread.get() != null) {
            throw new FatalErrorInWorkerThreadException(fatalErrorInThread.get());
        }
        return event == null ? Stream.empty() : Stream.of(event);
    }

    private void runPollingLoop() {
        try {
            while (true) {
                sqsEventRetriever.get().forEach(outboundQueue::add);
                if (threadShutdownRequested.get()) {
                    break;
                }
            }
        } catch (Throwable t) {
            // If fatalErrorInThread is unset, set it.  If it is already set then don't worry about it.
            fatalErrorInThread.compareAndSet(null, t);
            LOG.error("Thread shutdown unexpectedly", t);
        }
    }

    class FatalErrorInWorkerThreadException extends RuntimeException {
        FatalErrorInWorkerThreadException(Throwable cause) {
            super(cause);
        }
    }

    public static class TypedConfig {
        final String topicArn;
        final int numThreads;

        public TypedConfig(String topicArn, int numThreads) {
            this.topicArn = topicArn;
            this.numThreads = numThreads;
        }

        public static TypedConfig fromUntypedConfig(Config typesafeConfig) {
            return new TypedConfig(
                    typesafeConfig.getString("snsTopicArn"),
                    typesafeConfig.getInt("numThreads"));
        }
    }
}
