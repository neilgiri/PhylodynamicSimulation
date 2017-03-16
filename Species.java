import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * <p>A wrapper class to encapsulate information about a given bird
 * species in a deme. Each deme can contain optional list of bird
 * species, each with its own population ratio, life expectancy, and
 * other attributes. All birth and death life cycle activities are
 * performed at a per-species level.</p> 
 * 
 * <p>The species information is loaded from the parameters file
 * specified by the user in the following YAML format:
 * 
 * <pre>
 * BirdSpecies:
 *   -
 *     - deme: 1              # Index number of deme associated with species
 *     - Name: bird1          # Bird species name
 *       Fraction: 0.35       # Fraction of the population for this species
 *       broodStart: 30       # Day-of-year when brooding starts
 *       broodEnd: 107        # Day-of-year when brooding ends
 *       birthRate: 0.00214   # births per individual/day during the year.
 *       deathRate: 0.000091  # in deaths per individual per day, 1/3 years = 0.00091
 *
 *    - Name: bird2           # Bird species name
 *       Fraction: 0.5        # Fraction of the population for this species
 *       broodStart: 0        # Day-of-year when brooding starts
 *       broodEnd: 365        # Day-of-year when brooding ends
 *       birthRate: 0.000091  # births per individual/day during the whole year.
 *       deathRate: 0.000091  # in deaths per individual per day, 1/3 years = 0.00091
 *     
 *    - Name: other           # Bird species name
 *       Fraction: 0.15       # Fraction of the population for this species
 *       broodStart: 25       # Day-of-year when brooding starts
 *       broodEnd: 100        # Day-of-year when brooding ends
 *       birthRate: 0.0091    # births per individual/day during the whole year
 *       deathRate: 0.000091  # in deaths per individual per day, 1/3 years = 0.00091
 * </pre>
 * 
 * </p>
 * 
 * The above YAML file is part of the overall parameters that are supplied as
 * configuration to the simulation.  The data is typically processed in the 
 * following manner:
 * 
 * <pre>
 * 		ArrayList<Object> speciesList = (ArrayList<Object>) map.get("BirdSpecies");
 * 		for (int i = 0; (i < speciesList.size()); i++) {
 * 			Map<String, Object> speciesInfo = (Map<String, Object>) speciesList.get(i);
 *          Species si = new Species(speciesInfo);
 * 		}
 * </pre>
 * 
 *
 */
public class Species {
	/** A user-specified species name. This is just for identification. */ 
	public final String name;
	
	/** Fraction of total population that is this specific species. This value is
	 * used when creating the initial population of birds in a deme.
	 */
	public final double fraction;
	
	/** The births per individual per day during a whole year.  This value is
	 * automatically scaled based on the specified brooding period. For example
	 *  if average life expectancy  of the species is 5.2 years, and the brooding
	 *  season is then this value is computed as:
	 * 1.0 / (5.2 * 365) = 0.00052 
	 */
	public final double birthRate;
	
	/** Starting day in each year when birds start brooding. This day indicates the
	 * day when eggs actually start hatching.  This value is also used to compute
	 * broodBirthRate.
	 */
	public final int broodStart;

	/** Starting day in each year when birds stop brooding. This day indicates the
	 * day when all eggs are deemed to have hatched for the season.  This value is 
	 * also used to compute broodBirthRate.
	 */
	public final int broodEnd;

	/** The deaths per individual per day.  For example if average life expectancy
	 * of the species is 5.2 years, then this value is computed as:
	 * 1.0 / (5.2 * 365) = 0.00052.
	 * 
	 * <p>Typically, this value is set to be the same as birthRate to maintain
	 * steady population. However, this value can be set to a higher or lower 
	 * number to model growth in bird population (due to conservation efforts) or
	 * decline in population (due to various factors).</p>
	 * 
	 * @note This value is not impacted by the brooding period as bird die all
	 * the time.
	 */
	public final double deathRate;

