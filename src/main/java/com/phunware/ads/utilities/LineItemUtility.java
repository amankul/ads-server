package com.phunware.ads.utilities;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;

public class LineItemUtility {

    //Initiating Logger Object
    private static final Logger log = LogManager.getLogger();
    private static String randomvalue;
    private static String lineItemID;
    private static String budgetDaily;
    private static String lineItemTypeId;
    private static String defaultProfitPrct;
    private static String retailRate;
    private static String salesforceId;
    private static String startDate;
    private static String endDate;


    public static String createLineItem(String serviceEndPoint, String auth, String lineItemRequestEndPoint, String campaignID) {

        initializeData(serviceEndPoint);

        //Request Details
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

        //capturing campaign ID
        lineItemID = response.then().extract().path("data.id").toString();
        log.info("Created New LineItem - ID - " + lineItemID);

        return lineItemID;
    }


    public static void deleteLineItem(String serviceEndPoint, String lineItemRequestEndPoint, String auth, String lineItemID) {

        //Request Details
        String requestURL = serviceEndPoint + lineItemRequestEndPoint + "/" + lineItemID;

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

        log.info("Deleted LineItem -" + lineItemID);
    }


    public static void updateLineItem(String serviceEndPoint, String lineItemRequestEndPoint, String auth, String lineItemID , String statusID) {

        //Request Details
        String requestURL = serviceEndPoint + lineItemRequestEndPoint + "/" + lineItemID;

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

        log.info("Updated LineItem ID -" + lineItemID + ", Status to Running - 600");

    }


    public static void initializeData(String serviceEndPoint) {

        randomvalue = "LineItem" + LocalDateTime.now().toString().replaceAll("[-:.T]", "");
        budgetDaily = "1000.40";
        lineItemTypeId = "1";   //possible values 1,2,3,4
        defaultProfitPrct = "60";
        retailRate = "2.40";
        salesforceId = "123";
        //current time plus 4 minutes
        startDate = LocalDateTime.now(Clock.systemUTC()).plusMinutes(4).toString().replaceAll("\\..{2,}", "");
        //Current time plus 8 years
        endDate = LocalDateTime.now(Clock.systemUTC()).plusYears(8).toString().replaceAll("\\..{2,}", "");

    }


}
