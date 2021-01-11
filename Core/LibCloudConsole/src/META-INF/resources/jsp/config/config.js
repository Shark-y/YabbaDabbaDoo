/**
 * Some utility functions
 * @param id
 * @returns
 */
function getWidget(id) {
	return window.document.getElementById(id);
}

function uiHide(id) {
	var obj = getWidget(id);
	if ( ! obj) {
		LOGE("UIHide: Unable to find object  w/ id " + id);
		return;
	}
	getWidget(id).style.display = 'none';
}
function uiShow(id) {
	getWidget(id).style.display = 'block';
}


/**
 * Set the focus of a widget
 * @param id Id of the HTML widget
 */
function uiSetFocus(id) {
	var widget = getWidget(id);
	if ( widget) {
		widget.focus();
	}
}


/**
 * Moving options from/to listboxes 
 */
function move(tbFrom, tbTo, flag) {
	var arrFrom = new Array();
	var arrTo = new Array();
	var arrLU = new Array();
	var i;
	for (i = 0; i < tbTo.options.length; i++) {
		arrLU[tbTo.options[i].text] = tbTo.options[i].value;
		arrTo[i] = tbTo.options[i].text;
	}
	var fLength = 0;
	var tLength = arrTo.length;
	for (i = 0; i < tbFrom.options.length; i++) {
		arrLU[tbFrom.options[i].text] = tbFrom.options[i].value;
		if (tbFrom.options[i].selected && tbFrom.options[i].value != "") {
			arrTo[tLength] = tbFrom.options[i].text;
			tLength++;

			var name = tbFrom.options[i].value;
		} else {
			arrFrom[fLength] = tbFrom.options[i].text;
			fLength++;
		}
	}

	tbFrom.length = 0;
	tbTo.length = 0;
	var ii;

	for (ii = 0; ii < arrFrom.length; ii++) {
		var no = new Option();
		no.value = arrLU[arrFrom[ii]];
		no.text = arrFrom[ii];
		tbFrom[ii] = no;
	}

	for (ii = 0; ii < arrTo.length; ii++) {
		var no = new Option();
		no.value = arrLU[arrTo[ii]];
		no.text = arrTo[ii];
		tbTo[ii] = no;
	}
}

//adding a new customization option
function addField(fromLB, fieldName, fieldValue)
{
	//Retrieve elements from document body
	var name = fieldName.value;
	name = name.replace(/\s+/g, '');
	var value = fieldValue.value;
	
	//format new attribute (name:value)
	var attribute = name + ":" + value;
	
	//Create new option tag
	var newOption = document.createElement('option');
	newOption.value = attribute;
	newOption.innerHTML = attribute;
	
	//clear input fields
	fieldName.value = "";
	fieldValue.value = "";
	
	fromLB.appendChild(newOption);
}

/**
 * Select a value within a SELECT widget
 * @param objName Name of the widget
 * @param val Value to select in the widget
 */
function uiSetSelect(objName, val) 
{
	var obj = getWidget(objName);
	if ( ! obj ) return;
	
	var opts = obj.options;
	if ( ! opts ) return;
	
	for ( var i = 0 ; i < opts.length ; i++ ) {
		if ( opts[i].value.toLowerCase() == val.toLowerCase() ) {
			opts[i].selected = "selected";
		}
	}
}

/**
 * Check or uncheck an INPUT checkbox
 * @param name Widget id
 * @param val either 'true' or 'false'
 */
function uiSetCheckBox(name, val) 
{
	var obj = getWidget(name);
	obj.checked  = ( val.toLowerCase() == 'true' ? "checked" : "");
}

/**
 * Set values of a multiple SELECT widget
 * @param objName Widget id
 * @param vals Multi values string: 'val1,val2,....valn'
 */
function uiSetMultiValues(objName, vals) 
{
	var obj = getWidget(objName);
	var values = vals.split(',');
	
	for ( var i = 0 ; i < values.length ; i++ ) {
		if ( values[i] == "") {
			continue;
		}
		obj.options[i] = new Option();
		obj.options[i].value = values[i];
		obj.options[i].innerHTML = values[i];
	}
}

/**
 * Append an OPTION to a SELECT alement
 * @param selId Id of the SELECT
 * @param val Value
 * @param html Inner HTML
 */
