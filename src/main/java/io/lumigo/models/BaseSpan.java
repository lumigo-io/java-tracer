package io.lumigo.models;

import io.lumigo.core.utils.SecretScrubber;

public interface BaseSpan {
    BaseSpan scrub(SecretScrubber scrubber);

    BaseSpan reduceSize(int maxFieldSize);
}
