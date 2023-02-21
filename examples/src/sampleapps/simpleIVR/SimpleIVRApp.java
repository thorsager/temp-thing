/*
 * SimpleIVRApp.java
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

package sampleapps.simpleIVR;

// Java utilities
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import sampleapps.station.AvayaStation;
import sampleapps.station.AvayaStationAdapter;
import ch.ecma.csta.binding.CSTACommonArguments;
import ch.ecma.csta.binding.CallObject;
import ch.ecma.csta.binding.CancelTelephonyTones;
import ch.ecma.csta.binding.CancelTelephonyTonesResponse;
import ch.ecma.csta.binding.ConnectionID;
import ch.ecma.csta.binding.DeleteMessage;
import ch.ecma.csta.binding.DeleteMessageResponse;
import ch.ecma.csta.binding.DeviceID;
import ch.ecma.csta.binding.DisplayUpdatedEvent;
import ch.ecma.csta.binding.GenerateTelephonyTones;
import ch.ecma.csta.binding.GenerateTelephonyTonesResponse;
import ch.ecma.csta.binding.LocalDeviceID;
import ch.ecma.csta.binding.PlayEvent;
import ch.ecma.csta.binding.PlayMessage;
import ch.ecma.csta.binding.RecordEvent;
import ch.ecma.csta.binding.RecordMessage;
import ch.ecma.csta.binding.RecordMessageResponse;
import ch.ecma.csta.binding.RingerStatusEvent;
import ch.ecma.csta.binding.Stop;
import ch.ecma.csta.binding.StopEvent;
import ch.ecma.csta.binding.types.TelephonyTone;
import ch.ecma.csta.callassociated.CallAssociatedListener;
import ch.ecma.csta.callassociated.CallAssociatedServices;
import ch.ecma.csta.errors.CstaException;
import ch.ecma.csta.errors.InvalidDeviceStateException;
import ch.ecma.csta.monitor.MonitoringServices;
import ch.ecma.csta.voiceunit.VoiceUnitAdapter;
import ch.ecma.csta.voiceunit.VoiceUnitServices;

import com.avaya.api.media.audio.Audio;
import com.avaya.cmapi.APIProtocolVersion;
import com.avaya.cmapi.ServiceProvider;
import com.avaya.csta.async.AsynchronousCallback;
import com.avaya.csta.binding.MediaInfo;
import com.avaya.csta.binding.PlayMessagePrivateData;
import com.avaya.csta.binding.RegisterTerminalResponse;
import com.avaya.csta.binding.GenerateTelephonyTonesAbort;
import com.avaya.csta.binding.TerminalUnregisteredEvent;
import com.avaya.csta.binding.ToneCollectionFlushBuffer;
import com.avaya.csta.binding.ToneCollectionStart;
import com.avaya.csta.binding.ToneCollectionStop;
import com.avaya.csta.binding.ToneRetrievalCriteria;
import com.avaya.csta.binding.TonesRetrievedEvent;
import com.avaya.csta.binding.types.DependencyMode;
import com.avaya.csta.binding.types.MediaMode;
import com.avaya.csta.binding.types.TcollCause;
import com.avaya.csta.physical.LampModeConstants;
import com.avaya.csta.physical.RingerPatternConstants;
import com.avaya.csta.registration.RegistrationAdapter;
import com.avaya.csta.registration.RegistrationConstants;
import com.avaya.csta.tonecollection.ToneCollectionListener;
import com.avaya.csta.tonecollection.ToneCollectionServices;
import com.avaya.mvcs.framework.CmapiKeys;

/**
 * Purpose: This tutorial application demonstrates how to write a simple Interactive
 * Voice Response (IVR) application using the Avaya Communication Manager API.
 * It registers an extension with Communication Manager, and waits for that
 * extension to be dialed. When a user dials the associated extension number,
 * the application answers the incoming call, and plays a greeting to the user.
 * The user can then press 1 to hear a different greeting, 2 to start a
 * recording of everything the user says, 3 to play the last recording again,
 * 4 to transfer to another extension or 5 to conference in another extension.
 * All announcements / functions can be preempted by another button press. The
 * user can hang up and dial back into the application extension repeatedly.
 *
 * Registration is done asynchronously using Asynchronous Services.
 * 
 * @author AVAYA, Inc.
 * @version $Revision: 1.24 $
 */
@SuppressWarnings("unused")
public class SimpleIVRApp
{

    // These are the names of the Wave files that contain the pre-recorded
    // greetings. These wave files must be present on the server in the
    // directory specified by the "player" property in the
    // cmapi-user-configuration.properties file.

    private final static String GREETING = "greeting.wav";
    private final static String FACT = "fact.wav";
    private final static String RECORDING_STARTED = "record.wav";
    private final static String NOFILE = "nofile.wav";
    private final static String BEEP = "beep.wav";
    private final static String MENU = "menu.wav";
    private final static String XFER_MSG = "transfer.wav";
    private final static String CONF_MSG = "conference.wav";
    private final static String CONF_GREET = "conf-greet.wav";
    private final static String RECORD_WARN_ON = "record-warn-on.wav";
    private final static String RECORD_WARN_ON_FAIL = "record-warn-on-fail.wav";
    private final static String RECORD_WARN_OFF = "record-warn-off.wav";
    private final static String RECORD_WARN_OFF_FAIL = "record-warn-off-fail.wav";
    private final static String BAD_EXT = "bad-ext.wav";
    private final static String INVALID_KEY_PRESS = "invalidkeypress.wav";
//    private static final String PROP_FILE_CALL_SERVER = "callserver";
//    private static final String PROP_FILE_EXTENSION = "extension";
//    private static final String PROP_FILE_EXT_PASSWORD = "password";
//    private static final String PROP_FILE_MEDIA_CODEC = "codec";
//    private static final String PROP_FILE_MEDIA_ENCRYPTION = "encryption";
    private static final String PROP_FILE_CMAPI_SERVER = "cmapi1.server_ip";
    private static final String PROP_FILE_USERNAME = "cmapi1.username";
    private static final String PROP_FILE_PASSWORD = "cmapi1.password";
    private static final String PROP_FILE_SERVER_PORT ="cmapi1.server_port";
    private static final String PROP_FILE_SECURE ="cmapi1.secure";

