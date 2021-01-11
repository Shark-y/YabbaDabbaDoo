/**
 * Helper functions for ds.jsp 
 */

/**
 * Set obj visibility
 */
function setVisibility(id, visible, display) {
	var obj = document.getElementById(id);
	if ( !obj ) 
		return;
	if ( visible ) 
		obj.style.display = (display ? display : 'block');
	else 
		obj.style.display = 'none';
}


/**
 * Fires when the data source type combo changes
 */
function ds_type_change() {
	var cmb 	= document.getElementById('ds-type');
	if ( !cmb ) {
		LOGW('DataSourceType OnChange: Invalid COMBO (ds-type)');
		return;
	}
	var proto	= cmb.options[cmb.selectedIndex].value;
	var len		= 10;
	
	LOGD('DS type change proto: [' + proto + ']');
	
	// show elements based on the DS type
	for ( var i = 1 ; i <= len ; i++) {
		setVisibility('ssr' + i, proto == 'SOCKET' || proto == 'SOCKET_WITH_STORAGE');
		setVisibility('dbr' + i, proto == 'DATABASE' || proto == 'DATASTORE');
		setVisibility('smtpr' + i, proto.indexOf('SMTP') != -1 || proto == 'POP3' || proto.indexOf('IMAP') != -1);
		setVisibility('tw' + i, proto == 'SMS_TWILIO');
		setVisibility('fs' + i, proto == 'FILESYSTEM');
		setVisibility('pm' + i, proto == 'PROMETHEUS');
	}
	if ( proto == 'DATASTORE') { 
		// 6/11/2019 Don't hide DS-type setVisibility('rowDs', false)	// DS type
		setVisibility('dbr7', false);	// hide poll in (db)
	}
	// storage fileds/options 10/3/2017
	if ( proto == 'SOCKET_WITH_STORAGE') { 
		setVisibility('ssr6', true);	// storage fields
		setVisibility('ssr7', true);	// storage options
	}
	else  {
		setVisibility('ssr6', false);
		setVisibility('ssr7', false);
	}
	setVisibility('smtpr5', proto == 'SMTPS' || proto == 'POP3' || proto.indexOf('IMAP') != -1);	// SMTP user
	setVisibility('smtpr6', proto == 'SMTPS' || proto == 'POP3' || proto.indexOf('IMAP') != -1); 	// SMTP pwd
	setVisibility('ssr1', proto.indexOf('SOCKET') != -1 || proto.indexOf('SMTP') != -1 || proto == 'POP3' || proto.indexOf('IMAP') != -1); 	// Port
	setVisibility('smtpr3', proto == 'SMTP' || proto == 'SMTPS');								// SMTP(S) 'From'
	setVisibility('smtpr7', proto.indexOf('POP3') != -1  || proto.indexOf('IMAP') != -1 );		// Folder
	
	ds_set_def_port();
}

/**
 * Set the default DS port
 * See https://www.siteground.com/tutorials/email/protocols-pop3-smtp-imap/#What_is_POP3_and_which_are_the_default_POP3_ports
 */
function ds_set_def_port() {
	var cmb 	= document.getElementById('ds-type');
	var oPort	= document.getElementById('port');
	var port;
	if ( !cmb ) {
		LOGW('DataSourceType OnChange: Invalid COMBO (ds-type)');
		return;
	}
	var proto	= cmb.options[cmb.selectedIndex].value;
	switch ( proto ) {
		case 'SMTP':
			port = 25;
			break;
		case 'SMTPS':
			port = 465;
			break;
		case 'POP3':
			port = 110;
			break;
		case 'IMAP':
			port = 143;
			break;
		case 'IMAPS':
			port = 993;
			break;
	}
	if (port) {
		oPort.value = port;
	}
}