	/** Birth rate per individual per day during the brooding period. This value
	 * is automatically calculated based on the specified birth rate, broodingStart,
	 * and broodingEnd values as shown below:
	 * 
	 * broodBirthRate = birthRate * 365 / (broodEnd - broodStart);
	 */
	public final double broodBirthRate;
	
	/** The list of susceptible individuals in this species. Note that 
	 * susceptible population can already contain viruses that are endemic
	 * in them.  But they are not spreading infection via shedding of viruses. 
	 * This is part of the S -> I -> S model. */
	private final ArrayList<Host> susceptibles = new ArrayList<Host>();
	
	/** The list of infected individuals in this species. These are individuals
	 * who are actively spreading infection of a given virus. The virus 
	 * information is part of the host.
	 */
	private final ArrayList<Host> infecteds = new ArrayList<Host>();
	
	/** This is the transcendental class, immune to all forms of virus. Typically
	 * in avian influenza, this compartment is empty. 
	 */
	private final ArrayList<Host> recovereds   = new ArrayList<Host>();  
	
	/**
	 * Constructor to create species object from YAML parameters.
	 * 
	 * @param yamlParms The subset of parameters associated with this
	 * bird species obtained from a YAML file.  See top-level class on how
	 * this data is created.
	 */
	public Species(Map<String, Object> yamlParams) {
		name       = (String) yamlParams.get("Name");
		fraction   = (double) yamlParams.get("Fraction");
		broodStart = (int)    yamlParams.get("broodStart");
		broodEnd   = (int)    yamlParams.get("broodEnd");
		birthRate  = (double) yamlParams.get("birthRate");
		deathRate  = (double) yamlParams.get("deathRate");
		// Using the supplied information compute the brood birth rate
		broodBirthRate = birthRate * 365 / (broodEnd - broodStart);
	}
	
	/**
	 * Constructor to create species object with different fraction.
	 * 
	 * @param src The source species object from where rest of the
	 * information is to be obtained.
	 * 
	 * @param frac The fraction value to be associated with this
	 * species.
	 */
	public Species(final Species src, final double frac) {
		name       = src.name;
		fraction   = frac;
		broodStart = src.broodStart;
		broodEnd   = src.broodEnd;
		birthRate  = src.birthRate;
		deathRate  = src.deathRate;
		// Using the supplied information compute the brood birth rate
		broodBirthRate = src.broodBirthRate;
	}
	
	/**
	 * Method to create a given number of hosts in the S, I, R compartments
	 * for this host.
	 * 
	 * @param deme The deme with which this host is to be associated.
	 * 
	 * @param initialSus The total number of susceptible individuals. This value
	 * is multiplied by fraction to determine the number of individuals 
	 * to be created for this species.
	 * 
	 * @param initialInf The total number of infected individuals. This value
	 * is multiplied by fraction to determine the number of individuals 
	 * to be created for this species.
	 * 
	 * @param initialRec The total number of recovered individuals. This value
	 * is multiplied by fraction to determine the number of individuals 
	 * to be created for this species.
	 */
	public void createHosts(final int deme, final int initialSus, final int initialInf,
			final int initialRec) { 
		final int numSus = (int) (initialSus * fraction);
		final int numInf = (int) (initialInf * fraction);
		final int numRec = (int) (initialRec * fraction);
		// Create the specified number of susceptible individuals
		for (int i = 0; i < numSus; i++) {	
			susceptibles.add(new Host());
		}
		// Create the specified number of infected individuals
		for (int i = 0; (i < numInf); i++) {
			Virus v = new Virus(Parameters.urVirus, deme);
			infecteds.add(new Host(v));
		}	
		// Create the specified number of recovered individuals
		for (int i = 0; (i < numRec); i++) {		
			recovereds.add(new Host());
		}
	}

	/** Return the number of susceptible individuals of this species.
	 * 
	 * @return The number/count of susceptible individuals of this species.
	 */
	public int getS() {
		return susceptibles.size();
	}

	/** Return the number of infective individuals of this species.
	 * 
	 * @return The number/count of infective individuals of this species.
	 */
	public int getI() {
		return infecteds.size();
	}