    // These fields are used to track the name of the currently recording file
    // and to track the active call appearance. The fields are set to null when
    // that action is not currently taking place.
    private String currentRecordingFile = null;
    private String activeCallAppearance = null;

    // Set to true if we want to start a recording when the current
    // announcement is complete.
    private boolean needToRecord = false;

    // This thread is used to receive a shutdown notification when the
    // application is terminated.
    private MyShutdownThread shutdownThread;

    // Constants for conference or transfer operation.
    private static int NONE = 0;
    private static int CONFERENCE = 1;
    private static int TRANSFER = 2;
    private static int BAD_CONF_XFER = 3;
    private static int BAD_DROPPED = 4;

    // Indicates whether we are attempting a conference or a transfer.
    private int confOrXferState = NONE;

    // These fields are read in from the application.properties file. This file
    // must be populated with a valid IP address for Avaya Communication
    // Manager, as well as a valid extension number and password for the
    // application softphone.
    private String callServer;
    private String extension;
    private String password;
    private String codec;
    private String encryption;
    private boolean isMultiple = false;

    // The Device ID for our extension number will be supplied by the
    // Communication Manager API. The Connection ID is formulated from the
    // Device ID.
    private DeviceID id = null;
    private ConnectionID connection = null;

    // The Service Provider used to get the handles of various services.
    private ServiceProvider provider;

    // These fields will be populated with the handles of the various services,
    // which we will get through the ServiceProvider class.
    private VoiceUnitServices voiceSvcs;
    private ToneCollectionServices toneCollSvcs;
    private CallAssociatedServices callAssocSvcs;
    private AvayaStation station;
    // MonitoringServices will be used to add/remove voice unit listener and
    // tone collection listener.
    private MonitoringServices montSvcs;

    // These are the listeners that will be added to listen for events coming
    // from VoiceUnitServices, ToneCollectionServices, TerminalServices, and
    // PhysicalDeviceServices.
    private MyVoiceUnitListener playAndRecordListener;
    private MyToneCollectionListener toneCollListener;
    
    private MyAvayaStationListener avayaStationListener;
    
    private MyRegistrationListener registrationListener;
    
    private MyCallAssociatedListener callAssociatedListener;
    
    private boolean isRecordingToneSet = false;


    /**
     * Creates an instance of the SimpleIVRApp class, bootstraps it, then
     * starts the application.
     */
    public static void main(String[] args)
    {
        SimpleIVRApp app = new SimpleIVRApp();
        try
        {
            app.bootstrap();
            app.start();
        }
        catch (CstaException e)
        {
            System.out.println("Could not start the SimpleIVR app");
            e.printStackTrace(System.out);
            System.exit(0);
        }
        catch (Exception e)
        {
            System.out.println("Could not start the SimpleIVR app");
            e.printStackTrace(System.out);
            System.exit(0);
        }
    }

