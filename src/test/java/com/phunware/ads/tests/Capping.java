package com.phunware.ads.tests;

import com.phunware.ads.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Capping {

  // Initiating Logger Object
  private static final Logger log = LogManager.getLogger();
  private static String creativeID;
  private static String campaignID;
  private static String lineItemID;
  private static String placementID;
  private static String serviceEndPoint;
  private static String lineItemRequestEndPoint;
  private static String campaignRequestEndPoint;
  private static String placementRequestEndPoint;
  private static String runtimeEndPoint;
  private static String auth;
  private static String impressionURL;
  private static String clickURL;
  private static String winNotifyUrl;
  private static int noOfImpressions;
  private static double aerospikeCost;
  private static double expectedCost;
  private static double aerospikeSpend;
  private static double expectedSpend;

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
    creativeID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "creativeId",
            System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");
    this.serviceEndPoint = serviceEndPoint;
    this.lineItemRequestEndPoint = lineItemRequestEndPoint;
    this.placementRequestEndPoint = placementRequestEndPoint;
    this.campaignRequestEndPoint = campaignRequestEndPoint;
    this.runtimeEndPoint = runtimeEndPoint;
    this.auth = auth;

    // Capture Impression URL
    impressionURL =
        ServerRequestUtility.readDataFromPropertiesFile(
            "impressionURL",
            System.getProperty("user.dir") + "/src/main/resources/BidResponseData.Properties");
    log.info("Impression URL - " + impressionURL);
  }

  @AfterClass(alwaysRun = true)
  @Parameters({
    "serviceEndPoint",
    "auth",
    "campaignRequestEndPoint",
    "lineItemRequestEndPoint",
    "creativeRequestEndPoint",
    "placementRequestEndPoint"
  })
  public void postTestSteps(
      String serviceEndPoint,
      String auth,
      String campaignRequestEndPoint,
      String lineItemRequestEndPoint,
      String creativeRequestEndPoint,
      String placementRequestEndPoint) {

    //        delete Campaign, LineItem & Placement & Creative
    //        CampaignUtility.deleteCampaign(serviceEndPoint, campaignRequestEndPoint, auth,
    // campaignID);
    //        LineItemUtility.deleteLineItem(serviceEndPoint, lineItemRequestEndPoint, auth,
    // lineItemID);
    //        CreativeUtility.deleteCreative(serviceEndPoint, creativeRequestEndPoint, auth,
    // creativeID);
    //        PlacementUtility.deletePlacement(serviceEndPoint, placementRequestEndPoint, auth,
    // placementID);
  }

  // *****************  BUDGET CAP TESTS *************//

  /*
  Verify Placement budget cap limit when budget for Campaign=$100.03 , LineItem =$10.04 & Placement=$1.00
  */
  @Test(priority = 1)
  public void verifyBudgetCap_PlacementBudget() {

    // 0.05 of budget is already used, trying to reach rest of the placement budget (0.95) to hit
    // cap mark.

    // Updating lineitem retail price to 95
    LineItemUtility.updateLineItemUsingRequestBody(
        serviceEndPoint, lineItemRequestEndPoint, auth, lineItemID, "{\"retailRate\": 95}");

    // remove optimization target details from the placement.
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint,
        placementRequestEndPoint,
        auth,
        placementID,
        "{\"optimizationTarget\": null,\"optimizationTargetTypeId\": null,\"optimizationSampleSizeTypeId\": null,\"optimizationSampleSize\": null,\"optimizationEmailNotification\": null}");

    //waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint,auth);

    // Hitting the impression URL - 10 times
    ServerRequestUtility.hitImpressionURL(impressionURL.replace("${AUCTION_PRICE}", "1.5"), 10);

    // NOTE - LineItem with retail price 95 is hit 10 times (0.95) + earlier 0.05 making it match
    // the 1$ placement cap.

    // Hitting impression url one more time to cross budget cap
    ServerRequestUtility.hitImpressionURL(impressionURL.replace("${AUCTION_PRICE}", "1.5"), 1);

    getDataFromAeroSpike();

    // Sending Bid request after the placement cap is reached.
    ServerRequestUtility.postBidRequest_NoSucessCheck(runtimeEndPoint,"runTimeRequest.json");

    // Capture Placement related data from /var/phunware/dsp/logs/abm-dsp-srv.log
    String data =
        ServerRequestUtility.waitForLogsToGetPopulated(
            serviceEndPoint,
            "grep \"Placement: "
                + placementID
                + " Constraint: BudgetConstraint\" /var/phunware/dsp/logs/abm-dsp-srv.log | tail -1");


    // looking for BudgetConstraint invalidation in logs
    Assert.assertTrue(
        data.contains(placementID + " Constraint: BudgetConstraint is INVALID"),
        "Found in logs -> " + data);
  }

  /*
  Verify Line Item budget cap limit when budget for Campaign=$100.03 , LineItem =$2.00 & Placement=$10.00
  */
  @Test(priority = 2)
  public void verifyBudgetCap_LineItemBudget() {

    // 1.095 of budget is already used, trying to reach rest of the LI budget (0.905) to hit cap
    // mark.

    // Updating lineitem budget,retail rate and placement budgets
    LineItemUtility.updateLineItemUsingRequestBody(
        serviceEndPoint,
        lineItemRequestEndPoint,
        auth,
        lineItemID,
        "{\"budgetDaily\": 2.00,\"retailRate\": 90.5}");
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint, placementRequestEndPoint, auth, placementID, "{\"budgetDaily\": 10.00}");

      //waiting for DG
      ServerRequestUtility.waitForDataGenerator(serviceEndPoint,auth);

    // Hitting the impression URL - 10 times
    ServerRequestUtility.hitImpressionURL(impressionURL.replace("${AUCTION_PRICE}", "1.5"), 10);

    // NOTE - LineItem with retail price 90.5 is hit 10 times (0.905) + earlier 1.095 making it
    // match the 2$ LineItem cap.

    // Hitting impression url one more time to cross budget cap
    ServerRequestUtility.hitImpressionURL(impressionURL.replace("${AUCTION_PRICE}", "1.5"), 1);
    getDataFromAeroSpike();

    // Sending Bid request after the placement cap is reached.
    int statusCode = ServerRequestUtility.postBidRequest_NoSucessCheck(runtimeEndPoint,"runTimeRequest.json");
    Assert.assertEquals(
        statusCode, 204, "Status code returned after sending a bidrequest -" + statusCode);
  }


    // *****************  FREQUENCY CAP TESTS *************//

    /*
  Verify Frequency cap limit when budget for Campaign=$100.03 , LineItem =$10.00 & Placement=$10.00
  set placement - "dailyFcap": 10
  */

    @Test(priority = 3)
    public void verifyFrequencyCap() {

        LineItemUtility.updateLineItemUsingRequestBody(
                serviceEndPoint, lineItemRequestEndPoint, auth, lineItemID, "{\"budgetDaily\": 10.00}");

        PlacementUtility.updatePlacementWithRequestBody(
                serviceEndPoint, placementRequestEndPoint, auth, placementID, "{\"dailyFcap\": 10}");

        //waiting for DG
        ServerRequestUtility.waitForDataGenerator(serviceEndPoint,auth);

        //posting bid request with new device ID
        postBidRequest("runTimeRequest_NewDeviceID1.json");

        //hitting new impression url 11 times , making it go over the frequency cap "dailyFcap --> 10" set earlier
        ServerRequestUtility.hitImpressionURL(impressionURL.replace("${AUCTION_PRICE}", "1.5"), 11);

        // Sending Bid request after the placement cap is exceeded.
        int statusCode = ServerRequestUtility.postBidRequest_NoSucessCheck(runtimeEndPoint,"runTimeRequest_NewDeviceID1.json");
        Assert.assertEquals(
                statusCode, 204, "Status code returned after sending a bidrequest -" + statusCode);

        // Capture Placement related data from /var/phunware/dsp/logs/abm-dsp-srv.log
        String data =
                ServerRequestUtility.waitForLogsToGetPopulated(
                        serviceEndPoint,
                        "grep \"Placement: "
                                + placementID
                                + " Constraint: DeviceFrequencyCapConstraint is INVALID\" /var/phunware/dsp/logs/abm-dsp-srv.log | tail -1");

        // looking for BudgetConstraint invalidation in logs
        // Expecting "Placement: placementID Constraint: DeviceFrequencyCapConstraint is INVALID"
        Assert.assertTrue(
                data.contains(placementID + " Constraint: DeviceFrequencyCapConstraint is INVALID"),
                "Found in logs -> " + data);

    }

    /*
  Verify Frequency cap limit when placement cap is exceeded & when new device ID is used in the bid request
  */
    @Test(priority = 4)
    public void verifyFrequencyCap_NewDeviceID() {

        // Sending Bid request with a new device ID - Expecting the bid request to be successful.
        int statusCode = ServerRequestUtility.postBidRequest_NoSucessCheck(runtimeEndPoint,"runTimeRequest_NewDeviceID2.json" );
        Assert.assertEquals(
                statusCode, 200, "Status code returned after sending a bidrequest -" + statusCode);

    }


    // *****************  CAPPING UTILITY METHODS *************//

    public static void getDataFromAeroSpike() {

    // Capturing todays data to get Aerospike key
    String todaysDate = LocalDateTime.now().toString().replaceAll("[-:.T]", "").substring(0, 8);
    log.info("Aerospike Key - " + "sd:plac" + placementID + ":" + todaysDate);

    // getting data from aerospike
    log.info("Capturing Areospike data");
    String aeroSpikeData =
        AeroSpikeUtility.LogInToAerospikeExecuteAqlQueryAndReturnResponse(
            serviceEndPoint, "dsp", "counters", "sd:plac" + placementID + ":" + todaysDate);
    log.info("Aero Spike Data - " + aeroSpikeData);

    String regex = ".*?cost:(.*?)\\).*spend:(.*?)\\).*?.*imp:(.*?)\\).*?";
    Matcher regexMatcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(aeroSpikeData);
    Assert.assertTrue(regexMatcher.find(), "Could not find cost and sepnd from aerospike data");

    aerospikeCost = Double.valueOf(regexMatcher.group(1));
    log.debug("Cost from Aerospike -" + regexMatcher.group(1));
    aerospikeSpend = Double.valueOf(regexMatcher.group(2));
    log.debug("Spend from Aerospike -" + regexMatcher.group(2));
    noOfImpressions = Integer.valueOf(regexMatcher.group(3));
    log.debug("Impressions from Aerospike -" + regexMatcher.group(3));
  }

  public static void postBidRequest(String fileName){

      // POST REQUEST TO DSP SERVER, CAPTURE DATA
      HashMap<String, String> result = ServerRequestUtility.postBidRequest(runtimeEndPoint ,fileName );
      Assert.assertTrue(result.size() == 3, "Bid Request is not successful");
      for (HashMap.Entry<String, String> entry : result.entrySet()) {
          if (entry.getKey().equals("impressionURL")) {
              impressionURL = entry.getValue();
              log.info("IMPRESSION URL " + " -- " + impressionURL);
          }
          if (entry.getKey().equals("clickURL")) {
              clickURL = entry.getValue();
              log.info("CLICK URL " + " -- " + clickURL);
          }
          if (entry.getKey().equals("winNotifyUrl")) {
              winNotifyUrl = entry.getValue();
              log.info("WIN NOTIFY URL " + " -- " + winNotifyUrl);
          }
      }
  }

}
