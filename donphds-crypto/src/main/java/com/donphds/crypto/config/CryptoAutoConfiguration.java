package com.donphds.crypto.config;

import com.donphds.crypto.secret.KeyManager;
import com.donphds.crypto.secret.KeyProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoAutoConfiguration {

    @Bean
    public KeyManager keyManager(KeyProvider keyProvider) {
        return new KeyManager(keyProvider);
    }

    @Bean
    public KeyProvider keyProvider() {
        return new KeyProvider();
    }
}
