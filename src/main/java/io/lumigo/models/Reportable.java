package io.lumigo.models;

import io.lumigo.core.utils.SecretScrubber;

public interface Reportable {
    public Reportable scrub(SecretScrubber scrubber);

    public Reportable reduceSize(int maxFieldSize);
}
