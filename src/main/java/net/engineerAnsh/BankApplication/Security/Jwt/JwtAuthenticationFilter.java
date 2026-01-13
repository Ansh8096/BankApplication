package net.engineerAnsh.BankApplication.Security.Jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.engineerAnsh.BankApplication.Security.UserDetails.CustomUserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

// 🎯 What is this filter?
// A JWT filter runs on every incoming request and:
// Reads Authorization header, Extracts JWT, Validates JWT, Loads user details, Sets SecurityContextHolder (without this SecurityContextHolder will be empty)
// After this → Spring Security knows who is logged in.

@Component
@RequiredArgsConstructor
@Slf4j
// Why OncePerRequestFilter: Because it runs once per request, Avoids duplicate execution, Perfect for JWT
public class JwtAuthenticationFilter extends OncePerRequestFilter { // Interview one-liner: “The JWT filter validates the token and populates the Spring Security context so that authorization can occur.”


    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;

    // It checks every incoming request for a JWT token, validates it, and if valid, tells Spring Security “this user is authenticated”...
    // This runs before your controller is called.
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain
    )throws ServletException, IOException {
        log.info("JWT FILTER HIT");
        String authHeader = request.getHeader("Authorization"); // Reads the HTTP header send by the client => 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9'...

        // What this means:
        // If no token is present → user is anonymous
        // If header doesn’t start with Bearer → ignore it
        //👉 Do not block request here
        //👉 Just move on to next filter
        // ✔ Allows public APIs to work
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            log.info("JWT FILTER EXECUTED BUT THERE IS SOMETHING WRONG IN AUTH HEADER");
            filterChain.doFilter(request,response);
            return;
        }

        String token = authHeader.substring(7).trim(); // Removes "Bearer " (7 chars), Extracts only the JWT string...

        // Checks if token is valid or not...
        boolean tokenValid = jwtUtils.isTokenValid(token);

        // If token is invalid → skip authentication, Request continues but user stays unauthenticated (means he can use public api's)...
        if(!tokenValid){
            filterChain.doFilter(request,response);
            return;
        }

        String email = jwtUtils.extractEmail(token); // Reads sub (subject) from JWT, This is the user’s identity...

        // Good practice: Prevents re-authentication, Avoids overwriting existing security context...
        // In simpler words: set authentication of user-ofSpecificEmailExtractedFromTheGivenToken only if the securityContextHolder is empty (means no user is logged in)....
        if(email != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(email); // Loads user info: Email, Password (hashed), Roles
            // This object tells Spring Security:
            // “This user is authenticated and has these roles.”
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null, // credentials = null → password already verified...
                    userDetails.getAuthorities() // Authorities = roles...
            );

            // Adds: IP address, Session ID (if any), Useful for auditing & logging...
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            log.info("Setting auth in the contextHolder");
            // It tells Spring Security: “This request belongs to an authenticated user”. After this:  @PreAuthorize works , hasRole() works, Controllers know the user
            // we set the authentication token in the contextHolder...
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request,response);
    }
}
