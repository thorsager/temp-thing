/*
 * TutorialApp.java
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

package sampleapps.tutorial;

// Java utilities
import java.net.URL;
import java.util.Properties;

import sampleapps.station.AvayaStation;
import sampleapps.station.AvayaStationAdapter;
import ch.ecma.csta.binding.CSTAErrorCode;
import ch.ecma.csta.binding.ConnectionID;
import ch.ecma.csta.binding.DeleteMessage;
import ch.ecma.csta.binding.DeviceID;
import ch.ecma.csta.binding.DisplayUpdatedEvent;
import ch.ecma.csta.binding.HookswitchEvent;
import ch.ecma.csta.binding.LampModeEvent;
import ch.ecma.csta.binding.LocalDeviceID;
import ch.ecma.csta.binding.PlayEvent;
import ch.ecma.csta.binding.PlayMessage;
import ch.ecma.csta.binding.RecordEvent;
import ch.ecma.csta.binding.RecordMessage;
import ch.ecma.csta.binding.RecordMessageResponse;
import ch.ecma.csta.binding.RingerStatusEvent;
import ch.ecma.csta.binding.Stop;
import ch.ecma.csta.binding.StopEvent;
import ch.ecma.csta.errors.CstaException;
import ch.ecma.csta.monitor.MonitoringServices;
import ch.ecma.csta.voiceunit.VoiceUnitAdapter;
import ch.ecma.csta.voiceunit.VoiceUnitServices;

import com.avaya.api.media.audio.Audio;
import com.avaya.api.sessionsvc.exceptions.SessionCleanedUpException;
import com.avaya.cmapi.APIProtocolVersion;
import com.avaya.cmapi.ServiceProvider;
import com.avaya.cmapi.ServiceProviderListener;
import com.avaya.cmapi.evt.ServerConnectionDownEvent;
import com.avaya.cmapi.evt.ServerSessionNotActiveEvent;
import com.avaya.cmapi.evt.ServerSessionTerminatedEvent;
import com.avaya.csta.async.AsynchronousCallback;
import com.avaya.csta.binding.MediaInfo;
import com.avaya.csta.binding.RegisterTerminalResponse;
import com.avaya.csta.binding.TerminalUnregisteredEvent;
import com.avaya.csta.binding.ToneDetectedEvent;
import com.avaya.csta.binding.types.DependencyMode;
import com.avaya.csta.binding.types.MediaMode;
import com.avaya.csta.physical.LampModeConstants;
import com.avaya.csta.physical.RingerPatternConstants;
import com.avaya.csta.registration.RegistrationAdapter;
import com.avaya.csta.registration.RegistrationConstants;
import com.avaya.csta.tonedetection.ToneDetectionListener;
import com.avaya.mvcs.framework.CmapiKeys;

/**
 * Purpose: This tutorial application demonstrates how to write a simple Interactive
 * Voice Response (IVR) application using the Avaya Communication Manager API.
 * It registers an extension with Communication Manager, and waits for that
 * extension to be dialed. When a user dials the associated extension number,
 * the application answers the incoming call, and plays a greeting to the user.
 * The user can then press 1 record a message and have it played back to them.
 * All announcements / functions can be preempted by another button press. The
 * user can hang up and dial back into the application extension repeatedly.
 *
 * Registration is done synchronously using Registration Services.
 * 
 * @author AVAYA, Inc.
 * @version $Revision: 1.20 $
 */
@SuppressWarnings("unused")
public class TutorialApp
{

    // These fields are read in from the application.properties file. This file
    // must be populated with a valid IP address for Avaya Communication
    // Manager, as well as a valid extension number and password for the
    // application softphone.
    private String callServer=null;
    private String extension;
    private String password;
    private String codec;
    private String encryption;
    private boolean isMultiple = false;
    boolean getButtonInfo=true;
    MediaInfo localMediaInfo = null;
    
    // The Device ID for our extension number will be supplied by the
    // Communication Manager API. The Connection ID is formulated from the
    // Device ID.
    private DeviceID id = null;
    private ConnectionID connection = null;

