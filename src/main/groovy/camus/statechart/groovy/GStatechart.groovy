package camus.statechart.groovy

import camus.statechart.StateNotFoundException
import camus.statechart.Statechart

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class GStatechart implements Statechart<GState>{
	final GState rootState
	
	public static StatechartBuilder builder(Map args, Closure decl) {
		new StatechartBuilder(args, decl)
	}
	
	public static StatechartBuilder builder(Closure decl) {
		StatechartBuilder bldr = new StatechartBuilder([:], decl)
	}
	
	GStatechart(GState root) {
		this.rootState = root
	}
	
	public GState getAt(String path) throws StateNotFoundException {
		rootState[path]
	}
	
	public Set<GState> findStateByLuid(String luid) {
		Set<GState> found = new HashSet<GState>();
		
		List<GState> states = [rootState]
		while ( !states.isEmpty() ) {
			GState state = states.remove(0);
			if ( state.luid == luid ) {
				found.add(state);
			}
			
			states.addAll(state.childStates);
		}
		
		return found;
	}
	
	public GState locateState(GState fromState, String path) throws StateNotFoundException {
		GState current = fromState;

		int idx = 0;
		List<String> parts = path.split("/")
		String head = parts.get(0);
		if ( head.length() == 0 ) {
			current = getRootState();
			++idx;
		}
		else if ( head.startsWith("@") ) {
			Set<GState> founds = findStateByLuid(head.substring(1));
			if ( founds.size() == 1 ) {
				current = founds.iterator().next();
				++idx;
			}
			else if ( founds.size() == 0 ) {
				throw new StateNotFoundException("LUID=" + head);
			}
			else {
				throw new IllegalArgumentException("ambiguous state id=" + head);
			}
		}

		for (; idx < parts.size(); ++idx ) {
			if ( parts.get(idx).equals("..") ) {
				current = current.parentState
				if ( current == null ) {
					throw new StateNotFoundException("from=" + fromState.getGuid() + ", path=" + path);
				}
			}
			else if ( parts.get(idx).equals(".") ) { }
			else {
				current = current.parentState.getSubState(parts.get(idx));
			}
		}
		
		return current;
	}
}
