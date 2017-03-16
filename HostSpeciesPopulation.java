/* A population of host individuals */

import java.io.PrintStream;
import java.util.ArrayList;

public class HostSpeciesPopulation {
	// fields
	private int deme;
	private String name;	
	private int cases;	

	/**
	 * The susceptible, infected, and recovered population are organized on a
	 * per-species basis to make birth and death operations easier. 
	 */
	private ArrayList<Species> speciesList = null;

	private Environment environment = null;
	
	private double diversity;
	private double tmrca;
	private double netau;	
	private double serialInterval;
	private double antigenicDiversity;		

	private int newContacts;
	private int newRecoveries;

	/**
	 * Variable to track the current offset within a day. This is used to
	 * update viral population in the environment. See stepForward() method. 
	 */
	private double dayOffset = 0;
	
	// construct population, using Virus v as initial infection
	public HostSpeciesPopulation(int d) {
		// basic parameters
		deme = d;
		name = Parameters.demeNames[deme];
		if (Parameters.demeSpeciesList.size() <= deme) {
			throw new RuntimeException("Species not specified for deme #" + deme);
		}
		// Setup reference to environment
		environment = Parameters.getEnvironment(deme);
		if (environment == null) {
			throw new RuntimeException("Environment not specified.");
		}
		speciesList = Parameters.getSpeciesList(deme);
		// Create the individuals of different species in this deme
		// via helper method.
		reset();
	}

	// construct checkpointed host population and infecting viruses
	public HostSpeciesPopulation(int d, boolean checkpoint) {
		if (checkpoint == true) {
			throw new RuntimeException("Not correctly implemented to support species.");
		}
	}

	// accessors
	public int getN() {
		int sum = 0;
		for (Species s: speciesList) {
			sum += s.getN();
		}
		return sum;
	}

	public int getS() {
		int sum = 0;
		for (Species s: speciesList) {
			sum += s.getS();
		}
		return sum;
	}

	public int getI() {
		int sum = 0;
		for (Species s: speciesList) {
			sum += s.getI();
		}
		return sum;
	}

	public int getR() {
		int sum = 0;
		for (Species s: speciesList) {
			sum += s.getR();
		}
		return sum;
	}

	public double getPrS() {
		return (double) getS() / (double) getN();
	}

	public double getPrI() {
		return (double) getI() / (double) getN();
	}

	public double getPrR() {
		return (double) getR() / (double) getN();
	}	

	public int getRandomN() {
		return Random.nextInt(0, getN()-1);
	}

	public int getRandomS() {
		return Random.nextInt(0, getS()-1);
	}

	public int getRandomI() {
		return Random.nextInt(0, getI()-1);
	}

	public int getRandomR() {
		return Random.nextInt(0, getR()-1);
	}

	public Host getRandomHost() {
		// Generate a random species from 0 to speciesList.size
		int spIdx = Random.nextInt(0, speciesList.size() - 1);
		return speciesList.get(spIdx).getRandomHost();
	}

	public Host getRandomHostS() {
		int index = Random.nextInt(0, getS() - 1);
		for (Species s : speciesList) {
			if (index < s.getS()) {
				return s.getHostS(index);
			}
			index -= s.getS();
		}
		return null;  // no susceptible host 
	}

	public Host getRandomHostI() {
		final int totalI = getI();
		if (totalI > 0) {
			int index = Random.nextInt(0, totalI - 1);
			for (Species s : speciesList) {
				if (index < s.getI()) {
					return s.getHostI(index);
				}
				index -= s.getI();
			}
		}
		return null;  // no infected hosts
	}

	public Host getRandomHostR() {
		final int totalR = getR();
		if (totalR > 0) {
			int index = Random.nextInt(0, totalR - 1);
			for (Species s : speciesList) {
				if (index < s.getR()) {
					return s.getHostR(index);
				}
				index -= s.getR();
			}
		}
		return null;  // no recovered hosts
	}

