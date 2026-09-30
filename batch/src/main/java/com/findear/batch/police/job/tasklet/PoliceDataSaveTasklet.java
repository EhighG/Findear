package com.findear.batch.police.job.tasklet;

import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.police.repository.PoliceAcquiredDataRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.transaction.Transactional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Transactional
@RequiredArgsConstructor
@Component
@Slf4j
public class PoliceDataSaveTasklet implements Tasklet, StepExecutionListener {

    private final PoliceAcquiredDataRepository policeAcquiredDataRepository;

    @Value("${my.secret-key}")
    private String secretKey;

    @Override
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("경찰청 데이터 저장 Start!");
    }

    @Override
    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {

        log.info("경찰청 데이터 저장 End!");

        return ExitStatus.COMPLETED;
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        //경찰청 데이터 저장 로직
        // elastic search 모든 데이터 삭제
        policeAcquiredDataRepository.deleteAll();
        log.info("데이터 삭제 성공");

        String startDate = "20150101";
        LocalDateTime today = LocalDateTime.now();
        String todaysDate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        log.info(todaysDate + "까지 데이터 저장");

        String numOfRows = "30000";

        BufferedReader rd;

        List<String> responses = new ArrayList<>();

        // 경찰청 산하 습득물 데이터 처리
        int count = 0;
        for(int pageNo = 1; ; pageNo++) {

            /*URL*/
            String urlBuilder = "http://apis.data.go.kr/1320000/LosPtfundInfoInqireService/getPtLosfundInfoAccToClAreaPd" + "?" + URLEncoder.encode("serviceKey", StandardCharsets.UTF_8) + "=" + secretKey + /*Service Key*/
                    "&" + URLEncoder.encode("pageNo", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(String.valueOf(pageNo), StandardCharsets.UTF_8) + /*페이지번호*/
                    "&" + URLEncoder.encode("numOfRows", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(numOfRows, StandardCharsets.UTF_8) + /*한 페이지 결과 수*/
                    "&" + URLEncoder.encode("PRDT_CL_CD_01", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*대분류*/
                    "&" + URLEncoder.encode("PRDT_CL_CD_02", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*중분류*/
                    "&" + URLEncoder.encode("CLR_CD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*습득물 색상*/
                    "&" + URLEncoder.encode("START_YMD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(startDate, StandardCharsets.UTF_8) + /*검색시작일*/
                    "&" + URLEncoder.encode("END_YMD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(todaysDate, StandardCharsets.UTF_8) + /*검색종료일*/
                    "&" + URLEncoder.encode("N_FD_LCT_CD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8); /*습득지역*/

            URL url = new URL(urlBuilder);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");

            System.out.println("Response code: " + conn.getResponseCode());

            if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            boolean isEnd = false;
            String line;
            while ((line = rd.readLine()) != null) {

                if(!(line.contains("atcId")) && count == 0) {
                    count++;
                    continue;
                }

                if(!(line.contains("atcId")) && count == 1) {
                    isEnd = true;
                    break;
                }

                responses.add(line);
            }

            rd.close();
            conn.disconnect();

            if(isEnd) {
                break;
            }
        }


        // 경찰청 습득물 데이터 처리
        count = 0;
        for(int pageNo = 1; ; pageNo++) {

            /*URL*/
            String urlBuilder = "http://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd" + "?" + URLEncoder.encode("serviceKey", StandardCharsets.UTF_8) + "=" + secretKey + /*Service Key*/
                    "&" + URLEncoder.encode("pageNo", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(String.valueOf(pageNo), StandardCharsets.UTF_8) + /*페이지번호*/
                    "&" + URLEncoder.encode("numOfRows", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(numOfRows, StandardCharsets.UTF_8) + /*한 페이지 결과 수*/
                    "&" + URLEncoder.encode("PRDT_CL_CD_01", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*대분류*/
                    "&" + URLEncoder.encode("PRDT_CL_CD_02", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*중분류*/
                    "&" + URLEncoder.encode("CLR_CD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8) + /*습득물 색상*/
                    "&" + URLEncoder.encode("START_YMD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(startDate, StandardCharsets.UTF_8) + /*검색시작일*/
                    "&" + URLEncoder.encode("END_YMD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(todaysDate, StandardCharsets.UTF_8) + /*검색종료일*/
                    "&" + URLEncoder.encode("N_FD_LCT_CD", StandardCharsets.UTF_8) + "=" + URLEncoder.encode("", StandardCharsets.UTF_8); /*습득지역*/

            URL url = new URL(urlBuilder);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-type", "application/json");

            System.out.println("Response code: " + conn.getResponseCode());

            if(conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
                rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            }

            boolean isEnd = false;
            String line;
            while ((line = rd.readLine()) != null) {

                if(!(line.contains("atcId")) && count == 0) {
                    count++;
                    continue;
                }

                if(!(line.contains("atcId")) && count == 1) {
                    isEnd = true;
                    break;
                }

                responses.add(line);
            }


            rd.close();
            conn.disconnect();

            if(isEnd) {
                break;
            }
        }

        // 업로드 및 lastTrs 업데이트 실행
        readandSaveData(responses);


        return RepeatStatus.FINISHED;
    }

    public void readandSaveData(List<String> responses) {

        try {

            Long id = 1L;

            List<PoliceAcquiredData> policeAcquiredDataList = new ArrayList<>();

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();

            for (String response : responses) {

                // String -> document 변환
                Document xmlDoc = builder.parse(new ByteArrayInputStream(response.getBytes()));
                xmlDoc.getDocumentElement().normalize();

                Element root = xmlDoc.getDocumentElement();
                NodeList items = root.getElementsByTagName("item");

                // 데이터 읽어오기
                int length = items.getLength();

                String[] targetTagNames = {"atcId", "depPlace", "fdFilePathImg",
                        "fdPrdtNm", "fdSbjt", "fdYmd", "prdtClNm"};

                for (int i = 0; i < length; i++) {

                    PoliceAcquiredData policeAcquiredData;

                    Element item = (Element) items.item(i);
                    String[] rowData = new String[7];


                    for (int j = 0; j < targetTagNames.length; j++) {
                        Node valueNode = item.getElementsByTagName(targetTagNames[j]).item(0);
                        String value = (valueNode == null) ? null : valueNode.getTextContent();

                        // null값 있는(잘못된) 데이터는 pass
                        if (value == null) continue;

                        rowData[j] = value.trim();

                    }

                    String[] array;
                    String fdSbjt = "";
                    String clrNm = "";


                    // 색상 컬럼 분류 로직
                    if(rowData[4] != null) {
                        fdSbjt = rowData[4];

                        // '색'이란 단어를 기준으로 문자열 분할
                        String[] parts = rowData[4].split("색");

                        if(parts.length == 1) {
                            clrNm = null;
                        }
                        else {

                            // 분할된 문자열 중 마지막 부분을 선택하여 '색상' 추출
                            String lastPart = parts[parts.length - 2];

                            List<Integer> indexs = new ArrayList<>();
                            for(int j=0; j<lastPart.length(); j++) {
                                if(lastPart.charAt(j) == '(') {
                                    indexs.add(j);
                                }
                            }

                            if(indexs.size() < 2) {

                                clrNm = null;
                            } else {

                                String color = lastPart.substring(indexs.get(indexs.size()-2) + 1, indexs.get(indexs.size()-1));
                                clrNm = color;
                            }
                        }
                    }


                    // 대분류, 소분류 컬럼 분류 로직
                    String prdtClNm = rowData[6];
                    array = prdtClNm.split(" ");
                    String mainPrdtClNm = array[0];

                    String subPrdtClNm;
                    if(array.length != 3) {
                        subPrdtClNm = null;
                    }
                    else {

                        subPrdtClNm = array[2];
                    }

                    policeAcquiredData = new PoliceAcquiredData(id++,
                            rowData[0], rowData[1], rowData[2], rowData[3], rowData[4],
                            clrNm, rowData[5], rowData[6], mainPrdtClNm, subPrdtClNm);

                    policeAcquiredDataList.add(policeAcquiredData);

                }

                policeAcquiredDataRepository.saveAll(policeAcquiredDataList);

                policeAcquiredDataList.clear();

                log.info("데이터 저장 완료");

            }

            log.info("모든 데이터 저장 완료");

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

}