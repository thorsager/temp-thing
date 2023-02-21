/*
 * TutorialTtyPhone.java
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

package sampleapps.clientmediastack.tty;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Properties;
import com.avaya.common.logging.Level;
import com.avaya.common.logging.Logger;

import sampleapps.clientmediastack.TutorialMicrophone;
import sampleapps.clientmediastack.TutorialSpeaker;
import sampleapps.station.AvayaStation;
import sampleapps.station.AvayaStationAdapter;

import ch.ecma.csta.binding.DeviceID;
import ch.ecma.csta.errors.CstaException;

import com.avaya.api.media.MediaFactory;
import com.avaya.api.media.MediaSession;
import com.avaya.api.media.TtyCharacterType;
import com.avaya.api.media.TtyEncodingType;
import com.avaya.api.media.TtyTransmissionType;
import com.avaya.api.media.audio.Audio;
import com.avaya.api.media.audio.MediaEncryption;
import com.avaya.api.media.audio.RtpDirection;
import com.avaya.api.media.channels.AudioSink;
import com.avaya.api.media.channels.AudioSource;
import com.avaya.api.media.channels.TtySink;
import com.avaya.api.media.channels.TtySource;
import com.avaya.api.sessionsvc.exceptions.SessionCleanedUpException;
import com.avaya.cmapi.APIProtocolVersion;
import com.avaya.cmapi.ServiceProvider;
import com.avaya.cmapi.ServiceProviderListener;
import com.avaya.cmapi.evt.ServerConnectionDownEvent;
import com.avaya.cmapi.evt.ServerSessionNotActiveEvent;
import com.avaya.cmapi.evt.ServerSessionTerminatedEvent;
import com.avaya.csta.async.AsynchronousCallback;
import com.avaya.csta.binding.IPAddress;
import com.avaya.csta.binding.MediaInfo;
import com.avaya.csta.binding.MediaStartEvent;
import com.avaya.csta.binding.MediaStopEvent;
import com.avaya.csta.binding.RegisterTerminalResponse;
import com.avaya.csta.binding.TerminalUnregisteredEvent;
import com.avaya.csta.binding.types.DependencyMode;
import com.avaya.csta.binding.types.MediaMode;
import com.avaya.csta.physical.LampModeConstants;
import com.avaya.csta.registration.RegistrationAdapter;
import com.avaya.csta.registration.RegistrationConstants;
import com.avaya.csta.terminal.MediaControlAdapter;
import com.avaya.mvcs.framework.CmapiKeys;
import com.avaya.mvcs.media.audio.Codec;

/**
 * Purpose: This tutorial application demonstrates how to use the Avaya Client
 * Media stack library with TTY. Similar to the TutorialMediaStack, it
 * illustrates how a <code>MediaSink</code> like {@link TutorialSpeaker} and a
 * <code>MediaSource</code> like {@link TutorialMicrophone} can get access to
 * the Audio RTP stream. This version, however, also creates a TTYSink and
 * TTYSource in order to send and receive TTY messages using RFC2833.
 * <p>
 * The tutorial application registers a device (extension) with the Connector
 * Server and then waits for a call to come in. From a real phone, when a call
 * is made to this device, the device (application) answers the call. Incoming
 * Audio RTP Stream then goes to the Speaker. Audio from the Microphone goes to
 * the outgoing Audio RTP Stream.
 * <p>
 * An Applet is launched that will display the incoming TTY and allow the user
 * to type in TTY that will be immediately sent out. The intent is to simulate
 * an analog TTY device.
 * <p>
 * Registration is done asynchronously using Asynchronous Service.
 * 
 * @author Avaya Inc.
 */
@SuppressWarnings("unused")
public final class TutorialTtyPhone {

    private static final Logger log = Logger.getLogger(TutorialTtyPhone.class.getName());

    // These fields may be used to change the default port range for the media stream.
    // Default port range is 40000 - 47999, and the client-media-stack will pick two consecutive ports
    // within this range for each media stream. Each media stream requires two ports (for RTP & RTCP).
    // Note that it is not dependent on the addition of AudioSinks, AudioSources, TtySinks and/or
    // TtySources to the media stream.
    private static final String PROP_FILE_UDP_MIN = "com.avaya.mvcs.media.MediaManagerServices.local_udpport_min";
    private static final String PROP_FILE_UDP_MAX = "com.avaya.mvcs.media.MediaManagerServices.local_udpport_max";

