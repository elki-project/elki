package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;

/*CFP*/
public class HammingDistanceFunction implements PrimitiveDistanceFunction<BitVector>{

	public static final HammingDistanceFunction STATIC = new HammingDistanceFunction();
	private static long distanceComputations = 0;
	
	@Override
	public boolean isSymmetric() {
		return true;
	}

	@Override
	public boolean isMetric() {
		return true;
	}

	@Override
	public <T extends BitVector> DistanceQuery<T> instantiate(Relation<T> relation) {
		return new PrimitiveDistanceQuery<T>(relation, this);
	}

	@Override
	public double distance(BitVector o1, BitVector o2) {
		distanceComputations++;
		return ((BitVector)o1).hammingDistance(((BitVector)o2));
	}

	@Override
	public SimpleTypeInformation<? super BitVector> getInputTypeRestriction() {
		return new SimpleTypeInformation<BitVector>(BitVector.class);
	}

	public static void resetDistanceComputations(){
		distanceComputations = 0;
	}
	
	public static long getDistanceComputations(){
		return distanceComputations;
	}
}
