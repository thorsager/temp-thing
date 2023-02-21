/*
 * Click2Call.java
 * 
 * Copyright (c) 2002-2008 Avaya Inc. All rights reserved.
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
package sampleapps.ccs.click2call;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.naming.NamingException;
import javax.swing.JOptionPane;

import ch.ecma.csta.binding.ClearCall;
import ch.ecma.csta.binding.ConferencedEvent;
import ch.ecma.csta.binding.ConnectionClearedEvent;
import ch.ecma.csta.binding.ConnectionID;
import ch.ecma.csta.binding.DeliveredEvent;
import ch.ecma.csta.binding.DeviceID;
import ch.ecma.csta.binding.EstablishedEvent;
import ch.ecma.csta.binding.MakeCall;
import ch.ecma.csta.binding.MakeCallResponse;
import ch.ecma.csta.binding.OriginatedEvent;
import ch.ecma.csta.binding.SingleStepConferenceCall;
import ch.ecma.csta.binding.types.ParticipationType;
import ch.ecma.csta.callcontrol.CallControlAdapter;
import ch.ecma.csta.callcontrol.CallControlServices;
import ch.ecma.csta.errors.CstaException;
import ch.ecma.csta.errors.InvalidDeviceIDException;
import ch.ecma.csta.errors.NoActiveCallException;
import ch.ecma.csta.monitor.MonitoringServices;

import com.avaya.api.sessionsvc.exceptions.SessionCleanedUpException;
import com.avaya.cmapi.APIProtocolVersion;
import com.avaya.cmapi.ServiceProvider;
import com.avaya.cmapi.ServiceProviderListener;
import com.avaya.cmapi.evt.ServerConnectionDownEvent;
import com.avaya.cmapi.evt.ServerSessionNotActiveEvent;
import com.avaya.cmapi.evt.ServerSessionTerminatedEvent;
import com.avaya.csta.binding.GetThirdPartyDeviceId;
import com.avaya.csta.binding.GetThirdPartyDeviceIdResponse;
import com.avaya.csta.binding.SessionCharacteristics;
import com.avaya.csta.binding.types.DeviceIDType;
import com.avaya.csta.binding.types.EventFilterMode;
import com.avaya.csta.device.DeviceServices;
import com.avaya.mvcs.framework.CmapiKeys;

/**
 * Purpose: This application registers a station with Communication Manager and
 * monitors it for incoming calls. It provides a GUI to interact (call back,
 * delete etc) with the logged calls. The call log is persistent between
 * multiple runs of the application. This application also provides a GUI to
 * perform directory search. When the user enters either first or last name to
 * perform directory search, it returns all the matches found with full name,
 * telephone number and mail id and displays it in the GUI table.
 * 
 * The user can dial calls using the application. If a missed call is dialed
 * back, its status changes from "Missed" to "Called back". Calls with different
 * status ("Missed", "Answered", "Called back" and "Dialed") are displayed in
 * different colors.
 * 
 * High Level Steps: This application registers an extension as a CMAPI phone
 * (that is softphone enabled) with the Communication Manager in shared control
 * mode. Registration is done asynchronously using Asynchronous Services. The
 * application uses Call Control Services to place the call. The application
 * logs both incoming and dialed calls and reports in a GUI. It also provides a
 * GUI to perform directory search.
 * 
 * Prerequisites: - Edit the click2call.properties file found in the
 * cmapisdk/example/lib directory: Set "callserver" to the IP address of a CLAN
 * on Communication Manager. Set "extension" to an extension/phone that is
 * softphone enabled on Communication Manager and will be registered by this
 * application in shared control mode. Set "password" to the password
 * administered on Communication Manager for the extension/phone that is
 * softphone enabled. Set "providerUrl" to point to your LDAP server. - Make
 * sure you have at least one IP_API_A license on Communication Manager. - Make
 * sure you have the cmapi-client.properties file set to the IP address of the
 * connector server.
 * 
 * 
 * Possible variations: You can modify this application very easily to filter on
 * particular types of calls such as: all outside calls, only outside calls from
 * area code 402, only calls from a particular person or number.
 * 
 * build and run: You can build the click2call application and run it by going
 * to cmapisdk/examples/bin and running the script runClick2Call.sh.
 * 
 * A TSAPI Basic User license is required to run this application.
 * 
 * @author Avaya Inc.
 */
public final class Click2Call {
    private static final String SECURITY_CREDENTAILS = "ldap_securityCredentails";

    private static final String SECURITY_PRINCIPAL = "ldap_securityPrincipal";

    // These fields are read in from the click2call.properties file. The
    // application properties file must be populated with a valid name
    // for the Avaya Communication Manager, a valid extension number and
    // password.  If USE_TELURI is set to true then the switchName property is
    // not required and the extension will need to be in TelURI format.
    // NOTE: providerUrl is the URL of LDAP server. It should be of the form
    // ldap://ldap_server_address:port/search_root
    // port may be optional in the URL.
    private String switchName;
    private String extension;

    private String password;

    private String providerUrl;
    
    private String baseDN;

    private String securityPrincipal;

    private String securityCredentails;

