package me.north30.erp.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Jackson 全局序列化配置（Spring Boot 4 默认 Jackson 3：tools.jackson JsonMapper）。
 * <p>Long 按值域序列化：超出 JS Number 安全整数范围（2^53-1）的雪花 ID 输出字符串，
 * 防止前端精度丢失；total/userCount 等小值计数仍输出数字，与前端类型约定一致
 * （types/api.d.ts：id 为 string、PageResult.total 为 number）。</p>
 */
@Configuration
public class JacksonConfig {

    /** JS Number 安全整数上限（2^53-1） */
    private static final long MAX_SAFE_INTEGER = 9007199254740991L;
    /** JS Number 安全整数下限（-2^53+1） */
    private static final long MIN_SAFE_INTEGER = -9007199254740991L;

    @Bean
    public JsonMapperBuilderCustomizer longSafeRangeCustomizer() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, new SafeRangeLongSerializer(Long.class));
        module.addSerializer(Long.TYPE, new SafeRangeLongSerializer(Long.TYPE));
        return builder -> builder.addModule(module);
    }

    /**
     * 按安全整数范围序列化 Long 的 Serializer：范围内写数字、范围外写字符串。
     */
    static final class SafeRangeLongSerializer extends StdSerializer<Long> {

        private SafeRangeLongSerializer(Class<Long> type) {
            super(type);
        }

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializationContext ctxt) {
            if (value >= MIN_SAFE_INTEGER && value <= MAX_SAFE_INTEGER) {
                gen.writeNumber(value);
            } else {
                gen.writeString(String.valueOf(value));
            }
        }
    }
}
