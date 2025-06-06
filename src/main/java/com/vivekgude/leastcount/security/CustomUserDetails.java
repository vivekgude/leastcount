package com.vivekgude.leastcount.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class CustomUserDetails extends User {
    private static final long serialVersionUID = -2658865590104138085L;
    private final long userId;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, long userId) {
        super(username, password, authorities);
        this.userId = userId;
    }

}
 