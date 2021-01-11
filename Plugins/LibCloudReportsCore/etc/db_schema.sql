/**
 * Schema used for DB data source testing
 */

/* clean up */
DROP TABLE VDN;
DROP TABLE SPLIT;

/*
 * 	Avaya VDN Table
	================
	DATA ITEM	Field size	Type	Description
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
 */
CREATE TABLE VDN (VDN VARCHAR(12) NOT NULL
	, VDN_SYN VARCHAR(64) NOT NULL
	, CALLS_WAITING INT
	, OLDESTCALL VARCHAR(16) 
	, AVG_ANSWER_SPEED VARCHAR(16)
	, ABNCALLS INT
	, AVG_ABANDON_TIME VARCHAR(16)
	, ACDCALLS INT
	, AVG_ACD_TALK_TIME VARCHAR(16)
	, ACTIVECALLS INT
	, PRIMARY KEY(VDN)
);

CREATE TABLE SPLIT (SPLIT VARCHAR(12) NOT NULL
	, NAME VARCHAR(64) NOT NULL
	, CALLS_WAITNG INT
	, ACDCALLS INT
	, ACTIVECALLS INT 
	, PRIMARY KEY(SPLIT)
);