    // These fields are used to track the name of the currently recording file
    // and to track the active call appearance. The fields are set to null when
    // that action is not currently taking place.
    private String currentRecordingFile = null;

    // Set to true if we are done playing the recording
    private boolean ifDonePlaybackRecording = false;
    
    // Set to true if the station is onhook
    private boolean onHook = true;

    // This thread is used to receive a shutdown notification when the
    // application is terminated.
    private MyShutdownThread shutdownThread;

    // The Service Provider used to get the handles of various services.
    private ServiceProvider provider;
    private ServiceProviderListener serviceProviderListener;


    // These fields will be populated with the handles of the various services,
    // which we will get through the ServiceProvider class.
    private VoiceUnitServices voiceSvcs;
    private MonitoringServices montSvcs;

    private Properties reconnectProps;
    
    private AvayaStation station;
    // MonitoringServices will be used to add/remove voice unit listener and
    // tone detection listener.
    // These are the listeners that will be added to listen for events coming
    // from VoiceUnitServices, ToneCollectionServices, TerminalServices, and
    // PhysicalDeviceServices.
    private MyVoiceUnitListener playAndRecordListener;
    private MyToneDetectionListener toneDetectListener;
    private MyRegistrationListener registrationListener;
    private MyAvayaStationListener avayaStationListener;

    private static final String PROP_FILE_CALL_SERVER = "callserver";
    private static final String PROP_FILE_EXTENSION = "extension";
    private static final String PROP_FILE_EXT_PASSWORD = "password";
    private static final String PROP_FILE_MEDIA_CODEC = "codec";
    private static final String PROP_FILE_MEDIA_ENCRYPTION = "encryption";
    private static final String PROP_FILE_CMAPI_SERVER = "cmapi1.server_ip";
    private static final String PROP_FILE_USERNAME = "cmapi1.username";
    private static final String PROP_FILE_PASSWORD = "cmapi1.password";
    private static final String PROP_FILE_SERVER_PORT ="cmapi1.server_port";
    private static final String PROP_FILE_SECURE ="cmapi1.secure";
    private static final String GREETING = "0001";
    private static final String PROMPT = "0002";
    private static final String RECORD = "0003";
    private static final String BEEP = "0004";
    private static final String INVALID_KEY_PRESS = "0005";


