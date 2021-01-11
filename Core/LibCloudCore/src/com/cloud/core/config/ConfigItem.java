package com.cloud.core.config;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Properties;

import com.cloud.core.config.ServiceConfiguration;
import com.cloud.core.config.ServiceConfiguration.WidgetType;
import com.cloud.core.security.EncryptionTool;

/**
 * A Configuration item (widget). Item samples:
 * <pre>
 * [ k:test_00_05_primaryProviderPassword v:Hello World Lbl:Provider Password Grp:cfg_group00_backend Widget:Password]
 * [ k:test_TServerHost v:tcp://192.168.44.82:3000 Lbl:T-Server Host Grp:cfg_group01_backend Widget:TextBox]
 * [ k:test_backend_capabilities v:CAP_ACD_LOGIN Lbl:Backend Capabilities Grp:cfg_group01_backend Widget:DualMultiSelectNonEditable]
 * [ k:test_backend_vendor v:Genesys Lbl:Vendor Grp:cfg_group01_backend Widget:Label]
 * [ k:test_ui_DisplayFields v:holdTime,callerDN,calledDN Lbl:null Grp:cfg_group03_dfs Widget:DualMultiSelectEditable MultiVals:{duration=duration, callType=callType}]
 * [ k:test_ui_callLog00_callLog v:Subject,AssociateTo,RelatedTo,Comments,CallDuration,Holdtime,CallInitiatedTime,CallAnsweredTime,CallDisconnectTime,CallType Lbl:null Grp:cfg_group04_calllog Widget:DualMultiSelectEditable MultiVals:{dnis:1=dnis:1}]
 * </pre>
 * See the Cloud Config doc for details.
 * @author sharky
 *
 */
public class ConfigItem implements Serializable {
	private static final long serialVersionUID	= -8352837562283229996L;
	public static final String LFCR				= "\n\t";
	public static final String TAB				= "\t";
	
	public String key;				// the widget key
	public String name;				// item property name
	public String value;			// the value
	public String label;			// label (left side)
	
	public WidgetType type;			// widget type
	public Properties multiValues;	// Widget values for combo or diualmultiselct
	
	/**
	 * Special widget types: combo, dualmultisect<pre>
	 * combo&/configuration/screenpoprules.ini 			(COMBO WITH PROPERTIES)
	 * dualmultiselect&dnis:1 							(DUAL SELECT WITH VALS)
	 * dualmultiselect&/configuration/capabilities.ini	(DUAL SELCT WITH PROPERTIES)
	 * dualmultiselect&callType,duration</pre>
	 */
	public String widgetTypeRaw;	// For specialcombo, dualmultisect:
	
	public String group;			// <FIELDSET> widget grouping
	public String rowId;			// used to render multiple widgets in a row
	public String style;			// HTML style (if available)
	public String onclick;			// Optional Javascript fired by the onclick HTML event.
	
	// For INPUT type=text (text boxes) - OPTIONAL
	public String placeHolder;		// Placeholder for INPUT type=text only!
	public String pattern;			// HTML 5 validation using regular expressions. \d+ (numbers), etc...
	public String title;			// HTML 5 attribute of the same name.

	public String collapseRow;		// ID of the collapsible row (if available).
	
	public boolean required;		// True If required. Applies to INPUT type text only.
	
	@Override
	public String toString() {
		return "[ k:" + key + " v:" + value + " Lbl:" + label 
				+ " Grp:" + group + " Widget:" + type
				+ ( rowId != null ? " RowId:" + rowId : "")
				+ ( style != null ? " Style:" + style : "")
				+ ( multiValues != null ?  " MultiVals:" + multiValues : "") 
				+ ( title != null ?  " Title:" + title : "")
				+ ( pattern != null ?  " Pattern:" + pattern : "")
				+ "]";
	}
	
