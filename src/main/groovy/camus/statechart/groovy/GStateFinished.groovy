package camus.statechart.groovy;

import camus.statechart.FinalState

import async.AsyncOperationState

/**
 * 
 * @author Kang-Woo Lee
 */
class GStateFinished extends GState implements FinalState {
	public static final String GUID = "/finished";
	
	AsyncOperationState asyncOperationState
	Throwable failureCause
	
	public GStateFinished(GStatechart schart, GState parent) {
		super(schart, parent, GUID, "finished");
	}
}
