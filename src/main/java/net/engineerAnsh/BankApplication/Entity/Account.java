package net.engineerAnsh.BankApplication.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import net.engineerAnsh.BankApplication.Enum.AccountStatus;
import net.engineerAnsh.BankApplication.Enum.AccountType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number",unique = true,nullable = false,length = 20)
    private String accountNumber;

    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;

    @Column(name = "balance",nullable = false,
            precision = 19, // precision = digits_before_decimal + digits_after_decimal
            scale = 2) // This means: Exactly 2 digits after decimal
    private BigDecimal accountBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type",nullable = false)
    private AccountType accountType; // child or bank

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status",nullable = false)
    private AccountStatus accountStatus; // Active or Block

    // “We don’t define setters for createdAt and updatedAt.
    // Hibernate automatically manages them using @CreationTimestamp and @UpdateTimestamp.”
    @CreationTimestamp
    @Column(name = "account_created_at", updatable = false, nullable = false)
    private LocalDateTime accountCreatedAt;

    @UpdateTimestamp
    @Column(name = "account_updated_at")
    private LocalDateTime accountUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY) // FetchType.LAZY = “Load only when needed”,In this the Hibernate will : Load Account data first , then Load User data only when accessed...
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // it tells Spring Boot: " Do NOT include this field when converting this object to JSON ”
    private User user;

    @Column(name = "account_closed_at")
    private LocalDateTime accountClosedAt;

    @PrePersist // It tells Hibernate: “ Run this method automatically just BEFORE this entity is saved for the first time in the database.
    // To use '@PrePersist' a method must not take any param,also not return anything , should be inside the entity only...
    public void prePersist(){
        if(accountStatus == null){
            accountStatus = AccountStatus.ACTIVE;
        }
        if(accountBalance == null){
            accountBalance = BigDecimal.ZERO;
        }
        if(ifscCode == null || ifscCode.isEmpty()){
            ifscCode = "BOAN0000001";
        }
    }

}