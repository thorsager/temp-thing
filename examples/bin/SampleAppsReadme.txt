This README file gives instructions on how to configure, compile and
run the Avaya Communication Manager API DEMO Applications (Tutorial
App, TutorialClientMediaApp, TutorialTtyApp, EmailApp, SimpleIVRApp,
Softphone and Click2Call).

There are two versions of the TutorialApp, SimpleIVRApp and Click2Call
sample applications that demonstrate the difference between using 
Physical Device Services (PDS) and Call Control Services (CCS) to perform the
telephony operations.  A TSAPI Basic User license is required to run the Call 
Control Services versions of the sample applications.

The sample applications that use Call Control Services(CCS) are
sampleapps.ccs.tutorial, sampleapps.ccs.simpleIVR and sampleapps.ccs.click2call.
The Physical Device Services(PDS) versions of these sample applications are 
sampleapps.tutorial, sampleapps.simpleIVR and sampleapps.click2call.
The end-user functionality is the same between the CCS and PDS versions.  
The difference is in the services used for the implementation of these applications.

CONVENTIONS / INSTRUCTIONS
A) $SDK_PATH will be used in this guide to refer to the path to the
DMCC (formerly CMAPI) SDK directory. For a path such as /opt/cmapijava-sdk,
$SDK_PATH would be /opt/.

B) See the Avaya Application Enablement Services Installation guide for
details on how to configure the Communication Manager server and
connector server.

C) Make sure you have at least one VALUE_DMCC_DMC license on WebLM (or, if
you have no WebLM licenses, one IP_API_A license on Communication Manager)
for each DMCC softphone using these applications. Every instance of these
applications will require a license.

D) On your client PC, set the $JAVA_HOME variable and ensure that "javac"
and "java" are in the PATH of the user launching the application.  In order
to run the applications, you must have Java version 6.0 (1.6) or higher.

E) In case you want to use TLS 1.3, you must have Java version 8.0 (1.8) or higher.
   Variants:
   1) OracleJDK - 8u261-b12
   2) OpenJDK - 8u272-b10.
   To enable TLS 1.3 you need to set "-Djdk.tls.client.protocols=TLSv1.3" in
   Java system property.
   We have already added "-Djdk.tls.client.protocols=TLSv1.3" in examples/bin/build.xml.

F) These applications are compiled and launched using the Ant build
tool (included). Each application can be built or launched using a
different target. To launch an application, issue the following
commands:
(important)  cd $SDK_PATH/cmapijava-sdk/examples/bin
(on Linux)   ./ant.sh <target>
(on Windows) ant.bat <target>

For a guide to the available Ant targets, use the Ant switch
"-projecthelp".

Starting AE Services 7.0, new AE Services server installs will not have Avaya 
provided default certificate installed on AE Services server. Instead, AE 
Services server will have self-signed default certificate.
If you need to use TLS connection when connecting to DMCC service, you can 
export the AE Service server trust certificates installed on AE Services server.

This certificate can be obtained via AE Services server management Web console
by going to “Security -> Certificate Management -> CA Trusted Certificates” 
page and then check the certificate you want to export and then click on export
button. This opens a new page with the certificate in a window. Copy the entire
text in the window and save it to a file e.g., aesvcsCA.cer file.

On Linux/Unix based systems cd to $SDK_PATH/cmapijava-sdk/examples/resources  
and copy the aesvcsCA.cer file here. Then run the following command. It is 
assumed keystore default password is still “password”, if not please use 
the correct keystore password in the command below.  

keytool -alias aesvcs -import -file aesvcsCA.cer -keystore avaya.jks -storepass password


TUTORIAL APPLICATION - Registers an extension with Communication
Manager and waits for that extension to be dialed. When a user dials
the associated extension number, the application answers the incoming
call and plays a greeting to the user. The user can then press 1 to
record and play back a message.

    1) Open the file: $SDK_PATH/cmapijava-sdk/examples/resources/tutorial.properties
       and edit the following values:
            callserver = name of the Switch Connection (if it has been configured) or the
                         IP address of Communication Manager (C-LAN or Processor Ethernet)
            extension = extension to register and receive calls
            password = the extension's password on Communication Manager
            cmapi1.server_ip = IP address for the connector server (AE Server)
     
    2) On the AES Management Console (OAM) web-pages, navigate to the following page:
       AE Services -> DMCC -> Media Properties
            
    On this page, you will find "Player Directory" and "Recorder Directory" parameters.
    These two parameters must be set to the same directory for the Tutorial App to
    function properly. They both default to /var/media
     
    3) On the connector server (AE Server), copy all the wave files from the
           $SDK_PATH/cmapijava-sdk/examples/media
       directory to your specified recorder/player directory. These wave files are
       in G.711 format, therefore this application will only work with a G.711 codec.
            
    4) Launch the application using the Ant target: "runTutorial" (or
       "runTutorialCCS" to run the Call Control Services version of the
       application).

    5) From a station on the same Communication Manager server with which
    the tutorial application is registered, dial the extension number that
    you associated with the tutorial application and follow the directions
    you'll hear.


