
/* Implements an individual-based model in which the infection's genealogical history is tracked through time */

class Antigen {
	// Variable to hold dimension value from command-line arguments.
	public static int dimen = 1;

	// Variable to introduce skew in species after entries are created.
	private static double speciesSkew = 0;
		
	/** 
	 * Convenience method to override environment parameter settings
	 * in all demes.  The overrides are typically passed-in as
	 * command-line arguments. The overrides are handy to perform
	 * different types of analysis, such as Generalized Sensitivity
	 * Analysis (GSA).
	 * 
	 * @param param The name of the parameter
	 * @param value The value to be set for it.
	 */
	private static void updateEnvParam(String param, double value) {
		for (int i = 0; (i < Parameters.demeEnvList.size()); i++) {
			Parameters.demeEnvList.get(i).setParam(param, value);
		}
	}
	
	/**
	 * Convenience method to process command-line overrides for
	 * specific values in the YML file.
	 * 
	 * @param param The string associated with the parameter. Note
	 * that the string names and the actual parameters they impact
	 * are not 1-to-1 and some of them are downright confusing. Maybe
	 * we will eventually fix them. 
	 * 
	 * @param value The override value to be set for the given 
	 * parameter.
	 */
	private static void processParam(String param, String value) {
		switch (param) {
		case "recovery": 
			Parameters.nu = 1.0 / Double.parseDouble(value);
			break;
		case "contact": 
			Parameters.beta = Double.parseDouble(value);
			break;
		case "mutation": 
			Parameters.muPhenotype = Double.parseDouble(value);
			break;
		case "initialI":
			Parameters.initialI = Integer.parseInt(value);
			break;
		case "dimen":
			dimen = Integer.parseInt(value);
			break;
		case "cull":
			Parameters.culling = Double.parseDouble(value);
			break;
		case "vaccinate":
			Parameters.vaccinate = Double.parseDouble(value);
			break;
		case "burnin":
			Parameters.burnin = Integer.parseInt(value);
			break;
		case "outputDir":
			Parameters.outputDir = value;
			break;
		case "paramFile":
			// Nothing to be done here as this parameter has already
			// been processed (first thing in main)
			break;
		case "meanStep":
			Parameters.meanStep = Double.parseDouble(value);
			break;
		case "initialNs":
			final int pop = Integer.parseInt(value);
			Parameters.initialNs = new int[] {pop};
			break;
		case "demeAmplitudes":
			final double amp = Double.parseDouble(value);
			Parameters.demeAmplitudes = new double[] {amp};
			break;
		case "speciesSkew":
			speciesSkew = 50.0 - Double.parseDouble(value);
			break;
		// The following are environment parameters that are handled
		// by the environment class.
		case "envUpTakeRate":
		case "id50":
		case "envDurability":
		case "seasonalAmp":
		case "sheddingRate":
			final double val = Double.parseDouble(value);		
			updateEnvParam(param, val);  // Helper sets on all demes
			break;
		default:
			throw new RuntimeException("Unknown command-line argument: " + param);
		}
	}

	private static void setParamsFileName(String[] args) {
		for (int i = 0; (i < args.length); i += 2) {
			if ("paramFile".equals(args[i])) {
				Parameters.paramFile = args[i + 1];
			}
		}
	}
	
	public static void main(String[] args) {
		// Initialize static parameters. But first setup parameter
		// file name from command-line args (if one has been specified)
		final long startTime = System.currentTimeMillis();
		setParamsFileName(args);
		Parameters.load(Parameters.paramFile);		
		Parameters.initialize();
		// Process other command-line arguments supplied by the user (if any)
		for (int i = 0; (i < args.length); i += 2) {
			processParam(args[i], args[i + 1]);
		}
		// Skew species proportions for sensitivity tests
		if (speciesSkew != 0) {
			Parameters.skewSpecies(speciesSkew);
		}
		// Run the simulation
		SimulationSpecies sim = new SimulationSpecies();
		sim.run();	
		final long   endTime = System.currentTimeMillis();
		final double elapsedTime = (endTime - startTime) / 1000.0;
		System.out.println("Elapsed time: " + elapsedTime + " seconds.");
	}
}

