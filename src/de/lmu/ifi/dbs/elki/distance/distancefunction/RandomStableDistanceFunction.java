package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.Random;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

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
  /**
   * Constructor. Private - use the static instance!
   */
  private RandomStableDistanceFunction() {
    super();
  }
  
  /**
   * The random generator we work with.
   */
  private Random random = new Random();

  /**
   * Factory method for {@link Parameterizable}
   * 
   * Note: we need this method, to override the parent class' method.
   * 
   * @param config Parameterization
   * @return Distance function
   */
  public static RandomStableDistanceFunction parameterize(Parameterization config) {
    return RandomStableDistanceFunction.STATIC;
  }

  /**
   * Static instance
   */
  public static final RandomStableDistanceFunction STATIC = new RandomStableDistanceFunction();

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
    int hash = Util.mixHashCodes(o1.hashCode(), o2.hashCode(), 123456789);
    random.setSeed(hash);
    return new DoubleDistance(random.nextDouble());
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }
}
