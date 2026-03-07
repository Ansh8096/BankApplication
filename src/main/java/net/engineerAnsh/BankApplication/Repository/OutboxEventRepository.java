package net.engineerAnsh.BankApplication.Repository;

import jakarta.persistence.LockModeType;
import net.engineerAnsh.BankApplication.Entity.OutboxEvent;
import net.engineerAnsh.BankApplication.Enum.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
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

}

// Before improvement in this query():
// 3 schedulers
// 1 event
// → event published 3 times
