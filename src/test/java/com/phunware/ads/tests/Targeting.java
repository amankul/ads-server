package com.phunware.ads.tests;

import com.phunware.ads.utilities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class Targeting {


    //Initiating Logger Object
    private static final Logger log = LogManager.getLogger();

    private static String creativeID;
    private static String campaignID;
    private static String lineItemID;
    private static String placementID;

    private static String auth;
    private static String serviceEndPoint;
    private static String campaignRequestEndPoint;
    private static String lineItemRequestEndPoint;
    private static String placementRequestEndPoint;
    private static String creativeRequestEndPoint;


    @BeforeClass(alwaysRun = true)
    @Parameters({"serviceEndPoint", "auth", "campaignRequestEndPoint", "lineItemRequestEndPoint", "creativeRequestEndPoint", "placementRequestEndPoint"})
    public void preTestSteps(String serviceEndPoint, String auth, String campaignRequestEndPoint, String lineItemRequestEndPoint, String creativeRequestEndPoint, String placementRequestEndPoint) {

        Targeting.serviceEndPoint = serviceEndPoint;
        Targeting.auth = auth;
        Targeting.campaignRequestEndPoint = campaignRequestEndPoint;
        Targeting.lineItemRequestEndPoint = lineItemRequestEndPoint;
        Targeting.creativeRequestEndPoint = creativeRequestEndPoint;
        Targeting.placementRequestEndPoint = placementRequestEndPoint;

        //create Campaign, LineItem & Placement & Creative
        creativeID = CreativeUtility.createCreative(serviceEndPoint, auth, creativeRequestEndPoint);
        campaignID = CampaignUtility.createCampaign(serviceEndPoint, auth, campaignRequestEndPoint);
        lineItemID = LineItemUtility.createLineItem(serviceEndPoint, auth, lineItemRequestEndPoint, campaignID);
        placementID = PlacementUtility.createPlacement(serviceEndPoint, auth, placementRequestEndPoint, creativeID, lineItemID);


        //Update earier created Campaign, Line Item & Placement to Running  - `600 status ID`
        CampaignUtility.updateCampaign(serviceEndPoint, campaignRequestEndPoint, auth, campaignID,"600");
        LineItemUtility.updateLineItem(serviceEndPoint, lineItemRequestEndPoint, auth, lineItemID,"600");
        PlacementUtility.updatePlacement(serviceEndPoint, placementRequestEndPoint, auth, placementID,"600");


        //Change log4j2.xml logger level
        AdsServerUtility.updateLog4jLoggingLevel(serviceEndPoint,"debug","info");
        log.info("Log4j Log Level Upated");

    }


    @AfterClass(alwaysRun = true)
    public void postTestSteps() {

//        //delete Campaign, LineItem & Placement & Creative
//        CampaignUtility.deleteCampaign(serviceEndPoint, campaignRequestEndPoint, auth, campaignID);
//        LineItemUtility.deleteLineItem(serviceEndPoint, lineItemRequestEndPoint, auth, lineItemID);
//        CreativeUtility.deleteCreative(serviceEndPoint, creativeRequestEndPoint, auth, creativeID);
//        PlacementUtility.deletePlacement(serviceEndPoint, placementRequestEndPoint, auth, placementID);

    }


    @Test(priority = 1)
    public void verifyCreateCampaign() {
        log.info("TODO - ACTUAL TESTS");


    }

}
