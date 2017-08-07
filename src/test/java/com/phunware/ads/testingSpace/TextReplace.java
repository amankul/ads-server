package com.phunware.ads.testingSpace;

import com.phunware.ads.utilities.AdsServerUtility;

public class TextReplace {


    public static void main (String [] args)  {

        System.out.println("log 4j before update");
        System.out.println("-------------------------------------------------------------------------------------------");
        System.out.println(AdsServerUtility.LogInToServerExecuteShellCommandAndReturnResponse("dev","cat test/test.txt"));
        System.out.println("-------------------------------------------------------------------------------------------");

//        AdsServerUtility.updateLog4jLoggingLevel("dev", "sed -i 's/\\(Logger.*name=\\\"co.*=\\\"\\).*\\(\\\"\\)/\\1debug\\2/' test/test.txt");
//        AdsServerUtility.updateLog4jLoggingLevel("dev", "sed -i 's/\\(Root.*=\\\"\\).*\\(\\\"\\)/\\1info\\2/' test/test.txt");

        System.out.println("log 4j after update");
        System.out.println("-------------------------------------------------------------------------------------------");
        System.out.println(AdsServerUtility.LogInToServerExecuteShellCommandAndReturnResponse("dev","cat test/test.txt"));
        System.out.println("-------------------------------------------------------------------------------------------");

    }

}


//sed -i 's/\(Root.*=\"\).*\(\"\)/\1info\2/' test/test.txt
//sed -i 's/\(Logger.*name=\"co.*=\"\).*\(\"\)/\1debug\2/' test/test.txt


//  /opt/phunware/dsp/current/log4j2.xml  -- log4j2 xml location that needs to be changed.