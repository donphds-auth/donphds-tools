package com.donphds.ma.crypto.secret;

import com.donphds.ma.crypto.excpetion.CryptoException;
import com.donphds.ma.crypto.vo.SecretKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KeyProvider {

    public static final byte[] DEFAULT_AES_KEY = new byte[32];
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyProvider.class);
    private static final List<SecretKey> EMPTY = new ArrayList<>();

    static {
        Arrays.fill(DEFAULT_AES_KEY, (byte) 0xff);
    }

    private final AsyncHttpClient client =
            Dsl.asyncHttpClient(
                    new DefaultAsyncHttpClientConfig.Builder()
                            .setReadTimeout(10000)
                            .setConnectTimeout(10000)
                            .setMaxConnectionsPerHost(50)
                            .setPooledConnectionIdleTimeout(10000)
                            .setMaxConnections(1000)
                            .build());

    @Value("${donphds.ma.common.db_key_endpoint:}")
    private String endpoint;

    public CompletableFuture<List<SecretKey>> getSecretKey() {
        return StringUtils.isNotBlank(endpoint) ? fetch() : generateKey();
    }

    private CompletableFuture<List<SecretKey>> generateKey() {
        return CompletableFuture.<List<SecretKey>>supplyAsync(
                () ->
                        Collections.singletonList(
                                new SecretKey(
                                        "map-db-key", "jWJTpS9z/2CZSXMxUb9dP90ISZsys09M", 1)));
    }

    private CompletableFuture<List<SecretKey>> fetch() {
        return client.prepareGet(endpoint)
                .execute()
                .toCompletableFuture()
                .thenApply(this::parseResponse);
    }

    private List<SecretKey> parseResponse(Response rsp) {
        int statusCode = rsp.getStatusCode();
        if (statusCode / 100 == 4) {
            throw new CryptoException("not key fetch");
        }
        ObjectMapper om = new ObjectMapper();
        try {
            return om.readValue(rsp.getResponseBody(), new TypeReference<List<SecretKey>>() {});
        } catch (JsonProcessingException e) {
            LOGGER.error("密钥反序列化失败", e);
            return EMPTY;
        }
    }
}