TUTORIAL MEDIA STACK APPLICATION - Registers an extension with
Communication Manager and waits for that extension to be dialed. It
also adds a sample Speaker and Microphone to the Audio RTP Channel.
When a user dials the associated extension number, the application
answers the incoming call. The Speaker and Microphone are enabled. The
incoming Audio RTP stream is played on the Speaker and the audio from
Microphone is played onto the outgoing Audio RTP stream. Since
different systems have different sound cards, the sample Speaker and
Microphone may need to be modified. Also, the Speaker and Microphone
do not support transcoding. 

    1) Configure the application as for the Tutorial application above,
       except edit tutorialClientMediaStack.properties.

    2) IMPORTANT: The TutorialSpeaker and TutorialMicropone both assume that the
       system microphone and speakers are PCM and are performing the conversion.
       You may need to modify the following two Java classes (found under
       $SDK_PATH/cmapijava-sdk/examples/src) and adjust them to run on your system:
            sampleapps.clientmediastack.TutorialMicrophone
            sampleapps.clientmediastack.TutorialSpeaker

    3) Launch the application via the Ant target: "runTutorialMediaStack".


TUTORIAL TTY APPLICATION - Launches a Java Swing GUI that registers an extension
and allows the user to communicate with other Tutorial TTY applications or with
analog TTY/TDD devices.  This application is an extension of the Tutorial Media Stack
Application, so it uses the same TutorialSpeaker and TutorialMicrophone classes.
In addition, it attaches a TtySource (used to send TTY) and a TtySink (used
to receive TTY).  The TtySink and TtySource implementations are both objects
extended from the Swing JPanel component.  Keyboard events add the typed TTY char to a
buffer that is read by the TtySource and transmitted on the RTP stream.  TTY characters
received on the RTP stream are written to the TtySink and simply displayed by the GUI.

    1) This sample application shares the same configuration as the Tutorial Media
       Stack application above. However, the GUI supplies the ability to configure
       the properties.  After starting the application, click File->Options to
       modify the configuration.

    2) IMPORTANT: The TutorialSpeaker and TutorialMicropone both assume that the
       system microphone and speakers are PCM and are performing the conversion.
       You may need to modify the following two Java classes (found under
       $SDK_PATH/cmapijava-sdk/examples/src) and adjust them to run on your system:
            sampleapps.clientmediastack.TutorialMicrophone
            sampleapps.clientmediastack.TutorialSpeaker

    3) Launch the application via the Ant target: "runTutorialTtyApp"


EMAIL APPLICATION - Registers an extension in Shared Phone
Control mode with Communication Manager. This mode is a feature that
allows two stations to register with the same extension. The DMCC
phone watches the physical station's call activity by monitoring each
call appearance. When a call is placed to the physical station, the
calling name and number are taken from the display. If the call is
not answered, the application logs the call by sending an email.
     
    1) Edit $SDK_PATH/cmapijava-sdk/examples/resources/email_app.properties:
            callserver = IP address of Communication Manager (C-LAN or S8300)
            extension = the extension to register and receive calls
            password = the extension's password on Communication Manager
            smtpserver = email server used to send mail
            emailfrom = email address to report as the sender
            emailsubject = subject header to send in emails
            emailbody = email body preceding the caller's information
            emailbody1= email body following the caller's information
            emailnotifylist = email addresses to receive emails (comma-separated)
            cmapi1.server_ip = IP address for the connector server (AE Server)

    2) Launch the email application via the Ant target "runEmail".

    3) From any station, place a call to the station that was registered via
       the AE Server. Hang up the phone before the call is answered.
       Email will be sent to everyone on the "emailnotifylist" with the
       extension and name of the calling party. If it is an outside call with
       ANI, that number will be sent in the email as well. If the call is
       answered, no email will be sent.


