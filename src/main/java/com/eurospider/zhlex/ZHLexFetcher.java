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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Logger;

import org.apache.commons.io.FileUtils;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;

import org.apache.http.client.HttpClient;

import org.apache.commons.codec.binary.Base64;

import net.sf.practicalxml.ParseUtil;
import net.sf.practicalxml.OutputUtil;
import net.sf.practicalxml.builder.*;
import static net.sf.practicalxml.builder.XmlBuilder.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXParseException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;

public class ZHLexFetcher {

	private Logger LOGGER = Logger.getLogger (getClass ());
	static private HttpClient httpClient;
	static private File cacheDir;
	static private File outputDir;
	static private File outputPdfDir;
	static private File outputHtmlDir;
	static private File pdfToHtmlBinary;
	static PropertiesConfiguration configuration;
	static Map<String, Folder> folderMap = new HashMap<String, Folder>( );
	static Map<String, Meta> metaMap = new HashMap<String, Meta>( );	
	static Map<String, Content> contentMap = new HashMap<String, Content>( );
	
	private static void printUsage () {
		System.out.println ("Usage: java -jar target/zhlexfetcher-1.0-SNAPSHOT-jar-with-dependencies.jar zhlex.properties");
	}

	private File fetch( String url, String fileName )
	{
		File file = new File( cacheDir, fileName );
		
		boolean mustDownload = true;
		if( configuration.getBoolean( "fetcher.useLocalCache" ) ) {
			mustDownload = false;
			if( !file.exists( ) ) {
				mustDownload = true;
			}
		}
		
		if( mustDownload ) {
			url = url.replaceAll( " ", "%20" );
			LOGGER.info ("Downloading " + url);
			try
			{
				HttpUtilities.saveToFile (httpClient, url, file);
			} catch(IOException ex)
			{
				LOGGER.error( "Download error: " + url + ", " + ex.toString( ) );
				return null;
			}
		}
		
		return file;
	}

	private static class MyErrorHandler implements org.xml.sax.ErrorHandler {
		private int errorType = -1;
		SAXParseException error;

		public void warning( org.xml.sax.SAXParseException sAXParseException)
                        throws org.xml.sax.SAXException {
                    if (errorType < 0) {
                        errorType = 0;
                        error = sAXParseException;
                    }
                    //throw sAXParseException;
                }

                public void error(
                        org.xml.sax.SAXParseException sAXParseException)
                        throws org.xml.sax.SAXException {
                    if (errorType < 1) {
                        errorType = 1;
                        error = sAXParseException;
                    }
                    //throw sAXParseException;
                }

                public void fatalError(
                        org.xml.sax.SAXParseException sAXParseException)
                        throws org.xml.sax.SAXException {
                    errorType = 2;
                    throw sAXParseException;
                }

                public int getErrorType() {
                    return errorType;
                }

                public SAXParseException getError() {
                    return error;
                }
            }

	private Document createParser( File file ) throws FileNotFoundException, UnsupportedEncodingException {
		Document dom;

		InputStream fis = new FileInputStream( file );
		Reader r = new InputStreamReader( fis, "ISO-8859-1" );
		InputSource is = new InputSource( r );
		is.setEncoding( "ISO-8859-1" );

		ClassPathEntityResolver classPathEntityResolver = new ClassPathEntityResolver( );
 			
		dom = ParseUtil.validatingParse( is, classPathEntityResolver, 
			new MyErrorHandler( ) );

		return dom;
	}
	
	private String mapUrlToCachefileName( String url ) {
		return new String( Base64.encodeBase64( url.getBytes( ) ) );
	}
	
	private void pdfToHtml( File pdf, File outputDir ) throws IOException, InterruptedException {
		String[] cmd = new String[] { pdfToHtmlBinary.getAbsolutePath(), "-p", "-c", "-noframes", "-enc", "UTF-8", "-zoom", "2", pdf.getAbsolutePath() };

        ProcessBuilder pb = new ProcessBuilder( cmd );
        pb.directory( outputDir );
        Process process = pb.start( );
        process.waitFor( );
	}
	
