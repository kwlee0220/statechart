package camus.statechart.groovy

import camus.statechart.State
import camus.statechart.StateNotFoundException
import camus.statechart.StatechartExecution
import camus.statechart.support.AbstractState

import event.Event


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class GState<C extends StatechartExecution<C>> extends AbstractState<C> implements State<C> {
	Closure entry
	Closure exit
	List<GTransition> transitions = []
	
	GState(State parentState, String guid, boolean keepHistory, String exceptionChildStateId) {
		super(parentState, guid, keepHistory, exceptionChildStateId)
	}
	
	GState div(String rel) {
		switch ( rel ) {
			case "..":
				if ( parentState ) {
					parentState
				}
				break;
			case ".":
				break;
			default:
				def child = childStates[rel]
				if ( !child ) {
					throw new StateNotFoundException("$guid/$rel")
				}
				child
				break
		}
	}

	@Override
	public String enter(C context) {
		(entry) ? entry.call(context) : null
	}

	@Override
	public void leave(C context) {
		if ( exit ) {
			exit.call(context)
		}
	}

	@Override
	public String handleEvent(C context, Event event) {
		for ( GTransition trans: transitions ) {
			if ( trans.eventClass ) {
				if ( !event.isInstanceOf(trans.eventClass) ) {
					continue;
				}
			}
			
//			def result = (trans.action) ? trans.action.call(context, event) : null
			def result = trans.action.call(context, event)
			if ( result instanceof String && result.length() > 0 ) {
				return result as String
			}
		}
		
		return null;
	}
}