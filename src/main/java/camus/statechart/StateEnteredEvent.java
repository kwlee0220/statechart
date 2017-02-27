package camus.statechart;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class StateEnteredEvent extends StatechartEvent {
	private final String m_stateId;

	public StateEnteredEvent(State state) {
		m_stateId = state.getGuid();
	}

	public String getStateId() {
		return m_stateId;
	}

	@Override
	public String toString() {
		return String.format("Entered: state[%s]", m_stateId);
	}
}
