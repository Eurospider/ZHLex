#!/bin/sh

case "$0" in
   /* )
      base=`dirname $0`/..
   ;;
   * )
      base=`pwd`/`dirname $0`/..
   ;;
esac

# beautify directory, /a/b/c/../.. -> /a
base=`( cd $base; echo \`pwd\` )`

# where to find things
scriptdir="$base/scripts"
htdocsdir="$base/htdocs"
htmldir="$htdocsdir/html"
pdfdir="$htdocsdir/pdf"
metaxml="$htdocsdir/meta.xml"
csvfile="$htdocsdir/zhlex.csv"
archivedir="$htdocsdir/archive"
today=`date +"%Y%m%d"`
archivefile="$archivedir/zhlex-data-$today.tar.gz"

rm -rf "$htmldir"/* "$pdfdir"/* "$metaxml" "$csvfile" "$htdocsdir"/*.rss "$base"/cache

cd $base

java -jar -Xms128m -Xmx256m \
	target/zhlexfetcher-1.0-SNAPSHOT-jar-with-dependencies.jar \
	zhlex.properties

tar zcvf "$archivefile" "$htmldir"/* "$pdfdir"/* "$metaxml" "$csvfile" "$htdocsdir"/*.rss

$base/scripts/generate_rss.sh ikt ksta > htdocs/zhlex-ikt-filtered.rss
