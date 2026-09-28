package me.north30.erp.system.common.util;

import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;

/**
 * 图形验证码工具：基于 Hutool LineCaptcha（线段干扰），字符集去除易混淆字符（I/O/0/1）。
 * <p>headless 环境安全，输出 data URI（data:image/png;base64,...）。</p>
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

    private CaptchaUtil() {
    }

    /**
     * 生成验证码。
     *
     * @return 验证码文本与图片 data URI
     */
    public static Captcha create() {
        LineCaptcha captcha = new LineCaptcha(WIDTH, HEIGHT, CODE_LENGTH, LINE_COUNT);
        captcha.setGenerator(new RandomGenerator(CHARS, CODE_LENGTH));
        captcha.createCode();
        return new Captcha(captcha.getCode(), captcha.getImageBase64Data());
    }

    /**
     * 验证码生成结果。
     *
     * @param code    验证码文本（4 位，忽略大小写校验）
     * @param dataUri 图片 data URI（data:image/png;base64,...）
     */
    public record Captcha(String code, String dataUri) {
    }
}
