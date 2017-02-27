package camus.statechart;


import event.Event;
import net.jcip.annotations.Immutable;



/**
 *
 * @author Kang-Woo Lee (ETRI)
 */
@Immutable
public class EventHandledEvent extends StatechartEvent {
	private final String m_reactStateId;
	private final String m_toStateId;
	private final Event m_event;

	public EventHandledEvent(Event event, State reactState, String toStateId) {
		m_reactStateId = reactState.getGuid();
		m_toStateId = toStateId;
		m_event = event;
	}

    public String getReactingStateId() {
    	return m_reactStateId;
    }

    public String getToStateId() {
    	return m_toStateId;
    }

    public Event getEvent() {
    	return m_event;
    }

    @Override
    public String toString() {
    	String name = m_event.getEventTypeIds()[0];
    	int idx = name.lastIndexOf('.');
    	if ( idx >= 0 ) {
    		name = name.substring(idx+1);
    	}
    	
    	if ( m_toStateId != null ) {
        	return String.format("EventHandled: state[%s], event=%s, to=state[%s]",
        						m_reactStateId, name, m_toStateId);
    	}
    	else {
	    	return String.format("EventHandled: state[%s], event=%s", m_reactStateId, name);
    	}
    }
}
