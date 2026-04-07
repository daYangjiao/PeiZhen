package org.example.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String storeFile(MultipartFile file);

    String storeImage(MultipartFile file);

    String storeAvatar(MultipartFile file);
}
