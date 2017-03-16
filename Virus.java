/* Virus infection that has genotype, phenotype and ancestry */

import java.util.*;

public class Virus {

	// simulation fields
	private Virus parent;
	private Phenotype phenotype;
	private double birth;		// measured in years relative to burnin
	private int deme;
	
	// additional reconstruction fields
	private boolean marked;
	private boolean trunk;	// fill this at the end of the simulation
	private List<Virus> children = new ArrayList<Virus>(0);	// will be void until simulation ends	
	private double layout;
	private int coverage;		// how many times this Virus has been covered in tracing the tree backwards
	
	
	/** 
	 * A counter to track the current number of infected hosts that are actively
	 * shedding this virus.  This value is used to track the presence of this
	 * virus in the environment.  This value is incremented each time a host
	 * is infected with this virus.  This value is decremented when the host
	 * is no longer infected with this virus.
	 */
	private int numHostsShedding;
	
	/**
	 * The number of viruses in the environment. This value has to be very
	 * large at 1e12 (10^2).  This value is managed by the Environment class
	 * for each virus.
	 */
	private double volume;
	
	// initialization
	public Virus() {
		phenotype = PhenotypeFactory.makeVirusPhenotype();
		birth = Parameters.getDate();
	}
		
	// replication, copies the virus, but remembers the ancestry
	public Virus(Virus v, int d) {
		parent = v;
		phenotype = v.getPhenotype();
		birth = Parameters.getDate();
		deme = d;
	}
	
	public Virus(Virus v, int d, Phenotype p) {
		parent = v;
		phenotype = p;
		birth = Parameters.getDate();
		deme = d;
	}	
	
	public Virus(int d, Phenotype p) {
		parent = null;
		phenotype = p;
		birth = Parameters.getDate();
		deme = d;
	}		
	
	// methods
	public Phenotype getPhenotype() {
		return phenotype;
	}
	public void setPhenotype(Phenotype p) {
		phenotype = p;
	}	
	public double getBirth() {
		return birth;
	}
	public Virus getParent() {
		return parent;
	}
	public void setParent(Virus v) {
		parent = v;
	}
	public boolean isTrunk() {
		return trunk; 
	}
	public void makeTrunk() {
		trunk = true;
	}
	public void mark() {
		marked = true;
	}
	public boolean isMarked() {
		return marked;
	}
	public int getDeme() {
		return deme;
	}	
	public double getLayout() {
		return layout;
	}
	public void setLayout(double y) {
		layout = y;
	}
	public int getCoverage() {
		return coverage;
	}
	public void incrementCoverage() {
		coverage++;
	}
	
	// add virus node as child if does not already exist
	public void addChild(Virus v) {
		if (!children.contains(v)) {
			children.add(v);
		}
	}		
	public int getNumberOfChildren() {
		return children.size();
	}
	public List<Virus> getChildren() {
		return children;
	}	
	public boolean isTip() {
		return getNumberOfChildren() == 0 ? true : false;
	}
	
	// returns a mutated copy, original virus left intact
	public Virus mutate() {
		Phenotype mutP = phenotype.mutate();			// mutated copy
		Virus mutV = new Virus(this,deme,mutP);
		return mutV;
	}
	
	public Virus commonAncestor(Virus virusB) {
				
		Virus lineageA = this;
		Virus lineageB = virusB;
		Virus commonAnc = null;
		Set<Virus> ancestry = new HashSet<Virus>();		
		while (true) {
			if (!ancestry.add(lineageA)) { 
				commonAnc = lineageA;
				break; 
			}		
			if (!ancestry.add(lineageB)) { 
				commonAnc = lineageB;
				break; 
			}			
			if (lineageA.getParent() != null) {		
				lineageA = lineageA.getParent();
			}
			if (lineageB.getParent() != null) {
				lineageB = lineageB.getParent();
			}
			if (lineageA.getParent() == null && lineageB.getParent() == null) {	
				break;
			}
		}	
		
		return commonAnc;								// returns null when no common ancestor is present
		
	}
	
	public double distance(Virus virusB) {
		Virus ancestor = commonAncestor(virusB);
		if (ancestor != null) {
			double distA = getBirth() - ancestor.getBirth();
			double distB = virusB.getBirth() - ancestor.getBirth();
			return distA + distB;
		}
		else {
			return 0;
		}
	}
	
	public double getPhylogeneticDistance(Virus virusB) {
		return phenotype.distance(virusB.getPhenotype());
	}
	
	public double antigenicDistance(Virus virusB) {
		return phenotype.distance(virusB.getPhenotype());
	}	
	
	// is there a coalescence event within x amount of time? (measured in years)
	public double coalescence(Virus virusB, double windowTime) {

		Virus lineageA = this;
		Virus lineageB = virusB;
		Set<Virus> ancestry = new HashSet<Virus>();	
		double success = 0.0;
		
		double startTime = lineageA.getBirth();
		double time = startTime;
		while (time > startTime - windowTime) {
			if (lineageA.getParent() != null) {		
				lineageA = lineageA.getParent();
				time = lineageA.getBirth();
				ancestry.add(lineageA);
			}
			else {
				break;
			}
		}
		
		startTime = lineageB.getBirth();
		time = startTime;
		while (time > startTime - windowTime) {
			if (lineageB.getParent() != null) {		
				lineageB = lineageB.getParent();
				time = lineageB.getBirth();				
				if (!ancestry.add(lineageB)) { 
					success = 1.0;
					break; 
				}
			}
			else {
				break;
			}			
		}
		
		return success;	

	}	
	
	// this is the interval from this virus's birth back to its parent's birth
	public double serialInterval() {
		Virus p = getParent();
		return getBirth() - p.getBirth();
	}
	
	public String toString() {
		return Integer.toHexString(this.hashCode());
	}

	//-------- Methods for managing viruses in environment ------------//
	
	/**
	 * Inform this virus that it has infected a new host.  This method
	 * essentially tracks the current number of hosts infected by this
	 * virus.
	 */
	public void hostInfected() {
		this.numHostsShedding++;
	}
	
	/**
	 * Inform this virus that it is no longer infecting a new host.  This method
	 * essentially tracks the current number of hosts infected by this
	 * virus.
	 */
	public void hostRecovered() {
		this.numHostsShedding--;
	}
	
	/**
	 * Obtain the number of hosts actively shedding this virus.
	 * 
	 * @return The number of hosts shedding the virus.
	 */
	public int getNumShedding() {
		return this.numHostsShedding;
	}
	
	/**
	 * The raw number of viruses in the environment. This value is typically
	 * very large in the range of 1e12 (10^12).
	 * 
	 * @return The volume of viruses in the environment.
	 */
	public double getVolume() {
		return this.volume;
	}
	
	/**
	 * Set the raw volume of viruses in the environment. This value is 
	 * typically pretty large, in the range of 1e12.
	 * 
	 * @param volume The volume of this virus in the environment.
	 */
	public void setVolume(double volume) {
		this.volume = volume;
	}
}
