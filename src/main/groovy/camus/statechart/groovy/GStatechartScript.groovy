package camus.statechart.groovy

/**
 * 
 * @author Kang-Woo Lee (ETRI)
 */
abstract class GStatechartScript extends Script {
	def statechart(Closure decl) {
		new GStatechartBuilder([:], decl)
	}
}
