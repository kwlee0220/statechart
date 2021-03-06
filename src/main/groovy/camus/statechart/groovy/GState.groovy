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
		if ( entry ) {
			def code = entry.rehydrate(context, null, null)
			code.resolveStrategy = Closure.DELEGATE_ONLY
			code.call()
		}
		else {
			null
		}
	}

	@Override
	public void leave(C context) {
		if ( exit ) {
			def code = exit.rehydrate(context, null, null)
			code.resolveStrategy = Closure.DELEGATE_ONLY
			code.call()
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
			
			if ( trans.action ) {
				def code = trans.action.rehydrate(context, null, null)
				code.resolveStrategy = Closure.DELEGATE_ONLY
				def result = code.call(event)
				if ( result instanceof String && result.length() > 0 ) {
					return result as String
				}
			}
		}
		
		return null;
	}
}