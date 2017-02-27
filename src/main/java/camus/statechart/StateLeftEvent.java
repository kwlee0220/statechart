package camus.statechart;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class StateLeftEvent extends StatechartEvent {
	private final String m_stateId;

	public StateLeftEvent(State<?> state) {
		m_stateId = state.getGuid();
	}

	public String getStateId() {
		return m_stateId;
	}

	@Override
	public String toString() {
		return String.format("Left: state[%s]", m_stateId);
	}
}
