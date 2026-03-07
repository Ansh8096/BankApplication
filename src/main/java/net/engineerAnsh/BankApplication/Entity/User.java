package net.engineerAnsh.BankApplication.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // @GeneratedValue → MySQL AUTO_INCREMENT
    private Long userId;

    @Column(name = "name", nullable = false) // means can't store null values...
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    @Email
    private String email;

    @Column(name = "age", nullable = false)
    private Integer age; // age eligibility for loan.

    @Column(name = "phone_number", unique = true, nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private Boolean active = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // cascade = ALL → Auto save/update/delete
    // @OneToMany → User can have multiple types of account...`
    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, // FetchType.LAZY = “Load only when needed”,In this the Hibernate will : Load User data first , then Load Accounts only when required...
            orphanRemoval = true)   // orphanRemoval = true — Auto delete child , Meaning : If an account is removed from the list, it is deleted from DB...
    @JsonIgnore // It tells Spring Boot: " Do NOT include this field when converting this object to JSON ”
    private List<Account> accounts = new ArrayList<>(); // we are not creating the column for this because user can have multiple accounts...

    @Column(name = "kyc_status", nullable = false)
    private Boolean KycStatus;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(nullable = false)
    private boolean accountLocked = false;

    @Column(nullable = false)
    private int failedAttempts = 0; // if login tries multiple time, lock account...

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    // “We don’t define setters for createdAt and updatedAt.
    // Hibernate automatically manages them using @CreationTimestamp and @UpdateTimestamp.”
    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    // ManyToMany : One User can have many Roles, One Role can belong to many Users.
    @ManyToMany(fetch = FetchType.EAGER)
    // Eager means:  When you load a User, roles are loaded immediately (It is very important for the spring Security)
    @JoinTable( // Tells Hibernate: “Use a table named user_roles to connect users and roles.”
            name = "user_roles", // Hibernate creates a third table: This table links users <-> roles.
            joinColumns = @JoinColumn(name = "user_id"), // This column points to User , user_id is a foreign key to users.id (means: user_id → users.user_id)
            inverseJoinColumns = @JoinColumn(name = "role_id") // This column points to Role,  role_id is a foreign key to roles.id (means: role_id → roles.id)
    )
    private Set<Role> roles = new HashSet<>();

    @Column
    private LocalDateTime lastFailedAttempt;

    @Column
    private String lastLoginIp;

    @Column
    private String lastLoginDevice;

}

