/*
 * AvayaStation.java
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

package sampleapps.station;

// Java utilities
import java.util.EventListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.event.EventListenerList;

import ch.ecma.csta.binding.ButtonInformationEvent;
import ch.ecma.csta.binding.ButtonItem;
import ch.ecma.csta.binding.ButtonList;
import ch.ecma.csta.binding.ButtonPress;
import ch.ecma.csta.binding.ButtonPressEvent;
import ch.ecma.csta.binding.DeviceID;
import ch.ecma.csta.binding.DisplayUpdatedEvent;
import ch.ecma.csta.binding.GetButtonInformation;
import ch.ecma.csta.binding.GetButtonInformationResponse;
import ch.ecma.csta.binding.HookswitchEvent;
import ch.ecma.csta.binding.LampModeEvent;
import ch.ecma.csta.binding.MessageWaitingEvent;
import ch.ecma.csta.binding.MicrophoneGainEvent;
import ch.ecma.csta.binding.MicrophoneMuteEvent;
import ch.ecma.csta.binding.RingerStatusEvent;
import ch.ecma.csta.binding.SetHookswitchStatus;
import ch.ecma.csta.binding.SpeakerMuteEvent;
import ch.ecma.csta.binding.SpeakerVolumeEvent;
import ch.ecma.csta.binding.SubjectDeviceID;
import ch.ecma.csta.callcontrol.CallControlListener;
import ch.ecma.csta.errors.CstaException;
import ch.ecma.csta.monitor.MonitoringServices;
import ch.ecma.csta.physical.PhysicalDeviceListener;
import ch.ecma.csta.physical.PhysicalDeviceServices;

import com.avaya.cmapi.ServiceProvider;
import com.avaya.csta.async.AsynchronousCallback;
import com.avaya.csta.async.AsynchronousServices;
import com.avaya.csta.binding.E911CallBlockedEvent;
import com.avaya.csta.binding.GetDeviceId;
import com.avaya.csta.binding.GetDeviceIdResponse;
import com.avaya.csta.binding.GetRegistrationState;
import com.avaya.csta.binding.GetRegistrationStateResponse;
import com.avaya.csta.binding.LoginInfo;
import com.avaya.csta.binding.MediaInfo;
import com.avaya.csta.binding.RegisterDevice;
import com.avaya.csta.binding.RegisterTerminalRequest;
import com.avaya.csta.binding.RegisterTerminalResponse;
import com.avaya.csta.binding.ReleaseDeviceId;
import com.avaya.csta.binding.ServiceLinkStatusEvent;
import com.avaya.csta.binding.UnregisterTerminalRequest;
import com.avaya.csta.binding.types.DependencyMode;
import com.avaya.csta.binding.types.DeviceInstance;
import com.avaya.csta.binding.types.MediaMode;
import com.avaya.csta.binding.types.RegistrationState;
import com.avaya.csta.device.DeviceServices;
import com.avaya.csta.physical.ButtonFunctionConstants;
import com.avaya.csta.physical.ButtonIDConstants;
import com.avaya.csta.physical.LampModeConstants;
import com.avaya.csta.registration.RegistrationConstants;
import com.avaya.csta.registration.RegistrationListener;
import com.avaya.csta.registration.RegistrationServices;
import com.avaya.csta.terminal.TerminalListener;
import com.avaya.csta.terminal.TerminalServices;
import com.avaya.csta.terminal.MediaControlListener;


/**
 * This implementation is an example of how to
 * abstract your application somewhat from the CSTA based interface of
 * Communication Manager API. If this doesn't exactly suit your needs, feel
 * free to modify the code to do whater you need.
 * 
 * @author AVAYA, Inc.
 * @version $Revision: 1.9 $
 */

@SuppressWarnings("deprecation")
public class AvayaStation
{
    // Some of the fields in requests are required by CSTA, but don't have any
    // significance to Communication Manager or the API. This default value
    // is used in those instances.
    private static final String DEFAULT_VALUE = "0000";

    // This hashtable is used to track the IDs of all call appearance
    // buttons / lamps, as well as the lamp status.
    private Map<String, Short> callAppearanceLamps = new TreeMap<String, Short>();

    // These fields are read in from the application.properties file. This file
    // must be populated with a valid IP address for Avaya Communication
    // Manager, as well as a valid extension number and password for the
    // application softphone.
    private String callServerName = null;
    
    private String callServerAddress = null;
    private String extension = null;
    private DeviceInstance deviceInstance = DeviceInstance.VALUE_0;
    private ServiceProvider provider;

    // The Device ID for our extension number will be supplied by the
    // Communication Manager API. The Connection ID is formulated from the
    // Device ID.
    private DeviceID deviceID = null;
    // These fields will be populated with the handles of the various services,
    // which we will get through the ServiceProvider class.
    private DeviceServices devSvcs;
    
    private MyPhysicalDeviceListener physicalDevListener;

    
    private EventListenerList listenerList;
    private PhysicalDeviceServices physSvcs;
    // MonitoringServices will be used to add/remove terminal listener and  
    // media control listener.
    private MonitoringServices montSvcs;
    
    private TerminalServices termSvcs;
    private RegistrationServices regSvcs;
    private AsynchronousServices asyncSvcs;

    private boolean isMultiple = false;
    
        
    /**
     * Initializes the class with the IP address of the Communication Manager,
     * the extension number to be used, and the handle to the ServiceProvider
     * that the application got by calling ServiceProvider.getServiceProvider().
     *
     * It is important that the application only uses one ServiceProvider,
     * therefore the AvayaStation implementation cannot simply call
     * getServiceProvider on its own.
     * 
     * If the calling application is using CallInformationServices, the OAM
     * web pages must be administered with a valid Switch Connection and at
     * least one valid H.323 Gatekeeper for this init method to work properly.
     *
     * @param callServerAddress - The IP address of the Communication Manager
     * @param extension - The extension number corresponding to this station
     * @param provider - The handle to the ServiceProvider the application is
     *                      using.
     */
    public void init(String callServerAddress, String extension,
            ServiceProvider provider) throws Exception
    {
        init(callServerAddress, extension, DeviceInstance.VALUE_0, provider);
    }
    
