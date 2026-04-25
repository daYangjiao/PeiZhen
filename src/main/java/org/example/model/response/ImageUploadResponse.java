package org.example.model.response;

import lombok.Data;

@Data
public class ImageUploadResponse {

    private String url;

    private String originalUrl;

    private String scanUrl;

    private Boolean scanGenerated;
}
