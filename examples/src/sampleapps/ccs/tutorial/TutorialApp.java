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

package sampleapps.ccs.tutorial;

// Java utilities
import java.net.URL;
import java.util.Properties;

import sampleapps.station.AvayaStation;
import ch.ecma.csta.binding.AnswerCall;
import ch.ecma.csta.binding.ClearCall;
import ch.ecma.csta.binding.ConnectionClearedEvent;
import ch.ecma.csta.binding.ConnectionID;
import ch.ecma.csta.binding.DeleteMessage;
import ch.ecma.csta.binding.DeliveredEvent;
import ch.ecma.csta.binding.DeviceID;
import ch.ecma.csta.binding.EstablishedEvent;
import ch.ecma.csta.binding.LocalDeviceID;
import ch.ecma.csta.binding.PlayEvent;
import ch.ecma.csta.binding.PlayMessage;
import ch.ecma.csta.binding.RecordEvent;
import ch.ecma.csta.binding.RecordMessage;
import ch.ecma.csta.binding.RecordMessageResponse;
import ch.ecma.csta.binding.Stop;
import ch.ecma.csta.binding.StopEvent;
import ch.ecma.csta.callcontrol.CallControlAdapter;
import ch.ecma.csta.callcontrol.CallControlServices;
import ch.ecma.csta.errors.CstaException;
import ch.ecma.csta.monitor.MonitoringServices;
import ch.ecma.csta.voiceunit.VoiceUnitAdapter;
import ch.ecma.csta.voiceunit.VoiceUnitServices;

import com.avaya.api.media.audio.Audio;
import com.avaya.cmapi.APIProtocolVersion;
import com.avaya.cmapi.ServiceProvider;
import com.avaya.csta.binding.MediaInfo;
import com.avaya.csta.binding.TerminalUnregisteredEvent;
import com.avaya.csta.binding.ToneDetectedEvent;
import com.avaya.csta.binding.types.DependencyMode;
import com.avaya.csta.binding.types.MediaMode;
import com.avaya.csta.registration.RegistrationAdapter;
import com.avaya.csta.tonedetection.ToneDetectionListener;
import com.avaya.mvcs.framework.CmapiKeys;

/**
 * Purpose: This tutorial application demonstrates how to write a simple Interactive
 * Voice Response (IVR) application using the Avaya Communication Manager API.
 * It registers an extension with Communication Manager, and waits for that
 * extension to be dialed. When a user dials the associated extension number,
 * the application answers the incoming call and plays a greeting to the user.
 * 
 * Call Control Services is used to answer the call and the call control events 
 * are used to determine when to play the greeting.  
 * 
 * The user can then press 1 to record a message and have it played back to them.
 * All announcements / functions can be preempted by another button press. The
 * user can hang up and dial back into the application extension repeatedly.
 *
 * Registration is done synchronously using Registration Services.
 * 
 * A TSAPI Basic User license is required to run this application.
 * 
 * @author AVAYA, Inc.
 */

public class TutorialApp
{

    // These fields are read in from the application.properties file. This file
    // must be populated with a valid IP address for Avaya Communication
    // Manager, as well as a valid extension number and password for the
    // application softphone.
    private String callServer;
    private String extension;
    private String password;
    private String codec;
    private String encryption;

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
    private boolean ifDonePlayingRecording = false;

    // This thread is used to receive a shutdown notification when the
    // application is terminated.
    private MyShutdownThread shutdownThread;

    // The Service Provider used to get the handles of various services.
    private ServiceProvider provider;

    // These fields will be populated with the handles of the various services,
    // which we will get through the ServiceProvider class.
    private VoiceUnitServices voiceSvcs;
    private CallControlServices callControlSvcs;
    private AvayaStation station;
    // MonitoringServices will be used to add/remove voice unit listener and
    // tone detection listener.
    private MonitoringServices montSvcs;

    // These are the listeners that will be added to listen for events coming
    // from VoiceUnitServices, ToneCollectionServices, TerminalServices, and
    // PhysicalDeviceServices.
    private MyVoiceUnitListener playAndRecordListener;
    private MyToneDetectionListener toneDetectListener;
    private MyRegistrationListener registrationListener;
    private MyCallControlListener callControlListener;
    
