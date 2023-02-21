/*
 * SelectiveStreamApp.java
 * 
 * Copyright (c) 2002-2018 Avaya Inc. All rights reserved.
 * 
 * USE OR INSTALLATION OF THIS SAMPLE DEMONSTRATION SOFTWARE INDICATES THE END
 * USERS ACCEPTANCE OF THE GENERAL LICENSE TERMS AVAILABLE ON THE AVAYA WEBSITE
 * AT http://support.avaya.com/LicenseInfo/ (GENERAL LICENSE TERMS). DO NOT USE
 * THE SOFTWARE IF YOU DO NOT WISH TO BE BOUND BY THE GENERAL LICENSE TERMS. IN
 * ADDITION TO THE GENERAL LICENSE TERMS, THE FOLLOWING ADDITIONAL TERMS AND
 * RESTRICTIONS WILL TAKE PRECEDENCE AND APPLY TO THIS DEMONSTRATION SOFTWARE.
 * 
 * THIS DEMONSTRATION SOFTWARE IS PROVIDED FOR THE SOLE PURPOSE OF DEMONSTRATING
 * HOW TO USE THE SOFTWARE DEVELOPMENT KIT AND MAY NOT BE USED IN A LIVE OR
 * PRODUCTION ENVIRONMENT. THIS DEMONSTRATION SOFTWARE IS PROVIDED ON AN AS IS
 * BASIS, WITHOUT ANY WARRANTIES OR REPRESENTATIONS EXPRESS, IMPLIED, OR
 * STATUTORY, INCLUDING WITHOUT LIMITATION, WARRANTIES OF QUALITY, PERFORMANCE,
 * INFRINGEMENT, MERCHANTABILITY, OR FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * EXCEPT FOR PERSONAL INJURY CLAIMS, WILLFUL MISCONDUCT AND END USERS VIOLATION
 * OF AVAYA OR ITS SUPPLIERS INTELLECTUAL PROPERTY RIGHTS, INCLUDING THROUGH A
 * BREACH OF THE SOFTWARE LICENSE, NEITHER AVAYA, ITS SUPPLIERS NOR END USER
 * SHALL BE LIABLE FOR (i) ANY INCIDENTAL, SPECIAL, STATUTORY, INDIRECT OR
 * CONSEQUENTIAL DAMAGES, OR FOR ANY LOSS OF PROFITS, REVENUE, OR DATA, TOLL
 * FRAUD, OR COST OF COVER AND (ii) DIRECT DAMAGES ARISING UNDER THIS AGREEMENT
 * IN EXCESS OF FIFTY DOLLARS (U.S. $50.00).
 * 
 * To the extent there is a conflict between the General License Terms, your
 * Customer Sales Agreement and the terms and restrictions set forth herein, the
 * terms and restrictions set forth herein shall prevail solely for this Utility
 * Demonstration Software.
 */
package sampleapps.selectivestream;

import ch.ecma.csta.binding.ConnectionID;
import ch.ecma.csta.binding.DeviceID;
import ch.ecma.csta.binding.EstablishedEvent;
import ch.ecma.csta.binding.LocalDeviceID;
import ch.ecma.csta.binding.RecordMessage;
import ch.ecma.csta.binding.RecordMessageResponse;
import ch.ecma.csta.binding.Stop;
import ch.ecma.csta.callcontrol.CallControlAdapter;
import ch.ecma.csta.errors.CstaException;
import ch.ecma.csta.monitor.MonitoringServices;
import ch.ecma.csta.voiceunit.VoiceUnitServices;
import com.avaya.api.media.audio.Audio;
import com.avaya.cmapi.APIProtocolVersion;
import com.avaya.cmapi.ServiceProvider;
import com.avaya.cmapi.ServiceProviderListener;
import com.avaya.cmapi.evt.ServerConnectionDownEvent;
import com.avaya.cmapi.evt.ServerSessionNotActiveEvent;
import com.avaya.cmapi.evt.ServerSessionTerminatedEvent;
import com.avaya.csta.binding.EndpointRegisteredEvent;
import com.avaya.csta.binding.GetDeviceId;
import com.avaya.csta.binding.LoginInfo;
import com.avaya.csta.binding.MediaInfo;
import com.avaya.csta.binding.RegisterTerminalRequest;
import com.avaya.csta.binding.SelectivePartyList;
import com.avaya.csta.binding.SelectiveStreamRequest;
import com.avaya.csta.binding.TerminalUnregisteredEvent;
import com.avaya.csta.binding.types.DependencyMode;
import com.avaya.csta.binding.types.DeviceInstance;
import com.avaya.csta.binding.types.MediaContent;
import com.avaya.csta.binding.types.MediaMode;
import com.avaya.csta.binding.types.StreamContent;
import com.avaya.csta.device.DeviceServices;
import com.avaya.csta.registration.EndpointRegistrationStateAdapter;
import com.avaya.csta.registration.RegistrationAdapter;
import com.avaya.csta.registration.RegistrationServices;
import com.avaya.mvcs.framework.CmapiKeys;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import sampleapps.station.AvayaStation;

