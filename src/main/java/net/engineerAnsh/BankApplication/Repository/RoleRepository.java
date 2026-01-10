package net.engineerAnsh.BankApplication.Repository;

import net.engineerAnsh.BankApplication.Entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findByName(String name); // This lets you fetch roles like: ROLE_USER, ROLE_ADMIN
}
