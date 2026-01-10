package net.engineerAnsh.BankApplication.Config;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Entity.Role;
import net.engineerAnsh.BankApplication.Repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor // It allows (this.roleRepository = roleRepository)
public class RoleSeeder implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    // Implements CommandLineRunner Meaning:
    // This class will run once when the application starts (After Spring context is initialized).
    @Override
    public void run(String... args){
        // If 'ROLE_USER' is not in the role table, add it...
        if(roleRepository.findByName("ROLE_USER").isEmpty()) {
            roleRepository.save(new Role(null,"ROLE_USER"));
        }

        // If 'ROLE_ADMIN' is not in the role table, add it...
        if(roleRepository.findByName("ROLE_ADMIN").isEmpty()){
            roleRepository.save(new Role(null,"ROLE_ADMIN"));
        }
    }

}