	/**
	 * Get HTML options for the multi values (left side of the DUAL multi SELECT widget)
	 * @return
	 */
	public String getMultiValuesAsHTML(String selectedKey) {
		StringBuffer html = new StringBuffer();
		
		// NO left side vals defined!
		if ( multiValues == null) {
			return html.toString();
		}
		//Set<Object> okeys = multiValues.keySet();
		Enumeration<?> okeys 	= multiValues.propertyNames();
		boolean isDualSelWidget = ( type == WidgetType.DualMultiSelectEditable || type == WidgetType.DualMultiSelectNonEditable);
		
		//for (Object okey : okeys) {
		while ( okeys.hasMoreElements()) {
			String key		= okeys.nextElement().toString(); // okey.toString();
			String selected = (selectedKey != null && selectedKey.equals(key) ) ? " selected=\"selected\" " : "";
			String value	= multiValues.getProperty(key); 
			
			// ignore existing keys in widget vals (only for DualSelect widgets)
			if ( isDualSelWidget && this.value.contains(key)) {
				continue;
			}
			html.append( "<option value=\"" + key + "\"" 
					+ selected 
					+ " title=\"" + value + "\""
					+ ">" + value + "</option>");
		}
		return html.toString();
	}
	
	/**
	 * Get the selected values as HTML option(s)
	 * @return
	 */
	public String getSelectedAsHTML() {
		StringBuffer html 		= new StringBuffer();
		String[] vals 			= value.split(",");
		boolean hasLeftSideVals = multiValues != null;
		
		for (int i = 0; i < vals.length; i++) {
			// empty val[i] - note: val[i] never null!
			if ( vals[i].trim().length() == 0) {
				continue; 
			}
			
			// label available?
			String label = hasLeftSideVals && multiValues.getProperty(vals[i]) != null 
					? multiValues.getProperty(vals[i]) 
					: vals[i];
					
			html.append("<option value=\"" + vals[i] + "\" title=\"" + label + "\">" + label + "</option>");
		}
		return html.toString();
	}
	
	/**
	 * Return an HTML representation of this configuration item.
	 * @return
	 */
	public String toHTML () {
		String HTML = null ;
		
		// only text boxes/links suppported for now...
		if (type != WidgetType.TextBox && type != WidgetType.Hyperlink) {
			throw new IllegalArgumentException("Invalid widget type: " + type + " for key: " + key + " value: " + value);
		}
		
		String htmlStyle = getStyleAttribute();
		
		if ( type == WidgetType.TextBox) {
			HTML = "<input type=\"text\" " + htmlStyle + " name=\"" + key + "\" value=\"" + value + "\">";
		}
		if ( type == WidgetType.Hyperlink ) {
			HTML = "<a " + getOnclickAttribute() + " " +  htmlStyle + " name=\"" + key + "\" href=\"" + value + "\">" + label + "</a>";
		}
		return HTML;
	}

	public String getStyleAttribute() {
		if ( style != null)
			return "style=\"" + style + "\"";
		return "";
	}
	
	public String getOnclickAttribute() {
		if ( onclick != null)
			return "onclick=\"" + onclick + "\"";
		return "";
	}
	
	/**
	 * Returns the HTML name="value" or ""  if value is null.
	 * @param name Attribute name.
	 * @param value Attribute value.
	 * @return name="value" or ""  if value is null.
	 */
	public String getAttributeAsHTML(String name, String value) {
		return value != null ? name +"=\"" + value + "\"" : "";
	}

