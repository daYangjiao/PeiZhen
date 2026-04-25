package org.example.service.impl;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentScanImageProcessorTest {

    @Test
    void enhanceShouldCropPlainBorderAndKeepDocumentReadable() {
        BufferedImage source = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(new Color(245, 247, 250));
        graphics.fillRect(0, 0, 1000, 700);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(210, 120, 580, 360);
        graphics.setColor(new Color(37, 99, 235));
        graphics.fillRect(230, 140, 540, 320);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(300, 220, 280, 24);
        graphics.dispose();

        DocumentScanImageProcessor processor = new DocumentScanImageProcessor();
        BufferedImage enhanced = processor.enhance(source);

        assertThat(enhanced.getWidth()).isLessThan(1000);
        assertThat(enhanced.getHeight()).isLessThan(700);
        assertThat(enhanced.getWidth()).isGreaterThan(300);
        assertThat(enhanced.getHeight()).isGreaterThan(180);
        assertThat(new Color(enhanced.getRGB(enhanced.getWidth() / 2, enhanced.getHeight() / 2)).getBlue()).isGreaterThan(80);
    }
}
