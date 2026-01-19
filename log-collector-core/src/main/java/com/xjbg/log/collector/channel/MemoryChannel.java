package com.xjbg.log.collector.channel;

import com.xjbg.log.collector.model.LogInfo;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author kesc
 * @since 2023-03-30 10:30
 */
public class MemoryChannel<T extends LogInfo> extends Channel<T> {

    private final LinkedBlockingQueue<T> queue;

    public MemoryChannel() {
        super();
        this.queue = new LinkedBlockingQueue<>(this.getCapacity());
    }

    public MemoryChannel(int capacity, int byteCapacity, long byteSpeed, long recordSpeed, long flowControlInterval, float threshold) {
        super(capacity, byteCapacity, byteSpeed, recordSpeed, flowControlInterval, threshold);
        this.queue = new LinkedBlockingQueue<>(this.getCapacity());
    }

    @Override
    public void clear() {
        this.queue.clear();
    }

    @Override
    protected void doOffer(T r) throws Exception {
        if (!this.queue.offer(r)) {
            throw new InterruptedException("Failed to push record because of queue is full");
        }
    }

    @Override
    protected T doPoll() throws Exception {
        return this.queue.poll(200L, TimeUnit.MILLISECONDS);
    }

    @Override
    public int size() {
        return this.queue.size();
    }

    @Override
    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

}
