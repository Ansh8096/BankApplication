package net.engineerAnsh.BankApplication.Kafka.Repository;

import net.engineerAnsh.BankApplication.Kafka.Entity.FailedKafkaEvent;
import net.engineerAnsh.BankApplication.Kafka.Enums.FailedEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FailedKafkaEventRepository extends JpaRepository<FailedKafkaEvent, Long> {

    List<FailedKafkaEvent> findByStatusNotIn(List<FailedEventStatus> statuses);

    Optional<FailedKafkaEvent> findByEventId(String eventId);

    @Modifying
    @Query("""
            DELETE FROM FailedKafkaEvent f
            WHERE f.status IN :statuses
            AND f.createdAt < :cutoff
            """)
    int deleteTerminalEvents(
            @Param("statuses") List<FailedEventStatus> statuses,
            @Param("cutoff") LocalDateTime cutoff
    );

}
