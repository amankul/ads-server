package com.org.ads.utilities;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

public class ServerRequestUtility {

  // Initiating Logger Object
  private static final Logger LOG = LogManager.getLogger();
  private static String randomValue;

  //Send a Bid request, verify status code and capture clickURL, Impression URL, WinNotify URL & Transaction ID in a hask map and return it.
  public static HashMap<String, String> postBidRequest(String runtimeEndPoint, String fileName) {

    // Request Details
    String requestURL = runtimeEndPoint;

    // capturing random value generated during placement creation.
    randomValue =
        ServerRequestUtility.readDataFromPropertiesFile(
            "randomValue",
            System.getProperty("user.dir") + "/src/main/resources/random.Properties");

    String requestBody =
        JsonUtilities.jsonToString(
                System.getProperty("user.dir") + "/src/main/java/com/org/ads/json/" + fileName)
            .replaceAll("DealIDAutomation", "DealIDAutomation" + randomValue);

    // Printing Request Details
    LOG.info("REQUEST-URL:POST-" + requestURL);
    LOG.info("REQUEST-BODY:" + requestBody);

    // Extracting response after status code validation
    Response response =
        given()
            .header("Content-Type", "application/json")
            .request()
            .body(requestBody)
            .post(requestURL)
            .then()
            .statusCode(200)
            .extract()
            .response();

    // printing response
    LOG.info("RESPONSE:" + response.asString());
    LOG.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");

    // Capturing data from response
    String winNotifyUrl =
        response.then().extract().path("seatbid.bid.nurl").toString().replaceAll("[\\[\\]]", "");
    String clickAndImpressionURL = response.then().extract().path("seatbid.bid.adm").toString();
    String transactionID =
        response.then().extract().path("seatbid.bid.id").toString().replaceAll("[\\[\\]]", "");

    // parsing data
    HashMap<String, String> result = new HashMap<>();
    result.put("winNotifyUrl", winNotifyUrl);
    LOG.debug("Win Notify URL -" + winNotifyUrl);
    result.put("transactionID", transactionID);
    LOG.debug("transactionID -" + transactionID);

    Matcher regexMatcher =
        Pattern.compile(".+?(http.+?/click.*?)\".*?http.*?\"(http.?://.+?/impression.*?)\"")
            .matcher(clickAndImpressionURL);
    if (regexMatcher.find()) {
      result.put("clickURL", regexMatcher.group(1));
      LOG.debug("click URL -" + regexMatcher.group(1));
      result.put("impressionURL", regexMatcher.group(2));
      LOG.debug("impression URL -" + regexMatcher.group(2));
    }

    return result;
  }

  //Send a bid request
  public static int postBidRequest_NoSucessCheck(String runtimeEndPoint, String fileName) {

    // Request Details
    String requestURL = runtimeEndPoint;

    randomValue =
        ServerRequestUtility.readDataFromPropertiesFile(
            "randomValue",
            System.getProperty("user.dir") + "/src/main/resources/random.Properties");

    String requestBody =
        JsonUtilities.jsonToString(
                System.getProperty("user.dir") + "/src/main/java/com/org/ads/json/" + fileName)
            .replaceAll("DealIDAutomation", "DealIDAutomation" + randomValue);

    // Printing Request Details
    LOG.info("REQUEST-URL:POST-" + requestURL);
    LOG.info("REQUEST-BODY:" + requestBody);

    // Extracting response after status code validation
    Response response =
        given()
            .header("Content-Type", "application/json")
            .request()
            .body(requestBody)
            .post(requestURL)
            .then()
            .extract()
            .response();

    // printing response
    LOG.info("RESPONSE:" + response.asString());
    LOG.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");

    return response.statusCode();
  }

  //Pull data from DB to ADS server
  public static void pullDataToAdsServer(String serviceEndPoint) {
    AdsServerUtility.logInToServerExecuteShellCommandAndReturnResponse(
        serviceEndPoint, "touch /var/org/dsp/data/placements/v1/placements.md5");
  }

