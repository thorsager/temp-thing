/*
 * Click2Call.java
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

//*****************************************************************************
//* Package
//*****************************************************************************

package sampleapps.click2call;

//*****************************************************************************
//* Imports
//*****************************************************************************

// Java utilities
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.naming.NamingException;
import javax.swing.JOptionPane;

import sampleapps.ccs.click2call.LDAPQueryExecutor;
import sampleapps.station.AvayaStation;
import sampleapps.station.AvayaStationAdapter;
import ch.ecma.csta.binding.ButtonItem;
import ch.ecma.csta.binding.ButtonList;
import ch.ecma.csta.binding.DeviceID;
import ch.ecma.csta.binding.DisplayUpdatedEvent;
import ch.ecma.csta.binding.GetButtonInformation;
import ch.ecma.csta.binding.GetButtonInformationResponse;
import ch.ecma.csta.binding.GetLampMode;
import ch.ecma.csta.binding.GetLampModeResponse;
import ch.ecma.csta.binding.LampModeItem;
import ch.ecma.csta.binding.LampModeList;
import ch.ecma.csta.errors.CstaException;
import ch.ecma.csta.physical.PhysicalDeviceServices;

import com.avaya.api.sessionsvc.exceptions.SessionCleanedUpException;
import com.avaya.cmapi.APIProtocolVersion;
import com.avaya.cmapi.ServiceProvider;
import com.avaya.cmapi.ServiceProviderListener;
import com.avaya.cmapi.evt.ServerConnectionDownEvent;
import com.avaya.cmapi.evt.ServerSessionNotActiveEvent;
import com.avaya.cmapi.evt.ServerSessionTerminatedEvent;
import com.avaya.csta.async.AsynchronousCallback;
import com.avaya.csta.binding.RegisterTerminalResponse;
import com.avaya.csta.binding.TerminalUnregisteredEvent;
import com.avaya.csta.binding.types.DependencyMode;
import com.avaya.csta.binding.types.DeviceInstance;
import com.avaya.csta.binding.types.MediaMode;
import com.avaya.csta.physical.ButtonFunctionConstants;
import com.avaya.csta.physical.LampModeConstants;
import com.avaya.csta.registration.RegistrationAdapter;
import com.avaya.csta.registration.RegistrationConstants;
import com.avaya.mvcs.framework.CmapiKeys;

//*****************************************************************************
//* Click2Call
//*****************************************************************************

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
 * back, its status changes from "Missed" to "Called back". Calls with
 * different status ("Missed", "Answered", "Called back" and "Dialed") are
 * displayed in different colors.
 *
 * High Level Steps: This application registers an extension as a CMAPI phone
 * (that is softphone enabled) with the Communication Manager in shared control
 * mode. Registration is done asynchronously using Asynchronous Services.
 * The application uses the call appearance lamps to watch the station's
 * call activity. The application logs both incoming and dialed calls and
 * reports in a GUI. It also provides a GUI to perform directory search.
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
 * Possible variations: You can modify this application very easily to filter
 * on particular types of calls such as: all outside calls, only outside calls
 * from area code 402, only calls from a particular person or number.
 *
 * build and run: You can build the click2call application and run it by going
 * to cmapisdk/examples/bin and running the script runClick2Call.sh.
 *
 * @author AVAYA, Inc.
 */

public class Click2Call implements TableConstants
{

    private static final String SECURITY_CREDENTAILS = "ldap_securityCredentails";

    private static final String SECURITY_PRINCIPAL = "ldap_securityPrincipal";

    // This field is used to track the name/number of the calling party until a
    // lamp update comes in and it can be stored in the hash table.
    // Initialize to null string to help keep track of null string
    // display updates coming from Communication Manager.
    private static final String EMPTY_STRING = "";

    private String tempWhoCalled = EMPTY_STRING;
    
    // This hash table is used to track the button IDs of all call
    // apprearances, the name/number of who called and the status of whether or
    // not information needs to be displayed in the GUI.
    private Hashtable<String, MyCallApprStatus> myCaHashIds = new Hashtable<String, MyCallApprStatus>();

    // These fields are read in from the click2call.properties file. The
    // application properities file must be populated with a valid IP address
    // for Avaya Communication Manager and a valid extension number and
    // password.
    // NOTE: providerUrl is the URL of LDAP server. It should be of the form
    // ldap://ldap_server_address:port/search_root
    // port may be optional in the URL.

    private String callServer;
    private String extension;
    private String password;
    private String providerUrl;
    private String baseDN;
    private String securityPrincipal;
    private String securityCredentails;
    
    // This variable is initialized in the bootstrap() call. The variable is defined
    // as a member variable to allow recovery once an application has already started.  
    private Properties spProp;
    
    // name of properties file.
    private static final String PROPERTY_FILE = "click2call.properties";
    private static final String PROP_FILE_CALL_SERVER = "callserver";
    private static final String PROP_FILE_EXTENSION = "extension";
    private static final String PROP_FILE_EXT_PASSWORD = "password";
    private static final String PROP_FILE_CMAPI_SERVER = "cmapi1.server_ip";
    private static final String PROP_FILE_SERVER_PORT ="cmapi1.server_port";
    private static final String PROP_FILE_SECURE ="cmapi1.secure";
    
