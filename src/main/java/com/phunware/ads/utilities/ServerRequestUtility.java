package com.phunware.ads.utilities;

import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;

public class ServerRequestUtility {

    //Initiating Logger Object
    private static final Logger log = LogManager.getLogger();


    public static  HashMap <String , String>  postBidRequest(String runtimeEndPoint) {


        //Request Details
        String requestURL = runtimeEndPoint;
//        String regexSubString = runtimeEndPoint.replaceAll("http.*?//","").replace("/bidRequest","");

        String requestBody =
                JsonUtilities.jsonToString(
                        System.getProperty("user.dir")
                                + "/src/main/java/com/phunware/ads/json/runTimeRequest.json");

        //Printing Request Details
        log.info("REQUEST-URL:POST-" + requestURL);
        log.info("REQUEST-BODY:" + requestBody);

        //Extracting response after status code validation
        Response response =
                given()
                        .header("Content-Type", "application/json")
                        .request()
                        .body(requestBody)
                        .post(requestURL)
                        .then()
                        .statusCode(200)
                        .extract()
                        .response();

        //printing response
        log.info("RESPONSE:" + response.asString());
        log.debug("RESPONSE TIME :" + response.time() / 1000.0 + " Seconds");

        //Capturing data from response
        String winNotifyUrl = response.then().extract().path("seatbid.bid.nurl").toString().replaceAll("[\\[\\]]","");
        String clickAndImpressionURL = response.then().extract().path("seatbid.bid.adm").toString();


        //parsing data
        HashMap <String , String> result = new HashMap<>();
        result.put("winNotifyUrl",winNotifyUrl);
        log.debug("Win Notify URL -"+winNotifyUrl);

        Matcher regexMatcher = Pattern.compile(".+?(http.+?/click.*?)\"http.*?\"(http.?://.+?/impression.*?)\"").matcher(clickAndImpressionURL);
        if (regexMatcher.find()) {
            result.put("clickURL",regexMatcher.group(1));
            log.debug("click URL -"+ regexMatcher.group(1));
            result.put("impressionURL",regexMatcher.group(2));
            log.debug("impression URL -"+ regexMatcher.group(2));
        }

        return result;
    }







// RESPONSE FORMAT


//    {
//        "id": "6ed4801d-8724-4fc0-8de0-aaf1571e2f95",
//            "seatbid": [
//        {
//            "bid": [
//            {
//                "id": "1077d4c9-583d-4368-8a47-0b4bcd9dd668",
//                    "impid": "bdb394f3-cd35-4d9b-9b68-58b579408ffc",
//                    "price": 1.5,
//                    "adid": "10211-1",
//                    "nurl": "http://dsp-r-dev.phunware.com/winNotify?tx=1077d4c9-583d-4368-8a47-0b4bcd9dd668&id=6ed4801d-8724-4fc0-8de0-aaf1571e2f95&iid=bdb394f3-cd35-4d9b-9b68-58b579408ffc&sid=&pid=10306&crid=10211&dim=1&co=USA&d=68400000-8cf0-11bd-b23e-10b96e40000c&dm=iPhone&ca=Verizon+Wireless+USA&pl=iOS&b=249&do=&lo=30.3583022%2C-97.7304637&ip=192.200.182.12&tsrc=Tapit&pr=${AUCTION_PRICE}",
//                    "adm": "<a href=\"http://dsp-r-dev.phunware.com/click?tx=1077d4c9-583d-4368-8a47-0b4bcd9dd668&id=6ed4801d-8724-4fc0-8de0-aaf1571e2f95&iid=bdb394f3-cd35-4d9b-9b68-58b579408ffc&sid=&pid=10306&crid=10211&dim=1&co=USA&d=68400000-8cf0-11bd-b23e-10b96e40000c&dm=iPhone&ca=Verizon+Wireless+USA&pl=iOS&b=249&do=&lo=30.3583022%2C-97.7304637&ip=192.200.182.12&tsrc=Tapit&pr=${AUCTION_PRICE}\"><img src=\"https://lbs-dev.s3.amazonaws.com/10001/flower.jpeg\" /></a><img height=\"1\" width=\"1\" src=\"http://dsp-r-dev.phunware.com/impression?tx=1077d4c9-583d-4368-8a47-0b4bcd9dd668&id=6ed4801d-8724-4fc0-8de0-aaf1571e2f95&iid=bdb394f3-cd35-4d9b-9b68-58b579408ffc&sid=&pid=10306&crid=10211&dim=1&co=USA&d=68400000-8cf0-11bd-b23e-10b96e40000c&dm=iPhone&ca=Verizon+Wireless+USA&pl=iOS&b=249&do=&lo=30.3583022%2C-97.7304637&ip=192.200.182.12&tsrc=Tapit&pr=${AUCTION_PRICE}\" /><img src=\"www.phunware3.com/aaa\" width=\"1\" height=\"1\" border=\"0\" alt=\"\" /><img src=\"http://www.phunware6.com\" width=\"1\" height=\"1\" border=\"0\" alt=\"\" /><img src=\"http://www.phunware2.com\" width=\"1\" height=\"1\" border=\"0\" alt=\"\" /><img src=\"http://www.phunware5.com\" width=\"1\" height=\"1\" border=\"0\" alt=\"\" /><img src=\"http://www.phunware4.com\" width=\"1\" height=\"1\" border=\"0\" alt=\"\" />",
//                    "adomain": [
//                "http://phunware.com"
//                    ],
//                "iurl": "http://www.phunware.com",
//                    "cid": "10306",
//                    "crid": "10211",
//                    "cat": [
//                "IAB1",
//                        "IAB1-1",
//                        "IAB1-2",
//                        "IAB1-3",
//                        "IAB1-4",
//                        "IAB1-5"
//                    ],
//                "attr": [
//                1,
//                        2,
//                        3
//                    ],
//                "dealid": "AutomationDealID",
//                    "w": 320,
//                    "h": 50
//            }
//            ]
//        }
//    ],
//        "bidid": "1077d4c9-583d-4368-8a47-0b4bcd9dd668"
//    }






}
