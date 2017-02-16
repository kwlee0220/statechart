package camus.statechart;

import async.AsyncOperationState;

/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class StatechartFinishedEvent extends StatechartEvent {
	public final AsyncOperationState m_state;
	public final Throwable m_fault;
	
	public StatechartFinishedEvent(AsyncOperationState state, Throwable fault) {
		m_state = state;
		m_fault = fault;
	}
	
	@Override
	public String toString() {
		switch ( m_state ) {
			case COMPLETED:
			case CANCELLED:
				return "Finished[state=" + m_state + "]";
			case FAILED:
				return "Finished[state=" + m_state + ", fault=" + m_fault + "]";
			default:
				throw new RuntimeException("invalid AOP state=" + m_state);
		}
	}
}
