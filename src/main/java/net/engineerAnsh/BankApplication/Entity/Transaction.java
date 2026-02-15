package net.engineerAnsh.BankApplication.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import net.engineerAnsh.BankApplication.Enum.TransactionStatus;
import net.engineerAnsh.BankApplication.Enum.TransactionType;
import net.engineerAnsh.BankApplication.Utils.TransactionReferenceGenerator;
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

    @Column(
            name = "transaction_reference",
            updatable = false,
            nullable = false,
            unique = true,
            length = 30
    )
    private String transactionReference;

    @Column(name = "client_transaction_id", unique = true, length = 100)
    private String clientTransactionId;

    // Why '@JoinColumn' used here:-
    // @JoinColumn is for relationships (foreign keys), (means we should use this, because 'fromAccount' is a reference to another entity)...
    // Since 'fromAccount' is an object, not a simple value, we must use @JoinColumn...
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_number",
            referencedColumnName = "account_number"
    )
    @JsonIgnore
    private Account fromAccount;

    // Why @JoinColumn is CORRECT ?
    // This tells JPA: “This field is a relationship. So Store the number of Account in column to_account_number”...
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_number",
            referencedColumnName = "account_number"
    )
    @JsonIgnore
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

    // If we want the transactionReference early we use this:
    // If not then this method is fine
    @PrePersist
    public void prePersist(){
        if(transactionReference == null || transactionReference.isEmpty()){
            transactionReference = TransactionReferenceGenerator.generate();
        }
    }

}
