package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@RestController
@RequestMapping("/api/common")
@Api(tags = "文件上传接口")
@RequiredArgsConstructor
public class FileUploadController {

    private static final String UPLOAD_DIR = "D:/NSU/lab/houduan2026.02.10/peizhen/src/main/resources/static/uploads/";

    @PostMapping("/upload")
    @ApiOperation("上传文件")
    public ResponseResult<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                return ResponseResult.error("请选择要上传的文件");
            }

            // 检查文件大小（限制为5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseResult.error("文件大小不能超过5MB");
            }

            // 创建上传目录（如果不存在）
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 生成唯一的文件名
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = getFileExtension(originalFilename);
            String fileName = generateUniqueFileName(fileExtension);

            // 保存文件
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            Files.copy(file.getInputStream(), filePath);

            // 返回文件访问URL
            String fileUrl = "/uploads/" + fileName;
            return ResponseResult.success(fileUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseResult.error("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error("文件上传失败");
        }
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

    @PostMapping("/upload-image")
    @ApiOperation("上传图片")
    public ResponseResult<String> uploadImage(@RequestParam("file") MultipartFile file) {
        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseResult.error("请上传图片文件");
        }

        // 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (!extension.equals(".jpg") && !extension.equals(".jpeg") &&
                !extension.equals(".png") && !extension.equals(".gif") &&
                !extension.equals(".bmp") && !extension.equals(".webp")) {
                return ResponseResult.error("不支持的文件格式，请上传jpg、png、gif、bmp或webp格式的图片");
            }
        }

        return uploadFile(file);
    }
}