package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.example.common.ResponseResult;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
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
public class FileUploadController {

    @Value("${app.upload-dir:${user.dir}/uploads}")
    private String uploadDirBase;

    private String getUploadDir() {
        String dir = uploadDirBase == null ? "" : uploadDirBase.replace("\\", "/");
        return dir.endsWith("/") ? dir : dir + "/";
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "上传文件", notes = "上传任意业务文件，单文件大小限制 5MB。成功后返回可访问的相对路径。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "上传成功，data 为文件访问路径"),
            @ApiResponse(code = 400, message = "未上传文件、文件为空或文件过大"),
            @ApiResponse(code = 500, message = "文件保存失败")
    })
    public ResponseResult<String> uploadFile(
            @ApiParam(value = "待上传文件，表单字段名固定为 file", required = true)
            @RequestParam("file") MultipartFile file) {
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
            String uploadDir = getUploadDir();
            File uploadDirFile = new File(uploadDir);
            if (!uploadDirFile.exists()) {
                uploadDirFile.mkdirs();
            }

            // 生成唯一的文件名
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = getFileExtension(originalFilename);
            String fileName = generateUniqueFileName(fileExtension);

            // 保存文件
            Path filePath = Paths.get(uploadDir, fileName);
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

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "上传图片", notes = "上传图片文件，支持 jpg/jpeg/png/gif/bmp/webp，大小限制沿用通用上传规则。")
    @ApiResponses({
            @ApiResponse(code = 200, message = "上传成功，data 为图片访问路径"),
            @ApiResponse(code = 400, message = "文件不是图片或格式不支持"),
            @ApiResponse(code = 500, message = "文件保存失败")
    })
    public ResponseResult<String> uploadImage(
            @ApiParam(value = "待上传图片，表单字段名固定为 file", required = true)
            @RequestParam("file") MultipartFile file) {
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
