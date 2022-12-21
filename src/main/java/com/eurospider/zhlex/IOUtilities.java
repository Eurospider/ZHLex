/*
 * Copyright (C) 2013 LEXspider <lexspider@eurospider.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.eurospider.zhlex;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.mozilla.universalchardet.UniversalDetector;

/**
 *   Various I/O helper methods.
 *
 */
public class IOUtilities {

	/**
	 *  Checks that the specified directory exists and has read/write rights. Creates the directory if it does not exist.
	 * @throws IOException if the folder cannot be used
	 */
	public static void ensureDirectoryIsUsable (File directory) throws IOException {
		if (directory.exists ()) {
			if ((directory.isDirectory ()) && (directory.canWrite ()) && (directory.canRead ())) {
				//ok
			}else {
				throw new IOException ("File exists but cannot be used as a directory :: " + directory.getAbsolutePath ());
			}
		}else {
			if (!directory.mkdirs ()) {
				throw new IOException ("Could not create directory :: " + directory.getAbsolutePath ());
			}
		}
	}
	
	/**
	 * Checks if a file exists and is executable.
	 * @throws IOException if the file doesn't exist are is not a binary or is not executable
	 */
	public static void ensureFileExistsAndIsExecutable (File file) throws IOException {
		if (file.exists ()) {
			if (file.canExecute()) {
				//ok
			}else {
				throw new IOException ("File exists but is not executabe :: " + file.getAbsolutePath ());
			}
		}else {
			throw new IOException ("File doesn't exist :: " + file.getAbsolutePath ());
		}
	}
	
	/**
	 *   Checks that the specified file exists, is a regular file and can be read.
	 *
	 * @throws IOException if the file cannot be used
	 */
	public static void ensureFileCanBeRead (File file) throws IOException {
		ensureFileCanBeRead (file, false);
	}

	/**
	 *   Checks that the specified file exists, is a regular file and can be read. Additionally, if the file does not exist,
	 * it will be created if the corresponding flag is set to true (will throw an exception if set to false)
	 *
	 * @throws IOException if the file cannot be used
	 */
	public static void ensureFileCanBeRead (File file, boolean createIfMissing) throws IOException {
		if (file.exists ()) {
			if ((file.isFile ()) && (file.canRead ())) {
				//ok
			}else {
				throw new IOException ("File exists but cannot be used :: " + file.getAbsolutePath ());
			}
		}else {
			if (createIfMissing) {
				if (!file.createNewFile ()) {
					throw new IOException ("Creation of empty file failed (createNewFile returned false)");
				}
			}else {
				throw new IOException ("File does not exist :: " + file.getAbsolutePath ());
			}
		}
	}
	
	/**
	 * @see guessEncoding (byte[] data)
	 */
	public static String guessEncoding (File file) throws IOException {
		byte[] fileContent = FileUtils.readFileToByteArray (file);
		return guessEncoding (fileContent);
	}
	
	/**
	 *   Guess the encoding from a binary representation of a String. Best effort attempt, no guarantees, returns null on failure.
	 */
	public static String guessEncoding (byte[] data) {
		UniversalDetector encodingDetector = new UniversalDetector (null);
		encodingDetector.handleData (data, 0, data.length);
		encodingDetector.dataEnd ();
		return encodingDetector.getDetectedCharset ();
	}
	
	/**
	 *  Try to guess the encoding and build the string using that encoding. If the encoding cannot be guessed, the default
	 * platform encoding is used.
	 */
	public static String readStringWithUnknownEncoding (byte[] encoded) throws UnsupportedEncodingException {
		String encoding = guessEncoding (encoded);
		String ret;
		if (encoding != null) {
			ret = new String (encoded, encoding);
		}else {
			ret = new String (encoded);
		}
		return ret;
	}

	/**
	 *  Deletes the first 2-3 bytes if they represent a byte order marker. WARNING: this method will delete the 
	 * relevant UTF-16 BOM. Only use if the correct encoding is already known or can be read from the rest of the data.
	 */
	public static byte[] filterOutBom (byte[] data) {
		int start = 0;
		int byte1, byte2, byte3;
		byte1 = data[0];
		byte2 = data[1];
		byte3 = data[2];
		if ((byte1 == 0xef) && (byte2 == 0xbb) && (byte3 == 0xbf)) {
			start = 3;
		}else if ((byte1 == 0xfe) && (byte2 == 0xff)) {
			start = 2;
		}else if ((byte1 == 0xff) && (byte2 == 0xfe)) {
			start = 2;
		}
		return Arrays.copyOfRange (data, start, data.length);
	}

}
