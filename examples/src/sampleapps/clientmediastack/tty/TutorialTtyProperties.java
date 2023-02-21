/*
 * TutorialMediaProperties.java
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import com.avaya.common.logging.Logger;

import com.avaya.mvcs.framework.CmapiKeys;

/**
 * A helper class for storing the properties specific to the
 * {@link TutorialTtyApp} application.
 * 
 * @author Avaya Inc.
 * @since Apr 2, 2007
 */
public final class TutorialTtyProperties implements Cloneable {
    
    Logger log = Logger.getLogger(TutorialTtyProperties.class.getName());
    
    protected static final String PROP_FILE_CALL_SERVER = "callserver";

    protected static final String PROP_FILE_EXTENSION = "extension";

    protected static final String PROP_FILE_EXT_PASSWORD = "password";

    protected static final String PROP_FILE_MEDIA_CODEC = "codec";

    protected static final String PROP_FILE_MEDIA_ENCRYPTION = "encryption";

    protected static final String PROP_FILE_SERVER_IP = "cmapi1.server_ip";

    protected static final String PROP_FILE_USERNAME = "cmapi1.username";

    protected static final String PROP_FILE_PASSWORD = "cmapi1.password";

    protected static final String PROP_FILE_SERVER_PORT = "cmapi1.server_port";

    protected static final String PROP_FILE_SECURE = "cmapi1.secure";

    protected static final String PROP_FILE_TRUST_LOC =
            "cmapi.trust_store_location";

    protected static final String PROP_FILE_CONV_LOG = "app.conversationLog";
    
    protected static final String PROP_FILE_CONV_LOG_FILE = "app.conversationLog.file";
    
    protected static final String PROP_FILE_BAUD_RATE = "app.baudot.baud.rate";

    protected static final String PROP_FILE_RTP_DIRECTION = "app.rtp.direction";

    protected static final String PROP_FILE_REDUNDANCY = "app.tty.redundancy";

    private Properties props;

    private URL appURL;

    public String getAesEncryption() {
        return props.getProperty(PROP_FILE_SECURE);
    }
    
    public boolean isAesEncryptionEnabled() {
        return (props.getProperty(PROP_FILE_SECURE) != null
                && props.getProperty(PROP_FILE_SECURE).equals("true"));
    }

    public void setAesEncryptionEnabled(boolean aesEncryptionEnabled) {
        props.setProperty(PROP_FILE_SECURE,
                Boolean.toString(aesEncryptionEnabled));
    }

    public String getAesPassword() {
        return props.getProperty(PROP_FILE_PASSWORD);
    }

    public void setAesPassword(String aesPassword) {
        props.setProperty(PROP_FILE_PASSWORD, aesPassword);
    }

    public String getAesServerIP() {
        return props.getProperty(PROP_FILE_SERVER_IP);
    }

    public void setAesServerIP(String aesServerIP) {
        props.setProperty(PROP_FILE_SERVER_IP, aesServerIP);
    }

    public String getAesServerPort() {
        return props.getProperty(PROP_FILE_SERVER_PORT);
    }

    public void setAesServerPort(String aesServerPort) {
        props.setProperty(PROP_FILE_SERVER_PORT, aesServerPort);
    }

    public String getAesUsername() {
        return props.getProperty(PROP_FILE_USERNAME);
    }

    public void setAesUsername(String aesUsername) {
        props.setProperty(PROP_FILE_USERNAME, aesUsername);
    }

    public String getCallServerIP() {
        return props.getProperty(PROP_FILE_CALL_SERVER);
    }

    public void setCallServerIP(String callServerIP) {
        props.setProperty(PROP_FILE_CALL_SERVER, callServerIP);
    }

    public String getCodec() {
        return props.getProperty(PROP_FILE_MEDIA_CODEC, "G711U");
    }

    public void setCodec(String codec) {
        props.setProperty(PROP_FILE_MEDIA_CODEC, codec);
    }

    public String getExtension() {
        return props.getProperty(PROP_FILE_EXTENSION);
    }

