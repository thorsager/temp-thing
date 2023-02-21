/*
 * Softphone.java
 * 
 * Copyright (c) 2007 Avaya Inc. All rights reserved.
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

package sampleapps.softphone;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.net.NetworkInterface;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.UIManager;

import sampleapps.clientmediastack.TutorialMicrophone;
import sampleapps.clientmediastack.TutorialSpeaker;
import sampleapps.station.AvayaStation;
import sampleapps.station.ButtonTypes;
import ch.ecma.csta.binding.ButtonItem;
import ch.ecma.csta.binding.ButtonList;
import ch.ecma.csta.binding.ButtonPress;
import ch.ecma.csta.binding.ConnectionID;
import ch.ecma.csta.binding.DeviceID;
import ch.ecma.csta.binding.DisplayUpdatedEvent;
import ch.ecma.csta.binding.GetButtonInformation;
import ch.ecma.csta.binding.GetButtonInformationResponse;
import ch.ecma.csta.binding.HookswitchEvent;
import ch.ecma.csta.binding.LampModeEvent;
import ch.ecma.csta.binding.LocalDeviceID;
import ch.ecma.csta.binding.RingerStatusEvent;
import ch.ecma.csta.binding.SetHookswitchStatus;
import ch.ecma.csta.errors.CstaException;
import ch.ecma.csta.errors.InvalidDeviceStateException;
import ch.ecma.csta.monitor.MonitoringServices;
import ch.ecma.csta.physical.PhysicalDeviceAdapter;
import ch.ecma.csta.physical.PhysicalDeviceServices;

import com.avaya.api.media.MediaFactory;
import com.avaya.api.media.MediaSession;
import com.avaya.api.media.audio.Audio;
import com.avaya.api.media.audio.MediaEncryption;
import com.avaya.api.media.channels.MediaSink;
import com.avaya.api.media.channels.MediaSource;
import com.avaya.api.sessionsvc.exceptions.SessionCleanedUpException;
import com.avaya.cmapi.APIProtocolVersion;
import com.avaya.cmapi.ServiceProvider;
import com.avaya.cmapi.ServiceProviderListener;
import com.avaya.cmapi.callinformation.CallInformationServices;
import com.avaya.cmapi.evt.ServerConnectionDownEvent;
import com.avaya.cmapi.evt.ServerSessionNotActiveEvent;
import com.avaya.cmapi.evt.ServerSessionTerminatedEvent;
import com.avaya.csta.async.AsynchronousCallback;
import com.avaya.csta.binding.GetCallInformation;
import com.avaya.csta.binding.GetCallInformationResponse;
import com.avaya.csta.binding.IPAddress;
import com.avaya.csta.binding.MediaInfo;
import com.avaya.csta.binding.MediaStartEvent;
import com.avaya.csta.binding.MediaStopEvent;
import com.avaya.csta.binding.RegisterFailedEvent;
import com.avaya.csta.binding.RegisterTerminalResponse;
import com.avaya.csta.binding.RegisteredEvent;
import com.avaya.csta.binding.ServiceLinkStatusEvent;
import com.avaya.csta.binding.TerminalUnregisteredEvent;
import com.avaya.csta.binding.UnregisterEvent;
import com.avaya.csta.binding.ValidateDeviceSecurityCode;
import com.avaya.csta.binding.ValidateDeviceSecurityCodeResponse;
import com.avaya.csta.binding.types.DependencyMode;
import com.avaya.csta.binding.types.DeviceInstance;
import com.avaya.csta.binding.types.MediaMode;
import com.avaya.csta.registration.RegistrationAdapter;
import com.avaya.csta.registration.RegistrationConstants;
import com.avaya.csta.registration.RegistrationServices;
import com.avaya.csta.terminal.TerminalListener;
import com.avaya.csta.terminal.MediaControlAdapter;
import com.avaya.mvcs.framework.CmapiKeys;
import com.avaya.mvcs.media.audio.Codec;

/**
 * This sample softphone application, registers with Communication Manager 
 * via AEServer using the extension and other parameters defined in the properties
 * file. If a speaker and microphone devices are available the incomming media is 
 * sent to the speaker device and sampled stream from the micorphone is sent to
 * the far end.
 * This application can be used for registering in telecommuter mode
 * by providing proper parameters in softphone.properties file.
 * 
 * Registration is done Asysnchronously using Aysnchronous Services.
 * 
 */

public class Softphone {
	@SuppressWarnings("unused")
	private int numClicks = 0;
	private static final String defaultSkin = "sampleapps/softphone/skin.xml";
	private static SkinSystem skinSystem = new SkinSystem();
	private static MouseHandler handler = new MouseHandler(skinSystem);
	private static JPhoneFrame frame = new JPhoneFrame("Softphone", skinSystem);

	private static final String PROP_FILE_CALL_SERVER = "callserver";
	private static final String PROP_FILE_LOCAL_RTP_ADDR = "localrtpaddr";
	private static final String PROP_FILE_LOCAL_RTP_PORT = "localrtpport";
	private static final String PROP_FILE_EXTENSION = "extension";
	private static final String PROP_FILE_EXT_PASSWORD = "password";
	private static final String PROP_FILE_OLD_REGISTRATION_TYPE = "old_registration_type";
	private static final String PROP_FILE_SHARED = "shared";
        private static final String PROP_FILE_FORCE_LOGIN = "forceLogin";
	private static final String PROP_FILE_CMAPI_SERVER = "cmapi1.server_ip";
	private static final String PROP_FILE_USERNAME = "cmapi1.username";
	private static final String PROP_FILE_PASSWORD = "cmapi1.password";
	private static final String PROP_FILE_SERVER_PORT = "cmapi1.server_port";
	private static final String PROP_FILE_SECURE = "cmapi1.secure";
        private static final String PROP_FILE_TELECOMM_EXTENSION = "telecommuterext";
        private static final String PROP_FILE_MEDIA_CODEC = "codec";
        private static final String PROP_FILE_MEDIA_ENCRYPTION = "encryption";
        private static final String PROP_FILE_E911="e911";
	private static final String PROP_FILE_DEPENDENCY_MODE = "dependency";
	private static final String PROP_FILE_MEDIA_MODE = "media";
	private static final String PROP_FILE_GET_CALL_INFO = "getCallInfo";
	private static final String PROP_DEVICE_INSTANCE = "deviceInstance";
	private static final String PROP_FILE_VALIDATE_PASSWORD = "validatePassword";
	private static final String PROP_FILE_UNICODE_SCRIPTS = "unicodeScripts";

