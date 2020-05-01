package com.phunware.ads.tests;

import com.phunware.ads.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Targeting {

  // Initiating Logger Object
  private static final Logger LOG = LogManager.getLogger();

  private static String creativeID;
  private static String campaignID;
  private static String lineItemID;
  private static String placementID;
  private static String data;
  private static String impressionURL;
  private static String clickURL;
  private static String winNotifyUrl;
  private static String transactionID;

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

    // invoke data generator - forcing sync - cleaning up existing data.
    ServerRequestUtility.invokeDataGenerator(serviceEndPoint, auth);

    // create Campaign, LineItem & Placement & Creative
    creativeID = CreativeUtility.createCreative(serviceEndPoint, auth, creativeRequestEndPoint);
    campaignID = CampaignUtility.createCampaign(serviceEndPoint, auth, campaignRequestEndPoint);
    lineItemID =
        LineItemUtility.createLineItem(serviceEndPoint, auth, lineItemRequestEndPoint, campaignID);
    placementID =
        PlacementUtility.createPlacement(
            serviceEndPoint, auth, placementRequestEndPoint, creativeID, lineItemID);

    // Update earlier created Campaign, Line Item & Placement to Scheduled  - `500 status ID`
    CampaignUtility.updateCampaign(
        serviceEndPoint, campaignRequestEndPoint, auth, campaignID, "500");
    LineItemUtility.updateLineItem(
        serviceEndPoint, lineItemRequestEndPoint, auth, lineItemID, "500");
    PlacementUtility.updatePlacement(
        serviceEndPoint, placementRequestEndPoint, auth, placementID, "500");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    // check placement status ID - if it is not 600 invoke data generator Again
    if (PlacementUtility.getStatusId(serviceEndPoint, placementRequestEndPoint, auth, placementID)
        != 600) {
      // waiting for DG
      ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);
    }

    // Change log4j2.xml logger level
    AdsServerUtility.updateLog4jLoggingLevel(serviceEndPoint, "trace");
    LOG.info("Log4j Log Level Updated");

    // POST REQUEST TO DSP SERVER, CAPTURE DATA
    HashMap<String, String> result =
        ServerRequestUtility.postBidRequest(runtimeEndPoint, "runTimeRequest.json");
    Assert.assertTrue(result.size() == 4, "Bid Request is not successful");
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
        transactionID = entry.getValue();
        LOG.info("transactionID " + " -- " + transactionID);
      }
    }

    // wait for LOG file to get populated
    ServerRequestUtility.waitForLogsToGetPopulated(
        serviceEndPoint,
        "cat /var/phunware/dsp/logs/abm-dsp-srv.log | grep "
            + "\"Considering placement id "
            + placementID
            + " for Country constraint\"");

    // Capture Placement related data from /var/phunware/dsp/logs/abm-dsp-srv.log
    data =
        AdsServerUtility.logInToServerExecuteShellCommandAndReturnResponse(
            serviceEndPoint, "cat /var/phunware/dsp/logs/abm-dsp-srv.log | grep " + placementID);
    ServerRequestUtility.writeToFile(
        data, System.getProperty("user.dir") + "/src/main/resources/abm-dsp-srv.txt");

    LOG.info("Captured log lines from abm-dsp-srv.log containing Placement ID - " + placementID);
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
        "transactionID",
        transactionID,
        System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");

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

  // ================================== PLACEMENT CONSTRAINTS  ============================================= //

  /*
  Verify successful Country Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 1)
  public void verifyCountryConstraint() {
    placementConstraintValidator("CountryConstraint");
  }

  /*
  Verify successful Region Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 2)
  public void verifyRegionConstraint() {
    placementConstraintValidator("RegionConstraint");
  }

  /*
  Verify successful DMA Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 3)
  public void verifyDMAConstraint() {
    placementConstraintValidator("DMAConstraint");
  }

  /*
  Verify successful City Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 4)
  public void verifyCityConstraint() {
    placementConstraintValidator("CityConstraint");
  }

  /*
  Verify successful Zip Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 5)
  public void verifyZipConstraint() {
    placementConstraintValidator("ZipConstraint");
  }

  /*
  Verify successful Carrier Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 6)
  public void verifyCarrierConstraint() {
    placementConstraintValidator("CarrierConstraint");
  }

  /*
  Verify successful Language Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 7)
  public void verifyLanguageConstraint() {
    placementConstraintValidator("LanguageConstraint");
  }

  /*
  Verify successful Device Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 8)
  public void verifyDeviceConstraint() {
    placementConstraintValidator("DeviceConstraint");
  }

  /*
  Verify successful DeviceType Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 9)
  public void verifyDeviceTypeConstraint() {
    placementConstraintValidator("DeviceTypeConstraint");
  }

  /*
  Verify successful Brand Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 10)
  public void verifyBrandConstraint() {
    placementConstraintValidator("BrandConstraint");
  }

  /*
  Verify successful Hyper Local Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 11)
  public void verifyHyperLocalConstraint() {
    placementConstraintValidator("HyperLocalConstraint");
  }

  /*
  Verify successful OS Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 12)
  public void verifyOSConstraint() {
    placementConstraintValidator("OSConstraint");
  }

  /*
  Verify successful Gender Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 13)
  public void verifyGenderConstraint() {
    placementConstraintValidator("GenderConstraint");
  }

  /*
  Verify successful DevideID Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 14)
  public void verifyDeviceIdConstraint() {
    placementConstraintValidator("DeviceIdConstraint");
  }

  /*
  Verify successful CPC Optimization Target Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 15)
  public void verifyeCPCOptimizationTargetConstraint() {
    placementConstraintValidator("eCPCOptimizationTargetConstraint");
  }

  /*
  Verify successful Blocked Advertiser Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 16)
  public void verifyBlockedAdvertiserConstraint() {
    placementConstraintValidator("BlockedAdvertiserConstraint");
  }

  /*
  Verify successful Blocked Category Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 17)
  public void verifyBlockedCategoryConstraint() {
    placementConstraintValidator("BlockedCategoryConstraint");
  }

  /*
  Verify successful Schedule Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 18)
  public void verifyScheduleConstraint() {
    placementConstraintValidator("ScheduleConstraint");
  }

  /*
  Verify successful PMP Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 19)
  public void verifyPmpConstraint() {
    placementConstraintValidator("PmpConstraint");
  }

  /*
  Verify successful TrafficSource Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 20)
  public void verifyTrafficSourceConstraint() {
    placementConstraintValidator("TrafficSourceConstraint");
  }

  /*
  Verify successful BundleId Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 21)
  public void verifyBundleIdConstraint() {
    placementConstraintValidator("BundleIdConstraint");
  }

  /*
  Verify successful Domain Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 22)
  public void verifyDomainConstraint() {
    placementConstraintValidator("DomainConstraint");
  }

  /*
  Verify successful DeviceFrequency Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 23)
  public void verifyDeviceFrequencyCapConstraint() {
    placementConstraintValidator("DeviceFrequencyCapConstraint");
  }

  /*
  Verify successful Budget Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 24)
  public void verifyBudgetConstraint() {
    placementConstraintValidator("BudgetConstraint");
  }

  /*
  Verify successful Target Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 25)
  public void verifyTargetConstraint() {
    placementConstraintValidator("TargetConstraint");
  }

  /*
  Verify successful Bid Floor Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 26)
  public void verifyBidFloorConstraint() {
    placementConstraintValidator("BidFloorConstraint");
  }

  // ================================== CREATIVE CONSTRAINTS  ============================================= //
  /*
  Verify successful battr Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 27)
  public void verifyBattrConstraint() {
    creativeConstraintValidator("BattrConstraint");
  }

  /*
  Verify successful Btype Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 28)
  public void verifyBtypeConstraint() {
    creativeConstraintValidator("BtypeConstraint");
  }

  /*
  Verify successful Dimension Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 29)
  public void verifyDimensionTypeConstraint() {
    creativeConstraintValidator("DimensionTypeConstraint");
  }

  /*
  Verify successful MimeType Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 30)
  public void verifyMimeTypeConstraint() {
    creativeConstraintValidator("MimeTypeConstraint");
  }

  /*
  Verify successful Secure Constraint validation in logs  by sending a bid request that would match the placement created during the test run.
  */
  @Test(priority = 31)
  public void verifySecureConstraint() {
    creativeConstraintValidator("SecureConstraint");
  }

  // ================================== UTILITY METHODS ============================================= //

  public static void placementConstraintValidator(String regexSubString) {

    // Validating presence of "Placement: `placementID` Constraint: `constraint name` is valid" in
    // abm-dsp-srv.LOG file

    String regex =
        ".+?- (Placement.*?:.*?" + placementID + ".*?Constraint: " + regexSubString + " is valid)";
    Matcher regexMatcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(data);

    Assert.assertTrue(
        regexMatcher.find(), "Could not find pattern `" + regex + "` in the file abm-dsp-srv.txt");

    LOG.info(regexSubString + " passed, `abm-dsp-srv.log` Contains: ");
    LOG.info(regexMatcher.group(1));
  }

  public static void creativeConstraintValidator(String regexSubString) {

    // Validating presence of "CreativeConstraint: `constraint name` for Placement id:
    // `placementID`, Creative id: `creativeID` is VALID"

    String regex =
        ".+?(CreativeConstraint.*?:.*?"
            + regexSubString
            + ".*?Placement id:.*?"
            + placementID
            + ".*?Creative id:.*?"
            + creativeID
            + ".*?is VALID)";
    Matcher regexMatcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(data);

    Assert.assertTrue(
        regexMatcher.find(), "Could not find pattern `" + regex + "` in the file abm-dsp-srv.txt");

    LOG.info(regexSubString + " passed, `abm-dsp-srv.log` Contains: ");
    LOG.info(regexMatcher.group(1));
  }
}