	private void processFolder( Folder folder, Map<String, Folder> newFrontier ) throws FileNotFoundException, UnsupportedEncodingException, IOException, InterruptedException {
		File file = fetch( folder.url, mapUrlToCachefileName( folder.url ) );
		if( file == null ) return;
		
		// PDF content
		if( folder.url.endsWith( ".pdf" ) ) {
			Content content = contentMap.get( folder.url );
			// store it in output directory
			if(	configuration.getBoolean( "fetcher.storePdfs" ) ) {
				String fileName = content.id + ".pdf";
				LOGGER.info( "Storing PDF file to " + fileName );
				File dst = new File( outputPdfDir, fileName );
				FileUtils.copyFile( file, dst );
			}
				
			// do HTML conversion
			if( configuration.getBoolean( "fetcher.convertHtml" ) ) {
				String fileName = content.id + ".pdf";
				String baseName = content.id;
				File outputDir = new File( outputHtmlDir + "/" + baseName );
				IOUtilities.ensureDirectoryIsUsable( outputDir );
				File tmpPdfFile = new File( outputDir + "/" + fileName );
				FileUtils.copyFile( file, tmpPdfFile );
				
				LOGGER.info( "Converting PDF to HTML in '" + outputDir + "'" );
				
				pdfToHtml( tmpPdfFile, outputDir );
				
				FileUtils.forceDelete( tmpPdfFile );
			}
			
			return;
		}

		// subfolders
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			Document dom = createParser( file );
			NodeList nodes = (NodeList)xpath.evaluate( "/DATASHEET/DATASET/FIELDCONTENT[@columnname='Ordner']", dom, XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element elem = (Element)node;
					NamedNodeMap attrs = elem.getAttributes( );
					for( int j = 0; j < attrs.getLength( ); j++ ) {
						Attr attr = (Attr)attrs.item( j );
						if( attr.getNodeName( ) == "url" ) {
							String s = attr.getNodeValue( );
							String t = elem.getTextContent( );
							LOGGER.info( "Got folder URL " + s  + ": " + t);
							newFrontier.put( s, new Folder( s, t ) );
							folderMap.put( s, new Folder( s, t ) );
						}
					}
				}
			}
   		} catch( XPathExpressionException ex ) {
				LOGGER.error( "Parsing error: " + folder.url + ", " + ex.toString( ) );
				return;
		}

		// document metadata
		String lastId = "";
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			Document dom = createParser( file );
			NodeList nodes = (NodeList)xpath.evaluate( "/DATASHEET/DATASET/FIELDCONTENT[@columnname='Ordnungsnr']", dom, XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element elem = (Element)node;
					NamedNodeMap attrs = elem.getAttributes( );
					for( int j = 0; j < attrs.getLength( ); j++ ) {
						Attr attr = (Attr)attrs.item( j );
						if( attr.getNodeName( ) == "url" ) {
							String s = attr.getNodeValue( );
							String t = elem.getTextContent( );
							LOGGER.info( "Got document metadata URL " + s  + ": " + t);
							newFrontier.put( s, new Folder( s, t ) );
							metaMap.put( s, new Meta( s ) );
						}
						lastId = elem.getTextContent( );
					}
				}
			}
   		} catch( XPathExpressionException ex ) {
				LOGGER.error( "Parsing error: " + folder.url + ", " + ex.toString( ) );
				return;
		}

		// document content links
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			Document dom = createParser( file );
			NodeList nodes = (NodeList)xpath.evaluate( "/DATASHEET/DATASET/FIELDCONTENT[@columnname='Attachments']/LIST[@columnname='Attachments']", dom, XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element elem = (Element)node;
					NamedNodeMap attrs = elem.getAttributes( );
					for( int j = 0; j < attrs.getLength( ); j++ ) {
						Attr attr = (Attr)attrs.item( j );
						if( attr.getNodeName( ) == "url" ) {
							String s = attr.getNodeValue( );
							String t = elem.getTextContent( );
							LOGGER.info( "Got content URL " + s  + ": " + t);
							newFrontier.put( s, new Folder( s, t ) );
							contentMap.put( s, new Content( s, lastId ) );
						}
					}
				}
			}
   		} catch( XPathExpressionException ex ) {
				LOGGER.error( "Parsing error: " + folder.url + ", " + ex.toString( ) );
				return;
		}		
	}
	
	private void printStats( ) {
		LOGGER.info( "Seen " + folderMap.size( ) + " folders" );
		LOGGER.info( "Seen " + metaMap.size( ) + " document metadata items" );
		LOGGER.info( "Seen " + contentMap.size( ) + " content links" );
	}

	private void fillMetadata( ) throws FileNotFoundException, UnsupportedEncodingException {
        for( Map.Entry<String, Meta> entry : metaMap.entrySet( ) ) {
			String url = entry.getKey( );
			Meta meta = entry.getValue( );

			File file = fetch( url, mapUrlToCachefileName( url ) );
			if( file == null ) return;
			
			LOGGER.info( "Extracting Notes metadata from " + url );

			// document metadata
			try {
				XPath xpath = XPathFactory.newInstance( ).newXPath( );
				Document dom = createParser( file );
				NodeList nodes = ( NodeList )xpath.evaluate( "/DATASHEET/DATASET/FIELDCONTENT", dom, XPathConstants.NODESET);
				for (int i = 0; i < nodes.getLength( ); i++) {
					Node node = nodes.item( i );
					if( node.getNodeType( ) == Node.ELEMENT_NODE ) {
						Element elem = ( Element )node;
						NamedNodeMap attrs = elem.getAttributes( );
						for( int j = 0; j < attrs.getLength( ); j++ ) {
							Attr attr = ( Attr )attrs.item( j );
							if( attr.getNodeName( ).equals( "columnname" ) ) {
								if( attr.getNodeValue( ).equals( "Ordnungsnr" ) ) {
									meta.Ordnungsnr = elem.getTextContent( );
								} else if( attr.getNodeValue( ).equals( "Erlasstitel" ) ) {
									meta.Erlasstitel = elem.getTextContent( );
								} else if( attr.getNodeValue( ).equals( "Kurztitel" ) ) {
									meta.Kurztitel = elem.getTextContent( );
								} else if( attr.getNodeValue( ).equals( "Erlassdatum" ) ) {
									meta.Erlassdatum = elem.getTextContent( );
								} else if( attr.getNodeValue( ).equals( "Inkraftsetzungam" ) ) {
									meta.Inkraftsetzungam = elem.getTextContent( );
								} else if( attr.getNodeValue( ).equals( "Aufhebungam" ) ) {
									meta.Aufhebungam = elem.getTextContent( );
								} else if( attr.getNodeValue( ).equals( "Bandnr" ) ) {
									meta.Bandnr = elem.getTextContent( );
								} else if( attr.getNodeValue( ).equals( "Nachtragnr" ) ) {
									meta.Nachtragnr = elem.getTextContent( );
								} else if( attr.getNodeValue( ).equals( "Link" ))  {
									meta.Link = elem.getTextContent( );
								} else if( attr.getNodeValue( ).equals( "Publikationsdatum" ) ) {
									meta.Publikationsdatum = elem.getTextContent( );
								} else if( attr.getNodeValue( ).equals( "Materialien" ) ) {
									meta.Materialien = elem.getTextContent( );
								}
							}
						}
					}
				}
			} catch( XPathExpressionException ex ) {
					LOGGER.error( "Parsing metadata error: " + url + ", " + ex.toString( ) );
					return;
			}
			
			metaMap.put( url, meta );
		}
	}
			
	private void download( )  throws FileNotFoundException, UnsupportedEncodingException, IOException, InterruptedException {
		LOGGER.info("Starting ZHLex fetcher..");

		String url = "http://www2.zhlex.zh.ch/appl/zhlex_r.nsf/v?Open&vn=XMLLSAktuelleFassung";

		File file = fetch( url, mapUrlToCachefileName( url ) );
		if( file == null ) return;
		
		LOGGER.info ("Parsing main XML index page");
		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			Document dom = createParser( file );
			NodeList nodes = (NodeList)xpath.evaluate( "/DATASHEET/DATASET/FIELDCONTENT[@columnname='Ordner']", dom, XPathConstants.NODESET);
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element elem = (Element)node;
					NamedNodeMap attrs = elem.getAttributes( );
					for( int j = 0; j < attrs.getLength( ); j++ ) {
						Attr attr = (Attr)attrs.item( j );
						if( attr.getNodeName( ) == "url" ) {
							String s = attr.getNodeValue( );
							String t = elem.getTextContent( );
							LOGGER.info( "Got folder URL " + s  + ": " + t);
							folderMap.put( s, new Folder( s, t ) );
						}
					}
				}
			}
   		} catch( XPathExpressionException ex ) {
				LOGGER.error( "Parsing error: " + url + ", " + ex.toString( ) );
				return;
		}
			
		// small crawler with frontier
		Map<String, Folder> frontier = new HashMap<String, Folder>( folderMap );
		while( !frontier.isEmpty( ) ) {
			Map<String, Folder> newFrontier = new HashMap<String, Folder>( );
			for( Map.Entry<String, Folder> entry : frontier.entrySet( ) ) {
				processFolder( entry.getValue( ), newFrontier );
			}
			frontier = newFrontier;
		}
		
		LOGGER.info("Terminating ZHLex fetcher..");
	}
	
	private void saveState( ) throws IOException {
		File stateFile = new File( cacheDir, "state.ser" );
		LOGGER.info( "Saving state of fetch to " + stateFile );
		OutputStream fout = new FileOutputStream( stateFile );
		ObjectOutputStream oos = new ObjectOutputStream( fout );
		oos.writeObject( folderMap );
		oos.writeObject( metaMap );
		oos.writeObject( contentMap );
		oos.close( );
	}
	
	private void restoreState( ) throws IOException, ClassNotFoundException {
		File stateFile = new File( cacheDir, "state.ser" );
		LOGGER.info( "Restoring state of fetch from " + stateFile );
		InputStream fin = new FileInputStream( stateFile );
		ObjectInputStream ois = new ObjectInputStream( fin );
		folderMap = (Map<String, Folder>)ois.readObject( );
		metaMap = (Map<String, Meta>)ois.readObject( );
		contentMap = (Map<String, Content>)ois.readObject( );		
		ois.close( );
	}
	
	private void generateMetaXml( ) throws IOException, FileNotFoundException {
		LOGGER.info( "Generating meta XML" );

        ElementNode root = element( "sendung" );
		
        // info header
		DateTime ts = DateTime.now( );
		DateTimeFormatter fmt = DateTimeFormat.forPattern( "yyyy-MM-dd" );
		String dateStr = fmt.print( ts );
		DateTimeFormatter fmtIso = ISODateTimeFormat.dateTime( );
		String isoDate = fmtIso.print( ts );
        root.addChild(
			element( "info",
				element( "version", text( "1.0" ) ),
				element( "datumPublikation", text( dateStr ) ),
				element( "datumErstellung", text( isoDate ) )
			)
		);
		
		// erlasse
		root.addChild( generateErlasseXml( ) );

		// unused in ZHLex
		root.addChild(
			element( "autorenStamm",
				element( "autorenStammID", attribute( "autorText", "(kein Autor)" ), text( "16" ) )
			)
		);
		
		// we only have laws in ZHLex
		root.addChild(
			element( "typenStamm",
				element( "typenStammID", attribute( "typText", "Gesetz" ), text( "1" ) )
			)
		);
		
		// unused in ZHLex
		root.addChild(
			element( "stufenStamm",
				element( "stufenStammID",
					attribute( "stufenText", "(undefiniert)" ),
					attribute( "level", "0" ),
					text( "0" )
				)
			)
		);
				
		// serialize to file
        File metaXmlFile = new File( outputDir, "meta.xml" );
        OutputStream fout = new FileOutputStream( metaXmlFile );
        fout.write( new String( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" ).getBytes( Charset.forName( "UTF-8" ) ) );
        OutputUtil.compactStream( root.toDOM( ), fout );
        fout.close( ); 
	}

	private String convertDate( String ikt ) {
		// ikt is defined as required and not nullable, sorry, no other option
		if( ikt.equals( "" ) ) return "1000-01-01";
		DateTimeFormatter fmtIn = DateTimeFormat.forPattern( "dd.MM.yyyy" );
		DateTime time = fmtIn.parseDateTime( ikt );
		DateTimeFormatter fmtOut = DateTimeFormat.forPattern( "yyyy-MM-dd" );
		return fmtOut.print( time );
	}
	
	private String generateErlassId( String s )
	{
		String parts[] = s.split( "\\." );
		int res = 0;
		for( int i = 0; i < parts.length; i++ ) {
			res *= 1000;
			int partInt = Integer.parseInt( parts[i] );
			res += partInt;
		}
				
		return Integer.toString( res );
	}
	
	private ElementNode generateErlasseXml( ) {
		ElementNode root = element( "erlasse" );
        for( Map.Entry<String, Meta> entry : metaMap.entrySet( ) ) {
			Meta meta = entry.getValue( );
			String id = generateErlassId( meta.Ordnungsnr );
			String fileName = meta.Ordnungsnr;
						
			root.addChild(
				element( "erlass",
					element( "nummern",
						element( "erlassID", text( id ) ),
						element( "syst", text( meta.Ordnungsnr ) ),
						element( "struktur", text( meta.Ordnungsnr ) )
					),
					element( "autoren",
						element( "autorID", text( "16" ) )
					),
					element( "bezeichnungen",
						element( "titelVoll", text( meta.Erlasstitel ) ),
						element( "titelKurz", text( meta.Kurztitel ) )
					),
					element( "typen",
						element( "erlassTypID", text( "1" ) ),
						element( "normStufeID", text( "0" ) )
					),
					element( "daten",
						element( "iktErlass", text( convertDate( meta.Inkraftsetzungam ) ) ),
						meta.Aufhebungam.equals( "" ) ? null :
							element( "aktErlass", text( convertDate( meta.Aufhebungam ) ) ),
						element( "beschluesse",
							element( "beschlussDatum", text( convertDate( meta.Erlassdatum ) ) )
						),
						element( "letzteAenderung", text( convertDate( meta.Publikationsdatum ) ) )
					),
					element( "fassungen",
						element( "fassung",
							element( "nummer", text( "1" ) ),
							element( "iktFassung", text( convertDate( meta.Inkraftsetzungam ) ) ),
							element( "publikationen",
								element( "publikation", attribute( "publicationType", "i" ),
									text( meta.Link.replaceAll( "&amp;", "&" ) ) )
							),
							element( "dateiName", text( fileName ) )
						)
					)
				)
			);
		}
		return root;
	}
		
	public static void main( String[] args ) {	
		if( args.length != 1 ) {
			printUsage ();
			System.exit (1);
		}
		
		try {
			configuration = new PropertiesConfiguration (args[0]);
			ConfigurationSingleton configInstance = ConfigurationSingleton.getConfigInstance();
			configInstance.setConfiguration(configuration);		
			configuration.setThrowExceptionOnMissing (true);

			if (configuration.getBoolean ("fetcher.useProxy")) {
				httpClient = HttpUtilities.getProxyClient (configuration.getString ("fetcher.proxy.host"),
						  configuration.getInt ("fetcher.proxy.port"));
				System.getProperties().put("proxySet", "true");  
				System.getProperties().put("proxyHost", configuration.getString ("fetcher.proxy.host"));  
				System.getProperties().put("proxyPort", configuration.getInt ("fetcher.proxy.port"));  
			}else {
				httpClient = HttpUtilities.getDirectClient ();
			}

			cacheDir = new File (configuration.getString ("fetcher.cacheDir"));
			IOUtilities.ensureDirectoryIsUsable (cacheDir);

			outputDir = new File( configuration.getString( "fetcher.outputDir" ) );
			IOUtilities.ensureDirectoryIsUsable( outputDir );
			
			outputPdfDir = new File (configuration.getString ("fetcher.outputPdfDir"));
			IOUtilities.ensureDirectoryIsUsable( outputPdfDir );

			outputHtmlDir = new File (configuration.getString ("fetcher.outputHtmlDir"));
			IOUtilities.ensureDirectoryIsUsable( outputHtmlDir );
			
			pdfToHtmlBinary = new File (configuration.getString ("fetcher.pdftohtml"));
			IOUtilities.ensureFileExistsAndIsExecutable( pdfToHtmlBinary );
			
			// crawl the notes pages and store metadata serialized
			ZHLexFetcher fetcher = new ZHLexFetcher( );
			if( configuration.getBoolean( "fetcher.download" ) ) {
				fetcher.download( );
				
				// iterate all XML metadata fields and add them to metaMap
				fetcher.fillMetadata( );
				
				fetcher.saveState( );
			} else {
				fetcher.restoreState( );
			}

			// print statistics (how many documents, folders, metadata)
			fetcher.printStats( );
			
			// output meta XML
			fetcher.generateMetaXml( );
		} catch( Exception ex ) {
			// no matter what happens, write a log entry with the error
			Logger logger = Logger.getLogger( ex.getStackTrace( )[0].getClassName( ) );
			logger.error( "fetcher failed", ex );
		}
	}
	
}
