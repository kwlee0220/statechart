package camus.statechart;




/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class StateBouncedEvent<C extends StatechartExecution<C>> extends StatechartEvent {
	private final State<C> m_fromState;
	private final State<C> m_bounceState;

	public StateBouncedEvent(State<C> fromState, State<C> bounceState) {
		m_fromState = fromState;
		m_bounceState = bounceState;
	}

	public State<C> getFromState() {
		return m_fromState;
	}

	public State<C> getBounceState() {
		return m_bounceState;
	}

	@Override
	public String toString() {
		return String.format("Bounced: %s -> %s", m_fromState, m_bounceState);
	}
}
