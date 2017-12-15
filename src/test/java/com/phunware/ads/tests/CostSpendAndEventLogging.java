package com.phunware.ads.tests;

import com.phunware.ads.utilities.AeroSpikeUtility;
import com.phunware.ads.utilities.ServerRequestUtility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CostSpendAndEventLogging {

  // Initiating Logger Object
  private static final Logger LOG = LogManager.getLogger();
  private static String creativeID;
  private static String campaignID;
  private static String lineItemID;
  private static String placementID;
  private static String serviceEndPoint;
  private static String impressionURL;
  private static String clickURL;
  private static String winNotifyURL;
  private static String transactionID;
  private static int expectedImpressions;
  private static int noOfImpressions;
  private static String auth;

  // retailrateLineItem & bidPlacement values are taken from the Lineitem and Placement created.
  private static double retailRateLineItem = 5.00;
  private static double auctionPrice = 1.50;

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

    this.auth = auth;
    this.serviceEndPoint = serviceEndPoint;

    // capture ID's from properties file.
    placementID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "placementId",
            System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");

    String todaysDate = LocalDateTime.now().toString().replaceAll("[-:.T]", "").substring(0, 8);
    LOG.info("Aerospike Key - " + "sd:plac" + placementID + ":" + todaysDate);

    // capture and Hit URL's after replacing auction price
    impressionURL =
        ServerRequestUtility.readDataFromPropertiesFile(
            "impressionURL",
            System.getProperty("user.dir") + "/src/main/resources/BidResponseData.Properties");
    LOG.info("Impression URL - " + impressionURL);
    ServerRequestUtility.hitURL(impressionURL.replace("${AUCTION_PRICE}", "1.5"), 10);

    clickURL =
        ServerRequestUtility.readDataFromPropertiesFile(
            "clickURL",
            System.getProperty("user.dir") + "/src/main/resources/BidResponseData.Properties");
    LOG.info("Click URL - " + clickURL);
    ServerRequestUtility.hitURL(clickURL.replace("${AUCTION_PRICE}", "1.5"), 5);

    winNotifyURL =
        ServerRequestUtility.readDataFromPropertiesFile(
            "winNotifyURL",
            System.getProperty("user.dir") + "/src/main/resources/BidResponseData.Properties");
    LOG.info("Win Notify URL - " + winNotifyURL);
    ServerRequestUtility.hitURL(winNotifyURL.replace("${AUCTION_PRICE}", "1.5"), 5);

    transactionID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "transactionID",
            System.getProperty("user.dir") + "/src/main/resources/BidResponseData.Properties");
    ServerRequestUtility.hitConversionURL(transactionID, runtimeEndPoint, 5);

    expectedImpressions =
        Integer.parseInt(
            ServerRequestUtility.readDataFromPropertiesFile(
                "noOfHitsImpressionURL",
                System.getProperty("user.dir") + "/src/main/resources/BidResponseData.Properties"));
    LOG.info("Expected impressions - " + expectedImpressions);

    ServerRequestUtility.sleep(45);

    // getting data from aerospike
    LOG.info("Capturing Areospike data");
    String aeroSpikeData =
        AeroSpikeUtility.LogInToAerospikeExecuteAqlQueryAndReturnResponse(
            CostSpendAndEventLogging.serviceEndPoint,
            "dsp",
            "counters",
            "sd:plac" + placementID + ":" + todaysDate);
    LOG.info("Aero Spike Data - " + aeroSpikeData);

    String regex = ".*?cost:(.*?)\\).*spend:(.*?)\\).*?.*imp:(.*?)\\).*?";
    Matcher regexMatcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(aeroSpikeData);
    Assert.assertTrue(regexMatcher.find(), "Could not find cost and sepnd from aerospike data");

    aerospikeCost = Double.valueOf(regexMatcher.group(1));
    LOG.debug("Cost from Aerospike -" + regexMatcher.group(1));
    aerospikeSpend = Double.valueOf(regexMatcher.group(2));
    LOG.debug("Spend from Aerospike -" + regexMatcher.group(2));
    noOfImpressions = Integer.valueOf(regexMatcher.group(3));
    LOG.debug("Impressions from Aerospike -" + regexMatcher.group(3));
  }

  /*
  Verify Spend data for a placement in arerospike.
  */
  @Test(priority = 1)
  public void verifySpend() {
    // Aerospike Spend = (LineItem retailPrice * No Of Times Impression URL is hit)/1000
    expectedSpend = (retailRateLineItem * 10) / 1000;
    Double expectedSpendInMicroDollars = expectedSpend * Math.pow(10, 6);
    Assert.assertEquals(
        aerospikeSpend,
        expectedSpendInMicroDollars,
        "Expected spend - "
            + expectedSpendInMicroDollars
            + "Does not match with "
            + aerospikeSpend);
  }

  /*
   Verify Cost data for a placement in arerospike.
  */ @Test(priority = 2)
  public void verifyCost() {
    // Aerospike Cost = (Auction price * No Of Times Impression URL is hit)/1000
    expectedCost = (auctionPrice * 10) / 1000;
    Double expectedCostInMicroDollars = expectedCost * Math.pow(10, 6);
    Assert.assertEquals(
        aerospikeCost,
        expectedCostInMicroDollars,
        "Expected Cost - " + expectedCostInMicroDollars + "Does not match with " + aerospikeCost);
  }

  /*
   Verify Number of Impressions for a placement in arerospike.
  */ @Test(priority = 3)
  public void verifyNoOfImpressions() {
    Assert.assertEquals(
        noOfImpressions,
        expectedImpressions,
        " Number of Impressions in Areospike -"
            + noOfImpressions
            + " Does not match with Expected Impressions -"
            + expectedImpressions);
  }

  //  ******** EventLogging Tests *******
  /*
  TODO - Event logging functionality is not working as expected on stage.
  */

  @Test(priority = 4)
  public void verifyES() {
    ServerRequestUtility.getPlacementDataFromELasticSearch(serviceEndPoint, placementID, auth);
  }
}
