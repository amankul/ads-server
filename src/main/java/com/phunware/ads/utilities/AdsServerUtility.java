package com.phunware.ads.utilities;

import com.jcraft.jsch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.stream.Collectors;

/**
 * Created by pavankovurru on 7/19/17.
 */
public class AdsServerUtility {

    //jumpserver ssh details
    private static String jumpServerSshUser;
    private static String jumpServerSshPassword;
    private static String jumpServerSshHostName;
    private static int jumpServerSshPort;
    private static Session jumpServerSession;

    //server ssh details
    private static String serverUser;
    private static String serverPassword;
    private static String serverHostName;
    private static int serverSshPort;
    private static Session serverSession;

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
            serverUser = "root";
            serverPassword = "phunware10";
            serverHostName = "att-devadscratch01";
            serverSshPort = 22;
        }

        if (env.matches(".*?stage.*?")) {
            //jump server
            jumpServerSshUser = "pkovurru";
            jumpServerSshPassword = "pa55word";
            jumpServerSshHostName = "att-stagegw01.phunware.com";
            jumpServerSshPort = 22;

            //Actual Server
            serverUser = "developer";
            serverPassword = "phunware10";
            serverHostName = "att-stageadabmapi01";
            serverSshPort = 22;
        }

    }

    public static String LogInToServerExecuteShellCommandAndReturnResponse(String env, String shellCommand) {

        initialize(env);
        String output = "";


        try {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");

            //Connecting to jump server
            JSch jsch = new JSch();
            jumpServerSession = jsch.getSession(jumpServerSshUser, jumpServerSshHostName, jumpServerSshPort);
            jumpServerSession.setPassword(jumpServerSshPassword);
            jumpServerSession.setConfig(config);
            jumpServerSession.connect();
            log.info("SSH Connected - connected to jump server - " + jumpServerSshHostName);

            //Change the port number if the port is already used.
            int localPort = 15006;

            //Port Forwarding
            int assingedPort = jumpServerSession.setPortForwardingL(localPort, serverHostName, serverSshPort);


            Channel channel1 = jumpServerSession.openChannel("sftp");
            channel1.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel1;
            InputStream pem = sftpChannel.get("phunware-developer.pem");

            String result = new BufferedReader(new InputStreamReader(pem))
                    .lines().collect(Collectors.joining("\n"));


            //using a .pem file to get access to ssh
            String pemFileLocation = System.getProperty("user.dir") + "/src/main/resources/phunware-developer.pem";
            jsch.addIdentity(pemFileLocation, serverPassword);

            //Connecting to actual server -- syntax --> ssh -i filename.pem user@server
            serverSession = jsch.getSession(serverUser, "localhost", assingedPort);
            serverSession.setConfig(config);
            serverSession.connect(30000);
            log.info("SSH Connected - connected to server - " + serverHostName);

            //Executing and printing shell commands
            Channel channel = serverSession.openChannel("exec");
            ((ChannelExec) channel).setCommand(shellCommand);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);

            InputStream in = channel.getInputStream();
            channel.connect();
            byte[] temp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(temp, 0, 1024);
                    if (i < 0) break;
                    //capturing shell output as a multi line string
                    output = output + (new String(temp, 0, i));
                }
                if (channel.isClosed()) {
                    System.out.println("exit-status: " + channel.getExitStatus());
                    break;
                }
            }

            log.info("SSH Sessions Getting Disconnected - No Exceptions Observed");
        }

        catch (Exception e) {
            log.info("SSH Sessions Getting Disconnected - Exception observed");
            e.printStackTrace();
        }

        finally {
            serverSession.disconnect();
            jumpServerSession.disconnect();
        }

        return output;
    }

    public static void updateLog4jLoggingLevel(String env, String loggerLevel , String rootLoggerLevel , String log4j2Path) {

        log.info("Trying to update log4j2.xml presnt at -"+log4j2Path);

        LogInToServerExecuteShellCommandAndReturnResponse(env, "sed -i 's/\\(Logger.*name=\\\"co.*=\\\"\\).*\\(\\\"\\)/\\"+loggerLevel+"\\2/' "+log4j2Path);
        log.info("Updated Logger level to - "+loggerLevel);

        LogInToServerExecuteShellCommandAndReturnResponse(env, "sed -i 's/\\(Root.*=\\\"\\).*\\(\\\"\\)/\\1"+rootLoggerLevel+"\\2/' "+log4j2Path);
        log.info("Updated Root Logger level to - "+rootLoggerLevel);
    }
    
    public static void writeToFIle(String data){
        
        try {
            File myPemFile = new File(System.getProperty("user.dir") + "/src/main/resources/pemfile.pem");
            FileOutputStream pemFileStream = new FileOutputStream(myPemFile, true); // true to append
            pemFileStream.write(data.getBytes());
            pemFileStream.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


}