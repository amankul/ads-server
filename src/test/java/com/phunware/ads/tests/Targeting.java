package com.phunware.ads.tests;

import com.phunware.ads.utilities.*;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

public class Targeting {


    //Initiating Logger Object
    private static final Logger log = LogManager.getLogger();

    private static String creativeID;
    private static String campaignID;
    private static String lineItemID;
    private static String placementID;
    private static String data;



    @BeforeClass(alwaysRun = true)
    @Parameters({"serviceEndPoint", "auth", "runtimeEndPoint", "campaignRequestEndPoint", "lineItemRequestEndPoint", "creativeRequestEndPoint", "placementRequestEndPoint"})
    public void preTestSteps(String serviceEndPoint, String auth, String runtimeEndPoint, String campaignRequestEndPoint, String lineItemRequestEndPoint, String creativeRequestEndPoint, String placementRequestEndPoint) {

        //create Campaign, LineItem & Placement & Creative
        creativeID = CreativeUtility.createCreative(serviceEndPoint, auth, creativeRequestEndPoint);
        campaignID = CampaignUtility.createCampaign(serviceEndPoint, auth, campaignRequestEndPoint);
        lineItemID = LineItemUtility.createLineItem(serviceEndPoint, auth, lineItemRequestEndPoint, campaignID);
        placementID = PlacementUtility.createPlacement(serviceEndPoint, auth, placementRequestEndPoint, creativeID, lineItemID);

        //Update earier created Campaign, Line Item & Placement to Scheduled  - `500 status ID`
        CampaignUtility.updateCampaign(serviceEndPoint, campaignRequestEndPoint, auth, campaignID, "500");
        LineItemUtility.updateLineItem(serviceEndPoint, lineItemRequestEndPoint, auth, lineItemID, "500");
        PlacementUtility.updatePlacement(serviceEndPoint, placementRequestEndPoint, auth, placementID, "500");


        //Waiting for the data generator to change status to 600 - Running
        log.info("Waiting for the data generator, Giving it time to update status from scheduled to running : Wait Time 4 minutes");
        wait(240);


        //Change log4j2.xml logger level
        AdsServerUtility.updateLog4jLoggingLevel(serviceEndPoint, "trace");
        log.info("Log4j Log Level Updated");


        //Waiting for the data generator to pick running placements
        log.info("Waiting for the data generator, to pick up running placements :  Wait Time 4 minutes");
        wait(240);

        //invoke data generator
        //invokeDataGenerator(serviceEndPoint,auth);

        //POST REQUEST TO DSP SERVER, CAPTURE DATA
        HashMap<String, String> result = ServerRequestUtility.postBidRequest(runtimeEndPoint);
        Assert.assertTrue(result.size() == 3 , "Bid Request is not successful");
        for(HashMap.Entry<String,String> entry:result.entrySet()){
            log.info(entry.getKey()+ " -- "+entry.getValue());
        }

        //wait for log file to get populated
        waitForLogsToGetPopulated(serviceEndPoint);

        //Capture Placement related data from /var/phunware/dsp/logs/abm-dsp-srv.log
        data = AdsServerUtility.logInToServerExecuteShellCommandAndReturnResponse(serviceEndPoint, "cat /var/phunware/dsp/logs/abm-dsp-srv.log | grep " + placementID);
        writeToFile(data);

        //Considering placement id 10306 for Country constraint
        log.info("Captured log lines from abm-dsp-srv.log containing Placement ID - " + placementID);

    }

    @AfterClass(alwaysRun = true)
    @Parameters({"serviceEndPoint", "auth", "campaignRequestEndPoint", "lineItemRequestEndPoint", "creativeRequestEndPoint", "placementRequestEndPoint"})
    public void postTestSteps(String serviceEndPoint, String auth, String campaignRequestEndPoint, String lineItemRequestEndPoint, String creativeRequestEndPoint, String placementRequestEndPoint)

