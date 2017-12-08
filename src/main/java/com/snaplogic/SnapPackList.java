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
 * Created by gaian on 21/4/16.
 */
public class SnapPackList {

    final static Logger logger = Logger.getLogger(SnapPackList.class);

   /* public static void main(String args[]) throws Exception {

      //new SnapPackList().makeLogin("automation@snaplogic.com", "snap@12345", "/home/gaian/Desktop/Snapstats.xlsx", "https://canary.elastic.snaplogic.com");
       new SnapPackList().makeLogin("pnarayan@snaplogic.com", "AnyaMilen924!", "/home/gaian/Desktop/Snapstats.xlsx", "https://elastic.snaplogic.com");
    }
*/
    public void makeLogin(String username, String password, String outputFilePath, String baseURL) throws Exception {
        String token;
        RestAssured restAssured = new RestAssured();
        restAssured.baseURI = baseURL;

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
        int count = 0;
        Set<String> keySet = orgList.keySet();
        for (String key : keySet) {
            List<String> phaseOrgs = new ArrayList<String>();
                phaseOrgs.add(JsonPath.read(orgList.get(key),"snode_id").toString()+":"+JsonPath.read(orgList.get(key),"name").toString());

            //String orgName = JsonPath.read(orgList.get(key),"name");
            for(String org : phaseOrgs){
            	Thread.sleep(2000);
                System.out.println("Fetching info of org = "+org);
                String orgSnode = org.split(":")[0];
                String orgName = org.split(":")[1];
                if(count%10 == 0) {
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
                        .get("api/1/rest/admin/snappack/list/" + orgSnode);
                if(res.getStatusCode() != 200 || res.getStatusCode() != 201){
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
                    res = restAssured.given().header(authHeader).header(accept).header(acceptLanguage).header(acceptEncoding)
                            .get("api/1/rest/admin/snappack/list/" + orgSnode);
                }
                String resString = res.asString();
                if(res.statusCode() == 200){
                    System.out.println("Fetching snappacks of org  "+org);
                    JSONArray responseMap = JsonPath.read(resString, "$.response_map.packs");
                    res = restAssured.given().header(authHeader).header(accept).header(acceptLanguage).header(acceptEncoding)
                            .get("api/2/"+orgSnode+"/rest/snap_stats");
                    if(res.getStatusCode() != 200 || res.getStatusCode() != 201){
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
                        res = restAssured.given().header(authHeader).header(accept).header(acceptLanguage).header(acceptEncoding)
                                .get("api/2/"+orgSnode+"/rest/snap_stats");
                    }
                    for (int j = 0; j < responseMap.size(); j++) {
                        Map<String, String> dataMap = new HashMap<String, String>();
                        JSONArray snapList = JsonPath.read(responseMap.get(j), "snap_schemas");
                        dataMap.put(Constants.ORG, orgName);
                        dataMap.put(Constants.SNAPCOUNT, snapList.size()+"");
                        dataMap.put(Constants.VERSION, JsonPath.read(responseMap.get(j), "class_build_tag").toString());
                        dataMap.put(Constants.SNAPPACKNAME, JsonPath.read(responseMap.get(j), "snap_pack_label").toString());
                        dataMap.put(Constants.SNAPPACKTYPE, JsonPath.read(responseMap.get(j), "snap_pack_access_type").toString());
                        String snapListString = res.asString();
                        if(res.statusCode() == 200){
                            LinkedHashMap snapListResponse = JsonPath.read(snapListString, "$.response_map");
                            int pipelineCount = 0;
                            Set<String> keyset = snapListResponse.keySet();
                            for(String tempKey : keyset){
                                if(JsonPath.read(snapListResponse.get(tempKey), "snap_pack_label").equals(dataMap.get(Constants.SNAPPACKNAME))){
                                    pipelineCount += ((JSONArray)JsonPath.read(snapListResponse.get(tempKey), "entries")).size();
                                }
                            }
                            dataMap.put(Constants.PIPELINESCOUNT, pipelineCount+"");
                        }else{
                            dataMap.put(Constants.PIPELINESCOUNT, res.asString());
                        }
                        orgLevelData.add(dataMap);
                    }
                }else{
                    Map<String, String> dataMap = new HashMap<String, String>();
                    dataMap.put(Constants.ORG, orgName);
                    dataMap.put(Constants.PIPELINESCOUNT, resString);
                    orgLevelData.add(dataMap);
                }
                count++;
            }
        }
        storeDataIntoExcelFile(orgLevelData, outputFilePath);
    }
    public void storeDataIntoExcelFile(List<Map<String, String>> orgData, String outputFilePath) throws IOException {
        File file = new File(outputFilePath);
        HSSFWorkbook workbook = null;
        if(file.exists()){
            FileInputStream fileInputStream = new FileInputStream(file);
            workbook = new HSSFWorkbook(fileInputStream);
        }else{
            workbook = new HSSFWorkbook();
        }
        HSSFSheet sheet = workbook.createSheet("Snap Pack Details");
        List<String> headers = new ArrayList<String>();
        headers.add(Constants.ORG);
        headers.add(Constants.SNAPPACKNAME);
        headers.add(Constants.VERSION);
        headers.add(Constants.SNAPPACKTYPE);
        headers.add(Constants.SNAPCOUNT);
        headers.add(Constants.PIPELINESCOUNT);

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
            System.out.println("Snappack test completed");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}