    private String local_udpport_min = "40000"; // range of UDP ports to be used for RTP/RTCP
    private String local_udpport_max = "40099";
    
    private String activeCallAppearance = null;

    // The Device ID for our extension number will be supplied by the
    // Communication Manager API.
	private DeviceID id = null;

    // The Service Provider used to get the handles of various services.
    private ServiceProvider provider;
    private ServiceProviderListener serviceProviderListener;

    private AvayaStation station;

    // These are the listeners that will be added to listen for events coming
    // from RegistrationServices, and
    // PhysicalDeviceServices.
    private MyRegistrationListener registrationListener;

    private MyAvayaStationListener avayaStationListener;

    private MyMediaControlListener mediaControlListener;

    // The MediaSession and the Audio associated with it
    private MediaSession mediaSession;
    private Audio audio;
    private MediaInfo localMediaInfo;

    // Microphone and Speaker
    private AudioSource microphone;
    private AudioSink speaker;

    private final TutorialTtyProperties tutorialTtyProps;
    private Properties reconnectProps;
    
    private final TutorialTtyApp tutorialApp;
    
    private FileOutputStream conversationLog;
    
    private static final int DEFAULT_REDUNDANCY = 3;
    private int redundancy = DEFAULT_REDUNDANCY;

    protected TutorialTtyPhone(TutorialTtyApp tutorialApp, TutorialTtyProperties props) {
        this.tutorialApp = tutorialApp;
        this.tutorialTtyProps = props;
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
     *             calls to service provider, it is thrown to the caller.
     * @throws Exception - if any other runtime exception is generated, it is
     *             thrown to the caller.
     */
    public void bootstrap() throws CstaException, Exception {
        Properties spProp = new Properties();
        spProp.setProperty(CmapiKeys.SESSION_DURATION_TIMER, "240");
        spProp.setProperty(CmapiKeys.SESSION_CLEANUP_TIMER, "120");
        spProp.setProperty(CmapiKeys.SERVICE_PROVIDER_ADDRESS, tutorialTtyProps.getAesServerIP());
        spProp.setProperty(CmapiKeys.CMAPI_USERNAME, tutorialTtyProps.getAesUsername());
        spProp.setProperty(CmapiKeys.CMAPI_PASSWORD, tutorialTtyProps.getAesPassword());
        spProp.setProperty(CmapiKeys.SERVICE_PROVIDER_PORT, tutorialTtyProps.getAesServerPort());
        spProp.setProperty(CmapiKeys.SECURE_SERVICE_PROVIDER, tutorialTtyProps.getAesEncryption());
        if (tutorialTtyProps.getTrustStoreLocation() != null) {
            spProp.setProperty(CmapiKeys.TRUST_STORE_LOCATION, tutorialTtyProps.getTrustStoreLocation().trim());
        }

        // add following properties for validation client and service side certifications
        if(tutorialTtyProps.getTrustStorePassword() != null){
            spProp.setProperty(CmapiKeys.TRUST_STORE_PASSWORD, tutorialTtyProps.getTrustStorePassword().trim());
        }
        if(tutorialTtyProps.getKeyStoreLocation() != null){
            spProp.setProperty(CmapiKeys.KEY_STORE_LOCATION, tutorialTtyProps.getKeyStoreLocation().trim());
        }
        if(tutorialTtyProps.getKeyStoreType()!= null){
            spProp.setProperty(CmapiKeys.KEY_STORE_TYPE, tutorialTtyProps.getKeyStoreType().trim());
        }
        if(tutorialTtyProps.getKeyStorePassword() != null){
            spProp.setProperty(CmapiKeys.KEY_STORE_PASSWORD, tutorialTtyProps.getKeyStorePassword().trim());
        }
        if(tutorialTtyProps.getValidPeer() != null){
            spProp.setProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE, tutorialTtyProps.getValidPeer().trim());
        }
        if(tutorialTtyProps.getHostnameValidation() != null){
            spProp.setProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE_HOSTNAME, tutorialTtyProps.getHostnameValidation().trim());
        }
        
        spProp.put(CmapiKeys.SESSION_PROTOCOL_VERSION_LIST, APIProtocolVersion.VERSION_LIST);

