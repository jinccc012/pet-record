package com.harumi.petrecord.security;

import com.harumi.petrecord.security.JwtService.VerifiedToken;
import com.harumi.petrecord.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            VerifiedToken verified = jwtService.parseToken(token);
            CurrentUser currentUser = verified.user();

            // Re-check against the database on every request so deleted/disabled users and revoked
            // tokens (token_version bumped on logout, etc.) lose access immediately rather than at
            // token expiry. Empty result => user is gone or soft-deleted.
            Optional<Integer> currentVersion = userRepository.findTokenVersionById(currentUser.id());
            if (currentVersion.isEmpty() || currentVersion.get() != verified.tokenVersion()) {
                log.debug("Stale or revoked JWT on {} {}", request.getMethod(), request.getRequestURI());
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    currentUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + currentUser.role().name()))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (InvalidJwtException e) {
            log.debug("Invalid JWT presented on {} {}", request.getMethod(), request.getRequestURI());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }
}
