package com.donphds.ma.auth;

import static com.donphds.ma.auth.constant.AlipayConstants.APP_ID;
import static com.donphds.ma.auth.constant.AlipayConstants.BIZ_CONTENT;
import static com.donphds.ma.auth.constant.AlipayConstants.CHARSET;
import static com.donphds.ma.auth.constant.AlipayConstants.CODE;
import static com.donphds.ma.auth.constant.AlipayConstants.GRANT_TYPE;
import static com.donphds.ma.auth.constant.AlipayConstants.GRATE_TYPE_CODE;
import static com.donphds.ma.auth.constant.AlipayConstants.METHOD;
import static com.donphds.ma.auth.constant.AlipayConstants.METHOD_VAL;
import static com.donphds.ma.auth.constant.AlipayConstants.SIGN_TYPE;
import static com.donphds.ma.auth.constant.AlipayConstants.TIMESTAMP;
import static com.donphds.ma.auth.constant.AlipayConstants.VERSION;

import com.donphds.ma.auth.properties.AlipayProperties;
import com.donphds.ma.common.utils.CommonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DonphdsMaAuthApplicationTests {

    @Autowired AlipayProperties alipayProperties;
    @Autowired ObjectMapper om;

    @Test
    void contextLoads() {}

    @Test
    void testSign()
            throws JsonProcessingException, NoSuchAlgorithmException, InvalidKeySpecException,
                    InvalidKeyException, SignatureException {
        Map<String, String> params = new TreeMap<>(StringUtils::compare);
        params.put(APP_ID, alipayProperties.getAppId());
        Map<String, String> bizParams = new TreeMap<>(StringUtils::compare);
        bizParams.put(GRANT_TYPE, GRATE_TYPE_CODE);
        bizParams.put(CODE, alipayProperties.getAppId());
        params.put(BIZ_CONTENT, om.writeValueAsString(bizParams));
        params.put(METHOD, METHOD_VAL);
        params.put(CHARSET, alipayProperties.getCharset());
        params.put(SIGN_TYPE, alipayProperties.getSignType());
        params.put(TIMESTAMP, "2022-06-16 10:58:53");
        params.put(VERSION, alipayProperties.getVersion());
        StringJoiner sj = new StringJoiner("&");
        params.forEach(
                (k, v) -> {
                    sj.add(k + "=" + v);
                });
        String content = sj.toString();
        String s = CommonUtils.RSA2048Sign(content, alipayProperties.getAppPrivateKey());
        System.out.println(s);
    }
}
