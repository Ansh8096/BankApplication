package net.engineerAnsh.BankApplication.Config;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Services.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // @EnableWebSecurity Not required
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy
                        (SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN") // hasRole("ADMIN") → looks for ROLE_ADMIN or hasAuthority("ROLE_ADMIN") → looks for ROLE_ADMIN
                        .requestMatchers("/user/**", "/account/**").authenticated()
                        .anyRequest().permitAll()
                )
                .userDetailsService(customUserDetailsService) // giving the details of our user entity (or userDto which we ce)
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
