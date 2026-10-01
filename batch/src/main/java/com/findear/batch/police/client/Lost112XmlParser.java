package com.findear.batch.police.client;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lost112 응답 한 페이지(XML 본문 하나)를 파싱한다. 외부 입력이므로 DOCTYPE·외부 엔티티를 거부한다 (XXE 방지).
 * <ul>
 *   <li>루트 {@code response}, resultCode {@code 00} → 정상, {@code 03}(NODATA) → 빈 페이지, 그 밖 → 실패</li>
 *   <li>루트 {@code OpenAPI_ServiceResponse}(게이트웨이 오류: 키 미등록·트래픽 초과 등) → returnReasonCode·returnAuthMsg로 실패</li>
 *   <li>XML이 아니거나 구조가 다르면 실패</li>
 * </ul>
 */
@Component
public class Lost112XmlParser {

    static final String RESULT_OK = "00";
    static final String RESULT_NO_DATA = "03";

    public Lost112Page parse(byte[] xml) {

        Document document = readDocument(xml);
        Element root = document.getDocumentElement();

        switch (root.getTagName()) {
            case "response":
                return parseResponse(root);
            case "OpenAPI_ServiceResponse":
                throw gatewayError(root);
            default:
                throw new Lost112Exception("응답 구조가 다릅니다: 루트 <" + root.getTagName() + ">");
        }
    }

    private Lost112Page parseResponse(Element root) {

        Element header = firstChild(root, "header");
        String resultCode = text(header, "resultCode");
        if (resultCode == null) {
            throw new Lost112Exception("응답 구조가 다릅니다: resultCode 없음");
        }

        if (RESULT_NO_DATA.equals(resultCode)) {
            return Lost112Page.empty();
        }
        if (!RESULT_OK.equals(resultCode)) {
            // 명세 표기는 resultMag, 표준은 resultMsg — 둘 다 본다
            String message = text(header, "resultMsg");
            if (message == null) {
                message = text(header, "resultMag");
            }
            throw new Lost112Exception("resultCode " + resultCode + (message == null ? "" : " " + message));
        }

        Element body = firstChild(root, "body");
        List<Map<String, String>> items = new ArrayList<>();
        Element itemsElement = firstChild(body, "items");
        if (itemsElement != null) {
            for (Element item : children(itemsElement, "item")) {
                Map<String, String> values = new LinkedHashMap<>();
                for (Element field : children(item, null)) {
                    values.put(field.getTagName(), field.getTextContent() == null ? null : field.getTextContent().trim());
                }
                items.add(values);
            }
        }

        return new Lost112Page(toInteger(text(body, "pageNo")), toInteger(text(body, "numOfRows")),
                toLong(text(body, "totalCount")), items);
    }

    private Lost112Exception gatewayError(Element root) {

        Element header = firstChild(root, "cmmMsgHeader");
        String code = text(header, "returnReasonCode");
        String authMessage = text(header, "returnAuthMsg");
        String errMessage = text(header, "errMsg");
        if (code == null && authMessage == null && errMessage == null) {
            return new Lost112Exception("응답 구조가 다릅니다: 게이트웨이 오류 형식");
        }
        return new Lost112Exception("게이트웨이 오류 " + (code == null ? "" : code + " ")
                + (authMessage != null ? authMessage : errMessage == null ? "" : errMessage));
    }

    private Document readDocument(byte[] xml) {

        if (xml == null || xml.length == 0) {
            throw new Lost112Exception("빈 응답");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler()); // 기본 핸들러는 stderr에 출력하므로 치명 오류만 예외로 올리는 핸들러로 바꾼다
            return builder.parse(new InputSource(new ByteArrayInputStream(xml)));
        } catch (Exception e) {
            // 파서 메시지에는 본문 일부가 들어갈 수 있어 종류만 남긴다
            throw new Lost112Exception("XML로 읽을 수 없는 응답입니다 (" + e.getClass().getSimpleName() + ")");
        }
    }

    private static Element firstChild(Element parent, String tag) {
        if (parent == null) {
            return null;
        }
        List<Element> found = children(parent, tag);
        return found.isEmpty() ? null : found.get(0);
    }

    /** tag가 null이면 모든 자식 요소 */
    private static List<Element> children(Element parent, String tag) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && (tag == null || tag.equals(element.getTagName()))) {
                result.add(element);
            }
        }
        return result;
    }

    /** 자식 요소의 텍스트 (앞뒤 공백 제거). 없거나 비어 있으면 null */
    private static String text(Element parent, String tag) {
        Element child = firstChild(parent, tag);
        if (child == null || child.getTextContent() == null) {
            return null;
        }
        String value = child.getTextContent().trim();
        return value.isEmpty() ? null : value;
    }

    private static Integer toInteger(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long toLong(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
