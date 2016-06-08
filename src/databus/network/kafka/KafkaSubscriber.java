package databus.network.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;


public class KafkaSubscriber extends AbstractKafkaSubscriber {    

    public KafkaSubscriber() {
        this(null, 1);
    }

    public KafkaSubscriber(ExecutorService executor, int pollingThreadNumber) {
        super(executor, pollingThreadNumber);
    }

    @Override
    public void initialize(Properties properties) {
        super.initialize(properties);
        String saveThresholdValue = properties.getProperty("kafka.recordSaveThreshold");
        if (null != saveThresholdValue) {
            saveThreshold = Integer.parseUnsignedInt(saveThresholdValue);
        }
        if (saveThreshold < 1) {
            saveThreshold = 1;
        }
        positionCounters = new ConcurrentHashMap<String, AtomicInteger>();
        positionCache = new PositionsCache();
    }    

    @Override
    protected void initializePerThread() {
        super.initializePerThread();
        KafkaConsumer<Long, String> consumer = consumers.get(Thread.currentThread().getId());
        for(TopicPartition partition : consumer.assignment()) {
            long position = positionCache.get(partition.topic(), partition.partition());
            if (position >= 0) {
                consumer.seek(partition, position+1);
            } else if (doesSeekFromBeginning) {
                consumer.seekToBeginning(partition);;
            }
        }
    }

    /**
     * thread-safe method
     */
    @Override
    protected void cachePosition(String topic, int partition, long position) {
        positionCache.set(topic, partition, position);
        AtomicInteger counter = positionCounters.get(topic);
        if (null == counter) {
            counter = new AtomicInteger(1);
            positionCounters.put(topic, counter);
        }
        int currentCount = counter.incrementAndGet();
        while((currentCount=counter.get()) >= saveThreshold) {
            if (counter.compareAndSet(currentCount, currentCount-saveThreshold)) {
                positionCache.save(topic);
                break;
            }
        }        
    }
    
    private Map<String,AtomicInteger> positionCounters;
    private PositionsCache positionCache;    
    private int saveThreshold = 1;   
}
