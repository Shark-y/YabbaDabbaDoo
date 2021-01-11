/**
 * Polling logic for multiple service endpoints 
 * SEP = service-end-point
 * 1/19/2019 Obsolete/Unimplemented, may be useful in the future.
 */

function pollServices (urls) {
	for ( var i = 0 ; i < urls.length ; i++) {
		var url = urls[i];
		LOGD("Poll-Service " + url);
	} 
}