package com.example.digitalbank.security;

import com.example.digitalbank.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;

    public JwtAuthenticationFilter(JwtService jwtService, AccountRepository accountRepository) {
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)) {
                UUID accountId = jwtService.extractAccountId(token);
                accountRepository.findById(accountId).ifPresent(account -> {
                    List<GrantedAuthority> authorities = "ADMIN".equals(account.getRole())
                            ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                            : List.of();
                    var authentication = new UsernamePasswordAuthenticationToken(accountId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }

        chain.doFilter(request, response);
    }
}