    public void init(String callServerAddress, String extension,
                     DeviceInstance instance, ServiceProvider provider) throws Exception
    {
        if ((callServerAddress == null) || (extension == null)
                || (provider == null)) {
            throw new Exception("Call Server Address, extension number or " +
                    "provider is null.");
        }

        this.callServerAddress = callServerAddress;
        this.extension = extension;
        this.deviceInstance = instance;
        this.provider = provider;
        init();
    }

    /**
     * Initializes the class with the IP address of the Communication Manager,
     * the extension number to be used, and the handle to the ServiceProvider
     * that the application got by calling ServiceProvider.getServiceProvider().
     * 
     * It is important that the application only uses one ServiceProvider,
     * therefore the AvayaStation implementation cannot simply call
     * getServiceProvider on its own.
     * 
     * If the calling application uses this version of the init method, there is
     * no need to administer any H.323 Gatekeepers in the OAM pages.
     * 
     * @param callServerName - The name of the Communication Manager, as
     *      administered on the Switch Connection OAM web pages.
     * @param callServerAddress - The IP address of the Communication Manager
     * @param extension - The extension number corresponding to this station
     * @param provider - The handle to the ServiceProvider the application is
     *                      using.
     */
    public void init(String callServerName, String callServerAddress,
            String extension, ServiceProvider provider) throws Exception
    {
        init(callServerName, callServerAddress, extension, DeviceInstance.VALUE_0, provider);
    }
    
    public void init(String callServerName, String callServerAddress,
                     String extension, DeviceInstance instance, ServiceProvider provider) throws Exception
    {
        
        if ((callServerName == null) || (callServerAddress == null) ||
                (extension == null) || (provider == null)) {
            throw new Exception("Call Server Name, Call Server Address, " +
                    "extension number or provider is null.");
        }

        this.callServerName = callServerName;
        this.callServerAddress = callServerAddress;
        this.extension = extension;
        this.deviceInstance = instance;
        this.provider = provider;
        init();
    }

    /**
     * Initializes the class with the IP address of the Communication Manager,
     * the extension number to be used, and the handle to the ServiceProvider
     * that the application got by calling ServiceProvider.getServiceProvider().
     * 
     * It is important that the application only uses one ServiceProvider,
     * therefore the AvayaStation implementation cannot simply call
     * getServiceProvider on its own.
     * 
     * If the calling application uses this version of the init method, there is
     * no need to administer any H.323 Gatekeepers in the OAM pages.
     * 
     * @param callServerAddress - The IP address of the Communication Manager
     * @param extension - The extension number corresponding to this station
     * @param provider - The handle to the ServiceProvider the application is
     *                      using.
     * @param isMultiple - If true, then the DeviceID can be controlled by 
     *                     multiple sessions.
     */  
    public void init(String callServerAddress, String extension, 
            ServiceProvider provider, boolean isMultiple) throws Exception {
        init(callServerAddress, extension, DeviceInstance.VALUE_0, provider, isMultiple);
    }
    
    public void init(String callServerAddress, String extension, DeviceInstance instance,
                     ServiceProvider provider, boolean isMultiple) throws Exception {
        this.isMultiple = isMultiple;
        init(callServerAddress, extension, instance, provider);
    }

    /**
     * Initializes the class with the IP address of the Communication Manager,
     * the extension number to be used, and the handle to the ServiceProvider
     * that the application got by calling ServiceProvider.getServiceProvider().
     * 
     * It is important that the application only uses one ServiceProvider,
     * therefore the AvayaStation implementation cannot simply call
     * getServiceProvider on its own.
     * 
     * If the calling application uses this version of the init method, there is
     * no need to administer any H.323 Gatekeepers in the OAM pages.
     * 
     * @param callServerName - The name of the Communication Manager, as
     *                         administered on the Switch Connection OAM web pages.
     * @param callServerAddress - The IP address of the Communication Manager
     * @param extension - The extension number corresponding to this station
     * @param provider - The handle to the ServiceProvider the application is using.
     * @param isMultiple - If true, then the DeviceID can be controlled by 
     *                     multiple sessions.
     */  
    public void init(String callServerName, String callServerAddress, String extension,
                     ServiceProvider provider, boolean isMultiple) 
        throws Exception {
        init(callServerName, callServerAddress, extension, DeviceInstance.VALUE_0, provider, isMultiple);
    }
    
    public void init(String callServerName, String callServerAddress, String extension,
                     DeviceInstance instance, ServiceProvider provider, boolean isMultiple) 
        throws Exception {
        this.isMultiple = isMultiple;
        init(callServerName, callServerAddress, extension, instance, provider);
    }
    
    //**************************************************************************
    // initByName
    //**************************************************************************
    /**
     * Initializes the class with the IP address of the Communication Manager,
     * the extension number to be used, and the handle to the ServiceProvider
     * that the application got by calling ServiceProvider.getServiceProvider().
     * 
     * It is important that the application only uses one ServiceProvider,
     * therefore the AvayaStation implementation cannot simply call
     * getServiceProvider on its own.
     * 
     * The OAM web pages must be administered with a valid Switch Connection and
     * at least one valid H.323 Gatekeeper for this init method to work
     * properly.  The AE Services server will resolve the switch name to an
     * H.323 address for the switch using this information. 
     * 
     * If the calling application uses this version of the init method, there is
     * no need to administer any H.323 Gatekeepers in the OAM pages.
     * 
     * @param callServerName - The name of the Communication Manager, as
     *      administered on the Switch Connection OAM web pages.
     * @param extension - The extension number corresponding to this station
     * @param provider - The handle to the ServiceProvider the application is
     *                      using.
     */
    public void initByName(String callServerName, String extension,
            ServiceProvider provider) throws Exception
    {
        initByName(callServerName, extension, DeviceInstance.VALUE_0, provider);
    }
    