	/** Return the number of recovered individuals of this species.
	 * 
	 * @return The number/count of recovered individuals of this species.
	 */
	public int getR() {
		return recovereds.size();
	}

	/** Return the total number of individuals of this species.
	 * 
	 * @return The total number/count of susceptible + infective + 
	 * recovered individuals.
	 */
	public int getN() {
		return (getS() + getI() + getR());
	}
	
	/** Get a susceptible host at a given index.
	 * 
	 * @param index The index at which the susceptible host is to be returned.
	 * This value must be in the range 0 < index < getS()
	 * 
	 * @return The susceptible host at the given location.
	 */
	public Host getHostS(int index) {
		return susceptibles.get(index);
	}
	
	/** Get a infective host at a given index.
	 * 
	 * @param index The index at which the infective host is to be returned.
	 * This value must be in the range 0 < index < getI()
	 * 
	 * @return The infective host at the given location.
	 */
	public Host getHostI(int index) {
		return infecteds.get(index);
	}

	/** Get a recovered host at a given index.
	 * 
	 * @param index The index at which the recovered host is to be returned.
	 * This value must be in the range 0 < index < getI()
	 * 
	 * @return The recovered host at the given location.
	 */
	public Host getHostR(int index) {
		return recovereds.get(index);
	}

	/** Get a host from susceptible, infected, or recovered population.
	 * 
	 * @return A random host from this species.
	 */
	public Host getRandomHost() {
		// Figure out whether to pull from S, I or R
		int index = Random.nextInt(0, getN() - 1);
		if (index < getS()) {
			return getHostS(index);
		}
		index -= getS();  // account for #susceptible skipped
		if (index < getI()) {
			return getHostI(index);
		}
		index -= getI();  // account for #infective skipped
		return getHostR(index);
	}
	
	/** Simulate growth of population for this species.
	 * 
	 * This method must be used to simulate growth in the species population
	 * through the addition of new susceptible hosts.  The population is
	 * grown only if the dayOfYear is within the brooding period for this
	 * species.  
	 * 
	 * @param dayOfYear The current day of the year in the simulation. This
	 * value must be in the range 0 <= dayOfYear < 365.
	 */
	public void grow(int dayOfYear) {
		if ((broodStart < dayOfYear) && (dayOfYear < broodEnd)) {
			double totalBirthRate = getN() * broodBirthRate * Parameters.deltaT;
			int births = Random.nextPoisson(totalBirthRate);
			for (int i = 0; (i < births); i++) {
				Host h = new Host();
				susceptibles.add(h);
			}
		}
	}

	/** Convenience method to remove an entry from a given host list.
	 * 
	 * This method performs a quick removal operation by swapping the
	 * entry to be removed with the last element and then removing 
	 * the last entry.
	 * 
	 * @param list The list from which the entry is to be removed.
	 * 
	 * @param delIdx The index of the entry to be removed. 
	 */
	private void remove(ArrayList<Host> list, final int delIdx) {
		final int lastIdx   = list.size() - 1;
		final Host lastHost = list.get(lastIdx);
		list.set(delIdx, lastHost);
		list.remove(lastIdx);		
	}
	
	/** Simulate general death in a specific compartment (S, I, or R)
	 * 
	 * This method is a helper method that is used to simulate death in 
	 * the species population through removal of hosts in a given list.
	 * 
	 * @param compartment The list of hosts in a given compartment. Hosts
	 * are removed from this list to simulate death in hosts. 
	 * 
	 */
	private void decline(ArrayList<Host> compartment) {
		double totalDeathRate = compartment.size() * deathRate * Parameters.deltaT;
		int deaths = Random.nextPoisson(totalDeathRate);
		while ((deaths > 0) && (!compartment.isEmpty())) {
			// Randomly remove a host in the list.
			final int lastIdx = compartment.size() - 1;
			final int delIdx  = Random.nextInt(0, lastIdx);  // index to delete
			remove(compartment, delIdx);
			// Track number of deaths
			deaths--;
		}
	}
	
