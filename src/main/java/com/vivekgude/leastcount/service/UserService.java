package com.vivekgude.leastcount.service;

import com.vivekgude.leastcount.model.RegisterRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    UserDetails registerUser(RegisterRequest request);
}
