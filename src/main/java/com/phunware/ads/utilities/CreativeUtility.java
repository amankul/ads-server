package com.phunware.ads.utilities;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static io.restassured.RestAssured.given;

public class CreativeUtility {

    //Initiating Logger Object
    private static final Logger log = LogManager.getLogger();
    private static String randomvalue;
    private static String creativeID;
    private static ArrayList<String> dbResult;
    private static String advertiserId;
    private static String creativeTypeId;
    private static String clickurl;
    private static String iurlS3AssetId;
    private static String s3Asset;

    public static String createCreative(String serviceEndPoint, String auth, String creativeRequestEndPoint) {

        initializeData(serviceEndPoint);

        //Request Details
        String requestURL = serviceEndPoint + creativeRequestEndPoint;
        String requestBody =
                JsonUtilities.jsonToString(
                        System.getProperty("user.dir")
                                + "/src/main/java/com/phunware/ads/json/creative.json")
                        .replaceAll("advertiserIdToBeChanged", advertiserId)
                        .replaceAll("nameToBeChanged", randomvalue)
                        .replaceAll("creativeTypeIdToBeChanged", creativeTypeId)
                        .replaceAll("clickURLToBeChanged", clickurl)
                        .replaceAll("iurlS3AssetIdToBeChanged", iurlS3AssetId)
                        .replaceFirst(":\\[\\]", ":\\[" + s3Asset + "\\]"); //replacing s3AssetId

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
        creativeID = response.then().extract().path("data.id").toString();
        log.info("Created New Creative - ID - " + creativeID);

        return creativeID;
    }


    public static void deleteCreative(String serviceEndPoint, String creativeRequestEndPoint, String auth, String creativeID) {


        //Request Details
        String requestURL = serviceEndPoint + creativeRequestEndPoint + "/" + creativeID;

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

        log.info("Deleted Creative ID -" + creativeID);
    }


    public static void initializeData(String serviceEndPoint) {

        randomvalue = "Creative" + LocalDateTime.now().toString().replaceAll("[-:.T]", "");

        //Capturing advertiserId from DB
        String sqlQuery = "select id from advertiser where is_active = 1 limit 1";
        dbResult = MySqlUtility.query_Post_connection_To_MySQL_Via_JumpServer(sqlQuery, serviceEndPoint);
        advertiserId = dbResult.get(0);
        dbResult.clear();

        //Capturing iurlS3AssetId from DB
        String sqlQuery_iurlS3AssetId = "select id from s3_asset where  status_id not in (100) limit 1";
        dbResult = MySqlUtility.query_Post_connection_To_MySQL_Via_JumpServer(sqlQuery_iurlS3AssetId, serviceEndPoint);
        iurlS3AssetId = dbResult.get(0);
        dbResult.clear();

        creativeTypeId = "1";   //1 banner, 2 FullScreen, 3 Video, 4 Native
        clickurl = "http://www.clickurl1.com";

        //s3asset ID from db
        //Update this to match with the `creativeTypeId`
        String sqlQuery_S3AssetId = "select id from s3_asset where  status_id not in (100) limit 1,1";
        dbResult = MySqlUtility.query_Post_connection_To_MySQL_Via_JumpServer(sqlQuery_S3AssetId, serviceEndPoint);
        s3Asset = dbResult.get(0);
        dbResult.clear();
    }


}