    private static final String SESSION_DURATION = "20";
    private static final String CLEANUP_DELAY = "60";
    private static final String APP_DESCRIPTION = "Click2Call";
    private static final String PROVIDER_URL = "providerUrl";
    private static final String BASE_DN = "baseDN";

    // This thread is used to receive a shutdown notification when the
    // application is terminated.
    private MyShutdownThread shutdownThread;

    // The length of display on the station monitored.
    private int display_length;

    // Keep track of which lamp update is received.
    private int count = 0;
    private LampModeItem lamp = null;

    // The application
    private static Click2Call app;

    // The Device ID for our extension number will be supplied by the
    // Communication Manager API. The Connection ID is formulated from the
    // Device ID.
    private DeviceID id = null;

    // The Service Provider used to get the handles of various services.
    private ServiceProvider provider;
    private ServiceProviderListener serviceProviderListener;

    // These fields will be populated with the handles of the various services,
    // which we will get through the ServiceProvider class.
    private PhysicalDeviceServices phySvcs;
    private AvayaStation station;

    // These are the listeners that will be added to listen for events coming
    // from TerminalServices and AvayaStationServices.
    private MyRegistrationListener registrationListener;
    private MyAvayaStationListener avayaStationListener;

    // callParser parse the lamp update received to extract name &/or number
    // of caller.
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
    private static final int CALL_APPEARANCE_INDEX = 1;

    // Contents of log file are read into dataContent which is passed to GUI
    // object as initialization data.
    private String[][] dataContent;

    // Maintain a reference to the login gui instance.
    private LoginGUI loginGUI = null;
    
    private Timer reconnectTimer = new Timer("Reconnect Timer"); 

    /** 
     * Creates an instance of the Click2Call class and invoke the 
     * Login GUI
     */
    public static void main(String[] args)
    {
        app = new Click2Call();
        app.login();
    }
    
    /**
     * Method used to initialize the Login GUI instances.
     *
     */
    private void login(){
    	loginGUI = new LoginGUI(app);
    }
    

