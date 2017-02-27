package camus.statechart.groovy;

import groovy.lang.Closure;

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
class GTransition {
	GStateBuilder builder
	Class eventClass
	Closure cond
	Closure action
	
	GTransition(GStateBuilder builder) {
		this.builder = builder
	}
	
	def when(Closure decl) {
		cond = decl
		this
	}
	
	def then(Closure decl) {
		action = decl
		builder.transitions << this
		builder.currentTransition = null
		builder
	}
	
	@Override
	public String toString() {
		"${builder.guid}:$name"
	}
}
