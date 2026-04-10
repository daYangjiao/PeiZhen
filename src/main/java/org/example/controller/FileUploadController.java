package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.example.common.ResponseResult;
import org.example.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/common")
@Api(tags = "文件上传接口")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "上传文件")
    @ApiResponses({
            @ApiResponse(code = 200, message = "上传成功"),
            @ApiResponse(code = 400, message = "文件不合法"),
            @ApiResponse(code = 500, message = "上传失败")
    })
    public ResponseResult<String> uploadFile(
            @ApiParam(value = "文件", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseResult.success(fileStorageService.storeFile(file));
        } catch (IllegalArgumentException e) {
            return ResponseResult.error(e.getMessage());
        } catch (Exception e) {
            return ResponseResult.error("文件上传失败");
        }
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "上传图片")
    @ApiResponses({
            @ApiResponse(code = 200, message = "上传成功"),
            @ApiResponse(code = 400, message = "图片不合法"),
            @ApiResponse(code = 500, message = "上传失败")
    })
    public ResponseResult<String> uploadImage(
            @ApiParam(value = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseResult.success(fileStorageService.storeImage(file));
        } catch (IllegalArgumentException e) {
            return ResponseResult.error(e.getMessage());
        } catch (Exception e) {
            return ResponseResult.error("图片上传失败");
        }
    }

    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "上传头像")
    @ApiResponses({
            @ApiResponse(code = 200, message = "上传成功"),
            @ApiResponse(code = 400, message = "头像不合法"),
            @ApiResponse(code = 500, message = "上传失败")
    })
    public ResponseResult<String> uploadAvatar(
            @ApiParam(value = "头像文件", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseResult.success(fileStorageService.storeAvatar(file));
        } catch (IllegalArgumentException e) {
            return ResponseResult.error(e.getMessage());
        } catch (Exception e) {
            return ResponseResult.error("头像上传失败");
        }
    }
}
