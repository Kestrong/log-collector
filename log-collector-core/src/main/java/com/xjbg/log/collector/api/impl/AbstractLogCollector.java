package com.xjbg.log.collector.api.impl;

import com.xjbg.log.collector.LogCollectorConstant;
import com.xjbg.log.collector.LogCollectors;
import com.xjbg.log.collector.api.LogCollector;
import com.xjbg.log.collector.channel.Channel;
import com.xjbg.log.collector.channel.MemoryChannel;
import com.xjbg.log.collector.enums.RejectPolicy;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.transformer.DefaultLogTransformer;
import com.xjbg.log.collector.transformer.LogTransformer;
import com.xjbg.log.collector.utils.LogContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author kesc
 * @since 2023-03-30 10:31
 */
@SuppressWarnings(value = {"unused", "unchecked", "rawtypes"})
public abstract class AbstractLogCollector<T extends LogInfo, R> implements LogCollector<T> {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private String fallbackCollector;
    private String nextCollector;
    private Channel<T> channel;
    private LogTransformer<T, R> logTransformer;
    private RejectPolicy rejectPolicy = RejectPolicy.NOOP;
    private int batchSize = 1;
    protected final List<T> buffer = new ArrayList<>();
    protected Thread scheduleThread;
    private int poolSize = 0;
    protected ThreadPoolExecutor pool;
    protected volatile boolean start = false;
    protected volatile boolean scheduleThreadToStop = false;

    public AbstractLogCollector() {
        init();
    }

    protected void init() {
        channel = new MemoryChannel<>();
        logTransformer = new DefaultLogTransformer<>();
    }

    protected abstract void doLog(List<R> logInfos) throws Exception;

    protected R doTransform(T logInfo) {
        return getLogTransformer().transform(logInfo);
    }

    protected T completeLogInfo(T logInfo) {
        if (StringUtils.isBlank(logInfo.getLogId())) {
            logInfo.setLogId(UUID.randomUUID().toString());
        }
        if (StringUtils.isBlank(logInfo.getRequestId())) {
            logInfo.setRequestId(LogContextHolder.getContext().flatMap(x -> Optional.ofNullable(x.getRequestId())).orElse(null));
        }
        if (StringUtils.isBlank(logInfo.getUserId())) {
            logInfo.setUserId(LogContextHolder.getContext().flatMap(x -> Optional.ofNullable(x.getUserId())).orElse(null));
        }
        if (StringUtils.isBlank(logInfo.getTenantId())) {
            logInfo.setTenantId(LogContextHolder.getContext().flatMap(x -> Optional.ofNullable(x.getTenantId())).orElse(null));
        }
        if (StringUtils.isBlank(logInfo.getApplication())) {
            logInfo.setApplication(LogCollectorConstant.APPLICATION);
        }
        logInfo.setCreateTime(new Date());
        return logInfo;
    }

    @Override
    public boolean log(T logInfo) {
        try {
            completeLogInfo(logInfo);
            doLog(Collections.singletonList(doTransform(logInfo)));
            return true;
        } catch (Exception e) {
            log.warn("log occur error, the reason maybe: {}", e.getMessage());
            logAsyncFallback(logInfo);
            return false;
        } finally {
            if (getNextCollector() != null) {
                getNextCollector().log(logInfo);
            }
        }
    }

    @Override
    public boolean logBatch(List<T> logInfos) {
        List<R> transformLogs = new ArrayList<>();
        for (T logInfo : logInfos) {
            try {
                transformLogs.add(doTransform(completeLogInfo(logInfo)));
            } catch (Exception e) {
                log.warn("log transform error, the reason maybe: {}", e.getMessage());
                logAsyncFallback(logInfo);
                logInfos.remove(logInfo);
            }
        }
        try {
            doLog(transformLogs);
            return true;
        } catch (Exception e) {
            log.warn("log batch occur error, the reason maybe: {}", e.getMessage());
            logInfos.forEach(this::logAsyncFallback);
            return false;
        } finally {
            if (getNextCollector() != null) {
                getNextCollector().logBatch(logInfos);
            }
        }
    }

    protected void logAsyncFallback(T logInfo) {
        if (getFallbackCollector() != null) {
            try {
                getFallbackCollector().logAsync(logInfo);
            } catch (Exception e) {
                log.debug("log fallback fail", e);
            }
        }
    }

    protected boolean isExceedThreadHold() {
        return channel.size() >= channel.getCapacity() * channel.getThreshold();
    }

