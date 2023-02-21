/*
 * SessionManagementApp.java
 * 
 * Copyright (c) 2002-2007 Avaya Inc. All rights reserved.
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

package sampleapps.session;

// Java utilities
import java.net.URL;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import sampleapps.station.AvayaStation;
import sampleapps.station.AvayaStationAdapter;
import ch.ecma.csta.binding.DeviceID;
import ch.ecma.csta.binding.DisplayUpdatedEvent;
import ch.ecma.csta.binding.MonitorObject;
import ch.ecma.csta.binding.MonitorStartResponse;
import ch.ecma.csta.binding.RingerStatusEvent;
import ch.ecma.csta.errors.CstaException;
import ch.ecma.csta.monitor.MonitoringServices;
import ch.ecma.csta.physical.PhysicalDeviceListener;

import com.avaya.api.media.audio.Audio;
import com.avaya.cmapi.APIProtocolVersion;
import com.avaya.cmapi.ServiceProvider;
import com.avaya.csta.binding.GetDeviceIdList;
import com.avaya.csta.binding.GetDeviceIdListEvent;
import com.avaya.csta.binding.GetDeviceIdListResponse;
import com.avaya.csta.binding.GetMonitorList;
import com.avaya.csta.binding.GetMonitorListEvent;
import com.avaya.csta.binding.GetMonitorListResponse;
import com.avaya.csta.binding.GetSessionIdListResponse;
import com.avaya.csta.binding.MediaInfo;
import com.avaya.csta.binding.MonitorObjectData;
import com.avaya.csta.binding.SessionIDList;
import com.avaya.csta.binding.TerminalUnregisteredEvent;
import com.avaya.csta.binding.TransferMonitorObjects;
import com.avaya.csta.binding.TransferMonitorObjectsEvent;
import com.avaya.csta.binding.TransferMonitorObjectsResponse;
import com.avaya.csta.binding.types.DependencyMode;
import com.avaya.csta.binding.types.MediaMode;
import com.avaya.csta.device.DeviceServices;
import com.avaya.csta.device.DeviceServicesListener;
import com.avaya.csta.device.DeviceServicesListenerAdapter;
import com.avaya.csta.monitor.TransferProxy;
import com.avaya.csta.monitor.TransferProxyListener;
import com.avaya.csta.physical.LampModeConstants;
import com.avaya.csta.physical.RingerPatternConstants;
import com.avaya.csta.registration.RegistrationAdapter;
import com.avaya.csta.registration.RegistrationListener;
import com.avaya.mvcs.framework.CmapiKeys;

/**
 * Purpose: This tutorial application demonstrates how to write a session 
 * management application using the Device, Media and Call Control API (DMCC). 
 * It simulates a client application system with N+1 redundancy, where N is two.
 * The standby application takes over the device that belonged to one of 
 * the applications. 
 * 
 * Three service providers are started with one of them reserved as a standby.  
 * The remaining providers are used to make CSTA service requests. 
 * 
 * Note that each provider represents a separate client application.
 * 
 * The session state for each active provider is retrieved from the DMCC server
 * using the standby provider. Next, one of the active sessions is transferred 
 * to the standby application. After the transfer, the standby application will 
 * reconstruct the station and listeners that are required.  
 * 
 * @author AVAYA, Inc.
 */

@SuppressWarnings("unused")
public class SessionManagementApp
{

    // These are the names of the Wave files that contain the pre-recorded
    // greetings. These wave files must be present on the server in the
    // directory specified by the "player" property in the
    // cmapi-user-configuration.properties file.
    
    private static final String PROP_FILE_CMAPI_SERVER = "cmapi1.server_ip";
    private static final String PROP_FILE_USERNAME = "cmapi1.username";
    private static final String PROP_FILE_PASSWORD = "cmapi1.password";
    private static final String PROP_FILE_SERVER_PORT ="cmapi1.server_port";
    private static final String PROP_FILE_SECURE ="cmapi1.secure";
    private static final int TIMEOUT = 10000; // 10sec timeout for reading events

    int NUM_STATIONS = 0;
    int BASE_EXT = 3000;

    // These field is used to track the active call appearance. The field will  
    // be set to null when that action is not currently taking place.
    private String activeCallAppearance = null;

    // This thread is used to receive a shutdown notification when the
    // application is terminated.
    private MyShutdownThread shutdownThread;

    // These fields are read in from the application.properties file. This file
    // must be populated with a valid IP address for Avaya Communication
    // Manager, as well as a valid party number and password for the
    // application softphone.
    private String callServer;
    private String partyToTransfer;
    private String callingParty;
    private String calledParty;
    private String password;
    private String codec;
    private String encryption;
    private boolean isMultiple = false;

