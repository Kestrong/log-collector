package com.xjbg.log.collector.channel;

import com.xjbg.log.collector.model.LogInfo;

import java.util.concurrent.LinkedBlockingQueue;

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

    public MemoryChannel(int capacity, int byteCapacity, long byteSpeed, long recordSpeed, long flowControlInterval) {
        super(capacity, byteCapacity, byteSpeed, recordSpeed, flowControlInterval);
        this.queue = new LinkedBlockingQueue<>(this.getCapacity());
    }

    @Override
    public void clear() {
        this.queue.clear();
    }

    @Override
    protected void doPush(T r) throws Exception {
        this.queue.put(r);
    }

    @Override
    protected T doPull() throws Exception {
        return this.queue.take();
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
