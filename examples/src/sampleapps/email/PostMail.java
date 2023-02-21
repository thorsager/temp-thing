/*
 * PostMail.java
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
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;


//*****************************************************************************
//* PostMail
//*****************************************************************************


/**
 * This class is called when a lampMode event occurred that changed the
 * lamp status from flashing green to off.  That means the station
 * did not answer the call and the call went to coverage or the caller
 * hung up the phone. So need to send email to notify the user of the call. 
 * 
 * @param recipients - list of recipients to send email to
 * @param subject - subject for the email
 * @param message - message body for the email
 * @param from - the sender of the email
 * @param smtpserver - the email server you wish to send email to
 */

public class PostMail{
    
    public boolean sendTheMail( String[] recipients, String subject,
      String message, String from, String smtpserver) 
      throws MessagingException {
          
        if (recipients == null) {
            System.out.println("Cannot send email - recipients field is null");
            return false;
        }
        if (smtpserver == null) {
            System.out.println("cannot send mail - smtpserver field is null");
            return false;
         }
        if (from == null) {
            System.out.println("cannot send mail - from field is not set"); 
            return false;
        }
        if (subject == null) {
            System.out.println("cannot send mail - subject field is not set"); 
            return false;
        }
        if (message == null) {
            System.out.println("cannot send mail - message field is not set"); 
            return false;
        }
    
        boolean debug = false;

        //Set the host smtp address
        Properties props = new Properties();

        props.put("mail.smtp.host", smtpserver);
 
        // create some properties and get the default Session
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(debug);

        // create a message
        Message msg = new MimeMessage(session);

        // set the from and to address
        InternetAddress addressFrom = new InternetAddress(from);
        msg.setFrom(addressFrom);

        InternetAddress[] addressTo = new InternetAddress[recipients.length]; 
        for (int i = 0; i < recipients.length; i++)
        {
            addressTo[i] = new InternetAddress(recipients[i]);
        }
        msg.setRecipients(Message.RecipientType.TO, addressTo);
        
        // Optional : You can also set your custom headers in the Email if you
        // want
        msg.addHeader("MyHeaderName", "myHeaderValue");

        // Setting the Subject and Content Type
        msg.setSubject(subject);
        msg.setContent(message, "text/plain");
        Transport.send(msg);
        return true;
    }
}