    public void initByName(String callServerName, String extension,
                           DeviceInstance instance, ServiceProvider provider) throws Exception
    {
        if ((callServerName == null) || (extension == null)
                || (provider == null)) {
            throw new Exception("Call Server Name, extension number or " +
                    "provider is null.");
        }

        this.callServerName = callServerName;
        this.extension = extension;
        this.deviceInstance = instance;
        this.provider = provider;
        init();
    }
    
    //**************************************************************************
    // init
    //**************************************************************************
    /**
     * Gets handles to all of the services, gets the deviceID and adds
     * listeners.
     *
     *  add description
     */
    private void init() throws Exception
    {
        // Get handles to all of the services that are used later on.
        devSvcs =
            (DeviceServices) provider.getService(
                com.avaya.csta.device.DeviceServices.class.getName());

        physSvcs =
            (PhysicalDeviceServices) provider.getService(
                ch.ecma.csta.physical.PhysicalDeviceServices.class.getName());
        montSvcs = (MonitoringServices) provider.getService(
                ch.ecma.csta.monitor.MonitoringServices.class.getName());
        termSvcs = (TerminalServices) provider.getService(
        		com.avaya.csta.terminal.TerminalServices.class.getName());
        regSvcs = (RegistrationServices) provider.getService(
        		com.avaya.csta.registration.RegistrationServices.class.getName());
        asyncSvcs = (AsynchronousServices) provider.getService(
                com.avaya.csta.async.AsynchronousServices.class.getName());
        
        // The first thing that we do is to get a Device ID. This ID is used in
        // most other requests to the services. Calling this method populates
        // the id field of the AvayaStationImpl class.
        getID();

        // Add listeners to TerminalServices and PhysicalDeviceServices to be
        // notified of events coming from those services.
        addListeners();

        // Create the EventListenerList.
        listenerList = new EventListenerList();
    }
    
    /**
     * Allows the application to add an AvayaStationListener, TerminalListener
     * or MediaListener. PhysicalDeviceListener is not required, since
     * AvayaStationListener is an extension of PhysicalDeviceListener.
     * 
     * @param listener -
     *            The listener to be added. Must be one of the 3 types listed
     *            above.
     */
    public void addListener(EventListener listener) throws CstaException
    {   	
        if (listener == null)
        {
            throw new CstaException("Listener is null.");
        }

        if (listenerList == null)
        {
            throw new CstaException("Must call init() before addListener().");
        }

        if (listener instanceof AvayaStationListener)
        {
            listenerList.add(AvayaStationListener.class, (AvayaStationListener)listener);
        }
        else if(listener instanceof CallControlListener) {
            montSvcs.addCallControlListener(deviceID, (CallControlListener) listener);
        }
        else if(listener instanceof TerminalListener){
            montSvcs.addTerminalListener(deviceID, (TerminalListener) listener);
        }
        else if(listener instanceof RegistrationListener){
            montSvcs.addRegistrationListener(deviceID, (RegistrationListener) listener);
        }
        else if (listener instanceof MediaControlListener)
        {
            montSvcs.addMediaControlListener(
                deviceID, (MediaControlListener) listener);
        }
        else
        {
        	System.out.println("listener type = " + listener.getClass().toString());
            throw new CstaException("Unsupported listener type.");
        }
    }

    /**
     * Allows the application to remove an AvayaStationListener,
     * TerminalListener or MediaListener.
     * 
     * @param listener -
     *            The listener to be removed. Must be one of the 3 types listed
     *            above.
     */
    public void removeListener(EventListener listener) throws CstaException
    {
        if (listener == null)
        {
            throw new CstaException("Listener is null.");
        }

        if (listenerList == null)
        {
            throw new CstaException(
                "Must call init() before" + "removeListener().");
        }

        if (listener instanceof AvayaStationListener)
        {
            listenerList.remove(AvayaStationListener.class, (AvayaStationListener)listener);
        }
        else if (listener instanceof CallControlListener)
        {
        	montSvcs.removeCallControlListener(deviceID, (CallControlListener) listener);
        }
        else if (listener instanceof RegistrationListener)
	    {
	        montSvcs.removeRegistrationListener(deviceID, (RegistrationListener) listener);
	    }
        else if (listener instanceof MediaControlListener)
        {
            montSvcs.removeMediaControlListener(
                deviceID, (MediaControlListener) listener);
        }
        else
        {
            throw new CstaException("Unsupported listener type.");
        }
    }
    
