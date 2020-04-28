package io.lumigo.core.parsers.event;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SnsEventParser implements IEventParser<SNSEvent> {

    @Override
    public Object parse(SNSEvent event) {
        List<SnsEvent.Record> records = new LinkedList<>();
        for (SNSEvent.SNSRecord rec : event.getRecords()) {
            Map<String, SnsEvent.MessageAttribute> attributes = new HashMap<>();
            for (Map.Entry<String, SNSEvent.MessageAttribute> attributeEntry :
                    rec.getSNS().getMessageAttributes().entrySet()) {
                attributes.put(
                        attributeEntry.getKey(),
                        SnsEvent.MessageAttribute.builder()
                                .type(attributeEntry.getValue().getType())
                                .value(attributeEntry.getValue().getValue())
                                .build());
            }
            records.add(
                    SnsEvent.Record.builder()
                            .message(rec.getSNS().getMessage())
                            .messageAttributes(attributes)
                            .messageId(rec.getSNS().getMessageId())
                            .build());
        }
        return SnsEvent.builder().records(records).build();
    }
}
