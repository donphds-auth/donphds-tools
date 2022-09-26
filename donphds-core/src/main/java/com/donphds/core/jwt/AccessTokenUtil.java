package com.donphds.core.jwt;

import com.donphds.core.contants.JwtConstants;
import com.donphds.core.exception.ValidatedException;
import com.google.common.collect.Lists;
import lombok.experimental.UtilityClass;
import org.jose4j.base64url.Base64Url;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.OctetSequenceJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.lang.JoseException;

import java.nio.charset.StandardCharsets;

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
            long expireIn)
            throws JoseException {
        String signSecret = Base64Url.decodeToString(rsaPrivateKey, StandardCharsets.UTF_8.name());
        RsaJsonWebKey rsaJwk = (RsaJsonWebKey) JsonWebKey.Factory.newJwk(signSecret);
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setGeneratedJwtId();
        jwtClaims.setIssuedAtToNow();
        String cryptoSecret = Base64Url.decodeToString(octetSecret, StandardCharsets.UTF_8.name());
        OctetSequenceJsonWebKey aesJwk =
                (OctetSequenceJsonWebKey) JsonWebKey.Factory.newJwk(cryptoSecret);
        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setPlaintext(sub);
        jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.DIRECT);
        jwe.setEncryptionMethodHeaderParameter(
                ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
        jwe.setKey(aesJwk.getKey());
        jwtClaims.setSubject(jwe.getCompactSerialization());
        jwtClaims.setIssuedAtToNow();
        jwtClaims.setNotBefore(NumericDate.fromSeconds(creatAt));
        jwtClaims.setExpirationTimeMinutesInTheFuture(expireIn);
        jwtClaims.setIssuer(issuer);
        jwtClaims.setAudience(serviceId);
        jwtClaims.setStringClaim(JwtConstants.Claim.CLI, clientId);
        JsonWebSignature signature = new JsonWebSignature();
        signature.setKeyIdHeaderValue(rsaJwk.getKeyId());
        signature.setKey(rsaJwk.getPrivateKey());
        signature.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        signature.setPayload(jwtClaims.toJson());
        return signature.getCompactSerialization();
    }

    public static Boolean verify(String rsaPrivateKey) throws JoseException {
        try {
            String rsaSecretStr =
                    Base64Url.decodeToString(rsaPrivateKey, StandardCharsets.UTF_8.name());
            JsonWebKey jwk = JsonWebKey.Factory.newJwk(rsaSecretStr);
            JwtConsumer consumer =
                    new JwtConsumerBuilder()
                            .setVerificationKeyResolver(
                                    new JwksVerificationKeyResolver(Lists.newArrayList(jwk)))
                            .build();
            consumer.processToClaims(rsaSecretStr);
            return Boolean.TRUE;
        } catch (JoseException | InvalidJwtException e) {
            throw new ValidatedException(e);
        }
    }
}