    /**
     * Creates an instance of the TutorialApp class, bootstraps it, then starts
     * the application.
     */
    public static void main(String[] args)
    {

        TutorialApp app = new TutorialApp();
        try
        {
            app.bootstrap();
            
        }catch (Exception e)
        {
            System.out.println("Could not read properties file");
            e.printStackTrace(System.out);
            System.exit(0);
        }

        try
        {
            app.start();
        }
        catch (CstaException e)
        {
            System.out.println("Could not start the tutorial app");
            e.printStackTrace(System.out);
            System.exit(0);
        }
        catch (Exception e)
        {
            System.out.println("Could not start the tutorial app");
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

    void bootstrap() throws Exception
    {
        String tmp;
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
        String cmapiCustSSLClientCTXFactory;

        // Get all of the arguments from the application properties file
        ClassLoader cl = TutorialApp.class.getClassLoader();
        URL appURL = cl.getResource("tutorial.properties");
        Properties appProp = new Properties();
        appProp.load(appURL.openStream());
        
        tmp = appProp.getProperty(PROP_FILE_CALL_SERVER);
        if(tmp != null)
        {
            callServer = tmp.trim();
        }
        
        tmp = appProp.getProperty(PROP_FILE_EXTENSION);
        if(tmp !=null)
        {
            extension = tmp.trim();
        }
            
        tmp = appProp.getProperty("getButtonInfo");
        if(tmp != null && "false".equals(tmp.trim()))
            getButtonInfo = false;

        password = appProp.getProperty(PROP_FILE_EXT_PASSWORD).trim();
        codec = appProp.getProperty(PROP_FILE_MEDIA_CODEC, "g711U").trim();
        encryption = appProp.getProperty(PROP_FILE_MEDIA_ENCRYPTION, "none").trim();

        cmapiServerIp = appProp.getProperty(PROP_FILE_CMAPI_SERVER).trim();
        cmapiUsername = appProp.getProperty(PROP_FILE_USERNAME).trim();
        cmapiPassword = appProp.getProperty(PROP_FILE_PASSWORD).trim();
        // If there is no entry then we will assume the connection is secure.
        cmapiServerPort= appProp.getProperty(PROP_FILE_SERVER_PORT, "4722").trim();
        cmapiSecure= appProp.getProperty(PROP_FILE_SECURE, "true").trim();
        cmapiTrustStoreLocation = appProp.getProperty(CmapiKeys.TRUST_STORE_LOCATION);
        
        cmapiCustSSLClientCTXFactory = appProp.getProperty(CmapiKeys.CUSTOM_CLIENT_SSLCONTEXTFACTORY);

        //add following properties for validation client and service side certifications 
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

        //  add following properties for validation client and service side certifications 
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
        if(cmapiCustSSLClientCTXFactory != null){
        	spProp.setProperty(CmapiKeys.CUSTOM_CLIENT_SSLCONTEXTFACTORY, cmapiCustSSLClientCTXFactory.trim());
        }


        // Get a handle to the ServiceProvider class.
        provider = ServiceProvider.getServiceProvider(spProp);
        reconnectProps = spProp;

        voiceSvcs =
            (VoiceUnitServices) provider.getService(
                    ch.ecma.csta.voiceunit.VoiceUnitServices.class.getName());

        montSvcs = (MonitoringServices) provider.getService(
                ch.ecma.csta.monitor.MonitoringServices.class.getName());

        // Create a thread in whose context our cleanup will occur if the app
        // is terminated. The Communication Manager API connector server code
        // will clean up if an app goes away unexpectedly, but it's still good
        // to clean up.
        shutdownThread = new TutorialApp.MyShutdownThread();
        Runtime.getRuntime().addShutdownHook(shutdownThread);
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
        
        // Create new AvayaStation object.
        station = new AvayaStation();
        station.init(callServer, "", extension, provider);
        
        System.out.println(
            "\n\nTutorialApp Startup using switch="
                + callServer
                + " ext="
                + extension
                + " pw="
                + password + " startTime=" + System.currentTimeMillis());

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

        // Time to register the device. Pass in the password. Shared control
        // is set to false for this application. You can use the default media settings
        // (media goes to connector server over G.711 codec with no encryption), or you
        // can explicitly specify the codecs and encryption for the MediaInfo
        localMediaInfo = new MediaInfo();
        
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
    private void addListeners() throws CstaException, Exception
    {
        // Add an AvayaStationListner to receive events indicating phone rings,
        // lamp and display updates.
        avayaStationListener = new MyAvayaStationListener();
        station.addListener(avayaStationListener);

        // Add a listener so we can receive events indicating when the phone is
        // registered / unregistered.
        registrationListener = new MyRegistrationListener();
        montSvcs.addRegistrationListener(id, registrationListener);

        // Add a listener to receive events indicating when announcements are
        // done being played and when recordings have completed.
        playAndRecordListener = new MyVoiceUnitListener();
        montSvcs.addVoiceUnitListener(id, playAndRecordListener);

        // Add a listener to receive events when touch tones are detected.
        toneDetectListener = new MyToneDetectionListener();
        montSvcs.addToneDetectionListener(id, toneDetectListener);

        // Add a service provider listener to receive events indicating
        // the state of the server connection or session.  
        serviceProviderListener = new MyServiceProviderListener();
        provider.addServiceProviderListener(serviceProviderListener);  
    }

    /**
     * Removes all of the listeners that were added for the various services.
     */
    private void removeListeners() throws CstaException
    {
        station.removeListener(avayaStationListener);
        montSvcs.removeRegistrationListener(id, registrationListener);
        montSvcs.removeVoiceUnitListener(id, playAndRecordListener);
        montSvcs.removeToneDetectionListener(id, toneDetectListener);
        try {
            provider.removeServiceProviderListener(serviceProviderListener); 
        } catch(Exception e) {}
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

            System.out.println("Sending stop request");
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
     * Plays a message.
     *
     * @param messageName - The name of message file.
     */
    private void playMessage(String messageName)
    {
        // Stop playing / recording currently in progress.
        stopPlayingAndRecording();

        // Create a PlayMessage object, set required fields and request voice
        // services to play the message.
        PlayMessage playMessageRequest = new PlayMessage();
        playMessageRequest.setMessageToBePlayed(messageName);
        playMessageRequest.setOverConnection(connection);

        try
        {
            voiceSvcs.playMessage(playMessageRequest);
        }
        catch (CstaException cstae)
        {
            System.out.println("Error playing message " + cstae);
        }
    }

    /**
     * Stops any recording that is currently happening, and starts a new one.
     */
    private void startRecording()
    {
        // If we're already recording, stop the recording
        stopPlayingAndRecording();

        // Delete any previously recorded message.
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
                voiceSvcs.deleteMessage(msgDelRequest);
            }
            catch (CstaException e1)
            {
                System.out.println(
                    "An exception occured while trying to delete previously "
                        + "recorded file: -"
                        + currentRecordingFile);
                e1.printStackTrace();
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

        // Stop any announcements or recordings that are currently in progress.
        stopPlayingAndRecording();

        try
        {
            // Delete any recorded message.
            deletePreviousMsg();

            // Make sure no listeners were left around.
            removeListeners();

            // Call the synchronous version of unregister() because we want the
            // unregister to succeed before cleaning up and disconnecting.
            station.unregister();
            station.cleanup();
        }

        catch (Exception e)
        {
            System.out.println("Could not clean up properly");
            e.printStackTrace(System.out);
        }

        try{
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
     * Method recoverFromTerminatedSession
     * will do all of the required clean up and re-initialization to allow
     * the application to create a new session and continue running. This
     * method can only be called if bootstrap() has already been called.
     * 
     * @throws Exception
     *             if the bootstrap() was not previously called.
     */
    public void recoverFromTerminatedSession() throws Exception
    {
        if (reconnectProps != null)
        {                   
            // Get a handle to the ServiceProvider class.
            provider = ServiceProvider.getServiceProvider(reconnectProps);

            // Re-initialize the station obj.
            station = null;
             
            station = new AvayaStation();
            station.init(callServer, "", extension, provider);
            
            // The first thing that we do is to get a Device ID. The Device ID
            // is used in the requests to the services. Calling this method
            // populates the id field of the Click2Call class.
            id = station.getDeviceID();

            // Add appropriate listeners to listen to events during and after
            // registration.
            addListeners();

            // Registers an extension in shared control mode for use as a
            // CMAPI softphone station (IP_API_A) with Communication Mananger
            // and the connector server. Setting the second parameter to true
            // allows for shared control.
            station.register(password, DependencyMode.MAIN, MediaMode.SERVER, null, null, true,
                    localMediaInfo, null, new MyAsyncRegistrationCallback());
        }    
        else
        {
            throw new Exception("bootstrap() must be called before recoverFromTerminatedSession()" +
                                " can be called.");
        }
    }    

    
    /**
     * This is the terminal listener class. It extends TerminalAdapter rather
     * than implementing the TerminalListener interface. This allows us to not
     * implement all of the methods in TerminalListener.
     */
    private class MyRegistrationListener extends RegistrationAdapter
    {

        /**
         * This method is called if the extension has become unregistered with
         * Communication Manager. This may have occurred as a result of an
         * unregistration request from the application, a network outage, or
         * Communication Manager may have unregistered the extension for other
         * reasons.
         *
         * @param event - the UnregisterEvent
         */
        public void terminalUnregistered(TerminalUnregisteredEvent event)
        {
            System.out.println(
                "Phone unregistered because " + event.getReason());

            try {
                removeListeners();
				station.cleanup();
			} catch (CstaException e) {
				e.printStackTrace();
			}
            // Could have tried to re-register, but for simplicity sake, simply
            // exit
            System.exit(0);
        }
    }

    /**
     * This is the tone detection listener class.
     */
    private class MyToneDetectionListener implements ToneDetectionListener
    {
        /**
         * This method determines which digit is pressed by the user and takes
         * various actions accordingly.
         *
         * @param event - ToneDetectedEvent
         * 
         * Note: All the responses/events are delivered on one thread. This guarantees
	     * sequential processing of events/responses. However if the processing of an
         * event involves an I/O as this function does, you may
	     * want to process this event in a different thread.
	     * One approach would be to have one separate thread processing events and responses
	     * off of a queue. All Listeners would queue events/responses to this queue if 
	     * events/responses are either blocking or require sequential processing.
         */
        public void toneDetected(ToneDetectedEvent event)
        {
            System.out.println(event.getToneDetected());
            String digit = event.getToneDetected();

            if (digit.equals("1"))
            {
                // Tell the user that the recording will start right after
                // this announcement. Since startRecording is set to true,
                // the recording is actually started when the stopped event
                // is received for this announcement.
                playMessage(RECORD);
            }
            else if (digit.equals("#"))
            {
                // Stop recording the file. When the "stopped" event is
                // received for the recording, the play will begin.
                stopPlayingAndRecording();
            }
            else
            {
                stopPlayingAndRecording();
                
                // Tell the user that s/he pressed a digit not on the menu.
//                playMessage(INVALID_KEY_PRESS);
                System.out.println("Invalid digit detected " + digit);
            }
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
         * @param event - the DisplayUpdatedEvent
         */
        public void displayUpdated(DisplayUpdatedEvent event)
        {
            System.out.println(
                "Display update event: " + event.getContentsOfDisplay());
        }
        
        /**
         * Method is fired when a LampMode update is received
         * 
         * @param event
         *            the received LampModeEvent
         */
        public void lampUpdated(LampModeEvent event) {
            int lamp = Integer.parseInt(event.getLamp());
            int module = (lamp & 0x700) >> 8;
            int newLampState = event.getLampMode().intValue();
            lamp = lamp & 0xFF;
            String lampType = "red  ";
            if (event.getLampColor().intValue() == 3) {
                lampType = "green";
            }
            String[] state = { "BROKENFLUTTER", "FLUTTER", "OFF", "STEADY",
                    "WINK", "XXXXX", "INVERTEDWINK", "FLASH", "INVERTEDFLASH" };

            String lampState = "UNKNOWN";

            if (event.getLampMode() < 8) {
                lampState = state[newLampState];
            }

            System.out.println("Extension= " + extension + " lampUpdate [module: " + module
                    + " buttonNum: " + lamp + " lampType: " + lampType
                    + " state: " + lampState + " :: " +event.getLampMode() +"]");
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
         * @param event - the received LampModeEvent
         */
        public void callAppearanceLampUpdated(String lampID, Short lampValue)
        {
            if (LampModeConstants.FLASH.equals(lampValue))
            {
                // A flashing green call appearance lamp indicates that there
                // is
                // an incoming call, so answer it.
                System.out.println("Lamp " + lampID + " is now flashing.");
                onHook = false;
                try
                {
                    station.answerCall(lampID);
                    System.out.println("The phone was answered");
                }
                catch (CstaException e)
                {
                    System.out.println("Couldn't answer the phone" + e);
                }
            }
            else if (LampModeConstants.STEADY.equals(lampValue))
            {
                // A steady green call appearance lamp indicates that the call
                // is now established. We can play our greeting.
                System.out.println("Lamp " + lampID + " is now on.");
                System.out.println("The call has been established.");
                playMessage(GREETING);
            }
            else if (LampModeConstants.OFF.equals(lampValue))
            {
                if (onHook) {
                    // We're already onhook, don't do anything.  This will
                    // happen when the app is initializing
                    return;
                }
                
                onHook = true;
                // If the lamp is off, we can hang up now.
                //stopPlayingAndRecording();
                station.disconnect();
				System.out.println("Hung up the phone");
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
        
        public void hookswitchChanged(HookswitchEvent event) {
            System.out.println("hookswitchUpdate: "
                    + event.getHookswitchOnHook());
        }

    }

    /**
     * This is the voice unit listener class. It extends VoiceUnitAdapter
     * rather than implementing the VoiceUnitListener interface. This allows us
     * to not implement all of the methods in VoiceUnitListener.
     */
    private class MyVoiceUnitListener extends VoiceUnitAdapter
    {

        /**
         * playing is fired when a file starts being played.
         *
         * @param event - PlayEvent
         */
        public void playing(PlayEvent event)
        {
            System.out.println("Started playing file: " + event.getMessage());
        }

        /**
         * recording is fired when a file is recorded over a connection.
         *
         * @param event - RecordEvent
         */
        public void recording(RecordEvent event)
        {
            System.out.println(
                "Record event received for connection ID: "
                    + event.getConnection());
        }

        /**
         * This is the only event we actually use from VoiceUnitServices. This
         * event is used for the following reasons: 1) When the announcement
         * for the start of the recording is done playing, the recording will
         * begin. 2) When a recording has stopped, that recording will be
         * played back.
         * 
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

            if (onHook) {
                // Don't do anything if we're onhook
                return;
            }
            
            // Are we done recording?
            if ((fileName != null) && (fileName.equals(currentRecordingFile)) &&
                    (!ifDonePlaybackRecording))
            {
                System.out.println(
                    "The file " + fileName + " is done being recorded");

                String recording = new String(currentRecordingFile);

                ifDonePlaybackRecording = true;

                // The recording is done! Let's play it back now.
                playMessage(recording);

                return;
            }
            // BEEP would follow the RECORD message.
            else if (fileName.equals(RECORD))
            {
                System.out.println(
                        "The file " + fileName + " is done being played");
                playMessage(BEEP);
            }
            else if (fileName.equals(BEEP))
            {
                System.out.println(
                        "The file " + fileName + " is done being played");
                startRecording();
            }
            else if (fileName.equals(GREETING))
            {
                System.out.println(
                        "The file " + fileName + " is done being played");
            }
            // The user recording is done being played, so repeat the cycle.
            else if (ifDonePlaybackRecording)
            {
                System.out.println(
                        "The file " + fileName + " is done being played");
                ifDonePlaybackRecording = false;
                playMessage(PROMPT);
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
            cleanup();
        }
    }
    /**
     * This is the Asynchronous Services call back for registration
     * 
     */
    private class MyAsyncRegistrationCallback implements AsynchronousCallback {


        /**
         * Handle the asynchronous response from a registration request
         * 
         * @param response
         *            the response to a registration request
         */
        public void handleResponse(Object response) {

            if (response.getClass().getName().equals(RegisterTerminalResponse.class.getName())) {
                RegisterTerminalResponse regResp = (RegisterTerminalResponse) response;
                if (regResp.getCode().equals(RegistrationConstants.NORMAL_REGISTER)) {
                    registered(regResp);
                } else if(regResp.getCode().equals(RegistrationConstants.EXTENSION_IN_USE)
                        && isMultiple) {
                    System.out.println("Already registered for multiple control");
                    registered(regResp);                
                } else {
                    registerFailed(regResp);
                }
            } else if (response.getClass().getName().equals(CSTAErrorCode.class.getName())) {
                registerFailed(((CSTAErrorCode)response).getStateIncompatibility().toString());
            } else {
                System.out.println("This is bad. Asynchronous response is not of valid type: " + response);
            }

        }

        /**
         * Handle the Exception from a registration request
         * 
         * @param exception
         *            An Asynchronous Exception thrown in response to a
         *            registration request
         */
        public void handleException(Throwable exception) {
            System.out
            .println("Received Asynchronous Exception from Server. The stack trace from the Server is as follows");
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
         * application only registers one extension, there is no need to examine
         * the contents of this message.
         * 
         * @param resp
         *            the RegisteredTerminalResponse that contains information
         *            about the registration.
         * 
         * Note: All the responses/events are delivered on one thread. This
         * guarantees sequential processing of events/responses. However if the
         * processing of an response involves an I/O as this function does, you
         * may want to process this response in a different thread. One approach
         * would be to have one separate thread processing events and responses
         * off of a queue. All Listeners would queue events/responses to this
         * queue if events/responses are either blocking or require sequential
         * processing.
         */

        private void registered(RegisterTerminalResponse response) {
            System.out.println("\nTutorialApp extension=" + extension + 
                    " registered at: " + System.currentTimeMillis()+"\n");

            if(getButtonInfo)
                station.getButtonInfo();
        }

        /**
         * This method is called if registration failed. Possible causes of this
         * are:
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
        private void registerFailed(RegisterTerminalResponse resp) {
            try {
                System.out.println("Registration failed for extension " + extension + " : reason "
                        + resp.getReason());
                // Make sure no listeners were left around.
                removeListeners();
                station.cleanup();
            } catch (CstaException e) {
                System.out.println("Exception when removing listeners or "
                        + "releasing device ID " + e);
            }
        }

        private void registerFailed(String error) {
            try {
                System.out.println("Registration failed for extension " + extension + " : reason "
                        + error);
                // Make sure no listeners were left around.
                removeListeners();
                station.cleanup();
            } catch (CstaException e) {
                System.out.println("Exception when removing listeners or "
                        + "releasing device ID " + e);
            }
        }

    }

    
    /**
     * This class implements the methods of the ServiceProviderListener to respond
     * to the server connection and session events.
     */
    private class MyServiceProviderListener implements ServiceProviderListener
    {

        /**
         * Received the server connection down event. Attempt to reconnect,
         * reestablishing the previous session.
         */
        public void serverConnectionDown(ServerConnectionDownEvent event)
        {
            System.out.println("The connection to the server is down because "
                    + event.getReason());
            
            if (event.getCode() != ServerConnectionDownEvent.DISCONNECT_APP_INITIATED)
            {             
                try
                {
                    Thread.sleep(20000);
                    System.out.println("Attempting to reconnect.");
                    provider.reconnect();               
                }
                catch (SessionCleanedUpException ex)
                {
                    try
                    {
                        recoverFromTerminatedSession();
                    }
                    catch (Exception e)
                    {
                        System.out.println(
                                "Java Exception - Could not reconnect to the server "
                                    + e);
                        e.printStackTrace(System.out);
                        System.exit(0); // exits to the shutdownThread to perform cleanup                       
                    }
                }
                catch (Exception e)
                {
                    System.out.println(
                        "Java Exception - Could not reconnect to the server "
                            + e);
                    e.printStackTrace(System.out);
                    System.exit(0); // exits to the shutdownThread to perform cleanup
                }
            }
        }

        /**
         * Received the server session not active event. Attempt to disconnect
         * and reconnect the connection, reestablishing the previous session.
         */
        public void serverSessionNotActive(ServerSessionNotActiveEvent event)
        {
            System.out.println("The server session is not active because "
                    + event.getReason());

            try
            {
                System.out.println("Attempting to disconnect and reconnect.");

                // disconnect from the server and leave the session active
                provider.disconnect(false);
                // reconnect to the server
                Thread.sleep(30000);
                provider.reconnect();
            }
            catch (Exception e)
            {
                System.out.println(
                        "Java Exception - Could not disconnect and "
                        + "reconnect to the server "
                            + e);
                e.printStackTrace(System.out);
                System.exit(0); // exits to the shutdownThread to perform cleanup
            }
        }

        /**
         * Received the server session terminated event.  An attempt will be made to 
         * establish a new session.
         *
         *
         */
        public void serverSessionTerminated(ServerSessionTerminatedEvent event)
        {
            System.out.println("The server session has terminated because "
                    + event.getReason());
            
            if (event.getCode() != ServerSessionTerminatedEvent.SESSION_TERMINATED_APP_INITIATED)
            {            
                try
                {
                    System.out.println("Attempting to set up a new session.");

                    recoverFromTerminatedSession();
                }
                catch (CstaException e)
                {
                    System.out.println(
                        "CSTA Exception - Could not restart the app "
                        + e);
                    e.printStackTrace(System.out);
                    System.exit(0); // exits to the shutdownThread to perform cleanup
                }
                catch (Exception e)
                {
                    System.out.println(
                        "Java Exception - Could not reestablish the session because "
                            + e);
                    e.printStackTrace(System.out);
                    System.exit(0); // exits to the shutdownThread to perform cleanup
                }
            }
        }

    }
}
