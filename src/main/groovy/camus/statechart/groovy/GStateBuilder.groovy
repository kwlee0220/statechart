package camus.statechart.groovy

import groovy.lang.Closure

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class GStateBuilder {
	String guid, luid
	Closure entry, exit
	List<GStateBuilder> childStateBuilders = []
	List<GTransition> transitions = []
	
	String defaultStateId
	boolean keepHistory
	String exceptionChildStateId
	
	GTransition currentTransition
	
	GStateBuilder(GStateBuilder parent, String luid) {
		this.luid = luid
		if ( !parent ) {
			this.guid = "/" + luid
		}
		else if ( parent.guid != "/" ) {
			this.guid =  parent.guid + "/" + luid
		}
		else {
			this.guid = parent.guid + luid
		}
	}
	
	def GState build(GState parent) {
		GState state = new GState(parent, guid, keepHistory, exceptionChildStateId)
		state.entry = entry
		state.exit = exit
		if ( childStateBuilders.empty ) {
			state.defaultStateId = null
		}
		else {
			if ( !(state.defaultStateId = defaultStateId) ) {
				state.defaultStateId = childStateBuilders[0].luid
			}
			for ( GStateBuilder childBldr: childStateBuilders ) {
				GState child = childBldr.build(state)
				state.addChildState(child)
			}
		}
		state.transitions = transitions
		
		state
	}
	
	def state(String luid, Closure decl) {
		state(null, luid, decl)
	}
	
	def state(String luid) {
		state(null, luid, null)
	}
	
	def state(Map attrs, String luid, Closure decl) {
		GStateBuilder child = new GStateBuilder(this, luid)
		child.with decl
		childStateBuilders << child
		
		child
	}
	
	def defaultStateId(String id) {
		defaultStateId = id
	}
	
	def keepHistory(boolean flag) {
		keepHistory = flag
	}
	
	def entry(Closure decl) {
		this.entry = decl
	}
	
	def exit(Closure decl) {
		this.exit = decl
	}
	
	def on(eventExpr, Closure decl) {
		def eventClass = null
		if ( eventExpr instanceof String ) {
			eventClass = Class.forName(eventExpr)
		}
		else if ( eventExpr instanceof Class ) {
			eventClass = (Class)eventExpr
		}
		
		transitions << new GTransition(eventClass:eventClass, action:decl)
	}
	
	def on(Closure decl) {
		transitions << new GTransition(action:decl)
	}
	
	public String propertyMissing(String name) {
		println "propertyMissing($name)"
	}
	
	public String methodMissing(String name, args) {
		println "methodMissing($name,$args)"
	}
	
	@Override
	public String toString() {
		"State[${guid}]"
	}
}
