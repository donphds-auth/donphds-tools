package com.donphds.ma.configuration.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SerializerConfig {

    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper om() {
        JsonMapper.Builder builder = JsonMapper.builder();
        // 属性为 null 不进行序列化，默认序列化全部
        builder.serializationInclusion(JsonInclude.Include.NON_EMPTY);
        // 未知属性也不进行序列化
        builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 未转义字符也可以序列化
        builder.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true);
        // 单引号 + \ 的嵌套也可以序列化
        builder.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        JsonMapper jacksonMapper = builder.build();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(
                BigDecimal.class,
                new JsonSerializer<BigDecimal>() {
                    @Override
                    public void serialize(
                            BigDecimal value, JsonGenerator gen, SerializerProvider serializers)
                            throws IOException {
                        DecimalFormat decimalFormat = new DecimalFormat("#.##");
                        gen.writeString(decimalFormat.format(value));
                    }
                });
        jacksonMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        javaTimeModule.addSerializer(
                LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(
                LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        jacksonMapper.registerModule(javaTimeModule);
        jacksonMapper.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());
        return jacksonMapper;
    }
}
