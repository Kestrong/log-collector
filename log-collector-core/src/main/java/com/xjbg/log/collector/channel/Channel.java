package com.xjbg.log.collector.channel;

import com.xjbg.log.collector.model.LogInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.openjdk.jol.info.GraphStatsWalker;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author kesc
 * @since 2023-03-30 10:30
 */
@Slf4j
@Getter
@Setter
public abstract class Channel<T extends LogInfo> {
    protected static final Lock lock = new ReentrantLock();

    protected static final GraphStatsWalker walker = new GraphStatsWalker();

    protected volatile int capacity;

    protected volatile int byteCapacity;

    protected volatile long byteSpeed; // bps: bytes/s

    protected volatile long recordSpeed; // tps: records/s

    protected volatile long flowControlInterval;

    protected volatile float threshold;

    protected volatile AtomicLong totalBytes = new AtomicLong(0);

    protected volatile AtomicLong totalRecords = new AtomicLong(0);

    protected volatile long lastTimestamp = -1;

    public Channel() {
        this(10000, 8 * 1024 * 1024, 1024 * 1024, 1000, 3000, 0.8f);
    }

    public Channel(int capacity, int byteCapacity, long byteSpeed, long recordSpeed, long flowControlInterval, float threshold) {
        this.capacity = capacity;
        this.byteCapacity = byteCapacity;
        this.byteSpeed = byteSpeed;
        this.recordSpeed = recordSpeed;
        this.flowControlInterval = flowControlInterval;
        this.threshold = threshold;

        if (capacity <= 0) {
            throw new IllegalArgumentException(String.format(
                    "通道容量[%d]必须大于0.", capacity));
        }

        log.info("Channel set byte_speed_limit to " + byteSpeed
                + (byteSpeed <= 0 ? ", No bps activated." : "."));
        log.info("Channel set record_speed_limit to " + recordSpeed
                + (recordSpeed <= 0 ? ", No tps activated." : "."));

    }

    public void push(final T r) throws Exception {
        Validate.notNull(r, "record不能为空.");
        this.doPush(r);
    }

    public T pull() throws Exception {
        T record = this.doPull();
        this.statPull(1L, walker.walk(record).totalSize());
        return record;
    }

    protected abstract void doPush(T r) throws Exception;

    protected abstract T doPull() throws Exception;

    public abstract int size();

    public abstract boolean isEmpty();

    public abstract void clear();

    private void statPull(long recordSize, long byteSize) {

        boolean isChannelByteSpeedLimit = (this.byteSpeed > 0);
        boolean isChannelRecordSpeedLimit = (this.recordSpeed > 0);
        if (!isChannelByteSpeedLimit && !isChannelRecordSpeedLimit) {
            return;
        }
        try {
            lock.lockInterruptibly();
            totalRecords.addAndGet(recordSize);
            totalBytes.addAndGet(byteSize);
            long nowTimestamp = System.currentTimeMillis();
            long interval = nowTimestamp - lastTimestamp;
            if (interval - this.flowControlInterval >= 0) {
                long byteLimitSleepTime = 0;
                long recordLimitSleepTime = 0;
                if (isChannelByteSpeedLimit) {
                    long currentByteSpeed = totalBytes.get() * 1000 / interval;
                    if (currentByteSpeed > this.byteSpeed) {
                        // 计算根据byteLimit得到的休眠时间
                        byteLimitSleepTime = currentByteSpeed * interval / this.byteSpeed
                                - interval;
                    }
                }

                if (isChannelRecordSpeedLimit) {
                    long currentRecordSpeed = totalRecords.get() * 1000 / interval;
                    if (currentRecordSpeed > this.recordSpeed) {
                        // 计算根据recordLimit得到的休眠时间
                        recordLimitSleepTime = currentRecordSpeed * interval / this.recordSpeed
                                - interval;
                    }
                }

                // 休眠时间取较大值
                long sleepTime = Math.max(byteLimitSleepTime, recordLimitSleepTime);
                if (sleepTime > 0) {
                    try {
                        log.debug("totalBytes:{}, byteSpeed:{}, totalRecords:{}, recordSpeed:{}, sleep {} mills to limit speed.", totalBytes.get(), byteSpeed, totalRecords.get(), recordSpeed, sleepTime);
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                totalBytes.set(0);
                totalRecords.set(0);
                lastTimestamp = System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public void afterProperties(Map<String, String> properties) {

    }
}

