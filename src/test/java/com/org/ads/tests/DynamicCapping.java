package com.org.ads.tests;

import com.org.ads.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashMap;

public class DynamicCapping {

  // Initiating Logger Object
  private static final Logger LOG = LogManager.getLogger();
  private static String placementID;
  private static String creativeID;
  private static String campaignID;
  private static String lineItemID;
  private static String serviceEndPoint;
  private static String creativeRequestEndPoint;
  private static String placementRequestEndPoint;
  private static String campaignRequestEndPoint;
  private static String lineItemRequestEndPoint;
  private static String runtimeEndPoint;
  private static String auth;
  private static String impressionURL;
  private static String clickURL;
  private static String winNotifyUrl;
  private static String transactionID;
  HashMap<String, String> result = null;

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
    this.placementRequestEndPoint = placementRequestEndPoint;
    this.campaignRequestEndPoint = campaignRequestEndPoint;
    this.lineItemRequestEndPoint = lineItemRequestEndPoint;
    this.creativeRequestEndPoint = creativeRequestEndPoint;
    this.runtimeEndPoint = runtimeEndPoint;
    this.auth = auth;

    // Create new Creative,Campaign, LineItem & Placement
    creativeID = CreativeUtility.createCreative(serviceEndPoint, auth, creativeRequestEndPoint);
    campaignID = CampaignUtility.createCampaign(serviceEndPoint, auth, campaignRequestEndPoint);
    lineItemID =
        LineItemUtility.createLineItem(serviceEndPoint, auth, lineItemRequestEndPoint, campaignID);
    placementID =
        PlacementUtility.createPlacementExistingDealID(
            serviceEndPoint, auth, placementRequestEndPoint, creativeID, lineItemID);

