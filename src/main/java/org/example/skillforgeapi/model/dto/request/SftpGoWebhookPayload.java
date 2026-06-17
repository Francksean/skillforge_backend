package org.example.skillforgeapi.model.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class SftpGoWebhookPayload {
    private String event;
    private String username;
    private String path;
    private String fsPath;
    private Long size;
    private String protocol;
    private String sessionId;
    private Long timestamp;
    private Map<String, String> checksum;
}
