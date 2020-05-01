package com.org.ads.utilities;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Properties;

import static io.restassured.RestAssured.given;

public class PlacementUtility {

  // Initiating Logger Object
  private static final Logger LOG = LogManager.getLogger();
  private static String randomValue;
  private static String placementID;

  //Creates a new Placement
  public static String createPlacement(
      String serviceEndPoint,
      String auth,
      String placementRequestEndPoint,
      String creativeID,
      String lineItemID) {

    initializeData(serviceEndPoint);

    // Request Details
    String requestURL = serviceEndPoint + placementRequestEndPoint;
    String requestBody =
        JsonUtilities.jsonToString(
                System.getProperty("user.dir")
                    + "/src/main/java/com/org/ads/json/placement.json")
            .replaceAll("creativeIDsToBeChanged", creativeID)
            .replaceAll("lineItemIDToBeChanged", lineItemID)
            .replaceAll("DealIDAutomation", "DealIDAutomation" + randomValue)
            .replaceAll("nameToBeChanged", randomValue);

    writePropertyFile(
        "randomValue",
        randomValue,
        System.getProperty("user.dir") + "/src/main/resources/random.Properties");

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
    placementID = response.then().extract().path("data.id").toString();
    LOG.info("Created New PLacement - ID - " + placementID);

    return placementID;
  }

  //Creates a new placement using an existing deal ID
  public static String createPlacementExistingDealID(
      String serviceEndPoint,
      String auth,
      String placementRequestEndPoint,
      String creativeID,
      String lineItemID) {

    initializeData(serviceEndPoint);

    String randomvalue =
        PropertiesFileUtility.readDataFromPropertiesFile(
            "randomValue",
            System.getProperty("user.dir") + "/src/main/resources/random.Properties");

    // Request Details
    String requestURL = serviceEndPoint + placementRequestEndPoint;
    String requestBody =
        JsonUtilities.jsonToString(
                System.getProperty("user.dir")
                    + "/src/main/java/com/org/ads/json/placement.json")
            .replaceAll("creativeIDsToBeChanged", creativeID)
            .replaceAll("lineItemIDToBeChanged", lineItemID)
            .replaceAll("DealIDAutomation", "DealIDAutomation" + randomvalue)
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
    placementID = response.then().extract().path("data.id").toString();
    LOG.info("Created New PLacement - ID - " + placementID);

    return placementID;
  }

  //Delete placement
  public static void deletePlacement(
      String serviceEndPoint, String placementRequestEndPoint, String auth, String placementID) {

    // Request Details
    String requestURL = serviceEndPoint + placementRequestEndPoint + "/" + placementID;

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

    LOG.info("Deleted Placement ID - " + placementID);
  }

  //get Placement Details
  public static void getPlacement(
      String serviceEndPoint, String placementRequestEndPoint, String auth, String placementID) {

    // Request Details
    String requestURL = serviceEndPoint + placementRequestEndPoint + "/" + placementID;

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

    LOG.info("Getting details for Placement ID - " + placementID);
    LOG.info(response.asString());
  }

  //Update placement status ID
  public static void updatePlacement(
      String serviceEndPoint,
      String placementRequestEndPoint,
      String auth,
      String placementID,
      String statusID) {

    // Request Details
    String requestURL = serviceEndPoint + placementRequestEndPoint + "/" + placementID;

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

    LOG.info("Updated Placement ID - " + placementID + ", Status Updated to - " + statusID);
  }

  //Update placement based on the request body sent to it as one of the parameter
  public static void updatePlacementWithRequestBody(
      String serviceEndPoint,
      String placementRequestEndPoint,
      String auth,
      String placementID,
      String requestBody) {

    // Request Details
    String requestURL = serviceEndPoint + placementRequestEndPoint + "/" + placementID;

    // Printing Request Details
    LOG.info("REQUEST-URL:PUT - " + requestURL);
    LOG.info("REQUEST BODY - " + requestBody);

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

    LOG.info("Updated Placement ID - " + placementID);
  }

  //get placement status
  public static int getStatusId(
      String serviceEndPoint, String placementRequestEndPoint, String auth, String placementID) {

    // Request Details
    String requestURL = serviceEndPoint + placementRequestEndPoint + "/" + placementID;

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

    int statusID = Integer.valueOf(response.then().extract().path("data.statusId").toString());
    LOG.info("Status ID of placement - " + placementID + " is " + statusID);
    return statusID;
  }

  public static void initializeData(String serviceEndPoint) {
    randomValue = "Placement" + LocalDateTime.now().toString().replaceAll("[-:.T]", "");
  }

  public static void writePropertyFile(String key, String value, String filePath) {

    Properties prop = new Properties();
    OutputStream output = null;

    try {
      output = new FileOutputStream(filePath);
      prop.setProperty(key, value);
      prop.store(output, "Random Data");
      output.close();
    } catch (FileNotFoundException ex) {
      // file does not exist
      ex.printStackTrace();
    } catch (IOException ex) {
      // I/O error
      ex.printStackTrace();
    }
  }
}
