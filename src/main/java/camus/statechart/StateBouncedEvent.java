package camus.statechart;




/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class StateBouncedEvent extends StatechartEvent {
	private final String m_stateId;

	public StateBouncedEvent(State state) {
		m_stateId = state.getGuid();
	}

	public String getStateId() {
		return m_stateId;
	}

	@Override
	public String toString() {
		return String.format("Bounced[state=%s]", m_stateId);
	}
}