    /**
     * Gets the DeviceID for the given extension number. Note that this call
     * will not result in a call to DeviceServices to get the device ID.
     * DeviceServices was called in init(), and the device ID was cached. This
     * method call returns that cached value.
     * 
     * @return - the DeviceID for the extension number. Returns null if init()
     *         has not yet been called on startup or after station
     *         unregistration.
     */
    public DeviceID getDeviceID()
    {
        return deviceID;
    }
    /**
     * The extended method to register the CMAPI station, which includes
     * dependency and media mode. This invokes "RegisterTerminalRequest".
     *
     * @param password
     *            the station's password on the Communcitaion Manager
     * @param dependencyMode
     *            the station's dependency mode (MAIN, DEPENDENT or
     *            INDEPENDENT)
     * @param mediaMode
     *            the station's media mode (CLIENT, SERVER, NONE or
     *            TELECOMMUTER)
     * @param teleCommuterExtension -
     *            The telecommuter extension where media will be delivered.
     * @param e911 -
     *            The E911 emgergency call block option (null or "block").
     * @param forceLogin
     *            indicates if the application should try to forcefully
     *            register the station
     * @param localMediaInfo
     *            the local media stream's capabilities
     * @param callBack
     *            asynchronous callback class to handle the registration
     *            response
     * @throws Exception
     *             if anything goes wrong during station registration
     */
    public boolean register(String password, DependencyMode dependencyMode,
                    MediaMode mediaMode, String teleCommuterExtension, String e911, 
                    boolean forceLogin, MediaInfo localMediaInfo, 
                    String unicodeScripts, AsynchronousCallback callBack) throws Exception {

    	RegisterTerminalResponse temp = isRegistered(isMultiple);
    	if(temp != null) {
    		return true;
    	}   
    	// First need to create a request object, and populate the required
    	// fields.
    	RegisterTerminalRequest regRequest = createRegRequest(password,
    			dependencyMode, mediaMode, forceLogin, 
    			teleCommuterExtension, localMediaInfo, unicodeScripts, e911);

    	// Use the Asynchronous Service to send the registration request.
    	// Callback will be notified when request succeeds or fails.
    	// Create the registration services instance to send the redirection
    	// request to it
    	if(callBack != null)
    	{
    		asyncSvcs.sendRequest(regRequest, callBack);
    		return true;
    	}
    	else
    	{

    		// Send the registration request and wait for the listener to be
    		// notified
    		RegisterTerminalResponse regTermResp =
    			regSvcs.registerTerminal(regRequest);

    		if(regTermResp.getCode().equals(RegistrationConstants.NORMAL_REGISTER)) {
    			System.out.println("Registration succeeded");
    			return true;
    		}
    		else if(regTermResp.getCode().equals(RegistrationConstants.EXTENSION_IN_USE)
    				&& this.isMultiple) {
    			System.out.println("Already registered for multiple control");
    			return true;            
    		}
    		else{
    			System.out.println("Registration Failed: code=" +
    					regTermResp.getCode() + " reason=" + 
    					regTermResp.getReason());
    			return false;
    		}
    	}   	
    }

    /**
     * Registers the extension with Communication Manager. The result of the
     * registration will be received by the application if they have added a
     * TerminalListener through the addListener() method. This invokes "RegisterDevice".
     * 
     * @param password -
     *            The password for the extension that was passed in the init
     *            method.
     * @param sharedControl -
     *            If set to true, the application will be registered in shared
     *            control mode with Communication Manager. If false, it will be
     *            registered in exclusive mode.
     * @param localMediaInfo -
     *            Indicates the media parameters for this application. If set
     *            to null, all media will be sent to the connector server
     *            rather than to the application, and the codec will be G.711
     *            mu-law.
     */
    public void register(
        String password,
        boolean sharedControl,
        MediaInfo localMediaInfo)
        throws CstaException
    {

        // First need to create a request object, and populate the required
        // fields.
        RegisterDevice regRequest = new RegisterDevice();
        regRequest.setDevice(deviceID);

        // Create a login info object, and populate the password and shared
        // control mode fields.
        LoginInfo login = new LoginInfo();
        login.setPassword(password);
        login.setSharedControl(new Boolean(sharedControl));
        regRequest.setLoginInfo(login);
        regRequest.setLocalMediaInfo(localMediaInfo);

        // Send the registration request and wait for the listener to be
        // notified
        termSvcs.registerDevice(regRequest);
    }


    
    //**************************************************************************
    // createRegRequest
    //**************************************************************************
    /**
     * Creates a registration request from the given parameters.
     *
     * @param password The password of the station.
     * @param sharedControl Whether to register in shared control mode.
     * @param localMediaInfo Media parameters for application
     * @param e911 The E911 emgergency call block option.
     * @return RegisterTerminalRequest with these parameters.
     *      * @param password -
     *            The password for the extension that was passed in the init
     *            method.
     * @param dependencyMode
     *            This field can take "DEPENDENT" or "INDEPENDENT" or "MAIN"
     *            as valid values.
     * @param MediaMode
     *            This field can take "NONE" or "CLIENT" or "SERVER" or "TELECOMMUTER"
     *            as valid values.
     * @param localMediaInfo -
     *            Indicates the media parameters for this application. If set
     *            to null, all media will be sent to the connector server
     *            rather than to the application, and the codec will be G.711
     *            mu-law.
     * @param callBack -
     *            The asynchronous call back for registration response.
     */
    private RegisterTerminalRequest createRegRequest(String password, 
	    DependencyMode dependencyMode, MediaMode mediaMode,boolean forceLogin,
	    String telecommuterExt, 
            MediaInfo localMediaInfo, String unicodeScripts, String e911) {
        
        RegisterTerminalRequest regRequest = new RegisterTerminalRequest();
        regRequest.setDevice(deviceID);
        // Create a login info object, and populate the password and shared
        // control mode fields.
        LoginInfo login = new LoginInfo();
        login.setPassword(password);
        login.setDependencyMode(dependencyMode);
        login.setMediaMode(mediaMode);
        login.setForceLogin(forceLogin);

        login.setTelecommuterExtension(telecommuterExt);
        if ("block".equals(e911)) {
        	login.setE911(e911);
        }
        if(unicodeScripts != null && !"".equals(unicodeScripts)){

        	Integer unicodeScriptVal;
        	if(unicodeScripts.charAt(0) == '0' && (unicodeScripts.charAt(1) =='x' || unicodeScripts.charAt(1) =='X') ){
        		unicodeScriptVal = Integer.valueOf(unicodeScripts.substring(2), 16);
        	}
        	else if (unicodeScripts.charAt(0) == 'O' || unicodeScripts.charAt(0) == 'o'){
        		unicodeScriptVal = Integer.valueOf(unicodeScripts.substring(1), 8);
        	}
        	else{
        		unicodeScriptVal = Integer.valueOf(unicodeScripts, 10);
        	}
        	// CM expects unicodeScript (LSB) bit to be set indicating that the 
        	// endpoint is at least capable of supporting Latin characters.
        	unicodeScriptVal = unicodeScriptVal | 0x01;
        	System.out.println("Unicode scripts value sent in registration request=0x" + Integer.toHexString(unicodeScriptVal) );
        	login.setUnicodeScript(unicodeScriptVal);
        }
        regRequest.setLoginInfo(login);
        regRequest.setLocalMediaInfo(localMediaInfo);
        return regRequest;
    }
   
