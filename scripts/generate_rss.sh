#!/bin/sh

export TZ=CET

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

ORDER=$1
if test "x$ORDER" = "xpub"; then
   ORDER="pub"
else
   if test "x$ORDER" = "xikt"; then
      ORDER="ikt"
   else
      echo "ERROR: order date expected 'pub' or 'ikt'" 1>&2
      exit 1
   fi
fi

FILTER=$2
if test "x$FILTER" = "xall"; then
   FILTER="all"
else
   if test "x$FILTER" = "xksta"; then
      FILTER=ksta
   else
      echo "ERROR: filter to be 'all' or 'ksta'" 1>&2
      exit 1
   fi
fi

LANGUAGE=de
LANGUAGE_STREAM=$LANGUAGE-ch
case $ORDER in
	pub)
		TITLE_FEED="RSS Feed ZHLex (nach Publikationsdatum)"
		URL_FEED="http://lexspider-zhlex-data.eurospider.com/zhlex-pub.rss"
		DESCRIPTION_FEED="Feed Gesetze ZHLex (nach Publikationsdatum)"
		;;
	ikt)
		TITLE_FEED="RSS Feed ZHLex (nach IKT)"
		URL_FEED="http://lexspider-zhlex-data.eurospider.com/zhlex-ikt.rss"
		DESCRIPTION_FEED="Feed Gesetze ZHLex (nach IKT)"
		;;
esac

# where to find things
scriptdir="$base/scripts"
datadir="$base/data/azaref_index"
htdocsdir="$base/htdocs"
htmldir="$htdocsdir/html"
pdfdir="$htdocsdir/pdf"
metaxml="$htdocsdir/meta.xml"

lastBuildDate=`date --rfc-2822`

cat <<EOF
<?xml version="1.0" ?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
  <channel>
    <atom:link href="$URL_FEED" rel="self" type="application/rss+xml"/>
    <title>$TITLE_FEED</title>
    <lastBuildDate>${lastBuildDate}</lastBuildDate>
    <description>$DESCRIPTION_FEED</description>
    <link>https://www.zh.ch/de/politik-staat/gesetze-beschluesse/gesetzessammlung.html</link>
    <language>$LANGUAGE_STREAM</language>
EOF

ids=`xmlstarlet sel -t -m '/sendung/erlasse/erlass/nummern/syst' -v . -n $metaxml | sort`

for id in $ids; do
	title=`xmlstarlet sel -t -m "/sendung/erlasse/erlass/nummern/syst[text()='$id']" -v '../../bezeichnungen/titelVoll' -n $metaxml | sed 's/&amp;#8211;/-/g'`
	url=`xmlstarlet sel -t -m "/sendung/erlasse/erlass/nummern/syst[text()='$id']" -v '../../fassungen/fassung/publikationen/publikation' -n $metaxml | sed 's/&amp;/&#x26;/g'`
	pubDate=`xmlstarlet sel -t -m "/sendung/erlasse/erlass/nummern/syst[text()='$id']" -v '../../daten/letzteAenderung' -n $metaxml`
	iktDate=`xmlstarlet sel -t -m "/sendung/erlasse/erlass/nummern/syst[text()='$id']" -v '../../daten/iktErlass' -n $metaxml`
	description="IKT: $iktDate, Systematik: $id, $title"
	pubDate2822=`date -d"$pubDate" --rfc-2822`
	iktDate2822=`date -d"$iktDate" --rfc-2822`
	
	first=$(echo $id | cut -f 1 -d .)
	if test "$FILTER" = "ksta" -a $first -lt 631 -o $first -gt 673; then
		continue
	fi
	#echo ">> $id -> $first" 1>&2
	
	case "$ORDER" in
		pub)
			streamDate2822="$pubDate2822"
			;;
		ikt)
			streamDate2822="$iktDate2822"
			;;
	esac

	cat <<EOF
    <item>
      <title>$title</title>
      <link>$url</link>
      <guid>$url</guid>
      <pubDate>$streamDate2822</pubDate>
      <description>$description</description>
    </item>
EOF
done

cat <<EOF
  </channel>
</rss>
EOF

