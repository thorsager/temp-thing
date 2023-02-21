/*
 * EmailApp.java
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

package sampleapps.email;

//*****************************************************************************
//* Imports
//*****************************************************************************

// Java utilities
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.MessagingException;

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
import ch.ecma.csta.binding.RingerStatusEvent;
import ch.ecma.csta.errors.CstaException;
import ch.ecma.csta.monitor.MonitoringServices;
import ch.ecma.csta.physical.PhysicalDeviceServices;

import com.avaya.cmapi.APIProtocolVersion;
import com.avaya.cmapi.ServiceProvider;
import com.avaya.csta.async.AsynchronousCallback;
import com.avaya.csta.binding.RegisterTerminalResponse;
import com.avaya.csta.binding.TerminalUnregisteredEvent;
import com.avaya.csta.binding.types.DependencyMode;
import com.avaya.csta.binding.types.MediaMode;
import com.avaya.csta.physical.ButtonFunctionConstants;
import com.avaya.csta.physical.LampModeConstants;
import com.avaya.csta.registration.RegistrationAdapter;
import com.avaya.csta.registration.RegistrationConstants;
import com.avaya.mvcs.framework.CmapiKeys;


//*****************************************************************************
//* EmailApp
//*****************************************************************************

/**
 * Purpose: This application sends email each time a call to a specified
 * extension is not answered.
 *
 * High Level Steps: This application registers an extension as a CMAPI
 * phone (that is softphone enabled) with the Communication Manager in shared
 * control mode.  The application uses the call appearance lamps to watch the
 * station's call activity.  The application logs unanswered calls via email.
 *
 * Registration is done asynchronously using Asynchronous Services.
 * 
 * This application can easily be configured to send the message to
 * multiple email accounts as well as customizing the email message.
 *
 * Prerequisites:
 * - Edit the email_app.properties file found in the cmapisdk/example/lib
 * directory:
 *      Set "callserver" to the IP address of a CLAN on Communication Manager.
 *      Set "extension" to an extension/phone that is softphone enabled on
 *          Communication Manager and will be registered by this application
 *          in shared control mode.
 *      Set "password" to the password administered on Communication
 *          Manager for the extension/phone that is softphone enabled.
 *      Set "smtpserver" to the email server you wish to send email to.
 *      Set "emailfrom" to the email address that is the sender of the email.
 *      Set "emailnotifylist" to the email addresses to receive the emails.
 * - Make sure you have at least one IP_API_A license on Communication Manager.
 * - Make sure you have the cmapi-client.properties file set to the IP address
 * of the connector server.
 *
 *
 * Possible variations:   You can modify this application very easily to
 * filter on particular types of calls such as: all outside calls, only outside
 * calls from area code 402, only calls from a particular person or number, all
 * calls including answered calls.
 *
 * build and run:  You can build the email application and run it by going to
 * cmapisdk/examples/bin and running the script runEmailapp.sh.
 *
 *
 * @author AVAYA, Inc.
 * @version $Revision: 1.18 $
 **/

@SuppressWarnings("unused")
public class EmailApp {

    // This field is used to track the name/number of the calling party until a
    // lamp update comes in and it can be stored in the hash table.
    // Initialize to null string to help keep track of null string
    // display updates coming from Communication Manager.
    private static final String EMPTY_STRING = "";
    private String tempWhoCalled = EMPTY_STRING;

    // This field is used to hold the list of email recipients
    private String[] recipients;
    private String[] name_number;

    // This hash table is used to track the button IDs of all call
    // apprearances, the name/number of who called and the status of whether or
    // not email needs to be sent.
    private Hashtable<String, myCallApprStatus> myCaHashIds = new Hashtable<String, myCallApprStatus>();

    // These fields are read in from the application.properties file.  The
    // aplication properities file must be populated with a valid IP address for
    // Avaya Communication Manager, a valid extension number and password, a
    // valid SMTP email server, a list of email addresses to notify, a "from"
    // email address and a valid email subject & body.
    private String callServer;
    private String smtpserver;
    private String extension;
    private String password;
    private String emailnotifylist;
    private String emailfrom;
    private String emailsubject;
    private String emailbody;
    private String emailbody1;
    private String propertyfile = "email_app.properties";

