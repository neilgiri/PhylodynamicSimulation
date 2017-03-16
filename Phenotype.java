/* Interface for Phenotype objects */


public interface Phenotype {

	// provides the risk of infection (from 0 to 1) of a virus with this phenotype 
	// when contacting a Host with a List of Phenotypes forming their immune history
	double riskOfInfection( Phenotype[] immuneHistory);

	// return mutated Phenotype object
	// returned Phenotype is a newly constructed copy of original
	Phenotype mutate();
	
	// compare phenotypes and report antigenic distance
	double distance(Phenotype p);
	
	// this is used in output, should be a short string form
	// 2D: 0.5,0.6
	// sequence: "ATGCGCC"
	String toString();

	/**
	 * Determine the antigenic distance between two phenotypes.
	 * 
	 * @param p The other phenotype to compare with.
	 * 
	 * @return The antigenic distance between two phenotypes.
	 */
	double getAntigenicDistance(Phenotype p);
}