    // The Service Provider used to get the handles of various services.
    private ServiceProvider provider1;
    private ServiceProvider provider2;
    private ServiceProvider providerStdby;

    // Handles to various services
    MonitoringServices montSvcs;
    DeviceServices devSvcs;

    // These fields will be populated with the handles of the various services,
    // which we will get through the ServiceProvider class.
    private AvayaStation station1;  // Active session station 1
    private AvayaStation station2;  // Active session station 2
    private AvayaStation station1Stdby;  // Standby station 1 reference

    // These are the listeners that will be added to listen for events coming
    // from VoiceUnitServices, ToneCollectionServices, TerminalServices, and
    // PhysicalDeviceServices.    
    private MyAvayaStationListener avayaStationListener;
    
    private MyRegistrationListener registrationListener;

    private DeviceServicesListenerAdapter deviceServicesListener; 
    /**
     * Creates an instance of the SessionManagementApp class, bootstraps it, then
     * starts the application.
     */
    public static void main(String[] args)
    {
        SessionManagementApp app = new SessionManagementApp();
        try
        {
            app.bootstrap();
            app.start();
        }
        catch (CstaException e)
        {
            System.out.println("Could not start the session management app");
            e.printStackTrace(System.out);
            System.exit(0);
        }
        catch (Exception e)
        {
            System.out.println("Could not start the session management app");
            e.printStackTrace(System.out);
            System.exit(0);
        }
    }

