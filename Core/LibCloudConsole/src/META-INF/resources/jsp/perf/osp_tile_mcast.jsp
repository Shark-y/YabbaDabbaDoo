<%@page import="com.cloud.core.net.NetMulticaster"%>

		<!-- <h3>Multicast Tunnel</h3> -->
			&nbsp;
				<div class="row">
					<div class="col-lg-3 col-md-6 col-sm-6 col-xs-12">
			            <div class="info-tile info-tile-alt tile-primary">
			                <div class="info">
			                    <div class="tile-heading"><span>Packets Received</span></div>
			                    <div class="tile-body"><span id="packetsReceived">0</span></div>
			                </div>
			                <div class="stats">
			                    <div class="tile-heading"><span>KBytes Received</span></div>
			                    <div class="tile-body"><span id="bytesReceived">0</span></div>
			                </div>
			            </div>
			        </div>
					<div class="col-lg-3 col-md-6 col-sm-6 col-xs-12">
			            <div class="info-tile info-tile-alt tile-indigo">
			                <div class="info">
			                    <div class="tile-heading"><span>Packets Sent</span></div>
			                    <div class="tile-body"><span id="packetsSent">0</span></div>
			                </div>
			                <div class="stats">
			                    <div class="tile-heading"><span>KBytes Sent</span></div>
			                    <div class="tile-body"><span id="bytesSent">0</span></div>
			                </div>
			            </div>
			        </div>
					<div class="col-lg-3 col-md-6 col-sm-6 col-xs-12">
			            <div id="mcastStatus" class="info-tile info-tile-alt tile-danger">
			                <div class="info">
			                    <div class="tile-heading"><span>Status</span></div>
			                    <div class="tile-body "><span id="running">OFF</span></div>
			                </div>
			                <div class="stats">
			                    <div class="tile-content">
			                    	<a id="mctLink" href="#" onclick="return tunnelDo()">START</a>
			                    </div>
			                </div>
			            </div>
			        </div>
			    </div>
			    <!-- row -->
			    
				<div class="form-horizontal">
					<div class="form-group">
						<label class="col-sm-2 control-label">Multicast Address/Port</label>
						<div class="col-sm-2"><%=NetMulticaster.DEFAULT_ADDR%>/<%=NetMulticaster.DEFAULT_PORT%></div>
						<label class="col-sm-2 control-label">Local Server Port</label>
						<div class="col-sm-2"><input name="serverport" id="serverport" type="text" class="form-control" value="9876" required="required" title="Local server port"></div>
					</div>
					<div class="form-group">
						<label class="col-sm-2 control-label">Remote Host</label>
						<div class="col-sm-2"><input name="remotehost" id="remotehost" type="text" class="form-control" required="required" title="Remote endpoint IP addess or host name" ></div>
						<label class="col-sm-2 control-label">Remote Port</label>
						<div class="col-sm-2"><input name="remoteport" id="remoteport" type="text" class="form-control" value="9876" required="required" title="Remote endpoint port"></div>
					</div>
				</div>			
				
				<!-- 
				<div class="row">
					<div class="col-sm-8 col-sm-offset-2">
						<button class="btn-raised btn-primary btn">Start</button>
					</div>				
				</div>
				-->
<script type="text/javascript">

function pollMCastTunnel() {
	$.get( pollEndPoint + '?op=mct-status', function( json ) {
		// {"message":"OK","status":200,"tunnel":{"bytesSent":0,"packetsSent":0,"packetsReceived":0,"bytesReceived":0,"running":false}}
		LOGD("Poll mcast tunnel got " + JSON.stringify(json));
		if ( ! json.tunnel) {
			return;
		}
		$('#bytesSent').html((json.tunnel.bytesSent/1024).toFixed(1)); 		
		$('#packetsSent').html(json.tunnel.packetsSent);
		$('#bytesReceived').html((json.tunnel.bytesReceived/1024).toFixed(1)); 
		$('#packetsReceived').html(json.tunnel.packetsReceived);
		
		var running 	= json.tunnel.running ? 'ON' : 'OFF';
		var lbl			= json.tunnel.running ? 'STOP' : 'START';
		
		$('#running').html(running);
		$('#mctLink').html(lbl);
		
		if ( json.tunnel.running) {
			$('#mcastStatus').removeClass("tile-danger");
			$('#mcastStatus').addClass("tile-success");
		}
		else {
			$('#mcastStatus').removeClass("tile-success");
			$('#mcastStatus').addClass("tile-danger");
		}
	});
}

function tunnelDo () {
	// START, STOP
	var serverport = $('#serverport').val();
	var remotehost = $("#remotehost").val();
	var remoteport = $("#remoteport").val();
	
	if ( serverport == '' || remotehost == '' || remoteport == '') {
		notify('All fields are required.', 'danger');
		return false;
	}
	var data = { "action": $('#mctLink').html(), "serverport": serverport , "remotehost" : remotehost, "remoteport" : remoteport } ;
	LOGD('Tunnel: ' + JSON.stringify(data));

	notify('Please wait...', 'info');
	
	$.post( pollEndPoint + '?op=tunnel', data);
	return false;
}

</script>				