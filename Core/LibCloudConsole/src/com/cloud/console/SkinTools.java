package com.cloud.console;

import com.cloud.console.ThemeManager;

/**
 * Skin helper subs. Used by JSP include directives. for example: <pre>
 * &lt;head>
 * ...
 * &lt;jsp:include page="&lt;%=SkinTools.buildHeadTilePath(\"../../\") %>">
 * 	&lt;jsp:param value="&lt;%=SkinTools.buildBasePath(\"../../\", \"\") %>" name="basePath"/>
 * 	&lt;jsp:param value="&lt;%=theme%>" name="theme"/>
 * &lt;/jsp:include>
 * &lt;/head>
 * </pre>
 * @author vsilva
 *
 */
public class SkinTools {

	/** Available skins */
	public enum SkinType {
		bootstrap, 
		material,
		booterial,
		clouds,		// 12/7/2020
		altair		// 12/7/2020
	};
	
	static final ThemeManager tm 		= ThemeManager.getInstance();
	static final String theme			= tm.getThemeName() != null 	? tm.getThemeName() 	: SkinType.bootstrap.name() + "-blue";
	
	/** Skin name + trailing slash: material/ bootstrap/  **/ 
	public static final String SKIN_PATH		= theme.contains("-") 	? "skins/" + theme.split("-")[0] +  "/" 	:  "skins/bootstrap/";
	
	/** Relative path of the HTML <HEAD> tile for a particular skin. This tile must be included in the <head></head> of all JSPs **/
	public static final String TILE_PATH_HEAD		= SKIN_PATH + "tiles/tile_head.jsp";

	/** Relative path of the page start section for a particular skin. This tile must be included after the <body> of all JSPs **/
	public static final String TILE_PATH_PAGE_START	= SKIN_PATH + "tiles/tile_page_start.jsp";
	
	/** Relative path of the page end section for a particular skin. This tile must be included @ the end </body> of all JSPs **/
	public static final String TILE_PATH_PAGE_END	= SKIN_PATH + "tiles/tile_page_end.jsp";

	/** Status message jsp:include path */
	public static final String TILE_PATH_STATUS_MSG	= SKIN_PATH + "tiles/tile_status_msg.jsp";

	/** Relative path of the login modal section for a particular skin. This tile must be included @ the end &lt;/body> of a JSPs  for modal login **/
	public static final String TILE_PATH_LOGIN_MODAL = SKIN_PATH + "tiles/tile_login_modal.jsp";

	/** Relative path of custom modal section for a particular skin. This tile must be included @ the end &lt;/body> of a JSP for a modal dialog **/
	public static final String TILE_PATH_MODAL = SKIN_PATH + "tiles/tile_modal.jsp";

	/**
	 * Build a relative path of the HTML <HEAD> tile for a particular skin. Meant to e called by JSPs located in custom locations 
	 * @param prefix Custom path prefix. For example ../ 
	 * @return
	 */
	public static String buildHeadTilePath(String prefix) {
		return prefix + TILE_PATH_HEAD;
	}
	
	public static String buildPageStartTilePath(String prefix) {
		return prefix + TILE_PATH_PAGE_START;
	}
	
	public static String buildPageEndTilePath(String prefix) {
		return prefix + TILE_PATH_PAGE_END;
	}

	public static String buildStatusMessagePath(String prefix) {
		return prefix + TILE_PATH_STATUS_MSG;
	}

	/**
	 * jsp:param directive path builder.
	 * <pre> &lt;jsp:param value="&lt;%=SkinTools.buildBasePath(\"../../\", \"\") %>" name="basePath"/> </pre>
	 * @param prefix
	 * @param suffix
	 * @return Path : prefix/{SKIN_NAME}/suffix/
	 */
	public static String buildBasePath(String prefix) {
		return prefix + SKIN_PATH;
	}

	/**
	 * jsp:param directive path builder.
	 * <pre> &lt;jsp:param value="&lt;%=SkinTools.buildBasePath(\"../../\", \"\") %>" name="basePath"/> </pre>
	 * @param prefix
	 * @param suffix
	 * @return Path : prefix/{SKIN_NAME}/suffix/
	 */
	public static String buildBasePath(String prefix, String suffix) {
		return prefix + SKIN_PATH + suffix;
	}

	public static boolean isBootstrapTheme () {
		return SKIN_PATH.contains(SkinType.bootstrap.name());
	}
	
	public static boolean isMaterialTheme () {
		return SKIN_PATH.contains(SkinType.material.name());
	}

	public static boolean isTheme (SkinType type) {
		return SKIN_PATH.contains(type.name());
	}

	public static boolean isAltAirTheme () {
		return SKIN_PATH.contains(SkinType.altair.name());
	}

	public static boolean isCloudsTheme () {
		return SKIN_PATH.contains(SkinType.clouds.name());
	}
	
	public static String getSkinName() {
		return theme.contains("-") 	? theme.split("-")[0] : tm.getThemeName();
	}
	
	public static String cssInputClass () {
		if ( SKIN_PATH.contains(SkinType.altair.name()) ) {
			return "md-input";
		}
		return "form-control";
	}

	public static String cssFormGroupClass () {
		if ( SKIN_PATH.contains(SkinType.altair.name()) ) {
			return "uk-grid uk-width-1-1";
		}
		return "form-group";
	}

	public static String cssFormGroupLabelClass () {
		if ( SKIN_PATH.contains(SkinType.altair.name()) ) {
			return "uk-width-1-4";
		}
		return "col-sm-2 control-label";
	}
	
	public static String cssFormGroupContentClass () {
		if ( SKIN_PATH.contains(SkinType.altair.name()) ) {
			return "uk-width-3-4";
		}
		return "col-sm-10";
	}

	public static String cssPanelToolbarBtnClass () {
		if ( SKIN_PATH.contains(SkinType.altair.name()) ) {
			return "md-btn md-btn-small md-btn-flat";
		}
		return "btn btn-icon-rounded";
	}
	
}
