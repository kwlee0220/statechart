package camus.statechart.support;

import camus.statechart.State;
import camus.statechart.Statechart;
import camus.statechart.StatechartExecution;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public class StateChartUtils {
	public static final String[] toIdParts(String guid) {
		return guid.split("/");
	}
	
	public static final String toLuid(String guid) {
		String[] parts = toIdParts(guid);
		return parts.length > 0 ? parts[parts.length-1] : "";
	}

	public static <C extends StatechartExecution<C>> State<C> traverse(Statechart<C> schart,
																	State<C> from, String path) {
		State<C> current = from;
		for ( String seg: path.split("/") ) {
			switch ( seg ) {
				case "..":
					current = current.getParentState();
					break;
				case ".":
					break;
				case "":
					current = schart.getRootState();
					break;
				default:
					current = current.getChildState(seg);
					break;
			}
		}
		
		return current;
	}
}
