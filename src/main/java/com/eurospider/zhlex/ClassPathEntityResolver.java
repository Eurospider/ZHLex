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

import java.io.IOException;
import java.io.InputStream;
import java.io.File;

import java.net.URI;

import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

/**
 * Local entity resolver for the SAX parser to avoid loading of
 * external entities (as the XML import DTD from eurospider.com).
 * <p>
 * Applies a very simple algorithm: checks if the public URI is
 * a http URL, if yes checks if the full path is a resource, e. g.
 * <p>
 * An XML file with the following doctype declaration:
 * <p>
 * <pre>
 * &lt;!DOCTYPE retrieval-monitor-import PUBLIC
 *    "-//Eurospider/Retrieval Monitor Import DTD 0.0.1//EN"
 *        "http://www.eurospider.com/dtds/relevancy-retrieval-monitor-import-0.0.1.dtd"&gt;
 * </pre>
 * <p>
 * would try to request:
 * <p>
 * <pre>
 * http://www.eurospider.com/dtds/relevancy-retrieval-monitor-import-0.0.1.dtd
 * </pre>
 * <p>
 * This class checks now for the resource:
 * <p>
 * <pre>
 * dtds/relevancy-retrieval-monitor-import-0.0.1.dtd
 * </pre>
 * <p>
 * somewhere in the class path.
 * <p>
 * For example in a WAR file we usually have WEB-INF/classes as a classpath, so we
 * can safely put the DTD at the following location:
 * <p>
 * <pre>
 * WEB-INF/classes/dtds/relevancy-retrieval-monitor-import-0.0.1.dtd 
 * </pre>
 * <p>
 * Note that if we don't find the resource anywhere in the classpath we return null
 * thus causing the behaviour of default EntityResolver to take over (load from original
 * place).
 * <p>
 * Code example on how to set the entity resolver:
 * <p>
 * <pre>
 * XMLReader xmlReader = XMLReaderFactory.createXMLReader( );
 * MyHandler handler = new MyHandler( );
 * ClassPathEntityResolver classPathEntityResolver = new ClassPathEntityResolver( );
 * xmlReader.setContentHandler( handler );
 * xmlReader.setErrorHandler( handler );
 * xmlReader.setEntityResolver( classPathEntityResolver );
 * xmlReader.setFeature( "http://xml.org/sax/features/validation", true );
 * xmlReader.parse( new InputSource( someStream ) );
 * </pre>
 *
 */

public class ClassPathEntityResolver implements EntityResolver {
   public InputSource resolveEntity( String publicId, String systemId )
       throws IOException, SAXException {
      InputSource inputSource = null;
      
      try {
         // if we have an external entity on a web server we check if
         // we have a file named as mentionen as last in the URL path
         //System.out.println ("ENTITY RESOLVER received: (" + publicId + ", " + systemId + ")");
         String filename;
         InputStream inputStream;
         File f = new File (systemId);
         filename = f.getName ();
         //filename = "/dtds/" + filename;
         //System.out.println ("ENTITY RESOLVER parsed and searching for : " + filename);
         inputStream = ClassPathEntityResolver.class.getClassLoader( ).getResourceAsStream( filename );
         //System.out.println ("ENTITY RESOLVER found: " + inputStream);
         inputSource = new InputSource( inputStream);
      } catch( Exception e ) {
         // ignore exception, try to load from original place
      }
      
      return inputSource;
   }
}
