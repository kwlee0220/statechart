package camus.statechart;

import java.util.Collection;

import event.Event;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface State<T extends State<T>> {
	public static final String STAY = "$$stay";
	
	public Statechart<T> getStatechart();
	
	public T getParentState();
	public String getGuid();
	public String getLuid();
	
	public default boolean isComposite() {
		return !getChildStates().isEmpty();
	}
	public default boolean isFinal() {
		return this instanceof FinalState;
	}
	
	public T getInitialChildState();
	public T getChildState(String luid);
	public Collection<T> getChildStates();
	public boolean isAncestorOf(T state);
	public T traverse(String path);
	
	public T getRecentChildState();
	public void setRecentChildState(T child);
	
	public T getExceptionState();
	
	public default T enter(StatechartContext context) {
		return null;
	}
	public default void leave(StatechartContext context) { }
	public String handleEvent(StatechartContext context, Event event);
}
