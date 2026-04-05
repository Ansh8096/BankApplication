package net.engineerAnsh.BankApplication.Kafka.EventHandlers;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Kafka.Event.EventProcessedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.FraudDetectedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionCompletedEvent;
import net.engineerAnsh.BankApplication.Kafka.Event.TransactionEvent;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TransactionEventHandlerRegistry {

    private final FraudDetectedEventHandler fraudHandler;
    private final TransactionCompletedEventHandler completedHandler;
    private final EventProcessedHandler eventProcessedHandler;
    private final Map<Class<?>, TransactionEventHandler<?>> handlers = new HashMap<>(); // It stores: <Event class,Handler>

    @PostConstruct // Runs once after Spring creates the bean, It registers all handlers in the map...
    public void init() {
        handlers.put(FraudDetectedEvent.class, fraudHandler);
        handlers.put(TransactionCompletedEvent.class, completedHandler);
        handlers.put(EventProcessedEvent.class, eventProcessedHandler);
    }

    // we use @SuppressWarnings, Because the compiler says: "Hey, this cast might be unsafe", but I'm telling: “Trust me, I know what I’m doing.”
    @SuppressWarnings("unchecked")
    public <T extends TransactionEvent> TransactionEventHandler<T> getHandler(Class<?> clazz) { // We pass an event class like: FraudDetectedEvent.class, it returns 'FraudDetectedEventHandler'...

        // we are casting here, Because Java loses generic type info at runtime (type erasure).
        // So Java can't guarantee type safety → we manually cast.
        return (TransactionEventHandler<T>) handlers.get(clazz);

    }

}