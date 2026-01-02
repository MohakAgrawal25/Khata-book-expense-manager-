package com.expensetracker.app.controller;

import com.expensetracker.app.entity.User;
import com.expensetracker.app.dto.*;
import com.expensetracker.app.repository.UserRepository;
import com.expensetracker.app.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController 
{

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> registerUser(@RequestBody SignupRequest signUpRequest) 
    {
        // Your existing, successful signup logic
        if (userRepository.existsByUsername(signUpRequest.getUsername())) 
        {
            return ResponseEntity.badRequest().body("Username is already taken!");
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())) 
        {
            return ResponseEntity.badRequest().body("Email is already in use!");
        }

        User user = User.builder()
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .build();
        userRepository.save(user);
        
        // Return explicit 201 Created status for resource creation
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully!");
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            // *** CRITICAL STEP: Authenticates credentials using DaoAuthenticationProvider ***
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            
            // If successful, generate token
            String token = jwtUtil.generateToken(authentication);

            return ResponseEntity.ok(new JwtResponse(token));

        } catch (BadCredentialsException e) {
            // *** CRITICAL RESPONSE: Thrown if passwords don't match ***
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Invalid username or password");
        }
        catch (Exception e) {
        // Catch any other exceptions and return cleanly
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An unexpected error occurred"));
    }
}
}