package com.findear.batch.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

import java.io.IOException;

/**
 * 07 §5.2/§5.3 모양의 match 응답을 만드는 테스트 도우미.
 * findear: 요청의 모든 후보에 {@code findearRate}, lost: 모든 후보에 {@code lostRate}를 준다.
 */
public final class MatchResponses {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MatchResponses() {
    }

    public static JsonNode body(RecordedRequest request) {
        try {
            return MAPPER.readTree(request.getBody().utf8());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static long lostBoardId(RecordedRequest request) {
        return body(request).get("lostBoard").get("lostBoardId").asLong();
    }

    public static MockResponse respond(RecordedRequest request, double findearRate, double lostRate) {
        try {
            JsonNode body = body(request);
            String path = request.getUrl().encodedPath();
            ObjectNode response = MAPPER.createObjectNode();
            ArrayNode result = response.putArray("result");
            if (path.equals("/matching/findear")) {
                response.put("message", "ok");
                for (JsonNode candidate : body.get("acquiredBoardList")) {
                    ObjectNode item = result.addObject();
                    item.put("lostBoardId", body.get("lostBoard").get("lostBoardId").asLong());
                    item.put("acquiredBoardId", candidate.get("acquiredBoardId").asLong());
                    item.put("similarityRate", findearRate);
                }
            } else if (path.equals("/matching/lost")) {
                response.put("message", "ok");
                for (JsonNode candidate : body.get("acquiredBoardList")) {
                    ObjectNode item = result.addObject();
                    item.put("lostBoardId", body.get("lostBoard").get("lostBoardId").asLong());
                    item.put("acquiredBoardId", candidate.get("id").asText());
                    item.put("similarityRate", lostRate);
                    for (String key : new String[]{"atcId", "depPlace", "fdFilePathImg", "fdPrdtNm", "fdSbjt", "clrNm", "fdYmd", "mainPrdtClNm"}) {
                        item.set(key, candidate.get(key));
                    }
                }
            } else {
                return MatchMock.json(404, "{\"message\":\"없는 경로\"}");
            }
            return MatchMock.json(200, MAPPER.writeValueAsString(response));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static MockResponse respond(RecordedRequest request) {
        return respond(request, 0.8, 0.7);
    }
}
