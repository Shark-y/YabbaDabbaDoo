/**
 * Cookie subroutines - https://www.w3schools.com/js/js_cookies.asp
 */

/**
 * A Function to Set a Cookie
 * The parameters of the function above are the name of the cookie (cname), the value of the cookie (cvalue), and the
 * number of days until the cookie should expire (exdays).
 * The function sets a cookie by adding together the cookiename, the cookie value, and the expires string.
 */
function setCookie(cname, cvalue, exdays) {
	exdays 	= exdays || 1;
	var d 	= new Date();
	if ( !cvalue ) { 
		console.log("SET-Cookie: Invalid value for " + cname);
		return;
	}
	d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
	var expires = "expires=" + d.toUTCString();
	document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
}

/**
 * A Function to Get a Cookie. If the cookie is not found, return "".
 * @param cname Cookie name
 * @returns If the cookie is found (c.indexOf(name) == 0), return the value of the cookie (c.substring(name.length, c.length).
 */
function getCookie(cname) {
	var name = cname + "=";
	var ca = document.cookie.split(';');
	for (var i = 0; i < ca.length; i++) {
		var c = ca[i];
		while (c.charAt(0) == ' ') {
			c = c.substring(1);
		}
		if (c.indexOf(name) == 0) {
			return c.substring(name.length, c.length);
		}
	}
	return "";
}