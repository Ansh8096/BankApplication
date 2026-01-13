package net.engineerAnsh.BankApplication.Security;

import lombok.RequiredArgsConstructor;
import net.engineerAnsh.BankApplication.Security.Jwt.JwtAuthenticationFilter;
import net.engineerAnsh.BankApplication.Security.UserDetails.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // @EnableWebSecurity Not required
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
                .userDetailsService(customUserDetailsService) // giving the details of our user entity (or userDto which we created)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // This tells Spring Security: “I want to insert my own filter into the security filter chain”.Before Spring Security’s default login filter...
                .build();
    }
//    This tells Spring Security: “I want to insert my own filter into the security filter chain.”
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration auth) throws Exception {
        return auth.getAuthenticationManager();
    }
}
