package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

import java.util.BitSet;

/**
 * Provides a DistanceFunction to compute
 * a Distance between BitVectors based on the number of shared bits.
 *
 * @author Arthur Zimek
 */
public class SharedUnitedDistanceFunction extends SharingDependentItemsetDistanceFunction {

  /**
   * Provides a DistanceFunction to compute
   * a Distance between BitVectors based on the number of shared bits.
   */
  public SharedUnitedDistanceFunction() {
    super();
  }

  /**
   * Returns a distance between two Bitvectors.
   * Distance is 1-ratio(i,cardUnited),
   * where i is the number of bits shared by both BitVectors,
   * cardUnited is the cardinality (number of set bits)
   * of the union of both BitVector's BitSets,
   * and ratio(i,cardUnited) is 1 if cardUnited is 0, i/cardUnited otherwise.
   *
   * @param o1 first BitVector
   * @param o2 second BitVector
   * @return Distance between o1 and o2
   */
  public DoubleDistance distance(BitVector o1, BitVector o2) {
    BitSet b1 = o1.getBits();
    BitSet b2 = o2.getBits();
    b1.and(b2);
    BitSet united = o1.getBits();
    united.or(b2);
    int cardUnited = united.cardinality();
    int i = b1.cardinality();
    return new DoubleDistance(1 - ratio(i, cardUnited));
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#parameterDescription()
   */
  public String parameterDescription() {
    return "Distance is 1-ratio(i,cardUnited), where i is the number of bits shared by both BitVectors, cardUnited is the cardinality (number of set bits) of the union of both BitVector's BitSets, and ratio(i,cardUnited) is 1 if cardUnited is 0, i/cardUnited otherwise.";
  }


}