	public Virus getRandomInfection() {
		Virus v = null;
		Host h = getRandomHostI();
		if (h != null) {
			v = h.getInfection();
		}
		return v;
	}	

	public void resetCases() {
		cases = 0;
	}

	public int getCases() {
		return cases;
	}	

	public double getDiversity() {
		return diversity;
	}		

	public double getNetau() {
		return netau;
	}	

	public double getTmrca() {
		return tmrca;
	}	

	public double getSerialInterval() {
		return serialInterval;	
	}		

	public double getAntigenicDiversity() {
		return antigenicDiversity;
	}			

	public void stepForward() {
		final int day = (int) Parameters.day;
		if (Parameters.swapDemography) {
			swap();
		} else {
			grow(day % 365);
			decline();
		}
		recordContacts();
		recordRecoveries();
		distributeContacts();
		distributeRecoveries();				
		if (Parameters.transcendental) { 
			loseImmunity(); 
		}
		mutate();
		
		// Rather than clearing out the enviornment every time step, we
		// do it on per-day basis to improve simulation performance.
		dayOffset += Parameters.deltaT;
		if (dayOffset >= 1.0) {
			environment.clearViruses(deme, 1);
			dayOffset = 0;
			doEnviornmentalInfections(1.0);
		}
		
		sample();
	}

	public void doEnviornmentalInfections(double step) {
		int susCount = getS();   // initial values that are changed
		int envContact = (int) (susCount * step * environment.envUpTakeRate);
		for (int i = 0; ((i < envContact) && (susCount > 0)); i++) {
			// Get random virus from the environment
			Virus virus = environment.getVirus();
			if (virus == null) {
				continue;
			}
			// Get random species which is susceptible to infection
			int susIdx = Random.nextInt(0, susCount - 1);
			for (Species s : speciesList) {
				if (susIdx < s.getS()) {
					// Obtain the newly infected host (if infection was successful)
					if (s.infect(susIdx,  virus, deme, environment) != null) {
						cases++;     // infection actually occurred
						susCount--;  // update the counters to reflect
					}
					break;  // onto next contact
				}
				susIdx -= s.getS();  // onto the next species
			}
		}
	}


	// draw a Poisson distributed number of births and add these hosts to the end of the population list
	public void grow(final int dayOfYear) {
		for (Species s : speciesList) {
			s.grow(dayOfYear);
		}
	}

	public void decline() {
		for (Species s : speciesList) {
			s.decline();
		}
	}

	public void swap() {
		for (Species s : speciesList) {
			s.makeSusceptible();
		}
	}

	// draw a Poisson distributed number of contacts
	public void recordContacts() {
		// each infected makes I->S contacts on a per-day rate of beta * S/N
		double totalContactRate = getI() * getPrS() * Parameters.beta * Parameters.getSeasonality(deme) * Parameters.deltaT;
		newContacts = Random.nextPoisson(totalContactRate);			
	}

	// move from S->I following number of new contacts, from various species
	public void distributeContacts() {
		int susCount = getS();   // initial values that are changed
		int infCount = getI();   // in the loop below.
		
		for (int i = 0; (i < newContacts); i++) {
			if ((susCount > 0) && (infCount > 0)) {
				// Get random source of infection
				final Host infHost = getRandomHostI();
				// Get random species with susceptible to infect
				int susIdx = Random.nextInt(0, susCount - 1);
				for (Species s : speciesList) {
					if (susIdx < s.getS()) {
						// Obtain the newly infected host (if infection was successful)
						if (s.infect(susIdx,  infHost, deme, environment) != null) {
							cases++;     // infection actually occurred
							susCount--;  // update the counters to reflect
							infCount++;  // this.
						}
						break;  // onto next contact
					}
					susIdx -= s.getS();  // onto the next species
				}
			}
		}
	}

