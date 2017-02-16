package camus.statechart;

import async.AsyncOperationState;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
public interface FinalState {
	public AsyncOperationState getAsyncOperationState();
	public void setAsyncOperationState(AsyncOperationState state);
	
	public Throwable getFailureCause();
	public void setFailureCause(Throwable cause);
}
