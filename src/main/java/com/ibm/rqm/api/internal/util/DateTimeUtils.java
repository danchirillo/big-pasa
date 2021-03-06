/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2013. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.rqm.api.internal.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <p>Date/time utilities.</p>
 * 
 *  
 * @author  Paul Slauenwhite
 * @version 0.9
 * @since   0.9
 * @see     'ICleanerConstants
 */
public final class DateTimeUtils {

	private static String ISO_8601_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"; //$NON-NLS-1$
	private static String RFC_3339_DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; //$NON-NLS-1$
	
	private static SimpleDateFormat iso8601DateFormat = new SimpleDateFormat(ISO_8601_DATE_PATTERN, Locale.ENGLISH); 
	private static SimpleDateFormat rfc3339DateFormat = new SimpleDateFormat(RFC_3339_DATE_PATTERN, Locale.ENGLISH); 

	static {

		iso8601DateFormat.setTimeZone(TimeZone.getTimeZone("GMT_ID")); //$NON-NLS-1$
		rfc3339DateFormat.setTimeZone(TimeZone.getTimeZone("GMT_ID")); //$NON-NLS-1$
	}

	public static long parseDateTime(String dateTime) {

		if((dateTime != null) && (!dateTime.trim().isEmpty())){

			//Parse the date/time in the IETF/RFC3339 format (default):
			long parsedDateTime = parseRfc3339DateTime(dateTime);	

			//Parse the date/time in the W3C/ISO8601 format:
			if(parsedDateTime == -1){
				parsedDateTime = parseIso8601DateTime(dateTime);
			}

			return parsedDateTime;
		}

		return -1;
	}

	public static long parseIso8601DateTime(String dateTime) {

		if((dateTime != null) && (!dateTime.trim().isEmpty())){

			try {
				return (iso8601DateFormat.parse(dateTime).getTime());
			} 
			catch (ParseException p) {
				//Ignore and return -1.
			}
		}

		return -1;
	}

	public static String formatIso8601DateTime(long dateTime) {
		return (iso8601DateFormat.format(dateTime));
	}

	public static long parseRfc3339DateTime(String dateTime) {

		if((dateTime != null) && (!dateTime.trim().isEmpty())){

			try {
				return (rfc3339DateFormat.parse(dateTime).getTime());
			} 
			catch (ParseException p) {
				//Ignore and return -1.
			}
		}

		return -1;
	}

	public static String formatRfc3339DateTime(long dateTime) {
		return (rfc3339DateFormat.format(dateTime));
	}
}