	/** Simulate general death in the S, I, R population for this species.
	 * 
	 * This method must be used to simulate death in the species population
	 * through the removal of S, I, and R hosts.  Deaths in each one of the
	 * compartments (namely: S, I, and R) are accomplished via a helper
	 * method. 
	 */
	public void decline() {
		decline(susceptibles);
		decline(infecteds);
		decline(recovereds);
	}
	
	/**
	 * Convenience method to reset-and-move hosts from the given compartment
	 * into the susceptible compartment.
	 * 
	 * @param compartment The compartment in which a hosts are to be reset.
	 * If this compartment is not the susceptible then the reset host is 
	 * removed from this compartment and placed into the susceptible compartment.
	 */
	private void makeSusceptible(ArrayList<Host> compartment) {
		final double totalConvRate = getR() * birthRate * Parameters.deltaT;
		int conversions = Random.nextPoisson(totalConvRate);
		while ((conversions > 0) && (!compartment.isEmpty())) {
			// Randomly remove a host in the infected list
			final int lastIdx  = compartment.size() - 1;
			final int delIdx   = Random.nextInt(0, lastIdx); // index to reset
			final Host susHost = compartment.get(delIdx);
			susHost.reset();   // Clear history
			// Remove susceptible host from the current compartment if 
			// it is not already the susceptible compartment.
			if (compartment != susceptibles) {
				// To make the removal faster, use a swap and remove approach
				remove(compartment, delIdx);
				// Add reset host to the susceptible compartment
				susceptibles.add(susHost);
			}
		}
	}
	
	/**
	 * Convenience method to reset and move (if necessary) hosts from the 
	 * three compartments (namely S, I, and R) into the susceptible 
	 * compartment.
	 */
	public void makeSusceptible() {
		makeSusceptible(susceptibles);
		makeSusceptible(infecteds);
		makeSusceptible(recovereds);
	}

	/**
	 * Convenience method to try and infect a given susceptible host 
	 * with virus from an infected host.
	 * 
	 * This method performs checks between the susceptible host's acquired
	 * immunity (from previous infections) against the viral strain from
	 * the infected host (infHost).  If the pheotypes are different then
	 * the infection proceeds based on the risk probability. 
	 *  
	 * @param susIdx Index of the susceptible host to be infected.  This value
	 * must be in the range 0 <= susIdx < getS().
	 * 
	 * @param infHost The infecting host. This host must have a valid virus
	 * that it is currently spreading.
	 * 
	 * @param deme The number of the deme.
	 * 
	 * @return This method returns the host that was infected, if the infection
	 * was successful.  Otherwise this method returns null to indicate an infection
	 * did not occur.
	 */
	public Host infect(int susIdx, Host infHost, int deme, Environment environment) {
		return infect(susIdx, infHost.getInfection(), deme, environment);
	}
	
	/**
	 * Convenience method to infect host with a given virus.
	 * 
	 * This method performs checks between the susceptible host's acquired
	 * immunity (from previous infections) against the viral strain from
	 * the infected host (infHost).  If the pheotypes are different then
	 * the infection proceeds based on the risk probability. 
	 *  
	 * @param susIdx Index of the susceptible host to be infected.  This value
	 * must be in the range 0 <= susIdx < getS().
	 * 
	 * @param infHost The infecting host. This host must have a valid virus
	 * that it is currently spreading.
	 * 
	 * @param deme The number of the deme.
	 * 
	 * @return This method returns the host that was infected, if the infection
	 * was successful.  Otherwise this method returns null to indicate an infection
	 * did not occur.
	 */
	public Host infect(int susIdx, Virus virus, int deme, Environment environment) {
		Host susHost = susceptibles.get(susIdx);						
		// Check to see if the susceptible hosts acquired immunity
		// prevents a new infection by virus v
		Phenotype p            = virus.getPhenotype();		
		Phenotype[] history    = susHost.getHistory();
		double chanceOfSuccess = p.riskOfInfection(history);
		if (Random.nextBoolean(chanceOfSuccess)) {
			// Remove susceptible host from the list.
			remove(susceptibles, susIdx);
			// Infect the susceptible host with virus v
			susHost.infect(virus, deme, environment);
			// Now add the infected host to the appropriate compartment
			infecteds.add(susHost);
			// Infection successful
			return susHost;
		}
		// No infection occurred
		return null;
	}

