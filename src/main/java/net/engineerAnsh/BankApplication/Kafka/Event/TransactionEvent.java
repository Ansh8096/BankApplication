package net.engineerAnsh.BankApplication.Kafka.Event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


// When JSON comes in, Jackson needs to know: Which concrete class should I create?
// @JsonTypeInfo, This tells Jackson: How to identify the type, where to find it, field name
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME, // Use a string name (not class name) to identify the type.
        include = JsonTypeInfo.As.PROPERTY, // The type info will be inside the JSON as a field.
        property = "eventType", // JSON must contain a field like 'eventType'.
        visible = true // Makes "eventType" available as a normal field in your class as well, Without this, Jackson uses it internally but doesn’t bind it to your object.
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TransactionCompletedEvent.class, name = "TRANSACTION_COMPLETED"),
        @JsonSubTypes.Type(value = EventProcessedEvent.class, name = "EVENT_PROCESSED"),
        @JsonSubTypes.Type(value = FraudDetectedEvent.class, name = "FRAUD_DETECTED")
})
public interface TransactionEvent {
}


// Flow Summary :-
//  JSON comes in
//  Jackson reads eventType
//  Finds matching subtype
//  Creates correct class instance
//  Populates fields