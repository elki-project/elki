package experimentalcode.erich.distance;

import java.util.Iterator;

import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a LP-Norm for FeatureVectors.
 * 
 * @author Arthur Zimek
 * @param <V> the type of FeatureVector to compute the distances in between
 * @param <N> number type TODO: implement SpatialDistanceFunction
 */
public class SparseLPNormDistanceFunction<V extends SparseFeatureVector<V, N>, N extends Number> extends AbstractDistanceFunction<V, DoubleDistance> {

  /**
   * OptionID for {@link #P_PARAM}
   */
  public static final OptionID P_ID = OptionID.getOrCreateOptionID("lpnorm.p", "the degree of the L-P-Norm (positive number)");

  /**
   * P parameter
   */
  private final DoubleParameter P_PARAM = new DoubleParameter(P_ID, new GreaterConstraint(0));

  /**
   * Keeps the currently set p.
   */
  private double p;

  /**
   * Provides a LP-Norm for FeatureVectors.
   */
  public SparseLPNormDistanceFunction(Parameterization config) {
    super(new DoubleDistance());
    if (config.grab(P_PARAM)) {
      p = P_PARAM.getValue();
    }
  }

  /**
   * Returns the distance between the specified FeatureVectors as a LP-Norm for
   * the currently set p.
   * 
   * @param v1 first FeatureVector
   * @param v2 second FeatureVector
   * @return the distance between the specified FeatureVectors as a LP-Norm for
   *         the currently set p
   */
  public DoubleDistance distance(V v1, V v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }

    double sqrDist = 0;
    // do a "merging" iteration over both lists.
    Iterator<Pair<Integer, N>> i1 = v1.getNonNullComponents();
    Iterator<Pair<Integer, N>> i2 = v2.getNonNullComponents();
    Pair<Integer, N> c1 = i1.hasNext() ? i1.next() : null;
    Pair<Integer, N> c2 = i2.hasNext() ? i2.next() : null;
    while(c1 != null && c2 != null) {
      double manhattanI;
      if(c1.first == c2.first) {
        manhattanI = Math.abs(c1.second.doubleValue() - c2.second.doubleValue());
        c1 = i1.hasNext() ? i1.next() : null;
        c2 = i2.hasNext() ? i2.next() : null;
      }
      else if(c1.first < c2.first) {
        manhattanI = Math.abs(c1.second.doubleValue());
        c1 = i1.hasNext() ? i1.next() : null;
      }
      else {
        manhattanI = Math.abs(c2.second.doubleValue());
        c2 = i2.hasNext() ? i2.next() : null;
      }
      sqrDist += Math.pow(manhattanI, p);
    }
    // process asymmetric ends of the list.
    while(c1 != null) {
      double manhattanI = Math.abs(c1.second.doubleValue());
      sqrDist += Math.pow(manhattanI, p);
      c1 = i1.hasNext() ? i1.next() : null;
    }
    while(c2 != null) {
      double manhattanI = Math.abs(c2.second.doubleValue());
      sqrDist += Math.pow(manhattanI, p);
      c2 = i2.hasNext() ? i2.next() : null;
    }
    return new DoubleDistance(Math.pow(sqrDist, 1.0 / p));
  }
}