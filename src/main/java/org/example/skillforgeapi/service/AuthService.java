package org.example.skillforgeapi.service;

import org.example.skillforgeapi.exception.DuplicateResourceException;
import org.example.skillforgeapi.model.dto.request.LoginRequest;
import org.example.skillforgeapi.model.dto.request.RegisterRequest;
import org.example.skillforgeapi.model.dto.response.JwtResponse;
import org.example.skillforgeapi.util.Role;
import org.example.skillforgeapi.model.entity.User;
import org.example.skillforgeapi.repository.UserRepository;
import org.example.skillforgeapi.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.secret:admin123}")
    private String adminSecret;

    /**
     * Authentifier un utilisateur et générer un token JWT
     */
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);
        User user = (User) authentication.getPrincipal();

        log.info("Utilisateur connecté : {}", user.getUsername());

        return new JwtResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled()
        );
    }

    /**
     * Inscription d'un nouvel utilisateur (rôle TRAINEE par défaut)
     * Si adminSecret correspond, rôle ADMIN
     */
    @Transactional
    public JwtResponse register(RegisterRequest request) {
        // Vérifier l'unicité du username et email
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Nom d'utilisateur déjà pris: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email déjà utilisé: " + request.getEmail());
        }

        // Déterminer le rôle
        Role role = Role.TRAINEE;
        if (request.getAdminSecret() != null && adminSecret.equals(request.getAdminSecret())) {
            role = Role.ADMIN;
            log.warn("Création d'un compte ADMIN via le secret admin !");
        }

        // Créer l'utilisateur
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(role);
        user.setEnabled(true); // Compte activé par défaut

        user = userRepository.save(user);
        log.info("Nouvel utilisateur enregistré : {} (rôle: {})", user.getUsername(), role);

        // Générer un token JWT pour l'inscription automatique
        // On simule une authentification
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
        );
        String token = tokenProvider.generateToken(authentication);

        return new JwtResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled()
        );
    }
}
