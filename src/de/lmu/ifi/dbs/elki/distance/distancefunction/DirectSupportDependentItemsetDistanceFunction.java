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
public class DirectSupportDependentItemsetDistanceFunction
    extends FrequencyDependentItemsetDistanceFunction {

  /**
   * Provides a DistanceFunction to compute
   * a Distance between BitVectors based on the number of shared bits.
   */
  public DirectSupportDependentItemsetDistanceFunction() {
    super();
  }

  /**
   * Returns a distance between two Bitvectors.
   * Distance is support(%) * max{1-ratio(i,card1),1-ratio(i,card2)},
   * where i is the number of bits shared by both BitVectors,
   * o is the number of bits in the respective BitVector,
   * and ratio(i,card) is 1 if card is 0, i/card otherwise.
   *
   * @param v1 first BitVector
   * @param v2 second BitVector
   * @return Distance between o1 and o2
   */
  public DoubleDistance distance(BitVector v1, BitVector v2) {
    BitSet b1 = v1.getBits();
    BitSet b2 = v2.getBits();
    int card1 = b1.cardinality();
    int card2 = b2.cardinality();
    b1.and(b2);
    int i = b1.cardinality();
    double support = support(b1);
    return new DoubleDistance(support * Math.max(1.0 - ratio(i, card1), 1.0 - ratio(i, card2)));
  }

  @Override
  public String shortDescription() {
    return "Distance is support(%) * max{1-ratio(i,o1),1-ratio(i,o2)}, where i is the number of bits shared by both BitVectors, o is the number of bits in the respective BitVector, and ratio(i,o) is 1 if o is 0, i/o otherwise.\n";
  }
}
