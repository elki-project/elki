package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.BitVector;
import de.lmu.ifi.dbs.distance.DoubleDistance;

import java.util.BitSet;

/**
 * Provides a DistanceFunction to compute
 * a Distance between BitVectors based on the number of shared bits.
 *
 * @author Arthur Zimek
 */
public class SquareRootSupportLengthDependentItemsetDistanceFunction extends FrequencyDependentItemsetDistanceFunction {

  /**
   * Provides a DistanceFunction to compute
   * a Distance between BitVectors based on the number of shared bits.
   */
  public SquareRootSupportLengthDependentItemsetDistanceFunction() {
    super();
  }

  /**
   * Returns a distance between two Bitvectors.
   * Distance is (sqrt(1.0 / support(%)) * (1.0 / (i==0 ? 1 : i))) * max{1-ratio(i,card1),1-ratio(i,card2)},
   * where i is the number of bits shared by both BitVectors,
   * o is the number of bits in the respective BitVector,
   * and ratio(i,card) is 1 if card is 0, i/card otherwise.
   *
   * @param o1 first BitVector
   * @param o2 second BitVector
   * @return Distance between o1 and o2
   */
  public DoubleDistance distance(BitVector o1, BitVector o2) {
    BitSet b1 = o1.getBits();
    BitSet b2 = o2.getBits();
    int card1 = b1.cardinality();
    int card2 = b2.cardinality();
    b1.and(b2);
    int i = b1.cardinality();
    double support = support(b1);
    return new DoubleDistance((Math.max(1 - ratio(i, card1), 1 - ratio(i, card2)) * Math.sqrt(1.0 / support)) / (i == 0 ? 1 : i));
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    return "Distance is (sqrt(1.0 / support(%)) * (1.0 /(i==0 ? 1 : i))) * max{1-ratio(i,o1),1-ratio(i,o2)}, where i is the number of bits shared by both BitVectors, o is the number of bits in the respective BitVector, and ratio(i,o) is 1 if o is 0, i/o otherwise.";
  }
}
