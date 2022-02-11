package com.jonas.util.schecule;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncPool extends ThreadPoolExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncPool.class);

    public AsyncPool(int size) {
        super(size, Integer.MAX_VALUE, 60, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
        this.setThreadFactory(runnable -> new Thread(runnable) {{
            setName(String.format("AsyncPool#%s", getPoolSize()));
        }});
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        if (throwable != null) {
            logger.error("Exception in AsyncPool task", throwable);
        }
    }

}
