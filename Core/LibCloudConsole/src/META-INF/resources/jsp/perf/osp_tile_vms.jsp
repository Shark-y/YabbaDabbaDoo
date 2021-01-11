
	<div id="modal2" class="modal fade" tabindex="-1" role="dialog">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-body">
						<h3>Select a Virtual Machine</h3>
						
						<table id="tblVMs" class="table" style="width: 100%">
							<thead>
								<tr>
									<th>Id</th>
									<th>Display Name</th>
									<th>Provider</th>
								</tr>
							</thead>
							<tbody></tbody>
						</table>
						
					</div>
					
					<div class="modal-footer">
						<button id="btnClodeModal2" type="button" class="btn btn-primary" data-dismiss="modal">Close</button>
					</div>
					
				</div>
			</div>
	</div>

<script type="text/javascript">

/**
 * Fetch VM indo
 * @param pid vm process id
 */
function fetchVM (pid) {
	// for OS metrics, will update in the next loop
	vmpid 	= pid;	
	
	// update threads url	
	var url = tableThreads.ajax.url();
	tableThreads.ajax.url(url.replace(/vmpid=.*/g, 'vmpid=' + pid)).load();
	
	LOGD('Fetch vm ' + vmpid + ' threads url=' + tableThreads.ajax.url());
	$('#btnClodeModal2').click();
	
	notify('Using VM with process id ' + pid, 'info');
	return false;
}

</script>