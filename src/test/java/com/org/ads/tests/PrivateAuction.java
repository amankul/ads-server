package com.org.ads.tests;

import com.org.ads.utilities.*;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

public class PrivateAuction {

  // Initiating Logger Object
  private static final Logger LOG = LogManager.getLogger();
  private static String placementID;
  private static String campaignID;
  private static String lineItemID;
  private static String creativeID;
  private static String impressionURL;
  private static String serviceEndPoint;
  private static String lineItemRequestEndPoint;
  private static String campaignRequestEndPoint;
  private static String placementRequestEndPoint;
  private static String runtimeEndPoint;
  private static String auth;
  private static String randomValue;
  private static boolean didBidMatchWithPlacement;

  @BeforeClass(alwaysRun = true)
  @Parameters({
    "serviceEndPoint",
    "auth",
    "runtimeEndPoint",
    "campaignRequestEndPoint",
    "lineItemRequestEndPoint",
    "creativeRequestEndPoint",
    "placementRequestEndPoint"
  })
  public void preTestSteps(
      String serviceEndPoint,
      String auth,
      String runtimeEndPoint,
      String campaignRequestEndPoint,
      String lineItemRequestEndPoint,
      String creativeRequestEndPoint,
      String placementRequestEndPoint) {

    this.serviceEndPoint = serviceEndPoint;
    this.lineItemRequestEndPoint = lineItemRequestEndPoint;
    this.campaignRequestEndPoint = campaignRequestEndPoint;
    this.placementRequestEndPoint = placementRequestEndPoint;
    this.runtimeEndPoint = runtimeEndPoint;
    this.auth = auth;

    // Create new Campaign, LineItem & Placement

    creativeID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "creativeId",
            System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");
    campaignID = CampaignUtility.createCampaign(serviceEndPoint, auth, campaignRequestEndPoint);
    lineItemID =
        LineItemUtility.createLineItem(serviceEndPoint, auth, lineItemRequestEndPoint, campaignID);
    placementID =
        PlacementUtility.createPlacementExistingDealID(
            serviceEndPoint, auth, placementRequestEndPoint, creativeID, lineItemID);

    // Capture Impression URL
    impressionURL =
        ServerRequestUtility.readDataFromPropertiesFile(
            "impressionURL",
            System.getProperty("user.dir") + "/src/main/resources/BidResponseData.Properties");
    LOG.info("Impression URL - " + impressionURL);

    // Save created ID's in a property file
    ServerRequestUtility.writePropertyFile(
        "campaignId",
        campaignID,
        "lineItemId",
        lineItemID,
        "placementId",
        placementID,
        "creativeId",
        creativeID,
        "ImpressionURL",
        impressionURL,
        System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");

    // Update earlier created Campaign, Line Item & Placement to running  - `600 status ID`
    CampaignUtility.updateCampaign(
        serviceEndPoint, campaignRequestEndPoint, auth, campaignID, "600");
    LineItemUtility.updateLineItem(
        serviceEndPoint, lineItemRequestEndPoint, auth, lineItemID, "600");
    PlacementUtility.updatePlacement(
        serviceEndPoint, placementRequestEndPoint, auth, placementID, "600");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    // capture ID's from properties file.
    campaignID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "campaignId",
            System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");
    lineItemID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "lineItemId",
            System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");
    placementID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "placementId",
            System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");

    // capturing random value generated during placement creation.
    randomValue =
        ServerRequestUtility.readDataFromPropertiesFile(
            "randomValue",
            System.getProperty("user.dir") + "/src/main/resources/random.Properties");
  }

  @AfterClass
  public void cleanUp() {
    // update placement to have no deal ID
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint,
        placementRequestEndPoint,
        auth,
        placementID,
        "{\"dealId\": \"DealIDAutomation" + randomValue + "\"}");
  }