    {
        //delete Campaign, LineItem & Placement & Creative
        CampaignUtility.deleteCampaign(serviceEndPoint, campaignRequestEndPoint, auth, campaignID);
        LineItemUtility.deleteLineItem(serviceEndPoint, lineItemRequestEndPoint, auth, lineItemID);
        CreativeUtility.deleteCreative(serviceEndPoint, creativeRequestEndPoint, auth, creativeID);
        PlacementUtility.deletePlacement(serviceEndPoint, placementRequestEndPoint, auth, placementID);
    }


    // ================================== PLACEMENT CONSTRAINTS ============================================= //

    @Test(priority = 1)
    public void verifyCountryConstraint() {
        placementConstraintValidator("CountryConstraint");
    }

    @Test(priority = 2)
    public void verifyRegionConstraint() {
        placementConstraintValidator("RegionConstraint");
    }

    @Test(priority = 3)
    public void verifyDMAConstraint() {
        placementConstraintValidator("DMAConstraint");
    }

    @Test(priority = 4)
    public void verifyCityConstraint() {
        placementConstraintValidator("CityConstraint");
    }

    @Test(priority = 5)
    public void verifyZipConstraint() {
        placementConstraintValidator("ZipConstraint");
    }

    @Test(priority = 6)
    public void verifyCarrierConstraint() {
        placementConstraintValidator("CarrierConstraint");
    }

    @Test(priority = 7)
    public void verifyLanguageConstraint() {
        placementConstraintValidator("LanguageConstraint");
    }

    @Test(priority = 8)
    public void verifyDeviceConstraint() {
        placementConstraintValidator("DeviceConstraint");
    }

    @Test(priority = 9)
    public void verifyDeviceTypeConstraint() {
        placementConstraintValidator("DeviceTypeConstraint");
    }

    @Test(priority = 10)
    public void verifyBrandConstraint() {
        placementConstraintValidator("BrandConstraint");
    }

    @Test(priority = 11)
    public void verifyHyperLocalConstraint() {
        placementConstraintValidator("HyperLocalConstraint");
    }

    @Test(priority = 12)
    public void verifyOSConstraint() {
        placementConstraintValidator("OSConstraint");
    }

    @Test(priority = 13)
    public void verifyGenderConstraint() {
        placementConstraintValidator("GenderConstraint");
    }

    @Test(priority = 14)
    public void verifyDeviceIdConstraint() {
        placementConstraintValidator("DeviceIdConstraint");
    }

    @Test(priority = 15)
    public void verifyeCPCOptimizationTargetConstraint() {
        placementConstraintValidator("eCPCOptimizationTargetConstraint");
    }

    @Test(priority = 16)
    public void verifyBlockedAdvertiserConstraint() {
        placementConstraintValidator("BlockedAdvertiserConstraint");
    }

    @Test(priority = 17)
    public void verifyBlockedCategoryConstraint() {
        placementConstraintValidator("BlockedCategoryConstraint");
    }

    @Test(priority = 18)
    public void verifyScheduleConstraint() {
        placementConstraintValidator("ScheduleConstraint");
    }

    @Test(priority = 19)
    public void verifyPmpConstraint() {
        placementConstraintValidator("PmpConstraint");
    }

    @Test(priority = 20)
    public void verifyTrafficSourceConstraint() {
        placementConstraintValidator("TrafficSourceConstraint");
    }

    @Test(priority = 21)
    public void verifyBundleIdConstraint() {
        placementConstraintValidator("BundleIdConstraint");
    }

    @Test(priority = 22)
    public void verifyDomainConstraint() {
        placementConstraintValidator("DomainConstraint");
    }


    @Test(priority = 23)
    public void verifyDeviceFrequencyCapConstraint() {
        placementConstraintValidator("DeviceFrequencyCapConstraint");
    }

    @Test(priority = 24)
    public void verifyBudgetConstraint() {
        placementConstraintValidator("BudgetConstraint");
    }

    @Test(priority = 25)
    public void verifyTargetConstraint() {
        placementConstraintValidator("TargetConstraint");
    }

