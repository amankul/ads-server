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
    private static String bid;
    private static String budgetDaily;
    private static String locationRadius;
    private static String optimizationTarget;
    private static String optimizationTargetTypeId;
    private static String optimizationSampleSize;
    private static String optimizationSampleSizeTypeId;
    private static String pacingTypeId;
    private static String dailyFcap;
    private static String dealId;
    private static String platformId1;
    private static String minOSVersion1;
    private static String maxOSVersion1;
    private static String platformId2;
    private static String minOSVersion2;
    private static String maxOSVersion2;
    private static String domain1;
    private static String trafficSourceId1;
    private static String domain2;
    private static String trafficSourceId2;
    private static String bundleId1;
    private static String bundleId2;
    private static String demographicId1;
    private static String demographicId2;


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
                        .replaceAll("nameToBeChanged", randomvalue)
                        .replaceAll("bidToBeChanged", bid)
                        .replaceAll("budgetDailyToBeChanged", budgetDaily)
                        .replaceAll("locationRadiusToBeChanged", locationRadius)
                        .replaceAll("optTargetToBeChanged", optimizationTarget)
                        .replaceAll("optTargetTypeIDToBeChanged", optimizationTargetTypeId)
                        .replaceAll("optSampleSizeToBeChanged", optimizationSampleSize)
                        .replaceAll("optSampleSizeTypeIdToBeChanged", optimizationSampleSizeTypeId)
                        .replaceAll("PacingTypeIdToBeChanged", pacingTypeId)
                        .replaceAll("dailyFcapToBeChanged", dailyFcap)
                        .replaceAll("dealIdToBeChanged", dealId)
                        .replaceAll("platformId1ToBeChanged", platformId1)
                        .replaceAll("platformId2ToBeChanged", platformId2)
                        .replaceAll("minOSVersion1ToBeChanged", minOSVersion1)
                        .replaceAll("minOSVersion2ToBeChanged", minOSVersion2)
                        .replaceAll("maxOSVersion1ToBeChanged", maxOSVersion1)
                        .replaceAll("maxOSVersion2ToBeChanged", maxOSVersion2)
                        .replaceAll("domain1ToBeChanged", domain1)
                        .replaceAll("domain2ToBeChanged", domain2)
                        .replaceAll("trafficSourceId1ToBeChanged", trafficSourceId1)
                        .replaceAll("trafficSourceId2ToBeChanged", trafficSourceId2)
                        .replaceAll("bundleID1ToBeChanged", bundleId1)
                        .replaceAll("bundleID2ToBeChanged", bundleId2)
                        .replaceAll("demographicId1ToBeChanged", demographicId1)
                        .replaceAll("demographicId2ToBeChanged", demographicId2);


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

        log.info("Deleted Placement ID -" + placementID);
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

        log.info("Updated Placement ID -" + placementID + ", Status to Running - 600");

    }

    public static void initializeData(String serviceEndPoint) {

        randomvalue = "Placement" + LocalDateTime.now().toString().replaceAll("[-:.T]", "");
        bid = "1.50";
        budgetDaily = "100.10";
        locationRadius = "88.76";
        optimizationTarget = "10000.55";
        optimizationTargetTypeId = "1";  //1-4
        optimizationSampleSize = "9999.99";
        optimizationSampleSizeTypeId = "1";    //1-3
        pacingTypeId = "1";   //1,2
        dailyFcap = "100";  //integer
        dealId = "AutomationDealID";  //String
        platformId1 = "1";   //1-8
        minOSVersion1 = "1.2.3";
        maxOSVersion1 = "3.4.5";
        platformId2 = "1";  //1-8
        minOSVersion2 = "6";
        maxOSVersion2 = "9";
        domain1 = "https://www.domain1.com";   //alphanumeric String
        domain2 = "https://www.domain2.com";  //alphanumeric String
        trafficSourceId1 = "1";    //1,2,3
        trafficSourceId2 = "2";   //1,2,3
        bundleId1 = "bundleid1";   //alphanumeric String
        bundleId2 = "bundleid2";    //alphanumeric String
        demographicId1 = "1";   //1-118
        demographicId2 = "10";  //1-118
    }


}
