package com.phunware.ads.utilities;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static io.restassured.RestAssured.given;

public class CreativeUtility {

  // Initiating Logger Object
  private static final Logger LOG = LogManager.getLogger();
  private static String randomvalue;
  private static String creativeID;
  private static ArrayList<String> dbResult;
  private static String advertiserId;

  public static String createCreative(
      String serviceEndPoint, String auth, String creativeRequestEndPoint) {

    initializeData(serviceEndPoint);

    // Request Details
    String requestURL = serviceEndPoint + creativeRequestEndPoint;
    String requestBody =
        JsonUtilities.jsonToString(
                System.getProperty("user.dir")
                    + "/src/main/java/com/phunware/ads/json/creative.json")
            .replaceAll("advertiserIdToBeChanged", advertiserId)
            .replaceAll("nameToBeChanged", randomvalue);

    // Printing Request Details
    LOG.debug("REQUEST-URL:POST-" + requestURL);
    LOG.debug("REQUEST-BODY:" + requestBody);

    // Extracting response after status code validation
    Response response =
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", auth)
            .request()
            .body(requestBody)
            .post(requestURL)
            .then()
            .statusCode(201)
            .extract()
            .response();

    // printing response
    LOG.debug("RESPONSE:" + response.asString());
    LOG.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");

    // capturing created lineItem ID
    creativeID = response.then().extract().path("data.id").toString();
    LOG.info("Created New Creative - ID - " + creativeID);

    return creativeID;
  }

  public static String createJSCreative(
      String serviceEndPoint, String auth, String creativeRequestEndPoint) {

    initializeData(serviceEndPoint);

    // Request Details
    String requestURL = serviceEndPoint + creativeRequestEndPoint;
    String requestBody =
        JsonUtilities.jsonToString(
                System.getProperty("user.dir")
                    + "/src/main/java/com/phunware/ads/json/creativeJS.json")
            .replaceAll("advertiserIdToBeChanged", advertiserId)
            .replaceAll("nameToBeChanged", randomvalue);

    // Printing Request Details
    LOG.debug("REQUEST-URL:POST-" + requestURL);
    LOG.debug("REQUEST-BODY:" + requestBody);

    // Extracting response after status code validation
    Response response =
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", auth)
            .request()
            .body(requestBody)
            .post(requestURL)
            .then()
            .statusCode(201)
            .extract()
            .response();

    // printing response
    LOG.debug("RESPONSE:" + response.asString());
    LOG.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");

    // capturing created lineItem ID
    creativeID = response.then().extract().path("data.id").toString();
    LOG.info("Created New Creative - ID - " + creativeID);

    return creativeID;
  }

  public static void deleteCreative(
      String serviceEndPoint, String creativeRequestEndPoint, String auth, String creativeID) {

    // Request Details
    String requestURL = serviceEndPoint + creativeRequestEndPoint + "/" + creativeID;

    // Printing Request Details
    LOG.debug("REQUEST-URL:DELETE-" + requestURL);

    // Extracting response after status code validation
    Response response =
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", auth)
            .request()
            .delete(requestURL)
            .then()
            .statusCode(200)
            .extract()
            .response();

    LOG.info("Deleted Creative ID - " + creativeID);
  }

  public static void updateCreative(
      String serviceEndPoint,
      String creativeRequestEndPoint,
      String auth,
      String creativeID,
      String requestBody) {

    // Request Details
    String requestURL = serviceEndPoint + creativeRequestEndPoint + "/" + creativeID;

    // Printing Request Details
    LOG.debug("REQUEST-URL:PUT-" + requestURL);

    // Extracting response after status code validation
    Response response =
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", auth)
            .request()
            .body(requestBody)
            .put(requestURL)
            .then()
            .statusCode(200)
            .extract()
            .response();

    LOG.info("Updated Creative ID - " + creativeID + ", With request body - " + requestBody);
  }

  public static void getCreative(
      String serviceEndPoint, String creativeRequestEndPoint, String auth, String creativeID) {

    // Request Details
    String requestURL = serviceEndPoint + creativeRequestEndPoint + "/" + creativeID;

    // Printing Request Details
    LOG.debug("REQUEST-URL:GET-" + requestURL);

    // Extracting response after status code validation
    Response response =
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", auth)
            .request()
            .get(requestURL)
            .then()
            .statusCode(200)
            .extract()
            .response();

    LOG.info(" Creative ID - " + creativeID);
    LOG.info(response.asString());
  }

  public static void initializeData(String serviceEndPoint) {

    randomvalue = "Creative" + LocalDateTime.now().toString().replaceAll("[-:.T]", "");

    // Capturing advertiserId from DB
    String sqlQuery = "select id from advertiser where is_active = 1 limit 1";
    dbResult =
        MySqlUtility.query_Post_connection_To_MySQL_Via_JumpServer(sqlQuery, serviceEndPoint);
    advertiserId = dbResult.get(0);
    dbResult.clear();
  }
}