    /**
     * This is the bootstrap to read in the needed properties, and get handles
     * to all services that are used by the application.
     *
     * All applications will have to have some administered properties that
     * include at a minimum the Communication Manager address, the party
     * number and the password. This application uses a properties file to
     * administer these values. Other applications could choose to use a
     * database or have the user administer the values when the application
     * starts up. This is a choice for the application developer.
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
        
        // Get all of the arguments from the application properties file
        ClassLoader cl = SessionManagementApp.class.getClassLoader();
        URL appURL = cl.getResource("sessionmanagement.properties");
        Properties appProp = new Properties();
        appProp.load(appURL.openStream());
        callServer = appProp.getProperty("callserver").trim();
        partyToTransfer = appProp.getProperty("partytotransfer").trim();
        calledParty = appProp.getProperty("calledparty").trim();
        callingParty = appProp.getProperty("callingparty").trim();
        password = appProp.getProperty("password").trim();
        codec = appProp.getProperty("codec").trim();
        encryption = appProp.getProperty("encryption").trim();
        String sessionMode = appProp.getProperty("sessionMode");
        if(sessionMode != null && sessionMode.trim().equalsIgnoreCase("MULTIPLE")) {
            isMultiple = true;
        }
         
        cmapiServerIp = appProp.getProperty(PROP_FILE_CMAPI_SERVER).trim();
        cmapiUsername = appProp.getProperty(PROP_FILE_USERNAME).trim();
        cmapiPassword = appProp.getProperty(PROP_FILE_PASSWORD).trim();
        // If there is no entry then we will assume the connection is secure.
        cmapiServerPort= appProp.getProperty(PROP_FILE_SERVER_PORT, "4722").trim();
        cmapiSecure= appProp.getProperty(PROP_FILE_SECURE, "true").trim();
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

        spProp.put(CmapiKeys.SESSION_PROTOCOL_VERSION_LIST, APIProtocolVersion.VERSION_LIST);
        
        if(cmapiTrustStoreLocation != null){
            spProp.setProperty(CmapiKeys.TRUST_STORE_LOCATION, cmapiTrustStoreLocation.trim());
        }
        
        // add following properties for validation client and service side certifications 
        if(cmapiTrustStorePassword != null){
            spProp.setProperty(CmapiKeys.TRUST_STORE_PASSWORD, cmapiTrustStorePassword.trim());
        }
        if(cmapiKeyStoreLocation != null){
            spProp.setProperty(CmapiKeys.KEY_STORE_LOCATION, cmapiKeyStoreLocation.trim());
        }
        if(cmapiKeyStoreType != null){
    		spProp.setProperty(CmapiKeys.KEY_STORE_TYPE, cmapiKeyStoreType.trim());
    	}
        if(cmapiKeyStorePassword != null){
            spProp.setProperty(CmapiKeys.KEY_STORE_PASSWORD, cmapiKeyStorePassword.trim());
        }
        if(isValidPeer != null){
            spProp.setProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE, isValidPeer.trim());
        }
        if(hostnameValidation != null){
            spProp.setProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE_HOSTNAME, hostnameValidation.trim());
        }        
        // Get a handle to the ServiceProvider class.
        provider1 = ServiceProvider.getServiceProvider(spProp);
        provider2 = ServiceProvider.getServiceProvider(spProp);
        providerStdby = ServiceProvider.getServiceProvider(spProp);
        
        // Get handles to various services 
        montSvcs = (MonitoringServices) providerStdby.getService(
                MonitoringServices.class.getName());
        devSvcs = (DeviceServices) providerStdby.getService(DeviceServices.class.getName());
        
        // Create new AvayaStation object.
        station1 = new AvayaStation();
        station1.init(callServer, "", partyToTransfer, provider1, isMultiple);
        station2 = new AvayaStation();
        station2.init(callServer, "", callingParty, provider2, isMultiple);
        
        RegistrationListener regListener = new MyRegistrationListener(station1, provider1);
        AvayaStation[] stations = new AvayaStation[NUM_STATIONS];
        for (int i = 0; i < stations.length; i++) {
            stations[i] = new AvayaStation();
            String ext = BASE_EXT + i + "";
            stations[i].init(callServer, "", ext, provider1, isMultiple);        
            stations[i].addListener(regListener);             
        }
        System.out.println("Added " + NUM_STATIONS + " devices starting from " + BASE_EXT);
    }

    /**
     * This method actually starts the app. It must be called after calling
     * bootstrap or it will fail.
     *
     * @throws Exception - if an Exception is generated by one of the calls to
     * the services, it is thrown to the caller.
     */
    public void start() throws Exception
    {
        System.out.println(
            "Startup using switch="
                + callServer
                + " partyToTransfer="
                + partyToTransfer
                + " callingParty="
                + callingParty
                + " calledParty="
                + calledParty
                + " pw="
                + password);

        // Create a thread in whose context our cleanup will occur if the app is
        // terminated. The Communication Manager API connector server code will
        // clean up if an app goes away unexpectedly, but it's still good to
        // clean up.
        shutdownThread = new MyShutdownThread();
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        // This device which was added by another session can be used because
        // the DeviceServices events are for all devices on this session.
        deviceServicesListener =  new DeviceServicesListenerAdapter();
        
        // Add a DeviceServicesListener to listen for events that relate to all
        // the devices on this session.
        montSvcs.addDeviceServicesListener(deviceServicesListener.getDeviceID(), 
                deviceServicesListener);
        System.out.println("Adding DeviceServicesListener " + deviceServicesListener);
        
        // Add listeners to be notified of events coming from the various
        // services.
        addListeners(station1, provider1); 
        addListeners(station2, provider2);  
        
        // Register the stations
        register(station1, provider1);             
        register(station2, provider2);
        
        // Get the internal server state for information each session
        getSessionIdList(providerStdby);
        getDeviceIdList(provider1.getSessionID(), providerStdby);
        getMonitorList(provider1.getSessionID(), providerStdby);
        getDeviceIdList(provider2.getSessionID(), providerStdby);
        getMonitorList(provider2.getSessionID(), providerStdby);  
        getDeviceIdList(providerStdby.getSessionID(), providerStdby);
        getMonitorList(providerStdby.getSessionID(), providerStdby);  

        System.out.println("Transfer will start in 2 secs");
        Thread.sleep(2000);
        long starttime = System.currentTimeMillis();
        // Transfer session 1 the standby provider
        transferMonitorObjects(provider1.getSessionID(), 
                providerStdby.getSessionID(), providerStdby);
        long interval = (System.currentTimeMillis() - starttime);

        // Verify that the transferred station can receive events
        makecall(station2, partyToTransfer);

        // Verify that the server state information is correct
        getSessionIdList(providerStdby);
        getMonitorList(providerStdby.getSessionID(), providerStdby);
        //System.out.println("Transferred " + NUM_STATIONS + " devices, each with one monitor in " + interval + "msecs");
        
        // Verify that the transferred station can dial out.
        makecall(station1Stdby, calledParty);

        // Cleanup occurs in MyShutdownThread 
        System.exit(0);
    }
        