    /**
     * Synchronously unregisters the station with Communication Manager. This
     * version of the method should be called if you want the calling thread to
     * block until the unregistration is complete.  If the unregistration
     * attempt is successful, the method will simply return. If not successful,
     * an Exception will be thrown.  Note that the application is responsible
     * for calling station.cleanup() after unregistration is complete.  This is
     * different behavior than in 3.0 and previous releases!
     */
    public void unregister() throws CstaException
    {
        UnregisterTerminalRequest unregisterRequest = new UnregisterTerminalRequest();
        unregisterRequest.setDevice(deviceID);
        regSvcs.unregisterTerminal(unregisterRequest);
    }
         
    /**
     * Aynchronously unregisters the station with Communication Manager.
     * The result of the unregistration will be received by the
     * AsynchronousCallback object passed in.  Note that the application is
     * responsible for calling station.cleanup() after registration is complete.
     * This is different behavior than in 3.0 and previous releases!
     * 
     * @param callBack -
     *            The asynchronous call back for unregistration response.
     */
    public void unregister(AsynchronousCallback callBack) throws CstaException
    {
        UnregisterTerminalRequest unregisterRequest = new UnregisterTerminalRequest();
        unregisterRequest.setDevice(deviceID);
        
        // Use the Asynchronous Service to send the un-registration request. 
        // Our callback will be notified when the response is received and
        // cleanup will be performed at that point.
        asyncSvcs.sendRequest(unregisterRequest, callBack);
    }
    
    /**
     * Presses the indicated button. Button codes can be found in the following
     * classes: com.avaya.csta.physical.ButtonFunctionConstants
     * com.avaya.csta.physical.ButtonIDConstants
     * 
     * If "*" or "#" is passed in, this is converted to the proper button code.
     */
    public void pressButton(String buttonCode) throws CstaException
    {
        ButtonPress request = new ButtonPress();
        String translatedButton;

        if ("*".equals(buttonCode))
        {
            translatedButton = ButtonIDConstants.DIAL_PAD_STAR;
        }
        else if ("#".equals(buttonCode))
        {
            translatedButton = ButtonIDConstants.DIAL_PAD_POUND;
        }
        else
        {
            translatedButton = buttonCode;
        }
        request.setDevice(deviceID);
        request.setButton(translatedButton);
        // Send the request and catch any resulting CstaExceptions.
        physSvcs.pressButton(request);
    }

    /**
     * Presses the indicated sequence of digits on the keypad. Instances of '*'
     * and '#' in the dial string are converted to the proper button code. Note
     * that in order to initiate a simple call, this method is not needed. This
     * method is intended to be used for outpulsing digits after a call has
     * been connected.
     */
    public void pressKeypadButtons(String dialString) throws CstaException
    {
        for (int i = 0; i < dialString.length(); i++)
        {
        	pressButton(dialString.substring(i, i + 1));
        	
        	try 
        	{
        		//  sleep for 100 milliseconds before pressing another press the button
        		Thread.sleep(100);
        	} catch (InterruptedException e)
        	{	
        		System.out.println("Exception during sleep: " + e);
        	}
        }
    }

    /**
     * Initiates a call to the indicated extension / trunk. The AvayaStation
     * implementation will pick a free call appearance on which to make the
     * call.
     * 
     * A "glare" (conflict) condition can occur if an application is attempting
     * to use a call appearance at the same time as Communication Manager is
     * attempting to use that call appearance for an incoming call. This is how
     * that might occur, and what the result is.
     *  1) The application looks at its list of call appearances and selects
     * one that is not currently in use.
     *  2) The application sends a ButtonPress request for that call
     * appearance, which results in a button press stimulus being sent to
     * Communication Manager.
     *  3) Before Communication Manager receives this button press stimulus, it
     * receives an incoming call for the application's extension, and selects
     * the same call appearance for that incoming call that the application
     * selected for the outgoing call.
     *  4) When Communication Manager finally receives the button press
     * stimulus for the call appearance button, it interprets that stimulus as
     * a desire to answer the incoming call, rather than originating an
     * outgoing call.
     * 
     * In order to minimize the chances of an occurance such as this, it is
     * best to choose the free call appearance with the highest call appearance
     * number. Communication Manager will always select the lowest-numbered
     * free call appearance.
     * 
     * @param dialString -
     *            the string of digits to be dialed.
     * @return - the call appearance that was used for the call, or null if
     *         there were no free appearances.
     */
    public String initCall(String dialString) throws CstaException
    {
        String[] lampIDs =
            (String[]) callAppearanceLamps.keySet().toArray(new String[0]);
        Short[] states =
            (Short[]) callAppearanceLamps.values().toArray(new Short[0]);
        int i;
        boolean foundFree = false;

        // Choose the highest numbered free call appearance to avoid glare. For
        // more details, see the comments above.
        for (i = states.length - 1;(i >= 0); i--)
        {
            if (states[i].equals(LampModeConstants.OFF))
            {
                foundFree = true;
                break;
            }
        }

        if (!foundFree)
        {
            return null;
        }

        initCall(dialString, lampIDs[i]);
        return lampIDs[i];
    }