    /**
     * This is the bootstrap to read in the needed properties, and get handles
     * to all services that are used by the application.
     *
     * All applications will have to have some administered properties that
     * include at a minimum the Communication Manager address, the extension
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
        ClassLoader cl = SimpleIVRApp.class.getClassLoader();
        URL appURL = cl.getResource("simpleIVR.properties");
        Properties appProp = new Properties();
        appProp.load(appURL.openStream());
        callServer = appProp.getProperty("callserver").trim();
        extension = appProp.getProperty("extension").trim();
        password = appProp.getProperty("password").trim();
        codec = appProp.getProperty("codec", "g711U").trim();
        encryption = appProp.getProperty("encryption", "none").trim();
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
        provider = ServiceProvider.getServiceProvider(spProp);

        voiceSvcs = (VoiceUnitServices) provider.getService(
                ch.ecma.csta.voiceunit.VoiceUnitServices.class.getName());
        toneCollSvcs = (ToneCollectionServices) provider.getService(
                com.avaya.csta.tonecollection.ToneCollectionServices.class.getName());
        callAssocSvcs = (CallAssociatedServices) provider.getService(
                ch.ecma.csta.callassociated.CallAssociatedServices.class.getName());
        montSvcs = (MonitoringServices) provider.getService(
                ch.ecma.csta.monitor.MonitoringServices.class.getName());

        // Create new AvayaStation object.
        station = new AvayaStation();
        station.init(callServer, "", extension, provider, isMultiple);
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
                + " ext="
                + extension
                + " pw="
                + password);

        // Create a thread in whose context our cleanup will occur if the app
        // is
        // terminated. The Communication Manager API connector server code will
        // clean up if an app goes away unexpectedly, but it's still good to
        // clean up.
        shutdownThread = new MyShutdownThread();
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        // Get the device ID from the station class
        id = station.getDeviceID();

        // A connection ID is needed for requests to data services (tone
        // detection) and voice unit services (playing and recording files).
        // It is formulated from the device ID.
        connection = new ConnectionID();
        LocalDeviceID lid = new LocalDeviceID();
        lid.setStaticID(id);
        connection.setDeviceID(lid);

        // Add listeners to be notified of events coming from the various
        // services.
        addListeners();
        
        // Time to register the device. Pass in the password. Dependency mode is
        // is set to MAIN and media mode set to SERVER for this application.
        // Set up the codec and media encryption types based on the property file.
        // Register asynchronously since there's no reason to hold up this
        // thread waiting for the response.
        MediaInfo localMediaInfo = new MediaInfo();
        
        // check and set the optional codec settings
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
        
        // check and set the optional encryption settings
        if (Audio.SRTP_AES128_HMAC32_ENC_AUTH.equalsIgnoreCase(encryption) ||
        	Audio.SRTP_AES128_HMAC80_ENC_AUTH.equalsIgnoreCase(encryption) ||
        	Audio.SRTP_AES128_HMAC32_UNENC_AUTH.equalsIgnoreCase(encryption) ||
        	Audio.SRTP_AES128_HMAC80_UNENC_AUTH.equalsIgnoreCase(encryption)) {
        	System.out.println("This application does not support media encryption type="+encryption);
            removeListeners();
            station.cleanup();
            provider.disconnect(true);
            System.exit(1);
        } else if (Audio.SRTP_AES128_HMAC32_ENC_UNAUTH.equalsIgnoreCase(encryption))
            localMediaInfo.setEncryptionList(new String[] { Audio.SRTP_AES128_HMAC32_ENC_UNAUTH });
        else if (Audio.SRTP_AES128_HMAC80_ENC_UNAUTH.equalsIgnoreCase(encryption))
            localMediaInfo.setEncryptionList(new String[] { Audio.SRTP_AES128_HMAC80_ENC_UNAUTH });
        else if (Audio.SRTP_AES128_HMAC32_UNENC_UNAUTH.equalsIgnoreCase(encryption))
            localMediaInfo.setEncryptionList(new String[] { Audio.SRTP_AES128_HMAC32_UNENC_UNAUTH });
        else if (Audio.SRTP_AES128_HMAC80_UNENC_UNAUTH.equalsIgnoreCase(encryption))
            localMediaInfo.setEncryptionList(new String[] { Audio.SRTP_AES128_HMAC80_UNENC_UNAUTH });
        else if (Audio.AES.equalsIgnoreCase(encryption))
            localMediaInfo.setEncryptionList(new String[] { Audio.AES });
        else if (Audio.NOENCRYPTION.equalsIgnoreCase(encryption))
            localMediaInfo.setEncryptionList(new String[] { Audio.NOENCRYPTION });
        else // default
            localMediaInfo.setEncryptionList(new String[] {Audio.AES, Audio.NOENCRYPTION});
        
        System.out.println("setupAudio" +
            ": codec=" + localMediaInfo.getCodecs()[0] +
            ", encryption=" + localMediaInfo.getEncryptionList()[0]);
                                                                                

        station.register(password, DependencyMode.MAIN, MediaMode.SERVER, null, null, true,
                localMediaInfo, null, new MyAsyncRegistrationCallback());

        // That's all we do for now! The main thread is now going to go away,
        // and the rest of the action occurs as a result of events coming into
        // our listeners. Note that the Communication Manager API client code
        // has some persistent threads that keep the application alive. The
        // rest of the code executes in the context of these threads.
    }
        
    /**
     * Creates listener objects for the different services and adds them so the
     * app will be informed of events coming from the various services.
     */
    private void addListeners() throws CstaException
    { 
        // Add an AvayaStationListner to receive events indicating phone rings,
        // lamp and display updates.
        avayaStationListener = new MyAvayaStationListener();
        station.addListener(avayaStationListener);
        //montSvcs.addPhysicalDeviceListener(id, avayaStationListener);

        // Add a listener so we can receive events indicating when the phone is
        // registered or unregistered.
        registrationListener = new MyRegistrationListener();
        montSvcs.addRegistrationListener(id, registrationListener);
        
        // Add a listener to receive events indicating when announcements are
        // done being played and when recordings have completed.
        playAndRecordListener = new MyVoiceUnitListener();
        montSvcs.addVoiceUnitListener(id, playAndRecordListener);

        // Add a listener to receive events when touch tones are detected.
        toneCollListener = new MyToneCollectionListener();
        montSvcs.addToneCollectionListener(id, toneCollListener);

        // Add a listener to receive GenerateTelephonyTonesAbort.
        callAssociatedListener = new MyCallAssociatedListener();
    	callAssocSvcs.addCallAssociatedListener(callAssociatedListener, id);
    }

    /**
     * Removes all of the listeners that were added for the various services.
     */
    private void removeListeners() throws CstaException
    {
        station.removeListener(avayaStationListener);
        montSvcs.removeRegistrationListener(id, registrationListener);
        montSvcs.removeVoiceUnitListener(id, playAndRecordListener);
        montSvcs.removeToneCollectionListener(id, toneCollListener);
        callAssocSvcs.removeCallAssociatedListener(callAssociatedListener);
    }

    /**
     * Starts the tone collection service on the connector server so the app
     * will be notified when DTMF tones are detected. Note that the default
     * mode for DTMF tone collection on the connector server is out of band,
     * and that this must match the administration for DTMF tones on
     * Communication Manager or no tones will be collected. Check your
     * Communication Manager API documentation for more details on this topic.
     */
    private void startToneCollection()
    {
        ToneCollectionStart request = new ToneCollectionStart();
        ConnectionID connectionid = new ConnectionID();
        LocalDeviceID localId = new LocalDeviceID();
        localId.setStaticID(id);
        connectionid.setDeviceID(localId);

        CallObject object = new CallObject();
        object.setCall(connectionid);
        request.setObject(object);
        try
        {
            // Request that the connector server start tone collection for
            // this device.
            toneCollSvcs.startToneCollection(request);
        }
        catch (Exception e)
        {
            System.out.println("Could not start DTMF tone collection");
            e.printStackTrace(System.out);
        }
    }

    /**
     * Stops DTMF tone collection for this station.
     */
    private void stopToneCollection()
    {
        ToneCollectionStop request = new ToneCollectionStop();
        CallObject object = new CallObject();
        object.setDevice(id);
        request.setObject(object);
        try
        {
            toneCollSvcs.stopToneCollection(request);
        }
        catch (CstaException e)
        {
            System.out.println("Exception when stopping tone collection " + e);
        }
    }

