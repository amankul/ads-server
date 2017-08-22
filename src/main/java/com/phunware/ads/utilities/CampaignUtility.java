package com.phunware.ads.utilities;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static io.restassured.RestAssured.given;

public class CampaignUtility {

    //Initiating Logger Object
    private static final Logger log = LogManager.getLogger();
    private static String randomvalue;
    private static String campaignID;
    private static ArrayList<String> dbResult;
    private static String startDate;
    private static String endDate;
    private static int customerID;
    private static int advertiserID;


    public static String createCampaign(String serviceEndPoint, String auth, String campaignRequestEndPoint) {

        initializeData(serviceEndPoint);

        //Request Details
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
        campaignID = response.then().extract().path("data.id").toString();
        log.info("Created New Campaign - ID - " + campaignID);

        return campaignID;
    }


    public static void deleteCampaign(String serviceEndPoint, String campaignRequestEndPoint, String auth, String campaignID) {

        //Request Details
        String requestURL = serviceEndPoint + campaignRequestEndPoint + "/" + campaignID;

        //Delete Campaign

        given()
                .header("Content-Type", "application/json")
                .header("Authorization", auth)
                .request()
                .delete(requestURL)
                .then()
                .statusCode(200)
                .extract()
                .response();

        log.info("Deleted Campaign ID - " + campaignID);
    }

    public static void updateCampaign(String serviceEndPoint, String campaignRequestEndPoint, String auth, String campaignID , String statusID) {

        //Request Details
        String requestURL = serviceEndPoint + campaignRequestEndPoint + "/" + campaignID;

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

        log.info("Updated Campaign ID - " + campaignID + ", Status Updated to - " + statusID);
    }


    public static int getAdvertiserID(String serviceEndPoint) {
        String sqlQuery = "select id from advertiser where is_active = 1 limit 1";
        dbResult = MySqlUtility.query_Post_connection_To_MySQL_Via_JumpServer(sqlQuery, serviceEndPoint);
        return Integer.valueOf(dbResult.get(0));
    }


    public static int getCustomerID(String serviceEndPoint) {
        String sqlQuery_customer = "select id from customer where status_id not in (100) limit 1";
        dbResult = MySqlUtility.query_Post_connection_To_MySQL_Via_JumpServer(sqlQuery_customer, serviceEndPoint);
        return Integer.valueOf(dbResult.get(0));
    }

    public static void initializeData(String serviceEndPoint) {

        customerID = getCustomerID(serviceEndPoint);
        dbResult.clear();
        advertiserID = getAdvertiserID(serviceEndPoint);
        dbResult.clear();

        randomvalue = "Campaign" + LocalDateTime.now().toString().replaceAll("[-:.T]", "");


        //current time plus 3 minutes
        startDate = LocalDateTime.now(Clock.systemUTC()).plusMinutes(3).toString().replaceAll("\\..{2,}", "");

        //Current time plus 10 years
        endDate = LocalDateTime.now(Clock.systemUTC()).plusYears(10).toString().replaceAll("\\..{2,}", "");

    }


}