    /**
     * Initiates a call to the indicated extension / trunk.
     * 
     * @param dialString -
     *            the string of digits to be dialed.
     * @param callAppearance -
     *            the call appearance to utilize.
     */
    public void initCall(String dialString, String callAppearance)
        throws CstaException
    {
        // Press hold just in case the application is active on another
        // call appearance.
        pressButton(ButtonIDConstants.HOLD);
        // No need to go offhook, because pressing a call appearance button
        // when the station is onhook forces it to go offhook.
        pressButton(callAppearance);

        // Have to wait for the new call appearance to become active before
        // sending the dialed digits. The best way to do this would be to wait
        // for the corresponding call appearance lamp to go steady. For
        // simplicity of the example code, however, we simply sleep for a
        // second.
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            System.out.println("Exception during sleep: " + e);
        }
        pressKeypadButtons(dialString);
    }

    /**
     * Answers the call on the indicated call appearance.
     * 
     * @param callAppearance -
     *            The call appearance to answer.
     */
    public void answerCall(String callAppearance) throws CstaException
    {
        // Press hold just in case the application is active on another
        // call appearance.
        pressButton(ButtonIDConstants.HOLD);
        // No need to go offhook, because pressing a call appearance button
        // when the station is onhook forces it to go offhook.
        pressButton(callAppearance);
    }

    /**
     * Hang up the phone. The active call appearance will be disconnected.
     */
    public void disconnect()
    {
        onHook();
    }

    /**
     * Switch to the indicated call appearance and hang up the phone, thereby
     * disconnecting from the call.
     * 
     * @param callAppearance -
     *            The call appearance from which to disconnect.
     */
    public void disconnect(String callAppearance) throws CstaException
    {
        // Press hold just in case the application is active on another
        // call appearance.
        pressButton(ButtonIDConstants.HOLD);
        pressButton(callAppearance);

        // Have to wait for the new call appearance to become active before
        // sending the onhook. The best way to do this would be to wait
        // for the corresponding call appearance lamp to go steady. For
        // simplicity of the example code, however, we simply sleep for a
        // second.
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            System.out.println("Exception during sleep: " + e);
        }
        onHook();
    }

    /**
     * When the active call appearance is a conference call, and the CMAPI
     * softphone created the conference call, calling this method will cause
     * the last party that was joined to that call to be dropped off of the
     * conference.
     */
    public void dropLastParty() throws CstaException
    {
        pressButton(ButtonIDConstants.DROP);
    }

    /**
     * Returns an iterator that indicates the current state of all call
     * appearances.
     * 
     * @return - Map indicating current call appearance states. Key is call
     *         appearance ID, value is the state.
     */
    public Map<String, Short> getCallAppearanceStates()
    {
        return callAppearanceLamps;
    }

    /**
     * Puts the currently active call on hold. To return to it, use the
     * retrieve() method.
     */
    public void hold() throws CstaException
    {
        pressButton(ButtonIDConstants.HOLD);
    }

    
    public void getButtonInfo(){
    	
        try{
            // Get the information about all buttons administered on this
            // softphone. Create a new request and populate the device ID.
            GetButtonInformation buttonRequest = new GetButtonInformation();
            buttonRequest.setDevice(getDeviceID());

            // Send the request and get the response, catching any
            // resulting
            // CstaExceptions.
            GetButtonInformationResponse buttonResponse =
                physSvcs.getButtonInformation(buttonRequest);
            ButtonList list = buttonResponse.getButtonList();
            ButtonItem[] buttons = list.getButtonItem();
            System.out.println("Button Count for extension=" + extension + " " + list.getButtonItemCount());
            for (int i = 0; i < buttons.length; i++)
            {
                ButtonItem button = list.getButtonItem(i);

                int buttonNum = Integer.parseInt(button.getButton());
                int module = (buttonNum & 0x700) >> 8;
                buttonNum = buttonNum & 0xFF;
    
                String label = "";
    
                try {
                    label = ButtonTypes.getButtonTypes(
                            Integer.parseInt(button.getButtonFunction()))
                            .getName();
                } catch (Exception e) {
                    label = "Unknown";
                }
    
                System.out.println("Button: [module: " + module
                        + " number: " + buttonNum + "\tfunction: "
                        + button.getButtonFunction() + "\tname: " + label
                        + "]");
    
                // Is this a call appearance button?
                if (ButtonFunctionConstants
                        .CALL_APPR
                        .equals(buttons[i].getButtonFunction()))
                {
                    // Keep track of this button ID so we know which lamps
                    // correspond to call appearances. Current lamp status
                    // is off.
                    callAppearanceLamps.put(
                            buttons[i].getButton(),
                            LampModeConstants.OFF);
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Could not get button information ");
            e.printStackTrace(System.out);
        }
    }
    
    /**
     * Return to a previously held call appearance.
     * 
     * @param callAppearance -
     *            the call appearance to retrieve.
     */
    public void retrieve(String callAppearance) throws CstaException
    {
        // Press hold just in case the application is active on another
        // call appearance.
        pressButton(ButtonIDConstants.HOLD);
        pressButton(callAppearance);
    }

    /**
     * Blind transfer the current call to the indicated dial string.
     * 
     * @param dialString -
     *            Extension number or trunk to which to transfer.
     */
    public void transfer(String dialString) throws CstaException
    {
        pressButton(ButtonIDConstants.TRANSFER);
        pressKeypadButtons(dialString);
        pressButton(ButtonIDConstants.TRANSFER);
    }

    /**
     * Conference in the indicated extension number or trunk call.
     * 
     * @param dialString -
     *            Extension number or trunk call to conference in.
     */
    public void conference(String dialString) throws CstaException
    {
        pressButton(ButtonIDConstants.CONFERENCE);
        pressKeypadButtons(dialString);
        pressButton(ButtonIDConstants.CONFERENCE);
    }
    
    
    public void cleanup() throws CstaException{  	
    	removeListeners(); 
    	releaseID();
    }

    public PhysicalDeviceListener setIDAndGetPhysicalDeviceListener(
            DeviceID device, ServiceProvider provider) throws Exception {
        if(provider == null) {
            new IllegalArgumentException("ServiceProvider must be specified");
        }

        if(device == null) {
            new IllegalArgumentException("DeviceID must be specified");
        }
        // Get handles to all of the services that are used later on.
        devSvcs =
            (DeviceServices) provider.getService(
                com.avaya.csta.device.DeviceServices.class.getName());

        physSvcs =
            (PhysicalDeviceServices) provider.getService(
                ch.ecma.csta.physical.PhysicalDeviceServices.class.getName());
        montSvcs = (MonitoringServices) provider.getService(
                ch.ecma.csta.monitor.MonitoringServices.class.getName());
        regSvcs = (RegistrationServices) provider.getService(
                com.avaya.csta.registration.RegistrationServices.class.getName());
        asyncSvcs = (AsynchronousServices) provider.getService(
                com.avaya.csta.async.AsynchronousServices.class.getName());

        deviceID = device;

        // Add a physical device listener to receive events indicating phone
        // rings and display updates.
        physicalDevListener = new MyPhysicalDeviceListener();

        // Create the EventListenerList.
        listenerList = new EventListenerList();

        return physicalDevListener;
    }

    /**
     * Gets a device ID. Populates the id field with this value. Formulates a
     * connection ID based on the device ID and stores this value in the
     * connection field.
     */
    private void getID() throws CstaException
    {
        // use default instance
        getID(deviceInstance);
    }
    
    private void getID(DeviceInstance instance) throws CstaException
    {
        // Create a new request object and populate the needed fields.
        GetDeviceId devRequest = new GetDeviceId();
        if (callServerName != null && callServerName.length()>0) {
            devRequest.setSwitchName(callServerName);
        }
        if (callServerAddress != null && callServerAddress.length()>0) {
            devRequest.setSwitchIPInterface(callServerAddress);
        }
        
        devRequest.setExtension(extension);
        devRequest.setDeviceInstance(deviceInstance);
        
        if(this.isMultiple) {
            devRequest.setControllableByOtherSessions(Boolean.TRUE);
        }

        // Now, actually send the request to get a phone's id.
        GetDeviceIdResponse devResponse = devSvcs.getDeviceID(devRequest);
        deviceID = devResponse.getDevice();
    }

    /**
     * Releases the device ID. No need to release the connection ID, as this
     * was formulated from the device ID, not gotten from the server.
     */
    private void releaseID() throws CstaException
    {
        // default to locally-known deviceID
        releaseID(deviceID);
    }
    
    private void releaseID(DeviceID device) throws CstaException
    {
        ReleaseDeviceId deviceRequest = new ReleaseDeviceId();
        deviceRequest.setDevice(device);
        devSvcs.releaseDeviceID(deviceRequest);
        deviceID = null;
    }

    /**
     * Creates listener objects for the different services and adds them so the
     * app will be informed of events coming from the various services.
     */
    private void addListeners() throws CstaException
    {
        // Add a physical device listener to receive events indicating phone
        // rings and display updates.
        physicalDevListener = new MyPhysicalDeviceListener();
        montSvcs.addPhysicalDeviceListener(deviceID, physicalDevListener);
    }

    /**
     * Removes all of the listeners that were added for the various services.
     */
    private void removeListeners() throws CstaException
    {
        montSvcs.removePhysicalDeviceListener(deviceID, physicalDevListener);
    }

    /**
     * This method sends a request to physical device services to hang up the
     * phone.
     */
    private void onHook()
    {
        // Create a new request, and populate the required fields.
        SetHookswitchStatus hookRequest = new SetHookswitchStatus();

        // This indicates that the phone is to go on-hook.
        hookRequest.setHookswitchOnhook(Boolean.TRUE);

        // CSTA requires this field, but it has no meaning to the Communication
        // Manager since only one switch hook is supported. Populate it with a
        // default value.
        hookRequest.setHookswitch(DEFAULT_VALUE);
        hookRequest.setDevice(deviceID);

        try
        {
            // Send the request and catch any resulting CstaExceptions.
            physSvcs.setHookswitchStatus(hookRequest);
        }
        catch (CstaException e)
        {
            System.out.println("Could not hang up the phone");
            e.printStackTrace(System.out);
        }
    }
    
    //**************************************************************************
    // isRegistered
    //**************************************************************************
    /**
     * Gets the registration state for the given extension number. This can be
     * any of the following: Idle, Registered, Registering or Unregistering.
     * 
     * @return - the registration state for the extension number.
     * @throws CstaException
     */
    public RegistrationState isRegistered() throws CstaException {
        GetRegistrationState request = new GetRegistrationState();
        request.setDevice(this.deviceID);
        GetRegistrationStateResponse resp = regSvcs.getRegistrationState(request);
        RegistrationState state = resp.getRegistrationState();
        return state;
    }

    private RegisterTerminalResponse isRegistered(boolean reuseRegistration) 
    throws CstaException {
        if(reuseRegistration) {
            RegistrationState state = this.isRegistered();
            if(state.equals(RegistrationState.REGISTERED)) {
                //System.out.println(deviceID + " is already registered for multiple session control.");
                RegisterTerminalResponse regResp = new RegisterTerminalResponse();
                SubjectDeviceID subId = new SubjectDeviceID();
                subId.setDeviceIdentifier(this.deviceID);
                regResp.setDevice(subId);
                regResp.setSignalingEncryption("unknown");
                regResp.setCode(RegistrationConstants.NORMAL_REGISTER);
                regResp.setReason("Multiple session control of " + deviceID);
                return regResp;
            } else {
                //System.out.println(deviceID + " registering for multiple session control.");
            }
        }
        return null;
    }
    
    /**
     * This is the physical device listener class. It implements the
     * PhysicalDeviceListener interface, and passes along all events to any
     * registered listeners. Most events it passes through untouched, with the
     * only exception being lamp updates. The class uses EventListListener for
     * its implementation when multicasting events on to listeners. This is not
     * the most efficient implementation possible, but is among the simplest.
     */
    private class MyPhysicalDeviceListener implements PhysicalDeviceListener
    {

        /**
         * Method is fired when the LampMode update is received. Separate the
         * green call appearance lamp updates from the other lamp updates. The
         * green call appearance lamps tell the app the status of the call on
         * that call appearance. A flashing green lamp indicates an incoming
         * call. When that lamp becomes steady, that indicates that the call
         * has been established. When that lamp goes dark, the far end has hung
         * up. One thing to note about the implementation of this method is
         * that for Avaya terminals, the ID of the green call appearance ID is
         * the same as the ID for the corresponding call appearance button.
         * This fact is critical to the implementation of this application.
         * 
         * NOTE: The Communication Manager server periodically sends lamp
         * refreshes to terminals that don't indicate true transitions. For the
         * green call appearance lamps, this implementation will filter out the
         * refreshes and will only send true transistions on to the
         * application. All lamp updates (including refreshes) are sent to the
         * app for lamps that are NOT green call appearance lamps.
         * 
         * NOTE: In future releases of the Communication Manager API we will be
         * supplying Call Control Services which will, among other things, give
         * a much better way of determining call state. An Established event
         * will be sent to indicate the call is up, rather than having to
         * monitor the lamp and wait until it goes steady. A Connection Cleared
         * event will be sent when the far end has hung up, rather than having
         * to look for the lamp to go dark.
         * 
         * @param event
         *            the received LampModeEvent
         */
        public void lampUpdated(LampModeEvent event)
        {
            String lampID = event.getLamp();
            Short lampValue = event.getLampMode();
            Short lampColor = event.getLampColor();
            
            Object[] listeners = listenerList.getListenerList();

            // Check and see if this is a green call appearance lamp. If not,
            // send
            // a regular lampUpdated event to the application.
            if (!(callAppearanceLamps.containsKey(lampID))
                || (LampModeConstants.RED.equals(lampColor)))
            {
                for (int i = listeners.length - 2; i >= 0; i -= 2)
                {
                    if (listeners[i] == AvayaStationListener.class)
                    {
                        ((AvayaStationListener) listeners[i + 1]).lampUpdated(
                            event);
                    }
                }
                return;
            }
            
            // Store the new state for the lamp.
            callAppearanceLamps.put(lampID, lampValue);

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    ((AvayaStationListener) listeners[i
                            + 1]).callAppearanceLampUpdated(
                        lampID,
                        lampValue);
                }
            }
        }

        /*
         * Pass along the event untouched to the listeners.
         */
        public void displayUpdated(DisplayUpdatedEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    ((AvayaStationListener) listeners[i + 1]).displayUpdated(
                        event);
                }
            }
        }

        /*
         * Pass along the event untouched to the listeners.
         */
        public void ringerStatusUpdated(RingerStatusEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    (
                        (AvayaStationListener) listeners[i
                            + 1]).ringerStatusUpdated(
                        event);
                }
            }
        }

        /*
         * Pass along the event untouched to the listeners.
         */
        public void buttonInformationChanged(ButtonInformationEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    (
                        (AvayaStationListener) listeners[i
                            + 1]).buttonInformationChanged(
                        event);
                }
            }
        }

        /*
         * Pass along the event untouched to the listeners.
         */
        public void buttonPressed(ButtonPressEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    ((AvayaStationListener) listeners[i + 1]).buttonPressed(
                        event);
                }
            }
        }

        /*
         * Pass along the event untouched to the listeners.
         */
        public void hookswitchChanged(HookswitchEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    (
                        (AvayaStationListener) listeners[i
                            + 1]).hookswitchChanged(
                        event);
                }
            }
        }

        /*
         * Pass along the event untouched to the listeners. Not currently
         * supported, but provided for completeness.
         */
        public void messageWaitingChanged(MessageWaitingEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    (
                        (AvayaStationListener) listeners[i
                            + 1]).messageWaitingChanged(
                        event);
                }
            }
        }

        /*
         * Pass along the event untouched to the listeners. Not currently
         * supported, but provided for completeness.
         */
        public void microphoneGainSettingChanged(MicrophoneGainEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    (
                        (AvayaStationListener) listeners[i
                            + 1]).microphoneGainSettingChanged(
                        event);
                }
            }
        }

        /*
         * Pass along the event untouched to the listeners. Not currently
         * supported, but provided for completeness.
         */
        public void microphoneMuteStatusChanged(MicrophoneMuteEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    (
                        (AvayaStationListener) listeners[i
                            + 1]).microphoneMuteStatusChanged(
                        event);
                }
            }
        }

        /*
         * Pass along the event untouched to the listeners. Not currently
         * supported, but provided for completeness.
         */
        public void speakerMuteStatusChanged(SpeakerMuteEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    (
                        (AvayaStationListener) listeners[i
                            + 1]).speakerMuteStatusChanged(
                        event);
                }
            }
        }

        /*
         * Pass along the event untouched to the listeners. Not currently
         * supported, but provided for completeness.
         */
        public void speakerVolumeSettingChanged(SpeakerVolumeEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    (
                        (AvayaStationListener) listeners[i
                            + 1]).speakerVolumeSettingChanged(
                        event);
                }
            }
        }
        
        /*
         * Pass along the event untouched to the listeners. Not currently
         * supported, but provided for completeness.
         */
        public void e911CallBlocked(E911CallBlockedEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    (
                        (AvayaStationListener) listeners[i
                            + 1]).e911CallBlocked(
                        event);
                }
            }
        }        

        /*
         * Pass along the event untouched to the listeners.
         */
        public void serviceLinkStatusChanged(ServiceLinkStatusEvent event)
        {
            Object[] listeners = listenerList.getListenerList();

            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == AvayaStationListener.class)
                {
                    (
                        (AvayaStationListener) listeners[i
                            + 1]).serviceLinkStatusChanged(
                        event);
                }
            }
        }
    }
}