    /**
     * Flushes the tone collection buffer so we can start fresh.
     */
    private void flushBuffer()
    {
        ToneCollectionFlushBuffer request = new ToneCollectionFlushBuffer();
        ConnectionID connectionid = new ConnectionID();
        LocalDeviceID localId = new LocalDeviceID();
        localId.setStaticID(id);
        connectionid.setDeviceID(localId);

        CallObject object = new CallObject();
        object.setCall(connectionid);
        request.setObject(object);
        request.setSendEvent(Boolean.FALSE);
        try
        {
            toneCollSvcs.flushBuffer(request);
        }
        catch (CstaException e)
        {
            System.out.println("Exception occurred while flushing buffer");
        }
    }

    /**
     * Sends a ToneRetrievalCriteria request with the indicated parameters. One
     * or more can be specified. If a criterion doesn't matter, it should be
     * set to null.
     *
     * @param flushChar - Character to be used to flush the buffer
     * @param numTones - Number of tones before flushing
     * @param timeout - timeout before flushing
     */
    private void setRetrievalCriteria(
        String flushChar,
        Integer numTones,
        Long timeout)
    {
        // Start collecting a dial string to use to transfer the call.
        ToneRetrievalCriteria criteria = new ToneRetrievalCriteria();
        ConnectionID connectionid = new ConnectionID();
        LocalDeviceID localId = new LocalDeviceID();
        localId.setStaticID(id);
        connectionid.setDeviceID(localId);

        CallObject object = new CallObject();
        object.setCall(connectionid);
        criteria.setObject(object);
        criteria.setFlushCharacter(flushChar);
        criteria.setNumberOfTones(numTones);
        criteria.setInitialTimeout(timeout);
        criteria.setInterdigitTimeout(timeout);
        try
        {
            toneCollSvcs.setToneRetrievalCriteria(criteria);
        }
        catch (CstaException e)
        {
            System.out.println(
                "Exception occurred while setting tone"
                    + "collection criteria"
                    + e);
        }
    }

    /**
     * Find a previously held call appearance and activate it. Also works to
     * retrieve call appearances that are not active because of a conference or
     * transfer.
     *
     * @returns The newly activated call appearance that was retrieved. Null if
     * no held call appearances are found.
     */
    public String findHeldCall() throws CstaException
    {
        Map<String, Short> callAppearanceLamps;
        String[] lampIDs;
        Short[] states;
        int i;
        boolean foundHeld = false;

        // Get arrays of the lamp IDs and corresponding states.
        callAppearanceLamps = station.getCallAppearanceStates();
        lampIDs = callAppearanceLamps.keySet().toArray(new String[0]);
        states = callAppearanceLamps.values().toArray(new Short[0]);

        // Loop through the states until we find one that is held (wink) or
        // initiated a transfer or conference (flutter).
        for (i = 0; i < states.length; i++)
        {
            if (states[i].equals(LampModeConstants.WINK)
                || states[i].equals(LampModeConstants.FLUTTER))
            {
                foundHeld = true;
                break;
            }
        }

        if (!foundHeld)
        {
            return null;
        }

        return lampIDs[i];
    }

    /**
     * Sends a stop request to VoiceUnitServices to have it stop all playing
     * and recording functions.
     *
     * @param fileName - the name of the Wave file that is to be stopped
     */
    private void stopPlayingAndRecording()
    {
        try
        {
            // Create a request and populate it with the required fields.
            Stop stopRequest = new Stop();
            stopRequest.setConnection(connection);

            // Send the request, and catch any resulting CstaExceptions.
            voiceSvcs.stop(stopRequest);
        }
        catch (CstaException e)
        {
            System.out.println("Exception stopping the player / recorder");
            e.printStackTrace(System.out);
        }
    }

    /**
     * Plays the specified announcement.
     *
     * @param announcementName - the name of the announcement Wave file that is
     * to be played.
     */
    private void playAnnouncement(String announcementName)
    {
        String[] announcementNames = new String[1];
        announcementNames[0] = announcementName;
        playAnnouncements(announcementNames);
    }

    /**
     * Plays the specified announcement, followed by the menu.
     *
     * @param announcementName - the name of the announcement Wave file that is
     * to be played.
     */
    private void playAnnouncementAndMenu(String announcementName)
    {
        String[] announcementNames = new String[2];
        announcementNames[0] = announcementName;
        announcementNames[1] = MENU;
        playAnnouncements(announcementNames);
    }

    /**
     * Plays the specified announcement, followed by a beep.
     *
     * @param announcementName - the name of the announcement Wave file that is
     * to be played.
     */
    private void playAnnouncementAndBeep(String announcementName)
    {
        String[] announcementNames = new String[2];
        announcementNames[0] = announcementName;
        announcementNames[1] = BEEP;
        playAnnouncements(announcementNames);
    }

    /**
     * Plays the announcements specified in the array of Strings.
     *
     * @param announcementNames - An array of Strings containing the names of
     * the announcements to be played.
     */
    private void playAnnouncements(String[] announcementNames)
    {
        stopPlayingAndRecording();

        PlayMessagePrivateData playPrivateData = new PlayMessagePrivateData();
        playPrivateData.setFileList(announcementNames);
        CSTACommonArguments commArgs =
            CSTACommonArguments.newSinglePrivateData(playPrivateData);

        // Create a new PlayMessage request, and populate it with the
        // required fields.
        PlayMessage playReq = new PlayMessage();
        playReq.setOverConnection(connection);
        playReq.setMessageToBePlayed("0");
        playReq.setExtensions(commArgs);
        try
        {
            // Send the request, and catch any resulting CstaExceptions.
            voiceSvcs.playMessage(playReq);
            for (int i = 0; i < announcementNames.length; i++)
            {
                System.out.println(
                    "Sent play request for " + announcementNames[i]);
            }
        }
        catch (CstaException e)
        {
            System.out.println("Could not start playing file or recording");
            e.printStackTrace(System.out);
        }
    }

