package camus.statechart.groovy

import camus.statechart.State
import camus.statechart.StateNotFoundException
import camus.statechart.Statechart
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
	List<Transition> transitions = []
	
	GState(Statechart schart, State parentState, String guid, boolean keepHistory,
			String exceptionChildStateId) {
		super(schart, parentState, guid, keepHistory, exceptionChildStateId)
	}
	
	GState getAt(String path) {
		traverse(path)
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
	public State<C> enter(C context) {
		(entry) ? entry.call() : null
	}

	@Override
	public void leave(C context) {
		if ( exit ) {
			exit.call(context)
		}
	}

	@Override
	public State<C> handleEvent(C context, Event event) {
		for ( Transition trans: transitions ) {
			def matched = trans.cond(context, event)
			if ( matched ) {
				def result = (trans.action) ? trans.action(context, event) : null
				if ( result instanceof String && result.length() > 0 ) {
					return result as String
				}
				return trans.toStateId
			}
		}
		
		return null;
	}
}

class Transition {
	Closure cond
	Closure action
	String toStateId
	
	def when(Closure condExpr) {
		this.cond = condExpr
		this
	}
	
	def then(Closure actionExpr) {
		action = actionExpr;
		this
	}
	
	def moveTo(String targetStateId) {
		toStateId = targetStateId;
		this
	}
}
