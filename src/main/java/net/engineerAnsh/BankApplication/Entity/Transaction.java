package net.engineerAnsh.BankApplication.Entity;

import jakarta.persistence.*;
import lombok.*;
import net.engineerAnsh.BankApplication.Enum.TransactionStatus;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    public Long id;

    // Why '@JoinColumn' used here:-
    // @JoinColumn is for relationships (foreign keys), (means we should use this, because 'toAccount' is a reference to another entity)...
    // Since 'toAccount' is an object, not a simple value, we must use @JoinColumn...
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    // Why @JoinColumn is CORRECT ?
    // This tells JPA: “This field is a relationship. So Store the ID of Account in column to_account_id”...
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "balance",nullable = false,
            precision = 19, // precision = digits_before_decimal + digits_after_decimal
            scale = 2) // This means: Exactly 2 digits after decimal
    private BigDecimal amount;

    @Column(length = 250)
    private String remark;



}
