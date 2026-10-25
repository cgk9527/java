package com.ecommerce.common.util;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付宝 RSA2(SHA256withRSA) 签名工具
 * 说明: 应用私钥需为 PKCS8 格式(BEGIN PRIVATE KEY), 支付宝公钥为 X.509 格式(BEGIN PUBLIC KEY)
 */
public final class AlipaySignUtil {

    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    private AlipaySignUtil() {
    }

    /**
     * 生成签名(用于请求支付宝)
     */
    public static String sign(Map<String, String> params, String privateKey, String charset) {
        try {
            String content = buildContent(params);
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(loadPrivateKey(privateKey));
            signature.update(content.getBytes(charset));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("支付宝签名失败", e);
        }
    }

    /**
     * 校验签名(用于校验支付宝异步通知)
     */
    public static boolean verify(Map<String, String> params, String publicKey, String charset) {
        try {
            String sign = params.get("sign");
            if (sign == null || sign.isEmpty()) {
                return false;
            }
            String content = buildContent(params);
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(loadPublicKey(publicKey));
            signature.update(content.getBytes(charset));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 拼接待签名内容: 按键排序, 排除 sign / sign_type, 过滤空值
     */
    private static String buildContent(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            if ("sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            sb.append(key).append('=').append(value).append('&');
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private static PrivateKey loadPrivateKey(String privateKey) throws Exception {
        String b64 = stripHeaders(privateKey);
        byte[] decoded = Base64.getDecoder().decode(b64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static PublicKey loadPublicKey(String publicKey) throws Exception {
        String b64 = stripHeaders(publicKey);
        byte[] decoded = Base64.getDecoder().decode(b64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static String stripHeaders(String key) {
        return key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }
}