    /**
     * When this is set to true it will cause the application to use TelURI mode.  This means the
     * extension in the properties will need to be in a TelURI format (tel:1000;phone-context=dialstring).
     */  
    private boolean useTelURI = false;

    /**
     * When set to true this will cause the application to set the EventFilterMode to DesktopCallControl in the
     * SetSessionCharacteristic message
     */
    private boolean useDesktopCallControl = false;

    // This variable is initialized in the bootstrap() call. The variable is
    // defined
    // as a member variable to allow recovery once an application has already
    // started.
    private Properties spProp;

    // name of properties file.
    private static final String PROPERTY_FILE = "click2call.properties";

    private static final String PROP_FILE_SWITCH_NAME = "switchname";
    
    private static final String PROP_FILE_USE_TELURI = "useTelURI";
    
    private static final String PROP_FILE_USE_DCC = "useDesktopCallControl";

    private static final String PROP_FILE_EXTENSION = "extension";

    private static final String PROP_FILE_EXT_PASSWORD = "password";

    private static final String PROP_FILE_CMAPI_SERVER = "cmapi1.server_ip";

    private static final String PROP_FILE_SERVER_PORT = "cmapi1.server_port";

    private static final String PROP_FILE_SECURE = "cmapi1.secure";

    private static final String SESSION_DURATION = "20";
    
    private static final String CLEANUP_DELAY = "60";

    private static final String APP_DESCRIPTION = "Click2Call";

    private static final String PROVIDER_URL = "providerUrl";
    
    private static final String BASE_DN = "baseDN";

    // The Device ID for our extension number will be supplied by the
    // Communication Manager API. The Connection ID is formulated from the
    // Device ID.
    // private DeviceID id = null;

    // The Service Provider used to get the handles of various services.
    private ServiceProvider provider;

    private ServiceProviderListener serviceProviderListener;

    /**
     * These fields will be populated with the handles of the various services,
     * which we will get through the ServiceProvider class.
     */
    private CallControlServices callControlSvcs;

    /**
     * callParser parse the lamp update received to extract name &/or number of
     * caller.
     */
    private DisplayParser callParser;

    // Reference to GUI object
    private Click2CallGUI callLogGui;

    private DirectoryLookupGUI dirLookupGui;

    private LDAPQueryExecutor queryExecutor;

    private MyActionListener actionListener;

    // location of log file.
    private static final String LOG_FILE = "appdata/click2calldata/click2calllog";

    // number of columns stored corresponding to each call.
    private static final int NUM_OF_COLUMNS_PER_CALL = 4;

    // GUI.whichCallToDisconnect method returns an ArrayList containing
    // phone number of called party and the call appearance that call is
    // using. These are the indices of those fields in the ArrayList returned.
    private static final int PHONE_NUMBER_INDEX = 0;

    // Contents of log file are read into dataContent which is passed to GUI
    // object as initialization data.
    private String[][] dataContent;

    private Timer reconnectTimer = new Timer("Reconnect Timer");
    
    /**
     * Creates an instance of the Click2Call class and invokes the Login GUI
     */
    public static void main(String ... args) {
        Click2Call app = new Click2Call();
        app.login();
    }

    /**
     * Method used to initialize the Login GUI instances.
     */
    private void login() {
        new LoginGUI(this);
    }

    /**
     * Method used to invoke the Click2Call's bootstrap and start method. The
     * Login GUI will be redisplayed on all Login failures.
     * 
     * @param user - Username used during login
     * @param passwd - Password used during login
     */
    protected void startup(final StringBuffer user, final StringBuffer passwd) {
        try {
            bootstrap(user, passwd);
            start();
        } catch (CstaException e) {
            System.out.println("Could not start the app");
            e.printStackTrace(System.out);
            System.exit(0); // exits to the shutdownThread to perform cleanup
        } catch (RuntimeException e) {
            System.out.println("Could not start the app");
            e.printStackTrace(System.out);
            if (e.getCause() instanceof SecurityException) {
                LoginGUI loginGUI = new LoginGUI(this);
                loginGUI.failed();
            }
        } catch (Exception e) {
            System.out.println("Could not start the app");
            e.printStackTrace(System.out);
            System.exit(0); // exits to the shutdownThread to perform cleanup
        }
    }

