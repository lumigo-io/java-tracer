package com.lumigo.core.utils;

import org.pmw.tinylog.Logger;

import java.util.concurrent.TimeUnit;

public class TimeMeasure implements AutoCloseable {

    private long time;
    private String message;

    public TimeMeasure(String message) {
        this.message = message;
        this.time = System.nanoTime();
    }

    @Override
    public void close() {
        Logger.debug("Took {} milliseconds to run {}", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - this.time), message);
    }
}
