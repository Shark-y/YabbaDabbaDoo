/*
 * CREATE TABLE VDN (VDN VARCHAR(12) NOT NULL
	, NAME VARCHAR(64) NOT NULL
	, CALLS_WAITNG INT
	, OLDESTCALL VARCHAR(16) 
	, AVG_ANSWER_SPEED VARCHAR(16)
	, ABNCALLS INT
	, AVG_ABANDON_TIME VARCHAR(16)
	, ACDCALLS INT
	, AVG_ACD_TALK_TIME VARCHAR(16)
	, ACTIVECALLS INT 
);
 */
insert into vdn values('1000', 'vdn1000', 5 , '1:0:0','10', 2, '20', 8, '30', 8);
insert into vdn values('1001', 'vdn1001', 13 , '2:0:0','8', 3, '30', 23, '40', 13);
insert into vdn values('1002', 'vdn1002', 2 , '1:3:0','1', 12, '210', 12, '10', 2);
insert into vdn values('1003', 'vdn1003', 15 , '1:0:4','40', 8, '90', 2, '20', 1);