    private static final String PROP_FILE_CALL_SERVER = "callserver";
    private static final String PROP_FILE_EXTENSION = "extension";
    private static final String PROP_FILE_EXT_PASSWORD = "password";
    private static final String PROP_FILE_CMAPI_SERVER = "cmapi1.server_ip";
    private static final String PROP_FILE_USERNAME = "cmapi1.username";
    private static final String PROP_FILE_PASSWORD = "cmapi1.password";
    private static final String PROP_FILE_SERVER_PORT ="cmapi1.server_port";
    private static final String PROP_FILE_SECURE ="cmapi1.secure";
    private static final String PROP_FILE_SMTPSERVER = "smtpserver";
    private static final String PROP_FILE_EMAILLIST = "emailnotifylist";
    private static final String PROP_FILE_EMAILFROM = "emailfrom";
    private static final String PROP_FILE_EMAILSUBJ = "emailsubject";
    private static final String PROP_FILE_EMAILBODY = "emailbody";
    private static final String PROP_FILE_EMAILBODY1 = "emailbody1";

    // This thread is used to receive a shutdown notification when the
    // application is terminated.
    private MyShutdownThread shutdownThread;

    private int DISPLAY_LEN = 35;
    private boolean rc = false;
    private int count = 0;
    private LampModeItem lamp = null;

    // The Device ID for our extension number will be supplied by the
    // Communication Manager API.  The Connection ID is formulated from the
    // Device ID.
    private DeviceID id = null;

    // The Service Provider used to get the handles of various services.
    private ServiceProvider provider;

    // These fields will be populated with the handles of the various services,
    // which we will get through the ServiceProvider class.
    private PhysicalDeviceServices physSvcs;
    private MonitoringServices montSvcs;
    
    private AvayaStation station;

    private MyAvayaStationListener avayaStationListener;
    
    private MyRegistrationListener registrationListener;
 
    private AsynchronousCallback asyncCB = new MyAsyncRegistrationCallback();
    
    // Creates an instance of the EmailApp class, bootstraps it, then starts
    // the application.
    public static void main(String[] args) {
        EmailApp app = new EmailApp();
        try {
            app.bootstrap();
            app.start();
        } catch(CstaException e) {
            System.out.println("CSTA Exception - Could not start" +
                " the Email app " + e);
            e.printStackTrace(System.out);
            System.exit(0);    // exits to the shutdownThread to perform cleanup
        } catch (Exception e) {
            System.out.println("Java Exception - Could not start" +
                " the Email app "+ e);
            e.printStackTrace(System.out);
            System.exit(0);    // exits to the shutdownThread to perform cleanup
        }
    }

