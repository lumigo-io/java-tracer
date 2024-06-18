package io.lumigo.models;

import io.lumigo.core.utils.SecretScrubber;

public interface Reportable<T extends Reportable> {
    T scrub(SecretScrubber scrubber);

    T reduceSize(int maxFieldSize);
}
