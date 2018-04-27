package com.snaplogic;

import com.jayway.jsonpath.JsonPath;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.response.Response;
import net.minidev.json.JSONArray;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by gaian on 24/11/17.
 */
public class Insights {

   // final static Logger logger = Logger.getLogger(Insights.class);

    public void makeLogin(String username, String password, String outputFilePath, String baseURL) throws Exception {
        String token;
        RestAssured restAssured = new RestAssured();
        restAssured.baseURI = baseURL;

        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, -1095);
        Date start = c.getTime();
        c.add(Calendar.DATE, 1095);
        Date end = c.getTime();

        String authString = username + ":" + password;
        System.out.println("auth string: " + authString);
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);
        System.out.println("Base64 encoded auth string: " + authStringEnc);

        Response res = restAssured.given().header("Authorization", "Basic " + authStringEnc)
                .get("/api/1/rest/asset/session?caller=" + username);
        String json = res.asString();

        token = JsonPath.read(json, "$.response_map.token");
        if (null == token || token.length() == 0) {
            throw new Exception("Failed to login");
        }
        token = "SLToken " + token;
        Header authHeader = new Header("Authorization", token);
        Header accept = new Header("Accept", "application/json, text/javascript, */*; q=0.01");
        Header acceptLanguage = new Header("Accept-Language", "en-US,en;q=0.8");
        Header acceptEncoding = new Header("Accept-Encoding", "gzip, deflate, sdch");
        res = restAssured.given().header(authHeader).header(accept).header(acceptLanguage).header(acceptEncoding)
                .get("api/1/rest/asset/user/" + username);

        String orgListResponse = res.asString();
        LinkedHashMap<String, Map<String,String>> orgList = JsonPath.read(orgListResponse, "$.response_map.org_snodes");

        List<Map<String, String>> orgLevelData = new ArrayList<Map<String, String>>();

        Set<String> keySet = orgList.keySet();
        for (String key : keySet) {
            List<String> phaseOrgs = new ArrayList<String>();
            phaseOrgs.add(JsonPath.read(orgList.get(key),"snode_id").toString()+":"+JsonPath.read(orgList.get(key),"name").toString());
            int count = 0;
            //String orgName = JsonPath.read(orgList.get(key),"name");
            for(String org : phaseOrgs){
                Thread.sleep(2000);
               // System.out.println("Fetching info of org = "+org);
                String orgSnode = org.split(":")[0];
                String orgName = org.split(":")[1];
                if(count%40 == 0) {
                    Response login = restAssured.given().header("Authorization", "Basic " + authStringEnc)
                            .get("/api/1/rest/asset/session?caller=" + username);
                    String loginJson = login.asString();
                    token = "";
                    token = JsonPath.read(loginJson, "$.response_map.token");
                    if (null == token || token.length() == 0) {
                        throw new Exception("Failed to login");
                    }
                    token = "SLToken " + token;
                    authHeader = new Header("Authorization", token);
                }
                res = restAssured.given().header(authHeader).header(accept).header(acceptLanguage).header(acceptEncoding)
                        .get("api/2/"+orgSnode+"/rest/insights/summary?start_ts="+start.getTime()+"&end_ts="+end.getTime()+"&overview=false");
                String resString = res.asString();
                if(res.statusCode() == 200){
                    System.out.println("Fetching insights of org  "+org);
                    JSONArray responseMap = JsonPath.read(resString, "$.response_map.*");
                        Map<String, String> dataMap = new HashMap<String, String>();
                        dataMap.put(Constants.ORG, orgName);
                        dataMap.put(Constants.PIPELINESCOUNT, JsonPath.read(responseMap.get(2), "metric_value").toString());
                        dataMap.put(Constants.SNAPCOUNT, JsonPath.read(responseMap.get(8), "metric_value").toString());
                        dataMap.put(Constants.PIPELINE_EXECUTION_COUNT, JsonPath.read(responseMap.get(4), "metric_value").toString());
                        orgLevelData.add(dataMap);
                }else{
                    Map<String, String> dataMap = new HashMap<String, String>();
                    dataMap.put(Constants.ORG, orgName);
                    dataMap.put("Error Msg", resString);
                    orgLevelData.add(dataMap);
                }
                count++;
            }
        }
        String outputSheetName = "Insights Overview";
        storeDataIntoExcelFile(orgLevelData, outputFilePath, outputSheetName);
    }

    public void getSnapCount(String username, String password, String outputFilePath, String baseURL, long startTime, long endTime) throws Exception {
        String token;
        RestAssured restAssured = new RestAssured();
        restAssured.baseURI = baseURL;

        Date date = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int i = c.get(Calendar.DAY_OF_WEEK) - c.getFirstDayOfWeek();
        c.add(Calendar.DATE, -1095);
        Date start = c.getTime();
        c.add(Calendar.DATE, 1095);
        Date end = c.getTime();

        String authString = username + ":" + password;
        System.out.println("auth string: " + authString);
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);
        System.out.println("Base64 encoded auth string: " + authStringEnc);

        Response res = restAssured.given().header("Authorization", "Basic " + authStringEnc)
                .get("/api/1/rest/asset/session?caller=" + username);
        String json = res.asString();

        token = JsonPath.read(json, "$.response_map.token");
        if (null == token || token.length() == 0) {
            throw new Exception("Failed to login");
        }
        token = "SLToken " + token;
        Header authHeader = new Header("Authorization", token);
        Header accept = new Header("Accept", "application/json, text/javascript, */*; q=0.01");
        Header acceptLanguage = new Header("Accept-Language", "en-US,en;q=0.8");
        Header acceptEncoding = new Header("Accept-Encoding", "gzip, deflate, sdch");
        res = restAssured.given().header(authHeader).header(accept).header(acceptLanguage).header(acceptEncoding)
                .get("api/1/rest/asset/user/" + username);

        String orgListResponse = res.asString();
        LinkedHashMap<String, Map<String,String>> orgList = JsonPath.read(orgListResponse, "$.response_map.org_snodes");
        int count = 0;
        List<Map<String, String>> orgLevelData = new ArrayList<Map<String, String>>();
        Set<String> keySet = orgList.keySet();
        for (String key : keySet) {
            List<String> phaseOrgs = new ArrayList<String>();
            phaseOrgs.add(JsonPath.read(orgList.get(key),"snode_id").toString()+":"+JsonPath.read(orgList.get(key),"name").toString());

            //String orgName = JsonPath.read(orgList.get(key),"name");
            for(String org : phaseOrgs){
                Thread.sleep(2000);
               // System.out.println("Fetching info of org = "+org);
                String orgSnode = org.split(":")[0];
                String orgName = org.split(":")[1];
                if(count%40 == 0) {
                    Response login = restAssured.given().header("Authorization", "Basic " + authStringEnc)
                            .get("/api/1/rest/asset/session?caller=" + username);
                    String loginJson = login.asString();
                    token = "";
                    token = JsonPath.read(loginJson, "$.response_map.token");
                    if (null == token || token.length() == 0) {
                        throw new Exception("Failed to login");
                    }
                    token = "SLToken " + token;
                    authHeader = new Header("Authorization", token);
                }
                res = restAssured.given().header(authHeader).header(accept).header(acceptLanguage).header(acceptEncoding)
                        .get("api/2/"+orgSnode+"/rest/insights/snap?start_ts="+startTime+"&end_ts="+endTime);
                String resString = res.asString();
                if(res.statusCode() == 200){
                    System.out.println("Fetching snap insights of org  "+org);
                    JSONArray responseMap = JsonPath.read(resString, "$.response_map.*");
                    for(int j = 0;j < responseMap.size();j++) {
                        Map<String, String> dataMap = new HashMap<String, String>();
                        dataMap.put(Constants.ORG, orgName);
                        dataMap.put(Constants.SNAPNAME, JsonPath.read(responseMap.get(j), "id").toString());
                        dataMap.put(Constants.SNAPCOUNT, JsonPath.read(responseMap.get(j), "count").toString());
                        orgLevelData.add(dataMap);
                    }
                }else{
                    Map<String, String> dataMap = new HashMap<String, String>();
                    dataMap.put(Constants.ORG, orgName);
                    dataMap.put("Error Msg", resString);
                    orgLevelData.add(dataMap);
                }
                count++;
            }
        }
        String outputSheetName = "Insights SnapCount";
        storeDataIntoExcelFile(orgLevelData, outputFilePath, outputSheetName);
    }

    public void storeDataIntoExcelFile(List<Map<String, String>> orgData, String outputFilePath, String outputSheetName) throws IOException {
        File file = new File(outputFilePath);
        HSSFWorkbook workbook = null;
        if(file.exists()){
            FileInputStream fileInputStream = new FileInputStream(file);
            workbook = new HSSFWorkbook(fileInputStream);
        }else{
            workbook = new HSSFWorkbook();
        }
        HSSFSheet sheet = workbook.createSheet(outputSheetName);
        List<String> headers = new ArrayList<String>();
        headers.add(Constants.ORG);
        if(outputSheetName.equalsIgnoreCase("Insights Overview")) {
            headers.add(Constants.PIPELINESCOUNT);
            headers.add(Constants.PIPELINE_EXECUTION_COUNT);
        }else if(outputSheetName.equalsIgnoreCase("Insights SnapCount")){
            headers.add(Constants.SNAPNAME);
        }
        headers.add(Constants.SNAPCOUNT);

        HSSFFont font  =  workbook.createFont();
        HSSFCellStyle style = workbook.createCellStyle();
        font.setBold(true);
        style.setFont(font);

        HSSFRow rowZero = sheet.createRow(0);
        for(int h = 0;h < headers.size();h++){
            HSSFCell cell = rowZero.createCell(h);
            cell.setCellValue(headers.get(h));
            cell.setCellStyle(style);
        }

        for(int i = 0;i < orgData.size();i++){
            HSSFRow row = sheet.createRow(i+1);
            for(int h = 0;h < headers.size();h++){
                HSSFCell cell = row.createCell(h);
                cell.setCellValue(orgData.get(i).get(headers.get(h)));
            }
        }

        try
        {
            //Write the workbook in file system
            FileOutputStream out = new FileOutputStream(new File(outputFilePath));
            workbook.write(out);
            out.close();
            System.out.println(outputSheetName+" completed");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
