package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.example.unity.QrCodeUtil;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@Api(tags = "公开二维码接口")
public class PublicQrCodeController {

    private final QrCodeUtil qrCodeUtil;

    @GetMapping(value = "/order-qr/{orderId}.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ApiOperation(value = "获取订单服务核销二维码图片", notes = "返回订单服务开始核销用的二维码 PNG 图片，内容为 SERVICE_CONFIRM_{orderId}。")
    public ResponseEntity<byte[]> getOrderQrCode(@PathVariable Integer orderId) {
        byte[] imageBytes = qrCodeUtil.generateQrCodeBytes("SERVICE_CONFIRM_" + orderId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"order-" + orderId + "-qr.png\"")
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .contentType(MediaType.IMAGE_PNG)
                .body(imageBytes);
    }
}
