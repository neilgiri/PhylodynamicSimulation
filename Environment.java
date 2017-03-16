import java.util.ArrayList;
import java.util.Map;

/**
 * Class to model longer term environmental reservoir in which viruses live.
 * Each infective host starts shedding viruses to the environment. This class
 * essentially encapsulates the list of viruses currently present in the
 * environment. Once the infective host recovers, the concentration of the
 * virus exponentially decays.  The parameters used by this class are:  
 * 
 * 
 * <p>The conceptual mechanism has been adapted from Roche et. al at:
 * http://bedford.io/pdfs/papers/roche-aiv-persistence.pdf. See supplementary
 * material for suggested parameter values 
 * http://bedford.io/pdfs/papers/roche-aiv-persistence-supp.pdf 
 * </p>
 * 
 */
public class Environment {
	/**
	 * The environmental up take rate per unit volume. This value is
	 * typically set to 6.73 based on information from Roche et. al --
	 * http://bedford.io/pdfs/papers/roche-aiv-persistence.pdf
	 */
	public double envUpTakeRate;

	/**
	 * The ID50 value for the virus.  ID50 indicates The infective dose of 
	 * microorganisms that will cause 50% of exposed individuals to become ill.
	 * 
	 */
	private double id50;

	/** The environmental durability of the virus.  This is a period value
	 * and is represented in days. The value is typically 20 days that the
	 * virus on an average can exist in the environment. This value is 
	 * used in conjunction with seasonalAmp value and sheddingRate. 
	 */
	private double envDurability;

	/** The rate at which an infected host sheds viruses to the environment.
	 * This value is typically very lrage at 1e12 (10^2) virons per day.
	 * see: B. Roche, et al., Infect. Genet. Evol. 9, 800 (2009).
	 */
	private double sheddingRate;
	
	/**
	 * The seasonal amplitude to be used for modulating the environmental
	 * durability of the virus.  This value is used to model temperature
	 * differences in the deme.
	 */
	private double seasonalAmp;
	
	/**
	 * The list of viruses currently present in the environment. New viruses 
	 * are added as the birds get infected by it. Viruses are removed once
	 * their durability drops below a specified threshold.  
	 * 
	 * <p> Note that the reference to the viruses in this list may also exist
	 * as references in the Host that is shedding the virus.  The host that is
	 * shedding the virus updates Virus.envVolume indicating the volume of this
	 * virus in the environment.  This approach (where environmental parameter is
	 * actually updated by the Host rather than the Environment) is done as 
	 * a performance optimization</p>  
	 */
	private final ArrayList<Virus> virusList = new ArrayList<Virus>();
	
	/**
	 * A constant to refer to PI * 2
	 */
	private final double pi2 = Math.PI * 2;
	
	private int maxSize = 0;
	
	/**
	 * Constructor to create an object from YAML parameters.
	 * 
	 * @param yamlParms The subset of parameters associated with the
	 * environment obtained from a YAML file.  See the Parameters class
	 * on how it creates this object.
	 */
	public Environment(Map<String, Object> yamlParams) {
		envUpTakeRate       = (double) yamlParams.get("envUpTakeRate");
		id50                = (double) yamlParams.get("id50");
		envDurability       = (double) yamlParams.get("envDurability");
		seasonalAmp         = (double) yamlParams.get("seasonalAmp");
		// Some processing to make specification of shedding rate easier
		String shedRateParam= yamlParams.get("sheddingRate").toString();
		sheddingRate        = Double.parseDouble(shedRateParam);
	}

	/**
	 * Clear the viruses in the environment for the current day. 
	 *
	 * @param deme The deme with which this environment is associated.
	 */
	public void clearViruses(int deme, double step) {
		// First compute the seasonal (sesn) modulation based on north or south
		// hemisphere to account for durability variations based on temperature.
		double sesnOffset = Parameters.demeOffsets[deme];
		double sesnFactor = (pi2 * Parameters.getDate()) + (pi2 * sesnOffset);
		// Get the virus clearance rate for the current simulation day.
		double clearRate  = (1.0 + (seasonalAmp * Math.sin(sesnFactor))) / envDurability;
		clearRate *= step;
		double shedRate = sheddingRate * step;
		// Update virus volumes in the environment.
		int idx = 0;
		while (idx < virusList.size()) {
			final Virus v = virusList.get(idx);
			final double currVol = v.getVolume();
			final double numShed = v.getNumShedding();
			// Compute volume of virus for next time step based on current volume,
			// number of hosts shedding and environment. See Page S-15 in 
			// http://bedford.io/pdfs/papers/roche-aiv-persistence-supp.pdf
			double nextVol = shedRate  * numShed / clearRate;
			nextVol += (Math.exp(-clearRate * 30) * currVol);
			// Set the new volume of viruses
			v.setVolume(nextVol);
			// Remove the virus if needed
			if ((nextVol < id50) && (numShed == 0)) {
				// The volume is too low. Remove this virus.
				final int size  = virusList.size() - 1;
				Virus lastVirus = virusList.get(size);
				virusList.set(idx,  lastVirus);
				virusList.remove(size);
			} else {
				idx++;
			}
		}
		maxSize = Math.max(maxSize, virusList.size());
	}
	
	/**
	 * Add a virus to the environment.
	 * 
	 * Viruses should be added to the environment only once, the first time a
	 * host starts shedding it.
	 * 
	 * @param v The virus to be added to the environment.
	 */
	public void add(Virus v) {
		assert( v.getNumShedding() == 1);
	}
	
	/**
	 * Get a random virus from the environment. 
	 * 
	 * @return Virus from the environment. If a virus does not exist,
	 * then this method returns null.
	 */
	public Virus getVirus() {
		final int index = Random.nextInt(0, virusList.size());
		if (index < virusList.size()) {
			return virusList.get(index);
		}
		return null;
	}
	
	/** 
	 * Convenience method to override environment parameter settings
	 * in this object.  The overrides are typically passed-in as
	 * command-line arguments. The overrides are handy to perform
	 * different types of analysis, such as Generalized Sensitivity
	 * Analysis (GSA).  This method is directly invoked from Antigen.java 
	 * just after the default values are loaded from the given YML file.
	 * 
	 * @param param The name of the parameter
	 * @param value The value to be set for it.
	 */
	public void setParam(final String param, final double value) {
		switch (param) {
		case "envUpTakeRate": 
			envUpTakeRate = value; 
			break;
		case "id50": 
			id50 = value;
			break;
		case "envDurability":
			envDurability = value;
			break;
		case "seasonalAmp":
			seasonalAmp = value;
			break;
		case "sheddingRate":
			sheddingRate = value;
			break;
		default:
			System.err.println("Unrecognized environment parameter: " + 
					param + ".\nAborting!\n");
			System.exit(1);
		}
		// Print a message to confirm override
		System.out.println("Environment parameter " + param + 
				" value overriden to " + value);
	}
}