    /**
     * Stops any recording that is currently happening, and starts a new one.
     */
    private void startRecording()
    {
        // If we're already recording, stop the recording
        stopPlayingAndRecording();

        deletePreviousMsg();

        // Start the recording. Create a request object, and populate it with
        // the connection ID.
        RecordMessage request = new RecordMessage();
        request.setCallToBeRecorded(connection);
        try
        {
            // Send the request, get the response, and catch any resulting
            // CstaExceptions.
            RecordMessageResponse response = voiceSvcs.recordMessage(request);

            // Save the name of the recording file for later. This is used to
            // stop the recording and replay the file to the user.
            currentRecordingFile = response.getResultingMessage();
            System.out.println(
                "Started recording: file=" + currentRecordingFile);
        }
        catch (CstaException e)
        {
            System.out.println("Could not start recording ");
            e.printStackTrace(System.out);
        }
    }

    /**
     * Sends a GenerateTelephonyTones request to CallAssociatedServices
     * to have CM inject a tone into the RTP stream.
     *
     */
    private boolean generateRecordingWarningTone()
    {
        boolean result = false;
        
        if (!isRecordingToneSet) {
            try
            {
                // Create a request and populate it with the required fields.
                GenerateTelephonyTones request = new GenerateTelephonyTones();
                request.setConnectionToSendTone(connection);
                request.setToneToSend(TelephonyTone.RECORDWARNING);
                request.setToneDuration(0L);

                // Send the request, and catch any resulting CstaExceptions.
                GenerateTelephonyTonesResponse resp = callAssocSvcs.generateTelephonyTones(request);
                isRecordingToneSet = true;
                result = true;
            }
            catch (CstaException e)
            {
                System.out.println("Exception when sending GenerateTelephonyTones. "+e);
                e.printStackTrace(System.out);
            }
        }
        
        
        return result;
    }

    /**
     * Sends a CancelTelephonyTones request to CallAssociatedServices
     * to have CM stop injecting a tone into the RTP stream.
     *
     */
    private boolean cancelRecordingWarningTone()
    {
        boolean result = false;
        
        if (isRecordingToneSet) {
        	try
            {
                // Create a request and populate it with the required fields.
                CancelTelephonyTones request = new CancelTelephonyTones();
                request.setConnectionToStopTone(connection);

                // Send the request, and catch any resulting CstaExceptions.
                CancelTelephonyTonesResponse resp = callAssocSvcs.cancelTelephonyTones(request);
                isRecordingToneSet = false;
                result = true;
            }
            catch (CstaException e)
            {
                System.out.println("Exception when sending CancelTelephonyTones. "+e);
                e.printStackTrace(System.out);
            }
        }
        
        
        return result;
    }

    /**
     * This method is called before recording a file and at cleanup. It deletes
     * any previously recorded file.
     */
    private void deletePreviousMsg()
    {
        // If any file has been previously recorded, delete it.
        if (currentRecordingFile != null)
        {
            DeleteMessage msgDelRequest = new DeleteMessage();
            msgDelRequest.setMessageToBeDeleted(currentRecordingFile);

            try
            {
                DeleteMessageResponse msgDelResponse =
                    voiceSvcs.deleteMessage(msgDelRequest);
            }
            catch (CstaException e)
            {
                System.out.println(
                    "An exception occured while trying to delete previously "
                        + "recorded file: -"
                        + currentRecordingFile);
                e.printStackTrace();
            }
        }
    }

