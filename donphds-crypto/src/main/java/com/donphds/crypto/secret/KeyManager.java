package com.donphds.crypto.secret;

import com.donphds.crypto.excpetion.CryptoException;
import com.donphds.crypto.utils.CryptUtil;
import com.donphds.crypto.vo.SecretKey;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KeyManager implements ApplicationListener<ContextRefreshedEvent> {

    public static final AtomicInteger COUNT = new AtomicInteger(0);
    private static final String CACHE_SECRET_KET = "CACHE_SECRET";
    private static final SecretKey DEFAULT_KET;

    static {
        DEFAULT_KET =
                new SecretKey(
                        "default key",
                        Base64.getEncoder().encodeToString(KeyProvider.DEFAULT_AES_KEY),
                        0);
    }

    private final ConcurrentHashMap<Integer, SecretKey> keyMap = new ConcurrentHashMap<>();
    private final AsyncLoadingCache<String, List<SecretKey>> cache;
    private int version = 0;

    public KeyManager(KeyProvider provider) {
        cache =
                Caffeine.newBuilder()
                        .maximumSize(1000L)
                        .expireAfterWrite(65, TimeUnit.MINUTES)
                        .refreshAfterWrite(30, TimeUnit.MINUTES)
                        .executor(
                                new ThreadPoolExecutor(
                                        3,
                                        4,
                                        1,
                                        TimeUnit.MINUTES,
                                        new ArrayBlockingQueue<>(1000),
                                        r ->
                                                new Thread(
                                                        r,
                                                        String.format(
                                                                "secret coffeine thread %s",
                                                                COUNT.get()))))
                        .buildAsync((key, e) -> provider.getSecretKey());
        this.keyMap.put(0, DEFAULT_KET);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent e) {
        CryptUtil.SetManager(this);
    }

    public SecretKey getLatestKey() {
        return this.getKey(this.version);
    }

    public SecretKey getKey(int version) {
        load();
        if (this.version == 0) {
            log.warn("当前使用默认 secret key");
        }

        SecretKey secretKey = this.keyMap.get(version);
        if (Objects.isNull(secretKey)) {
            throw new CryptoException(
                    String.format("current version %s has not secret key", version));
        }
        return secretKey;
    }

    public void load() {
        this.cache
                .get(CACHE_SECRET_KET)
                .whenComplete(
                        ((keys, e) -> {
                            if (e == null) {
                                for (SecretKey key : keys) {
                                    this.keyMap.put(key.getVersion(), key);
                                    this.version = Math.max(key.getVersion(), this.version);
                                }
                            }
                        }));
    }
}
