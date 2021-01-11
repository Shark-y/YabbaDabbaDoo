package junit.reports.core;

public class MockObjects {

	public static void LOGD(String text) {
		System.out.println("[REPORTS-DBG] " + text);
	}

	public static void LOGE(String text) {
		System.err.println("[REPORTS-ERR] " + text);
	}

}