	private PhysicalDeviceServices deviceServices;
	private MonitoringServices montSvcs;
	private CallInformationServices callInfoSvcs;
	private ServiceProvider csta;
    private ServiceProviderListener serviceProviderListener;

	private boolean isRegistered = false;
	private boolean getCallInfo = false;

	private AvayaStation station;

	// The Device ID for our extension number will be supplied by the
	// Communication Manager API. The Connection ID is formulated from the
	// Device ID.

	private DeviceID device = null;
	private ConnectionID connection = null;
	private String switchAddr = "";
	private String localRTPClientAddr = "";
	private int localRTPClientPort = 47212;
	private String extension = "";
	private String password = "";
	private String teleCommuterExt = "";
        private String e911 = "";
	private String codec;
	private String encryption;
	private String unicodeScripts = "";
	private DependencyMode depMode = DependencyMode.MAIN;
	private MediaMode mediaMode = MediaMode.CLIENT;
        private boolean oldRegType = false;
	private boolean shared = false;
        private boolean forceLogin = false;
        private boolean isMultiple = false;
        private DeviceInstance deviceInstance = DeviceInstance.VALUE_0;
    
	public static final int BUTTON_DROP = 0x102;
	public static final int BUTTON_CONFERENCE = 0x103;
	public static final int BUTTON_TRANSFER = 0x104;
	public static final int BUTTON_HOLD = 0x105;

	private MyRegistrationListener registrationListener;
	private MyPhysicalDeviceListener physicalDeviceListener;
	private MyMediaControlListener mediaControlListener;
    
	// The MediaSession and the Audio associated with it
	private MediaSession mediaSession;
	private Audio audio;

	// Microphone and Speaker
	private MediaSource microphone;
	private MediaSink speaker;

	// Assuming there are 40 buttons on button module 1.
	private int CurrentGreenLampsState[] = new int[41];
	private int CurrentRedLampsState[] = new int[41];
	
    private RegistrationServices regSvcs;
    private int negotiatedVersionId = APIProtocolVersion.VERSION_CURRENT_ID;

    // Used to reconnect to the AE Server
    private Properties reconnectProps;
    private MediaInfo localMediaInfo;
    
    private String tcpReceiveBufferSize = null;
    
    MediaFactory mediaFactory = null;
    
	// The latest MediaStartEvent that was registered by the station's
	// MediaControlListener
	// private MediaStartEvent lastMediaStartEvent = null;

    public Softphone(String skinFile) throws Exception {

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
    	String cmapiKeyStoreType;
    	String isValidPeer;
    	String validatePassword;
        String hostnameValidation;
    	
    	ClassLoader cl = this.getClass().getClassLoader();
    	URL defaultURL;

    	if (cl != null){
    		skinSystem.loadSkin(cl.getResource(skinFile).toString());
    		defaultURL = cl.getResource("softphone.properties");
    	}
    	else {
    		URL u = ClassLoader.getSystemResource(skinFile);
    		skinSystem.loadSkin(""+u);
    		defaultURL = ClassLoader.getSystemResource("softphone.properties");
    	}


    	Properties appProp = new Properties();
    	if (defaultURL != null)
    		appProp.load(defaultURL.openStream());

    	switchAddr = appProp.getProperty(PROP_FILE_CALL_SERVER);
    	localRTPClientAddr = appProp.getProperty(PROP_FILE_LOCAL_RTP_ADDR, "local");
    	try {
    		localRTPClientPort = Integer.parseInt(appProp.getProperty(PROP_FILE_LOCAL_RTP_PORT));
    	} catch (NumberFormatException e) {
    		localRTPClientPort = 47212;
    	}
    	extension = appProp.getProperty(PROP_FILE_EXTENSION);
    	password = appProp.getProperty(PROP_FILE_EXT_PASSWORD);
        String oldregtype = appProp.getProperty(PROP_FILE_OLD_REGISTRATION_TYPE);
    	String sharedStr = appProp.getProperty(PROP_FILE_SHARED);
    	String forcelogin = appProp.getProperty(PROP_FILE_FORCE_LOGIN);
    	String dependency = appProp.getProperty(PROP_FILE_DEPENDENCY_MODE);
    	String media = appProp.getProperty(PROP_FILE_MEDIA_MODE);
    	cmapiServerIp = appProp.getProperty(PROP_FILE_CMAPI_SERVER);
    	cmapiUsername = appProp.getProperty(PROP_FILE_USERNAME);
    	cmapiPassword = appProp.getProperty(PROP_FILE_PASSWORD);
    	teleCommuterExt = appProp.getProperty(PROP_FILE_TELECOMM_EXTENSION);
    	codec = appProp.getProperty(PROP_FILE_MEDIA_CODEC, "g711U");
    	encryption = appProp.getProperty(PROP_FILE_MEDIA_ENCRYPTION, "none");
    	validatePassword = appProp.getProperty(PROP_FILE_VALIDATE_PASSWORD, "false");
    	unicodeScripts = appProp.getProperty(PROP_FILE_UNICODE_SCRIPTS, "");
    	hostnameValidation = appProp.getProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE_HOSTNAME);
    	String devInstance = appProp.getProperty(PROP_DEVICE_INSTANCE);
	
