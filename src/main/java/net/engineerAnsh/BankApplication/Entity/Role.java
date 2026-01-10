package net.engineerAnsh.BankApplication.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique is true because : In role table every role must appear only one time
    // else User A → role_id = 1 (role_name = ROLE_ADMIN)
    //      User B → role_id = 2 (role_name = ROLE_ADMIN), both the users will store two different role_id even though they have same name.
    @Column(name = "role_name",unique = true,nullable = false)
    private String name;
}