        // Get a handle to the ServiceProvider class.
        provider = ServiceProvider.getServiceProvider(spProp);
        reconnectProps = spProp;

        // Create new AvayaStation object.
        station = new AvayaStation();
        station.init(tutorialTtyProps.getCallServerIP(), "", tutorialTtyProps.getExtension(), provider);
    }

    /**
     * This method actually starts the app. It must be called after calling
     * bootstrap or it will fail.
     * 
     * @throws Exception - if an Exception is generated by one of the calls to
     *             the services, it is thrown to the caller.
     */
    public void start(TtySink guiSink, TtySource guiSource) throws Exception {
        log.info("Startup using switch="
                + tutorialTtyProps.getCallServerIP() + " ext="
                + tutorialTtyProps.getExtension() + " pw="
                + tutorialTtyProps.getExtensionPassword());

        // Get the device ID from the station class
        id = station.getDeviceID();

        // Add listeners to be notified of events coming from the various
        // services.
        addListeners();

        // Set the Media Properties like local endpoint IPAddress, Min and Max UDP Port Range,
        // if you do not want the defaults chosen by the Stack
        Properties mediaProp = new Properties();
        mediaProp.setProperty(PROP_FILE_UDP_MIN, local_udpport_min);
        mediaProp.setProperty(PROP_FILE_UDP_MAX, local_udpport_max);
        
        // Now create the media factory. Note: the factory only needs to be created once.
        // Multiple mediaSessions and audio streams (if needed) can be created from one factory.
        MediaFactory mediaFactory = MediaFactory.createFactory(mediaProp); // or specify "null" for default properties

        // Determine requested RTP direction (default is "XMIT_RCV")
        // N.B. the RTP direction affects both Audio and TTY streams.
        RtpDirection rtpDirection = RtpDirection.XMIT_RCV; // default
        
        if ("XMIT_ONLY".equalsIgnoreCase(tutorialTtyProps.getRtpDirection()) ||
            "XMITONLY".equalsIgnoreCase(tutorialTtyProps.getRtpDirection())) {
            rtpDirection = RtpDirection.XMIT_ONLY;
        } else if ("RCV_ONLY".equalsIgnoreCase(tutorialTtyProps.getRtpDirection()) ||
                   "RCVONLY".equalsIgnoreCase(tutorialTtyProps.getRtpDirection())) {
            rtpDirection = RtpDirection.RCV_ONLY;
        }

        // Create an audio stream (1st) and mediaSession (2nd) for the extension.
        // The RTP/RTCP ports will be chosen from the range specified in the
        // mediaProp used to create the mediaFactory.
        audio = mediaFactory.createAudio(tutorialTtyProps.getExtension(), rtpDirection);
        mediaSession = mediaFactory.createMediaSession(tutorialTtyProps.getExtension(), audio);
        
        // Create local MediaInfo. 
        MediaInfo localMediaInfo = new MediaInfo();

        // Create local RTP Address and add it to local MediaInfo.
        // Note: the local RTP/RTCP IP address and ports are
        // retrieved from the Audio (RTP Stack).
        IPAddress localRtpAddress = new IPAddress();
        localRtpAddress.setAddress(audio.getLocalRTPAddress().getAddress().getHostAddress());
        localRtpAddress.setPort(new Integer(audio.getLocalRTPAddress().getPort()));
        localMediaInfo.setRtpAddress(localRtpAddress);

        // Create local RTCP Address and add it to local MediaInfo.
        IPAddress localRtcpAddress = new IPAddress();
        localRtcpAddress.setAddress(audio.getLocalRTCPAddress().getAddress().getHostAddress());
        localRtcpAddress.setPort(new Integer(audio.getLocalRTCPAddress().getPort()));
        localMediaInfo.setRtcpAddress(localRtcpAddress);

        // Create local list of Codecs (default is G.711U only) and add it to local MediaInfo.
        String codec = tutorialTtyProps.getCodec();
        String[] codecList = null;
        int packetSize = Codec.DEFAULT_PACKET_SIZE;
        
        if (Audio.G711U.equalsIgnoreCase(codec))
            codecList = new String[]  { Audio.G711U };
        else if (Audio.G711A.equalsIgnoreCase(codec))
            codecList = new String[] { Audio.G711A };
        else if (Audio.G729.equalsIgnoreCase(codec))
            codecList = new String[] { Audio.G729 };
        else if (Audio.G729A.equalsIgnoreCase(codec))
            codecList = new String[] { Audio.G729A };
        else if (Audio.G723.equalsIgnoreCase(codec)) { // g723 is only valid for client media mode
            codecList = new String[] { Audio.G723 };
            packetSize = Codec.G723_PACKET_SIZE;
        } else {
            // default
            codec = Audio.G711U;
            codecList = new String[] { Audio.G711U };
        }
        localMediaInfo.setCodecs(codecList);
        
        // Create local list of Encryption algorithms (default is "both")
        // and add it to local MediaInfo.
        if (Audio.AES.equalsIgnoreCase(tutorialTtyProps.getMediaEncryption()))
            localMediaInfo.setEncryptionList(new String[] { Audio.AES });
        else if (Audio.NOENCRYPTION.equalsIgnoreCase(tutorialTtyProps.getMediaEncryption()))
            localMediaInfo.setEncryptionList(new String[] { Audio.NOENCRYPTION });
        else
            // default
            localMediaInfo.setEncryptionList(new String[] { Audio.AES, Audio.NOENCRYPTION });

        log.info("setupAudio" + ": address=" + InetAddress.getByName(localRtpAddress.getAddress())
                              + ", direction=" + rtpDirection
                              + ", codec=" + localMediaInfo.getCodecs()[0]
                              + ", encryption="+ localMediaInfo.getEncryptionList()[0]);

        // N.B. check that microphone & speaker
        // support the codec that you intend to use.
        // Currently, TutorialMicrophone & TutorialSpeaker
        // only support G.711U & G.711A codecs.

        // Create an Audio Source from the TutorialMicrophone
        // and attach it to the mediaSession as a source.
        // Also attach the TTY source to the MediaSession
        if (rtpDirection == RtpDirection.XMIT_RCV ||
            rtpDirection == RtpDirection.XMIT_ONLY) {
            microphone = new TutorialMicrophone();
            mediaSession.attachSource(microphone);
            microphone.setCodec(codec, packetSize);
            log.info("Added Audio source to media session");
        
            mediaSession.attachSource(guiSource);
            log.info("Added TTY source to media session");
        }

        // Now create an Audio Sink from the TutorialSpeaker
        // and attach it to the mediaSession as a sink.
        // Also attach the TTY sink to the MediaSession
        if (rtpDirection == RtpDirection.XMIT_RCV ||
            rtpDirection == RtpDirection.RCV_ONLY) {
            speaker = new TutorialSpeaker();
            mediaSession.attachSink(speaker);
            speaker.setCodec(codec, packetSize);
            log.info("Added Audio sink to media session");
        
            mediaSession.attachSink(guiSink);
            log.info("Added TTY sink to media session");
        }
        
        // Get TTY redundancy
        String redundancyStr = tutorialTtyProps.getTtyRedundancy();
        try {
        	redundancy = Integer.parseInt(redundancyStr);
        	if (redundancy < 0) { // redundancy=0 means: "don't change it".
        		log.info("Negative redundancy specified - deafulting to " + DEFAULT_REDUNDANCY);
            	redundancy = DEFAULT_REDUNDANCY;
        	}
        } catch(NumberFormatException nfe) {
    		log.info("Non-numeric redundancy specified - deafulting to " + DEFAULT_REDUNDANCY);
        	redundancy = DEFAULT_REDUNDANCY;
        }
        
        // enable TTY transmission
        setTtyEnabled(true, redundancy);

        // Now it's time to register the device.
        // Supply the extension password and specify MAIN DependencyMode (shared control
        // is not supported in the Client Media configuration).
        // Request Client Media configuration and supply the localMedia Information.
        // Don't need Telecommuter Mode nor E911 option. Force the login (if necessary).
        // Register asynchronously since there's no reason to hold up this
        // thread waiting for the response.
        station.register(tutorialTtyProps.getExtensionPassword(), DependencyMode.MAIN, MediaMode.CLIENT,
                         null, null, true, localMediaInfo, null, new MyAsyncRegistrationCallback());

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
    private void addListeners() throws CstaException, Exception {
        // Add a listener so we can receive events indicating when the phone is
        // registered / unregistered.
        registrationListener = new MyRegistrationListener();
        station.addListener(registrationListener);

        // Add an AvayaStationListner to receive events indicating phone rings,
        // lamp updates.
        avayaStationListener = new MyAvayaStationListener();
        station.addListener(avayaStationListener);

        // Add a listener to receive Media Events
        mediaControlListener = new MyMediaControlListener();
        station.addListener(mediaControlListener);

        // Add a service provider listener to receive events indicating
        // the state of the server connection or session.  
        serviceProviderListener = new MyServiceProviderListener();
        provider.addServiceProviderListener(serviceProviderListener);  
    }

    /**
     * Removes all of the listeners that were added for the various services.
     */
    private void removeListeners() throws CstaException {
        station.removeListener(avayaStationListener);
        station.removeListener(registrationListener);
        station.removeListener(mediaControlListener);
        try {
            provider.removeServiceProviderListener(serviceProviderListener);
        } catch (Exception e) {}
    }

    /**
     * This cleanup method assures us that we unregister our extension, remove
     * all the listeners, stop collecting DTMF tones and stop any announcements
     * or recordings that were happening. The connector server will cleanup if a
     * client goes away without doing this cleanup, but it's best to be safe.
     */
    public void cleanup() {
        log.info("cleaning up");

        try {
            // Cleanup Local audio
            if (audio != null) {
                audio.cleanup();
            }

            // Make sure no listeners were left around.
            removeListeners();

            // Call the synchronous version of unregister() because we want the
            // unregister to succeed before cleaning up and disconnecting.
            if (station != null && station.getDeviceID() != null) {
                station.unregister();
                station.cleanup();
            }

            // provider.disconnect will disconnect from the server and
            // terminate the active session.
            if (provider != null) {
                provider.disconnect(true);
            }

        } catch (Exception e) {
            log.warning("Could not clean up properly: " + e);
        }
        
        // close the conversation log file
        if (conversationLog != null) {
            try {
                conversationLog.close();
            } catch (IOException e) {
                log.warning("Unable to close conversation log file: "
                        + e.getMessage());
            }
        }
    }

    public boolean makeCall(String calledNumber) throws CstaException {
        log.info("MakeCall calling " + calledNumber);

        String callAppr = station.initCall(calledNumber);

        return (callAppr != null);
    }
    
    public void disconnect() {
        station.disconnect();
    }
    
    /**
     * method to be called by TutorialTtyApp to enable/disable TTY
     */
    protected void setTtyEnabled(boolean ttyEnabled, int redundancy) {
        if (log.isLoggable(Level.FINE))
            log.fine("setting ttyEnabled to " + ttyEnabled + " and redundancy=" + redundancy);
        
        if (ttyEnabled) {
            // if the configuration has conversation logging enabled try to
            // create the log file
            if (tutorialTtyProps.isConvLogEnabled()) {
                if (conversationLog == null) {
                    try {
                        conversationLog = new FileOutputStream(tutorialTtyProps.getConvLogFilename());
                        log.info("Conversation log file created at: "
                                + tutorialTtyProps.getConvLogFilename());
                    } catch (FileNotFoundException e) {
                        log.severe("Unable to open the configured log file,"
                                + " please check the options: " + e.getMessage());
                    }
                }
            }
            
            TtyTransmissionType ttt = TtyTransmissionType.RFC2833_US; // default
            String type = tutorialTtyProps.getBaudotBaudRate();
            for (TtyTransmissionType t : TtyTransmissionType.values()) {
                if (type.equals(t.toString())) {
                        ttt = t;
                        break;
                }
            }
            
            mediaSession.enableTty(TtyEncodingType.BAUDOT, ttt, conversationLog, redundancy);
        } else {
            mediaSession.disableTty();
        }
    }
    
    /**
     * This will set the mediaSession back into Letter mode
     */
    protected void resetTTYBackToLetterMode() {
        mediaSession.setTtyFigureLetterMode(TtyCharacterType.LETTER);
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
            station.init(tutorialTtyProps.getCallServerIP(), "", tutorialTtyProps.getExtension(), provider);
        
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
            station.register(tutorialTtyProps.getExtensionPassword(), DependencyMode.MAIN, MediaMode.CLIENT, null, null, true,
                    localMediaInfo, null, new MyAsyncRegistrationCallback());
        }    
        else
        {
            throw new Exception("bootstrap() must be called before recoverFromTerminatedSession()" +
                                " can be called.");
        }
    }    

    
    /**
     * This is the physical device listener class. It extends
     * PhysicalDeviceAdapter rather than implementing the PhysicalDeviceListener
     * interface. This allows us to not implement all of the methods in
     * PhysicalDeviceListener.
     */
    private class MyAvayaStationListener extends AvayaStationAdapter {

        /**
         * Method is fired when the LampMode update is received. In this app, we
         * only pay attention to updates to green call appearance lamps. These
         * lamps tell us the status of the call on that call appearance. A
         * flashing green lamp indicates an incoming call. When that lamp
         * becomes steady, that indicates that the call has been established.
         * When that lamp goes dark, the far end has hung up. One thing to note
         * about the implementation of this method is that for Avaya terminals,
         * the ID of the green call appearance ID is the same as the ID for the
         * corresponding call appearance button. This fact is critical to the
         * implementation of this application.
         * 
         * NOTE: In future releases of the Communication Manager API we will be
         * supplying Call Control Services which will, among other things, give
         * a much better way of determining call state. It will be a cleaner way
         * to determine when calls are incoming, when they are fully
         * established, and when the calls are disconnected.
         * 
         * @param event the received LampModeEvent
         */
        public void callAppearanceLampUpdated(String lampID, Short lampValue) {

            if (LampModeConstants.FLASH.equals(lampValue)) {
                // A flashing green call appearance lamp indicates that there is
                // an incoming call, so answer it.
                log.info("Lamp " + lampID + " is now flashing.");
                try {
                    station.answerCall(lampID);
                } catch (CstaException e) {
                    log.warning("Couldn't answer the phone" + e);
                }
                log.info("The phone was answered");
            } else if (LampModeConstants.STEADY.equals(lampValue)) {
                // A steady green call appearance lamp indicates that the call
                // is now established. We can play our greeting.
                log.info("Lamp " + lampID + " is now on.");
                activeCallAppearance = lampID;

                log.info("The call has been established.");
                
                tutorialApp.connected();

            } else if (LampModeConstants.OFF.equals(lampValue)
                    && activeCallAppearance != null
                    && lampID.equals(activeCallAppearance)) {

                log.info("Lamp " + lampID + " is now off.");

                // If this lamp ID matches that of the active call
                // appearance, the far end went on hook, and we can hang up.
                log.info("The other person hung up");
                activeCallAppearance = null;

                station.disconnect();
                log.info("Hung up the phone");
                
                tutorialApp.disconnected();

            } else if (LampModeConstants.FLUTTER.equals(lampValue)) {
                log.info("Lamp " + lampID + " is fluttering.");
            }
        }
    }

    /*
     * This is the Listener for Media Events. These events indicate when the far
     * end endpoint started or stopped media. Use these events to start or stop
     * media on the Client Media Stack.
     */
    private class MyMediaControlListener extends MediaControlAdapter {

        public void mediaStarted(MediaStartEvent startEvent) {
            try {
                // Get the Far End RTP/RTCP Address, the codec and the Packet
                // Size from the event.
                InetAddress remoteAddress =
                        InetAddress.getByName(startEvent.getRtpAddress().getAddress());
                int remoteRtpPort =
                        startEvent.getRtpAddress().getPort().intValue();
                int remoteRtcpPort =
                        startEvent.getRtcpAddress().getPort().intValue();
                String remoteCodec = startEvent.getCodec();

                MediaEncryption remoteEncryption = new MediaEncryption();
                remoteEncryption.setProtocol(startEvent.getEncryption().getProtocol());
                remoteEncryption.setTransmitKey(startEvent.getEncryption().getTransmitKey());
                remoteEncryption.setReceiveKey(startEvent.getEncryption().getReceiveKey());
                remoteEncryption.setPayloadType(startEvent.getEncryption().getPayloadType().intValue());

                int packetSize = startEvent.getPacketSize().intValue();

                log.info(" starting Audio: remote=" + remoteAddress + ":"
                        + remoteRtpPort + ", codec=" + remoteCodec
                        + ", packetsize=" + packetSize + ", protocol="
                        + remoteEncryption.getProtocol());

                // reset the letter/figure mode in the mediaSession to letter
                // mode so that we know it starts off correctly
                mediaSession.setTtyFigureLetterMode(TtyCharacterType.LETTER);
                
                // Start Audio on the Client Media Stack
                // with the Far End Audio Information
                audio.start(
                        new InetSocketAddress(remoteAddress, remoteRtpPort),
                        new InetSocketAddress(remoteAddress, remoteRtcpPort),
                        remoteCodec, packetSize, remoteEncryption);
                
                // notify the application
                tutorialApp.mediaStarted();
                
                // add a line to the conversation log file with date
                if (conversationLog!=null) {
                    PrintStream out = new PrintStream(conversationLog);
                    out.println("TTY conversation started: " + new Date());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        public void mediaStopped(MediaStopEvent stopEvent) {
            try {
                log.info("stopping Audio");
                // Stop Audio on the Client Media Stack
                audio.stop();

                // Stop the Speaker, since audio has stopped
                if (speaker != null)
                    speaker.close();

                // Stop the Microphone, since audio has stopped
                if (microphone != null)
                    microphone.close();
                
                // add a line to the conversation log file with date
                if (conversationLog != null) {
                    PrintStream out = new PrintStream(conversationLog);
                    out.println("TTY conversation stopped: " + new Date());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * This is the Asynchronous Services call back for RegistrationServices
     * 
     */
    private class MyAsyncRegistrationCallback implements AsynchronousCallback {
        /**
         * Handle the asynchronous response from a registration request
         * 
         * @param response the response to a registration request
         */
        public void handleResponse(Object response) {

            if (response.getClass().getName().equals(
                    RegisterTerminalResponse.class.getName())) {
                RegisterTerminalResponse regResp =
                        (RegisterTerminalResponse) response;
                if (regResp.getCode().equals(
                        RegistrationConstants.NORMAL_REGISTER)) {
                    registered(regResp);
                } else {
                    registerFailed(regResp);
                }
            } else {
                log.warning("This is bad. Asynchronous response is not of valid type: "
                        + response);
            }
        }

        /**
         * Handle the Exception from a registration request
         * 
         * @param exception An Asynchronous Exception thrown in response to a
         *            registration request
         */
        public void handleException(Throwable exception) {
            try {
                log.warning("Received Asynchronous Exception from Server: "
                        + exception);

                // Make sure no listeners were left around.
                removeListeners();
                station.cleanup();
            } catch (CstaException e) {
                log.warning("Exception when removing listeners or "
                        + "releasing device ID " + e);
            }

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
         * @param resp the RegisteredTerminalResponse that contains information
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
        private void registered(RegisterTerminalResponse resp) {
            log.info("Signaling encryption="
                    + resp.getSignalingEncryption());

            station.getButtonInfo();

            // Use new-line characters in these println statements to make the
            // instructions in the second println stand out.
            log.info("The phone is now registered and waiting for calls.\n");
            
            tutorialApp.registered();
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
         * @param resp the RegisterTerminalResponse
         */
        private void registerFailed(RegisterTerminalResponse resp) {
            try {

                // Make sure no listeners were left around.
                removeListeners();

                station.cleanup();
            } catch (CstaException e) {
                log.info("Exception when removing listeners or "
                        + "releasing device ID " + e);
            }

            tutorialApp.registerFailed(resp.getReason());
        }
    }

    /**
     * This is the Registration listener class. It extends RegistrationAdapter
     * rather than implementing the RegistrationListener interface. This allows
     * us to not implement all of the methods in RegistrationListener.
     */
    private class MyRegistrationListener extends RegistrationAdapter {

        /**
         * This method is called if the extension has become unregistered with
         * Communication Manager. This may have occurred as a result of an
         * unregistration request from the application, or Communication Manager
         * may have unregistered the extension for other reasons.
         * 
         * @param event the TerminalUnregisteredEvent
         */
        public void terminalUnregistered(TerminalUnregisteredEvent event) {
            try {
                log.info("Received unregistered event: reason "
                        + event.getReason());
                log.info("Terminating application");

                // Make sure no listeners were left around.
                removeListeners();
                station.cleanup();
                
                tutorialApp.unregistered(event.getReason());
                
            } catch (CstaException e) {
                log.info("Exception when removing listeners or "
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