	// draw a Poisson distributed number of contacts and move from S->I based upon this
	// this deme is susceptibles and other deme is infecteds
	public void betweenDemeContact(HostSpeciesPopulation hp) {

		// each infected makes I->S contacts on a per-day rate of beta * S/N
		double totalContactRate = hp.getI() * getPrS() * Parameters.beta * Parameters.betweenDemePro * Parameters.getSeasonality(deme) * Parameters.deltaT;
		int contacts = Random.nextPoisson(totalContactRate);
		int susCount = getS();   // initial values that are changed
		int infCount = getI();   // in the loop below.
		
		for (int i = 0; (i < contacts); i++) {
			if ((susCount > 0) && (infCount > 0)) {
				// Get random source of infection
				final Host infHost = hp.getRandomHostI();
				// Get random species with susceptible to infect
				int susIdx = Random.nextInt(0, susCount - 1);
				for (Species s : speciesList) {
					if (susIdx < s.getS()) {
						if (s.infect(susIdx,  infHost, deme, environment) != null) {
							cases++;     // infection actually occurred
							susCount--;  // update the counters to reflect
							infCount++;  // this.
						}
						break;  // onto next contact
					}
					susIdx -= s.getS();  // onto the next species
				}
			}
		}
	}
	
	// draw a Poisson distributed number of recoveries
	public void recordRecoveries() {	
		// each infected recovers at a per-day rate of nu
		double totalRecoveryRate = getI() * Parameters.nu * Parameters.deltaT;
		newRecoveries = Random.nextPoisson(totalRecoveryRate);	
	}

	// move from I->S following number of recoveries
	public void distributeRecoveries() {
		int infCount = getI();
		for (int i = 0; ((i < newRecoveries) && (infCount > 0)); i++) {
			int infIdx = Random.nextInt(0, infCount - 1);
			for (Species s : speciesList) {
				if (infIdx < s.getI()) {
					s.recoverHost(infIdx, environment);
					infCount--;
					break;  // onto next recovery
				}
				infIdx -= s.getI();  // onto the next species
			}
		}			
	}

	// draw a Poisson distributed number of R->S 
	public void loseImmunity() {
		// each recovered looses immunity at a per-day rate
		double totalReturnRate = getR() * Parameters.immunityLoss * Parameters.deltaT;
		int returns  = Random.nextPoisson(totalReturnRate);
		int recCount = getR();
		for (int i = 0; ((i < returns) && (recCount > 0)); i++) {
			int recIdx = Random.nextInt(0,  recCount - 1);
			for (Species s : speciesList) {
				if (recIdx < s.getR()) {
					s.looseImmunity(recIdx);
					recCount--;
					break;  // onto next immunity loss
				}
				recIdx -= s.getR();  // onto the next species
			}
		}			
	}	

	// draw a Poisson distributed number of mutations and mutate based upon this
	// mutate should not impact other Virus's Phenotypes through reference
	public void mutate() {
		// each infected mutates at a per-day rate of mu
		double totalMutationRate = getI() * Parameters.muPhenotype * Parameters.deltaT;
		int mutations = Random.nextPoisson(totalMutationRate);
		for (int i = 0; i < mutations; i++) {
			Host h = getRandomHostI();
			h.mutate(environment);
		}			
	}

	public void vaccinate() {
		// double totalVaccine = getS() * Parameters.vaccinate * Parameters.deltaT;
		int vaccine  = (int)(getS() * Parameters.vaccinate);
		int susCount = getS();
		
		for (int i = 0; ((i < vaccine) && (susCount > 0)); i++) {
			int susIdx = Random.nextInt(0, susCount - 1);
			for (Species s : speciesList) {
				if (susIdx < s.getS()) {
					s.vaccinate(susIdx);
					susCount--;
					break;  // onto next vaccination
				}
				susIdx -= s.getS();  // onto the next species
			}
		}
	}