    // Update Placement
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint,
        placementRequestEndPoint,
        auth,
        placementID,
        "{\"optimizationTarget\": 0.01,\"optimizationTargetTypeId\": 1}");

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
        "noOfHitsImpressionURL",
        "10",
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
  }

  @AfterClass
  public void cleanUp() {

    // delete earlier created campaign,LineItem & placement.
    CampaignUtility.deleteCampaign(serviceEndPoint, campaignRequestEndPoint, auth, campaignID);
    LineItemUtility.deleteLineItem(serviceEndPoint, lineItemRequestEndPoint, auth, lineItemID);
    PlacementUtility.deletePlacement(serviceEndPoint, placementRequestEndPoint, auth, placementID);
    CreativeUtility.deleteCreative(serviceEndPoint, creativeRequestEndPoint, auth, creativeID);
  }

  // *************** eCPC *************** //

  /*
    verify eCPC impressions , Placement Optimization Targets (0.01 eCPC, 10 Impressions)
    generate 1 click and 10 impressions.Expecting bid request to be successful.
  */
  @Test(priority = 1)
  public void verify_eCPC_Impressions() {

    // POST REQUEST TO DSP SERVER, CAPTURE DATA

    result =
        ServerRequestUtility.postBidRequest(runtimeEndPoint, "DynamicCapping/runTimeRequest.json");
    Assert.assertTrue(result.size() == 4, "Bid Request is not successful");

    // Capturing impressionURL,clickURL,winNotifyUrl & transactionID from bid response
    captureBidResponseData(result);

    // Hitting the impression URL - 5 times,Click URl 1 time and ImpressionURL 5 more times
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);
    ServerRequestUtility.hitURL(clickURL.replace("${AUCTION_PRICE}", "1"), 1);
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
    verify eCPC impressions , Placement Optimization Targets (0.01 eCPC, 10 Impressions)
    generate one more impression.Expecting bid request to be successful.
  */
  @Test(priority = 2)
  public void verify_eCPC_Impressions_Limit_Exceeded() {

    // Hitting the impression URL - 2 times,making ImpressionURL exceed placement limit of 10
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 2);

    // Wait time is needed for the server to pick up aerospike data
    ServerRequestUtility.sleep(45);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
    verify eCPC Spend , Placement Optimization Targets (0.01 eCPC, 0.05 Spend)
    generate 1 click and 10 impressions.Expecting bid request to be successful.
  */
  @Test(priority = 3)
  public void verify_eCPC_Spend() {

    // Update Placement
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint,
        placementRequestEndPoint,
        auth,
        placementID,
        "{\"optimizationTarget\": 0.01,\"optimizationTargetTypeId\": 1,\"optimizationSampleSize\": 0.05,\"optimizationSampleSizeTypeId\": 3}");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    // POST REQUEST TO DSP SERVER, CAPTURE DATA
    result =
        ServerRequestUtility.postBidRequest(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID1.json");
    Assert.assertTrue(result.size() == 4, "Bid Request is not successful");

    // Capturing impressionURL,clickURL,winNotifyUrl & transactionID from bid response
    captureBidResponseData(result);

    // Hitting the impression URL - 5 times,Click URl 1 time and ImpressionURL 5 more times
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);
    ServerRequestUtility.hitURL(clickURL.replace("${AUCTION_PRICE}", "1"), 1);
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID1.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
    verify eCPC Spend , Placement Optimization Targets (0.01 eCPC, 0.05 Spend)
    generate one more impression.Expecting bid request to be successful.
  */
  @Test(priority = 4)
  public void verify_eCPC_Spend_Limit_Exceeded() {

    // Hitting the impression URL - 2 times,making ImpressionURL exceed placement limit of 10
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 2);

    // Wait time is needed for the server to pick up aerospike data
    ServerRequestUtility.sleep(45);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID1.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  // *************** eCPA *************** //

  /*
    verify eCPA Impressions , Placement Optimization Targets (0.01 eCPA, 10 Impressions)
    generate 1 click and 10 impressions.Expecting bid request to be successful.
  */
  @Test(priority = 5)
  public void verify_eCPA_Impressions() {

    // Update Placement
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint,
        placementRequestEndPoint,
        auth,
        placementID,
        "{\"optimizationTarget\": 0.01,\"optimizationTargetTypeId\": 2,\"optimizationSampleSize\": 10,\"optimizationSampleSizeTypeId\": 1}");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    // POST REQUEST TO DSP SERVER, CAPTURE DATA
    result =
        ServerRequestUtility.postBidRequest(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID2.json");
    Assert.assertTrue(result.size() == 4, "Bid Request is not successful");

    // Capturing impressionURL,clickURL,winNotifyUrl & transactionID from bid response
    captureBidResponseData(result);

    // Hitting the impression URL - 5 times,Click URl 1 time and ImpressionURL 5 more times
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);
    ServerRequestUtility.hitURL(clickURL.replace("${AUCTION_PRICE}", "1"), 1);
    ServerRequestUtility.hitConversionURL(transactionID, runtimeEndPoint, 1);
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);

    // Wait time is needed for the server to pick up aerospike data
    ServerRequestUtility.sleep(45);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID2.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
   verify eCPA Impressions , Placement Optimization Targets (0.01 eCPA, 10 Impressions)
   generate 1 more impressions.Expecting bid request to be successful.
  */
  @Test(priority = 6)
  public void verify_eCPA_Impressions_Limit_Exceeded() {

    // Hitting the impression URL - 1 times,making ImpressionURL exceed placement limit of 10
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 2);

    // Wait time is needed for the server to pick up aerospike data
    ServerRequestUtility.sleep(45);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID2.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
    verify eCPA Spend , Placement Optimization Targets (0.01 eCPC, 0.05 Spend)
    generate 1 click and 10 impressions.Expecting bid request to be successful.
  */
  @Test(priority = 7)
  public void verify_eCPA_Spend() {

    // Update Placement
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint,
        placementRequestEndPoint,
        auth,
        placementID,
        "{\"optimizationTarget\": 0.01,\"optimizationTargetTypeId\": 2,\"optimizationSampleSize\": 0.05,\"optimizationSampleSizeTypeId\": 3}");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    // POST REQUEST TO DSP SERVER, CAPTURE DATA
    HashMap<String, String> result =
        ServerRequestUtility.postBidRequest(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID3.json");
    Assert.assertTrue(result.size() == 4, "Bid Request is not successful");

    // Capturing impressionURL,clickURL,winNotifyUrl & transactionID from bid response
    captureBidResponseData(result);

    // Hitting the impression URL - 5 times,Click URl 1 time and ImpressionURL 5 more times
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);
    ServerRequestUtility.hitURL(clickURL.replace("${AUCTION_PRICE}", "1"), 1);
    ServerRequestUtility.hitConversionURL(transactionID, runtimeEndPoint, 1);
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);

    // Wait time is needed for the server to pick up aerospike data
    ServerRequestUtility.sleep(45);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID3.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
    verify eCPA Spend , Placement Optimization Targets (0.01 eCPA, 0.05 Spend)
    generate one more impression.Expecting bid request to be successful.
  */
  @Test(priority = 8)
  public void verify_eCPA_Spend_Limit_Exceeded() {

    // Hitting the impression URL - 2 times,making ImpressionURL exceed placement limit of 10
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 2);

    // Wait time is needed for the server to pick up aerospike data
    ServerRequestUtility.sleep(45);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID3.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  // *************** CTR *************** //

  /*
   verify CTR Impressions , Placement Optimization Targets (50% CTR, 10 Impressions)
   generate 4 clicks and 10 impressions.Expecting bid request to be Unsuccessful.
  */
  @Test(priority = 9)
  public void verify_CTR_Impressions() {

    // Update Placement
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint,
        placementRequestEndPoint,
        auth,
        placementID,
        "{\"optimizationTarget\": 50,\"optimizationTargetTypeId\": 3,\"optimizationSampleSize\": 10,\"optimizationSampleSizeTypeId\": 1}");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    // POST REQUEST TO DSP SERVER, CAPTURE DATA
    result =
        ServerRequestUtility.postBidRequest(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID4.json");
    Assert.assertTrue(result.size() == 4, "Bid Request is not successful");

    // Capturing impressionURL,clickURL,winNotifyUrl & transactionID from bid response
    captureBidResponseData(result);

    // Hitting the impression URL - 5 times,Click URl 4 time and ImpressionURL 5 more times
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);
    ServerRequestUtility.hitURL(clickURL.replace("${AUCTION_PRICE}", "1"), 4);
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);

    // Wait time is needed for the server to pick up aerospike data
    ServerRequestUtility.sleep(45);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID4.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
   verify eCPA Impressions , Placement Optimization Targets (50% CTR, 10 Impressions)
   generate 1 more impressions.Expecting bid request to be successful.
  */
  @Test(priority = 10)
  public void verify_CTR_Impressions_Limit_Exceeded() {

    // Hitting the click URL - 2 more times, click URL hit 6 times in total, impression URl hit 12
    // times
    ServerRequestUtility.hitURL(clickURL.replace("${AUCTION_PRICE}", "1"), 2);
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 2);

    // Wait time is needed for the server to pick up aerospike data
    ServerRequestUtility.sleep(45);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID4.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
    verify eCPA Spend , Placement Optimization Targets (50% CTR, 0.05 Spend)
    generate 4 clicks and 10 impressions.Expecting bid request to be successful.
  */
  @Test(priority = 11)
  public void verify_CTR_Spend() {

    // Update Placement
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint,
        placementRequestEndPoint,
        auth,
        placementID,
        "{\"optimizationTarget\": 50,\"optimizationTargetTypeId\": 3,\"optimizationSampleSize\": 0.05,\"optimizationSampleSizeTypeId\": 3}");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    // POST REQUEST TO DSP SERVER, CAPTURE DATA
    result =
        ServerRequestUtility.postBidRequest(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID5.json");
    Assert.assertTrue(result.size() == 4, "Bid Request is not successful");

    // Capturing impressionURL,clickURL,winNotifyUrl & transactionID from bid response
    captureBidResponseData(result);

    // Hitting the impression URL - 5 times,Click URl 1 time and ImpressionURL 5 more times
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);
    ServerRequestUtility.hitURL(clickURL.replace("${AUCTION_PRICE}", "1"), 4);
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 5);

    // Wait time is needed for the server to pick up aerospike data
    ServerRequestUtility.sleep(45);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID5.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
    verify eCPA Spend , Placement Optimization Targets (50% CTR, 0.05 Spend)
    generate one more impression.Expecting bid request to be successful.
  */
  @Test(priority = 12)
  public void verify_CTR_Spend_Limit_Exceeded() {

    // Hitting the click URL - 2 more times, click URL hit 6 times in total, impression URl hit 12
    // times
    ServerRequestUtility.hitURL(clickURL.replace("${AUCTION_PRICE}", "1"), 2);
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1"), 2);

    // Wait time is needed for the server to pick up aerospike data
    ServerRequestUtility.sleep(45);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "DynamicCapping/runTimeRequest_newBundleID5.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  // *********** UTILITY METHODS ************ //
  public void captureBidResponseData(HashMap<String, String> result) {

    for (HashMap.Entry<String, String> entry : result.entrySet()) {
      if (entry.getKey().equals("impressionURL")) {
        impressionURL = entry.getValue();
        LOG.info("IMPRESSION URL " + " -- " + impressionURL);
      }
      if (entry.getKey().equals("clickURL")) {
        clickURL = entry.getValue();
        LOG.info("CLICK URL " + " -- " + clickURL);
      }
      if (entry.getKey().equals("winNotifyUrl")) {
        winNotifyUrl = entry.getValue();
        LOG.info("WIN NOTIFY URL " + " -- " + winNotifyUrl);
      }
      if (entry.getKey().equals("transactionID")) {
        transactionID = entry.getValue().toString().replaceAll("[\\[\\]]", "");
        LOG.info("transactionID " + " -- " + transactionID);
      }
    }

    // Save Impression Url, Click URl, WinNotify URL & No of times ImpressionURL is supposed to be
    // hit.
    ServerRequestUtility.writePropertyFile(
        "impressionURL",
        impressionURL,
        "clickURL",
        clickURL,
        "winNotifyURL",
        winNotifyUrl,
        "noOfHitsImpressionURL",
        "10",
        "transactionID",
        transactionID,
        System.getProperty("user.dir") + "/src/main/resources/BidResponseData.Properties");
  }

}
