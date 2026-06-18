package org.example.skillforgeapi.model.dto.request;

import lombok.Data;

@Data
public class SftpGoWebhookPayload {
    private String event;
    private String username;
    private String fsPath;
    private Long size;
}