function selectAddOption( selId, val, html)
{
	var oSelect	= getWidget(selId);
	var i  		= oSelect.options.length;
	
	if ( !val || val.length == 0) {
		LOGW("SELECT AddOption: Can't add empty values.");
		return;
	}
	oSelect.options[i] = new Option();
	oSelect.options[i].value = val;
	oSelect.options[i].innerHTML = html;
}

/**
 * Add a value from an INPUT element to a SELECT element
 * @param txtId Id of the INPUT
 * @param lbId Id of the SELECT
 */
function listBoxAdd( txtId, lbId)
{
	var oTxt 	= getWidget(txtId);
	var oSelect	= getWidget(lbId);
	
	if ( !oTxt || !oSelect) {
		LOGE("ListBoxAdd: Invalid TEXT or SELECT obj ids.");
		return;
	}
	var val = oTxt.value;
	selectAddOption(lbId, val, val);
}

function listBoxDel( selId)
{
	var oSelect	= getWidget(selId);
	if ( !oSelect || oSelect.selectedIndex < 0) {
		return;
	}
	oSelect.remove(oSelect.selectedIndex);
}

/**
 * Select all values on a multi SELECT. This is required so the OPTIONS will be sent to the server.
 * @param objName HTML SELECT id.
 */
function uiSelectMultiValues(objName) 
{
	var obj = getWidget(objName);
	var opts = obj.options;
	
	// if the are no options (user may have removed them). Add an empty (dummy) OPTION so the server 
	// will receive the empty update
	if ( opts.length == 0) {
		var i  			= 0;
		obj.options[i] 	= new Option();
		obj.options[i].value 	= '';
		obj.options[i].text 	= '';
		obj.options[i].selected = "selected";
		return;
	}
	for ( var i = 0 ; i < opts.length ; i++ ) {
		opts[i].selected = "selected";
	}
}

/**
 * Remove the selected element from the Available Attributes section of a MILTI select widget.
 * @param id Id of the HTML SELECT
 * @param label Name of the configuration item being processed
 */
function uiRemoveAvailableAttrib (id, label)  {
	var obj = getWidget(id);
	LOGD('uiRemoveAvailableAttrib Id:' + id + ' Selected Idx: ' + obj.selectedIndex);
	
	if ( !obj || obj.options.length == 0 ) 
		return;
	
	if ( obj.selectedIndex < 0 ) {
		alert ("Select an available attribute" + (label.length > 0 ? ' for ' + label + '.' : '.'));
		return;
	}
	obj.remove(obj.selectedIndex);
}

/**
 * Move down available
 * @param id multivals_PLUGINk01_00
 * @param label Hub Search URLs
 * @returns
 */
function uiMoveDownAvailableAttrib(id, label)  {
	var text 	= $( "#" + id + " option:selected" ).text();	// KEY:VALUE (value may have : )
	var key 	= id.replace('multivals_', '');					// Widget key
	
	LOGD('Move down Id:' + id + ' Lbl:' + label + ' txt:' + text + ' Widget:' + key); 
	
	$('#' + key + '_fieldName').val(text.substring(0, text.indexOf(':'))); 					// key
	$('#' + key + '_fieldValue').val(text.substring(text.indexOf(':') + 1, text.length)); 	// val
	
	uiRemoveAvailableAttrib (id, label);		// clean available
}

/**
 * select multi SELECTs. This is required so the multi vals will be sent to the server.
 */
/* UNUSED
function uiSelectMultiWidgets()
{
	// screen pop
	uiSelectMultiValues('ui_ScreenPop_FromLB');
	uiSelectMultiValues('ui_ScreenPop_ToLB');
	
	// display fields
	uiSelectMultiValues('ui_DisplayFields_ToLB');
	uiSelectMultiValues('ui_DisplayFields_FromLB');

	// call log
	uiSelectMultiValues('ui_callLog_FromLB');
	uiSelectMultiValues('ui_callLog_ToLB');
}
*/

/**
 * Fail over validation helper.
 * @deprecated This sub is not used any more.
 * @param comboEnabledId Id of the Failover enabled combo
 * @param txtHostId Id of the Failover host INPUT
 * @param txtIntervalId Id of the failover interval INPUT (ms)
 * @param cmbIsPrimaryId Id of the run mode combo: PRIMARY, SECONDARY or CLUSTER
 */
