<div class="panel panel-default" data-widget='{"draggable": "false"}'>
		<div class="panel-heading">
			<h2>Basic Form Elements</h2>
			<div class="panel-ctrls" data-actions-container="" data-action-collapse='{"target": ".panel-body"}'></div>
		</div>
		<div class="panel-editbox" data-widget-controls=""></div>
		<div class="panel-body">
			<form action="" class="form-horizontal row-border">
				<div class="form-group">
					<label class="col-sm-2 control-label">Simplest Input</label>
					<div class="col-sm-8">
						<input type="text" class="form-control">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Password Field</label>
					<div class="col-sm-8">
						<input type="password" class="form-control">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Input with Placeholder</label>
					<div class="col-sm-8">
						<input type="text" class="form-control" placeholder="Placeholder">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Disabled Input</label>
					<div class="col-sm-8">
						<input type="text" class="form-control" placeholder="Disabled Input" disabled>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Read only field</label>
					<div class="col-sm-8">
						<input type="text" class="form-control" readonly="readonly" value="Read only text goes here">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">With pre-defined value</label>
					<div class="col-sm-8">
						<input type="text" class="form-control" value="http://">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">With max length</label>
					<div class="col-sm-8">
						<input type="text" class="form-control" maxlength="20" placeholder="max 20 characters here">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Popover Input</label>
					<div class="col-sm-8">
						<input type="text" class="form-control popovers" placeholder="Popover Input" data-trigger="hover" data-toggle="popover" data-content="And here's some amazing content. It's very engaging. right?" data-original-title="A Title">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Tooltip Input</label>
					<div class="col-sm-8">
						<input type="text" class="form-control tooltips" data-trigger="hover" data-original-title="Tooltip text goes here. Tooltip text goes here.">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Select Box</label>
					<div class="col-sm-8">
						<select class="form-control" id="source">
							<optgroup label="Alaskan/Hawaiian Time Zone">
								<option value="AK">Alaska</option>
								<option value="HI">Hawaii</option>
							</optgroup>
							<optgroup label="Pacific Time Zone">
								<option value="CA">California</option>
								<option value="NV">Nevada</option>
								<option value="OR">Oregon</option>
								<option value="WA">Washington</option>
							</optgroup>
							<optgroup label="Mountain Time Zone">
								<option value="AZ">Arizona</option>
								<option value="CO">Colorado</option>
								<option value="ID">Idaho</option>
								<option value="MT">Montana</option><option value="NE">Nebraska</option>
								<option value="NM">New Mexico</option>
								<option value="ND">North Dakota</option>
								<option value="UT">Utah</option>
								<option value="WY">Wyoming</option>
							</optgroup>
							<optgroup label="Central Time Zone">
								<option value="AL">Alabama</option>
								<option value="AR">Arkansas</option>
								<option value="IL">Illinois</option>
								<option value="IA">Iowa</option>
								<option value="KS">Kansas</option>
								<option value="KY">Kentucky</option>
								<option value="LA">Louisiana</option>
								<option value="MN">Minnesota</option>
								<option value="MS">Mississippi</option>
								<option value="MO">Missouri</option>
								<option value="OK">Oklahoma</option>
								<option value="SD">South Dakota</option>
								<option value="TX">Texas</option>
								<option value="TN">Tennessee</option>
								<option value="WI">Wisconsin</option>
							</optgroup>
							<optgroup label="Eastern Time Zone">
								<option value="CT">Connecticut</option>
								<option value="DE">Delaware</option>
								<option value="FL">Florida</option>
								<option value="GA">Georgia</option>
								<option value="IN">Indiana</option>
								<option value="ME">Maine</option>
								<option value="MD">Maryland</option>
								<option value="MA">Massachusetts</option>
								<option value="MI">Michigan</option>
								<option value="NH">New Hampshire</option><option value="NJ">New Jersey</option>
								<option value="NY">New York</option>
								<option value="NC">North Carolina</option>
								<option value="OH">Ohio</option>
								<option value="PA">Pennsylvania</option><option value="RI">Rhode Island</option><option value="SC">South Carolina</option>
								<option value="VT">Vermont</option><option value="VA">Virginia</option>
								<option value="WV">West Virginia</option>
							</optgroup>
						</select>
					</div>
				</div>

				<div class="form-group">
					<label class="col-sm-2 control-label">Styled Select Box</label>
					<div class="col-sm-8">
						<select class="select form-control" placeholder="This is a placeholder">
							<option selected>First option</option>
							<option>Second option</option>
							<option>And another one</option>
						</select>
					</div>
				</div>

				<div class="form-group">
					<label class="col-sm-2 control-label">Disabled Dropdown</label>
					<div class="col-sm-8">
						<select class="form-control" disabled placeholder="Disabled Dropdown">
							<option>Alaska</option>
							<option>Lorem ipsum dolor.</option>
							<option>Amet, impedit aperiam?</option>
							<option>Nemo, alias, quasi?</option>
							<option>Inventore, expedita.</option>
						</select>
					</div>
				</div>

				<div class="form-group">
					<label class="col-sm-2 control-label">Multi-select Box</label>
					<div class="col-sm-8">
						<select class="form-control" multiple>
							<option>Lorem ipsum dolor.</option>
							<option>Amet, impedit aperiam?</option>
							<option>Nemo, alias, quasi?</option>
							<option>Inventore, expedita.</option>
						</select>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Inline Radio</label>
					<div class="col-sm-8">
						<div class="radio radio-inline radio-primary">
							<label>
								<input type="radio" name="optionsRadios1" checked="">
								Option 1
							</label>
						</div>
						<div class="radio radio-inline radio-primary">
							<label>
								<input type="radio" name="optionsRadios1">
								Option 2
							</label>
						</div>
						<div class="radio radio-inline radio-primary">
							<label>
								<input type="radio" name="optionsRadios1">
								Option 3
							</label>
						</div>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Radio</label>
					<div class="col-sm-8">
						<div class="radio radio-primary">
							<label>
								<input type="radio" name="optionsRadios1" checked="">
								Option one is this and that
							</label>
						</div>
						<div class="radio radio-primary">
							<label>
								<input type="radio" name="optionsRadios1">
								Option two can be something else
							</label>
						</div>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Inline Checkbox</label>
					<div class="col-sm-8">
						<div class="checkbox checkbox-inline checkbox-primary">
							<label>
								<input type="checkbox" name="checkboxs1" checked="">
								Option 1
							</label>
						</div>
						<div class="checkbox checkbox-inline checkbox-primary">
							<label>
								<input type="checkbox" name="checkboxs1">
								Option 2
							</label>
						</div>
						<div class="checkbox checkbox-inline checkbox-primary">
							<label>
								<input type="checkbox" name="checkboxs1">
								Option 3
							</label>
						</div>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Checkbox</label>
					<div class="col-sm-8">
						<div class="checkbox checkbox-primary">
							<label>
								<input type="checkbox" name="optionscheckboxs1" checked="">
								Option one is this and that
							</label>
						</div>
						<div class="checkbox checkbox-primary">
							<label>
								<input type="checkbox" name="optionsRadios1">
								Option two can be something else
							</label>
						</div>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Textarea</label>
					<div class="col-sm-8">
						<textarea class="form-control"></textarea>
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Autogrow Textarea</label>
					<div class="col-sm-8">
						<textarea class="form-control autosize"></textarea>
					</div>
					<div class="col-sm-2"><p class="help-block">Textbox auto grows as you type!</p></div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label">Fullscreen Textarea</label>
					<div class="col-sm-8">
						<textarea class="form-control fullscreen"></textarea>
					</div>
				</div>

				<div class="form-group">
					<label class="col-sm-2 control-label" for="disabledInput">Disabled input</label>
					<div class="col-sm-8">
						<input class="form-control" id="disabledInput" type="text" placeholder="Disabled input here..." disabled="">
					</div>
				</div>

				<div class="form-group has-warning">
					<label class="col-sm-2 control-label" for="inputWarning">Input warning</label>
					<div class="col-sm-8">
						<input type="text" class="form-control" id="inputWarning">
					</div>
				</div>

				<div class="form-group has-error">
					<label class="col-sm-2 control-label" for="inputError">Input error</label>
					<div class="col-sm-8">
						<input type="text" class="form-control" id="inputError">
					</div>
				</div>

				<div class="form-group has-success">
					<label class="col-sm-2 control-label" for="inputSuccess">Input success</label>
					<div class="col-sm-8">
						<input type="text" class="form-control" id="inputSuccess">
					</div>
				</div>

				<div class="form-group">
					<label class="col-sm-2 control-label" for="inputLarge">Large input</label>
					<div class="col-sm-8">
						<input class="form-control input-lg" type="text" id="inputLarge">
					</div>
				</div>
				<div class="form-group">
					<label class="col-sm-2 control-label" for="inputSmall">Small input</label>
					<div class="col-sm-8">
						<input class="form-control input-sm" type="text" id="inputSmall">
					</div>
				</div>


				<div class="form-group">
					<label class="col-sm-2 control-label" for="addon1">Default label with input addons</label>
					<div class="col-sm-8 input-group">
						<span class="input-group-addon">$</span>
						<input type="text" id="addon1" class="form-control">
						<span class="input-group-btn">
							<button class="btn btn-default btn-raised" type="button">Button</button>
						</span>
					</div>
				</div>

				<div class="form-group ">
					<label class="col-sm-2 control-label" for="addon3a">Floating label with 2 addons</label>
					<div class="col-sm-8 input-group">
						<span class="input-group-addon">$</span>
						<input type="text" id="addon3a" class="form-control">
						<p class="help-block">The label is inside the <code>input-group</code> so that it is positioned properly as a placeholder.</p>
						<span class="input-group-btn">
							<button type="button" class="btn btn-fab btn-fab-mini">
								<i class="material-icons">send</i>
							</button>
						</span>
					</div>
				</div>

				<div class="form-group ">
					<label class="col-sm-2 control-label" for="addon2">Floating label with right addon</label>
					<div class="col-sm-8 input-group">
						<input type="text" id="addon2" class="form-control">
						<span class="input-group-btn">
							<button type="button" class="btn btn-fab btn-fab-mini">
								<i class="material-icons">functions</i>
							</button>
						</span>
					</div>
				</div>

				<div class="form-group">
					<label class="col-sm-2 control-label" for="addon3">File Upload</label>
					<input type="file" id="inputFile4" multiple="">
					<div class="col-sm-8 input-group">
						<input type="text" readonly="" id="addon3" class="form-control" placeholder="Placeholder with file chooser...">
						<span class="input-group-btn input-group-sm">
							<button type="button" class="btn btn-fab btn-fab-mini">
								<i class="material-icons">attach_file</i>
							</button>
						</span>
					</div>
				</div>
			</form>
			
		</div>
		<div class="panel-footer">
			<div class="row">
				<div class="col-sm-8 col-sm-offset-2">
					<button class="btn-raised btn-primary btn">Submit</button>
					<button class="btn-default btn">Cancel</button>
				</div>
			</div>
		</div>
	</div>