    /**
     * This is the bootstrap to read in the needed properties, and get handles
     * to all services that are used by the application.
     * 
     * @param user - Username used during login
     * @param passwd - Password used during login
     * @throws CstaException if a CstaException is generated by one of the calls
     *             to service provider, it is thrown to the caller.
     * @throws Exception if any runtime exception is generated, it is thrown to
     *             the caller.
     */
    public void bootstrap(final StringBuffer user, final StringBuffer passwd) throws CstaException, Exception {
        String cmapiServerIp;
        String cmapiServerPort;
        String cmapiSecure;
        String cmapiTrustStoreLocation;

        // add following properties for validation client and service side
        // certifications
        String cmapiTrustStorePassword;
        String cmapiKeyStoreLocation;
        String cmapiKeyStorePassword;
        String cmapiKeyStoreType;
        String isValidPeer;
        String hostnameValidation;

        // Get all of the arguments from the properties file
        ClassLoader cl = Click2Call.class.getClassLoader();
        URL appURL = cl.getResource(PROPERTY_FILE);
        Properties appProp = new Properties();
        appProp.load(appURL.openStream());
        switchName = appProp.getProperty(PROP_FILE_SWITCH_NAME).trim();
        extension = appProp.getProperty(PROP_FILE_EXTENSION).trim();
        password = appProp.getProperty(PROP_FILE_EXT_PASSWORD).trim();
        
        providerUrl = appProp.getProperty(PROVIDER_URL).trim();
        baseDN = appProp.getProperty(BASE_DN).trim();

        securityPrincipal = appProp.getProperty(SECURITY_PRINCIPAL);
        if (securityPrincipal != null) {
            securityPrincipal = securityPrincipal.trim();
        }
        securityCredentails = appProp.getProperty(SECURITY_CREDENTAILS);
        if (securityCredentails != null) {
            securityCredentails = securityCredentails.trim();
        }

        String useTelURIStr = appProp.getProperty(PROP_FILE_USE_TELURI);
        if (useTelURIStr != null) {
            useTelURI = Boolean.parseBoolean(useTelURIStr.trim());
        }

        String useCCPStr = appProp.getProperty(PROP_FILE_USE_DCC);
        if (useCCPStr != null) {
            useDesktopCallControl = Boolean.parseBoolean(useCCPStr.trim());
        }

        cmapiServerIp = appProp.getProperty(PROP_FILE_CMAPI_SERVER).trim();

        // If there is no entry then we will assume the connection is secure.
        cmapiServerPort = appProp.getProperty(PROP_FILE_SERVER_PORT, "4722").trim();
        cmapiSecure = appProp.getProperty(PROP_FILE_SECURE, "true").trim();
        cmapiTrustStoreLocation = appProp.getProperty(CmapiKeys.TRUST_STORE_LOCATION);

        // add following properties for validation client and service side
        // certifications
        cmapiTrustStorePassword = appProp.getProperty(CmapiKeys.TRUST_STORE_PASSWORD);
        cmapiKeyStoreLocation = appProp.getProperty(CmapiKeys.KEY_STORE_LOCATION);
        cmapiKeyStoreType = appProp.getProperty(CmapiKeys.KEY_STORE_TYPE);
        cmapiKeyStorePassword = appProp.getProperty(CmapiKeys.KEY_STORE_PASSWORD);
        isValidPeer = appProp.getProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE);
        hostnameValidation = appProp.getProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE_HOSTNAME);

        spProp = new Properties();
        spProp.setProperty(CmapiKeys.SERVICE_PROVIDER_ADDRESS, cmapiServerIp);
        spProp.setProperty(CmapiKeys.CMAPI_USERNAME, user.toString().trim());
        spProp.setProperty(CmapiKeys.CMAPI_PASSWORD, passwd.toString().trim());
        spProp.setProperty(CmapiKeys.SERVICE_PROVIDER_PORT, cmapiServerPort);
        spProp.setProperty(CmapiKeys.SECURE_SERVICE_PROVIDER, cmapiSecure);

        spProp.put(CmapiKeys.SESSION_PROTOCOL_VERSION_LIST, APIProtocolVersion.VERSION_LIST);

        if (cmapiTrustStoreLocation != null) {
            spProp.setProperty(CmapiKeys.TRUST_STORE_LOCATION, cmapiTrustStoreLocation.trim());
        }

        // add following properties for validation client and service side
        // certifications
        if (cmapiTrustStorePassword != null) {
            spProp.setProperty(CmapiKeys.TRUST_STORE_PASSWORD, cmapiTrustStorePassword.trim());
        }
        if (cmapiKeyStoreLocation != null) {
            spProp.setProperty(CmapiKeys.KEY_STORE_LOCATION, cmapiKeyStoreLocation.trim());
        }
        if(cmapiKeyStoreType != null){
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
        // Clear out both buffers for security reasons.
        user.delete(0, user.length());
        passwd.delete(0, passwd.length());

        // Set the cleanup delay to be 60 seconds, so if we have a network
        // interruption, we can reestablish our session.
        spProp.setProperty(CmapiKeys.SESSION_CLEANUP_TIMER, CLEANUP_DELAY);
        // Set the session duration timer to be 20 seconds for the purposes of
        // this application to demonstrate / test recovery from network
        // interruptions.  You may may want a longer session duration for your
        // app.
        spProp.setProperty(CmapiKeys.SESSION_DURATION_TIMER, SESSION_DURATION);
        spProp.setProperty(CmapiKeys.APPLICATION_DESCRIPTION, APP_DESCRIPTION);

        // Get a handle to the ServiceProvider class.
        provider = ServiceProvider.getServiceProvider(spProp);

        // Set any session characteristics that we want to use.
        if (useTelURI || useDesktopCallControl) {
            SessionCharacteristics sc = new SessionCharacteristics();
            if (useTelURI) {
                sc.setDeviceIDType(DeviceIDType.TELURI);
            }
            if (useDesktopCallControl) {
                sc.setEventFilterMode(EventFilterMode.DESKTOPCALLCONTROL);
            }
            provider.setSessionCharacteristics(sc);
        }

        // Now, we can get the services that we need.
        callControlSvcs =
                (CallControlServices) provider.getService(ch.ecma.csta.callcontrol.CallControlServices.class.getName());

        MonitoringServices ms =
                (MonitoringServices) provider.getService(ch.ecma.csta.monitor.MonitoringServices.class.getName());

        ms.addCallControlListener(getDeviceID(extension), new MyCallControlListener());

        // initialize callParser to parse incoming calls.
        callParser = new DisplayParser();

        initGuiInstances();
    }

    /**
     * This method actually starts the app. It must be called after calling
     * bootstrap or it will fail.
     * 
     * @throws Exception if there is a problem adding a <code>ServiceProviderListener</code>
     */
    protected void start() throws Exception {
        System.out.println("Startup using switch=" + switchName + " ext=" + extension + " pw=" + password);

        // Create a thread in whose context our cleanup will occur if the app
        // is terminated. The Communication Manager API connector server code
        // will clean up if an app goes away unexpectedly, but it's still
        // good to clean up.
        MyShutdownThread shutdownThread = new MyShutdownThread();
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        serviceProviderListener = new MyServiceProviderListener();
        provider.addServiceProviderListener(serviceProviderListener);
    }

    /**
     * Method recoverFromTerminatedSession will do all of the required clean up
     * and re-initialization to allow the application to create a new session
     * and continue running. This method can only be called if bootstrap() has
     * already been called.
     * 
     * @throws IllegalStateException if {@link #bootstrap(StringBuffer, StringBuffer)} was not previously called.
     * @throws Exception    If there is a problem getting the service provider and/or adding the listener
     */
    private void recoverFromTerminatedSession() {
        if (spProp == null) {
            throw new IllegalStateException("bootstrap() must be called before this method can be called");
        }
        
        try {
            System.out.println("Attempting to set up a new session.");

            provider.removeServiceProviderListener(serviceProviderListener);
            // Get a handle to the ServiceProvider class.
            provider = ServiceProvider.getServiceProvider(spProp);
            
            // add a listener to capture service provider events
            serviceProviderListener = new MyServiceProviderListener();
            provider.addServiceProviderListener(serviceProviderListener);
            
            // Have to get handles to the services from the new ServiceProvider 
            callControlSvcs =
                (CallControlServices) provider.getService(ch.ecma.csta.callcontrol.CallControlServices.class.getName());
            MonitoringServices ms =
                (MonitoringServices) provider.getService(ch.ecma.csta.monitor.MonitoringServices.class.getName());
            ms.addCallControlListener(getDeviceID(extension), new MyCallControlListener());
        } catch (Exception e) {
            System.out.println(
                "Java Exception - Could not restart the app because "
                    + e);
            e.printStackTrace(System.out);
            // Other things may have gone wrong (e.g. adding listeners)
            // that a reconnect would not fix.  For this simple app,
            // we'll just exit.
            System.exit(0);
        }
    }

    /**
     * This cleanup method assures us that we unregister our extension, remove
     * all the listeners and release the device ID. The connector server will
     * cleanup if a client goes away without doing this cleanup, but it's best
     * to be safe.
     */
    public void cleanup() {
        System.out.println("Application is terminating; performing cleanup....");

        // Write the call data to LOG_FILE. Logs will be read at next launch of
        // this application.
        writeLog();

        try {
            // remove the listener
            provider.removeServiceProviderListener(serviceProviderListener);

            System.out.println("calling provider.disconnect");
            // provider.disconnect will disconnect from the server and
            // terminate the active session.
            provider.disconnect(true);
        } catch (Exception e) {
            System.out.println("Could not clean up properly");
            e.printStackTrace(System.out);
        }
    }

    /**
     * This loadLog method reads old call log (if any) saved in the log file and
     * stores it in a String array. The string array is used to initialize the
     * GUI.
     */
    private String[][] loadLog() {
        // Open a "connection" to the log file, create FileReader and read
        // the log into buffer.
        File file = new File(LOG_FILE);
        FileReader fileReader;
        StringBuffer buffer;

        // If file does not exists, no logs to load, so simply return null.
        if (!file.exists()) {
            return null;
        }

        try {
            fileReader = new FileReader(file);

            buffer = new StringBuffer();

            // It would be more efficient to read data in chunks,
            // but for simplicity of code, we read data character
            // by character here.

            for (int ch = fileReader.read(); ch != -1; ch = fileReader.read()) {
                buffer.append((char) ch);
            }

            // Every field of a particular call is stored in separate
            // lines in log file
            StringTokenizer tokens = new StringTokenizer(new String(buffer), "\n");

            // Sanity check! Every logged call has four fields, name, number,
            // time and status. If log file is corrupted disregard it and
            // return null.
            if ((tokens.countTokens() % NUM_OF_COLUMNS_PER_CALL != 0) || (tokens.countTokens() == 0)) {
                System.out.println("Corrupted data in " + LOG_FILE + " file");
                return null;
            }

            dataContent = new String[tokens.countTokens() / NUM_OF_COLUMNS_PER_CALL][NUM_OF_COLUMNS_PER_CALL];
            int counter = tokens.countTokens() / NUM_OF_COLUMNS_PER_CALL;

            for (int i = 0; i < counter; i++) {
                for (int j = 0; j < NUM_OF_COLUMNS_PER_CALL; j++) {
                    dataContent[i][j] = tokens.nextToken();
                }
            }

            return dataContent;
        } catch (IOException ioe) {
            System.out.println("Error loading logs.");
            return null;
        }
    }

    /**
     * writeLog method write the log back to log file at application shutdown.
     */
    private void writeLog() {
        // If registration failed, callLogGui will be null. In this case,
        // do nothing.
        if (callLogGui == null) {
            return;
        }

        File file = new File(LOG_FILE);
        FileWriter fileWriter;
        String newLine = "\n"; // delimeter for various fields of a call
        // and different calls.

        ArrayList<?> tableData = callLogGui.getTableData();

        try {
            // Delete the old file and create new one. If no logs are there
            // to write, just don't create the log file.
            file.delete();

            if ((tableData == null) || (tableData.size() == 0)) {
                return;
            }

            file.createNewFile();
            fileWriter = new FileWriter(file);

            for (int i = 0; i < tableData.size(); i++) {
                fileWriter.write(tableData.get(i).toString(), 0, tableData.get(i).toString().length());
                fileWriter.write(newLine, 0, newLine.length());
            }

            fileWriter.close();
        } catch (IOException e) {
            System.out.println("Error writing logs to file");
            return;
        }

    }

    /**
     * This method initiates a makeCall request to Call Control Services and
     * when the call is successful, the display is updated with call
     * information.
     * 
     * @param numberToCall number to be called
     * @param name The name of the extension being called
     * @throws CstaException
     */
    private void makeCall(final String numberToCall) throws CstaException {

        // get device id for the dial string
        DeviceID calledDevice;
        try {
            System.out.println("makeCall to extension: " + numberToCall);
            calledDevice = getDeviceID(numberToCall);
            MakeCall request = new MakeCall();
            request.setCallingDevice(getDeviceID(extension));
            request.setCalledDirectoryNumber(calledDevice);

            MakeCallResponse response = callControlSvcs.makeCall(request);
            callLogGui.callBack(numberToCall);
            callLogGui.displayCallStatus(actionListener, numberToCall, response.getCallingDevice());

        } catch (InvalidDeviceIDException e) {
            System.out.println("InvalidDeviceIDException for ext: " + numberToCall);

        } catch (Exception e) {
            System.out.println("Exception in MakeCall: " + e.getStackTrace());
        }
    }
    
    private void singleStepConf(final String joinExt) throws Exception {
        SingleStepConferenceCall sscc = new SingleStepConferenceCall();
        sscc.setActiveCall(callLogGui.getConnectionId());
        sscc.setDeviceToJoin(getDeviceID(joinExt));
        sscc.setParticipationType(ParticipationType.SILENT);
        callControlSvcs.singleStepConferenceCall(sscc);
    }

    /**
     * This method initiates a clearCall request to Call Control Services.
     * 
     * @param connId ConnectionID to clear
     * @throws CstaException If there is a problem when clearing the call
     */
    private void clearCall(final ConnectionID connId) throws CstaException {
        System.out.println("\nClearCall");
        ClearCall cRequest = new ClearCall();
        cRequest.setCallToBeCleared(connId);
        try {
            callControlSvcs.clearCall(cRequest);
        } catch (NoActiveCallException noce) {
            System.out.println("NoActiveCallException Received");
        }

        System.out.println("Call Cleared!");
    }

    /**
     * Get a deviceID object for a specified extension. This method will
     * determine if TelURI is being used or not and generate the correct object
     * based on that. If not TelURI then it will send a GetThirdPartyDeviceId
     * request to the server using the given extension and the switch name
     * specified in the properties.
     * 
     * @param extension To use in building the deviceID request
     * @return The id for the given extension
     * 
     * @throws Exception If there were any problems attempting to run the
     *             GetThirdPartyDeviceId request
     */
    private DeviceID getDeviceID(final String extension) throws Exception {
        if (useTelURI) {
            DeviceID deviceId = new DeviceID();
            deviceId.setContent(extension);
            return deviceId;
        } else {
            DeviceServices devSvcs =
                    (DeviceServices) provider.getService(com.avaya.csta.device.DeviceServices.class.getName());

            // Create a new request object and populate the needed fields.
            GetThirdPartyDeviceId devRequest = new GetThirdPartyDeviceId();

            devRequest.setExtension(extension);
            devRequest.setSwitchName(switchName);

            // Send the request to get a phone's id.
            GetThirdPartyDeviceIdResponse devResponse = devSvcs.getThirdPartyDeviceID(devRequest);
            return devResponse.getDevice();
        }
    }

    /**
     * Logs an incoming call
     * 
     * @param name The name for the extension
     * @param number The phone number
     * @param callStatus The status of the call
     */
    private void logCall(final String number, final String callStatus) {
        String callTime = callParser.getTime();
        callLogGui.displayCall(number, callTime, callStatus);
    }

    private class MyActionListener implements ActionListener {
        /**
         * Actions generated by GUI are received here and corresponding actions
         * are taken
         * 
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        public void actionPerformed(final ActionEvent ae) {
            // determine which button is pressed by the user and take
            // appropriate action
            String buttonPressed = ae.getActionCommand();

            if (buttonPressed == null) {
                return;
            } else if (buttonPressed.equals("delete")) {
                // remove the selected call from callLogGui.
                callLogGui.delete();
            } else if (buttonPressed.equals("callback")) {
                String number = null;

                try {
                    number = callLogGui.getNumberToCall();

                    if ((number != null) && (number.trim().length() != 0)) {
                        if (useTelURI) {
                            // number should start with tel:
                            if (!number.startsWith("tel:")) {
                                callLogGui.displayMessage(JOptionPane.ERROR_MESSAGE,
                                        "Invalid number string; call not attemped.");
                            }
                        } else {
                            // If caller number is not available,
                            // NumberFormatException will be thrown and
                            // message would be displayed to user.
                            Double.parseDouble(number);
                        }

                        // makeCall
                        makeCall(number);
                    } else {
                        // There is no number to call, callLogGui will display
                        // appropriate message.
                        callLogGui.callBack(number);
                    }
                } catch (CstaException cstae) {
                    callLogGui.displayMessage(JOptionPane.ERROR_MESSAGE,
                            "An exception occured while attempting the call.");
                    cstae.printStackTrace();
                } catch (NumberFormatException nfe) {
                    callLogGui.displayMessage(JOptionPane.ERROR_MESSAGE, "Invalid number string; call not attemped.");
                } catch (Exception e) {
                    System.out.println("Error while attempting call.");
                }

            } else if (buttonPressed.equals("deleteall")) {
                // tell callLogGui to make corresponding changes.
                callLogGui.deleteAll();
            } else if (buttonPressed.equals("singleStepConf")) {
                if (callLogGui.getConnectionId() != null) {
                    // tell callLogGui to make corresponding changes.
                    String numToConf;
                    try {
                        numToConf = callLogGui.getNumberToCall();
                        singleStepConf(numToConf);
                    } catch (Exception e) {
                        callLogGui.displayMessage(JOptionPane.ERROR_MESSAGE, "Unable to complete the conference: " + e);
                        e.printStackTrace();
                    }
                } else {
                    callLogGui.displayMessage(JOptionPane.ERROR_MESSAGE, "No active calls to conference with");
                }
            } else if (buttonPressed.equals("hangup")) {

                try {
                    // clear the call
                    clearCall(callLogGui.getConnectionId());

                    // close the call status window
                    callLogGui.closeWindow();

                } catch (ClassCastException cce) {
                    // ClassCastException denotes something
                    // went wrong with GUI, we can't do much
                    // here besides displaying a message.
                    callLogGui.displayMessage(JOptionPane.ERROR_MESSAGE, "Error disconnecting call.");
                    System.out.println("Error while disconnecting call. " + cce);
                    callLogGui.closeWindow();
                } catch (NoActiveCallException nace) {
                    callLogGui.closeWindow();
                } catch (CstaException cstae) {
                    // something went wrong disconnecting the
                    // call, we will get the number we are
                    // trying to disconnect and display that.

                    callLogGui.displayMessage(JOptionPane.ERROR_MESSAGE, "Error disconnecting call : "
                            + (String) callLogGui.whichCallToDisconnect(ae.getSource()).get(PHONE_NUMBER_INDEX));

                    callLogGui.closeWindow();

                }

            } else if (buttonPressed.equals("lookup")) {
                // Find the search string (name to look up). If it is null or
                // empty, simply return.
                String nameToLookUp = dirLookupGui.getNameToLookup();

                if ((nameToLookUp == null) || (nameToLookUp.equals(""))) {
                    return;
                } else {
                    ArrayList<String[]> queryResult;

                    // Use LDAPQueryExecutor object to perform a search for
                    // name entered.
                    try {
                        queryResult = queryExecutor.dirLookup(nameToLookUp);
                    } catch (NamingException ne) {
                        dirLookupGui.displayMessage(JOptionPane.WARNING_MESSAGE,
                                "An exception occured while attempting directory" + "lookup");
                        ne.printStackTrace();
                        return;
                    }

                    // If no result is found in response to query, display a
                    // message to user. Else, display the results in the
                    // DirectoryLookupGUI table.
                    if ((queryResult == null) || (queryResult.size() == 0)) {
                        dirLookupGui.displayMessage(JOptionPane.INFORMATION_MESSAGE,
                                "No information found corresponding to " + nameToLookUp);
                    } else {
                        dirLookupGui.displayData(queryResult);
                    }
                }

            } else if (buttonPressed.equals("DIRcall")) {
                // User performed a directory lookup and now wants to make a
                // call.

                // nameAndNumber would be used to store String array returned
                // by dirLookupGui.getNameAndNumberToDial() method. The first
                // String in the array would be name and second would be number.
                String numberToDial = dirLookupGui.getNumberToDial();

                if (numberToDial == null) {
                    return;
                }

                try {
                    if (! useTelURI) {
                        numberToDial = callParser.parseNumberToDial(numberToDial);
                        // Sanity check! If numberToDial contains any nonnumeric
                        // character or if it is empty string,
                        // NumberFormatException
                        // and NullPointerException would be thrown respectively.
                        Double.parseDouble(numberToDial);
                    }
                    makeCall(numberToDial);
                } catch (CstaException cstae) {
                    callLogGui.displayMessage(JOptionPane.ERROR_MESSAGE,
                            "An exception occured while attempting the call.");
                    cstae.printStackTrace();
                } catch (NumberFormatException nfe) {
                    dirLookupGui.displayMessage(JOptionPane.ERROR_MESSAGE, "Invalid call number");
                    return;
                } catch (NullPointerException npe) {
                    dirLookupGui.displayMessage(JOptionPane.ERROR_MESSAGE, "Invalid call number");
                    return;
                }

            } else if (buttonPressed.equals("DIRdeleteall")) {
                // tell dirLookupGui to delete all entries.
                dirLookupGui.deleteAll();
            } else if (buttonPressed.equals("DIRdelete")) {
                // tell dirLookupGui to delete selected entries.
                dirLookupGui.delete();
            }
        }
    }

    /**
     * Method initGuiInstances provides a central method to initialize the Gui
     * instances
     */
    private void initGuiInstances() {
        actionListener = new MyActionListener();
        // Reads the log and launches GUI initializing it with log read.
        callLogGui = new Click2CallGUI(actionListener, loadLog());

        queryExecutor = new LDAPQueryExecutor(providerUrl, baseDN, securityPrincipal, securityCredentails);
        dirLookupGui = new DirectoryLookupGUI(actionListener);
    }

    /**
     * This class implements the methods of the ServiceProviderListener to
     * respond to the server connection and session events.
     */
    private class MyServiceProviderListener implements ServiceProviderListener {

        /**
         * Received the server connection down event. Attempt to reconnect,
         * reestablishing the previous session.
         * 
         * (non-Javadoc)
         * @see com.avaya.cmapi.ServiceProviderListener#serverConnectionDown(com.avaya.cmapi.evt.ServerConnectionDownEvent)
         */
        public void serverConnectionDown(final ServerConnectionDownEvent event) {
            System.out.println("The connection to the server is down because " + event.getReason());

            if (event.getCode() != ServerConnectionDownEvent.DISCONNECT_APP_INITIATED) {
                System.out.println("Scheduling reconnect attempt in 10 seconds");
                reconnectTimer.schedule(new ReconnectTimerTask(), 10000);
            }
        }

        /**
         * Received the server session not active event. Attempt to disconnect
         * and reconnect the connection, reestablishing the previous session.
         */
        public void serverSessionNotActive(final ServerSessionNotActiveEvent event) {
            System.out.println("The server session is not active because " + event.getReason());

            try {
                System.out.println("Attempting to disconnect and reconnect.");

                // disconnect from the server and leave the session active
                provider.disconnect(false);
                System.out.println("Attempt to reconnect immediately");
                reconnectTimer.schedule(new ReconnectTimerTask(), 0);
            } catch (Exception e) {
                System.out.println("Java Exception - Could not disconnect and " + "reconnect to the server " + e);
                e.printStackTrace(System.out);
                System.exit(0); // exits to the shutdownThread to perform
                                // cleanup
            }
        }

        /**
         * Received the server session terminated event. An attempt will be made
         * to establish a new session.
         * 
         * 
         */
        public void serverSessionTerminated(final ServerSessionTerminatedEvent event) {
            System.out.println("The server session has terminated because " + event.getReason());

            if (event.getCode() != ServerSessionTerminatedEvent.SESSION_TERMINATED_APP_INITIATED) {
                recoverFromTerminatedSession();
            }
        }
    }

    enum EventType {ORIGINATED, DELIVERED, ESTABLISHED, CONFERENCED}
    
    /**
     * Used to handle Call Control events
     */
    private final class MyCallControlListener extends CallControlAdapter {
        
        private final HashMap<String, List<EventType>> callEventMap = new HashMap<String, List<EventType>>(); 
//        private final List<String> establishedCallList = Collections.synchronizedList(new ArrayList<String>());
        
        private void addEvent(final String callId, final EventType eventType) {
            List<EventType> eventList = callEventMap.get(callId);
            if (eventList==null) {
                eventList = new ArrayList<EventType>();
                callEventMap.put(callId, eventList);
            }
            
            eventList.add(eventType);
        }
        
        /*
         * (non-Javadoc)
         * @see ch.ecma.csta.callcontrol.CallControlAdapter#conferenced(ch.ecma.csta.binding.ConferencedEvent)
         */
        @Override
        public void conferenced(final ConferencedEvent event) {
            System.out.println("ConferencedEvent Received!");
            addEvent(event.getPrimaryOldCall().getCallID(), EventType.CONFERENCED);
        }

        /**
         * When the ConnectionClearedEvent is received, perform some cleanup
         */
        @Override
        public void connectionCleared(final ConnectionClearedEvent event) {
            String releasingDevice;
            if (useTelURI) {
                releasingDevice = event.getReleasingDevice().getDeviceIdentifier().getContent();
            } else {
                releasingDevice = event.getReleasingDevice().getDeviceIdentifier().getExtension();
            }
            
            // the connection cleared will always be sent once for the monitored device
            // if far end hangs up then there will be 2 connection cleared events
            System.out.println("ConnectionClearedEvent -> ReleasingDevice = " + releasingDevice);
            
            String callId = event.getDroppedConnection().getCallID();
            List<EventType> eventTypeList = callEventMap.remove(callId);
            if (eventTypeList != null && !eventTypeList.contains(EventType.ESTABLISHED)
                    && !eventTypeList.contains(EventType.ORIGINATED)) {
                // Then someone called this device but it never answered
                logCall(releasingDevice, "Missed");
            }
            
            if (!releasingDevice.equalsIgnoreCase(extension)) {
                if (eventTypeList!=null && eventTypeList.contains(EventType.ESTABLISHED)
                        && eventTypeList.contains(EventType.ORIGINATED)) {
                    callLogGui.clearReturnCall();
                }
    
                // try to close the window if the called device terminates the call
                callLogGui.closeWindow();
            }
        }

        /**
         * The call was delivered to the device, update the call log
         * accordingly.
         */
        @Override
        public void delivered(final DeliveredEvent event) {
            System.out.println("DeliveredEvent Received!");
            addEvent(event.getConnection().getCallID(), EventType.DELIVERED);

            // If the call was successfully made, change call status in GUI
            String callingDevice;
            if (useTelURI) {
                callingDevice = event.getCallingDevice().getDeviceIdentifier().getContent();
            } else {
                callingDevice = event.getCallingDevice().getDeviceIdentifier().getExtension();
            }

            if (callingDevice.equalsIgnoreCase(extension)) {
                // this device placed the call
                // add dialed to the log
                // this is done here instead of established event
                // in case the far end does not answer
                String calledDevice;
                if (useTelURI) {
                    calledDevice = event.getCalledDevice().getDeviceIdentifier().getContent();
                } else {
                    calledDevice = event.getCalledDevice().getDeviceIdentifier().getExtension();
                }

                String alertingDevice;
                if (useTelURI) {
                    alertingDevice = event.getAlertingDevice().getDeviceIdentifier().getContent();
                } else {
                    alertingDevice = event.getAlertingDevice().getDeviceIdentifier().getExtension();
                }

                if (!extension.equalsIgnoreCase(alertingDevice)) {
                    // add dialed if this is not a called back call
                    if (!callLogGui.isReturnCall()) {
                        logCall(calledDevice, "Dialed");
                    }

                }
            }
        }

        /**
         * The call was answered, update the call log/GUI accordingly.
         */
        @Override
        public void established(final EstablishedEvent event) {
            System.out.println("EstablishedEvent Received!");

//          establishedCallList.add(event.getEstablishedConnection().getCallID());
            addEvent(event.getEstablishedConnection().getCallID(), EventType.ESTABLISHED);

            String calledDevice;
            if (useTelURI) {
                calledDevice = event.getCalledDevice().getDeviceIdentifier().getContent();
            } else {
                calledDevice = event.getCalledDevice().getDeviceIdentifier().getExtension();
            }
            if (calledDevice.equalsIgnoreCase(extension)) {
                // a call was received by this device
                // get the calling device
                String callingDevice;
                if (useTelURI) {
                    callingDevice = event.getCallingDevice().getDeviceIdentifier().getContent();
                } else {
                    callingDevice = event.getCallingDevice().getDeviceIdentifier().getExtension();
                }
                logCall(callingDevice, "Answered");
            }

            // update the call status window
            callLogGui.setConnectedCallStatus();
        }

        /*
         * (non-Javadoc)
         * @see ch.ecma.csta.callcontrol.CallControlAdapter#originated(ch.ecma.csta.binding.OriginatedEvent)
         */
        @Override
        public void originated(final OriginatedEvent event) {
            System.out.println("OriginatedEvent Received!");
            addEvent(event.getOriginatedConnection().getCallID(), EventType.ORIGINATED);
        }
    }

    private class ReconnectTimerTask extends TimerTask {

        @Override
        public void run() {
            try {
                System.out.println("Attempting to reconnect.");
                provider.reconnect();               
            } catch (SessionCleanedUpException ex) {
                recoverFromTerminatedSession();
            } catch (Exception e) {
                System.out.println(
                    "Java Exception - Could not reconnect to the server "
                        + e);
                e.printStackTrace(System.out);
                System.out.println("Will try again in 10 seconds");
                reconnectTimer.schedule(new ReconnectTimerTask(), 10000);
                return;
            }
            System.out.println("Reconnected, canceling timer");
        }
    }
    
    /**
     * This is a very simple class that is used to do cleanup when the app is
     * terminated. The Communication Manager API server code will clean up if an
     * app goes away unexpectedly, but it's still good to clean up.
     */
    private class MyShutdownThread extends Thread {
        public void run() {
            cleanup();
        }
    }
}
