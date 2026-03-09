package net.engineerAnsh.BankApplication.Repository;

import jakarta.persistence.LockModeType;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Enum.OutboxStatus;
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
            WHERE e.status = :status
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findPendingEventsForUpdate(OutboxStatus status, Pageable pageable);

    // Without @Modifying, Spring would treat it as a select query...
    //
    @Modifying(clearAutomatically = true)
    @Query("""
            DELETE FROM OutboxEvent e
            WHERE e.status = :status
            AND e.processedAt < :cutoff
            """)
    int deleteProcessedEvents( // Return value: number of rows deleted...
            @Param("status") OutboxStatus status,
            @Param("cutoff") LocalDateTime cutoff);

}

// Before improvement in this query():
// 3 schedulers
// 1 event
// → event published 3 times
