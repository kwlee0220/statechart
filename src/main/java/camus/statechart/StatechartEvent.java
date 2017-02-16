package camus.statechart;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class StatechartEvent {
	protected static String getLastComponent(String str, char delim) {
		if ( str == null ) {
			return null;
		}
		
		int idx = str.lastIndexOf(delim);
		return (idx >= 0) ? str.substring(idx+1) : str;
	}
}
