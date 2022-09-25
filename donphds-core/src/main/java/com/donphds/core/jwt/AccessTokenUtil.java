package com.donphds.core.jwt;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import org.jose4j.base64url.Base64Url;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.OctetSequenceJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.AesKey;
import org.jose4j.keys.resolvers.JwksDecryptionKeyResolver;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.JoseException;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Map;

@UtilityClass
public class AccessTokenUtil {

    public static String generate(
            String issuer,
            String serviceId,
            String clientId,
            String octetSecret,
            String rsaPrivateKey,
            String sub,
            long creatAt,
            long expireIn
    ) throws JoseException {
        String signSecret = Base64Url.decodeToString(rsaPrivateKey, StandardCharsets.UTF_8.name());
        RsaJsonWebKey rsaJwk = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(signSecret);
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setGeneratedJwtId();
        jwtClaims.setIssuedAtToNow();
        String cryptoSecret = Base64Url.decodeToString(octetSecret, StandardCharsets.UTF_8.name());
        OctetSequenceJsonWebKey aesJwk = (OctetSequenceJsonWebKey) JsonWebKey.Factory.newJwk(cryptoSecret);
        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setPlaintext(sub);
        jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.DIRECT);
        jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
        jwe.setKey(aesJwk.getKey());
        jwtClaims.setSubject(jwe.getCompactSerialization());
        jwtClaims.setIssuedAtToNow();
        jwtClaims.setNotBefore(NumericDate.now());
        jwtClaims.setExpirationTimeMinutesInTheFuture(expireIn);
        jwtClaims.setIssuer(issuer);
        jwtClaims.setAudience(serviceId);
        jwtClaims.setStringClaim("cli", clientId);
        JsonWebSignature signature = new JsonWebSignature();
        signature.setKeyIdHeaderValue(rsaJwk.getKeyId());
        signature.setKey(rsaJwk.getPrivateKey());
        signature.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        signature.setPayload(jwtClaims.toJson());
        return signature.getCompactSerialization();
    }

    public static void main(String[] args) throws JoseException, InvalidJwtException, MalformedClaimException {
        RsaJsonWebKey rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId("k2");
        Key key = new AesKey(ByteUtil.randomBytes(32));
        OctetSequenceJsonWebKey octetSequenceJsonWebKey = new OctetSequenceJsonWebKey(key);
        String subject = JsonUtil.toJson(Map.of("nickname", "test", "picture", "https://baidu.com"));
        String issuer = "https://auth.donphds.com/discover";
        String jwtToken = generate(issuer, "service_id", "client_id", Base64Url.encode(octetSequenceJsonWebKey.toJson(JsonWebKey.OutputControlLevel.INCLUDE_SYMMETRIC), StandardCharsets.UTF_8.name()),
                Base64Url.encode(rsaJsonWebKey.toJson(JsonWebKey.OutputControlLevel.INCLUDE_PRIVATE), Charsets.UTF_8.name()), subject, 20, 20);

        JwtConsumer jwtConsumer =  new JwtConsumerBuilder()
                .setExpectedAudience("service_id")
                .setRequireExpirationTime()
                .setSkipSignatureVerification()
//                .setVerificationKeyResolver(new JwksVerificationKeyResolver(Lists.newArrayList(rsaJsonWebKey)))
                .build();
        JwtClaims jwtClaims = jwtConsumer.processToClaims(jwtToken);
        String sub = jwtClaims.getSubject();
        System.err.println(sub);

        JwtConsumer decryptConsumer = new JwtConsumerBuilder()
                .setDisableRequireSignature()
                .setDecryptionKeyResolver(new JwksDecryptionKeyResolver(Lists.newArrayList(octetSequenceJsonWebKey)))
                .build();
        JwtClaims jwtClaims1 = decryptConsumer.processToClaims(sub);
        System.err.println(jwtClaims1.toJson());
    }


}