    public void setExtension(String extension) {
        props.setProperty(PROP_FILE_EXTENSION, extension);
    }

    public String getExtensionPassword() {
        return props.getProperty(PROP_FILE_EXT_PASSWORD);
    }

    public void setExtensionPassword(String extensionPassword) {
        props.setProperty(PROP_FILE_EXT_PASSWORD, extensionPassword);
    }

    public String getMediaEncryption() {
        return props.getProperty(PROP_FILE_MEDIA_ENCRYPTION, "none");
    }

    public void setMediaEncryption(String mediaEncryption) {
        props.setProperty(PROP_FILE_MEDIA_ENCRYPTION, mediaEncryption);
    }

    public String getTrustStoreLocation() {
        return props.getProperty(PROP_FILE_TRUST_LOC);
    }

    public void setTrustStoreLocation(String trustStoreLocation) {
        props.setProperty(PROP_FILE_TRUST_LOC, trustStoreLocation);
    }

    public String getTrustStorePassword() {
        return props.getProperty(CmapiKeys.TRUST_STORE_PASSWORD);
    }

    public String getKeyStoreLocation() {
        return props.getProperty(CmapiKeys.KEY_STORE_LOCATION);
    }
    public String getKeyStoreType() {
        return props.getProperty(CmapiKeys.KEY_STORE_TYPE);
    }
    public String getKeyStorePassword() {
        return props.getProperty(CmapiKeys.KEY_STORE_PASSWORD);
    }
    public String getValidPeer() {
        return props.getProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE);
    }
    public String getHostnameValidation(){
        return props.getProperty(CmapiKeys.VALIDATE_SERVER_CERTIFICATE_HOSTNAME);
    }

    public void setTrustStorePassword(String trustStoreLocation) {
        props.setProperty(PROP_FILE_TRUST_LOC, trustStoreLocation);
    }

    public boolean isConvLogEnabled() {
        return (props.getProperty(PROP_FILE_CONV_LOG) != null
                && props.getProperty(PROP_FILE_CONV_LOG).equals("true"));
    }
    
    public void setConvLogEnabled(boolean val) {
        props.setProperty(PROP_FILE_CONV_LOG, Boolean.toString(val));
    }

    public String getConvLogFilename() {
        return props.getProperty(PROP_FILE_CONV_LOG_FILE);
    }

    public void setConvLogFilename(String convLogFilename) {
        props.setProperty(PROP_FILE_CONV_LOG_FILE, convLogFilename);
    }
    
    public String getBaudotBaudRate() {
        return props.getProperty(PROP_FILE_BAUD_RATE, "US");
    }
    
    public void setBaudotBaudRate(String baudotBaudRate) {
        props.setProperty(PROP_FILE_BAUD_RATE, baudotBaudRate);
    }
    
    public String getRtpDirection() {
        return props.getProperty(PROP_FILE_RTP_DIRECTION, "XMIT_RCV");
    }

    public void setRtpDirection(String rtpDirection) {
        props.setProperty(PROP_FILE_RTP_DIRECTION, rtpDirection);
    }

    public String getTtyRedundancy() {
        return props.getProperty(PROP_FILE_REDUNDANCY, "3");
    }

    public void setTtyRedundancy(String redundancy) {
        props.setProperty(PROP_FILE_REDUNDANCY, redundancy);
    }

    protected TutorialTtyProperties() {
        props = new Properties();

        ClassLoader cl = TutorialTtyApp.class.getClassLoader();
        appURL = cl.getResource("tutorialClientMediaStack.properties");
    }
    
    public void load() {
        try {
            props.load(appURL.openStream());
        } catch (IOException e) {
            log.warning("Unable to open the stored properties: " + e);
        }
    }

    public void store() throws FileNotFoundException, IOException,
            URISyntaxException {
        props.store(new FileOutputStream(new File(appURL.toURI())), null);
    }
    
    public TutorialTtyProperties clone() {
        TutorialTtyProperties clone = new TutorialTtyProperties();
        clone.props = (Properties) props.clone();
        return clone;
    }
}
