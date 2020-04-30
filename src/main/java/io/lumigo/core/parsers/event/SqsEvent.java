package io.lumigo.core.parsers.event;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder(toBuilder = true)
@Data(staticConstructor = "of")
public class SqsEvent {
    public List<Record> records;

    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class Record {
        public String body;
        public Map<String, MessageAttribute> messageAttributes;
        public String messageId;
    }

    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data(staticConstructor = "of")
    public static class MessageAttribute {
        public String type;
        public String value;
    }
}
