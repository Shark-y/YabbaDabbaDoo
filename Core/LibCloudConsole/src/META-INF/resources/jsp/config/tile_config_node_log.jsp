<%@page import="com.cloud.core.cron.LogCleanerService"%>
<%@page import="com.cloud.core.logging.Container"%>
<%@page import="com.cloud.console.HTTPServerTools"%>
<%@page import="com.cloud.console.HTMLConsoleLogUtil"%>
<%@page import="com.cloud.core.io.IOTools"%>
<%@page import="com.cloud.core.services.CloudServices"%>
<%@page import="com.cloud.core.services.NodeConfiguration"%>
<%@page import="com.cloud.core.logging.Auditor"%>
<%
	// get the server config. NOTE: The login page will change the cfg @ boot time!
	NodeConfiguration cfgServer 	= CloudServices.getNodeConfig(); 
	
	//defaut container log
	final String defLogFolder		= Container.getDefautContainerLogFolder();		
	
	//  get the log rotation pol (or default twice a day)
	final String logRotationPolicy = cfgServer.getProperty(NodeConfiguration.KEY_LOG_ROTATION_POL, HTMLConsoleLogUtil.ROLLOVER_TWICEADAY);

	// 2/2/19 log cleaner policy: defaults to remove older than 2 weeks
	final String logCleanerPolicy 	= cfgServer.getProperty(NodeConfiguration.KEY_LOG_CLEANER_POL, LogCleanerService.CleanPolicy.REMOVE_OLDER_THAN_2WEEKS.name());