	/**
	 * Convenience method to simulate end of infective period by an infected
	 * host.  
	 * 
	 * The infection in the host is cleared and the host is moved either to
	 * recovered or susceptible list depending on whether the infection is
	 * transcendental or not. 
	 * 
	 * @param infIdx Index of the infected host to be recovered.  This value
	 * must be in the range 0 <= infIdx < getI().
	 * 
	 * @param env The environment to use to report clearing an infection.
	 */
	public void recoverHost(final int infIdx, Environment env) {
		Host h = infecteds.get(infIdx);
		h.clearInfection(env);
		remove(infecteds, infIdx);

		if (Parameters.transcendental) {
			recovereds.add(h);
		} else {
			susceptibles.add(h);
		}
	}
	
	/**
	 * Convenience method to simulate loss of immunity in a given recovered
	 * host.
	 * 
	 * The recovered host is moved into the susceptible compartment.
	 *  
	 * @param recIdx Index of the recovered host to be made susceptible.  
	 * This value must be in the range 0 <= recIdx < getR().
	 */
	public void looseImmunity(final int recIdx) {
		Host h = recovereds.get(recIdx);
		remove(recovereds, recIdx);
		susceptibles.add(h);
	}
	
	/**
	 * Simulate vaccination of a susceptible host.
	 * 
	 * @param susIdx Index of the susceptible host to be infected.  This value
	 * must be in the range 0 <= susIdx < getS().
	 */
	public void vaccinate(final int susIdx) {
		Host h = susceptibles.get(susIdx);
		remove(susceptibles, susIdx);
		recovereds.add(h);
	}
	
	/**
	 * Cull (remove) an infected host.
	 * 
	 * @param infHost The infecting host. This host must have a valid virus
	 * that it is currently spreading.
	 */
	public void cull(final int infIdx) {
		remove(infecteds, infIdx);
	}
	
	/**
	 * Mark the current set of viruses (in the infected hosts) as the trunk of
	 * new generation of viruses to be created.
	 */
	public void makeTrunk() {
		for (Host h : infecteds) {
			Virus v = h.getInfection();
			v.makeTrunk();
			while (v.getParent() != null) {
				v = v.getParent();
				if (v.isTrunk()) {
					break;
				} else {
					v.makeTrunk();
				}
			}
		}
	}

	/**
	 * Clear out all compartments in this species.
	 */
	public void clear() {
		susceptibles.clear();
		infecteds.clear();
		recovereds.clear();
	}
	
	/**
	 * Print information about all the hosts in the given host list.
	 * 
	 * @param stream The output stream to which the host information is to 
	 * be printed.
	 * 
	 * @param hostList The list of hosts whose information is to be printed.
	 * 
	 * @param deme The number of the deme that contains this list.
	 */
	private void printHostPopulation(PrintStream stream, 
			ArrayList<Host> hostList, int deme) {
		for (Host h : hostList) {
			stream.print(deme + ":");
			h.printInfection(stream);
			stream.print(":");
			h.printHistory(stream);
			stream.println();
		}
	}

	/**
	 * Print information about the susceptible, infected, and recovered 
	 * hosts in this species.
	 * 
	 * @param stream The output stream to which the host information is to 
	 * be printed.
	 * 
	 * @param deme The number of the deme that contains this list.
	 */
	public void printHostPopulation(PrintStream stream, int deme) {
		printHostPopulation(stream, susceptibles, deme);
		printHostPopulation(stream, infecteds,    deme);
		printHostPopulation(stream, recovereds,   deme);
	}
	
}