    /**
     * This cleanup method assures us that we unregister our extension, remove
     * all the listeners, stop collecting DTMF tones and stop any announcements
     * or recordings that were happening. The connector server will cleanup if
     * a client goes away without doing this cleanup, but it's best to be safe.
     */
    private void cleanup()
    {
        System.out.println("The application is terminating: clean up.");

        // There is a chance that AvayaStation has already cleaned up as a
        // result of getting the unregistered event. In this case, the station
        // is already unregistered, the device ID has been released, and the
        // server should have cleaned up. Don't bother doing anything else.
        if (station.getDeviceID() == null)
        {
            return;
        }

        // Stop any announcements or recordings that are currently in progress.
        stopPlayingAndRecording();

        try
        {
        	System.out.println("CLEANING UP");
        	
            // Delete any files recorded.
            deletePreviousMsg();

            // Make sure no listeners were left around.
            removeListeners();
            stopToneCollection();
            
            // Call the synchronous version of unregister() because we want the
            // unregister to succeed before cleaning up and disconnecting.
            if(isMultiple) {
                try {
                    station.cleanup();
                } catch(InvalidDeviceStateException idse) {
                    station.unregister();
                    station.cleanup();                    
                }
            }
            else {
                station.unregister();
                station.cleanup();
            }
                      
            // provider.disconnect will disconnect from the server and
            // terminate the active session.
            provider.disconnect(true);
        }
        catch (Exception e)
        {
            System.out.println("Could not clean up properly");
            e.printStackTrace(System.out);
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
        /**
         * Method is fired when the a Display update is received
         *
         * @param event the DisplayUpdatedEvent
         */
        public void displayUpdated(DisplayUpdatedEvent event)
        {
            System.out.println(
                "Display update event: " + event.getContentsOfDisplay());
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
                System.out.println("Lamp " + lampID + " is now flashing.");
                try
                {
                    station.answerCall(lampID);
                }
                catch (CstaException e)
                {
                    System.out.println("Couldn't answer the phone" + e);
                }
                System.out.println("The phone was answered");
            }
            else if (LampModeConstants.STEADY.equals(lampValue))
            {
                // A steady green call appearance lamp indicates that the call
                // is now established. We can play our greeting.
                System.out.println("Lamp " + lampID + " is now on.");
                activeCallAppearance = lampID;

                if ((CONFERENCE == confOrXferState)
                    || (TRANSFER == confOrXferState))
                {
                    // We were in the middle of a conference or transfer. This
                    // is the second call appearance becoming active. No need
                    // to play the greeting again.
                    System.out.println("Conference or transfer initiated");
                }
                else if (BAD_DROPPED == confOrXferState)
                {
                    System.out.println(
                        "Back to primary call appearance "
                            + "after failed conference or transfer");
                    confOrXferState = NONE;
                    // We recovered from a bad number being entered for a
                    // conference or a transfer. Play a greeting that tells
                    // them so, then back to the menu.
                    playAnnouncementAndMenu(BAD_EXT);
                    // Set collection criteria, returning results after one
                    // digit
                    setRetrievalCriteria(null, new Integer(1), null);
                }
                else
                {
                    System.out.println("The call has been established.");
                    playAnnouncementAndMenu(GREETING);
                    // Set collection criteria, returning results after one
                    // digit
                    setRetrievalCriteria(null, new Integer(1), null);
                }
            }
            else if (LampModeConstants.BROKENFLUTTER.equals(lampValue))
            {
                System.out.println("Lamp " + lampID + " is in broken flutter.");
                // A conference went bad! Drop this line appearance and return
                // to the previous one.
                System.out.println("A bad extension number was entered.");
                confOrXferState = BAD_CONF_XFER;
                try
                {
                    station.disconnect();
                    activeCallAppearance = findHeldCall();
                    station.retrieve(activeCallAppearance);
                }
                catch (CstaException e)
                {
                    System.out.println(
                        "Exception when attemping to retrieve"
                            + "call appearance: "
                            + e);
                }
            }
            else if (LampModeConstants.OFF.equals(lampValue))
            {
                System.out.println("Lamp " + lampID + " is now off.");
                if (activeCallAppearance != null
                    && lampID.equals(activeCallAppearance))
                {

                    // If this lamp ID matches that of the active call
                    // appearance, the far end went on hook, and we can hang up.
                    System.out.println("The other person hung up");
                    activeCallAppearance = null;

                    // Stop any announcements or recordings that are currently
                    // in progress.
                    needToRecord = false;
                    stopPlayingAndRecording();
                    flushBuffer();

                    station.disconnect();
                    System.out.println("Hung up the phone");

                }
                else if ((CONFERENCE == confOrXferState)
                         || (TRANSFER == confOrXferState))
                {
                    // If we're in the middle of initiating a conference or
                    // transfer, ignore this. It's just the initial call
                    // appearance going dark.
                    System.out.println("Conference or transfer completed.");
                    confOrXferState = NONE;
                }
                else if (BAD_CONF_XFER == confOrXferState)
                {
                    System.out.println(
                        "Dropped bad conference or transfer attempt.");
                    confOrXferState = BAD_DROPPED;
                }
            }
            else if (LampModeConstants.FLUTTER.equals(lampValue))
            {
                System.out.println("Lamp " + lampID + " is fluttering.");
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
                System.out.println("The ringer is off.");
            }
            else
            {
                System.out.println("The ringer is on.");
            }
        }
    }

