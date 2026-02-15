package org.example.unity;

import org.springframework.stereotype.Component;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class QrCodeUtil {
    // 直接硬编码默认配置（删除原来的@Value注解）
    private final int width = 300;    // 默认宽度
    private final int height = 300;   // 默认高度
    private final String format = "PNG"; // 默认格式

    /**
     * 生成Base64格式的二维码
     */
    public String generateQrCodeBase64(String content) {
        if (content == null || content.isEmpty()) {
            throw new RuntimeException("二维码内容不能为空");
        }

        // 二维码配置参数
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8"); // 字符编码
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // 纠错等级（M级适中）
        hints.put(EncodeHintType.MARGIN, 1); // 边距

        try {
            // 生成二维码矩阵
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            // 转换为Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, format, outputStream);
            byte[] qrBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(qrBytes);
        } catch (Exception e) {
            throw new RuntimeException("二维码生成失败：" + e.getMessage(), e);
        }
    }
}