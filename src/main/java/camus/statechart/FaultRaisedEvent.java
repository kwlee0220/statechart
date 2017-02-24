package camus.statechart;

import event.Event;


/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
public class FaultRaisedEvent extends StatechartEvent {
	private final Throwable m_fault;
	private final String m_throwStateId;
	private final String m_toStateId;
	private final StatechartFaultCase m_faultCase;
	private final Event m_event;

	public FaultRaisedEvent(Throwable fault, State throwState, State toState,
							StatechartFaultCase faultCase, Event event) {
		m_fault = fault;
		m_throwStateId = throwState.getGuid();
		m_toStateId = (toState != null) ? toState.getGuid() : null;
		m_faultCase = faultCase;
		m_event = event;
	}

    public Throwable getFault() {
    	return m_fault;
    }

    public String getThrowingStateId() {
    	return m_throwStateId;
    }

    public String getToStateId() {
    	return m_toStateId;
    }

    public StatechartFaultCase getFaultCase() {
    	return m_faultCase;
    }

    public Event getEvent() {
    	return m_event;
    }

	@Override
	public String toString() {
    	String evName = getLastComponent(m_event.getEventTypeIds()[0], '.');

		return String.format("FaultRaised[event=%s,thrower=%s,to=%s,case=%s,fault=%s]",
							evName, m_throwStateId, m_toStateId, "" + m_faultCase, "" + m_fault);
	}
}