    /**
     * This is the bootstrap to read in the needed properties, and get handles
     * to all services that are used by the application.
     *
     * @throws CstaException if a CstaException is generated by one of the
     *   calls to service provider, it is thrown to the caller.
     * @throws Exception if any runtime exception is generated, it is thrown
     *   to the caller.
     */
    public void bootstrap() throws CstaException, Exception 
    {
        //add following properties for validation client and service side certifications 
        String cmapiTrustStorePassword;
        String cmapiKeyStoreLocation;
        String cmapiKeyStorePassword;
        String isValidPeer;
        String hostnameValidation;
        
        // Get all of the arguments from the properties file
        ClassLoader cl = EmailApp.class.getClassLoader();
        URL appURL  = cl.getResource(propertyfile);
        Properties appProp = new Properties();
        appProp.load(appURL.openStream());
        callServer = appProp.getProperty(PROP_FILE_CALL_SERVER).trim();
        extension = appProp.getProperty(PROP_FILE_EXTENSION).trim();
        password  = appProp.getProperty(PROP_FILE_EXT_PASSWORD).trim();
        smtpserver = appProp.getProperty(PROP_FILE_SMTPSERVER).trim();
        emailnotifylist = appProp.getProperty(PROP_FILE_EMAILLIST).trim();
        emailfrom = appProp.getProperty(PROP_FILE_EMAILFROM).trim();
        emailsubject = appProp.getProperty(PROP_FILE_EMAILSUBJ).trim();
        emailbody = appProp.getProperty(PROP_FILE_EMAILBODY).trim();
        emailbody1 = appProp.getProperty(PROP_FILE_EMAILBODY1).trim();
        
        String cmapiServerIp = appProp.getProperty(PROP_FILE_CMAPI_SERVER).trim();
        String cmapiUsername = appProp.getProperty(PROP_FILE_USERNAME).trim();
        String cmapiPassword = appProp.getProperty(PROP_FILE_PASSWORD).trim();
        // If there is no entry then we will assume the connection is secure.
        String cmapiServerPort= appProp.getProperty(PROP_FILE_SERVER_PORT, "4722").trim();
        String cmapiSecure= appProp.getProperty(PROP_FILE_SECURE, "true").trim();
        String cmapiTrustStoreLocation = appProp.getProperty(CmapiKeys.TRUST_STORE_LOCATION);
        
        // add following properties for validation client and service side certifications 
        cmapiTrustStorePassword = appProp.getProperty(CmapiKeys.TRUST_STORE_PASSWORD);
        cmapiKeyStoreLocation = appProp.getProperty(CmapiKeys.KEY_STORE_LOCATION);
        String cmapiKeyStoreType = appProp.getProperty(CmapiKeys.KEY_STORE_TYPE);
        cmapiKeyStorePassword = appProp.getProperty(CmapiKeys.KEY_STORE_PASSWORD);
        isValidPeer = appProp.getProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE);
        hostnameValidation = appProp.getProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE_HOSTNAME);

	 /**
         *  The first thing we need to do is verify all the fields in the
         *  application.properties file is set up correctly.  If they are not
         *  set up we can't send the email and no need to continue with the
         *  application
         */

        if (emailnotifylist == null)
        {
            System.out.println("Cannot send email - emailnotifylist field is" +
                " not set in properties file");
            System.exit(0);     // this is a real exit, no need to cleanup here
        }
        if (smtpserver == null)
        {
            System.out.println("cannot send mail - smtpserver field is not" +
                " set in properties file");
            System.exit(0);     // this is a real exit, no need to cleanup here
        }
        if (emailfrom == null)
        {
            System.out.println("cannot send mail - emailfrom field is not set" +
                " in properties file");
            System.exit(0);     // this is a real exit, no need to cleanup here
        }
        if (emailsubject == null)
        {
            System.out.println("cannot send mail - emailsubject field is not" +
                " set in properties file");
            System.exit(0);     // this is a real exit, no need to cleanup here
        }

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
        // set up the recipients list needed for sending email - list is
        // seperated by commas
        StringTokenizer st = new StringTokenizer(emailnotifylist, ",");

        int totalToList = st.countTokens();
        recipients = new String[totalToList];
        for (int i=0; i < totalToList; i++)
        {
                recipients[i] = st.nextToken();
        }

        // Get a handle to the ServiceProvider class.
        provider = ServiceProvider.getServiceProvider(spProp);

        // Get a handle to physical device services so I can get all buttons
        // administered on the device as well as lamp mode information.

        physSvcs = (PhysicalDeviceServices) provider.getService(
            ch.ecma.csta.physical.PhysicalDeviceServices.class.getName());
        
        // Get a handle to Monitoring services so that Listeners
        // could be added and removed.
        montSvcs = (MonitoringServices) provider.getService(
                ch.ecma.csta.monitor.MonitoringServices.class.getName());

        // Create new AvayaStation object.
        station = new AvayaStation();
        station.init(callServer, "", extension, provider);

    }

    /**
     * This method actually starts the app.  It must be called after calling
     * bootstrap or it will fail.
     *
     * @throws CstaException if a CstaException is generated by one of the
     *   calls to the services, it is thrown to the caller.
     */
    public void start() throws CstaException {
        System.out.println("Startup using Communication Manager= "
        +callServer +" ext= " +extension+" pw= " +password
        +" smtpserver= " +smtpserver);

        // Create a thread in whose context our cleanup will occur if the app
        // is terminated. The Communication Manager API connector server code
        // will clean up if an app goes away unexpectedly, but it's still
        // good to clean up.
        shutdownThread = new MyShutdownThread();
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        try {
            // The first thing that we do is to get a Device ID.  The Device ID
            // is used in the requests to the services. Calling this method
            // populates the id field of the EmailApp class.

            id = station.getDeviceID();

            addListeners();

            // Registers an extension for exclusive use as a CMAPI softphone station
            // (IP_API_A) with Communication Mananger and the connector server.
            // Setting the second parameter to true allows for shared control.
            // Register asynchronously since there's no reason to hold up this
            // thread waiting for the response.
            
            station.register(password, DependencyMode.DEPENDENT, MediaMode.NONE, null, null, true,
            		null, null, new MyAsyncRegistrationCallback());

            // That's all we do for now!  The main thread is now going to go
            // away, and the rest of the action occurs as a result of events
            // coming into our listeners.  Note: the Communication Manager API
            // client code has some persistent threads that keep the application
            // alive.  The rest of the code executes in the context of these
            // threads.

        } catch(CstaException e) {
            System.out.println("Could not start up properly");
            e.printStackTrace(System.out);
        } catch (Exception e) {
            System.out.println("station.getDeviceId failed");
            e.printStackTrace(System.out);
        }
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

        // Add a listener so we can receive events indicating when the phone is
        // registered or unregistered.
        registrationListener = new MyRegistrationListener();
        station.addListener(registrationListener);
    }
  
    /**
     * Removes all of the listeners that were added for the various services.
     */
    private void removeListeners() throws CstaException
    {
        station.removeListener(avayaStationListener);
        station.removeListener(registrationListener);
    }
    
    /**
     * This cleanup method assures us that we unregister our extension, remove
     * all the listeners and release the device ID.  The connector server will
     * cleanup if a client goes away without doing this cleanup, but it's best
     * to be safe.
     */
    private void cleanup() {
        System.out.println("The email application is terminating: clean up.");

        // There is a chance that AvayaStation has already cleaned up as a
        // result of getting the unregistered event. In this case, the station
        // is already unregistered, the device ID has been released, and the
        // server should have cleaned up. Don't bother doing anything else.
        if (station.getDeviceID() == null)
        {
            return;
        }

        try {
            // Make sure no listeners were left around.
            station.removeListener(avayaStationListener);
            station.removeListener(registrationListener);

            System.out.println("calling station.unregister");
            // Call the synchronous version of unregister() because we want the
            // unregister to succeed before cleaning up and disconnecting.
            station.unregister();
            station.cleanup();

            System.out.println("calling provider.disconnect");
            // provider.disconnect will disconnect from the server and
            // terminate the active session.
            provider.disconnect(true);

        } catch(CstaException e) {
            System.out.println("Email app could not clean up properly");
            e.printStackTrace(System.out);
        } catch (Exception e) {
            System.out.println("station.unregister could not clean up properly");
            e.printStackTrace(System.out);
        }
    }

    /**
     * This class provides the call appearance id and the status for each
     * call appearance within a station object.  The purpose of this object
     * is to send email when the call has not been answered and to clear the
     * status when the call has been answered.  It also stores the display
     * information of who called on each call appreance line.
     *
     **/
    private class myCallApprStatus {
        boolean needToSendEmail = false;
        String whoCalled = null;

        /**
         * Method: setWhoCalled
         *
         * Sets the value of the field 'whoCalled' to the parameter
         * 'callingparty' passed in.  The field 'whoCalled' has the
         * following description:  Specifies a string of the name or
         * number of the calling party.
         *
         * @param callingparty
         */
        public void setWhoCalled(String callingparty)
        {
            this.whoCalled = callingparty;
        }

        /**
         * Method: getWhoCalled
         *
         * Returns the value of field 'whoCalled'. The field
         * 'whoCalled' has the following description: Specifies a string
         * of the name or number of the calling party.
         *
         * @return the value of field 'whoCalled'.
         */
        public String getWhoCalled()
        {
            return this.whoCalled;
        }

        /**
         * Method: setEmailFlag
         *
         * Sets the value of the field 'needToSendEmail' to the parameter
         * 'value' passed in.  The field 'needToSendEmail' has the
         * following description:  Specifies if email needs to be sent
         * for this call.
         *
         * @param callingparty
         */
        public void setEmailFlag(boolean value)
        {
            this.needToSendEmail = value;
        }

        /**
         * Method: getEmailFlag
         *
         * Returns the value of field 'needToSendEmail'. The field
         * 'needToSendEmail' has the following description: Specifies
         * if email needs to be sent for this call.
         *
         * @return the value of field 'needToSendEmail'.
         */
        public boolean getEmailFlag()
        {
            return this.needToSendEmail;
        }
    }

    /**
     * This is the physical device listener class.  It extends
     * PhysicalDeviceAdapter rather than implementing the PhysicalDeviceListener
     * interface.  This allows us to NOT implement all of the methods in
     * PhysicalDeviceListener.
     */
    private class MyAvayaStationListener extends AvayaStationAdapter {

        /**
         * Method is executed when the a Display update is received
         * @param event the DisplayUpdatedEvent
         */
        public void displayUpdated(DisplayUpdatedEvent event) {

            DISPLAY_LEN = event.getContentsOfDisplay().length();

            if (event.getContentsOfDisplay().substring(0, DISPLAY_LEN).trim().
                    equals(EMPTY_STRING))
            {
                // Communication Manager sends many display updates some of
                // which are null - just ignore the null ones
                return;
            }

            // Since there are many display updates coming in from CM, we need
            // to capture the display and store it.  The first non-null display
            // is the one with the name of who is calling.  The second non-null
            // display is the one with the name/number of who is calling.
            // Therefore, the second non-null display is the one to capture.
            // The whoCalled variable will store the data until the lampMode
            // event at which time it will be stored in the hash table and the
            // local variable will be cleared out.

            // first non-null display - need to wait for the second non-null
            // display as it has both the name and number.
            if (tempWhoCalled.equals(EMPTY_STRING) && (count == 0))
            {
                count = 1;
            }
            else if (tempWhoCalled.equals(EMPTY_STRING) && (count == 1))
            {
                // get name and number from display - they are seperated by
                // white space.
                StringTokenizer stringtoke = new StringTokenizer(event.
                    getContentsOfDisplay().substring(2,DISPLAY_LEN).trim());

                int totalList = stringtoke.countTokens();
                for (int i=0; i < totalList; i++)
                {
                    String s = stringtoke.nextToken();
                    tempWhoCalled = tempWhoCalled.concat(s);
                    // put a space between every string except the last one
                    if ( i < totalList - 1)
                    {
                        tempWhoCalled = tempWhoCalled.concat(" ");
                    }
                }

                // Set count to two.  The second display update with
                // information (non null display update) has the called number
                // with it.  That is the one we want to send with the email.
                count = 2;

                try {
                    /**
                     * Check for a flashing call appearance lamp.  When found
                     * tie the name/number from the display to that call
                     * appearance.  If no flashing call appearance lamp is
                     * found, the name/number will be tied to the call
                     * appearance on the callAppearanceLampUpdated event.
                     *
                     */
                    // Get the information about all buttons on this softphone.
                    // Create a new request and populate the device ID.
                    GetLampMode lampmodeinfo = new GetLampMode();
                    lampmodeinfo.setDevice(id);
                    // Send the request and get the response, catching any
                    // resulting CstaExceptions.
                    GetLampModeResponse lampmodeResponse =
                    physSvcs.getLampMode(lampmodeinfo);

                    // physSvcs.getButtonInformation(buttonRequest);
                    LampModeList lmlist = lampmodeResponse.getLampModeList();
                    for (Enumeration<?> e = lmlist.enumerateLampModeItem();
                             e.hasMoreElements();)
                    {
                        lamp = ((LampModeItem)e.nextElement());

                        if ((lamp.getButton() != null) &&
                            (lamp.getLampColor().equals(LampModeConstants.GREEN)) &&
                            (lamp.getLampMode().equals(LampModeConstants.FLASH)))
                        {
                            myCallApprStatus castatus = myCaHashIds.get(lamp.getButton());

                            if ((castatus.getWhoCalled() == null) &&
                                (!tempWhoCalled.equals(EMPTY_STRING)))
                            {
                                castatus.setWhoCalled(tempWhoCalled);
                                castatus.setEmailFlag(true);
                            }
                                  // clear out the tempWhoCalled variable and
                                  // get ready for the next call
                                  tempWhoCalled = EMPTY_STRING;
                                  count = 0;
                        }
                    }
                } catch (CstaException e) {
                    System.out.println("CSTA Exception - Could not get" +
                        " button information "+ e);
                    e.printStackTrace(System.out);

                } catch (Exception e) {
                    System.out.println("Java Exception - Could not get" +
                        " button information "+ e);
                    e.printStackTrace(System.out);
                }
            }
        }

        /**
         * Method is executed when a lampMode event is received for a green
         * call appearance lamp.  This application only pays attention
         * to updates from call apprearance lamps.  These lamps tell the
         * status of the call via the station's call appearance.  A flashing
         * lamp indicates an incoming call.  When that lamp becomes
         * steady, that indicates that the call has been established/answered.
         * When that lamp goes off, the far end has hung up.
         *
         * This application is interested in watching lamps go from flashing
         * to off - means the caller hung up (need to send email) and
         * flashing to steady - means the call was answered
         * (do not send email).
         *
         * @param event the received LampModeEvent
         */
        public void callAppearanceLampUpdated(String lampID, Short lampValue) {

            if(LampModeConstants.FLASH.equals(lampValue)
               && (myCaHashIds.containsKey(lampID)))
            {

                // A flashing call appearance lamp indicates that
                // there is an incoming call to the station.
                System.out.println("Call appearance Lamp " + lampID
                     + " is now flashing.");

                /**
                 * This flashing lampMode event is tied to the last
                 * DisplayUpdate event and therefore the display information
                 * captured in the DisplayUpdate (name/number of calling party)
                 * corresponds to this call apprearance call.
                 *
                 * Need to get the CallApprStatus obj out of the hash table
                 * and update it with the local variable "tempWhoCalled"
                 * and set the needToSendEmail flag if it has not already been
                 * set.  It motst likely will have been set on the display
                 * update.  If for some reason the lamp is not flashing when
                 * the display update comes in, the hash table will be updated
                 * here.
                 *
                 */
                myCallApprStatus callapprstatus = myCaHashIds.get(lampID);

                if ((callapprstatus.getWhoCalled() == null) &&
                    (!tempWhoCalled.equals(EMPTY_STRING)))
                {
                       callapprstatus.setWhoCalled(tempWhoCalled);
                       callapprstatus.setEmailFlag(true);
                }
                // clear out the tempWhoCalled variable and get ready for
                // the next call
                tempWhoCalled = EMPTY_STRING;
                count = 0;
            } else if (LampModeConstants.STEADY.equals(lampValue)
                && ( myCaHashIds.containsKey(lampID)))
            {
                myCallApprStatus callapprstatus = myCaHashIds.get(lampID);

                if (callapprstatus.getWhoCalled() != null)
                {

                    // A steady green call appearance lamp indicates that the
                    // call has been answered.
                    System.out.println("call appearance Lamp " + lampID
                        + " is now on.  The call has been answered");

                    // Since the call has been answered there is no need to send
                    // email.  Get the CallApprStatus obj out of the hash table
                    // and update the needToSendEmail flag as well as clear out
                    // the whoCalled flag.

                    callapprstatus.setEmailFlag(false);
                    callapprstatus.setWhoCalled(null);
                }
            } else if (LampModeConstants.OFF.equals(lampValue)
                && ( myCaHashIds.containsKey(lampID)))
            {
                // Only look for the indication that the lamp is turned off if
                // the call appearance button number is contained in the hash
                // table.  If so the far end went on hook so send email.

                myCallApprStatus callapprstatus = myCaHashIds.get(lampID);

                // Only send the email if the needToSendEmail flag is set and
                // the whoCalled string is not empty
                if ((callapprstatus.getEmailFlag() == true)
                    && (!callapprstatus.getWhoCalled().equals(null)))
                {
                    System.out.println("Lamp " + lampID + " turned off," +
                        " the other person hung up");

                    // send the email
                    PostMail aPostMail = new PostMail();

                    try {
                        rc = aPostMail.sendTheMail(recipients, emailsubject,
                            emailbody + callapprstatus.getWhoCalled() +
                            emailbody1, emailfrom, smtpserver);
                    } catch (MessagingException e) {
                       System.out.println("MessagingException on postMail");
                       e.printStackTrace();
                    }
                    if (rc == false)
                    {
                        System.out.println("problem sending email to ("  +
                            smtpserver + ") - no email sent");
                    } else {
                        System.out.println("email sent to server " + smtpserver);
                    }
                    // clear out the needToSendEmail flag and the whoCalled
                    // string
                    callapprstatus.setEmailFlag(false);
                    callapprstatus.setWhoCalled(null);
                }
            }
        }

        /**
         * Method is executed when the RingerStatusEvent is received.
         * This is the event that you would key off if you wanted to answer
         * the call.  This application only looks at the DisplayUpdates events
         * and lampMode events and does not want to answer the call.
         *
         * @param event the received RingerStatusEvent
         */
        public void ringerStatusUpdated( RingerStatusEvent event) {
            //Right now we do nothing with the RingerStatusEvent
        	return;
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
		 * 				An Asynchronous Exception thrown in response to a registration request
		 */
		public void handleException(Throwable exception) {			
	        try
	        {    
	        	System.out.println("Received Asynchronous Exception from Server. The stack trace from the Server is as follows");
	        	exception.printStackTrace();
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
	           try {
                // Get the information about all buttons on this phone.
                // Create a new request and populate the device ID.
                GetButtonInformation buttonRequest = new GetButtonInformation();
                buttonRequest.setDevice(id);

                // Send the request and get the response, catching any
                // resulting CstaExceptions.
                GetButtonInformationResponse buttonResponse =
                    physSvcs.getButtonInformation(buttonRequest);
                ButtonList list = buttonResponse.getButtonList();

                // Need to loop through all buttons on the station and pull out
                // the Call Appearance buttons.
                for (int i=0; i < list.getButtonItemCount(); i++)
                {
                    ButtonItem[] buttons = list.getButtonItem();

                    // Is this a call appearance button?
                    if (ButtonFunctionConstants.CALL_APPR.equals(
                            buttons[i].getButtonFunction()))
                    {
                        // Keep track of the call appearance button ID so we
                        // know when a call comes in.
                        myCallApprStatus callapprstatus =
                            new myCallApprStatus();
                        callapprstatus.setEmailFlag(false);
                        callapprstatus.setWhoCalled(null);
                        // add the object to the hash table
                        myCaHashIds.put(buttons[i].getButton(),callapprstatus);
                                                                    
                    }
                }
            } catch (CstaException e) {
                    System.out.println("CSTA Exception - Could not get button" +
                        " information "+ e);
                    e.printStackTrace(System.out);

            } catch (Exception e) {
                    System.out.println("Java Exception - Could not get button" +
                        " information "+ e);
                    e.printStackTrace(System.out);
            }
	    	
            // Let the station also get the Button Info for this device
            station.getButtonInfo();
            
	    	System.out.println("The phone is now registered and ready for use");
            
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
     * This is a very simple class that is used to do cleanup when the app is
     * terminated.  The Communication Manager API server code will clean up if
     * an app goes away unexpectedly, but it's still good to clean up.
     */
    private class MyShutdownThread extends Thread {
        public void run() {
            cleanup();
        }
    }
}