    /**
     * This method performs synchronous registration to Communication Manager.
     * 
     * @param station   The AvayaStation to be registered.
     * @param provider  The ServiceProvider to use for sending requests.
     * @throws CstaException
     */
    private void register(AvayaStation station, ServiceProvider provider) 
    throws CstaException {
        
        // Time to register the device. Pass in the password. Shared control
        // is set to false for this application. Default media settings (media
        // goes to connector server over G.711 codec) are fine, thus the null
        // handle for the MediaInfo.
        // Register asynchronously since there's no reason to hold up this
        // thread waiting for the response.
        MediaInfo localMediaInfo = new MediaInfo();
        
        // optional codec settings
        if (Audio.G711U.equalsIgnoreCase(codec))
            localMediaInfo.setCodecs(new String[] {Audio.G711U});
        else if (Audio.G711A.equalsIgnoreCase(codec))
            localMediaInfo.setCodecs(new String[] {Audio.G711A});
        else if (Audio.G729.equalsIgnoreCase(codec))
            localMediaInfo.setCodecs(new String[] {Audio.G729});
        else if (Audio.G729A.equalsIgnoreCase(codec))
            localMediaInfo.setCodecs(new String[] {Audio.G729A});
        else // default
            localMediaInfo.setCodecs(new String[] {Audio.G711U, Audio.G711A});
        
        // optional encryption settings
        if (Audio.AES.equalsIgnoreCase(encryption))
            localMediaInfo.setEncryptionList(new String[] {Audio.AES});
        else if (Audio.NOENCRYPTION.equalsIgnoreCase(encryption))
            localMediaInfo.setEncryptionList(new String[] {Audio.NOENCRYPTION});
        else // default
            localMediaInfo.setEncryptionList(new String[] {Audio.AES, Audio.NOENCRYPTION});
        
        System.out.println(" setupAudio" +
            ": codec=" + localMediaInfo.getCodecs()[0] +
            ", encryption=" + localMediaInfo.getEncryptionList()[0]);

        try {
			station.register(password, DependencyMode.MAIN, MediaMode.SERVER, null, null, true,
			        localMediaInfo,null, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        System.out.println(station.getDeviceID() + " phone registered for " +
                (isMultiple ? "multiple":"single") + " session control.");
    }
    
    //**************************************************************************
    // makecall
    //**************************************************************************
    /**
     * @param from
     * @param to
     * @throws Exception
     */
    private void makecall(AvayaStation from, String to) throws Exception {
        from.initCall(to, "263");
        
        // Wait for the call to be answered
        Thread.sleep(5000);
        from.disconnect();
    }
        
    /**
     * Creates listener objects for the different services and adds them so the
     * app will be informed of events coming from the various services.
     */
    private void addListeners(AvayaStation station, ServiceProvider provider) 
    throws Exception
    { 
        MonitoringServices montSvcs = (MonitoringServices) provider.getService(
                ch.ecma.csta.monitor.MonitoringServices.class.getName());
        // Get the device
        DeviceID id = station.getDeviceID();
        
        // Add an AvayaStationListner to receive events indicating phone rings,
        // lamp and display updates.
        avayaStationListener = new MyAvayaStationListener(station);
        station.addListener(avayaStationListener);
        
        // Add a listener so we can receive events indicating when the phone is
        // registered or unregistered.
        registrationListener = new MyRegistrationListener(station, provider);
        montSvcs.addRegistrationListener(id, registrationListener);
    }

    /**
     * Removes all of the listeners that were added for the various services.
     */
    private void removeListeners(AvayaStation station, ServiceProvider provider) 
    throws Exception
    {
        MonitoringServices montSvcs = (MonitoringServices) provider.getService(
                ch.ecma.csta.monitor.MonitoringServices.class.getName());
        DeviceID id = station.getDeviceID();
        station.removeListener(avayaStationListener);
        montSvcs.removeRegistrationListener(id, registrationListener);     
    }    
  
    /**
     * Retrieves the list of sessions that belong to this user.
     * 
     * @param  provider the service provider which will deliver the request.
     * @return String[] the list of sessions belonging to this user.
     * @throws Exception
     */
    private String[] getSessionIdList(ServiceProvider provider) throws Exception {
        GetSessionIdListResponse sessionResp = provider.getSessionIdList();
        SessionIDList sessionRespList = sessionResp.getSessionIDList();
        String[] sessionIDArr = sessionRespList.getSessionID();    
        
        StringBuilder sb = new StringBuilder();
        sb.append("SessionID list: ");
        for(int i = 0; i < sessionIDArr.length; i++) {
            sb.append(sessionIDArr[i] + ", ");                    
        }
        System.out.println("\n***** GetSessionIdListResponse *****");
        System.out.println(sb.toString());
        return sessionIDArr;
    }

    /**
     * Retrieves the list of devices that belong to the specified session identifier.
     * 
     * @param  sessionID the session whose device list will be retrieved.
     * @param  provider  the service provider used to send the request.
     * @return String[]  the list of devices belonging to the sessionID.
     * @throws Exception
     */
    private DeviceID[] getDeviceIdList(String sessionID, 
            ServiceProvider provider) throws Exception {

        DeviceServices myDevSvcs = (DeviceServices) provider.getService(
                com.avaya.csta.device.DeviceServices.class.getName());
        GetDeviceIdList request = new GetDeviceIdList();
        if(sessionID != null) {
            request.setSessionID(sessionID);
        }
        GetDeviceIdListResponse resp = myDevSvcs.getDeviceIdList(request);

        List<DeviceID> deviceList = new ArrayList<DeviceID>(); 
        List<GetDeviceIdListEvent> events = 
            deviceServicesListener.getDeviceIdListEvent(TIMEOUT);
        for(GetDeviceIdListEvent event : events) {
            DeviceID[] temp = event.getDeviceIDList().getDeviceID();
            for (int i = 0; i < temp.length; i++) {
                deviceList.add(temp[i]);
            }
        }
        DeviceID[] deviceIDArr = deviceList.toArray(new DeviceID[0]);
        StringBuilder sb = new StringBuilder();
        sb.append("Session " + sessionID + " device" + 
                (deviceIDArr.length > 1 ? "s:" : ":"));
        for(int i = 0; i < deviceIDArr.length; i++) {
            sb.append((i == 0 ? " " : ", ") + deviceIDArr[i]);                    
        }
        System.out.println("\n***** GetDeviceIdListResponse *****");
        System.out.println(sb.toString());
        return deviceIDArr;
    }

    /**
     * Retrieves the devices and associated monitors that belong to the 
     * specified session identifier.
     * 
     * @param  sessionID the session whose monitor list will be retrieved.
     * @param  provider  the service provider used to send the request.
     * @return String[]  the list of devices belonging to the sessionID.
     * @throws Exception
     */
    private MonitorObjectData[] getMonitorList(String sessionID, 
            ServiceProvider provider) throws Exception {
        
        DeviceServices myDevSvcs = (DeviceServices) provider.getService(
                com.avaya.csta.device.DeviceServices.class.getName());
        GetMonitorList request = new GetMonitorList();
        if(sessionID != null) {
            request.setSessionID(sessionID);
        }
        GetMonitorListResponse resp = myDevSvcs.getMonitorList(request);

        // Get the events which contain the chunked response data
        List<MonitorObjectData> modList = new ArrayList<MonitorObjectData>();

        List<GetMonitorListEvent> events = 
            deviceServicesListener.getMonitorListEvent(TIMEOUT);
        for(GetMonitorListEvent event : events) {
            MonitorObjectData[] temp = 
                event.getMonitorObjectDataList().getMonitorObjectData();
            for (int i = 0; i < temp.length; i++) {
                modList.add(temp[i]);
            }
        }
        
        MonitorObjectData[] monObjDataArr = modList.toArray(new MonitorObjectData[0]);
        StringBuilder sb = new StringBuilder();
        sb.append("Session " + sessionID + " device" + 
                (monObjDataArr.length > 1 ? "s:" : ":"));
        for(int i = 0; i < monObjDataArr.length; i++) {
            MonitorObjectData mod = monObjDataArr[i];
            DeviceID device = mod.getMonitorObject().getDeviceObject();
            
            if(device != null) {
                sb.append((i == 0 ? " " : ", ") + "\n" + device);
                MonitorStartResponse[] monStartRespArr = mod.getMonitorStartResponse(); 
                sb.append(", cref" + 
                        (monStartRespArr.length > 1 ? "s: {" : ": {"));
                
                for(int j = 0; j < monStartRespArr.length; j++) {
                    MonitorStartResponse msr = monStartRespArr[j];
                    String crossRef = msr.getMonitorCrossRefID();
                    sb.append((j == 0 ? "" : ", ") + crossRef);
                }     
                sb.append("}");     
            }
        }
        System.out.println("\n***** GetMonitorListResponse *****");
        System.out.println(sb.toString());
        return monObjDataArr;
    }
    
    /**
     * Transfers the original session's devices to the new session. This can be
     * used to provide N + 1 client application duplication. N = 2 for this 
     * sample application because there are 2 active sessions and 1 standy session.
     *  
     * @param originalSessionID the session whose devices will be transferred.
     * @param newSessionID      the recipient of the devices that are transferred.
     * @param provider          the service provider used to send the request.
     * @throws Exception
     */
    private void transferMonitorObjects(String originalSessionID, 
            String newSessionID, ServiceProvider provider) throws Exception {

        DeviceServices myDevSvcs = (DeviceServices) provider.getService(
                com.avaya.csta.device.DeviceServices.class.getName());
        TransferProxy transferProxy = (TransferProxy)provider.getService(
                ch.ecma.csta.monitor.MonitoringServices.class.getName());
        
        TransferMonitorObjects request = new TransferMonitorObjects();
        request.setFromSessionID(originalSessionID);
        request.setToSessionID(newSessionID);
        Map<MonitorObject, List<TransferProxyListener>> listenerMap = 
            new HashMap<MonitorObject, List<TransferProxyListener>>();
        TransferMonitorObjectsResponse resp = myDevSvcs.transferMonitorObjects(request);
        
        // Get the events which contain the chunked response data
        List<TransferMonitorObjectsEvent> events = 
            deviceServicesListener.getTransferMonitorObjectEvent(TIMEOUT);
        for(TransferMonitorObjectsEvent event : events) {
            Map<MonitorObject, List<TransferProxyListener>> current =
                transferProxy.createTransferProxyListeners(event);
            listenerMap.putAll(current);
        }
        
        // loop through transferMap and replace with application specific listeners
        StringBuilder sb = new StringBuilder();
        for(MonitorObject monitorObject: listenerMap.keySet()) {  
            DeviceID deviceID = monitorObject.getDeviceObject();
            AvayaStation station = new AvayaStation(); 
            PhysicalDeviceListener avayaStationListener = 
                station.setIDAndGetPhysicalDeviceListener(deviceID, providerStdby);
            List<TransferProxyListener> list = listenerMap.get(monitorObject);
            sb.append(station.getDeviceID() + "added listeners [\n");
            for(TransferProxyListener listener: list) {
                EventListener replacement = null;
                if (listener instanceof PhysicalDeviceListener) {
                    station.addListener(new MyAvayaStationListener(station));
                    replacement = avayaStationListener;
                } 
                else if (listener instanceof RegistrationListener) {
                    replacement = new MyRegistrationListener(station, provider);
                }
                
                transferProxy.replaceTransferProxyListener(monitorObject, listener, 
                        replacement);
                sb.append(replacement.getClass().getName() + ", ");
                if(deviceID.getExtension().equals(partyToTransfer)) {
                    station1Stdby = station;         
                } 
            }
        }
        //System.out.println(sb.substring(0, sb.lastIndexOf(",")));
        System.out.println("Transferred session " + originalSessionID + " to " + newSessionID);
    }
    
    /**
     * This cleanup method assures us that we unregister our party, remove
     * all the listeners, stop collecting DTMF tones and stop any announcements
     * or recordings that were happening. The connector server will cleanup if
     * a client goes away without doing this cleanup, but it's best to be safe.
     */
    private void cleanup(AvayaStation station, ServiceProvider provider)
    {
        String id = station.getDeviceID().getExtension();
        
        
        System.out.println("The application is terminating: clean up party "+id);

        // There is a chance that AvayaStation has already cleaned up as a
        // result of getting the unregistered event. In this case, the station
        // is already unregistered, the device ID has been released, and the
        // server should have cleaned up. Don't bother doing anything else.
        if (station.getDeviceID() == null)
        {
            return;
        }
        
        try
        {            
            // Call the synchronous version of unregister() because we want the
            // unregister to succeed before cleaning up and disconnecting.
            station.unregister();   
        }
        catch (Exception e)
        {
            System.out.println("Could not clean up properly party " + id);
            e.printStackTrace(System.out);
        } finally {
            try {
                station.cleanup();
            } 
            catch(Exception e) 
            {
                System.out.println("Could not clean up properly party " + id);
                e.printStackTrace(System.out);
            }
        }
    }

    private class MyDeviceServicesListener implements DeviceServicesListener {
        private int timeout = TIMEOUT; // 10 seconds
        private DeviceID device;
        private ServiceProvider provider;
        private final GetDeviceIdListEvent LastGDLEvent = new GetDeviceIdListEvent();
        private final GetMonitorListEvent LastGMLEvent = new GetMonitorListEvent();
        private final TransferMonitorObjectsEvent LastTMOEvent = new TransferMonitorObjectsEvent();
        
        private List<GetDeviceIdListEvent> deviceList = new ArrayList<GetDeviceIdListEvent>();
        private List<GetMonitorListEvent> monitorList = new ArrayList<GetMonitorListEvent>();
        private List<TransferMonitorObjectsEvent> transferList = new ArrayList<TransferMonitorObjectsEvent>();

        public MyDeviceServicesListener(int timeout, ServiceProvider provider, 
                DeviceID device) {
            this.timeout = timeout;
            this.provider = provider;
            this.device = device;
        }
        
        public DeviceID getDeviceID() {
            return this.device;            
        }
        
        public ServiceProvider getProvider() {
            return this.provider;
        }
        
        //**************************************************************************
        // getDeviceIdList
        //**************************************************************************
        /* (non-Javadoc)
         * @see com.avaya.csta.device.DeviceServicesListener#getDeviceIdList(com.avaya.csta.binding.GetDeviceIdListEvent)
         */
        public synchronized void getDeviceIdList(GetDeviceIdListEvent event) {
            synchronized(deviceList) {
                deviceList.add(event);
                if(event.getIsFinalResponse())
                    deviceList.add(LastGDLEvent);
                deviceList.notify();
            }
            System.out.println("Queue add " + event);
        }

        //**************************************************************************
        // getMonitorList
        //**************************************************************************
        /* (non-Javadoc)
         * @see com.avaya.csta.device.DeviceServicesListener#getMonitorList(com.avaya.csta.binding.GetMonitorListEvent)
         */
        public void getMonitorList(GetMonitorListEvent event) {
            synchronized(monitorList) {
                monitorList.add(event);
                if(event.getIsFinalResponse())
                    monitorList.add(LastGMLEvent);
                monitorList.notify();
            }
            System.out.println("Queue add " + event);
        }

        //**************************************************************************
        // transferMonitorObjects
        //**************************************************************************
        /* (non-Javadoc)
         * @see com.avaya.csta.device.DeviceServicesListener#transferMonitorObjects(com.avaya.csta.binding.TransferMonitorObjectsEvent)
         */
        public void transferMonitorObjects(TransferMonitorObjectsEvent event) {
            synchronized(transferList) {
                transferList.add(event);
                if(event.getIsFinalResponse())
                    transferList.add(LastTMOEvent);
                transferList.notify();
            }
            System.out.println("Queue add " + event);
        }

        public List<GetDeviceIdListEvent>getDeviceIdListEvent() throws Exception {
            List<GetDeviceIdListEvent> eventList = readQueue(deviceList);
            return eventList;
        }   
        
        public List<GetMonitorListEvent>getMonitorListEvent() throws Exception {
            List<GetMonitorListEvent> eventList = readQueue(monitorList);
            return eventList;
        }     
        
        public List<TransferMonitorObjectsEvent> getTransferMonitorObjectEvent() throws Exception {
            List<TransferMonitorObjectsEvent> eventList = readQueue(transferList);
            return eventList;
        }     
        
        private <T extends Object> List<T> readQueue(List<T> source) {
            List<T> eventList = new ArrayList<T>();
            boolean isDone = false;
            long endTime = timeout + System.currentTimeMillis();
            long timeLeft = endTime;

            while(!isDone && timeLeft > 0) {
                if(source.isEmpty()) {
                    synchronized(source) {
                        try {
                            //System.out.println("Queue wait");
                            source.wait(timeout);
                        } catch(InterruptedException ie) {
                            System.out.println("Queue wait interrupted");
                            // do nothing
                        }
                    }
                } else {
                    T event = null;
                    synchronized(source) {
                        event = source.remove(0);
                        endTime = System.currentTimeMillis() + timeout;
                    }
                    eventList.add(event);
                    if(event instanceof GetDeviceIdListEvent) {
                        isDone = ((GetDeviceIdListEvent)event).getIsFinalResponse();
                    } else if(event instanceof GetMonitorListEvent) {
                        isDone = ((GetMonitorListEvent)event).getIsFinalResponse();
                    } else if(event instanceof TransferMonitorObjectsEvent) {
                        isDone = ((TransferMonitorObjectsEvent)event).getIsFinalResponse();
                    }
                    //System.out.println("Queue remove: " + event);
                }
                timeLeft = endTime - System.currentTimeMillis();
            }
            return eventList;            
        }
    }
    
    /**
     * This is the physical device listener class. It extends
     * PhysicalDeviceAdapter rather than implementing the
     * PhysicalDeviceListener interface. This allows us to not implement all of
     * the methods in PhysicalDeviceListener.
     */
    private class MyAvayaStationListener extends AvayaStationAdapter
    {   
        private AvayaStation station = null;
        String id = null;
        
        public MyAvayaStationListener(AvayaStation station) {
            this.station = station;
            this.id = station.getDeviceID().getExtension() + ": ";
        }
        
        /**
         * Method is fired when the a Display update is received
         *
         * @param event the DisplayUpdatedEvent
         */
        public void displayUpdated(DisplayUpdatedEvent event)
        {
            System.out.println(id + "Display update event: " + 
                    event.getContentsOfDisplay());
        }

        /**
         * Method is fired when the LampMode update is received. In this app,
         * we only pay attention to updates to green call appearance lamps.
         * These lamps tell us the status of the call on that call appearance.
         * A flashing green lamp indicates an incoming call. When that lamp
         * becomes steady, that indicates that the call has been established.
         * When that lamp goes dark, the far end has hung up. One thing to note
         * about the implementation of this method is that for Avaya terminals,
         * the ID of the green call appearance ID is the same as the ID for the
         * corresponding call appearance button. This fact is critical to the
         * implementation of this application.
         *
         * NOTE: In future releases of the Communication Manager API we will be
         * supplying Call Control Services which will, among other things, give
         * a much better way of determining call state. It will be a cleaner
         * way to determine when calls are incoming, when they are fully
         * established, and when the calls are disconnected.
         * 
         * Note: All the responses/events are delivered on one thread. This guarantees
         * sequential processing of events/responses. However if the processing of an
         * event involves an I/O as this function does, you may
         * want to process this event in a different thread.
         * One approach would be to have one separate thread processing events and responses
         * off of a queue. All Listeners would queue events/responses to this queue if 
         * events/responses are either blocking or require sequential processing.
         *
         * @param event the received LampModeEvent
         */
        public void callAppearanceLampUpdated(String lampID, Short lampValue)
        {

            if (LampModeConstants.FLASH.equals(lampValue))
            {
                // A flashing green call appearance lamp indicates that there
                // is an incoming call, so answer it.
                System.out.println(id + " Lamp " + lampID + " is now flashing.");
                try
                {
                    station.answerCall(lampID);
                }
                catch (CstaException e)
                {
                    System.out.println(id + " Couldn't answer the phone" + e);
                }
                System.out.println(id + " The phone was answered");
            }
            else if (LampModeConstants.STEADY.equals(lampValue))
            {
                // A steady green call appearance lamp indicates that the call
                // is now established. We can play our greeting.
                System.out.println(id +"Lamp " + lampID + " is now on.");
                activeCallAppearance = lampID;

                System.out.println(id + "The call has been established.");
            }
            else if (LampModeConstants.BROKENFLUTTER.equals(lampValue))
            {
                System.out.println(id + "Lamp " + lampID + " is in broken flutter.");
            }
            else if (LampModeConstants.OFF.equals(lampValue))
            {
                System.out.println(id + "Lamp " + lampID + " is now off.");
                if (activeCallAppearance != null
                    && lampID.equals(activeCallAppearance))
                {

                    // If this lamp ID matches that of the active call
                    // appearance, the far end went on hook, and we can hang up.
                    System.out.println(id + "The other person hung up");
                    activeCallAppearance = null;

                    station.disconnect();
                    System.out.println(id + "Hung up the phone");

                }
            }
            else if (LampModeConstants.FLUTTER.equals(lampValue))
            {
                System.out.println(id + "Lamp " + lampID + " is fluttering.");
            }
        }

        /**
         * Method is fired when the RingerStatusEvent is received.
         *
         * @param event - the received RingerStatusEvent
         */
        public void ringerStatusUpdated(RingerStatusEvent event)
        {
            // If it is a ringer off message, return and ignore it.
            if (RingerPatternConstants.OFF.equals(event.getRingPattern()))
            {
                System.out.println(id +"The ringer is off.");
            }
            else
            {
                System.out.println(id +"The ringer is on.");
            }
        }
    }
 
    /**
     * This is the Registration listener class. It extends RegistrationAdapter rather
     * than implementing the RegistrationListener interface. This allows us to not
     * implement all of the methods in RegistrationListener.
     */
    private class MyRegistrationListener extends RegistrationAdapter
    {
        private AvayaStation station;
        private ServiceProvider provider;
        private String id;
        
        public MyRegistrationListener(AvayaStation station, ServiceProvider provider) {
            this.station = station;
            this.provider = provider;
            this.id = station.getDeviceID().getExtension() + ": ";
        }
        /**
         * This method is called if the party has become unregistered with
         * Communication Manager. This may have occurred as a result of an
         * unregistration request from the application, or Communication
         * Manager may have unregistered the party for other reasons.
         * 
         * @param event
         *            the TerminalUnregisteredEvent
         */
        public void terminalUnregistered(TerminalUnregisteredEvent event)
        {
            try
            {
                System.out.println(id + "Received unregistered event: reason " +
                        event.getReason());
                System.out.println(id + "Terminating application");
                // Make sure no listeners were left around.
                removeListeners(this.station, this.provider);
                station.cleanup();                          
                System.exit(0);
            }
            catch (Exception e)
            {
                System.out.println(id + "Exception when removing listeners or "
                        + "releasing device ID " + e);
            }
        }
    }    
    
    /**
     * This is a very simple class that is used to do cleanup when the app is
     * terminated. The Communication Manager API server code will clean up if
     * an app goes away unexpectedly, but it's still good to clean up.
     */
    private class MyShutdownThread extends Thread
    {
        public void run()
        {
            
            // Unregister the stations and terminate the application
            if(station1Stdby == null) {
                
                // Transfer didn't succeed
                cleanup(station1, provider1);
            } else {
                // Transfer succeeded
                cleanup(station1Stdby, providerStdby);
            }
            cleanup(station2, provider2);
            
            // Disconnect each service provider from AES server
            try {
                provider1.disconnect(true);
            } catch(Exception e) {            
            }
            try {
                provider2.disconnect(true);
            } catch(Exception e) {            
            }
            try {
                montSvcs.removeDeviceServicesListener(
                        deviceServicesListener.getDeviceID(), deviceServicesListener);
            } catch(Exception e) {
                e.printStackTrace();
            }
            try {
                providerStdby.disconnect(true);
            } catch(Exception e) {            
            }
        }
    }
}
