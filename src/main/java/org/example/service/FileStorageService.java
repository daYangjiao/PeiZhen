package org.example.service;

import org.springframework.web.multipart.MultipartFile;
import org.example.model.response.ImageUploadResponse;

public interface FileStorageService {

    String storeFile(MultipartFile file);

    String storeImage(MultipartFile file);

    ImageUploadResponse storeDocumentImage(MultipartFile file);

    String storeAvatar(MultipartFile file);
}
