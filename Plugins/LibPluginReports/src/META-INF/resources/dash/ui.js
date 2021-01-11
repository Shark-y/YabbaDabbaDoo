/**
 * UI Helper functions used by dash & thresh JSPs
 */

/**
 * Delete a row in a  table.
 * @param row HTMLRow obj to delete
 */
function tableDelRow(row) {
	while ( row.parentNode && row.tagName.toLowerCase() != 'tr') {
		row = row.parentNode;
	}
	if ( row.parentNode && row.parentNode.rows.length > 1) {
		row.parentNode.removeChild(row);
	}
}

/**
 * Remove all rows in a HTML TABLE.
 * @param table HTML Table object.
 */
function tableClean (table) {
	var rowCount 	= table.rows.length;
	
	// don't delete the 1st row (headings)
	for ( var i = 1 ; i < rowCount ; i++ ) {
		tableDelRow (table.rows[1]);
	}
}

/**
 * Delete a table row by searching an object id in the cellIdx cell.
 * @param tableId Id of the HTML table to search
 * @param cellIdx Cell number to search for each row
 * @param key Search key
 */
function tableDelRowByKey (tableId, cellIdx, key) {
	var table	= getElem (tableId); 

	for ( var i = 1 ; i < table.rows.length ; i ++ ) {
		var cells = table.rows[i].cells;
		if ( cells[cellIdx].innerHTML.indexOf(key) != -1 ) {
			tableDelRow(table.rows[i]);
		}
	}
}

/**
 * Get an DOM object by it's Id.
 * @param id ID of the DOM obj.
 * @returns DOM object.
 */
function getElem (id) {
	return document.getElementById(id);
}

/**
 * Clear all OPTIONS in an HTML SELECT.
 * @param selectbox SELCT object.
 */
function comboClear(selectbox) {
    for ( var i = selectbox.options.length - 1 ; i >=0 ; i--) {
        selectbox.remove(i);
    }
}

/**
 * Add options to a comobo from a CSV string: v1,v2,...v(n)
 * @param combo HTML select obj
 * @param csvList Select Values: v1,v2,...v(n)
 * @param selName selected OPTION name from csvList
 */
function comboAddOptions (combo, csvList, selName) {
	var fields 	= csvList.split(",");
	for ( var i = 0 ; i < fields.length; i++) {
		var option 		= document.createElement("option");
		option.text 	= fields[i];
		option.value 	= fields[i];
		option.selected	= (selName && fields[i] == selName) ? true : false;
		combo.add(option);
	}
}

/**
 * Get the selected value of an HTML SELECT (combo)
 * @param id Id of the DOM SELECT object.
 * @returns
 */
function comboGetValue (id) {
	var cmb = getElem (id);
	return cmb.options[cmb.selectedIndex].value;
}

/**
 * Check if elem is an alpha string
 * @param elem Example: 'Hello World' OK 123 (null)
 * @returns An Array, containing the matches, one item for each match, or null if no match is found
 */
function isAlphabetic(elem) {
	return elem.match(/^[a-zA-Z ]+$/g);
}

/**
 * Check is value is in range.
 * <pre>RANGE TESTER
 * var range= '1-5,12,15';
 * console.log("t1=" + isInRange(range, 8) );
 * console.log("t1=" + isInRange(range, 1) );
 * console.log("t1=" + isInRange(range, 15) ); </pre>
 * @param range A range 12-,5,8,10,11
 * @param val Value to check
 */
function isInRange(range, val) {
	var array = range.split(',');
	/* 5/1/2019
	if ( typeof(val) != 'number') 
		val = parseInt(val);
	*/
	for ( var i = 0 ; i < array.length ; i++) {
		var elem = array[i];	// always of type 'string'
		//LOGD('Elem: ' + elem + ' type:' + typeof(elem));
		
		if ( isAlphabetic(elem) ) {
			//LOGD('Found string: ' + elem + ' == ' + val + "?");
			if ( elem == val ) {
				return true;
			}
			else {
				continue;
			}
		}
		// convert key val to number for comparison
		if ( typeof(val) != 'number') {
			val = parseInt(val);
		}
		if ( elem.indexOf('-') != -1) {
			var min = parseInt(elem.split('-')[0]);
			var max = parseInt(elem.split('-')[1]);
			if ( val >= min && val <= max) {
				return true;
			}
		}
		else {
			if ( val == parseInt(elem)) {
				return true;
			}
		}
	}
	return false;
}

