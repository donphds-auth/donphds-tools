package com.donphds.configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.*;
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
        builder.serializationInclusion(JsonInclude.Include.NON_EMPTY);
        builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        builder.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS, true);
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
