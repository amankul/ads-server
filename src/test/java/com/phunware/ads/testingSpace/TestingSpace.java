package com.phunware.ads.testingSpace;

import com.jcraft.jsch.JSchException;
import com.phunware.ads.utilities.AeroSpikeUtility;

/**
 * Created by pavankovurru on 6/5/17.
 */
public class TestingSpace {

    public static void main (String [] args) throws JSchException {


        String result = AeroSpikeUtility.LogInToAerospikeExecuteAqlQueryAndReturnResponse("dev","adserver","counters","sd:plac10272");


        System.out.println("");
        System.out.println("AeroSpike Result -");
        System.out.println(result);

        System.out.println("");
        System.out.println("                    ¯\\_(ツ)_/¯");
        System.out.println("                               ¯\\_(ツ)_/¯");



    }
    }

