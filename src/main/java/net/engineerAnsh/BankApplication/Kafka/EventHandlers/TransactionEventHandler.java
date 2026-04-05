package net.engineerAnsh.BankApplication.Kafka.EventHandlers;

import net.engineerAnsh.BankApplication.Kafka.Event.TransactionEvent;

public interface TransactionEventHandler<T extends TransactionEvent> {
    void handle(T event);
}


// This interface represents:--
// It defines a handler (processor) for events.
// “Given an event of type T, I know how to handle it.”