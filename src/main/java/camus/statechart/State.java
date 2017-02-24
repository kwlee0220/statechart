package camus.statechart;

import java.util.Collection;

import event.Event;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface State<C extends StatechartExecution<C>> {
	public static final String FINISH_STATE_GUID = "/finished";
	
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
	
	public default State<C> enter(C context) {
		return null;
	}
	public default void leave(C context) { }
	public State<C> handleEvent(C context, Event event);
	
	public static final State STOP_PROPAGATE = new State() {
		@Override
		public State getParentState() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getGuid() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLuid() {
			throw new UnsupportedOperationException();
		}

		@Override
		public State getInitialChildState() {
			throw new UnsupportedOperationException();
		}

		@Override
		public State getChildState(String luid) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Collection<State> getChildStates() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isAncestorOf(State state) {
			throw new UnsupportedOperationException();
		}

		@Override
		public State getRecentChildState() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setRecentChildState(String stateId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public State getExceptionState() {
			throw new UnsupportedOperationException();
		}

		@Override
		public State handleEvent(StatechartExecution context, Event event) {
			return null;
		}
	};
}