    // state variable that is used to determine whether 
    // there is an active call or not
    private boolean callIsActive = false;

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
    public void bootstrap() throws CstaException, Exception
    {
        String cmapiServerIp;
        String cmapiUsername;
        String cmapiPassword;
        String cmapiServerPort;
        String cmapiSecure;
        String cmapiTrustStoreLocation;

        //  add following properties for validation client and service side certifications 
        String cmapiTrustStorePassword;
        String cmapiKeyStoreLocation;
        String cmapiKeyStorePassword;
        String isValidPeer;
        String hostnameValidation;

        // Get all of the arguments from the application properties file
        ClassLoader cl = TutorialApp.class.getClassLoader();
        URL appURL = cl.getResource("tutorial.properties");
        Properties appProp = new Properties();
        appProp.load(appURL.openStream());
        callServer = appProp.getProperty(PROP_FILE_CALL_SERVER).trim();
        extension = appProp.getProperty(PROP_FILE_EXTENSION).trim();
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

        voiceSvcs =
            (VoiceUnitServices) provider.getService(
                ch.ecma.csta.voiceunit.VoiceUnitServices.class.getName());

        montSvcs = (MonitoringServices) provider.getService(
                ch.ecma.csta.monitor.MonitoringServices.class.getName());
        
        callControlSvcs = (CallControlServices) provider.getService(
                ch.ecma.csta.callcontrol.CallControlServices.class.getName());

        // Create new AvayaStation object.
        station = new AvayaStation();
        station.init(callServer, extension, provider);
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
        // is terminated. The Communication Manager API connector server code
        // will clean up if an app goes away unexpectedly, but it's still good
        // to clean up.
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

        // Time to register the device. Pass in the password. Shared control
        // is set to false for this application. You can use the default media settings
        // (media goes to connector server over G.711 codec with no encryption), or you
        // can explicitly specify the codecs and encryption for the MediaInfo
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
                                                                                

        // Register synchronously to show the easiest way.
        boolean retValue = station.register(password, DependencyMode.MAIN, MediaMode.SERVER, null, null, true,
        		localMediaInfo, null, null);

        if(retValue){

            System.out.println(
                "The phone is now registered and waiting for calls.\n");

            System.out.println(
                "DIAL "
                    + extension
                    + " FROM ANOTHER PHONE TO ACCESS THE APPLICATION.\n");
            
        } else {
            System.out.println("Registration failed, exiting");
        	removeListeners();
            station.cleanup();
        }
        
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
        
        // Add a listener to receive call control events
        callControlListener = new MyCallControlListener();
        montSvcs.addCallControlListener(id, callControlListener);  
           
    }

    /**
     * Removes all of the listeners that were added for the various services.
     */
    private void removeListeners() throws CstaException
    {
        
        montSvcs.removeRegistrationListener(id, registrationListener);
        montSvcs.removeVoiceUnitListener(id, playAndRecordListener);
        montSvcs.removeToneDetectionListener(id, toneDetectListener);
        montSvcs.removeCallControlListener(id, callControlListener);
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
        if (station.getDeviceID() == null)
        {
            return;
        }

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
     * This method sends an answerCall request to Call Control Services
     * to answer a call.  
     * 
     * @param connId - The ConnectionID of the call to answer
     * @throws CstaException
     */
    private void answerCall(ConnectionID connId) throws CstaException
    {
        // answer the call
        System.out.println("Answering Call ");
        AnswerCall request = new AnswerCall();  
        
        // get the connection id to answer
        request.setCallToBeAnswered(connId);       
        callControlSvcs.answerCall(request);

    }
    
    /**
     * This method sends a clearCall request to Call Control Services
     * which will release all devices on the call.
     * 
     * @param connId
     * @throws CstaException
     */
    private void clearCall(ConnectionID connId) throws CstaException
    {
        // answer the call
        System.out.println("Clearing Call ");
        ClearCall request = new ClearCall();  
        
        // get the connection id to answer
        request.setCallToBeCleared(connId);  
        callControlSvcs.clearCall(request);

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
                // Tell the user that s/he pressed a digit not on the menu.
                playMessage(INVALID_KEY_PRESS);
                System.out.println("Invalid digit detected " + digit);
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
        	// don't do anything if far end hung up
        	if (!callIsActive) {
        		return;
        	}
            String fileName = event.getMessage();
            
            // Are we done recording?
            if ((fileName != null) && (fileName.equals(currentRecordingFile)) &&
                    (!ifDonePlayingRecording))
            {
                System.out.println(
                    "The file " + fileName + " is done being recorded");

                String recording = new String(currentRecordingFile);

                ifDonePlayingRecording = true;

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
            else if (ifDonePlayingRecording)
            {
                System.out.println(
                        "The file " + fileName + " is done being played");
                ifDonePlayingRecording = false;
                playMessage(PROMPT);
            }

        }
    }
    private class MyCallControlListener extends CallControlAdapter 
    {
        
        public void connectionCleared(ConnectionClearedEvent event) {
            System.out.println("Received ConnectionCleared Event, x" + extension);
            callIsActive = false;
            stopPlayingAndRecording();
        }

        public void delivered(DeliveredEvent event) {           
            System.out.println("Received Delivered Event, x" + extension);
                       
            try {
                answerCall(event.getConnection());
            } catch (CstaException e) {
                // answerCall Failed
            	// clear the call
            	try {
					clearCall(event.getConnection());
				} catch (CstaException e1) {
					System.out.println("clearCall failed after answercall failure");
				}
            }            
        }

        public void established(EstablishedEvent event) {
            System.out.println("Received Established Event, x" + extension);   
            callIsActive = true;
            playMessage(GREETING);            
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