    @Test(priority = 26)
    public void verifyBidFloorConstraint() {
        placementConstraintValidator("BidFloorConstraint");
    }


    // ================================== CREATIVE CONSTRAINTS ============================================= //


    @Test(priority = 27)
    public void verifyBattrConstraint() {
        creativeConstraintValidator("BattrConstraint");
    }

    @Test(priority = 28)
    public void verifyBtypeConstraint() {
        creativeConstraintValidator("BtypeConstraint");
    }

    @Test(priority = 29)
    public void verifyDimensionTypeConstraint() {
        creativeConstraintValidator("DimensionTypeConstraint");
    }

    @Test(priority = 30)
    public void verifyMimeTypeConstraint() {
        creativeConstraintValidator("MimeTypeConstraint");
    }

    @Test(priority = 31)
    public void verifySecureConstraint() {
        creativeConstraintValidator("SecureConstraint");
    }


    // ================================== UTILITY METHODS ============================================= //


    public static void placementConstraintValidator(String regexSubString) {

        // Validating presence of "Placement: `placementID` Constraint: `constraint name` is valid" in abm-dsp-srv.log file

        String regex = ".+?- (Placement.*?:.*?" + placementID + ".*?Constraint: " + regexSubString + " is valid)";
        Matcher regexMatcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(data);

        Assert.assertTrue(regexMatcher.find(), "Could not find pattern `" + regex + "` in the file abm-dsp-srv.txt");

        log.info(regexSubString + " passed, `abm-dsp-srv.log` Contains: ");
        log.info(regexMatcher.group(1));

    }

    public static void creativeConstraintValidator(String regexSubString) {

        // Validating presence of "CreativeConstraint: `constraint name` for Placement id: `placementID`, Creative id: `creativeID` is VALID"

        String regex = ".+?(CreativeConstraint.*?:.*?" + regexSubString + ".*?Placement id:.*?" + placementID + ".*?Creative id:.*?" + creativeID + ".*?is VALID)";
        Matcher regexMatcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(data);

        Assert.assertTrue(regexMatcher.find(), "Could not find pattern `" + regex + "` in the file abm-dsp-srv.txt");

        log.info(regexSubString + " passed, `abm-dsp-srv.log` Contains: ");
        log.info(regexMatcher.group(1));

    }


    public static void writeToFile(String data) {

        try {
            File myFile = new File(System.getProperty("user.dir") + "/src/main/resources/abm-dsp-srv.txt");
            FileOutputStream pemFileStream = new FileOutputStream(myFile, false);
            pemFileStream.write(data.getBytes());
            pemFileStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void wait(int timeInSeconds) {
        try {
            Thread.sleep(timeInSeconds * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void waitForLogsToGetPopulated(String serviceEndPoint){

        int retry = 12;
        int count=0;
        String size = "";

        while (retry >= count) {
            log.info("Waiting for abm-dsp-srv.log to get populated with constraint data ..");
            wait(5);
            //looking for String "Considering placement id `placementID` for Country constraint"
            size = AdsServerUtility.logInToServerExecuteShellCommandAndReturnResponse(serviceEndPoint, "cat /var/phunware/dsp/logs/abm-dsp-srv.log | grep " + "\"Considering placement id " +placementID +" for Country constraint\"");
            count++;
            if (size.length()>10){
                break;
            }
        }
    }

    public static void invokeDataGenerator(String serviceEndPoint , String auth ){

        String requestURL = serviceEndPoint +"/api/v1.0/datagenerator";

        //Printing Request Details
        log.debug("REQUEST-URL:POST-" + requestURL);

        //Extracting response after status code validation
        Response response =
                given()
                        .header("Authorization", auth)
                        .request()
                        .get(requestURL)
                        .then()
                        .statusCode(200)
                        .extract()
                        .response();

        //printing response
        log.info("RESPONSE:" + response.asString());
        log.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");


    }

}
