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
            serverUser = "developer";
            serverPassword = "phunware10";
            serverHostName = "att-devadsrv01";
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
            serverHostName = "att-stageadsrv01";
            serverSshPort = 22;
        }

    }

    public static String logInToServerExecuteShellCommandAndReturnResponse(String serviceEndPoint, String shellCommand) {

        initialize(serviceEndPoint);
        String output = "";
        log.debug("Executing Command -"+ shellCommand);


        try {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");

            //Connecting to jump server
            JSch jsch = new JSch();
            jumpServerSession = jsch.getSession(jumpServerSshUser, jumpServerSshHostName, jumpServerSshPort);
            jumpServerSession.setPassword(jumpServerSshPassword);
            jumpServerSession.setConfig(config);
            jumpServerSession.connect();
            log.debug("SSH Connected - connected to jump server - " + jumpServerSshHostName);

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

            //writing data to pem file
            //Note: data will be flushed out as soon as this method done executing.
            writeToFile(result);

            //using a .pem file to get access to ssh
            String pemFileLocation = System.getProperty("user.dir") + "/src/main/resources/pemfile.pem";

            //using a .pem file to get access to ssh
            jsch.addIdentity(pemFileLocation, serverPassword);

            //Connecting to actual server -- syntax --> ssh -i filename.pem user@server
            serverSession = jsch.getSession(serverUser, "localhost", assingedPort);
            serverSession.setConfig(config);
            serverSession.connect(30000);
            log.debug("SSH Connected - connected to server - " + serverHostName);

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
                    log.debug("exit-status: " + channel.getExitStatus());
                    break;
                }
            }

            log.debug("SSH Sessions Getting Disconnected - No Exceptions Observed");
        } catch (Exception e) {
            log.debug("SSH Sessions Getting Disconnected - Exception observed");
            e.printStackTrace();
        } finally {
            serverSession.disconnect();
            jumpServerSession.disconnect();
            writeToFile("");
        }

        return output;
    }

    public static void updateLog4jLoggingLevel(String serviceEndPoint, String loggerLevel) {

        // TODO - Update log4j2Path to have a dynamic path if it is not the same for all the environments.
        String log4j2Path = "/opt/phunware/dsp/current/log4j2.xml";

        //LogInToServerExecuteShellCommandAndReturnResponse(serviceEndPoint, "sed -i 's/\\(Logger.*name=\\\"co.*=\\\"\\).*\\(\\\"\\)/\\1" + loggerLevel + "\\2/' " + log4j2Path);
        logInToServerExecuteShellCommandAndReturnResponse(serviceEndPoint,"sudo -u root /bin/cp /home/developer/Automation/log4j2.xml /opt/phunware/dsp/current/log4j2.xml");
        log.info("Updated Logger level to - " + loggerLevel);

    }

    public static void writeToFile(String data) {

        try {
            File myPemFile = new File(System.getProperty("user.dir") + "/src/main/resources/pemfile.pem");
            FileOutputStream pemFileStream = new FileOutputStream(myPemFile, false);
            pemFileStream.write(data.getBytes());
            pemFileStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





}