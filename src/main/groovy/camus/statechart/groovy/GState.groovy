package camus.statechart.groovy

import java.awt.Desktop.Action

import camus.statechart.State
import camus.statechart.StateNotFoundException
import camus.statechart.Statechart
import camus.statechart.StatechartContext

import event.Event


/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class GState implements State<GState> {
	final GState parentState
	final String guid
	final String luid
	String defaultStateId
	Closure entry
	Closure exit
	Map<String,GState> childStates = [:]
	List<Transition> transitions = []
	
	boolean keepHistory
	GState recentChildState
	GState exceptionChildState
	
	GState(GState parentState, String guid, String luid) {
		this.parentState = parentState
		this.guid = guid
		this.luid = luid
	}

	@Override
	public Statechart<GState> getStatechart() {
		return statechart;
	}
	
	@Override
	public GState getInitialChildState() {
		childStates[defaultStateId]
	}
	
	@Override
	public Collection<GState> getChildStates() {
		this.childStates.values()
	}

	@Override
	public GState getChildState(String luid) {
		childStates[luid];
	}
	
	def addChildState(GState child) {
		childStates[child.luid] = child
	}

	@Override
	public GState traverse(String path) {
		String[] parts = path.split("/");
		
		path.split("/").inject(this) { GState current, String pathSeg ->
			switch ( pathSeg ) {
				case "..":
					current.parentState
					break;
				case ".":
					current;
					break;
				case "":
					current.statechart.rootState
					break;
				default:
					current.childStates[pathSeg]
					break;
			}
		}
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
	public GState enter(StatechartContext context) {
		(entry) ? entry.call() : null
	}

	@Override
	public void leave(StatechartContext context) {
		if ( exit ) {
			exit.call(context)
		}
	}

	@Override
	public String handleEvent(StatechartContext context, Event event) {
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

	@Override
	public GState getRecentChildState() {
		return keepHistory ? recentChildState : null
	}

	@Override
	public void setRecentChildState(GState child) {
		recentChildState = (keepHistory) ? child : null
	}

	@Override
	public boolean isAncestorOf(GState state) {
		state.guid.startsWith(guid)
	}

	@Override
	public GState getExceptionState() {
		return exceptionState;
	}
	
	@Override
	public String toString() {
		"State[${guid}]"
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
