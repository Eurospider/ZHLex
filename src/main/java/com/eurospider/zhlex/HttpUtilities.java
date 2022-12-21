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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 *   Helper methods for gatherers that get content from the web.
 *
 */
public class HttpUtilities {
	private static final int CONNECTION_TIMEOUT = 300000;
	private static final int SO_TIMEOUT = 300000;

	public static HttpClient getDirectClient () {
		return getDirectClient (CONNECTION_TIMEOUT, SO_TIMEOUT);
	}

	public static HttpClient getDirectClient (int connectTimeout, int soTimeout) {
		HttpClient httpClient = new DefaultHttpClient ();
		HttpConnectionParams.setConnectionTimeout (httpClient.getParams (), connectTimeout);
		HttpConnectionParams.setSoTimeout (httpClient.getParams (), soTimeout);
		return httpClient;
	}

	public static HttpClient getProxyClient (String host, int port) {
		return getProxyClient (host, port, CONNECTION_TIMEOUT, SO_TIMEOUT);
	}

	public static HttpClient getProxyClient (String host, int port, int connectTimeout, int soTimeout) {
		HttpClient httpClient = getDirectClient (connectTimeout, soTimeout);
		HttpHost proxy = new HttpHost(host, port);
		ConnRouteParams.setDefaultProxy (httpClient.getParams (), proxy);
		return httpClient;
	}

	public static HttpClient getProxyClientWithAutomaticGzipDecompression (String host, int port) {
		return getProxyClientWithAutomaticGzipDecompression (host, port, CONNECTION_TIMEOUT, SO_TIMEOUT);
	}

	public static HttpClient getProxyClientWithAutomaticGzipDecompression (String host, int port, int connectTimeout, int soTimeout) {
		HttpClient httpClient = getDirectClient (connectTimeout, soTimeout);
		HttpHost proxy = new HttpHost(host, port);
		ConnRouteParams.setDefaultProxy (httpClient.getParams (), proxy);
		((DefaultHttpClient)httpClient).addResponseInterceptor (new HttpResponseInterceptor () {
			@Override
			public void process (final HttpResponse response, final HttpContext context) throws HttpException, IOException {
				HttpEntity entity = response.getEntity ();
				if (entity != null) {
					Header contentEncodingHeader = entity.getContentEncoding ();
					if (contentEncodingHeader != null) {
						HeaderElement[] headerElements = contentEncodingHeader.getElements ();
						for (int i = 0; i < headerElements.length; i++) {
							if (headerElements[i].getName ().equalsIgnoreCase ("gzip")) {
								response.setEntity (new GzipDecompressingEntity (response.getEntity ()));
								return;
							}
						}
					}
				}
			}
		});
		return httpClient;
	}

	public static void saveToFile (HttpClient httpClient, String url, File outputFile) throws IOException {
		InputStream in = getContentAsStream (httpClient, url);
		try {
			FileOutputStream fos = new FileOutputStream (outputFile);
			try {
				IOUtils.copy (in, fos);
			}finally {
				fos.close ();
			}
		}finally {
			in.close ();
		}
	}

	public static InputStream getContentAsStream (HttpClient httpClient, String url) throws IOException {
		return issueGetRequest (httpClient, url).getContent ();
	}

	/**
	 *   Return the content of a HTML page. This should only be used for relatively small pages. For longer or non-html
	 * content, use of saveToFile method is highly recommended instead.
	 */
	public static String getPageContent (HttpClient httpClient, String url) throws IOException {
		return EntityUtils.toString (issueGetRequest (httpClient, url));
	}
	
	private static HttpEntity issueGetRequest (HttpClient httpClient, String url) throws IOException {
		HttpUriRequest request = new HttpGet (url);
		HttpResponse httpResponse = httpClient.execute (request);
		StatusLine statusLine = httpResponse.getStatusLine ();
		if (statusLine.getStatusCode () != HttpStatus.SC_OK) {
			EntityUtils.consume (httpResponse.getEntity ());
			throw new IOException ("Could not retrieve content. Server responded with status code " + 
					  statusLine.getStatusCode () + " - " + statusLine.getReasonPhrase ());
		}
		return httpResponse.getEntity ();
	}

}
