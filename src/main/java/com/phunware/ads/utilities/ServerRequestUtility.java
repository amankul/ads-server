package com.phunware.ads.utilities;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

public class ServerRequestUtility {

  // Initiating Logger Object
  private static final Logger log = LogManager.getLogger();
  private static String randomValue;

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
                System.getProperty("user.dir") + "/src/main/java/com/phunware/ads/json/" + fileName)
            .replaceAll("DealIDAutomation", "DealIDAutomation" + randomValue);

    // Printing Request Details
    log.info("REQUEST-URL:POST-" + requestURL);
    log.info("REQUEST-BODY:" + requestBody);

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
    log.info("RESPONSE:" + response.asString());
    log.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");

    // Capturing data from response
    String winNotifyUrl =
        response.then().extract().path("seatbid.bid.nurl").toString().replaceAll("[\\[\\]]", "");
    String clickAndImpressionURL = response.then().extract().path("seatbid.bid.adm").toString();

    // parsing data
    HashMap<String, String> result = new HashMap<>();
    result.put("winNotifyUrl", winNotifyUrl);
    log.debug("Win Notify URL -" + winNotifyUrl);

    Matcher regexMatcher =
        Pattern.compile(".+?(http.+?/click.*?)\".*?http.*?\"(http.?://.+?/impression.*?)\"")
            .matcher(clickAndImpressionURL);
    if (regexMatcher.find()) {
      result.put("clickURL", regexMatcher.group(1));
      log.debug("click URL -" + regexMatcher.group(1));
      result.put("impressionURL", regexMatcher.group(2));
      log.debug("impression URL -" + regexMatcher.group(2));
    }

    return result;
  }

  public static int postBidRequest_NoSucessCheck(String runtimeEndPoint, String fileName) {

    // Request Details
    String requestURL = runtimeEndPoint;

    randomValue =
        ServerRequestUtility.readDataFromPropertiesFile(
            "randomValue",
            System.getProperty("user.dir") + "/src/main/resources/random.Properties");

    String requestBody =
        JsonUtilities.jsonToString(
                System.getProperty("user.dir") + "/src/main/java/com/phunware/ads/json/" + fileName)
            .replaceAll("DealIDAutomation", "DealIDAutomation" + randomValue);

    // Printing Request Details
    log.info("REQUEST-URL:POST-" + requestURL);
    log.info("REQUEST-BODY:" + requestBody);

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
    log.info("RESPONSE:" + response.asString());
    log.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");

    return response.statusCode();
  }

  public static void pullDataToAdsServer(String serviceEndPoint) {
    AdsServerUtility.logInToServerExecuteShellCommandAndReturnResponse(
        serviceEndPoint, "touch /var/phunware/dsp/data/placements/v1/placements.md5");
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

  public static String waitForLogsToGetPopulated(String serviceEndPoint, String command) {

    int retry = 6;
    int count = 0;
    String data = "";

    while (retry >= count) {
      log.info("Waiting for abm-dsp-srv.log to get populated with  data ..");
      ServerRequestUtility.wait(5);
      // looking for String "Placement: `placementID` Constraint: BudgetConstraint is INVALID"
      data =
          AdsServerUtility.logInToServerExecuteShellCommandAndReturnResponse(
              serviceEndPoint, command);
      count++;
      if (data.length() > 10) {
        log.info("Found Data --> " + data);
        return data;
      }
    }
    return "No data found for placement in /var/phunware/dsp/logs/abm-dsp-srv.log";
  }

  public static void invokeDataGenerator(String serviceEndPoint, String auth) {

    log.info("Invoking Data Generator");

    String requestURL = serviceEndPoint + "/api/v1.0/datagenerator";

    // Printing Request Details
    log.debug("REQUEST-URL:POST-" + requestURL);

    // Extracting response after status code validation
    Response response =
        given()
            .header("Authorization", auth)
            .request()
            .get(requestURL)
            .then()
            .statusCode(200)
            .extract()
            .response();

    // printing response
    log.info("DG Run Complete - " + response.asString());
    log.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");
  }

  public static void writePropertyFile(
      String key,
      String value,
      String key1,
      String value1,
      String key2,
      String value2,
      String key3,
      String value3,
      String filePath) {

    Properties prop = new Properties();
    OutputStream output = null;

    try {
      output = new FileOutputStream(filePath);
      prop.setProperty(key, value);
      prop.setProperty(key1, value1);
      prop.setProperty(key2, value2);
      prop.setProperty(key3, value3);
      prop.store(output, key + "," + key1 + "," + key2 + "," + key3 + " --> Data");
      output.close();
    } catch (FileNotFoundException ex) {
      // file does not exist
      ex.printStackTrace();
    } catch (IOException ex) {
      // I/O error
      ex.printStackTrace();
    }
  }

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

  public static void hitImpressionURL(String url, int numOfHits) {

    log.info("Hitting URL - " + url);
    log.info(numOfHits + " Times");

    for (int i = 0; i < numOfHits; i++) {
      given().header("Content-Type", "application/json").request().get(url).then().statusCode(204);
    }

    log.info("Impression URL is hit " + numOfHits + " Times");
  }

  public static void waitForDataGenerator(String serviceEndPoint, String auth) {

    // Invoking DG
    ServerRequestUtility.invokeDataGenerator(serviceEndPoint, auth);

    // Waiting for the Dg to pick up updated data
    ServerRequestUtility.pullDataToAdsServer(serviceEndPoint);
    log.info("Waiting for the data generator, Giving it time to update: Wait Time 3.5 minutes");
    ServerRequestUtility.wait(210);
  }
}
