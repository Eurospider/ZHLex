# LEXspider ZHLEX web interface

# Files

/meta.xml     XML metadata file, following the lexspider.xsd schema  
/pdf          PDF files with sentences   
/html         converted HTML files

# Installation and running

mvn install package   
java -jar -Xms128m -Xmx256m target/zhlexfetcher-1.0-SNAPSHOT-jar-with-dependencies.jar zhlex.properties

Expose the 'htdocs' folder to the web (e.g. using an Apache web server)

# Live system (password-protected)

http://lexspider-zhlex-data.eurospider.com/zhlex-ikt-filtered.rss

SVN: https://svn.eurospider.com/projects/ZHLex/trunk/