	/**
	 * Convert the {@link WidgetType} enum to a string name.
	 * @return Widget type as string
	 */
	private String wigdetTypeToString() {
		if ( type == WidgetType.TextBox) 			return "text"; // bug in old names (should be textbox).
		if ( type == WidgetType.Boolean) 			return ServiceConfiguration.WIDGET_TYPE_KEY_BOOLEAN;
		if ( type == WidgetType.BooleanWithLink) 	return ServiceConfiguration.WIDGET_TYPE_KEY_BOOL_WLINK;
		if ( type == WidgetType.TextArea) 			return ServiceConfiguration.WIDGET_TYPE_KEY_TEXTAREA;
		if ( type == WidgetType.Password) 			return ServiceConfiguration.WIDGET_TYPE_KEY_PWD;
		if ( type == WidgetType.CheckBox) 			return ServiceConfiguration.WIDGET_TYPE_KEY_CHECKBOX;
		if ( type == WidgetType.Hyperlink) 			return ServiceConfiguration.WIDGET_TYPE_KEY_LINK;
		if ( type == WidgetType.Label) 				return ServiceConfiguration.WIDGET_TYPE_KEY_LABEL;
		
		// Special types. Must use the raw widget type value 
		if ( type == WidgetType.ComboBox || type == WidgetType.DualMultiSelectEditable || type == WidgetType.DualMultiSelectNonEditable) {
			if ( widgetTypeRaw != null) 					return widgetTypeRaw;
			if ( type == WidgetType.ComboBox) 				return ServiceConfiguration.WIDGET_TYPE_KEY_COMBO;
			if ( type == WidgetType.DualMultiSelectEditable) return ServiceConfiguration.WIDGET_TYPE_KEY_DUALSELECT;
		}
		if ( type == WidgetType.SingleMultiSelectEditable) 	return ServiceConfiguration.WIDGET_TYPE_KEY_SSELECT;
		
		// all failed? This is probably a bug.
		return type.name();
	}
	
	/**
	 * Serialize to XML. See the Cloud Config doc for the XML format.
	 * @return XML string for this item.<pre>
	 * &lt;property>
	 * 	&lt;key>test_backend_capabilities&lt;/key>
	 * 	&lt;name>backend_capabilities&lt;/name>
	 * 	&lt;value>&lt;![CDATA[CAP_ACD_LOGIN]]>&lt;/value>
	 * 	&lt;attributes>
	 * 		&lt;label>Backend Capabilities&lt;/label>
	 * 		&lt;group>cfg_group01_backend&lt;/group>
	 * 		&lt;widget>&lt;![CDATA[dualmultiselect&/configuration/capabilities.ini]]>&lt;/widget>
	 * 	&lt;/attributes>
	 * &lt;/property> </pre>
	 */
	public String toXML() {
		return "\n<property>" 
				+ LFCR + "<key>" + key + "</key>" 
				+ LFCR + "<name>" + name + "</name>" 
				+ LFCR + "<value><![CDATA[" + internalGetValue() + "]]></value>" 
				
				// start attributes
				+ LFCR + "<attributes>"
				+ ( label != null		? LFCR + TAB + "<label><![CDATA[" + label + "]]></label>" : "")
				+ LFCR + TAB + "<group>" + group + "</group>" 
				+ LFCR + TAB + "<widget><![CDATA[" +  wigdetTypeToString() + "]]></widget>"
				
				// Optional attributes.
				+ ( rowId != null 		? LFCR + TAB + "<rowid>" + rowId 	+ "</rowid>" : "")
				+ ( style != null 		? LFCR + TAB + "<style><![CDATA[" 	+ style + "]]></style>" : "")
				+ ( title != null 		? LFCR + TAB + "<title><![CDATA[" 	+ title + "]]></title>" : "")
				+ ( pattern != null 	? LFCR + TAB + "<pattern>" 			+ pattern 		+ "</pattern>" : "")
				+ ( placeHolder != null ? LFCR + TAB + "<placeholder><![CDATA[" 	+ placeHolder 	+ "]]></placeholder>" : "")
				+ ( onclick != null 	? LFCR + TAB + "<onclick><![CDATA[" 		+ onclick 		+ "]]></onclick>" : "")
				+ ( collapseRow != null ? LFCR + TAB + "<collapserow>" 				+ collapseRow 	+ "</collapserow>" : "")
				
				// HTML5 required attribute only available in text boxes.
				+ ( type == WidgetType.TextBox || type == WidgetType.Password ? LFCR + TAB + "<required>" 			+ required 		+ "</required>" : "")
				
				+ LFCR + "</attributes>"
				// End attributes
				
				+ "\n</property>";
	}

	/**
	 * Get item value. Encrypt if type is password.
	 * @return Value (encrypted if password).
	 */
	private String internalGetValue() {
		return type == WidgetType.Password ? EncryptionTool.encryptAndTagPassword(value) : value;
	}
}