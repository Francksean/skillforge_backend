package org.example.skillforgeapi.model.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class SftpGoWebhookPayload {
    private String event;
    private String username;
    private String action;
    private String path;
    private Long size;
    private Long fileModified;
    private Map<String, String> checksum;
    private String provider;
}