	public void cull() {
		// double cull = getI() * Parameters.culling * Parameters.deltaT;
		//int numCull = Random.nextPoisson(cull);
		int numCull  = (int) (getI() * Parameters.culling);
		int infCount = getI();
		// Cull the computed number of individuals
		for (int i = 0; ((i < numCull) && (infCount > 0)); i++) {
			int infIdx = Random.nextInt(0, infCount - 1);
			for (Species s : speciesList) {
				if (infIdx < s.getI()) {
					s.cull(infIdx);
					infCount--;
					break;  // onto next culling.
				}
				infIdx -= s.getI();  // onto the next species
			}
		}
	}

	// draw a Poisson distributed number of samples and add them to the VirusSample
	// only sample after burnin is completed
	public void sample() {
		if (getI()>0 && Parameters.day >= Parameters.burnin) {

			double totalSamplingRate = Parameters.tipSamplingRate * Parameters.deltaT;
			if (Parameters.tipSamplingProportional) {
				totalSamplingRate *= getI();
			} 

			int samples = Random.nextPoisson(totalSamplingRate);
			for (int i = 0; (i < samples); i++) {
				Host h = getRandomHostI();
				Virus v = h.getInfection();
				VirusTree.add(v);
			}
		}
	}

	// through current infected population assigning ancestry as trunk
	public void makeTrunk() {
		for (Species s : speciesList) {
			s.makeTrunk();
		}
	}	

	public void updateDiversity() {

		diversity = 0.0;
		tmrca = 0.0;
		antigenicDiversity = 0.0;		
		netau = 0.0;
		serialInterval = 0.0;

		if (getI()>1) { 

			double coalCount = 0.0;	
			double coalOpp = 0.0;
			double coalWindow = Parameters.netauWindow / 365.0;
			int sampleCount = Parameters.diversitySamplingCount;

			for (int i = 0; i < sampleCount; i++) {
				Virus vA = getRandomInfection();
				Virus vB = getRandomInfection();
				if (vA != null && vB != null) {
					double dist = vA.distance(vB);
					diversity += dist;
					if (dist > tmrca) {
						tmrca = dist;
					}
					antigenicDiversity += vA.antigenicDistance(vB);
					coalOpp += coalWindow;
					coalCount += vA.coalescence(vB, coalWindow);
					serialInterval += vA.serialInterval();
				}
			}	

			diversity /= (double) sampleCount;
			tmrca /= 2.0;
			antigenicDiversity /= (double) sampleCount;		
			netau = coalOpp / coalCount;
			serialInterval /= (double) sampleCount;

		}

	}	

	public void printState(PrintStream stream) {
		updateDiversity();
		stream.printf("\t%.4f\t%.4f\t%.4f\t%.5f\t%.4f\t%d\t%d\t%d\t%d\t%d", getDiversity(), getTmrca(), getNetau(), getSerialInterval(), getAntigenicDiversity(), getN(), getS(), getI(), getR(), getCases());
	}	

	public void printHeader(PrintStream stream) {
		stream.printf("\t%sDiversity\t%sTmrca\t%sNetau\t%sSerialInterval\t%sAntigenicDiversity\t%sN\t%sS\t%sI\t%sR\t%sCases", name, name, name, name, name, name, name, name, name, name);
	}

	// reset population to factory condition
	public void reset() {
		int initialR = 0;
		if (Parameters.transcendental) {
			initialR = (int) ((double) Parameters.initialNs[deme] * Parameters.initialPrT);
		}
		// Determine total number of initial infected individuals
		final int initialI = (deme == Parameters.initialDeme - 1) ? 
				Parameters.initialI : 0;
		// Compute the number of initial susceptibles
		int initialS = Parameters.initialNs[deme] - initialR - initialI;
		// Now have the species create individuals for each entry.
		for (Species s : speciesList) {
			s.clear();
			s.createHosts(deme, initialS, initialI, initialR);
		}
	}

	public void printHostPopulation(PrintStream stream) {
		for (Species s : speciesList) {
			s.printHostPopulation(stream, deme);
		}
	}

}
