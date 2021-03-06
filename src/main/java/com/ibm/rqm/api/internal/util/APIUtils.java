/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2011, 2020. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Attribute;
import org.jdom.Element;

/**
 * <p>API utilities.</p>
 * 
 *  
 * @author  Paul Slauenwhite
 * @version 1.0
 * @since   0.9
 */
public class APIUtils {

	public static InputStream copy(InputStream incomingStream) throws IOException {
		return new ByteArrayInputStream(toByteArray(incomingStream));
	}

	public static String toString(List<?> list){

		StringBuilder string = new StringBuilder();

		if((list != null) && (!list.isEmpty())){

			for(Object element : list){

				if(string.length() > 0){
					string.append(", "); //$NON-NLS-1$
				}

				string.append(element);
			}
		}

		return (string.toString());
	}

	public static String toString(InputStream inputStream) throws IOException {

		String xml = new String(toByteArray(inputStream), IAPIConstants.ENCODING_UTF8);
		
		//Problem:  XML content returned from the RQM Reportable REST API may contain special characters (e.g. no-break space), 
		//          which cause the JDOM XML parser to throw a SAXParseException:
		//          Invalid byte 1 of 1-byte UTF-8 sequence.
		//Solution: Scrub (remove or escape) all characters in the XML content not supported by XML.
		//Note:     As of RQM 6.0.6, the RQM Reportable REST API will not return XML content containing special characters.
		//Defect:   170880
		xml = scrubXmlCharacters(xml);
		
		return xml;
	}

	private static byte[] toByteArray(InputStream inputStream) throws IOException {
		ByteArrayOutputStream fileBytesOutputStream = new ByteArrayOutputStream();
		try {
			int read;
			byte[] next = new byte[32768];
			while ((read = inputStream.read(next)) != -1) {
				fileBytesOutputStream.write(next, 0, read);
			}

		} finally {
			inputStream.close();
			fileBytesOutputStream.close();
		}
		return fileBytesOutputStream.toByteArray();
	}
	
	public static boolean isSet(String value){		
		return ((value != null) && (!value.trim().isEmpty()));
	}

	public static boolean isAttributeSet(Element element, String attributeNalue){		
		
		if((element != null) && (isSet(attributeNalue))){
		
			Attribute attribute = element.getAttribute(attributeNalue);
			
			if(attribute != null){
				return (isSet(attribute.getValue()));
			}
		}
		
		return false;
	}

	public static boolean contains(Object[] array, Object element){

		if((array != null) && (array.length > 0) && (element != null)){

			for(Object arrayElement : array){

				if(element.equals(arrayElement)){
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Remove unicode characters not supported by XML.  The XML spec only
	 * allows the following characters:
	 * 
	 * 	Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
	 * 
	 * Also remove invalid entities such as &#11;
	 * 
	 * See workitem 42573
	 * 
	 * @see <link>http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char</link>
	 */
	//Note: Copied from com.ibm.rqm.integration.service.Util.scrubXmlCharacters(String).
	public static String scrubXmlCharacters (String s) {
		if (s == null || "".equals(s)) { //$NON-NLS-1$
			return s;
		}
	
		// Escaped control characters such as &#11; are not
		// valid in xml 1.0.  Including them in the xml would
		// cause any well written xml parser to fail.  These
		// characters need to be unescaped in the string so
		// that they can be checked by the character range
		// check below
		Pattern entityEscapePattern = Pattern.compile("&#(\\d+);"); //$NON-NLS-1$
		Matcher matcher = entityEscapePattern.matcher(s);
		while (matcher.find()) {
			int unescaped = Integer.valueOf(matcher.group(1));
			char character = ((char)(unescaped));
			if (!isValidXmlCharacter(character)) {
				s = s.replaceAll(matcher.group(), Character.toString(character));
			}
		}
	
		// ensure that all characters in the string are
		// in the range:
		// #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
		// characters not in this range will be omitted
		// from the string
		final int len = s.length();
		final StringBuilder builder = new StringBuilder();
		for (int i=0; i<len; i++) {
			char c = s.charAt(i);

			//Escape special characters (including Microsoft smart punctuation) with a decimal numeric character reference:
			//  0x2013: en dash
			//  0x2014: em dash 
			//  0x2032: prime 
			//  0x201C: left double quotation mark
			//  0x201D: right double quotation mark
			//  0x2018: left single quotation mark
			//  0x2019: right single quotation mark
			//  0x00A0: no-break space
			//  0x00AD: soft hyphen
			//  0x00B5: micro sign
			//  0x0247: latin small letter e with stroke
			//  0x2011: non-breaking hyphen
			//  0x2026: horizontal ellipsis
	        if((c == 0x2013) ||
	        		(c == 0x2014) ||
	        		(c == 0x2032) ||
	        		(c == 0x201C) ||
	        		(c == 0x201D) ||
	        		(c == 0x2018) ||
	        		(c == 0x2019) ||
	        		(c == 0x00A0) ||
	        		(c == 0x00AD) ||
	        		(c == 0x00B5) ||
	        		(c == 0x0247) ||
	        		(c == 0x2011) ||
	        		(c == 0x2026)){
				builder.append("&#"); //$NON-NLS-1$
				builder.append(((int)(c)));
				builder.append(";"); //$NON-NLS-1$
			}
			else if (isValidXmlCharacter(c)){
				builder.append(c);
			}
			else if (Character.isHighSurrogate(c) && (i+1<len)) {
				char c2 = s.charAt(i+1);
				if (Character.isLowSurrogate(c2)) {
					builder.append("&#"); //$NON-NLS-1$
					builder.append(Character.toCodePoint(c, c2));
					builder.append(";"); //$NON-NLS-1$
				}
			}
		}
	
		return builder.toString();
	}
	
	/**
	 * <p>Determines if a character is a valid XML character.</p>
	 * 
	 * <p>The <a href="http://www.w3.org/TR/2006/REC-xml11-20060816/#charsets">XML 1.1</a>  
	 * specification defines the following characters as valid XML characters:</p>
	 * 
	 * <ul>
	 * <li>#x9</li>
	 * <li>#xA</li>
	 * <li>#xD</li>
	 * <li>#x20-#xD7FF</li>
	 * <li>#xE000-#xFFFD</li>
	 * <li>#x10000-#x10FFFF</li>
	 * </ul>
	 * 
	 */
	//Note: Copied from com.ibm.rqm.integration.service.Util.isValidXmlCharacter(char).
	private static boolean isValidXmlCharacter(char character) {
		return ((character == 0x9) ||
				(character == 0xA) ||
				(character == 0xD) ||
				((character >= 0x20) && (character <= 0xD7FF)) ||
				((character >= 0xE000) && (character <= 0xFFFD)) ||
				((character >= 0x10000) && (character <= 0x10FFFF)));		
	}
}
