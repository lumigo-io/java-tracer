package com.lumigo.core.utils;

import org.pmw.tinylog.Logger;

public class TimeMeasure implements AutoCloseable {

    private long time;
    private String message;

    public TimeMeasure(String message) {
        this.message = message;
        this.time = System.nanoTime();
    }

    @Override
    public void close() {
        Logger.debug("Took {} nano seconds to run {}", (System.nanoTime() - this.time), message);
    }
}
