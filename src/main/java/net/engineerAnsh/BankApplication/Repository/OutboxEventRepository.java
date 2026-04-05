package net.engineerAnsh.BankApplication.Repository;

import jakarta.persistence.LockModeType;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Enum.outbox.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // This prevents: Duplicate processing, Scheduler collisions, Race conditions
    // Fetch the oldest unprocessed events (batch processing)...
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.status IN :statuses
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findPendingEventsForUpdate(
            @Param("statuses") List<OutboxStatus> statuses,
            Pageable pageable
    );

    // Without @Modifying, Spring would treat it as a select query...
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM OutboxEvent e
            WHERE e.status IN :statuses
            AND e.processedAt < :cutoff
            """)
    int deleteProcessedEvents(
            @Param("statuses") List<OutboxStatus> statuses,
            @Param("cutoff") LocalDateTime cutoff
    );

}

// Before improvement in this query():
// 3 schedulers
// 1 event
// → event published 3 times
