package org.example.skillforgeapi.model.entity;

public enum Role {
    TRAINEE,
    TRAINER,
    ADMIN;

    // Utile pour la conversion depuis une chaîne
    public static Role fromString(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TRAINEE;
        }
    }
}