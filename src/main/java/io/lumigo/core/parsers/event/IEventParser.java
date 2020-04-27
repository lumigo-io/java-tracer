package io.lumigo.core.parsers.event;

public interface IEventParser<T> {
    Object parse(T event);
}
