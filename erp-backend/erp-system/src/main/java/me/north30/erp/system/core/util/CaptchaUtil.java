package me.north30.erp.system.core.util;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 图形验证码自绘工具：Java AWT 生成 4 位验证码 PNG（无第三方依赖，headless 安全）。
 * <p>字符集去除易混淆字符（I/O/0/1），输出 data URI（data:image/png;base64,...）。</p>
 */
public final class CaptchaUtil {

    /** 验证码字符数 */
    public static final int CODE_LENGTH = 4;

    /** 字符集：去除易混淆的 I、O、0、1 */
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /** 图片宽度（像素） */
    private static final int WIDTH = 130;

    /** 图片高度（像素） */
    private static final int HEIGHT = 42;

    /** 干扰线数量 */
    private static final int LINE_COUNT = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    private CaptchaUtil() {
    }

    /**
     * 生成验证码。
     *
     * @param code 验证码文本
     * @return data URI 格式的 PNG 图片
     */
    public static String toDataUri(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 白色背景
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            // 干扰线
            for (int i = 0; i < LINE_COUNT; i++) {
                g.setColor(randomPastelColor());
                g.setStroke(new BasicStroke(1.0f + RANDOM.nextFloat()));
                g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                    RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }
            // 逐字符绘制：随机颜色、轻微旋转
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 30);
            g.setFont(font);
            int charWidth = (WIDTH - 20) / CODE_LENGTH;
            for (int i = 0; i < CODE_LENGTH; i++) {
                String ch = String.valueOf(code.charAt(i));
                g.setColor(randomDarkColor());
                AffineTransform saved = g.getTransform();
                double rotation = (RANDOM.nextDouble() - 0.5) * 0.5;
                int x = 10 + i * charWidth;
                int y = 32 + RANDOM.nextInt(4);
                g.rotate(rotation, x + charWidth / 2.0, y - 10);
                g.drawString(ch, x, y);
                g.setTransform(saved);
            }
            // 噪点
            for (int i = 0; i < 60; i++) {
                g.setColor(randomPastelColor());
                g.fillRect(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 1, 1);
            }
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException("验证码图片生成失败", e);
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    /**
     * 生成 4 位随机验证码文本。
     */
    public static String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private static Color randomPastelColor() {
        return new Color(180 + RANDOM.nextInt(60), 180 + RANDOM.nextInt(60), 180 + RANDOM.nextInt(60));
    }

    private static Color randomDarkColor() {
        return new Color(RANDOM.nextInt(110), RANDOM.nextInt(110), RANDOM.nextInt(110));
    }
}
