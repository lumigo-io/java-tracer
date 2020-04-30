package io.lumigo.core.parsers.event;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SqsEventParser implements IEventParser<SQSEvent> {

    @Override
    public Object parse(SQSEvent event) {
        List<SqsEvent.Record> records = new LinkedList<>();
        for (SQSEvent.SQSMessage rec : event.getRecords()) {
            Map<String, SqsEvent.MessageAttribute> attributes = new HashMap<>();
            for (Map.Entry<String, SQSEvent.MessageAttribute> attributeEntry :
                    rec.getMessageAttributes().entrySet()) {
                attributes.put(
                        attributeEntry.getKey(),
                        SqsEvent.MessageAttribute.builder()
                                .type(attributeEntry.getValue().getDataType())
                                .value(attributeEntry.getValue().getStringValue())
                                .build());
            }
            records.add(
                    SqsEvent.Record.builder()
                            .body(rec.getBody())
                            .messageAttributes(attributes)
                            .messageId(rec.getMessageId())
                            .build());
        }
        return SqsEvent.builder().records(records).build();
    }
}
