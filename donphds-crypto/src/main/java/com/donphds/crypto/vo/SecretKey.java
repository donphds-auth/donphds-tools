package com.donphds.crypto.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SecretKey {

    @JsonProperty("key_id")
    private String keyId;

    @JsonProperty("aes_key")
    private String aseKey;

    private int version;
}