function validateFailOver(/*cmbEnabledId,*/ txtHostId, txtIntervalId, cmbIsPrimaryId) {
	//var combo	= getWidget(cmbEnabledId);						// Enabled combo
	//var enabled	= combo.options[combo.selectedIndex].value;	// failover enabled (Y/N)
	var oTxt 	= getWidget(txtHostId);							// primary (failover) host widget
	var oTxtInt = getWidget(txtIntervalId);						// interval
	var oCmb	= getWidget(cmbIsPrimaryId);					// Run mode combo
	
	if ( !oTxt || !oTxtInt || !oCmb) {
		LOGE("Validate failover: Invalid arguments: failover host, interval Id or combo is primary.");
		return true;
	}
	var host	= oTxt.value;									// primary (failover) host name/ip/url
	var isPrim	= oCmb.options[oCmb.selectedIndex].value;		// Run mode: PRIMARY, SECONDARY, CLUSTER
	
	//alert(/*"FO Enabled=" + enabled +*/ " Prihost=" + host + " RunMode=" + isPrim);
	
	// fail over validations...
	
	// is run mode is primary then it is ok.
	if ( isPrim == 'PRIMARY' /*'true'*/ ) {
		return true;
	}
	if ( (!host || host.length == 0) && isPrim == 'SECONDARY' /*'false'*/) {
		alert("A primary host is required when failover is enabled and run mode is secondary.");
		oTxt.focus();
		return false;
	}
	return true; //false;
}

function config_upload_lic() 
{
	var frm = document.forms["frmConfig"];
	
	// update the encoding type & action!
	frm.enctype = "multipart/form-data";
	frm.action += "?action=upload_lic";
	// the from will be submitted here...
}


/**
 * URI: ping.jsp?type=1&amp;A=om_stats03.service.host&amp;B=om_stats032.service.hostB
 * Becomes:
 * ping.jsp?type=1&amp;A=hostA&amp;B=hostB
 */
function config_ping (uri, name, spec) 
{
	var temp = uri.split("?");
	var qryStr = temp.length > 1 ? temp[1] : "";	// query string
	temp = qryStr.split("&");
	
	// loop thru query string
	// replacing values w/ the real widget's
	for ( var i = 0 ; i < temp.length ; i++) {
		var keyVal 	= temp[i];
		var key 	= keyVal.split('=')[0];
		var val 	= keyVal.split('=')[1];
		var widget 	= getWidget(val);
		var realVal	= widget != null ? widget.value : null;
		//alert('wk=' + val + " w=" + widget + ' rv=' + realVal);
		if ( realVal) {
			uri = uri.replace(val, realVal);
		}
	}
	//alert("uri=" + uri)
	window.open (uri, name, spec);
}


/**
 * Flip the visibility of an HTML elemnt by type.
 * @param list list of element ids.
 * @param dispType Display type: table-row (for TR), block, etc...
 */
function flipVisibilityByType (list, dispType) {
	var ids = list.split(',');
	
	for ( var i = 0 ; i < ids.length ; i++ ) {
		var obj = document.getElementById(ids[i]);

		if ( obj ) {
			var disp = obj.style.display; 
			//alert(obj.style.width);
			if ( disp == dispType || disp == '') {
				obj.style.display = 'none';
			}
			else {
				obj.style.display = dispType;
			}
		}
	}
}

/**
 * 9/5/2020 Set modal status message
 * @param id Modal id.
 * @param text Message
 * @param color One of info, success, warn or danger
 */
function modalSetStatus (id, text, color, skin) {
	color 	= color || 'info'; 
	skin 	= skin || 'clouds'; 
	
	var html = '<div class="alert alert-dismissable alert-' + color + '">' + text + '<button type="button" class="close" data-dismiss="alert" aria-hidden="true">&times;</button></div>' ;
	if ( skin == 'altair') {
		html = '<div class="uk-alert alert-dismissable uk-alert-' + color + '" data-uk-alert>' + text + '<a href="#" class="uk-alert-close uk-close"></a></div>' ;
	}
	$('#' + id).html(html);
}

