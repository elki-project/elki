package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Provides the local neighborhood to be considered in the PCA as the k nearest
 * neighbors of an object.
 * 
 * @see AbstractLocalPCAPreprocessor
 * @author Elke Achtert
 */
@Title("Knn Query Based Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database. The PCA is based on k nearest neighbor queries.")
public class KNNQueryBasedLocalPCAPreprocessor extends AbstractLocalPCAPreprocessor implements Parameterizable {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("localpca.k", "The number of nearest neighbors considered in the PCA. " + "If this parameter is not set, k ist set to three " + "times of the dimensionality of the database objects.");

  /**
   * Optional parameter to specify the number of nearest neighbors considered in
   * the PCA, must be an integer greater than 0. If this parameter is not set, k
   * is set to three times of the dimensionality of the database objects.
   * <p>
   * Key: {@code -localpca.k}
   * </p>
   * <p>
   * Default value: three times of the dimensionality of the database objects
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0), true);

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  private Integer k = null;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public KNNQueryBasedLocalPCAPreprocessor(Parameterization config) {
    super(config);
    config = config.descend(this);
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
  }

  @Override
  public <V extends NumberVector<?, ?>> Instance<V> instantiate(Database<V> database) {
    int instk;
    if(k == null) {
      instk = 3 * database.dimensionality();
    }
    else {
      instk = k;
    }
    return new Instance<V>(database, pcaDistanceFunction, pca, instk);
  }

  /**
   * The actual preprocessor instance.
   * 
   * @param <V> the type of NumberVector handled by this Preprocessor
   * @author Erich Schubert
   * 
   * Note: final, since overriding the constructor will likely fail!
   */
  public static final class Instance<V extends NumberVector<?, ?>> extends AbstractLocalPCAPreprocessor.Instance<V> {
    /**
     * The value of k.
     */
    final int k;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param pcaDistanceFunction distance function
     * @param pca PCA runner class
     * @param k k
     */
    public Instance(Database<V> database, DistanceFunction<? super V, DoubleDistance> pcaDistanceFunction, PCAFilteredRunner<? super V, DoubleDistance> pca, Integer k) {
      super(database);
      this.k = k;
      preprocess(database, pcaDistanceFunction, pca);
    }

    @Override
    protected List<DistanceResultPair<DoubleDistance>> objectsForPCA(DBID id, Database<V> database, DistanceQuery<V, DoubleDistance> distQuery) {
      return database.kNNQueryForID(id, k, distQuery);
    }
  }
}