/**
 * 
 */

// level: info, danger, warning, success
function growl ( text, level, delay) {
	level = level || 'info';
	delay = delay || 30000;
	$.growl({ message : text }, {type : level, placement : {from : 'top', align : 'right'}, delay : delay, offset : {x : 20, y : 85} } );
}

