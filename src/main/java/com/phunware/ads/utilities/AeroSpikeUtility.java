package com.phunware.ads.utilities;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.stream.Collectors;


/** Created by pavankovurru on 7/19/17. */
public class AeroSpikeUtility {

  //jumpserver ssh details
  private static String jumpServerSshUser;
  private static String jumpServerSshPassword;
  private static String jumpServerSshHostName;
  private static int jumpServerSshPort;
  private static Session jumpServerSession;

  // Aerospike server ssh details
  private static String aerospikeserverUser;
  private static String aerospikeserverPassword;
  private static String aerospikeserverHostName;
  private static int aerospikeserverSshPort;
  private static Session aerospikeserverSession;

  //Initiating Logger Object
  private static final Logger log = LogManager.getLogger();



  public static void initialize(String env) {

    //jump server
    if (env.matches(".*?dev.*?")) {

      //jump server
      jumpServerSshUser = "pkovurru";
      jumpServerSshPassword = "pa55word";
      jumpServerSshHostName = "att-stagegw01.phunware.com";
      jumpServerSshPort = 22;

      //Actual Server
      aerospikeserverUser = "ec2-user";
      aerospikeserverPassword = "phunware10";
      aerospikeserverHostName = "ec2-54-237-10-197.compute-1.amazonaws.com";
      aerospikeserverSshPort = 22;
    }


    //TODO UPDATE THESE DETAILS AFTER THE STAGE ENV IS READY.
    if (env.matches(".*?stage.*?")) {
      //jump server
      jumpServerSshUser = "pkovurru";
      jumpServerSshPassword = "pa55word";
      jumpServerSshHostName = "att-stagegw01.phunware.com";
      jumpServerSshPort = 22;

      //Actual Server
      aerospikeserverUser = "developer";
      aerospikeserverPassword = "phunware10";
      aerospikeserverHostName = "att-stageadabmapi01";
      aerospikeserverSshPort = 22;
    }

  }

  public static String LogInToAerospikeExecuteAqlQueryAndReturnResponse(String env ,String nameSpace, String setName, String aerospikeRecordkey) {

    initialize(env);
    String aerospikeResult="";


    try {
      java.util.Properties config = new java.util.Properties();
      config.put("StrictHostKeyChecking", "no");

      //Connecting to jump server
      JSch jsch = new JSch();
      jumpServerSession = jsch.getSession(jumpServerSshUser, jumpServerSshHostName, jumpServerSshPort);
      jumpServerSession.setPassword(jumpServerSshPassword);
      jumpServerSession.setConfig(config);
      jumpServerSession.connect(15000);
      log.info("SSH Connected - connected to jump server - "+ jumpServerSshHostName);

      //Change the port number if the port is already used.
      int localPort = 17242;

      //Port Forwarding
      int assingedPort=jumpServerSession.setPortForwardingL(localPort, aerospikeserverHostName, aerospikeserverSshPort);


      //capturing contents of pem file from remote server
      Channel channelSftp = jumpServerSession.openChannel("sftp");
      channelSftp.connect();
      ChannelSftp sftpChannel = (ChannelSftp) channelSftp;
      InputStream pem = sftpChannel.get("newawskey.pem");   //capturing data from a pem file located in jumpserver
      String result = new BufferedReader(new InputStreamReader(pem))
              .lines().collect(Collectors.joining("\n"));

      //writing data to pem file
      //Note: data will be flushed out as soon as this method done executing.
      writeToFile(result);

      //using a .pem file to get access to ssh
      String pemFileLocation = System.getProperty("user.dir") + "/src/main/resources/pemfile.pem";
      jsch.addIdentity(pemFileLocation,aerospikeserverPassword);

      //Connecting to actual server -- syntax --> ssh -i filename.pem user@server
      aerospikeserverSession = jsch.getSession(aerospikeserverUser, "localhost", assingedPort);
      aerospikeserverSession.setConfig(config);
      aerospikeserverSession.connect(15000);
      log.info("SSH Connected - connected to server - "+aerospikeserverHostName);


      //Change the port number if the port is already used.
      int localPortAerospike = 17243;

      //Port Forwarding for connecting to aerospike
      int assingedPortAerospike=aerospikeserverSession.setPortForwardingL(localPortAerospike, "localhost", 3000);

      //Connecting to Areospike
      AerospikeClient client = new AerospikeClient("localhost", assingedPortAerospike);
      Key key = new Key(nameSpace,setName,aerospikeRecordkey);


      //Read Areospike record using unique Aerospike key
      Record record = client.get(null,key);
      aerospikeResult=record.toString();
      client.close();

      log.info("SSH Sessions Getting Disconnected - No Exceptions Observed");
    }


    catch (Exception ee) {
      log.info("SSH Sessions Getting Disconnected - Exception observed");
      ee.printStackTrace();
    }


    finally{
      aerospikeserverSession.disconnect();
      jumpServerSession.disconnect();
      //flushing contents from pem file
      writeToFile("");
    }

    return aerospikeResult;

  }

  public static void writeToFile(String data){

    try {
      File myPemFile = new File(System.getProperty("user.dir") + "/src/main/resources/pemfile.pem");
      FileOutputStream pemFileStream = new FileOutputStream(myPemFile, false); // true to append
      pemFileStream.write(data.getBytes());
      pemFileStream.close();
    }
    catch (IOException e){
      e.printStackTrace();
    }
  }


}