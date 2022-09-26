package com.donphds.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Order(-2)
@Component
@AllArgsConstructor
@ConditionalOnBean(ServerWebExchange.class)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper om;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory bufferFactory = response.bufferFactory();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer dataBuffer;
        try {
            if (ex instanceof HttpStatusCodeException) {
                response.setStatusCode(((HttpStatusCodeException) ex).getStatusCode());
                String statusText = ((HttpStatusCodeException) ex).getStatusText();
                dataBuffer =
                        bufferFactory.wrap(
                                om.writeValueAsBytes(
                                        Maps.immutableEntry(
                                                "msg",
                                                ((HttpStatusCodeException) ex).getStatusText())));
                return response.writeWith(Mono.just(dataBuffer));
            } else if (ex instanceof IOException) {
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                dataBuffer =
                        bufferFactory.wrap(
                                om.writeValueAsBytes(Maps.immutableEntry("msg", "网络发生了一点小抖动~")));
            } else {
                response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                dataBuffer =
                        bufferFactory.wrap(
                                om.writeValueAsBytes(Maps.immutableEntry("msg", "未知异常")));
            }
        } catch (JsonProcessingException e) {
            log.error("全局异常消息序列化失: ", e);
            dataBuffer = bufferFactory.wrap("".getBytes());
        }
        log.error("Exception Stack: ", ex);
        return response.writeWith(Mono.just(dataBuffer));
    }
}
