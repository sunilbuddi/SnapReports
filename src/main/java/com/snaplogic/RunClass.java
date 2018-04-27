package com.snaplogic;




import java.text.SimpleDateFormat;
import java.util.Date;
import org.testng.annotations.Test;

/**
 * Created by gaian on 7/4/16.
 */
public class RunClass {

    static String username = null;
    static String password = null;
    static String outputFilePath = null;
    static String baseURL = null;

    //@Test()
    public static void main(String args[]) throws Exception {
      /* username = args[0];
        password = args[1];
        //outputFilePath = args[2];
        baseURL = args[2];*/
        username = "msangar@snaplogic.com";
        password = "Sn@p2015!!!!";
        //outputFilePath = "/home/gaian/Desktop/";
        baseURL = "https://elastic.snaplogic.com";


        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        Date date = new Date();
        outputFilePath = "SnaplexAndInsights.xlsx";
        /*new SnaplexStatistics().makeLogin(username, password,outputFilePath, baseURL);
        new Insights().makeLogin(username, password, outputFilePath, baseURL);
        new Insights().getSnapCount(username, password, outputFilePath, baseURL);
        new SnapPackList().makeLogin(username, password, outputFilePath, baseURL);*/
       /* new Thread() {
            public void run() {
                try {
                    new SnaplexStatistics().makeLogin(username, password,outputFilePath, baseURL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            }.start();*/
      /*  new Thread() {
            public void run() {
                try {
                        new Insights().makeLogin(username, password, outputFilePath, baseURL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start(); */
        new Thread() {
            public void run() {
                try {
                    new Insights().getSnapCount(username, password, outputFilePath, baseURL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start(); 
       /* new Thread() {
            public void run() {
                try {
                    new SnapPackList().makeLogin(username, password, outputFilePath, baseURL);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start(); */
    }
}
