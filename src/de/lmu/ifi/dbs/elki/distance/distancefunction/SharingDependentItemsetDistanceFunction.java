package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.BitVector;

/**
 * Provides a DistanceFunction to compute
 * a Distance between BitVectors based on the number of shared bits.
 *
 * @author Arthur Zimek
 */
public abstract class SharingDependentItemsetDistanceFunction extends AbstractDoubleDistanceFunction<BitVector> {

  protected SharingDependentItemsetDistanceFunction() {
    super();
  }

  /**
   * Returns 1 if card is 0,
   * i divided by card otherwise.
   *
   * @param i    the number of bits
   * @param card the cardinality of a bitset
   * @return 1 if card is 0,
   *         i divided by card otherwise
   */
  protected double ratio(int i, int card) {
    return card == 0 ? 1 : ((double) i) / card;
  }
}