    	// If there is no entry then we will assume the connection is secure.
    	cmapiServerPort = appProp.getProperty(PROP_FILE_SERVER_PORT, "4722");
    	cmapiSecure = appProp.getProperty(PROP_FILE_SECURE, "true");
    	cmapiTrustStoreLocation = appProp.getProperty(CmapiKeys.TRUST_STORE_LOCATION);

    	// add following properties for validation client and service side certifications 
    	cmapiTrustStorePassword = appProp.getProperty(CmapiKeys.TRUST_STORE_PASSWORD);
    	cmapiKeyStoreLocation = appProp.getProperty(CmapiKeys.KEY_STORE_LOCATION);
    	cmapiKeyStorePassword = appProp.getProperty(CmapiKeys.KEY_STORE_PASSWORD);
    	cmapiKeyStoreType = appProp.getProperty(CmapiKeys.KEY_STORE_TYPE);
    	isValidPeer = appProp.getProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE);

    	Properties spProp = new Properties();
        spProp.setProperty(CmapiKeys.SESSION_DURATION_TIMER, "240");
        spProp.setProperty(CmapiKeys.SESSION_CLEANUP_TIMER, "120");
    	spProp.setProperty(CmapiKeys.SERVICE_PROVIDER_ADDRESS, cmapiServerIp);
    	spProp.setProperty(CmapiKeys.CMAPI_USERNAME, cmapiUsername);
    	spProp.setProperty(CmapiKeys.CMAPI_PASSWORD, cmapiPassword);
    	spProp.setProperty(CmapiKeys.SERVICE_PROVIDER_PORT, cmapiServerPort);
    	spProp.setProperty(CmapiKeys.SECURE_SERVICE_PROVIDER, cmapiSecure);

    //	spProp.setProperty(CmapiKeys.SESSION_PROTOCOL_VERSION,
    //		APIProtocolVersion.VERSION_4_2);

        // set the list of protocol versions we support.
    	spProp.put(CmapiKeys.SESSION_PROTOCOL_VERSION_LIST, APIProtocolVersion.VERSION_LIST);
        
    	if (cmapiTrustStoreLocation != null) {
    		spProp.setProperty(CmapiKeys.TRUST_STORE_LOCATION,
    				cmapiTrustStoreLocation.trim());
    	}

    	// add following properties for validation client and service side certifications 
    	if(cmapiTrustStorePassword != null){
    		spProp.setProperty(CmapiKeys.TRUST_STORE_PASSWORD, cmapiTrustStorePassword.trim());
    	}
    	if(cmapiKeyStoreLocation != null){
    		spProp.setProperty(CmapiKeys.KEY_STORE_LOCATION, cmapiKeyStoreLocation.trim());
    	}
    	if(cmapiKeyStorePassword != null){
    		spProp.setProperty(CmapiKeys.KEY_STORE_PASSWORD, cmapiKeyStorePassword.trim());
    	}
    	if(cmapiKeyStoreType != null){
    		spProp.setProperty(CmapiKeys.KEY_STORE_TYPE, cmapiKeyStoreType.trim());
    	}
    	if(isValidPeer != null){
    		spProp.setProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE, isValidPeer.trim());
    	}
        if(hostnameValidation != null){
            spProp.setProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE_HOSTNAME, hostnameValidation.trim());
        }

    	if ("true".equals(oldregtype))
    	{
    		oldRegType = true;
    	}
    	if ("true".equals(forcelogin))
    	{
    		forceLogin = true;
    	}
    	if ("true".equals(sharedStr))
    	{
	    	shared = true;
	    }
    	if ("true".equals(appProp.getProperty(PROP_FILE_GET_CALL_INFO)))
    	{
    		getCallInfo = true;
    	}
    	if (dependency == null)
    	{
    		depMode = DependencyMode.MAIN;
    	}
    	else if (dependency.equalsIgnoreCase("MAIN"))
    		depMode = DependencyMode.MAIN;
    	else if (dependency.equalsIgnoreCase("INDEPENDENT"))
    		depMode = DependencyMode.INDEPENDENT;
    	else
    		// default dependency mode is DEPENDENT
    		depMode = DependencyMode.DEPENDENT;

    	if (media == null)
    		mediaMode = MediaMode.CLIENT;
    	else if (media.equalsIgnoreCase("CLIENT"))
    		mediaMode = MediaMode.CLIENT;
    	else if (media.equalsIgnoreCase("SERVER"))
    		mediaMode = MediaMode.SERVER;
    	else if (media.equalsIgnoreCase("NONE"))
    		mediaMode = MediaMode.NONE;
    	else if (media.equalsIgnoreCase("TELECOMMUTER")) {
    		mediaMode = MediaMode.TELECOMMUTER;
    		teleCommuterExt = appProp.getProperty(PROP_FILE_TELECOMM_EXTENSION).trim();
    		e911 = appProp.getProperty(PROP_FILE_E911);
    		if(e911 != null)
    		{
    			e911 = e911.trim();
    		}

    	} else
    		// default media mode is client media
    		mediaMode = MediaMode.CLIENT;

    	// for backward compatibility sake.
    	if(dependency == null && media == null )
    	{
    		if (shared)
    		{
    			depMode = DependencyMode.DEPENDENT;
    			mediaMode = MediaMode.NONE;
    		}
    		else if (teleCommuterExt != null)
    		{

    			depMode = DependencyMode.MAIN;
    			mediaMode = MediaMode.TELECOMMUTER;
    			teleCommuterExt = appProp.getProperty(PROP_FILE_TELECOMM_EXTENSION).trim();
    			e911 = appProp.getProperty(PROP_FILE_E911);
    			if(e911 != null)
    			{
    				e911 = e911.trim();
    			}
    		}
    	}     	

        String sessionMode = appProp.getProperty("sessionMode");
    	if(sessionMode != null && sessionMode.trim().equalsIgnoreCase("MULTIPLE")) {
    		isMultiple = true;
    	}

    	if (!"".equals(devInstance)){
    		if("1".equals(devInstance)){
    			deviceInstance = DeviceInstance.VALUE_1;
    		}
    		else if("2".equals(devInstance)){
    			deviceInstance = DeviceInstance.VALUE_2;
    		}
    		else if("3".equals(devInstance)){
    			deviceInstance = DeviceInstance.VALUE_3;
    		}
    		else if("4".equals(devInstance)){
    			deviceInstance = DeviceInstance.VALUE_4;
    		}
    		else if("5".equals(devInstance)){
    			deviceInstance = DeviceInstance.VALUE_5;
    		}
    		else if("6".equals(devInstance)){
    			deviceInstance = DeviceInstance.VALUE_6;
    		}
    		else if("7".equals(devInstance)){
    			deviceInstance = DeviceInstance.VALUE_7;
    		}
    		else if("8".equals(devInstance)){
    			deviceInstance = DeviceInstance.VALUE_8;
    		}
    		else if("9".equals(devInstance)){
    			deviceInstance = DeviceInstance.VALUE_9;
    		}
    	}
    	
    	// Set the TCP Receive Buffer Size in the property file
    	tcpReceiveBufferSize = appProp.getProperty(CmapiKeys.TCP_RECEIVE_BUFFER_SIZE);
        if (tcpReceiveBufferSize != null){
            spProp.setProperty(CmapiKeys.TCP_RECEIVE_BUFFER_SIZE, tcpReceiveBufferSize);
        }
    	
    	// Get a handle to the ServiceProvider class.
    	csta = ServiceProvider.getServiceProvider(spProp);
        reconnectProps = spProp;

    	String protocol = csta.getNegotiatedAPIVersion();
    	System.out.println("Negotiated protocol version = "+protocol+" : "+APIProtocolVersion.getAPIRelease(protocol));

    	negotiatedVersionId = APIProtocolVersion.getAPIVersionId(protocol);
    	frame.setActionListener(new PhoneListener());

    	// Create new AvayaStation object.
    	station = new AvayaStation();
    	station.init(switchAddr, "", extension, deviceInstance, csta, isMultiple);

    	device = station.getDeviceID();

    	regSvcs = (RegistrationServices) csta.getService(
    			com.avaya.csta.registration.RegistrationServices.class.getName());

    	System.out.println(device.getContent());

    	// A connection ID is needed for requests to data services (tone
    	// detection) and voice unit services (playing and recording files).
    	// It is formulated from the device ID.
    	connection = new ConnectionID();
    	LocalDeviceID lid = new LocalDeviceID();
    	lid.setStaticID(device);
    	connection.setDeviceID(lid);

    	// get the physical device services
    	// deviceServices = csta.getPhysicalDeviceServices();
    	deviceServices = (PhysicalDeviceServices) csta
    	.getService(ch.ecma.csta.physical.PhysicalDeviceServices.class
    			.getName());

    	montSvcs = (MonitoringServices) csta
    	.getService(ch.ecma.csta.monitor.MonitoringServices.class
    			.getName());

    	if( getCallInfo )
    	{
    			callInfoSvcs = (CallInformationServices)csta.getService(
    					CallInformationServices.class.getName());
    	}
    	// Add physical device and registration listeners.
    	addListeners();
    	
    	if( "true".equals(validatePassword))
    	{
    		if( negotiatedVersionId >= APIProtocolVersion.getAPIVersionId(APIProtocolVersion.VERSION_4_2)){

    			ValidateDeviceSecurityCode req = new ValidateDeviceSecurityCode();
    			ValidateDeviceSecurityCodeResponse resp = null;
    			req.setDevice(device);
    			req.setSecurityCode(password);

    			try{
    				resp = regSvcs.validateDeviceSecurityCode(req);
    			}catch (Exception e){
    				System.out.println("validateDeviceSecurityCode request failed resp=" + e.getMessage());
    			}

    			if(resp != null && resp.getResponse() != 0){
    				String [] errStr = resp.getReason().split("-"); 
    				System.out.println("Invalid password in properties file for device=" + device );
    				for(int i=0; i < errStr.length; i++){
    					System.out.println(errStr[i]);
    				}
    			}
    		}
    	}
    	
    	MediaInfo localMediaInfo = new MediaInfo();
    	if (mediaMode == MediaMode.CLIENT) {
    		// Set the Properties like local IPAddress, Min and Max UDP Port
    		// Range,
    		// Default Codec and Default Packet Size if you do not want the
    		// Defaults
    		// chosen by the Stack
    		mediaFactory = MediaFactory.createFactory(null);

    		// Create Audio and MediaSession from the Factory from key
    		if (localRTPClientAddr.equals("local")) {
    			audio = mediaFactory.createAudio(extension);
    			mediaSession = mediaFactory.createMediaSession(extension);
    		} else if (localRTPClientAddr.equals("preferipv6")){
    			localRTPClientAddr = findLocalIPAddress(false);
        		InetSocketAddress userRtpAddr = new InetSocketAddress(localRTPClientAddr, localRTPClientPort);
        		InetSocketAddress userRtcpAddr = new InetSocketAddress(localRTPClientAddr, localRTPClientPort+1);
    			audio = mediaFactory.createAudio(extension, userRtpAddr, userRtcpAddr);
    			mediaSession = mediaFactory.createMediaSession(extension, audio);
    		} else {
        		InetSocketAddress userRtpAddr = new InetSocketAddress(localRTPClientAddr, localRTPClientPort);
        		InetSocketAddress userRtcpAddr = new InetSocketAddress(localRTPClientAddr, localRTPClientPort+1);
    			audio = mediaFactory.createAudio(extension, userRtpAddr, userRtcpAddr);
    			mediaSession = mediaFactory.createMediaSession(extension, audio);
    		}

    		// Create a Source and attach it to the MediaSession
    		microphone = (MediaSource) new TutorialMicrophone();
    		mediaSession.attachSource(microphone);

    		// Create a Sink and attach it to the MediaSession
    		speaker = (MediaSink) new TutorialSpeaker();
    		mediaSession.attachSink(speaker);

    		// Create Local RTP Address. The local RTP/RTCP Address
    		// can be retrieved from Audio (RTP Stack).
    		IPAddress localRtpAddress = new IPAddress();
    		localRtpAddress.setAddress(audio.getLocalRTPAddress().getAddress()
    				.getHostAddress());
    		localRtpAddress.setPort(new Integer(audio.getLocalRTPAddress()
    				.getPort()));
    		localMediaInfo.setRtpAddress(localRtpAddress);

    		// Create Local RTCP Address
    		IPAddress localRtcpAddress = new IPAddress();
    		localRtcpAddress.setAddress(audio.getLocalRTCPAddress()
    				.getAddress().getHostAddress());
    		localRtcpAddress.setPort(new Integer(audio.getLocalRTCPAddress()
    				.getPort()));
    		localMediaInfo.setRtcpAddress(localRtcpAddress);
    	}

		// Create Local Codec and set default as G.711U
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
		else if (Audio.G723.equalsIgnoreCase(codec)) { // N.B. g723 is only valid
			// for client media mode
			codecList = new String[] { Audio.G723 };
			packetSize = Codec.G723_PACKET_SIZE;
		} else {
			// default
			codec = Audio.G711U;
			codecList = new String[] { Audio.G711U };
		}
		localMediaInfo.setCodecs(codecList);
		
		if (microphone != null)
		    microphone.setCodec(codec, packetSize); // N.B. check that microphone & speaker
		if (speaker != null)
		    speaker.setCodec(codec, packetSize);    // support the codec that you intend to use.
		// Currently, TutorialMicrophone & TutorialSpeaker
		// only support G.711U & G.711A codecs.

        // check and set the optional media encryption settings
        if (Audio.SRTP_AES128_HMAC32_ENC_AUTH.equalsIgnoreCase(encryption) ||
        	Audio.SRTP_AES128_HMAC80_ENC_AUTH.equalsIgnoreCase(encryption) ||
        	Audio.SRTP_AES128_HMAC32_UNENC_AUTH.equalsIgnoreCase(encryption) ||
        	Audio.SRTP_AES128_HMAC80_UNENC_AUTH.equalsIgnoreCase(encryption)) {
        	System.out.println("This application does not support media encryption type="+encryption);
            removeListeners();
            station.cleanup();
            csta.disconnect(true);
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

		System.out.println("setupAudio" 
				+ ": codec=" + localMediaInfo.getCodecs()[0]
			    + ", encryption=" + localMediaInfo.getEncryptionList()[0]);
	
    	try {
    	    if (oldRegType) {
    		// add the TerminalListener before registering
    		station.addListener(new MyTerminalListener());
    
    		// Use deprecated registerDevice
    		station.register(password, shared, null);
    	    } else {
        		// Use asynchronous registerTerminal since we don't need to block this
        		// thread waiting for the response.
        		station.register(password, depMode, mediaMode, teleCommuterExt, e911, forceLogin, 
        				localMediaInfo, unicodeScripts, new MyAsyncRegistrationCallback());
    	    }
    	}
    	catch (Exception e){
    		System.out.println(e.getMessage());
			System.out.println("Terminating application");
    		cleanup();
    		System.exit(0);
    	}
    }

    public String findLocalIPAddress(boolean preferIPv6) {
        String cleanIPAddr = "";
        String token = "%\\d";
        String eth, localIPv6 = null, localIPv4 = null;

	try {
	Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

	while(networkInterfaces.hasMoreElements()) {
		NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
		eth = ni.getName();

		Enumeration<InetAddress> networkAddresses = ni.getInetAddresses();

		while (networkAddresses.hasMoreElements()){
			InetAddress ip = (InetAddress) networkAddresses.nextElement();

			// Filter out link-local and loop back addresses as they are not valid
			if(!eth.equals("lo") && !ip.isLinkLocalAddress()) {
				Pattern pattern = Pattern.compile(token);
				Matcher matcher = pattern.matcher(ip.getHostAddress());
				cleanIPAddr = matcher.replaceAll("");

				if (cleanIPAddr.replaceAll("[^:]","").length() > 1)
					localIPv6 = cleanIPAddr;
				if (cleanIPAddr.replaceAll("[^.]","").length() > 1)
					localIPv4 = cleanIPAddr;
			}
		}
	}
	} catch (Exception e) {
		e.printStackTrace();
	}
	if (preferIPv6 && localIPv6 != null ) {
		return localIPv6;
	} else {
		return localIPv4;
	}
    }

	public Component createComponents() {
		// size the window using the outermost boundaries of the currently
		// loaded skin
		Point outerBounds = skinSystem.getOuterBounds();
		JPanel pane = new JPanel();
		pane.setBorder(BorderFactory.createEmptyBorder(
                0, // top
				0, // left
				outerBounds.y, // bottom
				outerBounds.x) // right
				);
		pane.setLayout(new GridLayout(0, 1));
		return pane;
	}

	public static void main(String[] args) throws Exception {

		String skinFile = defaultSkin;
		if (args.length >= 1) {
			if (args[0].equals("-h") || args[0].equals("-?")) {
				System.out.println("Usage: Softphone skinfile\n");
				System.out.println("       Example: Softphone sampleapps/softphone/big_skin.xml");
				System.out.println("       (defaults to " + defaultSkin + ")");
				return;
			} else {
				skinFile = args[0];
			}
		}

		try {
			UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
		}

		frame.addMouseListener(handler);
		frame.addMouseMotionListener(handler);
		frame.setResizable(false);
		frame.setLocation(100, 100);
		frame.setUndecorated(true);

		Softphone app = new Softphone(skinFile);
		Component contents = app.createComponents();
		frame.getContentPane().add(contents, BorderLayout.CENTER);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				return;
			}
		});
		frame.pack();
		
	}

	/**
	 * Creates listener objects for the different services and adds them so the
	 * app will be informed of events coming from the various services.
	 */
	private void addListeners() throws CstaException, Exception {
		// Add an AvayaStationListner to receive events indicating phone rings,
		// lamp and display updates.
		physicalDeviceListener = new MyPhysicalDeviceListener();
		montSvcs.addPhysicalDeviceListener(device, physicalDeviceListener);

		// Add a listener so we can receive events indicating when the phone is
		// registered or unregistered.
		registrationListener = new MyRegistrationListener();
		montSvcs.addRegistrationListener(device, registrationListener);

		if (mediaMode == MediaMode.CLIENT) {
			// Add a listener to receive Media Events
			mediaControlListener = new MyMediaControlListener();
			montSvcs.addMediaControlListener(device, mediaControlListener);
		}

        // Add a service provider listener to receive events indicating
        // the state of the server connection or session.  
        serviceProviderListener = new MyServiceProviderListener();
        csta.addServiceProviderListener(serviceProviderListener);  
	}

	/**
	 * Removes all of the listeners that were added for the various services.
	 */
	private void removeListeners() throws CstaException {
		montSvcs.removePhysicalDeviceListener(device, physicalDeviceListener);
		montSvcs.removeRegistrationListener(device, registrationListener);
		if (mediaMode == MediaMode.CLIENT) {
			montSvcs.removeMediaControlListener(device, mediaControlListener);
		}
	}

	/**
	 * This method assures us that we unregister our extension, remove all the
	 * listeners, stop collecting DTMF tones and stop any announcements or
	 * recordings that were happening. The connector server will cleanup if a
	 * client goes away without doing this cleanup, but it's best to be safe.
	 */
	private void cleanup() {
		System.out.println("The application is terminating: clean up.");

		// There is a chance that AvayaStation has already cleaned up as a
		// result of getting the unregistered event. In this case, the station
		// is already unregistered, the device ID has been released, and the
		// server should have cleaned up. Don't bother doing anything else.
		if (station.getDeviceID() == null) {
			return;
		}

		try {
			// Cleanup Local audio
			if (audio != null) {
				audio.cleanup();
			}

			// Make sure no listeners were left around.
			removeListeners();

			// Call the synchronous version of unregister() because we want the
			// unregister to succeed before cleaning up and disconnecting.
            if(isMultiple) {
                try {
                    station.cleanup();
                } catch(InvalidDeviceStateException idse) {
                	if(isRegistered){
                		station.unregister();
                	}
                    station.cleanup();                    
                }
            }
            else {
            	if(isRegistered){
            		station.unregister();
            	}
                station.cleanup();
            }
			// provider.disconnect will disconnect from the server and
			// terminate the active session.
			csta.disconnect(true);
			// release all resources associated with service provider
			// e.g, worker threads, timer threads etc...
			csta.stopServiceProvider();
			// release the NIO-ChannelServicer thread associated 
			// with media factory by calling the shutdown.
			if( mediaFactory != null){
			    mediaFactory.shutdown();
			}
		} catch (Exception e) {
			System.out.println("Could not clean up properly");
			e.printStackTrace(System.out);
		}
	}
	
	private class MyTerminalListener implements TerminalListener {
		public void registered(RegisteredEvent event) {
			System.out.println("Softphone registered");
			frame.setVisible(true);
			isRegistered = true;
			
			GetButtonInformation request = new GetButtonInformation();
			request.setDevice(device);
			try {
				GetButtonInformationResponse resp = deviceServices.getButtonInformation(request);
				ButtonList list = resp.getButtonList();
				ButtonList termList = new ButtonList();
				System.out.println("Button Count: " + list.getButtonItemCount());
				for (int i=0; i < list.getButtonItemCount(); i++) {
					ButtonItem button = list.getButtonItem(i);
					
					int buttonNum = Integer.parseInt(button.getButton());
					int module = (buttonNum & 0x700) >> 8;
					buttonNum = buttonNum & 0xFF;
					
					// send terminal module buttons to the frame
					if (module == 1)
						termList.addButtonItem(button);
					
					String label = "";
					
					try {
						label = ButtonTypes.getButtonTypes(Integer.parseInt(button.getButtonFunction())).getName();
					} catch (Exception e) {
						label = "Unknown";
					}
					
					System.out.println("Button: [module: " + module + " number: " + buttonNum  + "\tfunction: " + button.getButtonFunction() + "\tname: " + label + "]");
				}
				
				frame.setButtonList(termList);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    
		public void registerFailed(RegisterFailedEvent event) {
			System.out.println("Softphone registerFailed: " + event.getReason());
			System.exit(0);
		}
    
		public void unregistered(UnregisterEvent event) {
			System.out.println("Softphone unregistered");
			isRegistered = false;
			System.exit(0);
		}
	}
    

	private class MyPhysicalDeviceListener extends PhysicalDeviceAdapter {

		/**
		 * Method is fired when a Display update is received
		 * 
		 * @param event
		 *            the DisplayUpdatedEvent
		 */
		public void displayUpdated(DisplayUpdatedEvent event) {
			
			System.out.println("displayUpdate: for:" + extension + "'"
				+ event.getContentsOfDisplay() + "'");
			
			//	show the extension if the display is blank
			String display = "";
			if (event.getContentsOfDisplay() != null && !event.getContentsOfDisplay().equals("")){
				display = event.getContentsOfDisplay();
				removeSpaces(display);
			}
			if("".equals(display))
			{
				display = extension;
			}
			
			frame.drawDisplayText(display);
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
			boolean green = false;
			String lampType = "red  ";
			if (event.getLampColor().intValue() == 3) {
				green = true;
				lampType = "green";
			}
			String[] state = { "BROKENFLUTTER", "FLUTTER", "OFF", "STEADY",
					"WINK", "XXXXX", "INVERTEDWINK", "FLASH", "INVERTEDFLASH" };

			String lampState = "UNKNOWN";

			if (event.getLampMode() < 8) {
				lampState = state[newLampState];
			}
			// we only care about rendering terminal module lamps to the screen
			if (module == 1) {
				if (green) {
					if (CurrentGreenLampsState[lamp] != newLampState) {
						CurrentGreenLampsState[lamp] = newLampState;
						frame.updateLamp(lamp, true, newLampState);
					}
				} else {
					if (CurrentRedLampsState[lamp] != newLampState) {
						CurrentRedLampsState[lamp] = newLampState;
						frame.updateLamp(lamp, false, newLampState);
					}
				}
			}

			System.out.println(" lampUpdate [module: " + module
					+ " buttonNum: " + lamp + " lampType: " + lampType
					+ " state: " + lampState + "]");
		}

		/**
		 * Method is fired when a RingerStatusEvent is received
		 * 
		 * @param event
		 *            the received RingerStatusEvent
		 */
		public void ringerStatusUpdated(RingerStatusEvent event) {
			System.out.println("ringerUpdate: " + event.getRingPattern());
		}

		public void hookswitchChanged(HookswitchEvent event) {
			System.out.println("hookswitchUpdate: "
					+ event.getHookswitchOnHook());
			frame.setHookState(event.getHookswitchOnHook().booleanValue());
		}

		public void serviceLinkStatusChanged(ServiceLinkStatusEvent event) {
            System.out.println("ServiceLinkStatusEvent: "
                   	+ event.getServiceLinkUp());
            //frame.setHookState(event.getServiceLinkUp().booleanValue());
        }

		private String removeSpaces(String input) {
			if (input == null || input.equals(""))
				return input;
			StringTokenizer tokenizer = new StringTokenizer(input, " ", false);
			String output = "";

			while (tokenizer.hasMoreElements())
				output += tokenizer.nextElement();

			return output;

		}
	}

	private class PhoneListener implements ActionListener {

		public PhoneListener() {
		}

		/**
		 * Note: All the responses/events are delivered on one thread. This
		 * guarantees sequential processing of events/responses. However if the
		 * processing of an event involves an I/O as this function does, you may
		 * want to process this event in a different thread. One approach would
		 * be to have one separate thread processing events and responses off of
		 * a queue. All Listeners would queue events/responses to this queue if
		 * events/responses are either blocking or require sequential
		 * processing.
		 */

		public void actionPerformed(ActionEvent event) {

			// don't try to process commands if we're not registered
			if (!isRegistered)
				return;

			try {
				String command = event.getActionCommand();

				if (command.equals("handset")) {
					// System.out.println("changing hook state! (" +
					// !frame.getHookState() + ")");
					SetHookswitchStatus hookStateRequest = new SetHookswitchStatus();
					hookStateRequest.setDevice(device);
					hookStateRequest.setHookswitch("0");
					hookStateRequest.setHookswitchOnhook(new Boolean(!frame
							.getHookState()));
					deviceServices.setHookswitchStatus(hookStateRequest);
					frame.setHookState(!frame.getHookState());
				} else if (command.startsWith("keypad")) {
					// dial me some digits
					int index = command.indexOf(".") + 1;
					pushButton(Integer.parseInt(command.substring(index)));
				} else if (command.startsWith("feat")) {
					int index = command.indexOf(".") + 1;
					int button = Integer.parseInt(command.substring(index));
					button = (1 << 8) + button;
					pushButton(button);
				} else if (command.equals("drop")) {
					pushButton(BUTTON_DROP);
				} else if (command.equals("conf")) {
					pushButton(BUTTON_CONFERENCE);
				} else if (command.equals("trans")) {
					pushButton(BUTTON_TRANSFER);
				} else if (command.equals("hold")) {
					pushButton(BUTTON_HOLD);
				} else if (command.equals("exit")) {
					System.out.println("Terminating application");
					cleanup();
					System.exit(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/**
		 * Note: All the responses/events are delivered on one thread. This
		 * guarantees sequential processing of events/responses. However if the
		 * processing of an event involves an I/O as this function does, you may
		 * want to process this event in a different thread. One approach would
		 * be to have one separate thread processing events and responses off of
		 * a queue. All Listeners would queue events/responses to this queue if
		 * events/responses are either blocking or require sequential
		 * processing.
		 */
		public void pushButton(int button) {
			try {
				ButtonPress bpRequest = new ButtonPress();
				bpRequest.setDevice(device);
				bpRequest.setButton(Integer.toString(button));
				deviceServices.pressButton(bpRequest);
			} catch (Exception e) {
				e.printStackTrace();
			}
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

				System.out.println(" response code " + regResp.getCode());
				if (regResp.getCode().equals(RegistrationConstants.NORMAL_REGISTER)) {
					registered(regResp);				
				} else {
					registerFailed(regResp);
				}
			}
			else {
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

			System.out.println("Received Asynchronous Exception from Server. The stack trace from the Server is as follows");
			exception.printStackTrace();
			System.out.println("Terminating application");
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
			System.out.println("Softphone registered");
			frame.setVisible(true);
			isRegistered = true;

			GetButtonInformation request = new GetButtonInformation();
			request.setDevice(device);
			try {
				GetButtonInformationResponse resp = deviceServices
				.getButtonInformation(request);
				ButtonList list = resp.getButtonList();
				ButtonList termList = new ButtonList();
				System.out
				.println("Button Count: " + list.getButtonItemCount());
				for (int i = 0; i < list.getButtonItemCount(); i++) {
					ButtonItem button = list.getButtonItem(i);

					int buttonNum = Integer.parseInt(button.getButton());
					int module = (buttonNum & 0x700) >> 8;
				buttonNum = buttonNum & 0xFF;

				// send terminal module buttons to the frame
				if (module == 1)
					termList.addButtonItem(button);

				String label = button.getButtonLabel();


				if("".equals(label))
				{

					try {
						label = ButtonTypes.getButtonTypes(
								Integer.parseInt(button.getButtonFunction()))
								.getName() + " CONSTRUCTED";
					} catch (Exception e) {
						label = "Unknown";
					}
				}

				System.out.println("Button: [ module: " + module
						+ " number: " + buttonNum + "\tfunction: "
						+ button.getButtonFunction() + "\tname: " + label
						+ " ]");
				}

				frame.setButtonList(termList);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if( getCallInfo )
			{
				GetCallInformationResponse callInformation = null;
				
				if ( callInfoSvcs != null) {
					GetCallInformation req = new GetCallInformation();
					req.setExtension(extension);
					req.setDevice(device);

					try {
						callInformation = callInfoSvcs.getCallInformation(req);
					} catch (CstaException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (callInformation !=null) {
					String stationName = callInformation.getStationName();

					System.out.println("User Name: " + stationName);
					frame.drawDisplayText(stationName);
				}
			}

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
			System.out.println("Registration failed: reason " + resp.getReason());
			System.out.println("Terminating application");
			cleanup();
			System.exit(0);
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
		 * @param event
		 *            the TerminalUnregisteredEvent
		 */
		public void terminalUnregistered(TerminalUnregisteredEvent event) {
			System.out.println("Received unregistered event: reason " + event.getReason());
			System.out.println("Terminating application");
			cleanup();
			System.exit(0);
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
				InetAddress remoteAddress = InetAddress.getByName(startEvent
						.getRtpAddress().getAddress());
				int remoteRtpPort = startEvent.getRtpAddress().getPort()
						.intValue();
				int remoteRtcpPort = startEvent.getRtcpAddress().getPort()
						.intValue();
				String remoteCodec = startEvent.getCodec();

				MediaEncryption remoteEncryption = new MediaEncryption();
				remoteEncryption.setProtocol(startEvent.getEncryption()
						.getProtocol());
				remoteEncryption.setTransmitKey(startEvent.getEncryption()
						.getTransmitKey());
				remoteEncryption.setReceiveKey(startEvent.getEncryption()
						.getReceiveKey());
				remoteEncryption.setPayloadType(startEvent.getEncryption()
						.getPayloadType().intValue());

				int packetSize = startEvent.getPacketSize().intValue();

				System.out.println(" starting Audio: remote=" + remoteAddress
						+ ":" + remoteRtpPort + ", codec=" + remoteCodec
						+ ", packetsize=" + packetSize + ", protocol="
						+ remoteEncryption.getProtocol());

				// Start Audio on the Client Media Stack
				// with the Far End Audio Information
				audio.start(
						new InetSocketAddress(remoteAddress, remoteRtpPort),
						new InetSocketAddress(remoteAddress, remoteRtcpPort),
						remoteCodec, packetSize, remoteEncryption);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public void mediaStopped(MediaStopEvent stopEvent) {
			try {
				System.out.println(" stopping Audio");
				// Stop Audio on the Client Media Stack
				audio.stop();

				// Stop the Speaker, since audio has stopped
        		if (speaker != null)
				    speaker.close();

				// Stop the Microphone, since audio has stopped
        		if (microphone != null)
				    microphone.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
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
            csta = ServiceProvider.getServiceProvider(reconnectProps);

            // Re-initialize the station obj.
            station = null;
             
            station = new AvayaStation();
        	station.init(switchAddr, "", extension, deviceInstance, csta, isMultiple);
        
            // The first thing that we do is to get a Device ID. The Device ID
            // is used in the requests to the services. Calling this method
            // populates the id field of the Click2Call class.
            device = station.getDeviceID();

            // Add appropriate listeners to listen to events during and after
            // registration.
            addListeners();

            // Registers an extension in shared control mode for use as a
            // CMAPI softphone station (IP_API_A) with Communication Mananger
            // and the connector server. Setting the second parameter to true
            // allows for shared control.
            station.register(password, depMode, mediaMode, teleCommuterExt, e911, forceLogin, 
                    localMediaInfo, unicodeScripts, new MyAsyncRegistrationCallback());
        }    
        else
        {
            throw new Exception("bootstrap() must be called before recoverFromTerminatedSession()" +
                                " can be called.");
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
            
            if (event.getCode() == ServerConnectionDownEvent.DISCONNECT_APP_INITIATED)
            { 
                return;
            }
            
            // Disconnect was not app-initiated, so try to recover the session
    		for (int j=0; j<3; j++) {
                try
                {
                    Thread.sleep(30000);
                    System.out.println("Attempting to reconnect.");
                    csta.reconnect();               
                }
                catch (ConnectException ce)
                {
		            continue;
                }
                catch (IOException ce)
                {
		            continue;
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
                csta.disconnect(false);
                // reconnect to the server
                Thread.sleep(30000);
                csta.reconnect();
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