    /**
     * This is the tone collection listener class.
     */
    private class MyToneCollectionListener implements ToneCollectionListener
    {
      /** Note: All the responses/events are delivered on one thread. This guarantees
   	    * sequential processing of events/responses. However if the processing of an
        * event involves an I/O as this function does, you may
   	    * want to process this event in a different thread.
   	    * One approach would be to have one separate thread processing events and responses
   	    * off of a queue. All Listeners would queue events/responses to this queue if 
   	    * events/responses are either blocking or require sequential processing.
        */
    	public void tonesRetrieved(TonesRetrievedEvent event)
        {

            // This application only sets the number of tones in the retrieval
            // criteria to one, so we know that if the retrieval cause was
            // CHARCOUNTRECEIVED, there is only a single digit in the buffer.
            if (event.getTcollcause() == TcollCause.CHARCOUNTRECEIVED)
            {
                String digit = event.getTones();
                System.out.println("A digit was collected=" + digit);

                // Do different things based on the digit that was received.
                if ("1".equals(digit))
                {

                    playAnnouncementAndMenu(FACT);
                    // Set collection criteria: return results after one digit
                    setRetrievalCriteria(null, new Integer(1), null);

                }
                else if ("2".equals(digit))
                {

                    // Tell the user that the recording will start right after
                    // this announcement. Since startRecording is set to true,
                    // the recording is actually started when the stopped even
                    // is received for this announcement.
                    needToRecord = true;
                    playAnnouncementAndBeep(RECORDING_STARTED);
                    // Set collection criteria: return results after one digit
                    setRetrievalCriteria(null, new Integer(1), null);

                }
                else if ("#".equals(digit))
                {

                    // Stop recording the file. When the "stopped" event is
                    // received for the recording, the play will begin.
                    stopPlayingAndRecording();
                    // Set collection criteria: return results after one digit
                    setRetrievalCriteria(null, new Integer(1), null);

                }
                else if ("3".equals(digit))
                {

                    if (null == currentRecordingFile)
                    {
                        playAnnouncementAndMenu(NOFILE);
                    }
                    else
                    {
                        playAnnouncementAndMenu(currentRecordingFile + ".wav");
                    }
                    // Set collection criteria: return results after one digit
                    setRetrievalCriteria(null, new Integer(1), null);

                }
                else if ("4".equals(digit))
                {

                    playAnnouncement(XFER_MSG);
                    confOrXferState = TRANSFER;
                    // This allows us to retrieve a dial string, terminated by
                    // a pound sign.
                    setRetrievalCriteria("#", null, null);

                }
                else if ("5".equals(digit))
                {

                    playAnnouncement(CONF_MSG);
                    confOrXferState = CONFERENCE;
                    // This allows us to retrieve a dial string, terminated by
                    // a pound sign.
                    setRetrievalCriteria("#", null, null);

                }
                else if ("6".equals(digit))
                {

                    if (generateRecordingWarningTone()) {
	                    // Tell the user that the Recording Warning Tone
	                	// is now activated. Note that the CM won't actually
                    	// generate the tone until the next call - that is
                    	// until the user hangs up and redials the
                    	// SimpleIVR application.
	                	playAnnouncementAndMenu(RECORD_WARN_ON);
                    }
                    else
                    {
                        // Tell the user that the Recording Warning Tone
                    	// can't be activated
	                	playAnnouncementAndMenu(RECORD_WARN_ON_FAIL);
                    }
                    setRetrievalCriteria(null, new Integer(1), null);

                }
                else if ("7".equals(digit))
                {

                    if (cancelRecordingWarningTone()) {
	                    // Tell the user that the Recording Warning Tone
	                	// is now cancelled. Note that the CM won't actually
                    	// cancel the tone until the current call ends.
	                	playAnnouncementAndMenu(RECORD_WARN_OFF);
                    }
                    else
                    {
                        // Tell the user that the Recording Warning Tone
                    	// can't be cancelled
	                	playAnnouncementAndMenu(RECORD_WARN_OFF_FAIL);
                    }
                    setRetrievalCriteria(null, new Integer(1), null);

                }
                else
                {

                    System.out.println("Invalid digit detected: " + digit);
                    // Tell the user s/he pressed a digit not on menu and
                    // repeat the menu.
                    playAnnouncementAndMenu(INVALID_KEY_PRESS);
                    setRetrievalCriteria(null, new Integer(1), null);

                }
            }
            else if (event.getTcollcause() == TcollCause.FLUSHCHARRECEIVED)
            {
                try
                {
                    // Don't strip off the terminating # sign. If Communication
                    // Manager is configured with a dial plan that has varying
                    // length extensions, the # can be used to signal the end
                    // of a shorter extension.
                    String dialString = event.getTones();
                    dialString = dialString.substring(0, dialString.length());
                    // Check to see if we were doing a conference or a
                    // transfer.
                    if (TRANSFER == confOrXferState)
                    {
                        station.transfer(dialString);
                    }
                    else if (CONFERENCE == confOrXferState)
                    {
                        station.conference(dialString);
                        // After completing the conference, return to the menu.
                        playAnnouncementAndMenu(CONF_GREET);
                        // Don't change conferenceOrTransfer, the conference is
                        // not yet complete.
                    }
                    setRetrievalCriteria(null, new Integer(1), null);
                }
                catch (CstaException e)
                {
                    System.out.println(
                        "Exception occurred while attempting "
                            + "a conference or transfer"
                            + e);
                }
            }
        }
    }

    /**
     * This is the voice unit listener class. It extends VoiceUnitAdapter
     * rather than implementing the VoiceUnitListener interface. This allows us
     * to not implement all of the methods in VoiceUnitListener.
     */
    private class MyVoiceUnitListener extends VoiceUnitAdapter
    {
        public void playing(PlayEvent event)
        {
            // If we got to here, then the filename is alphanumeric. This
            // application only uses alphanumeric filenames for play, not for
            // record.
            CSTACommonArguments commArgs = event.getExtensions();
            PlayMessagePrivateData playPrivateData =
                (PlayMessagePrivateData) commArgs.getSinglePrivateData();

            System.out.println(
                "Started playing file " + playPrivateData.getPlayMessage());
        }

        public void recording(RecordEvent event)
        {
            System.out.println(
                "Record event received for connection ID: "
                    + event.getConnection());
        }

        /**
         * This is the only event we actually use from VoiceUnitServices. This
         * event is used for the following reasons:
         *  1) When the announcement for the start of the recording is done
         * playing, the recording will begin.
         *  2) When a recording has stopped, that recording will be played
         * back.
         * Note: All the responses/events are delivered on one thread. This guarantees
	     * sequential processing of events/responses. However if the processing of an
         * event involves an I/O as this function does, you may
	     * want to process this event in a different thread.
	     * One approach would be to have one separate thread processing events and responses
	     * off of a queue. All Listeners would queue events/responses to this queue if 
	     * events/responses are either blocking or require sequential processing.
         */
        public void stopped(StopEvent event)
        {
            String fileName = event.getMessage();
            String[] fileList;

            // Are we done recording?
            if (fileName.equals(currentRecordingFile))
            {
                System.out.println(
                    "The file " + fileName + " is done being recorded");

                // The recording is done! Let's play it back now.
                playAnnouncementAndMenu(currentRecordingFile + ".wav");
                return;
            }
            else if (!("0".equals(fileName)))
            {
                // All play files have alphanumeric filenames, so if it's not
                // a recording, then the message field should be "0".
                System.out.println(
                    "Unexpected filename received in stopped "
                        + "event: "
                        + fileName);
                return;
            }

            // If we got to here, then the filename is alphanumeric. This
            // application only uses alphanumeric filenames for play, not for
            // record.
            CSTACommonArguments commArgs = event.getExtensions();
            Object privateData = commArgs.getSinglePrivateData();
            if (privateData instanceof PlayMessagePrivateData)
            {
                PlayMessagePrivateData playPrivateData =
                    (PlayMessagePrivateData) privateData;
                fileList = playPrivateData.getFileList();
            }
            else
            {
                System.out.println(
                    "Received unexpected private data type in "
                        + "stopped event.");
                return;
            }

            for (int i = 0; i < fileList.length; i++)
            {
                System.out.println(
                    "The file " + fileList[i] + " is done being played.");
            }

            // Check to see if we need to start a recording now. We'll
            // only do that if we just finished playing the beep.
            if (needToRecord && fileList[fileList.length - 1].equals(BEEP))
            {
                // Time to start the recording
                startRecording();
                needToRecord = false;
            }
        }
    }