/**
 * Purpose: This tutorial application demonstrates how to use selective stream
 * functionality using the Device, Media and Call Control API (DMCC). 
 * It registers a station in dependent or independent mode and monitors 
 * EstablishedEvent for a station. When any party added in the call, 
 * an EstablishedEvent is received by an application and option is given to 
 * send Selective Stream Request to Communication Manager for requesting 
 * split stream based on parties in the call. Also this application has ability 
 * to configure station to receive selective stream at time of registration.
 *
 * @author AVAYA, Inc.
 */
@SuppressWarnings("unused")
public class SelectiveStreamApp
{
    private static boolean isRecording = false;
    private static ServiceProvider serviceProvider = null;
    private static RegistrationServices regSvcs = null;
    private static DeviceServices devSvcs = null;
    private static MonitoringServices monSvcs = null;
    private static VoiceUnitServices voiceSvcs = null;
    private static ServiceProviderListener sessionListener = null;
    private static MyRegistrationListener regListener = null;
    private static MyEptRegistrationListener eptRegListener = null;
    private static DeviceID deviceA = null;

    private static final String PROP_FILE_CMAPI_SERVER = "cmapi1.server_ip";
    private static final String PROP_FILE_USERNAME = "cmapi1.username";
    private static final String PROP_FILE_PASSWORD = "cmapi1.password";
    private static final String PROP_FILE_SERVER_PORT = "cmapi1.server_port";
    private static final String PROP_FILE_SECURE = "cmapi1.secure";

    // These fields are read in from the selectivestream.properties file. This file
    // must be populated with a valid IP address for Avaya Communication
    // Manager, as well as a valid station and password for the station.
    private String callServer;
    private String station;
    private String password;
    private String dependencyMode;
    private String codec;
    private String encryption;
    private String sessionMode;
    private String ssrMode;
    private boolean isMultiple = false;

    MyCallControlListener listener;

    public final List<DeviceID> DEVICE_LIST = new ArrayList<DeviceID>();

    AvayaStation avayaStation;

    // This thread is used to receive a shutdown notification when the
    // application is terminated.
    private MyShutdownThread shutdownThread;
    
    private Scanner sc = null;

