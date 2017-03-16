/* Holds random number genator necessities */
/* Trying to encapsulate this, so the RNG particulars can be changed if necessary */ 
/* Completely static class, allows no instances to be instantiated */

//import cern.jet.random.*;

public class Random {
	
	private static final boolean Debug = false;
	// The following static random number generators are initialized with specific engine
	// depending on whether the Debug flag is true or false.
	private static final cern.jet.random.Uniform uniform;
	private static final cern.jet.random.Normal  normal;
	private static final cern.jet.random.Exponential exponential;
	private static final cern.jet.random.Gamma gamma;
	private static final cern.jet.random.Poisson poisson;
	
	static {
		cern.jet.random.engine.RandomEngine engine = null;
		if (Debug) {
			engine = new cern.jet.random.engine.MersenneTwister();
		} else {
			engine = cern.jet.random.AbstractDistribution.makeDefaultGenerator();
		}
		// Create the random number generators with given engine
		cern.jet.random.Uniform.staticSetRandomEngine(engine);
		uniform     = new cern.jet.random.Uniform(engine);
		normal      = new cern.jet.random.Normal(0, 1.0, engine);
		exponential = new cern.jet.random.Exponential(1.0, engine);
		gamma       = new cern.jet.random.Gamma(1.0, 1.0, engine);
		poisson     = new cern.jet.random.Poisson(0, engine);
	}
	
	// methods

	public static int nextInt(int from, int to) {
		return uniform.nextIntFromTo(from, to);
	}	
	
	public static double nextDouble() {
		return uniform.nextDouble();		
	}
	
	public static double nextDouble(double from, double to) {
		return uniform.nextDoubleFromTo(from, to);		
	}	

	public static double nextNormal() {
		return normal.nextDouble(0.0, 1.0);
	}
	
	public static double nextNormal(double mean, double sd) {
		return normal.nextDouble(mean, sd);
	}	

	// tuned with mean
	public static double nextExponential(double lambda) {
		return exponential.nextDouble(1.0 / lambda);
	}
	
	// tuned with alpha and beta, matching Mathematica's notation
	public static double nextGamma(double alpha, double beta) {
		return gamma.nextDouble(alpha, 1 / beta);
	}	
	
	public static int nextPoisson(double lambda) {
		return poisson.nextInt(lambda);
	}
	
	public static boolean nextBoolean(double p) {
		boolean x = false;
		if (nextDouble() < p) {
			x = true;
		}
		return x;
	}	
	
	private Random() {}
}
