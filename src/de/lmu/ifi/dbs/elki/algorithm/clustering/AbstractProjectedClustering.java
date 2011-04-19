package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.PROCLUS;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract superclass for projected clustering algorithms, like {@link PROCLUS}
 * and {@link de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ORCLUS}.
 * 
 * @author Elke Achtert
 * @param <V> the type of FeatureVector handled by this Algorithm
 */
public abstract class AbstractProjectedClustering<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater than 0.
   * <p>
   * Key: {@code -projectedclustering.k}
   * </p>
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("projectedclustering.k", "The number of clusters to find.");

  /**
   * Parameter to specify the multiplier for the initial number of seeds, must
   * be an integer greater than 0.
   * <p>
   * Default value: {@code 30}
   * </p>
   * <p>
   * Key: {@code -projectedclustering.k_i}
   * </p>
   */
  public static final OptionID K_I_ID = OptionID.getOrCreateOptionID("projectedclustering.k_i", "The multiplier for the initial number of seeds.");

  /**
   * Parameter to specify the dimensionality of the clusters to find, must be an
   * integer greater than 0.
   * <p>
   * Key: {@code -projectedclustering.l}
   * </p>
   */
  public static final OptionID L_ID = OptionID.getOrCreateOptionID("projectedclustering.l", "The dimensionality of the clusters to find.");

  /**
   * Holds the value of {@link #K_ID}.
   */
  protected int k;

  /**
   * Holds the value of {@link #K_I_ID}.
   */
  protected int k_i;

  /**
   * Holds the value of {@link #L_ID}.
   */
  protected int l;

  /**
   * The euclidean distance function.
   */
  private DistanceFunction<? super V, DoubleDistance> distanceFunction = EuclideanDistanceFunction.STATIC;

  /**
   * Internal constructor.
   * 
   * @param k K parameter
   * @param k_i K_i parameter
   * @param l L parameter
   */
  public AbstractProjectedClustering(int k, int k_i, int l) {
    super();
    this.k = k;
    this.k_i = k_i;
    this.l = l;
  }

  /**
   * Returns the distance function.
   * 
   * @return the distance function
   */
  protected DistanceFunction<? super V, DoubleDistance> getDistanceFunction() {
    return distanceFunction;
  }

  /**
   * Returns the distance function.
   * 
   * @return the distance function
   */
  protected DistanceQuery<V, DoubleDistance> getDistanceQuery(Database database) {
    return QueryUtil.getDistanceQuery(database, distanceFunction);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer extends AbstractParameterizer {
    protected int k;

    protected int k_i;

    protected int l;

    /**
     * Get the parameter k, see {@link #K_ID}
     * 
     * @param config Parameterization
     */
    protected void configK(Parameterization config) {
      IntParameter kP = new IntParameter(K_ID, new GreaterConstraint(0));
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    /**
     * Get the parameter k_i, see {@link #K_I_ID}
     * 
     * @param config Parameterization
     */
    protected void configKI(Parameterization config) {
      IntParameter k_iP = new IntParameter(K_I_ID, new GreaterConstraint(0), 30);
      if(config.grab(k_iP)) {
        k_i = k_iP.getValue();
      }
    }

    /**
     * Get the parameter l, see {@link #L_ID}
     * 
     * @param config Parameterization
     */
    protected void configL(Parameterization config) {
      IntParameter lP = new IntParameter(L_ID, new GreaterConstraint(0));
      if(config.grab(lP)) {
        l = lP.getValue();
      }
    }
  }
}