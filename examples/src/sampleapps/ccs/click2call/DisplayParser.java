/*
 * DisplayParser.java
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

package sampleapps.ccs.click2call;

import java.text.DateFormat;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * This class provides the functionality of parsing a lamp update to obtain
 * caller name and number. It also contains a method for parsing the phone
 * number obtained from directory lookup.
 * 
 * @author Avaya Inc.
 */
public final class DisplayParser {

    public void parseNameNumber(final String lampUpdateData, final Map<String, String> extnToName) {
        String callerId = "";
        String callingNumber = "";
        StringTokenizer tokens = new StringTokenizer(lampUpdateData);
        String lastToken = "";
        int tokensCount = tokens.countTokens();

        // validNumberCharSet represents the characters caller number can have.
        String validNumberCharSet = "0123456789-";
        String defaultValue = "Not available";

        boolean ifValidNumber = true;

        // If caller number is present, it would be the last token.
        for (int i = 1; i <= tokensCount - 1; i++) {
            callerId = callerId + tokens.nextToken() + " ";
        }
        // remove the last whitespace.
        callerId = callerId.trim();

        // If tokensCount == 0, tokens.nextToken() will throw
        // NoSuchElementException.
        try {
            lastToken = tokens.nextToken();
        } catch (NoSuchElementException nsee) {
            // No number is available, so we will store defaultValue as calling
            // number.
        }

        // Check if the last token is indeed the caller number or its part of
        // caller name.
        for (int j = 0; j < lastToken.length(); j++) {
            if (validNumberCharSet.indexOf(lastToken.charAt(j)) == -1) {
                ifValidNumber = false;
            }
        }

        if (ifValidNumber) {
            callingNumber = lastToken;
        } else {
            callerId = callerId + lastToken;
        }

        // If called id is not present, record default value.
        if (callerId == null || callerId.trim().length() == 0) {
            callerId = defaultValue;
        } else {
            callerId = callerId.substring(2).trim();
        }

        if (callingNumber == null || callingNumber.trim().length() == 0) {
            callingNumber = defaultValue;
        } else {
            callingNumber = parseCallingNumber(callingNumber);
        }

        extnToName.put(callingNumber, callerId);

        System.out.println("ParseNameNumber: name=" + callerId + " number=" + callingNumber);

    }

    public String getTime() {

        String callTime;

        GregorianCalendar calendar;
        DateFormat dateTimeFormatter = DateFormat.getDateTimeInstance();

        // record time of call reception.
        calendar = new GregorianCalendar();
        callTime = "" + dateTimeFormatter.format(calendar.getTime());
        return callTime;

    }

    /**
     * This methods check validity of the caller number and returns string
     * representation of the number removing any '-' if present. The caller
     * number should contain digits and '-' only.
     * 
     * @param callingNumber
     * @return String caller number with any '-' removed if present
     */
    public String parseCallingNumber(String callingNumber) {
        for (int i = 0; i < callingNumber.length(); i++) {
            if (callingNumber.charAt(i) == '-') {
                callingNumber = callingNumber.substring(0, i) + callingNumber.substring(i + 1, callingNumber.length());
                i--;
            }
        }

        return callingNumber;
    }

    /**
     * This methods parses the number obtained by performing directory search.
     * It removes any leading + (if present) and removes the whitespaces.
     * 
     * @param number the number obtained from directory lookup
     * 
     * @return String number with any leading + or whitespaces removed if
     *         present returns null if length of number is 0 after removing
     *         leading and trailing whitespaces.
     */
    public String parseNumberToDial(String number) {
        number = number.trim();

        if (number.length() == 0) {
            return null;
        }

        if (number.charAt(0) == '+') {
            number = number.substring(1);
        }

        // remove white spaces if occurring between digits of this number
        for (int i = 0; i < number.length(); i++) {
            if (number.charAt(i) == ' ') {
                number = number.substring(0, i) + number.substring(i + 1, number.length());
                i--;
            }

        }
        return number;
    }
}
