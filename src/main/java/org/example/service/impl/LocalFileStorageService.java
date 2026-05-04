package org.example.service.impl;

import org.example.service.FileStorageService;
import org.example.model.response.ImageUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private static final long MAX_UPLOAD_SIZE = 5 * 1024 * 1024L;
    private static final long MAX_DOCUMENT_IMAGE_UPLOAD_SIZE = 10 * 1024 * 1024L;
    private static final long MAX_AVATAR_SIZE = 10 * 1024 * 1024L;
    private static final int AVATAR_MAX_SIDE = 720;
    private static final float AVATAR_JPEG_QUALITY = 0.82f;
    private static final float DOCUMENT_JPEG_QUALITY = 0.9f;
    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"))
    );

    @Value("${app.upload-dir:${user.dir}/uploads}")
    private String uploadDirBase;

    @Override
    public String storeFile(MultipartFile file) {
        try {
            validateBasicFile(file, MAX_UPLOAD_SIZE, "请选择要上传的文件", "文件大小不能超过5MB");
            ensureUploadDirExists();

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = getFileExtension(originalFilename);
            String fileName = generateUniqueFileName(fileExtension);
            Path filePath = Paths.get(getUploadDir(), fileName);
            Files.copy(file.getInputStream(), filePath);
            return "/uploads/" + fileName;
        } catch (IOException e) {
            throw new IllegalArgumentException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String storeImage(MultipartFile file) {
        validateImage(file, MAX_UPLOAD_SIZE, "图片大小不能超过5MB");
        return storeFile(file);
    }

    @Override
    public ImageUploadResponse storeDocumentImage(MultipartFile file) {
        try {
            validateImage(file, MAX_DOCUMENT_IMAGE_UPLOAD_SIZE, "图片大小不能超过10MB");
            ensureUploadDirExists();

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String originalExtension = getFileExtension(originalFilename);
            if (originalExtension == null || originalExtension.isBlank()) {
                originalExtension = ".jpg";
            }
            String originalFileName = generateUniqueFileName(originalExtension.toLowerCase(Locale.ROOT));
            Path originalPath = Paths.get(getUploadDir(), originalFileName);
            Files.copy(file.getInputStream(), originalPath);

            ImageUploadResponse response = new ImageUploadResponse();
            response.setOriginalUrl("/uploads/" + originalFileName);
            response.setUrl(response.getOriginalUrl());
            try {
                BufferedImage sourceImage = ImageIO.read(originalPath.toFile());
                if (sourceImage == null) {
                    throw new IllegalArgumentException("图片解析失败");
                }
                BufferedImage enhanced = new DocumentScanImageProcessor().enhance(sourceImage);
                String scanFileName = generateUniqueFileName("_scan.jpg");
                Path scanPath = Paths.get(getUploadDir(), scanFileName);
                writeJpeg(enhanced, scanPath, DOCUMENT_JPEG_QUALITY);
                response.setScanUrl("/uploads/" + scanFileName);
                response.setScanGenerated(true);
            } catch (Exception scanError) {
                response.setScanUrl(response.getOriginalUrl());
                response.setScanGenerated(false);
            }
            return response;
        } catch (IOException e) {
            throw new IllegalArgumentException("图片上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String storeAvatar(MultipartFile file) {
        try {
            validateImage(file, MAX_AVATAR_SIZE, "头像大小不能超过10MB");
            ensureUploadDirExists();

            BufferedImage sourceImage;
            try (InputStream inputStream = file.getInputStream()) {
                sourceImage = ImageIO.read(inputStream);
            }

            if (sourceImage == null) {
                throw new IllegalArgumentException("头像图片解析失败");
            }

            BufferedImage normalizedImage = normalizeAvatarImage(sourceImage);
            String fileName = generateUniqueFileName(".jpg");
            Path filePath = Paths.get(getUploadDir(), fileName);
            writeJpeg(normalizedImage, filePath, AVATAR_JPEG_QUALITY);
            return "/uploads/" + fileName;
        } catch (IOException e) {
            throw new IllegalArgumentException("头像上传失败: " + e.getMessage(), e);
        }
    }

    private void validateBasicFile(MultipartFile file, long maxSize, String emptyMessage, String sizeMessage) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(sizeMessage);
        }
    }

    private void validateImage(MultipartFile file, long maxSize, String sizeMessage) {
        validateBasicFile(file, maxSize, "请选择要上传的图片", sizeMessage);

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("请上传图片文件");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的图片格式，请上传jpg、png、gif、bmp或webp格式图片");
        }
    }

    private void ensureUploadDirExists() {
        File uploadDirFile = new File(getUploadDir());
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }
    }

    private String getUploadDir() {
        String dir = uploadDirBase == null ? "" : uploadDirBase.replace("\\", "/");
        return dir.endsWith("/") ? dir : dir + "/";
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    private String generateUniqueFileName(String extension) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = dateFormat.format(new Date());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "_" + uuid + extension;
    }

    private BufferedImage normalizeAvatarImage(BufferedImage sourceImage) {
        BufferedImage squareImage = cropToSquare(sourceImage);
        int sourceWidth = squareImage.getWidth();
        int sourceHeight = squareImage.getHeight();
        int longestSide = Math.max(sourceWidth, sourceHeight);

        if (longestSide <= AVATAR_MAX_SIDE) {
            BufferedImage target = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = target.createGraphics();
            applyRenderHints(graphics);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, sourceWidth, sourceHeight);
            graphics.drawImage(squareImage, 0, 0, null);
            graphics.dispose();
            return target;
        }

        double scale = (double) AVATAR_MAX_SIDE / longestSide;
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));

        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        applyRenderHints(graphics);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, targetWidth, targetHeight);
        graphics.drawImage(squareImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return target;
    }

    private BufferedImage cropToSquare(BufferedImage sourceImage) {
        int sourceWidth = sourceImage.getWidth();
        int sourceHeight = sourceImage.getHeight();
        int squareSize = Math.min(sourceWidth, sourceHeight);
        int sourceX = Math.max(0, (sourceWidth - squareSize) / 2);
        int sourceY = Math.max(0, (sourceHeight - squareSize) / 2);

        BufferedImage target = new BufferedImage(squareSize, squareSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        applyRenderHints(graphics);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, squareSize, squareSize);
        graphics.drawImage(
                sourceImage,
                0,
                0,
                squareSize,
                squareSize,
                sourceX,
                sourceY,
                sourceX + squareSize,
                sourceY + squareSize,
                null
        );
        graphics.dispose();
        return target;
    }

    private void applyRenderHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private void writeJpeg(BufferedImage image, Path filePath, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG writer not available");
        }

        ImageWriter writer = writers.next();
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
             ImageOutputStream ios = ImageIO.createImageOutputStream(fos)) {
            writer.setOutput(ios);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }
}