  public static void writeToFile(String data, String filename) {

    try {
      File myFile = new File(filename);
      FileOutputStream pemFileStream = new FileOutputStream(myFile, false);
      pemFileStream.write(data.getBytes());
      pemFileStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void wait(int timeInSeconds) {
    try {
      Thread.sleep(timeInSeconds * 1000);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  //Waiting for the server logs to get populated after sending a shell command.
  public static String waitForLogsToGetPopulated(String serviceEndPoint, String command) {

    int retry = 6;
    int count = 0;
    String data = "";

    while (retry >= count) {
      LOG.info("Waiting for abm-dsp-srv.log to get populated with  data ..");
      ServerRequestUtility.wait(5);
      // looking for String "Placement: `placementID` Constraint: BudgetConstraint is INVALID"
      data =
          AdsServerUtility.logInToServerExecuteShellCommandAndReturnResponse(
              serviceEndPoint, command);
      count++;
      if (data.length() > 10) {
        LOG.info("Found Data --> " + data);
        return data;
      }
    }
    return "No data found for placement in /var/org/dsp/logs/abm-dsp-srv.log";
  }

  //Invoke Data Generator via API call
  public static void invokeDataGenerator(String serviceEndPoint, String auth) {

    LOG.info("Invoking Data Generator");
    String requestURL = serviceEndPoint + "/api/v1.0/datagenerator";

    // Printing Request Details
    LOG.info("REQUEST-URL:POST-" + requestURL);

    // Extracting response after status code validation
    Response response =
        given()
            .header("Authorization", auth)
            .request()
            .get(requestURL)
            .then()
            .extract()
            .response();

    response.then().statusCode(200);

    // printing response
    LOG.info("DG Run Complete - " + response.asString());
    LOG.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");
  }

  //Writes 5 key value pairs to a property file
  public static void writePropertyFile(
      String key,
      String value,
      String key1,
      String value1,
      String key2,
      String value2,
      String key3,
      String value3,
      String key4,
      String value4,
      String filePath) {

    Properties prop = new Properties();
    OutputStream output = null;

    try {
      output = new FileOutputStream(filePath);
      prop.setProperty(key, value);
      prop.setProperty(key1, value1);
      prop.setProperty(key2, value2);
      prop.setProperty(key3, value3);
      prop.setProperty(key4, value4);
      prop.store(output, key + "," + key1 + "," + key2 + "," + key3 + "," + key4 + "   --> Data");
      output.close();
    } catch (FileNotFoundException ex) {
      // file does not exist
      ex.printStackTrace();
    } catch (IOException ex) {
      // I/O error
      ex.printStackTrace();
    }
  }

  //reads data from a property file
  public static String readDataFromPropertiesFile(String propertyName, String filePath) {

    Properties pro = null;

    try {
      pro = new Properties();
      FileInputStream fi = new FileInputStream(filePath);
      pro.load(fi);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return pro.getProperty(propertyName);
  }

  //Hit URL using OkHttpClient
  public static void hitURL(String url, int numOfHits) {

    LOG.info("Hitting URL - " + url);
    for (int i = 0; i < numOfHits; i++) {
      OkHttpClient client = new OkHttpClient();
      Request request = new Request.Builder().url(url).get().build();
      try {
        client.newCall(request).execute();
        LOG.info("URL hit " + i + "Times");
      } catch (IOException e) {
        e.printStackTrace();
      }
      ServerRequestUtility.sleep(3);
    }
  }

  //Hit Conversion URL using OkHttpClient
  public static void hitConversionURL(String transactionID, String runtimeEndPoint, int numOfHits) {

    String url = runtimeEndPoint.replaceAll("bidRequest", "conversion?tx=" + transactionID);
    LOG.info("Hitting Conversion URL - " + url);

    for (int i = 0; i < numOfHits; i++) {
      OkHttpClient client = new OkHttpClient();
      Request request = new Request.Builder().url(url).get().build();
      try {
        client.newCall(request).execute();
        LOG.info("URL hit " + i + "Times");
      } catch (IOException e) {
        e.printStackTrace();
      }
      ServerRequestUtility.sleep(3);
    }
  }

  //Wait for the data generator to pull data to Ads Server
  public static void waitForDataGenerator(String serviceEndPoint, String auth) {

    // Invoking DG
    ServerRequestUtility.invokeDataGenerator(serviceEndPoint, auth);

    // Waiting for the DG to pick up updated data
    ServerRequestUtility.pullDataToAdsServer(serviceEndPoint);
    LOG.info("Waiting for the data generator, Giving it time to update: Wait Time 4 minutes");
    ServerRequestUtility.wait(240);
  }

  // Sleep
  public static void sleep(int seconds) {
    LOG.info("Wait time -" + seconds + " Seconds");

    try {
      Thread.sleep(seconds * 1000);
    } catch (InterruptedException ex) {
    } catch (IllegalArgumentException ex) {
    }
  }

  //get Placement data from Elastic searc using reporting API endpoint
  public static Response getPlacementDataFromELasticSearch(
      String serviceEndPoint, String placementID, String auth) {

    // Using reporting API
    Long startOfToday = LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toEpochSecond() * 1000;
    Long endOfToday =
        LocalDate.now().plusDays(1).atStartOfDay(ZoneId.of("UTC")).toEpochSecond() * 1000;

    String url =
        serviceEndPoint
            + "/api/v1.0/metrics/placements?pids="
            + placementID
            + "&stdt="
            + startOfToday
            + "&endt="
            + endOfToday;

    LOG.info("URL -" + url);

    Response response =
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", auth)
            .request()
            .get(url)
            .then()
            .extract()
            .response();

    LOG.info("Captured ES data - " + response.asString());
    return response;
  }

  public static String getLogData(String serviceEndPoint, String placementID) {
    return AdsServerUtility.logInToServerExecuteShellCommandAndReturnResponse(
            serviceEndPoint, "cat /var/org/dsp/logs/abm-dsp-srv.log | grep " + placementID);
  }
}
