package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.Random;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * This is a dummy distance providing random values (obviously not metrical),
 * useful mostly for unit tests and baseline evaluations: obviously this
 * distance provides no benefit whatsoever.
 * 
 * This distance is based on the combined hash codes of the two objects queried,
 * if they are different. Extra caution is done to ensure symmetry and objects
 * with the same ID will have a distance of 0. Obviously this distance is not
 * metrical.
 * 
 * @author Erich Schubert
 */
public class RandomStableDistanceFunction extends AbstractDBIDDistanceFunction<DoubleDistance> {
  // TODO: add seed parameter!

  /**
   * Static instance
   */
  public static final RandomStableDistanceFunction STATIC = new RandomStableDistanceFunction((new Random()).nextLong());

  /**
   * Seed for reproducible random.
   */
  private long seed;

  /**
   * Constructor. Usually it is preferred to use the static instance!
   */
  public RandomStableDistanceFunction(long seed) {
    super();
    this.seed = seed;
  }

  @Override
  public DoubleDistance distance(DBID o1, DBID o2) {
    int c = o1.compareTo(o2);
    if(c == 0) {
      return DoubleDistance.FACTORY.nullDistance();
    }
    // Symmetry
    if(c > 0) {
      return distance(o2, o1);
    }
    return new DoubleDistance(pseudoRandom(seed, Util.mixHashCodes(o1.hashCode(), o2.hashCode(), (int) seed)));
  }

  /**
   * Pseudo random number generator, adaption of the common rand48 generator
   * which can be found in C (man drand48), Java and attributed to Donald Knuth.
   * 
   * @param seed Seed value
   * @param input Input code
   * 
   * @return Pseudo random double value
   */
  private double pseudoRandom(final long seed, int input) {
    // Default constants from "man drand48"
    final long mult = 0x5DEECE66DL;
    final long add = 0xBL;
    final long mask = (1L << 48) - 1; // 48 bit
    // Produce an initial seed each
    final long i1 = (input ^ seed ^ mult) & mask;
    final long i2 = (input ^ (seed >>> 16) ^ mult) & mask;
    // Compute the first random each
    final long l1 = (i1 * mult + add) & mask;
    final long l2 = (i2 * mult + add) & mask;
    // Use 53 bit total:
    final int r1 = (int) (l1 >>> 22); // 48 - 22 = 26
    final int r2 = (int) (l2 >>> 21); // 48 - 21 = 27
    double random = ((((long) r1) << 27) + r2) / (double) (1L << 53);
    return random;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected RandomStableDistanceFunction makeInstance() {
      return RandomStableDistanceFunction.STATIC;
    }
  }
}