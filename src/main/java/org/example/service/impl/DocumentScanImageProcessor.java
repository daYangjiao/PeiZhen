package org.example.service.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class DocumentScanImageProcessor {

    private static final int MAX_OUTPUT_WIDTH = 1600;
    private static final int MAX_OUTPUT_HEIGHT = 1600;
    private static final int MIN_CONTENT_BOX = 80;
    private static final int EDGE_STEP = 4;
    private static final int BORDER_PADDING = 18;

    public BufferedImage enhance(BufferedImage source) {
        if (source == null) {
            throw new IllegalArgumentException("图片解析失败");
        }
        BufferedImage rgb = toRgb(source);
        int[] box = detectDocumentBox(rgb);
        BufferedImage cropped = crop(rgb, box);
        BufferedImage scaled = scaleDown(cropped);
        return enhanceContrast(scaled);
    }

    private BufferedImage toRgb(BufferedImage source) {
        BufferedImage target = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        applyRenderHints(graphics);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, target.getWidth(), target.getHeight());
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return target;
    }

    private int[] detectDocumentBox(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        long[] background = averageBorderColor(image);
        int left = width;
        int top = height;
        int right = -1;
        int bottom = -1;

        for (int y = 0; y < height; y += EDGE_STEP) {
            for (int x = 0; x < width; x += EDGE_STEP) {
                if (isDifferentFromBackground(image.getRGB(x, y), background)) {
                    left = Math.min(left, x);
                    top = Math.min(top, y);
                    right = Math.max(right, x);
                    bottom = Math.max(bottom, y);
                }
            }
        }

        if (right < left || bottom < top || right - left < MIN_CONTENT_BOX || bottom - top < MIN_CONTENT_BOX) {
            return new int[]{0, 0, width, height};
        }

        left = Math.max(0, left - BORDER_PADDING);
        top = Math.max(0, top - BORDER_PADDING);
        right = Math.min(width - 1, right + BORDER_PADDING);
        bottom = Math.min(height - 1, bottom + BORDER_PADDING);
        return new int[]{left, top, right + 1, bottom + 1};
    }

    private long[] averageBorderColor(BufferedImage image) {
        long red = 0;
        long green = 0;
        long blue = 0;
        long count = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int border = Math.max(4, Math.min(width, height) / 20);

        for (int y = 0; y < height; y += EDGE_STEP) {
            for (int x = 0; x < width; x += EDGE_STEP) {
                if (x > border && x < width - border && y > border && y < height - border) {
                    continue;
                }
                Color color = new Color(image.getRGB(x, y));
                red += color.getRed();
                green += color.getGreen();
                blue += color.getBlue();
                count++;
            }
        }

        if (count == 0) {
            return new long[]{245, 245, 245};
        }
        return new long[]{red / count, green / count, blue / count};
    }

    private boolean isDifferentFromBackground(int rgb, long[] background) {
        Color color = new Color(rgb);
        int distance = Math.abs(color.getRed() - (int) background[0])
                + Math.abs(color.getGreen() - (int) background[1])
                + Math.abs(color.getBlue() - (int) background[2]);
        int saturation = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()))
                - Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
        return distance > 42 || saturation > 36;
    }

    private BufferedImage crop(BufferedImage image, int[] box) {
        int width = Math.max(1, box[2] - box[0]);
        int height = Math.max(1, box[3] - box[1]);
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        applyRenderHints(graphics);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.drawImage(image, 0, 0, width, height, box[0], box[1], box[2], box[3], null);
        graphics.dispose();
        return target;
    }

    private BufferedImage scaleDown(BufferedImage image) {
        double scale = Math.min(1.0, Math.min((double) MAX_OUTPUT_WIDTH / image.getWidth(), (double) MAX_OUTPUT_HEIGHT / image.getHeight()));
        if (scale >= 1.0) {
            return image;
        }
        int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        applyRenderHints(graphics);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.drawImage(image, 0, 0, width, height, null);
        graphics.dispose();
        return target;
    }

    private BufferedImage enhanceContrast(BufferedImage image) {
        BufferedImage target = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y));
                int red = adjustChannel(color.getRed());
                int green = adjustChannel(color.getGreen());
                int blue = adjustChannel(color.getBlue());
                target.setRGB(x, y, new Color(red, green, blue).getRGB());
            }
        }
        return target;
    }

    private int adjustChannel(int value) {
        int adjusted = (int) Math.round((value - 128) * 1.08 + 128 + 4);
        return Math.max(0, Math.min(255, adjusted));
    }

    private void applyRenderHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
}
