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

public class CostAndSpend {

  // Initiating Logger Object
  private static final Logger log = LogManager.getLogger();
  private static String creativeID;
  private static String campaignID;
  private static String lineItemID;
  private static String placementID;
  private static String env;
  private static String impressionURL;
  private static int expectedImpressions;
  private static int noOfImpressions;

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
    env = serviceEndPoint;

    String todaysDate = LocalDateTime.now().toString().replaceAll("[-:.T]", "").substring(0, 8);
    log.info("Aerospike Key - " + "sd:plac" + placementID + ":" + todaysDate);

    // Hit Impression URL after replacing auction price
    impressionURL =
        ServerRequestUtility.readDataFromPropertiesFile(
            "impressionURL",
            System.getProperty("user.dir") + "/src/main/resources/BidResponseData.Properties");
    log.info("Impression URL - " + impressionURL);
    ServerRequestUtility.hitImpressionURL(impressionURL.replace("${AUCTION_PRICE}", "1.5"), 10);

    expectedImpressions =
        Integer.parseInt(
            ServerRequestUtility.readDataFromPropertiesFile(
                "noOfHitsImpressionURL",
                System.getProperty("user.dir") + "/src/main/resources/BidResponseData.Properties"));
    log.info("Expected impressions - " + expectedImpressions);

    // getting data from aerospike
    log.info("Capturing Areospike data");
    String aeroSpikeData =
        AeroSpikeUtility.LogInToAerospikeExecuteAqlQueryAndReturnResponse(
            env, "dsp", "counters", "sd:plac" + placementID + ":" + todaysDate);
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
}