    /**
     * Method used to invoke the Click2Call's bootstrap and start method.
     * The Login GUI will be redisplayed on all Login failures.
     * @param user - Username used during login
     * @param passwd - Passwors used during login
     */
    public void startup(StringBuffer user, StringBuffer passwd){
        try
        {
            app.bootstrap(user, passwd);
            app.start();
        }
        catch (CstaException e)
        {
            System.out.println("Could not start the app");
            e.printStackTrace(System.out);
            System.exit(0); // exits to the shutdownThread to perform cleanup
        }
        catch (RuntimeException e)
        {
            System.out.println("Could not start the app");
            e.printStackTrace(System.out);
            if (e.getCause() instanceof SecurityException) {
                loginGUI = new LoginGUI(app);
                loginGUI.failed();
            }
        }
        catch (Exception e)
        {
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
     * @throws CstaException
     *             if a CstaException is generated by one of the calls to
     *             service provider, it is thrown to the caller.
     * @throws Exception
     *             if any runtime exception is generated, it is thrown to the
     *             caller.
     */
    public void bootstrap(StringBuffer user, StringBuffer passwd) throws CstaException, Exception
    {        
        String cmapiServerIp;
        String cmapiServerPort;
        String cmapiSecure;
        String cmapiTrustStoreLocation;
        
        // add following properties for validation client and service side certifications 
        String cmapiTrustStorePassword;
        String cmapiKeyStoreLocation;
        String cmapiKeyStorePassword;
        String isValidPeer;
        String hostnameValidation;
        // Get all of the arguments from the properties file
        ClassLoader cl = Click2Call.class.getClassLoader();
        URL appURL = cl.getResource(PROPERTY_FILE);
        Properties appProp = new Properties();
        appProp.load(appURL.openStream());
        callServer = appProp.getProperty(PROP_FILE_CALL_SERVER).trim();
        extension = appProp.getProperty(PROP_FILE_EXTENSION).trim();
        password = appProp.getProperty(PROP_FILE_EXT_PASSWORD).trim();
        
        providerUrl = appProp.getProperty(PROVIDER_URL).trim();
        baseDN = appProp.getProperty(BASE_DN).trim();
        
        securityPrincipal = appProp.getProperty(SECURITY_PRINCIPAL);
        if(securityPrincipal != null)
            securityPrincipal = securityPrincipal.trim();
        securityCredentails = appProp.getProperty(SECURITY_CREDENTAILS);
        if(securityCredentails != null)
            securityCredentails = securityCredentails.trim();
            
        cmapiServerIp = appProp.getProperty(PROP_FILE_CMAPI_SERVER).trim();
        
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

        spProp = new Properties();
        spProp.setProperty(CmapiKeys.SERVICE_PROVIDER_ADDRESS, cmapiServerIp);
        spProp.setProperty(CmapiKeys.CMAPI_USERNAME, user.toString().trim());
        spProp.setProperty(CmapiKeys.CMAPI_PASSWORD, passwd.toString().trim());
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

        // Get a handle to physical device services so I can get all buttons
        // administered on the device as well as lamp mode information.
        phySvcs =
            (PhysicalDeviceServices) provider.getService(
                ch.ecma.csta.physical.PhysicalDeviceServices.class.getName());

        // Create new AvayaStation object. Since we are registering in DEPENDENT Mode,
        // do not use DeviceInstance=0 (used by MAIN).
        station = new AvayaStation();
        station.init(callServer, "", extension, DeviceInstance.VALUE_2, provider);

        // initialize callParser to parse incoming calls.
        callParser = new DisplayParser();
    }

    /**
     * This method actually starts the app. It must be called after calling
     * bootstrap or it will fail.
     *
     * @throws CstaException
     *             if a CstaException is generated by one of the calls to the
     *             services, it is thrown to the caller.
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
        // will clean up if an app goes away unexpectedly, but it's still
        // good to clean up.
        shutdownThread = new MyShutdownThread();
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        // The first thing that we do is to get a Device ID. The Device ID
        // is used in the requests to the services. Calling this method
        // populates the id field of the Click2Call class.
        id = station.getDeviceID();

        // Add appropriate listeners to listen to events during and after
        // registration.
        addListeners();

        // Registers an extension in DEPENDENT mode with no media for use as a
        // CMAPI softphone station (IP_API_A) with Communication Mananger
        // and the connector server.  
        // Register asynchronously since there's no reason to hold up this
        // thread waiting for the response.

        station.register(password, DependencyMode.DEPENDENT, MediaMode.NONE, null, null, true,
                null, null, new MyAsyncRegistrationCallback());

        // That's all we do for now! The main thread is now going to go
        // away, and the rest of the action occurs as a result of events
        // coming into our listeners. Note: the Communication Manager API
        // client code has some persistent threads that keep the application
        // alive. The rest of the code executes in the context of these
        // threads.

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
    public void recoverFromTerminatedSession()
    {
        try {
            if (spProp != null) {                   
                provider.removeServiceProviderListener(serviceProviderListener);

                // Get a handle to the ServiceProvider class.
                provider = ServiceProvider.getServiceProvider(spProp);
    
                // Get new  handle to Physical Device Services from new Service Provider
                phySvcs =
                    (PhysicalDeviceServices) provider.getService(
                        ch.ecma.csta.physical.PhysicalDeviceServices.class.getName());
    
            	// Re-initialize the station obj.
                station = new AvayaStation();
                station.init(callServer, "", extension, provider);
                
                // The first thing that we do is to get a Device ID. The Device ID
                // is used in the requests to the services. Calling this method
                // populates the id field of the Click2Call class.
                id = station.getDeviceID();
    
                // Add appropriate listeners to listen to events during and after
                // registration.
                addListeners();
    
                // Registers an extension in DEPENDENT mode with no media for use as a
                // CMAPI softphone station (IP_API_A) with Communication Mananger
                // and the connector server.
    
        		station.register(password, DependencyMode.DEPENDENT, MediaMode.NONE, null, null, true,
        				null, null, new MyAsyncRegistrationCallback());
            }    
            else {
                throw new Exception("bootstrap() must be called before recoverFromTerminatedSession()" +
                                    " can be called.");
            }
        }
        catch (Exception e)
        {
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
    public void cleanup()
    {
        System.out.println(
            "Application is terminating; performing cleanup....");

        // Write the call data to LOG_FILE. Logs will be read at next launch of
        // this application.
        writeLog();

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
            // Make sure no listeners were left around.
            removeListeners();

            System.out.println("calling station.unregister");
            // Call the synchronous version of unregister() because we want the
            // unregister to succeed before cleaning up and disconnecting.
            station.unregister();
            station.cleanup();
            
            System.out.println("calling provider.disconnect");
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
     * This loadLog method reads old call log (if any) saved in the log file
     * and stores it in a String array. The string array is used to initialize
     * the GUI.
     */
    public String[][] loadLog()
    {
        // Open a "connection" to the log file, create FileReader and read
        // the log into buffer.
        File file = new File(LOG_FILE);
        FileReader fileReader;
        StringBuffer buffer;

        // If file does not exists, no logs to load, so simply return null.
        if (!file.exists())
        {
            return null;
        }

        try
        {
            fileReader = new FileReader(file);

            buffer = new StringBuffer();

            // It would be more efficient to read data in chunks,
            // but for simplicity of code, we read data character
            // by character here.

            for (int ch = fileReader.read(); ch != -1; ch = fileReader.read())
            {
                buffer.append((char) ch);
            }

            // Every field of a particular call is stored in separate
            // lines in log file
            StringTokenizer tokens =
                new StringTokenizer(new String(buffer), "\n");

            // Sanity check! Every logged call has four fields, name, number,
            // time and status. If log file is corrupted disregard it and
            // return null.
            if ((tokens.countTokens() % NUM_OF_COLUMNS_PER_CALL != 0)
                || (tokens.countTokens() == 0))
            {
                System.out.println("Corrupted data in " + LOG_FILE + " file");
                return null;
            }

            dataContent =
                new String[tokens.countTokens()
                    / NUM_OF_COLUMNS_PER_CALL][NUM_OF_COLUMNS_PER_CALL];
            int counter = tokens.countTokens() / NUM_OF_COLUMNS_PER_CALL;

            for (int i = 0; i < counter; i++)
            {
                for (int j = 0; j < NUM_OF_COLUMNS_PER_CALL; j++)
                {
                    dataContent[i][j] = tokens.nextToken();
                }
            }

            return dataContent;
        }
        catch (IOException ioe)
        {
            System.out.println("Error loading logs.");
            return null;
        }
    }

    /**
     * writeLog method write the log back to log file at application shutdown.
     */
    public void writeLog()
    {
        // If registration failed, callLogGui will be null.  In this case,
        // do nothing.
        if (callLogGui == null) {
            return;
        }
        
        File file = new File(LOG_FILE);
        FileWriter fileWriter;
        String newLine = "\n"; // delimeter for various fields of a call
        // and different calls.

        ArrayList<?> tableData = callLogGui.getTableData();

        try
        {
            // Delete the old file and create new one. If no logs are there
            // to write, just don't create the log file.
            file.delete();

            if ((tableData == null) || (tableData.size() == 0))
            {
                return;
            }

            file.createNewFile();
            fileWriter = new FileWriter(file);

            for (int i = 0; i < tableData.size(); i++)
            {
                fileWriter.write(
                    tableData.get(i).toString(),
                    0,
                    tableData.get(i).toString().length());
                fileWriter.write(newLine, 0, newLine.length());
            }

            fileWriter.close();
        }
        catch (IOException e)
        {
            System.out.println("Error writing logs to file");
            return;
        }

    }

    /**
     * addListeners add registration and Avaya Station listeners to receive
     * events during boot and run time.
     *
     * @throws CstaException if CstaException is generated while adding
     * listeners and it is thrown to the caller.
     */
    private void addListeners() throws CstaException, Exception
    {
        // Add a registration listener so we can receive events indicating
        // when the phone is registered & unregistered.
        registrationListener = new MyRegistrationListener();
        station.addListener(registrationListener);

        // Add a physical device listener to receive events indicating phone
        // rings, lamp updates and display updates.
        avayaStationListener = new MyAvayaStationListener();
        station.addListener(avayaStationListener);

        // Add a service provider listener to receive events indicating
        // the state of the server connection or session.  
        serviceProviderListener = new MyServiceProviderListener();
        provider.addServiceProviderListener(serviceProviderListener);
    }

    /**
     * removeListeners removes registration and Avaya Station listeners added
     * during startup.
     *
     * @throws CstaException if CstaException is generated while removing
     * listeners and it is thrown to the caller.
     */
    private void removeListeners() throws CstaException, Exception
    {
        station.removeListener(registrationListener);
        station.removeListener(avayaStationListener);
        provider.removeServiceProviderListener(serviceProviderListener);
    }

    /**
     * logCall simply logs an incoming call.
     *
     * @param call to be logged, callStatus(Missed/Answered)
     */
    public void logCall(String call, String callStatus)
    {
        if (call == null || callStatus == null)
        {
            System.out.println("Invalid call data; not logging this call.");
            return;
        }

        ArrayList<?> callData = callParser.parseString(call, callStatus);

        String Name =
            (String) callData.get(CallLogTableConstants.NAME_COLUMN_INDEX);
        String number =
            (String) callData.get(CallLogTableConstants.NUMBER_COLUMN_INDEX);
        String callTime =
            (String) callData.get(CallLogTableConstants.TIME_COLUMN_INDEX);

        callLogGui.displayCall(Name, number, callTime, callStatus);
    }
   
    /**
     * This class stores status of all the call appearances on the monitored
     * station.
     */
    private class MyCallApprStatus
    {
        // ifCallMissed denotes if the call was missed or answered.
        boolean ifCallMissed = false;

        // whoCalled stores callerID.
        String whoCalled = null;

        // initialize all lamps at boot to status OFF and update status
        // according to lamp update events.
        Short currentLampStatus = LampModeConstants.OFF;

        // ifOffhook tells if a call is currently established on this lamp.
        boolean ifDialing = false;

        // string to store the latest usefull display update received while
        // recording the call being dialed.
        String currentDialDisplayUpdate = null;

        public void setWhoCalled(String callingparty)
        {
            this.whoCalled = callingparty;
        }

        public String getWhoCalled()
        {
            return this.whoCalled;
        }

        public void setIfCallMissed(boolean value)
        {
            this.ifCallMissed = value;
        }

        public boolean getIfCallMissed()
        {
            return this.ifCallMissed;
        }

        public void setCurrentLampStatus(Short lampStatus)
        {
            this.currentLampStatus = lampStatus;
        }

        public Short getCurrentLampStatus()
        {
            return this.currentLampStatus;
        }

        public void setIfDialing(boolean value)
        {
            this.ifDialing = value;
        }

        public boolean getIfDialing()
        {
            return this.ifDialing;
        }

        public void setCurrentDialDisplayUpdate(String value)
        {
            this.currentDialDisplayUpdate = value;
        }

        public String getCurrentDialDisplayUpdate()
        {
            return this.currentDialDisplayUpdate;
        }

    }

    private class MyActionListener implements ActionListener
    {
        /**
         * Actions generated by GUI are received here and corresponding actions
         * are taken.
         */
        public void actionPerformed(ActionEvent ae)
        {
            // determine which button is pressed by the user and take
            // appropriate action
            String buttonPressed = ae.getActionCommand();

            if (buttonPressed == null)
            {
                return;
            }
            else if (buttonPressed.equals("delete"))
            {
                // remove the selected call from callLogGui.
                callLogGui.delete();
            }
            else if (buttonPressed.equals("callback"))
            {
                String number = null;
                String name = null;

                try
                {
                    number = callLogGui.getNumberToCall();
                    name = callLogGui.getNameToCall();

                    if ((number != null) && (number.trim().length() != 0))
                    {
                        // If caller number is not available,
                        // NumberFormatException will be thrown and
                        // message would be displayed to user.
                        Double.parseDouble(number);

                        String callAppearanceUsed = null;

                        callAppearanceUsed = station.initCall(number);

                        // If no call appearance is currently free to
                        // make this call, display a message to user.
                        if (callAppearanceUsed == null)
                        {
                            callLogGui.displayMessage(
                                JOptionPane.ERROR_MESSAGE,
                                "No more free call appearances available. "
                                    + "Please disconnect"
                                    + "\n"
                                    + "some of the active calls before "
                                    + "attempting new ones.");
                        }
                        else
                        {
                            // If the call was successfully made
                            // change call status in GUI.
                            callLogGui.displayCallStatus(
                                this,
                                number,
                                name,
                                callAppearanceUsed);

                            MyCallApprStatus callApprStatus =
                                myCaHashIds.get(
                                callAppearanceUsed);

                            // Here we are returning a received call. So just
                            // change the status of the call, don't log it as
                            // a separate "Dialed" call.
                            if (callApprStatus != null)
                            {
                                callApprStatus.setIfDialing(false);
                            }
                            callLogGui.callBack(number);
                        }
                    }
                    else
                    {
                        // There is no number to call, callLogGui will display
                        // appropriate message.
                        callLogGui.callBack(number);
                    }

                }
                catch (CstaException cstae)
                {
                    callLogGui.displayMessage(
                        JOptionPane.ERROR_MESSAGE,
                        "An exception occured while attempting the call.");
                    cstae.printStackTrace();
                }
                catch (NumberFormatException nfe)
                {
                    callLogGui.displayMessage(
                        JOptionPane.ERROR_MESSAGE,
                        "Invalid number string; call not attemped.");
                }
                catch (Exception e)
                {
                    System.out.println("Error while attempting call.");
                }

            }
            else if (buttonPressed.equals("deleteall"))
            {
                // tell callLogGui to make corresponding changes.
                callLogGui.deleteAll();
            }
            else if (buttonPressed.equals("hangup"))
            {

                try
                {
                    // obtain the callAppearance call to be
                    // disconnected is going on and
                    // disconnect that call.
                    String callAppearance =
                        (String) callLogGui.whichCallToDisconnect(
                            ae.getSource()).get(
                            CALL_APPEARANCE_INDEX);
                    station.disconnect(callAppearance);
                }
                catch (ClassCastException cce)
                {
                    // ClassCastException denotes something
                    // went wrong with GUI, we can't do much
                    // here besides displaying a message.
                    callLogGui.displayMessage(
                        JOptionPane.ERROR_MESSAGE,
                        "Error disconnecting call.");
                    System.out.println(
                        "Error while disconnecting call. " + cce);
                }
                catch (CstaException cstae)
                {
                    // something went wrong disconnecting the
                    // call, we will get the number we are
                    // trying to disconnect and display that.
                    callLogGui.displayMessage(
                        JOptionPane.ERROR_MESSAGE,
                        "Error disconnecting call : "
                            + (String) callLogGui.whichCallToDisconnect(
                                ae.getSource()).get(
                                PHONE_NUMBER_INDEX));

                }

            }
            else if (buttonPressed.equals("lookup"))
            {
                // Find the search string (name to look up). If it is null or
                // empty, simply return.
                String nameToLookUp = dirLookupGui.getNameToLookup();

                if ((nameToLookUp == null) || (nameToLookUp.equals("")))
                {                	
                    return;
                }
                else
                {
                    ArrayList<String[]> queryResult;

                    // Use LDAPQueryExecutor object to perform a search for
                    // name entered.
                    try
                    {
                        queryResult = queryExecutor.dirLookup(nameToLookUp);
                    }
                    catch (NamingException ne)
                    {
                        dirLookupGui.displayMessage(
                            JOptionPane.WARNING_MESSAGE,
                            "An exception occured while attempting directory"
                                + "lookup");
                        ne.printStackTrace();
                        return;
                    }

                    // If no result is found in response to query, display a
                    // message to user. Else, display the results in the
                    // DirectoryLookupGUI table.
                    if ((queryResult == null) || (queryResult.size() == 0))
                    {
                        dirLookupGui.displayMessage(
                            JOptionPane.INFORMATION_MESSAGE,
                            "No information found corresponding to "
                                + nameToLookUp);
                    }
                    else
                    {
                        dirLookupGui.displayData(queryResult);
                    }
                }

            }
            else if (buttonPressed.equals("DIRcall"))
            {
                // User performed a directory lookup and now wants to make a
                // call.

                // nameAndNumber would be used to store String array returned
                // by dirLookupGui.getNameAndNumberToDial() method. The first
                // String in the array would be name and second would be number.
                String[] nameAndNumber;
                String numberToDial;
                String nameOfCallee;

                nameAndNumber = dirLookupGui.getNameAndNumberToDial();

                if (nameAndNumber == null)
                {
                    return;
                }

                nameOfCallee = nameAndNumber[0];
                numberToDial = nameAndNumber[1];

                if (numberToDial == null)
                {
                    return;
                }

                try
                {

                    numberToDial = callParser.parseNumberToDial(numberToDial);

                    // Sanity check! If numberToDial contains any nonnumeric
                    // character or if it is empty string,
                    // NumberFormatException
                    // and NullPointerException would be thrown respectively.
                    Double.parseDouble(numberToDial);

                    String callAppearanceUsed = station.initCall(numberToDial);

                    // If no call appearance is currently free to
                    // make this call, display a message to user.
                    if (callAppearanceUsed == null)
                    {
                        callLogGui.displayMessage(
                            JOptionPane.ERROR_MESSAGE,
                            "No more free call appearances available. "
                                + "Please disconnect"
                                + "\n"
                                + "some of the active calls before "
                                + "attempting new ones.");
                    }
                    else
                    {
                        // If the call was successfully made
                        // change call status in GUI.
                        callLogGui.displayCallStatus(
                            this,
                            numberToDial,
                            nameOfCallee,
                            callAppearanceUsed);
                        
                        // This dialed call would be logged when user
                        // disconnects it.
                    }
                }
                catch (CstaException cstae)
                {
                    callLogGui.displayMessage(
                        JOptionPane.ERROR_MESSAGE,
                        "An exception occured while attempting the call.");
                    cstae.printStackTrace();
                }
                catch (NumberFormatException nfe)
                {
                    dirLookupGui.displayMessage(
                        JOptionPane.ERROR_MESSAGE,
                        "Invalid call number");
                    return;
                }
                catch (NullPointerException npe)
                {
                    dirLookupGui.displayMessage(
                        JOptionPane.ERROR_MESSAGE,
                        "Invalid call number");
                    return;
                }

            }
            else if (buttonPressed.equals("DIRdeleteall"))
            {
                // tell dirLookupGui to delete all entries.
                dirLookupGui.deleteAll();
            }
            else if (buttonPressed.equals("DIRdelete"))
            {
                // tell dirLookupGui to delete selected entries.
                dirLookupGui.delete();
            }

        }
    }

    /**
     * When a call appearance lamp goes "OFF", this method checks if a call was
     * being dialed through that call appearance lamp and logs it. If a call
     * was received (missed or answered) on that call appearance lamp, it
     * simply returns.
     *
     * @param lampID
     */
    private void logDialedCall(String lampID)
    {
        MyCallApprStatus callApprStatus =
            myCaHashIds.get(lampID);
        
        if (!callApprStatus.getIfDialing())
        {
            return;
        }
        
        callLogGui.removeCallStatus(lampID);

        // If the user activated a call appearance, but 
        String currentDisplay = callApprStatus.getCurrentDialDisplayUpdate();
        if (currentDisplay == null) {
            return;
        }                      
        
        // log the dialed call here.
        ArrayList<?> callData =
            callParser.parseString(
                currentDisplay,
                "Dialed");
        callLogGui.displayCall(
            (String) callData.get(0),
            (String) callData.get(1),
            (String) callData.get(2),
            (String) callData.get(3));
    }
    
    /**
     * Method initGuiInstances
     * provides a central method to initialize the Gui instances
     *
     */
    private void initGuiInstances()
    {
        actionListener = new MyActionListener();
        // Reads the log and launches GUI initializing it with log read.
        callLogGui = new Click2CallGUI(actionListener, loadLog());

        queryExecutor = new LDAPQueryExecutor(providerUrl, baseDN, securityPrincipal, securityCredentails);
        dirLookupGui = new DirectoryLookupGUI(actionListener);
    }    
    
    /**
     * This class implements various methods of AvayaStationAdapter to receive
     * and parse all display updates occurring on monitored station.
     * 
     * Note: All the events are delivered on one thread. This guarantees
     * sequential processing of events. However if the processing of an
     * event involves an I/O as this function does, you may
     * want to process this in a different thread.
     * One approach would be to have a QueuedExecutor for the application
     * that would process events. All the Listeners would queue the events
     * to a queue to be executed by QueuedExecutor if, either they are blocking or
     * they require sequential processing.
     */
    private class MyAvayaStationListener extends AvayaStationAdapter
    {

        public void displayUpdated(DisplayUpdatedEvent event)
        {

            display_length = event.getContentsOfDisplay().length();

            if (event.getContentsOfDisplay().trim().equals(EMPTY_STRING)
                || event.getContentsOfDisplay().trim().length() == 2)
            {
                return;
            }

            // Since there are many display updates coming in from CM, we need
            // to capture the display and store it. The first non-null display
            // is the one with the name of who is calling. The second non-null
            // display is the one with the name/number of who is calling.
            // Therefore, the second non-null display is the one to capture.
            // The whoCalled variable will store the data until the lampMode
            // event at which time it will be stored in the hash table and the
            // local variable will be cleared out.

            // first non-null display - need to wait for the second non-null
            // display as it has both the name and number.

            GetLampMode lampModeInfo;
            GetLampModeResponse lampModeResponse;
            LampModeList lmlist;

            try
            {

                // Get the information about all buttons on this
                // softphone.
                // Create a new request and populate the device ID.
                lampModeInfo = new GetLampMode();
                lampModeInfo.setDevice(id);
                // Send the request and get the response, catching any
                // resulting CstaExceptions.
                lampModeResponse = phySvcs.getLampMode(lampModeInfo);

                lmlist = lampModeResponse.getLampModeList();

                for (Enumeration<?> e = lmlist.enumerateLampModeItem();
                    e.hasMoreElements();
                    )
                {
                    lamp = ((LampModeItem) e.nextElement());

                    if ((lamp.getButton() != null)
                        && (lamp.getLampColor().equals(LampModeConstants.GREEN))
                        && (lamp.getLampMode().equals(LampModeConstants.STEADY)))
                    {
                        MyCallApprStatus castatus =
                            myCaHashIds.get(
                            lamp.getButton());

                        if ((castatus != null)
                            && (castatus.getIfDialing() == true))
                        {
                            castatus.setCurrentDialDisplayUpdate(
                                event
                                    .getContentsOfDisplay()
                                    .substring(2)
                                    .trim());
                        }

                    }
                }

            }
            catch (CstaException e)
            {
                System.out.println(
                    "CSTA Exception - Could not get"
                        + " button information "
                        + e);
                e.printStackTrace(System.out);
                return;
            }
            catch (Exception e)
            {
                System.out.println(
                    "Java Exception - Could not get"
                        + " button information "
                        + e);
                e.printStackTrace(System.out);
                return;
            }

            if (tempWhoCalled.equals(EMPTY_STRING) && (count == 0))
            {
                count = 1;
            }
            else if (tempWhoCalled.equals(EMPTY_STRING) && (count == 1))
            {

                System.out.println(
                    "Display update event: " + event.getContentsOfDisplay());

                tempWhoCalled =
                    event
                        .getContentsOfDisplay()
                        .substring(2, display_length)
                        .trim();

                // Set count to two. The second display update with
                // information (non null display update) has the called
                // number with it. That is the one we want to store and display.
                count = 2;

                /**
                 * Check for a flashing call appearance lamp. When found tie
                 * the name/number from the display to that call appearance. If
                 * no flashing call appearance lamp is found, the name/number
                 * will be tied to the call appearance on the
                 * callAppearanceLampUpdated event.
                 *
                 */

                for (Enumeration<?> e = lmlist.enumerateLampModeItem();
                    e.hasMoreElements();
                    )
                {
                    lamp = ((LampModeItem) e.nextElement());

                    if ((lamp.getButton() != null)
                        && (lamp.getLampColor().equals(LampModeConstants.GREEN))
                        && (lamp.getLampMode().equals(LampModeConstants.FLASH)))
                    {
                        MyCallApprStatus castatus =
                            myCaHashIds.get(
                            lamp.getButton());

                        if ((castatus.getWhoCalled() == null)
                            && (!tempWhoCalled.equals(EMPTY_STRING)))
                        {
                            castatus.setWhoCalled(tempWhoCalled);
                            castatus.setIfCallMissed(true);
                        }

                        // clear out the tempWhoCalled variable and
                        // get ready for the next call
                        tempWhoCalled = EMPTY_STRING;
                        count = 0;
                    }
                }

            }
        }

        /**
         * Method is executed when a lampMode event is received for a green
         * call appearance lamp. This application only pays attention to
         * updates from call apprearance lamps. These lamps tell the status of
         * the call via the station's call appearance. A flashing lamp
         * indicates an incoming call. When that lamp becomes steady, that
         * indicates that the call has been established/answered. When that
         * lamp goes off, the far end has hung up.
         *
         * This application is interested in watching lamps go from flashing to
         * off - means the caller hung up and flashing to steady - means the
         * call was answered
         *
         * @param event - the received LampModeEvent
         */

        public void callAppearanceLampUpdated(String lampID, Short lampValue)
        {

            if (LampModeConstants.FLASH.equals(lampValue)
                && (myCaHashIds.containsKey(lampID)))
            {

                // A flashing call appearance lamp indicates that
                // there is an incoming call to the station.
                System.out.println(
                    "Call appearance Lamp " + lampID + " is now flashing.");

                /**
                 * This flashing lampMode event is tied to the last
                 * DisplayUpdate event and therefore the display information
                 * captured in the DisplayUpdate (name/number of calling party)
                 * corresponds to this call apprearance call.
                 *
                 * Need to get the CallApprStatus obj out of the hash table and
                 * update it with the local variable "tempWhoCalled" and set
                 * the ifCallMissed flag if it has not already been set. It
                 * motst likely will have been set on the display update. If
                 * for some reason the lamp is not flashing when the display
                 * update comes in, the hash table will be updated here.
                 *
                 */
                MyCallApprStatus callApprStatus =
                    myCaHashIds.get(lampID);

                callApprStatus.setCurrentLampStatus(LampModeConstants.FLASH);

                if ((callApprStatus.getWhoCalled() == null)
                    && (!tempWhoCalled.equals(EMPTY_STRING)))
                {
                    callApprStatus.setWhoCalled(tempWhoCalled);
                    callApprStatus.setIfCallMissed(true);
                }
            }
            else if (
                LampModeConstants.STEADY.equals(lampValue)
                    && (myCaHashIds.containsKey(lampID)))
            {
                MyCallApprStatus callApprStatus =
                    myCaHashIds.get(lampID);

                if (callApprStatus.getCurrentLampStatus()
                    == LampModeConstants.OFF)
                {
                    callApprStatus.setCurrentLampStatus(
                        LampModeConstants.STEADY);
                    callApprStatus.setIfDialing(true);
                }

                if (callApprStatus.getWhoCalled() != null)
                {
                    // A steady green call appearance lamp indicates that
                    // the call has been answered.
                    System.out.println(
                        "call appearance Lamp "
                            + lampID
                            + " is now on.  The call has been answered");

                    // Log the answered call.
                    logCall(callApprStatus.getWhoCalled(), "Answered");

                    callApprStatus.setIfCallMissed(false);
                    callApprStatus.setWhoCalled(null);

                    callApprStatus.setCurrentLampStatus(
                        LampModeConstants.STEADY);
                    callApprStatus.setIfDialing(false);
                }
            }
            else if (
                LampModeConstants.OFF.equals(lampValue)
                    && (myCaHashIds.containsKey(lampID)))
            {
                // Only look for the indication that the lamp is turned
                // off if the call appearance button number is contained
                // in the hash table. If so the far end went on hook.
                MyCallApprStatus callApprStatus =
                    myCaHashIds.get(lampID);

                // Check here if the call was dialed or received and log the
                // dialed call here.
                logDialedCall(lampID);

                callApprStatus.setCurrentLampStatus(LampModeConstants.OFF);
                callApprStatus.setIfDialing(false);

                if ((callApprStatus.getIfCallMissed() == true)
                    && !callApprStatus.getWhoCalled().equals(null))
                {
                    System.out.println(
                        "Lamp "
                            + lampID
                            + " turned off,"
                            + " the other person hung up");

                    logCall(callApprStatus.getWhoCalled(), "Missed");

                    // clear out the ifCallMissed flag and the whoCalled string
                    callApprStatus.setIfCallMissed(false);
                }
                callApprStatus.setWhoCalled(null);
                callApprStatus.setCurrentDialDisplayUpdate(null);
                // clear out the tempWhoCalled variable and get ready for
                // the next call
                tempWhoCalled = EMPTY_STRING;
                count = 0;
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
                System.out.println("Scheduling reconnect attempt in 10 seconds");
                reconnectTimer.schedule(new ReconnectTimerTask(), 10000);
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
                System.out.println("Attempt to reconnect immediately");
                reconnectTimer.schedule(new ReconnectTimerTask(), 0);
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
                recoverFromTerminatedSession();
            }
        }

    }

    private class ReconnectTimerTask extends TimerTask {

        @Override
        public void run() {
            try
            {
                System.out.println("Attempting to reconnect.");
                provider.reconnect();               
            }
            catch (SessionCleanedUpException ex)
            {
                try
                {
                    System.out.println("Session was terminated, starting a new one");
                    recoverFromTerminatedSession();
                }
                catch (Exception e)
                {
                    System.out.println(
                            "Java Exception - Could not reconnect to the server "
                                + e);
                    e.printStackTrace(System.out);
	                System.out.println("Will try again in 10 seconds");
    	            reconnectTimer.schedule(new ReconnectTimerTask(), 10000);
                }
            }
            catch (Exception e)
            {
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
     * This is the Asynchronous Services call back for RegistrationServices
     * 
     */
    private class MyAsyncRegistrationCallback implements AsynchronousCallback
    {
        /** 
         * Handle the asynchronous response from a registration request
         * @param response
         *          the response to a registration request
         */
        public void handleResponse(Object response){            
            
            if(response.getClass().getName().equals(RegisterTerminalResponse.class.getName())){
                RegisterTerminalResponse regResp = (RegisterTerminalResponse) response; 
                if (regResp.getCode().equals(RegistrationConstants.NORMAL_REGISTER)){
                    registered(regResp);                
                } else{
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
         *              An Asynchronous Exception thrown in response to a registration request
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
         *            about the registration.
         * 
         * Note: All the requests/events are delivered on one thread. This guarantees
         * sequential processing of requests/events. However if the processing of an
         * event involves an I/O as this function does, you may
         * want to process this in a different thread.
         * One approach would be to have a QueuedExecutor for the application
         * that would process requests/events. All the Listeners would queue the 
         * requests/events to a queue to be executed by QueuedExecutor if, either they 
         * are blocking or they require sequential processing.
	     */

        private void registered(RegisterTerminalResponse resp)
        {
            System.out.println(
                    "The phone " + extension + " is now registered.");

            try
            {
                GetButtonInformation request = new GetButtonInformation();
                request.setDevice(id);

                GetButtonInformationResponse buttonResponse =
                    phySvcs.getButtonInformation(request);
                ButtonList list = buttonResponse.getButtonList();

                for (int i = 0; i < list.getButtonItemCount(); i++)
                {
                    ButtonItem[] buttons = list.getButtonItem();

                    if (ButtonFunctionConstants
                            .CALL_APPR
                            .equals(buttons[i].getButtonFunction()))
                    {
                        MyCallApprStatus callapprstatus =
                            new MyCallApprStatus();
                        callapprstatus.setIfCallMissed(false);
                        callapprstatus.setWhoCalled(null);

                        myCaHashIds.put(buttons[i].getButton(), callapprstatus);
                    }
                }

                if (actionListener == null || callLogGui == null || 
                        queryExecutor == null || dirLookupGui == null )
                {
                    initGuiInstances();
                }
            }
            catch (CstaException e)
            {
                System.out.println(
                        "CSTA Exception - Could not get button"
                        + " information "
                        + e);
                e.printStackTrace(System.out);
            }
            catch (Exception e)
            {
                System.out.println(
                        "Java Exception - Could not get button"
                        + " information "
                        + e);
                e.printStackTrace(System.out);
            }   

            // Let the station also get the Button Info for this device
            station.getButtonInfo();
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
            catch (Exception e)
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
            catch (Exception e)
			{
                System.out.println(
                        "Exception when removing listeners or "
                            + "releasing device ID "
                            + e);            	
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

}
