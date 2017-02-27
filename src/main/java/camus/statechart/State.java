package camus.statechart;

import java.util.Collection;

import event.Event;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface State<C extends StatechartExecution<C>> {
	public static final String FINISH_STATE_GUID = "/finished";
	public static final String STOP_PROPAGATE_GUID = "$$stop_propagate";
	
	public State<C> getParentState();
	public String getGuid();
	public String getLuid();
	
	public default boolean isComposite() {
		return !getChildStates().isEmpty();
	}
	public default boolean isFinal() {
		return this instanceof FinalState;
	}
	
	public State<C> getInitialChildState();
	public State<C> getChildState(String luid);
	public Collection<State<C>> getChildStates();
	public boolean isAncestorOf(State<C> state);
	
	public State<C> getRecentChildState();
	public void setRecentChildState(String stateId);
	
	public State<C> getExceptionState();
	
	public default String enter(C context) {
		return null;
	}
	public default void leave(C context) { }
	public String handleEvent(C context, Event event);
}
