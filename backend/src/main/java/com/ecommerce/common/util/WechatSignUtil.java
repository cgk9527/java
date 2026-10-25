package com.ecommerce.common.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 微信支付签名/XML 工具(v2 MD5 签名)
 */
public final class WechatSignUtil {

    private WechatSignUtil() {
    }

    public static String sign(Map<String, String> params, String apiKey) {
        String content = buildSignString(params) + "&key=" + apiKey;
        return md5(content);
    }

    public static boolean verify(Map<String, String> params, String apiKey) {
        String sign = params.get("sign");
        if (sign == null || sign.isEmpty()) {
            return false;
        }
        return sign.equalsIgnoreCase(sign(params, apiKey));
    }

    private static String buildSignString(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            if ("sign".equals(key)) {
                continue;
            }
            sb.append(key).append('=').append(value).append('&');
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("MD5 计算失败", e);
        }
    }

    public static String buildXml(Map<String, String> params) {
        StringBuilder sb = new StringBuilder("<xml>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            sb.append('<').append(entry.getKey()).append("><![CDATA[")
                    .append(value).append("]]></").append(entry.getKey()).append('>');
        }
        sb.append("</xml>");
        return sb.toString();
    }

    public static Map<String, String> parseXml(String xml) {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            NodeList nodes = document.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    map.put(node.getNodeName(), node.getTextContent());
                }
            }
        } catch (Exception ignored) {
            // 解析失败返回空 map
        }
        return map;
    }
}