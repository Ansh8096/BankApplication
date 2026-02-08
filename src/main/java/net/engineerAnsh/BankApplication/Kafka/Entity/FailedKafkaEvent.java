package net.engineerAnsh.BankApplication.Kafka.Entity;

import jakarta.persistence.*;
import lombok.*;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Table(name = "failed_kafka_event")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedKafkaEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", nullable = false)
    private String transactionReference;

    // Stores the Kafka topic where the event came from...
    @Column(nullable = false)
    private String topic;

    // Stores the entire original Kafka message...
    @Lob // @Lob = Large Object (used for Long json)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR) // Tells Hibernate which SQL type to use, 'LONGVARCHAR' id used for 'JSON payloads'...
    @Column(nullable = false)
    private String payload;

    // It stores the reason for failure...
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FailedEventStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
