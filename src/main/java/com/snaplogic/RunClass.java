package com.snaplogic;


import org.testng.annotations.*;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by gaian on 7/4/16.
 */
public class RunClass {

    static String username = null;
    static String password = null;
    static String outputFilePath = null;
    static String baseURL = null;

    @org.testng.annotations.Test()
    public void runReport() throws Exception {
      /* username = args[0];
        password = args[1];
        //outputFilePath = args[2];
        baseURL = args[2];*/
        username = "sbuddi@snaplogic.com";
        password = "snapLogic@12345";
    	//outputFilePath = "/home/gaian/Desktop/";
    	baseURL = "https://stage.elastic.snaplogic.com";


        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        Date date = new Date();
        outputFilePath = "SnaplexAndInsights.xlsx";
        new Thread() {
            public void run() {
                try {
                    new SnaplexStatistics().makeLogin(username, password,outputFilePath, baseURL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            }.start();
        new Thread() {
            public void run() {
                try {
                        new Insights().makeLogin(username, password, outputFilePath, baseURL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        new Thread() {
            public void run() {
                try {
                    new Insights().getSnapCount(username, password, outputFilePath, baseURL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
        new Thread() {
            public void run() {
                try {
                    new SnapPackList().makeLogin(username, password, outputFilePath, baseURL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