SIMPLEIVR APPLICATION - Registers an extension with Communication
Manager and waits for incoming calls. When a call is received, the
application answers, plays a greeting, plays a recorded menu, and
prompts the user to enter a digit of her/his choice. The user can
press 1 to hear a fact, 2 to record a message, 3 to hear the last
recorded message, 4 to transfer the call, or 5 to conference in
another party.

    1) Edit $SDK_PATH/cmapijava-sdk/examples/resources/simpleIVR.properties:
            callserver = IP address of Communication Manager (C-LAN or S8300)
            extension = extension to register and receive calls
            password = the extension's password on Communication Manager
            cmapi1.server_ip = IP address for the connector server (AE Server)
            switchname = name of the Switch Connection to Communication Manager
     
    2) On AES Management Console (OAM) web-pages, navigate to the following page:
           AE Services -> DMCC -> Media Properties
            
    On this page, you will find "Player Directory" and "Recorder Directory" parameters.
    These two parameters must be set to the same directory for the SimpleIVR App to
    function properly. They both default to /var/media.
     
    3) On the connector server (AE Server), copy all the wave files from the
           $SDK_PATH/cmapijava-sdk/examples/media
       directory to your specified recorder/player directory. These wave files are
       in G.711 format, therefore this application will only work with a G.711 codec.

    4) Launch the application via the Ant target: "runSimpleIVR" (or "runSimpleIVRCCS"
    to run the Call Control Services version of the application).
     
    5) From a station on the same Communication Manager server with which
    the SimpleIVR application is registered, dial the extension number that
    you associated with the SimpleIVR application and follow the directions
    you'll hear.


SOFTPHONE APPLICATION - Registers an extension as a DMCC
softphone with the Communication Manager, and displays a GUI of a
telephone. The user can make and receive calls using the GUI.

    Based on the launch configuration in softphone.properties file, this
    application can be used in any of the following modes:

    a) Regular phone, with complete control of the extension i.e media and
        all the station features offered by the Communication Manager. Note: 
        this application can receive media (audio) RTP stream associated with
        the call. However this application relies on device drivers provided
        by the operating system for speaker/microphone hardware, to
        deliver/receive the media to/from the speaker/microphone.

    b) Shared mode (i.e, dependent/independent), with/without
        media(CLIENT_MEDIA/NO_MEDIA). If used in "dependent" mode another
        endpoint (physical or a softphone or a DMCC client) must be registered
        to the Communication Manager with the same extension.

    c) Telecommuter mode: In this mode media (audio) associated with the call
        is sent to antother extension e.g.(home/hotel number aka Telecommuter
        extension). This mode can be used by a person working from home/hotel 
        and controlling their work phone via this application over a VPN link.
        The user of this app can place and receive call via the company's 
        Communication Manager (PBX switch).

    *** NOTE *** Unlike the other example applications, this is a GUI
    application. It will not run on a LINUX system in run level 3, or from
    a telnet window, etc.

    1) Open the file $SDK_PATH/cmapijava-sdk/examples/resources/softphone.properties
       and edit the following values:

           callserver          = name of the Switch Connection (if it has been configured) or
                                 the IP address of Communication Manager (C-LAN or S8300)
           extension           = extension to be registered and receive calls (must be
                                 administered as softphone enabled)
           password            = the extension's password on Communication Manager
           cmapi1.server_ip    = IP address for the connector server (AE Server)
           media               = either "none" or "client" or "server" or "telecommuter"
           dependency          = either "main" or "dependent" or "independent" 
           forceLogin          = "true" if you want to take over the registration of an
                                 already registered ip-endpoint, else "false"
	       unicodeScripts      = Set this to script values that the application
			                     supports, e.g, 0x04000001. Each bit in this 32-bit
			                     number represents a unique unicode script.

    2) Launch the application via the Ant target: "runSoftphone".

    3) Click on the GUI-handset to go off hook, and click on the buttons to make 
       calls between the softphone and other extensions on the Communication Manager.

CLICK2CALL APPLICATION - Registers an extension with Communication
Manager and monitors that extension for incoming and dialed calls. This
application also provides a GUI to perform directory lookup using LDAP.
Incoming call data is collected from display updates on the monitored
extension and displayed by the GUI as caller id, caller number, call time
and status (answered, missed, called back). Outgoing calls are also
logged. Calls are displayed color-coded according to status. The user
can use the call log GUI to return a call, or the directory lookup
GUI to make a call.

    1) Open the file $SDK_PATH/cmapijava-sdk/examples/resources/click2call.properties
       and edit the following values:

            callserver       = IP address of Communication Manager (C-LAN or S8300)
            extension        = extension to register and receive calls
            password         = the extension's password on Communication Manager
            switchname       = Switch Connection name of the Communication Manager
            cmapi1.server_ip = IP address for the connector server (AE Server)
            providerUrl      = (optional) URL to the LDAP server.  An example for domain
                               name "example.com" is shown below:
                               ldap://ldap_server.example.com:389/o=example.com

    2) Launch the application via the Ant target: "runClick2Call" (or "runClic2CallCCS"
       to run the Call Control Services version of the application).

    3) Log in using a valid username / password.  The default login is username = 
       avaya, password = avayapassword.

    4) From a station on the Communication Manager server with which the
       Click2Call application is registered, dial the extension number that
       you associated with the Click2Call application.