    /** 
     * This is the Asynchronous Services call back for RegistrationServices
     * 
	 */
    private class MyAsyncRegistrationCallback implements AsynchronousCallback
	{
		/** 
		 * Handle the asynchronous response from a registration request
		 * @param response
		 *  		the response to a registration request
		 */
		public void handleResponse(Object response){			
			if(response.getClass().getName().equals(RegisterTerminalResponse.class.getName())){
				RegisterTerminalResponse regResp = (RegisterTerminalResponse) response;	
				if (regResp.getCode().equals(RegistrationConstants.NORMAL_REGISTER)){
					System.out.println("SimpleIVR: calling registered");
					registered(regResp);				
				} else {
					System.out.println("SimpleIVR: calling register failed");
					registerFailed(regResp);
				}
            } else {
                System.out.println("This is bad. Asynchronous response is not of valid type: " + response);
            }
				
		}

		/**
		 * Handle the Exception from a registration request
		 * 
		 * @param exception
		 * 				An Asynchronous Exception thrown in response to a registration request
		 */
		public void handleException(Throwable exception) {			
        	System.out.println("Received Asynchronous Exception from Server. The stack trace from the Server is as follows");
        	exception.printStackTrace();
			cleanup();        
            System.exit(0);
		}
		
		/**
	     * This method is called when the extension has successfully been
	     * registered with Communication Manager. The only action that is taken
	     * is a scan of the buttons on the phone to determine which ones are
	     * call appearance buttons. These are used later on to determine when
	     * the call has been established, and when we need to hang up.
	     * 
	     * The only interesting piece of data in the registered event is the
	     * extension number of the device that was registered. Since this
	     * application only registers one extension, there is no need to
	     * examine the contents of this message.
	     * 
	     * @param resp
	     *            the RegisteredTerminalResponse that contains information 
	     * 			  about the registration.
	     * 
         * Note: All the responses/events are delivered on one thread. This guarantees
	     * sequential processing of events/responses. However if the processing of an
	     * response involves an I/O as this function does, you may
	     * want to process this response in a different thread.
	     * One approach would be to have one separate thread processing events and responses
	     * off of a queue. All Listeners would queue events/responses to this queue if 
	     * events/responses are either blocking or require sequential processing.
	     */

	    private void registered(RegisterTerminalResponse resp)
	    {
			System.out.println("SimpleIVR: signaling encryption="+resp.getSignalingEncryption());
			
            // Request that the connector server start tone collection for
            // this device.
            startToneCollection();
            
	    	station.getButtonInfo();
	    	
            // Use new-line characters in these println statements to make the
            // instructions in the second println stand out.
            System.out.println(
                "The phone is now registered and waiting for calls.\n");

            System.out.println(
                "DIAL "
                    + extension
                    + " FROM ANOTHER PHONE TO ACCESS THE APPLICATION.\n");
            
	    }
	    
	    
	    /**
	     * This method is called if registration failed. Possible causes of
	     * this are:
	     * 
	     * No network connectivity to Communication Manager: Make sure that
	     * network connectivity exists between the connector server and the
	     * Communication Manager IP interface.
	     * 
	     * Wrong extension, wrong password: Make sure that the extension number
	     * that is administered in the application.properties file matches an
	     * extension number that is administered on Communication Manager. Make
	     * sure that the password matches that administered for the extension.
	     * Make sure that the "Softphone?" field is set to "y" for that
	     * extension.
	     * 
	     * @param resp
	     *            the RegisterTerminalResponse
	     */
        private void registerFailed(RegisterTerminalResponse resp)
        {
            try
            {
                System.out.println("Registration failed: reason " + resp.getReason());
                // Make sure no listeners were left around.
                removeListeners();
                station.cleanup();
            }
            catch (CstaException e)
            {
                System.out.println(
                    "Exception when removing listeners or "
                        + "releasing device ID "
                        + e);
            }
            
            System.exit(0);
        }
	}	
 
    /**
     * This is the Registration listener class. It extends RegistrationAdapter rather
     * than implementing the RegistrationListener interface. This allows us to not
     * implement all of the methods in RegistrationListener.
     */
    private class MyRegistrationListener extends RegistrationAdapter
    {

        /**
         * This method is called if the extension has become unregistered with
         * Communication Manager. This may have occurred as a result of an
         * unregistration request from the application, or Communication
         * Manager may have unregistered the extension for other reasons.
         * 
         * @param event
         *            the TerminalUnregisteredEvent
         */
        public void terminalUnregistered(TerminalUnregisteredEvent event)
        {
            try
            {
                System.out.println("Received unregistered event: reason " +
                        event.getReason());
                System.out.println("Terminating application");
                // Make sure no listeners were left around.
                removeListeners();
                station.cleanup();                          
                System.exit(0);
            }
            catch (CstaException e)
            {
                System.out.println(
                    "Exception when removing listeners or "
                        + "releasing device ID "
                        + e);
            }
        }
    }    
    
    /**
     * This is the Registration listener class. It extends RegistrationAdapter rather
     * than implementing the RegistrationListener interface. This allows us to not
     * implement all of the methods in RegistrationListener.
     */
    private class MyCallAssociatedListener implements CallAssociatedListener
    {
        /**
         * This method is called if the recording warning tone is no longer being
         * generated by CM. This may have occurred as a result of a fail-over of CM.
         * 
         * @param event
         *            the GenerateTelephonyTonesAbort
         */
        public void toneGenerationFailed(GenerateTelephonyTonesAbort event)
        {
            System.out.println("Received telephony tone failed event: reason-code=" +
                    event.getFailedToneResponse());
            
            // Attempt to re-generate the tone.
            generateRecordingWarningTone();
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
            cleanup();
        }
    }
}
