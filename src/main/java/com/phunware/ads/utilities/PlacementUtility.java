package com.phunware.ads.utilities;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;

public class PlacementUtility {

    //Initiating Logger Object
    private static final Logger log = LogManager.getLogger();
    private static String randomvalue;
    private static String placementID;


    public static String createPlacement(String serviceEndPoint, String auth, String placementRequestEndPoint, String creativeID, String lineItemID) {

        initializeData(serviceEndPoint);

        //Request Details
        String requestURL = serviceEndPoint + placementRequestEndPoint;
        String requestBody =
                JsonUtilities.jsonToString(
                        System.getProperty("user.dir")
                                + "/src/main/java/com/phunware/ads/json/placement.json")
                        .replaceAll("creativeIDsToBeChanged", creativeID)
                        .replaceAll("lineItemIDToBeChanged", lineItemID)
                        .replaceAll("nameToBeChanged", randomvalue);


        //Printing Request Details
        log.debug("REQUEST-URL:POST-" + requestURL);
        log.debug("REQUEST-BODY:" + requestBody);

        //Extracting response after status code validation
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

        //printing response
        log.debug("RESPONSE:" + response.asString());
        log.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");

        //capturing created lineItem ID
        placementID = response.then().extract().path("data.id").toString();
        log.info("Created New PLacement - ID - " + placementID);

        return placementID;
    }


    public static void deletePlacement(String serviceEndPoint, String placementRequestEndPoint, String auth, String placementID) {

        //Request Details
        String requestURL =
                serviceEndPoint + placementRequestEndPoint + "/" + placementID;

        //Printing Request Details
        log.debug("REQUEST-URL:DELETE-" + requestURL);

        //Extracting response after status code validation
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

        log.info("Deleted Placement ID - " + placementID);
    }


    public static void updatePlacement(String serviceEndPoint, String placementRequestEndPoint, String auth, String placementID , String statusID) {

        //Request Details
        String requestURL =
                serviceEndPoint + placementRequestEndPoint + "/" + placementID;

        //Printing Request Details
        log.debug("REQUEST-URL:PUT-" + requestURL);

        //Extracting response after status code validation
        Response response =
                given()
                        .header("Content-Type", "application/json")
                        .header("Authorization", auth)
                        .request()
                        .body("{\"statusId\": "+statusID+"}")
                        .put(requestURL)
                        .then()
                        .statusCode(200)
                        .extract()
                        .response();

        log.info("Updated Placement ID - " + placementID + ", Status Updated to - " + statusID);

    }

    public static void initializeData(String serviceEndPoint) {
        randomvalue = "Placement" + LocalDateTime.now().toString().replaceAll("[-:.T]", "");
    }


}