SESSION MANAGEMENT APPLICATION:
This tutorial application demonstrates how to write a session
management application using the Device, Media and Call Control API (DMCC).
It simulates a client application system with N+1 redundancy, where N is two.
The standby application takes over the devices that belonged to one of 
the applications. 

Three service providers are started - with one of them reserved as a standby. 
The remaining providers are used to make CSTA service requests.

Note that each provider represents a separate client application.

    The session state for each active provider is retrieved from the DMCC
    server using the standby provider. Next, one of the active sessions is 
    transferred to the standby application. After the transfer, the standby 
    application will reconstruct the station and listeners that are required.

    1) Open the file $SDK_PATH/cmapijava-sdk/examples/resources/sessionmanagement.properties
       and edit the following values:

        callserver          = IP address of Communication Manager (C-LAN or S8300)
        cmapi1.server_ip    = IP address for the connector server (AE Server)
        partytotransfer     = extension transferred by the application to a
                              different session.
        callingparty        = extension calling the transferred party after
                              the session transfer is successful.
        calledparty         = the transferred party calls this extension after
                              session transfer is successful.
        password            = the extension's password on Communication Manager.
        
    2) Launch the application via the Ant target: "runSessionManagement".

    3) The messages on the console will display the steps taken to transfer a session:

        - GetDeviceIDListResponse and GetMonitorListResponse message content for
          each of the sessions involved.
        - a message that indicates the transferred session was terminated.
        - a message to confirm that one session has been transferred to another one.
        - a message that indicates listeners have been created and replaced for
          each transferred monitor cross reference id.
        - Finally, "transferred" extension would call "calledparty" extension
          if it is registered.

SELECTIVE STREAM APPLICATION:
This tutorial application demonstrates how to write an application which 
utilizes selective stream API introduced since 8.0.1 for requesting selective
stream from one or more parties in the call. It registers a station
in Dependent or Independent mode and monitors Established Event for a
station. When any party added in the call with a station, EstablishedEvent 
is received by an application and option is given to send SelectiveStreamRequest 
to Communication Manager for requesting split stream based on parties in the
call. Application interact with user during runtime to take input for 
various API input parameters. 

You can also request Split stream at time of registration. Refer setting
described in step#1 below.

Application also performs recording on AE Services server for the selected
device. Location and filename is printed on console during execution. Usually
files are located in /var/media location on AE Services Server. It is good
idea to cleanup such files once testing is complete.

To dig deep into application working, please refere source code at location
$SDK_PATH/cmapijava-sdk/examples/src/sampleapps/selectivestream/SelectiveStreamApp.java

There may be unused device and/or session remain active on AE Services
Server at end of application execution. To manually cleanup device and/or session, go to
OAM -> Status -> Status and Control -> DMCC Servic Summary -> Select Session
and click Terminate Session. 

    1) Open the file $SDK_PATH/cmapijava-sdk/examples/resources/selectivestream.properties
       and edit the following values:

        callserver          = IP address of Communication Manager (C-LAN or S8300)
        cmapi1.server_ip    = IP address for the connector server (AE Server)
        station     	    = Station for which you want to send Split Stream
                              Request
	password            = the extension's password on Communication Manager.
	dependencyMode	    = Dependency mode for registering station
                              (DEPENDENT or INDEPENDENT)	
        codec		    = Media Codec for registering station
	encryption	    = Media encryption for registering station
	sessionMode	    = Set it to true if you want multipe session to
			      control a device else set it to false.
	ssrMode		    = Set this to pre-call, if you want to request
			      selective stream configuration at time of registration using
			      RegisterTerminalRequest. Set this to mid-call,
			      if you want to request selective stream configuration during mid-call using
			      SelectiveStreamRequest API.
        
    2) Build the application via the Ant target: "buildSelectiveStreamApp" or
 						 "buildAll".

    3) Launch the application via the Ant target: "runSelectiveStreamApp".


