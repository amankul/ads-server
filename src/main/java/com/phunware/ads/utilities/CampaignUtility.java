package com.phunware.ads.utilities;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static io.restassured.RestAssured.given;

public class CampaignUtility {

  // Initiating Logger Object
  private static final Logger LOG = LogManager.getLogger();
  private static String randomvalue;
  private static String campaignID;
  private static ArrayList<String> dbResult;
  private static String startDate;
  private static String endDate;
  private static int customerID;
  private static int advertiserID;

  //Creates new Campaign
  public static String createCampaign(
      String serviceEndPoint, String auth, String campaignRequestEndPoint) {

    initializeData(serviceEndPoint);

    // Request Details
    String requestURL = serviceEndPoint + campaignRequestEndPoint;
    String requestBody =
        JsonUtilities.jsonToString(
                System.getProperty("user.dir")
                    + "/src/main/java/com/phunware/ads/json/campaign.json")
            .replaceAll("customerIdToBeChanged", "" + customerID)
            .replaceAll("advertiserIdToBeChanged", "" + advertiserID)
            .replaceAll("nameToBeChanged", randomvalue)
            .replaceAll("startDateToBeChanged", startDate)
            .replaceAll("endDateToBeChanged", endDate);

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

    // capturing campaign ID
    campaignID = response.then().extract().path("data.id").toString();
    LOG.info("Created New Campaign - ID - " + campaignID);

    return campaignID;
  }

  //Delete Campaign
  public static void deleteCampaign(
      String serviceEndPoint, String campaignRequestEndPoint, String auth, String campaignID) {

    // Request Details
    String requestURL = serviceEndPoint + campaignRequestEndPoint + "/" + campaignID;

    // Delete Campaign

    given()
        .header("Content-Type", "application/json")
        .header("Authorization", auth)
        .request()
        .delete(requestURL)
        .then()
        .statusCode(200)
        .extract()
        .response();

    LOG.info("Deleted Campaign ID - " + campaignID);
  }

  //Update campaign status
  public static void updateCampaign(
      String serviceEndPoint,
      String campaignRequestEndPoint,
      String auth,
      String campaignID,
      String statusID) {

    // Request Details
    String requestURL = serviceEndPoint + campaignRequestEndPoint + "/" + campaignID;

    // Extracting response after status code validation
    Response response =
        given()
            .header("Content-Type", "application/json")
            .header("Authorization", auth)
            .request()
            .body("{\"statusId\": " + statusID + "}")
            .put(requestURL)
            .then()
            .statusCode(200)
            .extract()
            .response();

    LOG.info("Updated Campaign ID - " + campaignID + ", Status Updated to - " + statusID);
  }

  //Update campaign based on the request body sent as one of the parameters
  public static void updateCampaignUsingRequestBody(
      String serviceEndPoint,
      String campaignRequestEndPoint,
      String auth,
      String campaignID,
      String requestBody) {

    // Request Details
    String requestURL = serviceEndPoint + campaignRequestEndPoint + "/" + campaignID;

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

    LOG.info("Updated Campaign ID - " + campaignID);
  }

  //Gets Advertiser ID from DB
  public static int getAdvertiserID(String serviceEndPoint) {
    String sqlQuery = "select id from advertiser where is_active = 1 limit 1";
    dbResult =
        MySqlUtility.query_Post_connection_To_MySQL_Via_JumpServer(sqlQuery, serviceEndPoint);
    return Integer.valueOf(dbResult.get(0));
  }

  //gets Customer ID from DB
  public static int getCustomerID(String serviceEndPoint) {
    String sqlQuery_customer = "select id from customer where status_id not in (100) limit 1";
    dbResult =
        MySqlUtility.query_Post_connection_To_MySQL_Via_JumpServer(
            sqlQuery_customer, serviceEndPoint);
    return Integer.valueOf(dbResult.get(0));
  }

  public static void initializeData(String serviceEndPoint) {

    customerID = getCustomerID(serviceEndPoint);
    dbResult.clear();
    advertiserID = getAdvertiserID(serviceEndPoint);
    dbResult.clear();

    randomvalue = "Campaign" + LocalDateTime.now().toString().replaceAll("[-:.T]", "");

    // current time plus 10 Seconds
    startDate =
        LocalDateTime.now(Clock.systemUTC()).plusSeconds(10).toString().replaceAll("\\..{2,}", "");

    // Current time plus 10 years
    endDate =
        LocalDateTime.now(Clock.systemUTC()).plusYears(10).toString().replaceAll("\\..{2,}", "");
  }
}
