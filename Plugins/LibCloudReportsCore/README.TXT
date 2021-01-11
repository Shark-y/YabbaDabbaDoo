Cloud Real Time Statistic Service
---------------------------------
Overview
---------
This service provides TCP listener services to telephony real time statistics pumped by call managers such as:

* Avaya RT socket
* Avaya Wallboard
 * Any generic statistic pump client based on TCP sockets.

For example, a raw socket buffer provided by the Avaya RT socket VDN table (see the rt-socket tech description) such as:

F1 Record Type - CVDN Table
DATA ITEM		Field size	Type	Description
F1					2	string	Static “F1” string
VDN					7	number	VDN number
VDN					20	synonym	VDN synonym
INPROGRESS-ATAGENT	4	number	Calls waiting
OLDESTCALL			5	mm:ss	Oldest call
AVG_ANSWER_SPEED	5	mm:ss	Average answer speed
ABNCALLS			4	number	Abandoned calls
AVG_ABANDON_TIME	5	mm:ss	Average Abandon Time
ACDCALLS			4	number	ACD calls
AVG_ACD_TALK_TIME	5	mm:ss	Avg. ACD talk time
ACTIVECALLS			4	Number	Active calls

F1|  74500|Eastern Sales|   2|:00||   8|58:54|  46|23:37|  54 
F1|  78808|Eastern Sales|   3|:00||   6|40:58|  11|56: 1|  99 
F1|  35611|Eastern Sales|   1|:00||   9|12:51|  78|23:45|  99 
F1|   3503|Eastern Sales|   7|:00||   3|46:40|  97|50: 0|  59 
F1|  55400|Eastern Sales|   4|:00||   7|30:46|  27|46:43|  80 
F1|   5292|Eastern Sales|   5|:00||   5|13:38|  33| 3:39|  87 
F1|  48870|Eastern Sales|   1|:00||   5|20: 3|  66|24:30|  39 

Will produce the JSON object:
{"batchDate":1451855183491,"batchData":[{"F1":"F1","VDN":"74500","ACDCALLS":"46","ABNCALLS":"8",,,,}],...} 

TCP Listeners
-------------
<listeners>
<listener>
	<name>CSPLIT Table</name>
	<port>7000</port>
	<description>CSPLIT Table Listener</description>
	<format>
		<header></header>
		<footer>F3|END_OF_RECORDS</footer>
		<fieldSep>\|</fieldSep>
		<fields>F1,SPLIT,CALLS_WAITING,AVAILABLE,ANSTIME/ACDCALLS,ABNCALLS,ACD,OLDESTCALL,ACDCALLS,ACDTIME/ACDCALLS,ABNTIME/ABNCALLS,AGINRING,ONACD,INACW,OTHER,INAUX,STAFFED,EWTHIGH,EWTMEDIUM,EWTLOW,DA_INQUEUE+DA_INRING,100*(ACCEPTABLE/CALLSOFFERED),SERVICELEVEL,CALLSOFFERED</fields>
	</format>
</listener>
<listener>
	<name>CVDN Table</name>
	<port>7001</port>
	<description>CVDN Table Listener</description>
	<format>
		<header></header>
		<footer>F3|END_OF_RECORDS</footer>
		<fieldSep>\|</fieldSep>
		<fields>F1,VDN,VDN SYN,INPROGRESS-ATAGENT,OLDESTCALL,AVG_ANSWER_SPEED,ABNCALLS,AVG_ABANDON_TIME,ACDCALLS,AVG_ACD_TALK_TIME,ACTIVECALLS</fields>
	</format>
</listener>
</listeners>

Dashboard API
-------------
<dashboards>
<dashboard>
	<listener>CVDN Table</listener>
	<key>VDN</key>
	<metric>
		<name>INPROGRESS-ATAGENT</name>
		<description>Calls Waiting</description>
		<type>number</type>
		<widget>gauge</widget>
	</metric>
	<metric>
		<name>ACTIVECALLS</name>
		<description>Active Calls</description>
		<type>number</type>		
		<widget>gauge</widget>
	</metric>
</dashboard>
</dashboards>
