package camus.statechart.support;

import camus.statechart.State;
import camus.statechart.StatechartExecution;
import camus.statechart.Statechart;

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
		return parts[parts.length-1];
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
