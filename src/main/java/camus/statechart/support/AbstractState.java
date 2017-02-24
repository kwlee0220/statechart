package camus.statechart.support;

import java.util.Collection;
import java.util.Map;

import camus.statechart.State;
import camus.statechart.StatechartExecution;
import camus.statechart.StateExistsException;
import camus.statechart.StateNotFoundException;

import com.google.common.collect.Maps;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public abstract class AbstractState<C extends StatechartExecution<C>> implements State<C> {
	private final State<C> m_parent;
	private final String m_guid;
	private final String m_luid;
	
	private String m_defaultStateId;
	private final Map<String,State<C>> m_childStates = Maps.newHashMap();

	private final String m_exceptionChildStateId;
	private final boolean m_keepHistory;
	private String m_recentChildStateId;
	
	protected AbstractState(State<C> parent, String guid, boolean keepHistory,
							String exceptionChildStateId) {
		m_parent = parent;
		m_guid = guid;
		m_luid = StateChartUtils.toLuid(guid);
		
		m_keepHistory = keepHistory;
		m_exceptionChildStateId = exceptionChildStateId;
	}

	@Override
	public State<C> getParentState() {
		return m_parent;
	}

	@Override
	public String getGuid() {
		return m_guid;
	}

	@Override
	public String getLuid() {
		return m_luid;
	}

	@Override
	public State<C> getInitialChildState() {
		return m_childStates.get(m_defaultStateId);
	}
	
	public void setDefaultStateId(String stateId) {
		m_defaultStateId = stateId;
	}

	@Override
	public State<C> getChildState(String luid) {
		return m_childStates.get(luid);
	}

	@Override
	public Collection<State<C>> getChildStates() {
		return m_childStates.values();
	}
	
	public void addChildState(State<C> child) {
		if ( m_childStates.putIfAbsent(child.getLuid(), child) != null ) {
			throw new StateExistsException("child luid=" + child.getLuid());
		}
	}

	@Override
	public boolean isAncestorOf(State<C> state) {
		return state.getGuid().startsWith(getGuid());
	}

	@Override
	public State<C> getRecentChildState() {
		if ( m_keepHistory ) {
			return ( m_recentChildStateId != null ) ? m_childStates.get(m_recentChildStateId) : null;
		}
		else {
			return null;
		}
	}
	
	@Override
	public void setRecentChildState(String childLuid) {
		if ( m_keepHistory ) {
			if ( !m_childStates.containsKey(childLuid) ) {
				throw new StateNotFoundException("unknown child state: luid=" + childLuid);
			}
			
			m_recentChildStateId = childLuid;
		}
	}

	@Override
	public State<C> getExceptionState() {
		return m_childStates.get(m_exceptionChildStateId);
	}

	@Override
	public String toString() {
		return String.format("State[%s]", m_guid);
	}
}
