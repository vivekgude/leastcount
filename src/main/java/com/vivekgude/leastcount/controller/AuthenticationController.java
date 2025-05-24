package com.vivekgude.leastcount.controller;

import com.vivekgude.leastcount.model.AuthenticationRequest;
import com.vivekgude.leastcount.model.AuthenticationResponse;
import com.vivekgude.leastcount.model.RegisterRequest;
import com.vivekgude.leastcount.model.dao.User;
import com.vivekgude.leastcount.repositories.UserRepository;
import com.vivekgude.leastcount.security.JWTUtils;
import com.vivekgude.leastcount.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController extends BaseController{

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JWTUtils jwtUtils;
    private final UserRepository userRepository;

    public AuthenticationController(AuthenticationManager authenticationManager, UserService userService,
            JWTUtils jwtUtils, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        final UserDetails userDetails = userService.loadUserByUsername(request.getUsername());
        final String jwt = jwtUtils.generateToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new AuthenticationResponse(jwt, user.getUsername(), user.getId()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        UserDetails userDetails = userService.registerUser(request);
        final String jwt = jwtUtils.generateToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(new AuthenticationResponse(jwt, user.getUsername(), user.getId()));
    }
} 