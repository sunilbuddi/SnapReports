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
import java.text.DecimalFormat;
import java.util.*;

/**
 * Created by gaian on 21/4/16.
 */
public class SnaplexStatistics {

    final static Logger logger = Logger.getLogger(SnaplexStatistics.class);
    /*public static void main(String args[]) throws Exception {
        username = args[0];
        password = args[1];
        outputFilePath = args[2];
        baseURL = args[3];
        makeLogin(username, password, outputFilePath, baseURL);
    }*/

    public void makeLogin(String username, String password, String outputFilePath, String baseURL) throws Exception {
        System.out.println("Entered snaplex statistics class");
        String token;
        RestAssured restAssured = new RestAssured();
        restAssured.baseURI = baseURL;

        String authString = username + ":" + password;
        //System.out.println("auth string: " + authString);
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
        int count = 0;
        for (String key : keySet) {
            List<String> phaseOrgs = new ArrayList<String>();
            if(orgList.get(key).containsKey("phase_snodes")){
                JSONArray phaseOrgsArray = JsonPath.read(orgList.get(key),"phase_snodes");
                for(int a = 0;a < phaseOrgsArray.size();a++){
                    phaseOrgs.add(JsonPath.read(phaseOrgsArray.get(a),"name").toString());
                }
            }else{
                phaseOrgs.add(JsonPath.read(orgList.get(key),"name").toString());
            }
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
            //String orgName = JsonPath.read(orgList.get(key),"name");
            for(String org : phaseOrgs){
                System.out.println("Fetching snaplex info for org : "+org);
                res = restAssured.given().header(authHeader).header(accept).header(acceptLanguage).header(acceptEncoding)
                        .get("api/1/rest/slserver/snaplex_cc_details_with_info?subscriber_id=" + org);
                String resString = res.asString();
                if(res.statusCode() == 200){
                    JSONArray responseMap = JsonPath.read(resString, "$.response_map");


                    for (int j = 0; j < responseMap.size(); j++) {
                        JSONArray running = JsonPath.read(responseMap.get(j), "running");
                        String plexName = JsonPath.read(responseMap.get(j) , "label");
                        if (running != null && running.size() > 0) {

                            for (int k = 0; k < running.size(); k++) {
                                Map<String, String> runList = new HashMap<String, String>();
                                runList.put(Constants.ORG, org);
                                runList.put(Constants.SNAPLEX, plexName);
                                runList.put(Constants.NODENAME, JsonPath.read(running.get(k), "label").toString());

                                if(running.get(k).toString().contains("version")) {
                                    String version = JsonPath.read(running.get(k), "version");
                                    runList.put(Constants.VERSION, version);
                                }

                                if(running.get(k).toString().contains("location")) {
                                    String location = JsonPath.read(running.get(k), "location");
                                    runList.put(Constants.LOCATION, location);
                                }

                                if(running.get(k).toString().contains("active_threads")) {
                                    int activeThreads = JsonPath.read(running.get(k), "stats.active_threads");
                                    runList.put(Constants.ACTIVETHREADS, activeThreads + "");
                                }

                                DecimalFormat df = new DecimalFormat("#.00");
                                if(running.get(k).toString().contains("mem_used_absolute")) {
                                    String memUsedAbsolute = JsonPath.read(running.get(k), "stats.mem_used_absolute") + "";
                                    String memUsedAbsoluteInDouble = df.format(Long.parseLong(memUsedAbsolute) / (1024.00 * 1024.00 * 1024.00));
                                    runList.put(Constants.MEMORYUSED, memUsedAbsoluteInDouble);
                                }

                                if(running.get(k).toString().contains("open_file_descriptors")) {
                                    int openFileDescriptors = JsonPath.read(running.get(k), "stats.open_file_descriptors");
                                    runList.put(Constants.OPENFILEDESCRIPTORS, openFileDescriptors + "");
                                }

                                if(running.get(k).toString().contains("jvm_max_mem_size")) {
                                    long jvmMaxMemSize = Long.parseLong(JsonPath.read(running.get(k), "info_map.jvm_max_mem_size") + "");
                                    String jvmMaxMemSizeInDouble = df.format(jvmMaxMemSize / (1024.00 * 1024.00 * 1024.00));
                                    runList.put(Constants.JVMMEMORY, jvmMaxMemSizeInDouble);
                                }

                                if(running.get(k).toString().contains("max_file_descriptors")) {
                                    int maxFileDescriptors = JsonPath.read(running.get(k), "info_map.max_file_descriptors");
                                    runList.put(Constants.MAXFILEDESCRIPTORS, maxFileDescriptors+"");
                                }

                                if(running.get(k).toString().contains("os_arch")) {
                                    String osArch = JsonPath.read(running.get(k), "info_map.os_arch");
                                    runList.put(Constants.OSARC, osArch);
                                }

                                if(running.get(k).toString().contains("os_name")) {
                                    String osName = JsonPath.read(running.get(k), "info_map.os_name");
                                    runList.put(Constants.OSNAME, osName);
                                }

                                if(running.get(k).toString().contains("os_version")) {
                                    String osVersion = JsonPath.read(running.get(k), "info_map.os_version");
                                    runList.put(Constants.OSVERSION, osVersion);
                                }

                                if(running.get(k).toString().contains("processors")) {
                                    int processors = JsonPath.read(running.get(k), "info_map.processors");
                                    runList.put(Constants.PROCESSORS, processors + "");
                                }

                                if(running.get(k).toString().contains("total_mem_size")) {
                                    long totalMemSize = Long.parseLong(JsonPath.read(running.get(k), "info_map.total_mem_size") + "");
                                    String totalMemSizeInDouble = df.format(totalMemSize / (1024.00 * 1024.00 * 1024.00));
                                    runList.put(Constants.MACHINEMEMORY, totalMemSizeInDouble);
                                }

                                if(running.get(k).toString().contains("java_version")) {
                                    String javaVersion = JsonPath.read(running.get(k), "info_map.jvm_properties.java_version");
                                    runList.put(Constants.JAVAVERSION, javaVersion);
                                }
                                // runList.put(plexName , statusInfo);
                                orgLevelData.add(runList);
                            }

                        }
                    }

                }
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
        HSSFSheet sheet = workbook.createSheet("Snaplex Statistics");
        List<String> headers = new ArrayList<String>();
        headers.add(Constants.ORG);
        headers.add(Constants.SNAPLEX);
        headers.add(Constants.NODENAME);
        headers.add(Constants.PROCESSORS);
        headers.add(Constants.MACHINEMEMORY);
        headers.add(Constants.MEMORYUSED);
        headers.add(Constants.JVMMEMORY);
        headers.add(Constants.JAVAVERSION);
        headers.add(Constants.OSARC);
        headers.add(Constants.OSNAME);
        headers.add(Constants.OSVERSION);
        headers.add(Constants.OPENFILEDESCRIPTORS);
        headers.add(Constants.MAXFILEDESCRIPTORS);
        headers.add(Constants.VERSION);
        headers.add(Constants.LOCATION);

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
            System.out.println("Test Completed");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}