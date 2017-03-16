/* Stores parameters for use across simulation */
/* Start with parameters in source, implement input file later */
/* A completely static class.  */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Parameters {
	
	// global parameters
	public static double day = 0;
	public static Virus urVirus = null;
	public static Phenotype urImmunity = null;		
	
	// simulation parameters
	public static int burnin = 0;
	public static int endDay = 5000; 
	public static double deltaT = 0.1;                                 	// number of days to move forward in a single timestep
	public static double broodSeasonStart = 30.0;
	public static double broodSeasonEnd = 107.0;	
	public static int printStep = 10;									// print to out.timeseries every week
	public static double tipSamplingRate = 0.0002;						// in samples per deme per day
	public static int tipSamplesPerDeme = 1000;
	public static boolean tipSamplingProportional = true;				// whether to sample proportional to prevalance
	public static double treeProportion = 0.1;							// proportion of tips to use in tree reconstruction
	public static int diversitySamplingCount = 1000;					// how many samples to draw to calculate diversity, Ne*tau, serial interval
	public static int netauWindow = 100;								// window in days to calculate Ne*tau		
	public static boolean repeatSim = true;								// repeat simulation until endDay is reached?
	public static boolean immunityReconstruction = false;				// whether to print immunity reconstruction to out.immunity
	public static boolean memoryProfiling = false;						// requires -javaagent:classmexer.jar to run
	public static double yearsFromMK = 1.0;
	public static boolean pcaSamples = false;							// whether to rotate and flip virus tree
	public static boolean reducedOutput = false;						// whether to output only out.summary and out.timeseries
	public static boolean detailedOutput = false;						// whether to output out.hosts and out.viruses files enabling checkpointing
	public static boolean restartFromCheckpoint = false;				// whether to load population from out.hosts
	
	// metapopulation parameters
	public static int demeCount = 3;
	public static String[] demeNames = {"north", "tropics", "south"};
	public static int[] initialNs = {1000000,1000000,1000000};	
	
	// host parameters
	public static double birthRate = 0.000091;				// in births per individual per day, 1/30 years = 0.000091
	public static double deathRate = 0.000091;				// in deaths per individual per day, 1/30 years = 0.000091
	public static boolean swapDemography = true;				// whether to keep overall population size constant
	public static double broodBirthRate = 1.7;
	public static double seasonDeathRate = 1.2;
	public static double baseRate = 0.000091;
		
	// epidemiological parameters
	public static int initialI = 10;							// in individuals
	public static int initialDeme = 2;						// index of deme where infection starts, 1..n
	public static double initialPrR = 0.5; 					// as proportion of population
	public static double beta = 0.36; // 0.3					// in contacts per individual per day
	public static double nu = 0.2; //0.2						// in recoveries per individual per day
	public static double betweenDemePro = 0.0005;				// relative to within-deme beta
    public static double culling = 0.1;
    public static double vaccinate = 0.1;
	// transcendental immunity
	public static boolean transcendental = false;
	public static double immunityLoss = 0.01;					// in R->S per individual per day
	public static double initialPrT = 0.0;
	
	// seasonal betas
	public static double[] demeBaselines = {1,1,1};
	public static double[] demeAmplitudes = {0.1,0,0.1};
	public static double[] demeOffsets = {0,0,0.5};				// relative to the year
	
	// phenotype parameters
	public static String phenotypeSpace = "geometric";			// options include: "geometric", "geometric3d", "geometric10d"
	public static double muPhenotype = 0.005; 					// in mutations per individual per day

	// parameters specific to GeometricPhenotype
	public static double smithConversion = 0.1;					// multiplier to distance to give cross-immunity
	public static double homologousImmunity = 0.05;				// immunity raised to antigenically identical virus
	public static double initialTraitA = -6;	
	public static double meanStep = 0.3; 
	public static double sdStep = 0.3; 
	public static boolean mut2D = false;						// whether to mutate in a full 360 degree arc
	public static boolean fixedStep = false;					// whether to fix mutation step size
	
	public static String outputDir = "simulation_output";
	public static String paramFile = "parameters_turkey.yml";

	/** Array list containing the environment for each deme.
	 * 
	 */
	public static final ArrayList<Environment> demeEnvList = 
			new ArrayList<Environment>();

	/** Array list containing the list of species in each deme.
	 * 
	 */
	public static final ArrayList<ArrayList<Species>> demeSpeciesList = 
			new ArrayList<ArrayList<Species>>();

	/**
	 * Dimensionless cross-immunity parameter that is used to determine
	 * propability of infection.  This value is used to map linear distances
	 * between the simulated phenotypes (i.e., number of nucleotide 
	 * changes) to estimate antigenic difference.  The use of this parameter
	 * is discussed in http://bedford.io/pdfs/papers/roche-aiv-persistence-supp.pdf 
	 */
	public static double antigenicShape = 3.0;

	/**
	 * Minimal cross immunity value to be used to determine antigenic distance 
	 * between two simulated phenotypes based on the distance between them.
	 */
	public static double minCrossImmunity = 0.7;
	
	// measured in years, starting at burnin
	public static double getDate() {
		return ((double) day - (double) burnin ) / 365.0;
	}
	
	public static boolean dayIsInteger() {
   		return Math.ceil(day) - Math.floor(day) == 0;
	}		
	
	public static double getSeasonality(int index) {
		double baseline = demeBaselines[index];
		double amplitude = demeAmplitudes[index];
		double offset = demeOffsets[index];
		double beta = baseline + amplitude * Math.cos(2*Math.PI*getDate() + 2*Math.PI*offset);
		return beta;
	}
		
	// initialize
	public static void initialize() {
		urVirus = new Virus();
		urImmunity = PhenotypeFactory.makeHostPhenotype();
	}
	
	public static void load() {
		load("parameters.yml");
	}
	
	// load parameters.yml	
	@SuppressWarnings("unchecked")
	public static void load(final String paramFilePath) {
		try {
			org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
			InputStream input = new FileInputStream(new File(paramFilePath));
			Map<String, Object> map = (Map<String, Object>) yaml.load(input);
			input.close();
			
			System.out.println("Loading parameters from " + paramFilePath);
		
			if (map.get("burnin") != null) {
				burnin = (int) map.get("burnin");
			}
			if (map.get("endDay") != null) {
				endDay = (int) map.get("endDay");
			}
			if (map.get("deltaT") != null) {
				deltaT = (double) map.get("deltaT");
			}
			if (map.get("broodSeasonStart") != null) {
				broodSeasonStart = (double) map.get("broodSeasonStart");
			}
			if (map.get("broodSeasonEnd") != null) {
				broodSeasonEnd = (double) map.get("broodSeasonEnd");
			}		
			if (map.get("printStep") != null) {
				printStep = (int) map.get("printStep");
			}
			if (map.get("tipSamplingRate") != null) {
				tipSamplingRate = (double) map.get("tipSamplingRate");
			}
			if (map.get("tipSamplesPerDeme") != null) {
				tipSamplesPerDeme = (int) map.get("tipSamplesPerDeme");
			}
			if (map.get("tipSamplingProportional") != null) {
				tipSamplingProportional = (boolean) map.get("tipSamplingProportional");
			}
			if (map.get("treeProportion") != null) {
				treeProportion = (double) map.get("treeProportion");
			}
			if (map.get("diversitySamplingCount") != null) {
				diversitySamplingCount = (int) map.get("diversitySamplingCount");	
			}
			if (map.get("netauWindow") != null) {
				netauWindow = (int) map.get("netauWindow");	
			}
			if (map.get("repeatSim") != null) {
				repeatSim = (boolean) map.get("repeatSim");
			}
			if (map.get("immunityReconstruction") != null) {
				immunityReconstruction = (boolean) map.get("immunityReconstruction");
			}
			if (map.get("memoryProfiling") != null) {
				memoryProfiling = (boolean) map.get("memoryProfiling");
			}
			if (map.get("yearsFromMK") != null) {
				yearsFromMK = (double) map.get("yearsFromMK");
			}
			if (map.get("pcaSamples") != null) {
				pcaSamples = (boolean) map.get("pcaSamples");
			}
			if (map.get("reducedOutput") != null) {
				reducedOutput = (boolean) map.get("reducedOutput");	
			}		
			if (map.get("detailedOutput") != null) {			
				detailedOutput = (boolean) map.get("detailedOutput");
			}
			if (map.get("restartFromCheckpoint") != null) {					
				restartFromCheckpoint = (boolean) map.get("restartFromCheckpoint");
			}
			if (map.get("demeCount") != null) {				
				demeCount = (int) map.get("demeCount");
			}
			if (map.get("demeNames") != null) {					
				demeNames = toStringArray((List<String>) map.get("demeNames"));
			}
			if (map.get("initialNs") != null) {				
				initialNs = toIntArray((List<Integer>) map.get("initialNs"));	
			}
			if (map.get("birthRate") != null) {					
				birthRate = (double) map.get("birthRate");
			}
			if (map.get("deathRate") != null) {					
				deathRate = (double) map.get("deathRate");
			}
			if (map.get("swapDemography") != null) {				
				swapDemography = (boolean) map.get("swapDemography");
			}
			if (map.get("seasonDeathRate") != null) {
				seasonDeathRate = (double) map.get("seasonDeathRate");
			}
			if (map.get("broodBirthRate") != null) {
				broodBirthRate = (double) map.get("broodBirthRate");
			}
			if (map.get("baseRate") != null) {
				baseRate = (double) map.get("baseRate");
			}
			if (map.get("initialI") != null) {				
				initialI = (int) map.get("initialI");
			}
			if (map.get("initialDeme") != null) {				
				initialDeme = (int) map.get("initialDeme");
			}
			if (map.get("initialPrR") != null) {				
				initialPrR = (double) map.get("initialPrR");
			}
			if (map.get("beta") != null) {				
				beta = (double) map.get("beta");
			}
			if (map.get("nu") != null) {				
				nu = (double) map.get("nu");
			}
			if (map.get("betweenDemePro") != null) {				
				betweenDemePro = (double) map.get("betweenDemePro");
			}
			if (map.get("culling") != null) {
			    culling = (double) map.get("culling");
			}
			if (map.get("vaccinate") != null) {
			    vaccinate = (double) map.get("vaccinate");
			}
			if (map.get("transcendental") != null) {			
				transcendental = (boolean) map.get("transcendental");
			}
			if (map.get("immunityLoss") != null) {				
				immunityLoss = (double) map.get("immunityLoss");
			}
			if (map.get("initialPrT") != null) {					
				initialPrT = (double) map.get("initialPrT");
			}
			if (map.get("demeBaselines") != null) {
				demeBaselines = toDoubleArray((List<Double>) map.get("demeBaselines"));	
			}
			if (map.get("demeAmplitudes") != null) {
				demeAmplitudes = toDoubleArray((List<Double>) map.get("demeAmplitudes"));
			}
			if (map.get("demeOffsets") != null) {
				demeOffsets = toDoubleArray((List<Double>) map.get("demeOffsets"));
			}	
			if (map.get("phenotypeSpace") != null) {					
				phenotypeSpace = (String) map.get("phenotypeSpace");
			}
			if (map.get("muPhenotype") != null) {				
				muPhenotype = (double) map.get("muPhenotype");
			}
			if (map.get("smithConversion") != null) {				
				smithConversion = (double) map.get("smithConversion");
			}
			if (map.get("homologousImmunity") != null) {				
				homologousImmunity = (double) map.get("homologousImmunity");
			}
			if (map.get("initialTraitA") != null) {				
				initialTraitA = (double) map.get("initialTraitA");
			}
			if (map.get("meanStep") != null) {				
				meanStep = (double) map.get("meanStep");
			}
			if (map.get("sdStep") != null) {				
				sdStep = (double) map.get("sdStep");
			}
			if (map.get("mut2D") != null) {				
				mut2D = (boolean) map.get("mut2D");	
			}
			if (map.get("fixedStep") != null) {				
				fixedStep = (boolean) map.get("fixedStep");	
			}	
			// Load the list of species in the various demes if specified.
			if (map.get("BirdSpecies") != null) {
				loadSpeciesList(map);
			}
			// Load the environment information if specified.
			if (map.get("Environment") != null) {
				loadEnvironment(map);
			}
			// Load antigenic shape and cross immunity values
			if (map.get("antigenicShape") != null) {
				antigenicShape = (double) map.get("antigenicShape");
			}
			if (map.get("minCrossImmunity") != null) {
				minCrossImmunity = (double) map.get("minCrossImmunity");
			}			
		} catch (IOException e) {
			System.out.println("Cannot load " + paramFilePath + ", using defaults");
		}		
	
	}
	
	public static ArrayList<Species> getSpeciesList(int deme) {
		return demeSpeciesList.get(deme);
	}

	public static Environment getEnvironment(int deme) {
		return demeEnvList.get(deme);
	}

	/**
	 * Helper method to load environment information for various demes.
	 * 
	 * @param map The YAML map with data from the parameters file.
	 */
	@SuppressWarnings("unchecked")
	private static void loadEnvironment(Map<String, Object> map) {
		// First resize demeSpecices list to desired value.
		while (demeEnvList.size() < demeCount) {
			demeEnvList.add(null);
		}
		ArrayList<Object> envList = (ArrayList<Object>) map.get("Environment");
		for (int deme = 0; (deme < envList.size()); deme++) {
			Map<String, Object> envInfo = (Map<String, Object>) envList.get(deme);
			int demeNum = (int) envInfo.get("deme");
			Environment env = new Environment(envInfo);
			// Add species list to the appropriate deme entry.
			demeEnvList.set(demeNum, env);
		}
	}
	
	/** Helper method to load species for various demes.
	 * 
	 * @param map The YAML map with data from the parameters file.
	 */
	@SuppressWarnings("unchecked")
	private static void loadSpeciesList(Map<String, Object> map) {
		// First resize demeSpecices list to desired value.
		while (demeSpeciesList.size() < demeCount) {
			demeSpeciesList.add(null);
		}
		ArrayList<Object> demeList = (ArrayList<Object>) map.get("BirdSpecies");
		for (int deme = 0; (deme < demeList.size()); deme++) {
			ArrayList<Object> speciesInfoList = (ArrayList<Object>) demeList.get(deme);
			int demeNum = -1;  // To be filled-in below.
			final ArrayList<Species> speciesList = new ArrayList<Species>();
			for (int i = 0; (i < speciesInfoList.size()); i++) {
				Map<String, Object> speciesInfo = 
					(Map<String, Object>) speciesInfoList.get(i);
				// This entry can either be a deme entry to species information.
				if (speciesInfo.get("deme") != null) {
					demeNum = (int) speciesInfo.get("deme");
				} else {
					// Species information entry
					speciesList.add(new Species(speciesInfo));
				}
			}
			// Add species list to the appropriate deme entry.
			demeSpeciesList.set(demeNum, speciesList);
		}
	}
	
	/**
	 * Method to skew the percentage of species by a given value for 
	 * sensitivity analysis.  This method adds the given skew value
	 * from the bird species with highest population while evenly
	 * removing from other species. Species with zero (or less) fractions
	 * are then removed from the deme.
	 * 
	 * @param skew The skew in species population to be introduced in
	 * the given deme.
	 * 
	 * @param demeNum The deme in which the population skew is to be
	 * introduced.
	 */
	private static void skewSpecies(double skew, final int demeNum) {
		// First find the species with max population.
		ArrayList<Species> speciesList = demeSpeciesList.get(demeNum);
		if (speciesList.size() < 2) {
			System.err.println("Too few species to introduce skew.");
			return;  // Too few entries to introduce skew
		}
		int maxIndex = 0;
		for (int i = 1; (i < speciesList.size()); i++) {
			if (speciesList.get(i).fraction > speciesList.get(maxIndex).fraction) {
				maxIndex = i;
			}
		}
		// Ensure that the skew does not exceed max fraction value
		if (Math.abs(skew) > speciesList.get(maxIndex).fraction) {
			System.err.println("Skew of " + skew + " reset to maximum " +
					"fraction of: " + speciesList.get(maxIndex).fraction);
			skew = speciesList.get(maxIndex).fraction * Math.signum(skew);
		}
		// Now add the skew to the population without it exceeding 1.0
		final int speciesCount = speciesList.size() - 1;
		for (int i = 0; (i < speciesList.size()); i++) {
			double speSkew = (i == maxIndex) ? skew : (-skew / speciesCount);
			double netFrac = speciesList.get(i).fraction + speSkew;
			if ((netFrac < 0) || (netFrac > 1.0)) {
				System.err.println("Fraction of " + speciesList.get(i).name +
						" become: " + netFrac);
				netFrac = Math.max(0, Math.min(1.0, netFrac));
			}
			// Update entry with new fractional value
			speciesList.set(i, new Species(speciesList.get(i), netFrac));
		}
		// Remove entries with zero population
		int idx = 0;
		double totalFrac = 0;
		while (idx < speciesList.size()) {
			if (speciesList.get(idx).fraction <= 0.0) {
				speciesList.remove(idx);
			} else {
				totalFrac += speciesList.get(idx).fraction;
				idx++;
			}
		}
		if (totalFrac != 1.0) {
			System.out.println("The total  fraction after skewing was not 1.0. It was: " + totalFrac);
		}
	}
	
	/**
	 * Method to skew the percentage of species by a given value for 
	 * sensitivity analysis.  This method adds the given skew value
	 * to population in all the demese using helper method.
	 * 
	 * @param skew The skew in species population to be introduced in
	 * the given deme.
	 * 
	 * @param demeNum The deme in which the population skew is to be
	 * introduced.
	 */
	public static void skewSpecies(final double skew) {
		for (int i = 0; (i < demeSpeciesList.size()); i++) {
			skewSpecies(skew, i);
		}
	}
	
	private static int[] toIntArray(List<Integer> list) {
  		int[] ret = new int[list.size()];
  		for (int i = 0; i < ret.length; i++) {
    		ret[i] = list.get(i);
    	}
  		return ret;
	}
	
	private static double[] toDoubleArray(List<Double> list) {
  		double[] ret = new double[list.size()];
  		for (int i = 0; i < ret.length; i++) {
    		ret[i] = list.get(i);
    	}
  		return ret;
	}	
	
	private static String[] toStringArray(List<String> list) {
  		String[] ret = new String[list.size()];
  		for (int i = 0; i < ret.length; i++) {
    		ret[i] = list.get(i);
    	}
  		return ret;
	}	
	
}