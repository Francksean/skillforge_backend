package org.example.skillforgeapi.service;

import com.jcraft.jsch.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.skillforgeapi.model.entity.Asset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SftpClientService {

    @Value("${app.sftp.host}")
    private String host;

    @Value("${app.sftp.port}")
    private int port;

    @Value("${app.sftp.username}")
    private String username;

    @Value("${app.sftp.password}")
    private String password;

    /**
     * Lit un fichier depuis SFTPGo et retourne un InputStream
     * Nécessite une librairie SFTP (ex: JSch, Apache Commons VFS)
     * Ici, on donne l'exemple avec un pseudo-code.
     */
    public void streamFileToResponse(Asset asset, HttpServletResponse response) throws IOException {
        if (asset == null || asset.getSftpPath() == null) {
            throw new IllegalArgumentException("Chemin SFTP invalide");
        }

        JSch jSch = new JSch();
        Session session = null;
        ChannelSftp channel = null;
        InputStream remoteStream = null;

        try {
            // 1. Connexion SFTP
            session = jSch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no"); // À sécuriser en prod
            session.connect();

            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            // 2. Récupération du flux distant (sans le charger en RAM)
            remoteStream = channel.get(asset.getSftpPath());

            // 3. Configuration des headers HTTP pour forcer le téléchargement
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + asset.getName() + "\"");
            // Optionnel : si vous connaissez la taille, faites response.setContentLengthLong(taille);

            // 4. STREAMING PUR : copie directe du flux SFTP vers la sortie HTTP
            // (transfère par blocs de 8KB, ne consomme presque pas de RAM)
            remoteStream.transferTo(response.getOutputStream());
            response.getOutputStream().flush();

        } catch (JSchException | SftpException e) {
            throw new IOException("Erreur lors du streaming SFTP : " + e.getMessage(), e);
        } finally {
            // 5. Fermeture des ressources (SEULEMENT après la fin du transfert)
            if (remoteStream != null) {
                try {
                    remoteStream.close();
                } catch (IOException ignored) {
                }
            }
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}