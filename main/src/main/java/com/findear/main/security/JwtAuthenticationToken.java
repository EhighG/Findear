package com.findear.main.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * principal : memberId <br>
 * credentials : accessToken
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private Object principal;
    private Object credentials;

    public JwtAuthenticationToken(Object principal) {
        super(null);
        this.principal = principal;
        setAuthenticated(false);
    }

    public JwtAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public static JwtAuthenticationToken authenticated(Object principal, Object credentials,
                                                       Collection<? extends GrantedAuthority> authorities) {
        return new JwtAuthenticationToken(principal, credentials, authorities);
    }

    public static JwtAuthenticationToken unauthenticated(Object principal) {
        return new JwtAuthenticationToken(principal);
    }
}