    @Override
    public void logAsync(T logInfo) {
        if (isExceedThreadHold()) {
            RejectPolicy rejectPolicy = getRejectPolicy();
            if (RejectPolicy.CALLER_RUNS.equals(rejectPolicy)) {
                log(logInfo);
                return;
            }
            if (RejectPolicy.DISCARD.equals(rejectPolicy)) {
                return;
            }
            if (RejectPolicy.FALLBACK.equals(rejectPolicy)) {
                logAsyncFallback(logInfo);
                return;
            }
            if (RejectPolicy.DISCARD_OLDEST.equals(rejectPolicy)) {
                try {
                    T t = channel.pull();
                } catch (Exception e) {
                    //ignore
                }
                logAsync(logInfo);
                return;
            }
        }
        try {
            completeLogInfo(logInfo);
            channel.push(logInfo);
        } catch (Exception e) {
            log.warn("log async occur error, the reason maybe: {}", e.getMessage());
            logAsyncFallback(logInfo);
        }
    }

    @Override
    public void cleanLog(Date before) throws Exception {

    }

    @Override
    public void start() {
        if (start) {
            return;
        }
        synchronized (this) {
            if (start) {
                return;
            }
            if (getPoolSize() > 0) {
                pool = new ThreadPoolExecutor(getPoolSize(), getPoolSize(), 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
            }
            scheduleThreadToStop = false;
            scheduleThread = new Thread(() -> {
                try {
                    TimeUnit.SECONDS.sleep(3L);
                } catch (Exception e) {
                    //ignore
                }
                log.info("{} collector start collecting log.", type());
                LOOP:
                while (!scheduleThreadToStop) {
                    try {
                        while (channel.isEmpty()) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(1000L);
                                if (scheduleThreadToStop) {
                                    break LOOP;
                                }
                            } catch (Exception e) {
                                //ignore
                            }
                        }
                        if (buffer.size() < getBatchSize()) {
                            buffer.add(channel.pull());
                        }
                        if (buffer.size() >= getBatchSize()) {
                            List<T> temp = new ArrayList<>(buffer);
                            if (pool != null) {
                                pool.execute(() -> logBatch(temp));
                            } else {
                                logBatch(temp);
                            }
                            buffer.clear();
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
                if (buffer.size() > 0) {
                    logBatch(new ArrayList<>(buffer));
                    buffer.clear();
                }
                log.info("{} collector stop collecting log.", type());
            });
            scheduleThread.setDaemon(true);
            scheduleThread.setName(String.format("%s collector scheduleThread", type()));
            scheduleThread.start();
            start = true;
        }
    }

    @Override
    public void stop() {
        if (!start) {
            return;
        }
        synchronized (this) {
            if (!start) {
                return;
            }
            scheduleThreadToStop = true;
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
            if (scheduleThread.getState() != Thread.State.TERMINATED) {
                // interrupt and wait
                scheduleThread.interrupt();
                try {
                    scheduleThread.join();
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }
            scheduleThread = null;
            if (pool != null) {
                try {
                    pool.shutdown();
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
                pool = null;
            }
            try {
                getChannel().clear();
            } catch (Exception e) {
                //ignore
            }
            start = false;
        }
    }

    public Channel<T> getChannel() {
        return channel;
    }

    public void setChannel(Channel<T> channel) {
        this.channel = channel;
    }

    public LogTransformer<T, R> getLogTransformer() {
        return logTransformer;
    }

    public void setLogTransformer(LogTransformer<T, R> logTransformer) {
        this.logTransformer = logTransformer;
    }

    public RejectPolicy getRejectPolicy() {
        return rejectPolicy;
    }

    public void setRejectPolicy(RejectPolicy rejectPolicy) {
        this.rejectPolicy = rejectPolicy;
    }

    public LogCollector getFallbackCollector() {
        if (StringUtils.isBlank(fallbackCollector)) {
            return null;
        }
        return LogCollectors.getCollector(fallbackCollector);
    }

    @Override
    public void setFallbackCollector(String fallbackCollector) {
        this.fallbackCollector = fallbackCollector;
    }

    public LogCollector getNextCollector() {
        if (StringUtils.isBlank(nextCollector)) {
            return null;
        }
        return LogCollectors.getCollector(nextCollector);
    }

    public void setNextCollector(String nextCollector) {
        this.nextCollector = nextCollector;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public static abstract class None implements LogCollector<LogInfo> {

        private None() {
        } // not to be instantiated

    }

}