%>
			<div class="panel panel-default card md-card" data-widget='{"draggable": "false"}'>
				<div class="panel-heading card-header md-card-toolbar">
					<h2 class="panel-title md-card-toolbar-heading-text" data-toggle="tooltip" data-placement="top" title="">Log System</h2>
					<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
				</div>
				<div class="panel-body card-body md-card-content">
					<!-- folder -->
					<div class="form-group uk-form-row uk-grid uk-width-1-1">
						<label class="col-sm-2 control-label uk-width-1-4">Folder</label>
						<div class="col-sm-10 uk-width-3-4">
							<input id="<%=NodeConfiguration.KEY_LOG_PATH%>" type="text" class="form-control md-input" autocomplete="off" data-toggle="tooltip"
								name="<%=NodeConfiguration.KEY_LOG_PATH%>" value="<%=cfgServer.getLogFolder().startsWith("logs") ? defLogFolder : cfgServer.getLogFolder()%>"
								title="Folder where the log files will be saved.">
						</div>
					</div>
					<!-- rotation policy -->
					<div class="form-group uk-form-row uk-grid uk-width-1-1">
						<label class="col-sm-2 control-label uk-width-1-4">Rotation Policy</label>
						<div class="col-sm-10 uk-width-3-4">
							<select	id="<%=NodeConfiguration.KEY_LOG_ROTATION_POL%>" name="<%=NodeConfiguration.KEY_LOG_ROTATION_POL%>" data-toggle="tooltip"
								title="Log rotation policy." class="form-control" data-md-selectize>
								<!-- 
								<option value="<%=HTMLConsoleLogUtil.ROLLOVER_HOURLY%>" <%=logRotationPolicy.equals(HTMLConsoleLogUtil.ROLLOVER_HOURLY) ? "selected" : "" %>>Hourly</option>
								<option value="<%=HTMLConsoleLogUtil.ROLLOVER_TWICEADAY%>" <%=logRotationPolicy.equals(HTMLConsoleLogUtil.ROLLOVER_TWICEADAY) ? "selected" : "" %>>Twice a day</option>
								<option value="<%=HTMLConsoleLogUtil.ROLLOVER_WEEKLY%>" <%=logRotationPolicy.equals(HTMLConsoleLogUtil.ROLLOVER_WEEKLY) ? "selected" : "" %>>Once a week</option>
								<option value="10MB" <%=logRotationPolicy.equals("10MB") ? "selected" : "" %>>By size: 10Mb</option>
								<option value="50MB" <%=logRotationPolicy.equals("50MB") ? "selected" : "" %>>By size: 50Mb</option>
								<option value="100MB" <%=logRotationPolicy.equals("100MB") ? "selected" : "" %>>By size: 100Mb</option>
								-->
								<option value="<%=HTMLConsoleLogUtil.ROLLING_ONCEADAY%>" <%=logRotationPolicy.equals(HTMLConsoleLogUtil.ROLLING_ONCEADAY) ? "selected" : "" %>>Once a day</option>
								<option value="<%=HTMLConsoleLogUtil.ROLLING_WEEKLY%>" <%=logRotationPolicy.equals(HTMLConsoleLogUtil.ROLLING_WEEKLY) ? "selected" : "" %>>Once a week</option>
								<option value="<%=HTMLConsoleLogUtil.ROLLING_ZIPPEDWEEKLY%>" <%=logRotationPolicy.equals(HTMLConsoleLogUtil.ROLLING_ZIPPEDWEEKLY) ? "selected" : "" %>>Once a week (Zip Compressed)</option>
								<option value="10485760" <%=logRotationPolicy.equals("10485760") ? "selected" : "" %>>By size: 10Mb</option>
								<option value="52428800" <%=logRotationPolicy.equals("52428800") ? "selected" : "" %>>By size: 50Mb</option>
								<option value="104857600" <%=logRotationPolicy.equals("104857600") ? "selected" : "" %>>By size: 100Mb</option>
								<option value="524288000" <%=logRotationPolicy.equals("524288000") ? "selected" : "" %>>By size: 500Mb</option>
							</select>
						</div>
					</div>
					
					<div class="form-group uk-form-row uk-grid uk-width-1-1">
						<label class="col-sm-2 control-label uk-width-1-4">Mask RegExp <a href="#" onclick="return helpMask()">?</a></label>
						<div class="col-sm-10 uk-width-3-4">
							<input id="<%=NodeConfiguration.KEY_LOG_MASK_REGEXP%>" type="text" class="form-control md-input" autocomplete="off" data-toggle="tooltip"
								name="<%=NodeConfiguration.KEY_LOG_MASK_REGEXP%>" value="<%=cfgServer.getLogMaskRegExp() != null ? cfgServer.getLogMaskRegExp() : ""%>"
								title="Regular expression used to mask sensitive information from log messages." 
								placeholder="A regular expression for example (META_DNIS.)[0-9]{1,}">
						</div>
					</div>
					
					<div class="form-group uk-form-row uk-grid uk-width-1-1">
						<label class="col-sm-2 control-label uk-width-1-4">Message Mask</label>
						<div class="col-sm-10 uk-width-3-4">
							<input id="<%=NodeConfiguration.KEY_LOG_MASK%>" type="text" class="form-control md-input" autocomplete="off" data-toggle="tooltip"
								name="<%=NodeConfiguration.KEY_LOG_MASK%>" value="<%=cfgServer.getLogMask() != null ? cfgServer.getLogMask() : ""%>"
								title="Replacement mask used when matches are found." placeholder="For exmaple: $1xxx">
						</div>
					</div>
					<!-- 2/2/2019 Log cleaner -->
					<div class="form-group uk-form-row uk-grid uk-width-1-1">
						<label class="col-sm-2 control-label uk-width-1-4">Log Cleaner</label>
						<div class="col-sm-10 uk-width-3-4">
							<select	id="<%=NodeConfiguration.KEY_LOG_CLEANER_POL%>" name="<%=NodeConfiguration.KEY_LOG_CLEANER_POL%>" data-toggle="tooltip"
								title="Log Cleaner policy." class="form-control" data-md-selectize>
								<option value="<%=LogCleanerService.CleanPolicy.REMOVE_OLDER_THAN_2WEEKS%>" <%=logCleanerPolicy.equals(LogCleanerService.CleanPolicy.REMOVE_OLDER_THAN_2WEEKS.name()) ? "selected" : "" %>>Remove older than 2 weeks</option>
								<option value="<%=LogCleanerService.CleanPolicy.REMOVE_OLDER_THAN_1MONTH%>" <%=logCleanerPolicy.equals(LogCleanerService.CleanPolicy.REMOVE_OLDER_THAN_1MONTH.name()) ? "selected" : "" %>>Remove older than 1 month</option>
								<option value="<%=LogCleanerService.CleanPolicy.REMOVE_BIGGER_THAN_200MB%>" <%=logCleanerPolicy.equals(LogCleanerService.CleanPolicy.REMOVE_BIGGER_THAN_200MB.name()) ? "selected" : "" %>>Remove bigger than 200MB</option>
								<option value="<%=LogCleanerService.CleanPolicy.REMOVE_BIGGER_THAN_500MB%>" <%=logCleanerPolicy.equals(LogCleanerService.CleanPolicy.REMOVE_BIGGER_THAN_500MB.name()) ? "selected" : "" %>>Remove bigger than 500MB</option>
								<option value="<%=LogCleanerService.CleanPolicy.DISABLED%>" <%=logCleanerPolicy.equals(LogCleanerService.CleanPolicy.DISABLED.name()) ? "selected" : "" %>>Disabled</option>
							</select>
						</div>
					</div>
					
				</div>
			</div>

			<!--  mask sensitive info from log messages -->
			<script>
			function helpMask () {
				getObject('<%=NodeConfiguration.KEY_LOG_MASK_REGEXP%>').value = '(META_DNIS.)[0-9]{1,}';
				getObject('<%=NodeConfiguration.KEY_LOG_MASK%>').value = '$1x';
				notify('Mask DNIS numbers.', 'info');
				return false;
			}
			</script>
			
