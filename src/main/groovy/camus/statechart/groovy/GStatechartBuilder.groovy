package camus.statechart.groovy

import java.util.Map

import groovy.lang.Closure
import groovy.transform.InheritConstructors

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
@InheritConstructors
class GStatechartBuilder {
	GStateBuilder stateBuilder
	
	GStatechartBuilder(Map args, Closure decl) {
		stateBuilder = new GStateBuilder(null, "");
		stateBuilder.with decl
	}
	
	GStatechart build() {
		new GStatechart(stateBuilder.build(null))
	}
}
