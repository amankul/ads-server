package com.phunware.ads.utilities;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;

public class LineItemUtility {

  // Initiating Logger Object
  private static final Logger LOG = LogManager.getLogger();
  private static String randomvalue;
  private static String lineItemID;
  private static String budgetDaily;
  private static String lineItemTypeId;
  private static String defaultProfitPrct;
  private static String retailRate;
  private static String salesforceId;
  private static String startDate;
  private static String endDate;

  public static String createLineItem(
      String serviceEndPoint, String auth, String lineItemRequestEndPoint, String campaignID) {

    initializeData(serviceEndPoint);

    // Request Details
    String requestURL = serviceEndPoint + lineItemRequestEndPoint;
    String requestBody =
        JsonUtilities.jsonToString(
                System.getProperty("user.dir")
                    + "/src/main/java/com/phunware/ads/json/lineItem.json")
            .replaceAll("budgetDailyToBeChanged", budgetDaily)
            .replaceAll("lineItemTypeIdToBeChanged", lineItemTypeId)
            .replaceAll("campaignIdToBeChanged", campaignID)
            .replaceAll("LineItemNameToBeChanged", randomvalue)
            .replaceAll("defaultProfitPrctToBeChanged", defaultProfitPrct)
            .replaceAll("retailRateToBeChanged", retailRate)
            .replaceAll("salesforceIdToBeChanged", salesforceId)
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
    lineItemID = response.then().extract().path("data.id").toString();
    LOG.info("Created New LineItem - ID - " + lineItemID);

    return lineItemID;
  }

  public static void deleteLineItem(
      String serviceEndPoint, String lineItemRequestEndPoint, String auth, String lineItemID) {

    // Request Details
    String requestURL = serviceEndPoint + lineItemRequestEndPoint + "/" + lineItemID;

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

    LOG.info("Deleted LineItem - " + lineItemID);
  }

  public static void updateLineItem(
      String serviceEndPoint,
      String lineItemRequestEndPoint,
      String auth,
      String lineItemID,
      String statusID) {

    // Request Details
    String requestURL = serviceEndPoint + lineItemRequestEndPoint + "/" + lineItemID;

    // Printing Request Details
    LOG.debug("REQUEST-URL:PUT-" + requestURL);

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

    LOG.info("Updated LineItem ID - " + lineItemID + ", Status Updated to - " + statusID);
  }

  public static void updateLineItemUsingRequestBody(
      String serviceEndPoint,
      String lineItemRequestEndPoint,
      String auth,
      String lineItemID,
      String requestBody) {

    // Request Details
    String requestURL = serviceEndPoint + lineItemRequestEndPoint + "/" + lineItemID;

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

    LOG.info("Updated LineItem ID - " + lineItemID + " With request Body " + requestBody);
  }

  public static void initializeData(String serviceEndPoint) {

    randomvalue = "LineItem" + LocalDateTime.now().toString().replaceAll("[-:.T]", "");
    budgetDaily = "10.04";
    lineItemTypeId = "2"; // possible values 1,2,3,4
    defaultProfitPrct = "60";
    retailRate = "5.00";
    salesforceId = "123";
    // current time plus 30 seconds
    startDate =
        LocalDateTime.now(Clock.systemUTC()).plusSeconds(30).toString().replaceAll("\\..{2,}", "");
    // Current time plus 8 years
    endDate =
        LocalDateTime.now(Clock.systemUTC()).plusYears(8).toString().replaceAll("\\..{2,}", "");
  }
}