    /**
     * This is the bootstrap to read in the needed properties, and get handles
     * to all services that are used by the application.
     *
     * All applications will have to have some administered properties that
     * include at a minimum the Communication Manager address, the party number
     * and the password. This application uses a properties file to administer
     * these values. Other applications could choose to use a database or have
     * the user administer the values when the application starts up. This is a
     * choice for the application developer.
     *
     * @throws CstaException - if a CstaException is generated by one of the
     * calls to service provider, it is thrown to the caller.
     * @throws Exception - if any other runtime exception is generated, it is
     * thrown to the caller.
     */
    public void bootstrap() throws CstaException, Exception
    {
        String cmapiServerIp;
        String cmapiUsername;
        String cmapiPassword;
        String cmapiServerPort;
        String cmapiSecure;
        String cmapiTrustStoreLocation;

        // add following properties for validation client and service side certifications 
        String cmapiTrustStorePassword;
        String cmapiKeyStoreLocation;
        String cmapiKeyStorePassword;
        String isValidPeer;
        String hostnameValidation;

        ClassLoader cl = SelectiveStreamApp.class.getClassLoader();
        URL appURL = cl.getResource("selectivestream.properties");
        Properties appProp = new Properties();
        appProp.load(appURL.openStream());
        callServer = appProp.getProperty("callserver").trim();
        station = appProp.getProperty("station").trim();
        password = appProp.getProperty("password").trim();
        dependencyMode = appProp.getProperty("dependencyMode", "DEPENDENT").trim();
        codec = appProp.getProperty("codec").trim();
        encryption = appProp.getProperty("encryption").trim();
        sessionMode = appProp.getProperty("sessionMode");
        if (sessionMode != null && sessionMode.trim().equalsIgnoreCase("MULTIPLE")) {
            isMultiple = true;
        }
        ssrMode = appProp.getProperty("ssrMode");

        cmapiServerIp = appProp.getProperty(PROP_FILE_CMAPI_SERVER).trim();
        cmapiUsername = appProp.getProperty(PROP_FILE_USERNAME).trim();
        cmapiPassword = appProp.getProperty(PROP_FILE_PASSWORD).trim();
        // If there is no entry then we will assume the connection is secure.
        cmapiServerPort = appProp.getProperty(PROP_FILE_SERVER_PORT, "4722").trim();
        cmapiSecure = appProp.getProperty(PROP_FILE_SECURE, "true").trim();
        cmapiTrustStoreLocation = appProp.getProperty(CmapiKeys.TRUST_STORE_LOCATION);

        // add following properties for validation client and service side certifications 
        cmapiTrustStorePassword = appProp.getProperty(CmapiKeys.TRUST_STORE_PASSWORD);
        cmapiKeyStoreLocation = appProp.getProperty(CmapiKeys.KEY_STORE_LOCATION);
        String cmapiKeyStoreType = appProp.getProperty(CmapiKeys.KEY_STORE_TYPE);
        cmapiKeyStorePassword = appProp.getProperty(CmapiKeys.KEY_STORE_PASSWORD);
        isValidPeer = appProp.getProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE);
        hostnameValidation = appProp.getProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE_HOSTNAME);

        Properties spProp = new Properties();
        spProp.setProperty(CmapiKeys.SERVICE_PROVIDER_ADDRESS, cmapiServerIp);
        spProp.setProperty(CmapiKeys.CMAPI_USERNAME, cmapiUsername);
        spProp.setProperty(CmapiKeys.CMAPI_PASSWORD, cmapiPassword);
        spProp.setProperty(CmapiKeys.SERVICE_PROVIDER_PORT, cmapiServerPort);
        spProp.setProperty(CmapiKeys.SECURE_SERVICE_PROVIDER, cmapiSecure);
        spProp.setProperty(CmapiKeys.SESSION_DURATION_TIMER, "240");
        spProp.setProperty(CmapiKeys.SESSION_CLEANUP_TIMER, "120");
        spProp.put(CmapiKeys.SESSION_PROTOCOL_VERSION_LIST, APIProtocolVersion.VERSION_LIST);

        if (cmapiTrustStoreLocation != null) {
            spProp.setProperty(CmapiKeys.TRUST_STORE_LOCATION, cmapiTrustStoreLocation.trim());
        }
        // add following properties for validation client and service side certifications 
        if (cmapiTrustStorePassword != null) {
            spProp.setProperty(CmapiKeys.TRUST_STORE_PASSWORD, cmapiTrustStorePassword.trim());
        }
        if (cmapiKeyStoreLocation != null) {
            spProp.setProperty(CmapiKeys.KEY_STORE_LOCATION, cmapiKeyStoreLocation.trim());
        }
        if (cmapiKeyStoreType != null) {
            spProp.setProperty(CmapiKeys.KEY_STORE_TYPE, cmapiKeyStoreType.trim());
        }
        if (cmapiKeyStorePassword != null) {
            spProp.setProperty(CmapiKeys.KEY_STORE_PASSWORD, cmapiKeyStorePassword.trim());
        }
        if (isValidPeer != null) {
            spProp.setProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE, isValidPeer.trim());
        }
        if(hostnameValidation != null){
            spProp.setProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE_HOSTNAME, hostnameValidation.trim());
        }
        spProp.setProperty(CmapiKeys.AES_FIPS_MODE, "false");
        
        sc = new Scanner(System.in);

        System.out.println("=====================");
        System.out.println("Selective Stream App");
        System.out.println("=====================");
        serviceProvider = ServiceProvider.getServiceProvider(spProp);

        sessionListener = new MyServiceProviderListener();
        serviceProvider.addServiceProviderListener(sessionListener);

        System.out.println("Created session = " + serviceProvider.getSessionID());
        regSvcs = (RegistrationServices) serviceProvider.getService(RegistrationServices.class.getName());
        devSvcs = (DeviceServices) serviceProvider.getService(DeviceServices.class.getName());
        monSvcs = (MonitoringServices) serviceProvider.getService(MonitoringServices.class.getName());
        voiceSvcs = (VoiceUnitServices) serviceProvider.getService(VoiceUnitServices.class.getName());

        avayaStation = new AvayaStation();
        avayaStation.init(callServer, "", station, serviceProvider, isMultiple);

        regListener = new MyRegistrationListener(avayaStation);
        eptRegListener = new MyEptRegistrationListener();
    }

    private DependencyMode getDependencyMode()
    {
        if (dependencyMode.equalsIgnoreCase(DependencyMode.MAIN.toString())) {
            return DependencyMode.MAIN;
        } else if (dependencyMode.equalsIgnoreCase(DependencyMode.INDEPENDENT.toString())) {
            return DependencyMode.INDEPENDENT;
        } else {
            return DependencyMode.DEPENDENT;
        }
    }

    public void start() throws Exception
    {
        // Create a thread in whose context our cleanup will occur if the app is
        // terminated. The Communication Manager API connector server code will
        // clean up if an app goes away unexpectedly, but it's still good to
        // clean up.
        shutdownThread = new MyShutdownThread();
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        try {
            // get the device for specified station
            deviceA = getDevice();

            monSvcs.addRegistrationListener(deviceA, regListener);
            monSvcs.addEndpointRegistrationStateListener(deviceA, eptRegListener);

            // setup monitors in case of mid-call
            listener = new MyCallControlListener();
            monSvcs.addCallControlListener(deviceA, listener);
            System.out.println("Registered EstablishedEvent Listener for device = " + deviceA.getContent());

            // register a station
            registerDevice(regSvcs, deviceA, password, MediaMode.SERVER, getDependencyMode());

        } catch (CstaException e) {
            System.out.println("Unexpected error occured. Terminate session manually where this device "
                + "belongs from OAM(Status->Staus and control->DMCC Service Summary) and try again.");
            e.printStackTrace(System.out);
        }
    }

    /**
     * Creates an instance of the SessionManagementApp class, bootstraps it,
     * then starts the application.
     * @param args
     */
    public static void main(String[] args)
    {
        SelectiveStreamApp app = new SelectiveStreamApp();
        try {
            app.bootstrap();
            app.start();
        } catch (CstaException e) {
            System.out.println("Could not start the selective stream app");
            e.printStackTrace(System.out);
        } catch (Exception e) {
            System.out.println("Could not start the selective stream app");
            e.printStackTrace(System.out);
        }
    }

    private DeviceID getDevice() throws CstaException
    {
        GetDeviceId gdi = new GetDeviceId();
        gdi.setControllableByOtherSessions(Boolean.TRUE);
        gdi.setExtension(station);
        gdi.setSwitchName(callServer);
        gdi.setDeviceInstance(DeviceInstance.VALUE_0);
        return devSvcs.getDeviceID(gdi).getDevice();
    }

    private void registerDevice(RegistrationServices regSvcs, DeviceID device, String password, MediaMode mediaMode, DependencyMode dependencyMode) throws CstaException
    {
        MediaInfo localMediaInfo = new MediaInfo();

        // optional codec settings
        if (Audio.G711U.equalsIgnoreCase(codec)) {
            localMediaInfo.setCodecs(new String[]{Audio.G711U});
        } else if (Audio.G711A.equalsIgnoreCase(codec)) {
            localMediaInfo.setCodecs(new String[]{Audio.G711A});
        } else if (Audio.G729.equalsIgnoreCase(codec)) {
            localMediaInfo.setCodecs(new String[]{Audio.G729});
        } else if (Audio.G729A.equalsIgnoreCase(codec)) {
            localMediaInfo.setCodecs(new String[]{Audio.G729A});
        } else // default
        {
            localMediaInfo.setCodecs(new String[]{Audio.G711U, Audio.G711A});
        }

        // optional encryption settings
        if (Audio.AES.equalsIgnoreCase(encryption)) {
            localMediaInfo.setEncryptionList(new String[]{Audio.AES});
        } else if (Audio.NOENCRYPTION.equalsIgnoreCase(encryption)) {
            localMediaInfo.setEncryptionList(new String[]{Audio.NOENCRYPTION});
        } else // default
        {
            localMediaInfo.setEncryptionList(new String[]{Audio.AES, Audio.NOENCRYPTION});
        }

        System.out.println("setupAudio"
            + ": codec=" + localMediaInfo.getCodecs()[0]
            + ", encryption=" + localMediaInfo.getEncryptionList()[0]);

        boolean isPrecall = true;
        if (ssrMode != null && ssrMode.equalsIgnoreCase("mid-call")) {
            isPrecall = false;
        }

        int mediaContent = 0;
        boolean mediaTonesAnnc = false;
        System.out.println("ssrMode = " + ssrMode);
        if (isPrecall) {
            System.out.println("Enter value for mediaContent\n[0=FULL, \n1=MAIN_STATION_ONLY, \n2=ALL_BUT_MAIN_STATION]: ");
            mediaContent = sc.nextInt();
            sc.nextLine();
            System.out.println("Enter value of mediaTonesAnnc[true/false]: ");
            mediaTonesAnnc = (sc.hasNextBoolean()) ? sc.nextBoolean() : true;
            sc.nextLine();
        } else {
            System.out.println("Registered a device with default MediaContent=FULL and MediaTonesAnnc=false.");
        }

        RegisterTerminalRequest rtr = new RegisterTerminalRequest();
        rtr.setDevice(device);
        LoginInfo loginInfo = new LoginInfo();
        loginInfo.setMediaMode(mediaMode);
        loginInfo.setDependencyMode(dependencyMode);
        loginInfo.setPassword(password);
        if (isPrecall) {
            loginInfo.setMediaContent(mediaContent == 0
                ? MediaContent.FULL : mediaContent == 1
                    ? MediaContent.MAIN_STATION_ONLY
                    : MediaContent.ALL_BUT_MAIN_STATION);
            loginInfo.setMediaTonesAnnc(mediaTonesAnnc);
        }
        rtr.setLocalMediaInfo(localMediaInfo);
        rtr.setLoginInfo(loginInfo);
        regSvcs.registerTerminal(rtr);
    }

    private void sendSelectiveStreamRequest()
    {
        System.out.println("Do you want to send Selective Stream Request?(Y/N)");
        char ip = sc.nextLine().charAt(0);
        if (ip == 'y' || ip == 'Y') {
            System.out.println("Enter value for streamContent\n[0=FULL, \n1=MAIN_STATION_ONLY, \n2=ALL_BUT_MAIN_STATION, \n3=SELECTIVE_INCLUDE, \n4=SEELCTIVE_EXCLUDE]: ");
            int streamContent = sc.nextInt();
            sc.nextLine();
            String selectivePartyList = "";
            if (streamContent == 3 || streamContent == 4) {
                for (int i = 0; i < DEVICE_LIST.size(); i++) {
                    System.out.println(i + " : " + DEVICE_LIST.get(i).getContent());
                }
                System.out.println("Choose comma separate indexes parties in single line for selctivePartyList: ");
                selectivePartyList = sc.next();
            }
            System.out.println("Enter value of mediaTonesAnnc[true/false]: ");
            boolean mediaTonesAnnc = (sc.hasNextBoolean()) ? sc.nextBoolean() : true;
            sc.nextLine();
            SelectiveStreamRequest ssr = new SelectiveStreamRequest();
            ssr.setDevice(deviceA);
            ssr.setStreamContent(streamContent == 0 ? StreamContent.FULL
                : streamContent == 1 ? StreamContent.MAIN_STATION_ONLY
                    : streamContent == 2 ? StreamContent.ALL_BUT_MAIN_STATION
                        : streamContent == 3 ? StreamContent.SELECTIVE_INCLUDE
                            : StreamContent.SELECTIVE_EXCLUDE);
            if (!selectivePartyList.isEmpty()) {
                SelectivePartyList spl = new SelectivePartyList();
                String[] idx = selectivePartyList.split(",");
                DeviceID[] arr = new DeviceID[idx.length];
                int cnt = 0;
                for (String id : idx) {
                    arr[cnt++] = DEVICE_LIST.get(Integer.valueOf(id));
                }
                spl.setDevice(arr);
                ssr.setSelectivePartyList(spl);
            }
            ssr.setMediaTonesAnnc(mediaTonesAnnc);
            try {
                regSvcs.selectiveStream(ssr);
                System.out.println("Sent Selective Stream Request successfully...");
            } catch (CstaException e) {
                System.out.println("Error occured while sending Selective Stream Request.");
                e.printStackTrace(System.out);
            }
        } else {
            System.out.println("Add parties in the call with device to send "
                    + "selective stream request using SelectiveStreamRequest API.");
        }
    }

    private void startRecording()
    {
        ConnectionID connId = new ConnectionID();
        LocalDeviceID ldi = new LocalDeviceID();
        ldi.setStaticID(deviceA);
        connId.setDeviceID(ldi);
        RecordMessage rm = new RecordMessage();
        rm.setCallToBeRecorded(connId);
        try {
            RecordMessageResponse rmr = voiceSvcs.recordMessage(rm);
            System.out.println("Started recording at location = /var/media/" 
                    + rmr.getResultingMessage() + ".wav on AES Server.");
            isRecording = true;
        } catch (CstaException e) {
            System.out.println("Can not start recording.");
            e.printStackTrace(System.out);
        }
    }

    private void stopRecording()
    {
        ConnectionID connId = new ConnectionID();
        LocalDeviceID ldi = new LocalDeviceID();
        ldi.setStaticID(deviceA);
        connId.setDeviceID(ldi);
        Stop s = new Stop();
        s.setConnection(connId);
        try {
            voiceSvcs.stop(s);
            isRecording = false;
            System.out.println("Stopped recording.");
        } catch (CstaException e) {
            System.out.println("Can not stop recording.");
            e.printStackTrace(System.out);
        }
    }

    private class MyCallControlListener extends CallControlAdapter
    {
        @Override
        public void established(EstablishedEvent ee)
        {
            boolean isPrecall = true;
            if (ssrMode != null && ssrMode.equalsIgnoreCase("mid-call")) {
                isPrecall = false;
            }
            System.out.println("Established event received for, callingDevice = " 
                + ee.getCallingDevice().getDeviceIdentifier().getContent() 
                + ", calledDevice = " + ee.getCalledDevice().getDeviceIdentifier().getContent());
            // choose device which is different than station specified in selectivestream.properties
            DeviceID device = null;
            if (ee.getCalledDevice().getDeviceIdentifier().getContent().contains(station.trim())) {
                device = ee.getCallingDevice().getDeviceIdentifier();
            } else {
                device = ee.getCalledDevice().getDeviceIdentifier();
            }
            if(device != null && !DEVICE_LIST.contains(device))
                DEVICE_LIST.add(device);
            
            if (!isRecording) {
                startRecording();
            }
            if (!isPrecall) {
                sendSelectiveStreamRequest();
            }
        }
    }

    /**
     * Informs application if session is terminated and initiates cleanup of 
     * an application
     */
    private class MyServiceProviderListener implements ServiceProviderListener
    {
        @Override
        public void serverConnectionDown(ServerConnectionDownEvent event)
        {
            System.out.println("The connection to the server is down because: " + event.getReason());
            System.exit(0); // exits to the shutdownThread to perform cleanup
        }

        @Override
        public void serverSessionNotActive(ServerSessionNotActiveEvent event)
        {
            System.out.println("The server session is not active because " + event.getReason());
            System.exit(0); // exits to the shutdownThread to perform cleanup
        }

        @Override
        public void serverSessionTerminated(ServerSessionTerminatedEvent event)
        {
            System.out.println("The server session has terminated because " + event.getReason());
            System.exit(0); // exits to the shutdownThread to perform cleanup
        }
    }

    /**
     * This cleanup method assures us that we unregister our party, remove all
     * the listeners, recordings that were happening. The connector server will
     * cleanup if a client goes away without doing this cleanup, but it's best
     * to be safe.
     */
    private void cleanup(AvayaStation station)
    {
        // clean up scanner
        if (sc != null) {
            sc.close();
        }
        String id = station.getDeviceID().getExtension();
        System.out.println("The application is terminating: clean up party " + id);
        // There is a chance that AvayaStation has already cleaned up as a
        // result of getting the unregistered event. In this case, the station
        // is already unregistered, the device ID has been released, and the
        // server should have cleaned up. Don't bother doing anything else.
        if (station.getDeviceID() == null) {
            return;
        }
        if (isRecording) {
            stopRecording();
        }
        try {
            // Call the synchronous version of unregister() because we want the
            // unregister to succeed before cleaning up and disconnecting.
            station.unregister();
        } catch (Exception e) {
            System.out.println("Could not clean up properly station: " + id);
            e.printStackTrace(System.out);
        } finally {
            try {
                station.cleanup();
            } catch (Exception e) {
                System.out.println("Could not clean up properly station: " + id);
                e.printStackTrace(System.out);
            }
        }
    }

    /**
     * This is a very simple class that is used to do cleanup when 
     * application is terminated. The Communication Manager API server code 
     * will clean up if an application goes away unexpectedly, but it's still 
     * good to clean up.
     */
    private class MyShutdownThread extends Thread
    {
        @Override
        public void run()
        {
            System.out.println("Shutdown thread called.");
            // Unregister the station and terminate the application
            if (avayaStation != null) {
                cleanup(avayaStation);
            }
            // Disconnect each service provider from AES server
            try {
                if (serviceProvider != null) {
                    serviceProvider.removeServiceProviderListener(sessionListener);
                    serviceProvider.disconnect(true);
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
        }
    }

    /**
     * Removes all of the listeners that were added for the various services.
     */
    private void removeListeners() throws CstaException
    {
        monSvcs.removeCallControlListener(deviceA, listener);
        monSvcs.removeRegistrationListener(deviceA, regListener);
        monSvcs.removeEndpointRegistrationStateListener(eptRegListener);
    }

    /**
     * Handles the case of undesired un-registration from Communication Manager.
     */
    private class MyRegistrationListener extends RegistrationAdapter
    {
        private final AvayaStation station;
        public MyRegistrationListener(AvayaStation station)
        {
            this.station = station;
        }

        @Override
        public void terminalUnregistered(TerminalUnregisteredEvent event)
        {
            try {
                System.out.println("Received unregistered event: reason " + event.getReason());
                System.out.println("Terminating application...");
                removeListeners();
                station.cleanup();
                System.exit(0);
            } catch (CstaException e) {
                System.out.println("Exception when removing listeners or releasing device ID " + e);
            }
        }
    }

    private class MyEptRegistrationListener extends EndpointRegistrationStateAdapter
    {
        public MyEptRegistrationListener() {}

        @Override
        public void registrationEventNotify(EndpointRegisteredEvent event)
        {
            System.out.println("Device is registered in " + getDependencyMode().toString() + " mode.");
            boolean isPrecall = true;
            if (ssrMode != null && ssrMode.equalsIgnoreCase("mid-call")) {
                isPrecall = false;
            }
            if(!isPrecall)
                System.out.println("Add parties in the call with device to send "
                    + "selective stream request using SelectiveStreamRequest API.");
        }
    }

}
