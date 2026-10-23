package me.north30.erp.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 全局序列化配置（Spring Boot 4 默认 Jackson 3：tools.jackson JsonMapper）。
 * <p>Long 统一序列化为字符串：雪花 ID 超出 JS Number 安全整数范围（2^53），
 * 直接输出数字会导致前端精度丢失（如用户 ID 被舍入后查询报"不存在"）。
 * 前端按需 Number() 收敛计数字段。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer longToStringCustomizer() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        return builder -> builder.addModule(module);
    }
}