  /*
  Verify PMP Private Auction when
  1)"private_auction":1 in bid request json body
  2)Deal Id in bid request json body matches with the placement DealID
  3)Other Deal ID params valid - pmp.deals.bidfloor:"1.00" < placement bid:1.5

  Expected : bid request should be successful
  */
  @Test(priority = 1)
  public void verify_PmpValid_BidRequest() {
    PlacementUtility.getPlacement(serviceEndPoint, placementRequestEndPoint, auth, placementID);
    // Sending Bid request
    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint,
            "PmpRunTimeRequest/PrivateAuctionTrue_BidFloorLessThanPlacementBidRequest.json");
    Assert.assertEquals(
        statusCode, 200, "Status code returned after sending a bidrequest -" + statusCode);
  }

  /*
  Verify PMP Private Auction when
  1)"private_auction":1 in bid request json body
  2)Deal Id in bid request json body doesnot match with the placement DealID
  3)Other Deal ID params valid - pmp.deals.bidfloor:"1.00" < placement bid:1.5

  Expected : bid request should fail on PmpConstraint
  */
  @Test(priority = 2)
  public void verifyPmp_NonMatching_DealId_InBidRequest() {

    // Sending Bid request and asserting on status code
    int statusCode =
        postBidRequest_NonMatchingDealID(
            runtimeEndPoint,
            "PmpRunTimeRequest/PrivateAuctionTrue_BidFloorLessThanPlacementBidRequest.json");
    Assert.assertEquals(
        statusCode, 204, "Status code returned after sending a bidrequest -" + statusCode);

    // Capture Placement related data from /var/org/dsp/logs/abm-dsp-srv.log
    String data =
        ServerRequestUtility.waitForLogsToGetPopulated(
            serviceEndPoint,
            "grep \"Placement: "
                + placementID
                + " Constraint: PmpConstraint is INVALID\" /var/org/dsp/logs/abm-dsp-srv.log | tail -1");

    // looking for PmpConstraint invalidation in logs
    // Expecting "Placement: placementID Constraint: PmpConstraint is INVALID"
    Assert.assertTrue(
        data.contains(placementID + " Constraint: PmpConstraint is INVALID"),
        "Found in logs -> " + data);
  }

  /*
  Verify PMP Private Auction when
  1)"private_auction":1 in bid request json body
  2)Deal Id in bid request json body matches with the placement DealID
  3)Other Deal ID params valid - pmp.deals.bidfloor:"2.00" > placement bid:1.5

  Expected : bid request should fail on PmpConstraint
  */
  @Test(priority = 3)
  public void verifyPmp_NonCompliant_BidFloor_InBidRequest() {

    // Sending Bid request and asserting on status code
    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint,
            "PmpRunTimeRequest/PrivateAuctionTrue_NonMatching_BidFloorMoreThanPlacementBidRequest.json");
    Assert.assertEquals(
        statusCode, 204, "Status code returned after sending a bidrequest -" + statusCode);

    // Capture Placement related data from /var/org/dsp/logs/abm-dsp-srv.log
    String data =
        ServerRequestUtility.waitForLogsToGetPopulated(
            serviceEndPoint,
            "grep \"Placement: "
                + placementID
                + " Constraint: PmpConstraint is INVALID\" /var/org/dsp/logs/abm-dsp-srv.log | tail -1");

    // looking for PmpConstraint invalidation in logs
    // Expecting "Placement: placementID Constraint: PmpConstraint is INVALID"
    Assert.assertTrue(
        data.contains(placementID + " Constraint: PmpConstraint is INVALID"),
        "Found in logs -> " + data);
  }

  /*
  Verify PMP Private Auction when
  1)"private_auction":0 in bid request json body
  2)Deal Id in bid request json body matches with the placement DealID
  3)Other Deal ID params valid - pmp.deals.bidfloor:"1.00" < placement bid:1.5

  Expected : bid request should be successful
  */
  @Test(priority = 4)
  public void verify_PmpValid_BidRequest_PrivateAutionFalse() {
    PlacementUtility.getPlacement(serviceEndPoint, placementRequestEndPoint, auth, placementID);
    // Sending Bid request
    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint,
            "PmpRunTimeRequest/PrivateAuctionFalse_BidFloorLessThanPlacementBidRequest.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));

    ServerRequestUtility.sleep(45);
  }

  /*
  Verify PMP Private Auction when
  1)"private_auction":0 in bid request json body
  2)Deal Id in bid request json body doesnot match with the placement DealID
  3)Other Deal ID params valid - pmp.deals.bidfloor:"1.00" < placement bid:1.5

  Expected : bid request should fail on PmpConstraint
  */
  @Test(priority = 5)
  public void verifyPmp_NonMatching_DealId_InBidRequest_PrivateAutionFalse() {

    // Sending Bid request  and asserting on status code
    int statusCode =
        postBidRequest_NonMatchingDealID(
            runtimeEndPoint,
            "PmpRunTimeRequest/PrivateAuctionFalse_BidFloorLessThanPlacementBidRequest.json");

    if (didBidMatchWithPlacement) {
      Assert.assertEquals(
          statusCode, 204, "Status code returned after sending a bidrequest -" + statusCode);

      // Capture Placement related data from /var/org/dsp/logs/abm-dsp-srv.log
      String data =
          ServerRequestUtility.waitForLogsToGetPopulated(
              serviceEndPoint,
              "grep \"Placement: "
                  + placementID
                  + " Constraint: PmpConstraint is INVALID\" /var/org/dsp/logs/abm-dsp-srv.log | tail -1");

      // looking for PmpConstraint invalidation in logs
      // Expecting "Placement: placementID Constraint: PmpConstraint is INVALID"
      Assert.assertTrue(
          data.contains(placementID + " Constraint: PmpConstraint is INVALID"),
          "Found in logs -> " + data);
    }
  }

  /*
  Verify PMP Private Auction when
  1)"private_auction":0 in bid request json body
  2)Deal Id in bid request json body matches with the placement DealID
  3)Other Deal ID params Invalid - pmp.deals.bidfloor:"2.00" > placement bid:1.5

  Expected : bid request should fail on PmpConstraint
  */
  @Test(priority = 6)
  public void verifyPmp_NonCompliant_BidFloor_InBidRequest_PrivateAutionFalse() {

    // Sending Bid request  and asserting on status code
    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint,
            "PmpRunTimeRequest/PrivateAuctionFalse_NonMatching_BidFloorMoreThanPlacementBidRequest.json");

    if (didBidMatchWithPlacement) {
      Assert.assertEquals(
          statusCode, 204, "Status code returned after sending a bidrequest -" + statusCode);

      // Capture Placement related data from /var/org/dsp/logs/abm-dsp-srv.log
      String data =
          ServerRequestUtility.waitForLogsToGetPopulated(
              serviceEndPoint,
              "grep \"Placement: "
                  + placementID
                  + " Constraint: PmpConstraint is INVALID\" /var/org/dsp/logs/abm-dsp-srv.log | tail -1");

      // looking for PmpConstraint invalidation in logs
      // Expecting "Placement: placementID Constraint: PmpConstraint is INVALID"
      Assert.assertTrue(
          data.contains(placementID + " Constraint: PmpConstraint is INVALID"),
          "Found in logs -> " + data);
    }
  }

  /*
  Verify PMP Private Auction when
  1)"private_auction":0 in bid request json body
  2)Placement do not have a deal ID
  3)Other Deal ID params valid - pmp.deals.bidfloor:"1.00" < placement bid:1.5

  Expected : bid request should be successful
  */
  @Test(priority = 7)
  public void verify_Pmp_PrivateAuction_False_NoDealID_InPlacement() {

    // update placement to have no deal ID
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint, placementRequestEndPoint, auth, placementID, "{\"dealId\": null}");

    // waiting for DG to pull recent changes
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    // Sending Bid request
    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint,
            "PmpRunTimeRequest/PrivateAuctionFalse_BidFloorLessThanPlacementBidRequest.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  // ********** UTILITY METHODS *********** //

  public static int postBidRequest_NonMatchingDealID(String runtimeEndPoint, String fileName) {

    // Request Details
    String requestURL = runtimeEndPoint;
    String randomValue = LocalDateTime.now().toString().replaceAll("[-:.T]", "");

    String requestBody =
        JsonUtilities.jsonToString(
                System.getProperty("user.dir") + "/src/main/java/com/org/ads/json/" + fileName)
            .replaceAll("DealIDAutomation", "DealIDAutomation" + randomValue);

    // Printing Request Details
    LOG.info("REQUEST-URL:POST-" + requestURL);
    LOG.info("REQUEST-BODY:" + requestBody);

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

    //Validate if bid response matched the placement created.
    if (response.getStatusCode()==200) {
      String winNotifyUrl =
              response.then().extract().path("seatbid.bid.nurl").toString().replaceAll("[\\[\\]]", "");

      Pattern pattern = Pattern.compile(".*?pid=(.*?)&.*");
      Matcher matcher = pattern.matcher(winNotifyUrl);
      if (matcher.find()) {
        if (matcher.group(1).matches(placementID)) {
          didBidMatchWithPlacement = true;
        }
      }
    }

    // printing response
    LOG.info("RESPONSE:" + response.asString());
    LOG.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");

    return response.statusCode();
  }
}
