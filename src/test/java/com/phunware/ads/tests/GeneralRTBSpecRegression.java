package com.phunware.ads.tests;

import com.phunware.ads.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class GeneralRTBSpecRegression {

  // Initiating Logger Object
  private static final Logger LOG = LogManager.getLogger();
  private static String creativeID;
  private static String placementID;
  private static String campaignID;
  private static String lineItemID;
  private static String serviceEndPoint;
  private static String runtimeEndPoint;
  private static String creativeRequestEndPoint;
  private static String placementRequestEndPoint;
  private static String campaignRequestEndPoint;
  private static String lineItemRequestEndPoint;
  private static String auth;

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

    this.runtimeEndPoint = runtimeEndPoint;
    this.serviceEndPoint = serviceEndPoint;
    this.creativeRequestEndPoint = creativeRequestEndPoint;
    this.placementRequestEndPoint = placementRequestEndPoint;
    this.campaignRequestEndPoint = campaignRequestEndPoint;
    this.lineItemRequestEndPoint = lineItemRequestEndPoint;
    this.auth = auth;

    // capture ID's from properties file.
    placementID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "placementId",
            System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");
    creativeID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "creativeId",
            System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");
    campaignID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "campaignId",
            System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");
    lineItemID =
        ServerRequestUtility.readDataFromPropertiesFile(
            "lineItemId",
            System.getProperty("user.dir") + "/src/main/resources/runTimeData.Properties");

    LOG.info("Placement Details ID -" + placementID);
    PlacementUtility.getPlacement(serviceEndPoint, placementRequestEndPoint, auth, placementID);
  }

  @AfterClass
  public void cleanUP() {

    // delete earlier created campaign,LineItem & placement.
    CampaignUtility.deleteCampaign(serviceEndPoint, campaignRequestEndPoint, auth, campaignID);
    LineItemUtility.deleteLineItem(serviceEndPoint, lineItemRequestEndPoint, auth, lineItemID);
    PlacementUtility.deletePlacement(serviceEndPoint, placementRequestEndPoint, auth, placementID);
    CreativeUtility.deleteCreative(serviceEndPoint, creativeRequestEndPoint, auth, creativeID);
  }

  // **************** SECURE RTB TESTS ****************

  /*
  Verify secure parameter in bid request when creative secure = true & bid request secure = false, Expecting sucessful bid response
  */
  @Test(priority = 1)
  public void verifySecureConstraint_BidSecureFalse_CreativeSecureTrue() {

    // update creative secure parameter to false
    CreativeUtility.updateCreative(
        serviceEndPoint, creativeRequestEndPoint, auth, creativeID, "{\"secure\": true}");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    LOG.info("Creative ID -" + creativeID + " has Secure set to true");

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/Secure/secureFalse.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
  Verify secure parameter in bid request when creative secure = true & bid request secure = true, Expecting successful bid response
  */
  @Test(priority = 2)
  public void verifySecureConstraint_BidSecureTrue_CreativeSecureTrue() {
    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/Secure/secureTrue.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
  Verify secure parameter in bid request when creative secure = false & bid request secure = true, Expecting failed bid response
  */
  @Test(priority = 3)
  public void verifySecureConstraint_BidSecureFalse_CreativeSecureFalse() {

    // update creative secure parameter to false
    CreativeUtility.updateCreative(
        serviceEndPoint, creativeRequestEndPoint, auth, creativeID, "{\"secure\": false}");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    LOG.info("Creative ID -" + creativeID + " has Secure set to False");
    CreativeUtility.getCreative(serviceEndPoint, creativeRequestEndPoint, auth, creativeID);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/Secure/secureTrue.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
  Verify secure parameter in bid request when creative secure = false & bid request secure = true, Expecting successful bid response
  */
  @Test(priority = 4)
  public void verifySecureConstraint_BidSecureTrue_CreativeSecureFalse() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/Secure/secureFalse.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  // **************** MimeType RTB Tests ****************

  /*
  Verify mimes parameter in bid request when creative mime = gif & bid request mime has gif as one of the array element
  */
  @Test(priority = 5)
  public void verifyMimeType_BidRequestGIF_CreativeGIF() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/MimeType/MimeGif.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
  Verify mimes parameter in bid request when creative mime = gif & bid request mime array does not have gif
  */
  @Test(priority = 6)
  public void verifyMimeType_BidRequest_NonGif_Creative_GIF() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/MimeType/MimeJPG.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  // ****************  Dimension RTB Tests ********************

  /*
  Verify mimes parameter in bid request when creative Dimension = 320X50 & bid request dimension is 320X50
  */
  @Test(priority = 7)
  public void verifyDimension_BidRequestGIF_CreativeGIF() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/Dimension/Dimension320X50.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
  Verify mimes parameter in bid request when creative Dimension = 320X50 & bid request dimension is 320X480
  */
  @Test(priority = 8)
  public void verifyDimension_BidRequest_NonGif_Creative_GIF() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/Dimension/Dimension320X480.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  // ****************  Battr RTB Tests ********************

  /*
  Verify Battr parameter in bid request when creative Battr = 1,2,3 & bid request Battr is 1
  */
  @Test(priority = 9)
  public void verifyBattr_BidRequest_Battr1_Creative_Battr_1_2_3() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/Battr/BattrID_1.json");
    Assert.assertEquals(
        statusCode, 204,ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
    Verify Battr parameter in bid request when creative Battr = 1,2,3 & bid request Battr is 4,5
  */
  @Test(priority = 10)
  public void verifyBattr_BidRequest_Battr4_5_Creative_Battr_1_2_3() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/Battr/BattrID_4_5.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  // ****************  BTYPE RTB Test ********************

  /*
    Verify bid request  with btype = 2 (banner creative should be blocked)
  */
  @Test(priority = 11)
  public void verifyBtype_2_BannerCreative() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/BType/BType_2_Banner.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  // ****************  API RTB Tests ********************

  /*
    Verify API/JS when creative is JS(Non-Rich)  & bid request jS is 0, Expected - bid request should not serve
  */
  @Test(priority = 12)
  public void verifyAPI_JS_NonRich_Creative_BidRequest_JS_0() {

    String oldCreativeID = creativeID;

    // create a new JS creative and assign it to the placement
    creativeID = CreativeUtility.createJSCreative(serviceEndPoint, auth, creativeRequestEndPoint);
    PlacementUtility.updatePlacementWithRequestBody(
        serviceEndPoint,
        placementRequestEndPoint,
        auth,
        placementID,
        "{\"creativeIds\": [\"" + creativeID + "\"]}");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    // Delete Old Creative ID
    CreativeUtility.deleteCreative(serviceEndPoint, creativeRequestEndPoint, auth, oldCreativeID);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/API_JavaScript/JS_0.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
   Verify API/JS when creative is JS(Non-Rich)  & bid request jS is 1, Expected - bid request should  serve
  */
  @Test(priority = 13)
  public void verifyAPI_JS_NonRich_Creative_BidRequest_JS_1() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/API_JavaScript/JS_1.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
   Verify API/JS when creative is JS(Rich)  & bid request jS is 0, Expected - bid request should not serve
  */
  @Test(priority = 14)
  public void verifyAPI_JS_Rich_Creative_BidRequest_JS_0() {

    // update  JS creative to "mediaCapabilityId": 2 - RICH
    CreativeUtility.updateCreative(
        serviceEndPoint, creativeRequestEndPoint, auth, creativeID, "{\"mediaCapabilityId\": 2}");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/API_JavaScript/JS_0.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
   Verify API/JS when creative is JS(Rich)  & bid request jS is 1, Expected - bid request should  serve
  */
  @Test(priority = 15)
  public void verifyAPI_JS_Rich_Creative_BidRequest_JS_1() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/API_JavaScript/JS_1.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
  Verify API/JS when creative is JS(MRAID), bid request JS is 0 and bidrequest has framwork ID 3 or 5 , Expected - bid request should not serve
  */
  @Test(priority = 16)
  public void verifyAPI_JS_MRAID_Creative_BidRequest_JS_0_API_3_5() {

    // Update  JS creative to "mediaCapabilityId": 3 - IN APP RICH MEDIA
    CreativeUtility.updateCreative(
        serviceEndPoint, creativeRequestEndPoint, auth, creativeID, "{\"mediaCapabilityId\": 3}");

    // waiting for DG
    ServerRequestUtility.waitForDataGenerator(serviceEndPoint, auth);

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/API_JavaScript/JS_0_API_3_5.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
  Verify API/JS when creative is JS(MRAID), bid request JS is 1 and bidrequest has framwork ID 3 or 5 , Expected - bid request should  serve
  */
  @Test(priority = 17)
  public void verifyAPI_JS_MRAID_Creative_BidRequest_JS_1_API_3_5() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/API_JavaScript/JS_1_API_3_5.json");
    Assert.assertEquals(
        statusCode, 200, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
  Verify API/JS when creative is JS(MRAID), bid request JS is 0 and bidrequest has framwork ID 3 or 5 , Expected - bid request should not serve
  */
  @Test(priority = 18)
  public void verifyAPI_JS_MRAID_Creative_BidRequest_JS_0_API_1_2_4() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/API_JavaScript/JS_0_API_1_2_4.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  /*
  Verify API/JS when creative is JS(MRAID), bid request JS is 1 and bidrequest has framwork ID 1,2 or 4 , Expected - bid request should not serve
  */
  @Test(priority = 19)
  public void verifyAPI_JS_MRAID_Creative_BidRequest_JS_1_API_1_2_4() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/API_JavaScript/JS_1_API_1_2_4.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }

  // ****************  BTYPE RTB Tests ********************

  /*
  Verify bid request with btype = 3 (JS creative should be blocked)
  */
  @Test(priority = 20)
  public void verifyBtype_3_JSCreative() {

    int statusCode =
        ServerRequestUtility.postBidRequest_NoSucessCheck(
            runtimeEndPoint, "RTBSpecRegression/BType/BType_3_JavaScript.json");
    Assert.assertEquals(
        statusCode, 204, ServerRequestUtility.getLogData(serviceEndPoint,placementID));
  }


}
