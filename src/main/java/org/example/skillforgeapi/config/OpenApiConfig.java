package org.example.skillforgeapi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de la documentation OpenAPI / Swagger.
 *
 * <p>Une fois l'application démarrée, la documentation est accessible via :
 * <ul>
 *     <li>Swagger UI : <a href="http://localhost:8081/swagger-ui.html">http://localhost:8081/swagger-ui.html</a></li>
 *     <li>Spec JSON : <a href="http://localhost:8081/v3/api-docs">http://localhost:8081/v3/api-docs</a></li>
 * </ul>
 *
 * <p>La quasi-totalité des endpoints nécessite un JWT. Cliquez sur le bouton
 * « Authorize » dans Swagger UI et collez le token (sans le préfixe « Bearer »).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "SkillForge API",
                version = "0.0.1",
                description = """
                        API REST de la plateforme SkillForge : gestion des modules de formation,
                        scénarios, et assets (upload via SFTP).

                        **Authentification** : la plupart des endpoints requièrent un JWT
                        (header `Authorization: Bearer <token>`). Récupérez un token via
                        les endpoints `/api/auth/**`, puis cliquez sur « Authorize ».
                        """,
                contact = @Contact(name = "Équipe SkillForge", email = "ngapepouekarl@gmail.com"),
                license = @License(name = "Propriétaire")
        ),
        servers = {
                @Server(url = "http://localhost:8081", description = "Environnement local")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "Authentification JWT. Saisissez uniquement le token (sans « Bearer »).",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
