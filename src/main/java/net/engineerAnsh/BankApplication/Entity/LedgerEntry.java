package net.engineerAnsh.BankApplication.Entity;

import jakarta.persistence.*;
import lombok.*;
import net.engineerAnsh.BankApplication.Enum.ledgerEntry.LedgerEntryType;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries",
        indexes = { // Adds indexes for faster queries
                // account_id index, Faster account history lookups (eg: SELECT * FROM ledger_entries WHERE account_id = 101);
                @Index(name = "idx_ledger_account", columnList = "account_id"),
                // transaction_reference index, Faster transaction-based lookups (eg: SELECT * FROM ledger_entries WHERE transaction_reference = 'TXN123');
                @Index(name = "idx_ledger_txn_ref", columnList = "transaction_reference")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which account this entry belongs to...
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // Business transaction reference...
    @Column(name = "transaction_reference", nullable = false, length = 50)
    private String transactionReference;

    // Amount involved...
    @Column(nullable = false,
            precision = 19, // precision = digits_before_decimal + digits_after_decimal
            scale = 2) // This means: Exactly 2 digits after decimal
    private BigDecimal amount;

    // CREDIT or DEBIT...
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private LedgerEntryType entryType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 255)
    private String description;
}
