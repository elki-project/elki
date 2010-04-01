package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
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
 * @see LocalPCAPreprocessor
 * @author Elke Achtert
 * @param <V> the type of NumberVector handled by this Preprocessor
 */
@Title("Knn Query Based Local PCA Preprocessor")
@Description("Materializes the local PCA and the locally weighted matrix of objects of a database. The PCA is based on k nearest neighbor queries.")
public class KnnQueryBasedLocalPCAPreprocessor<V extends NumberVector<V, ?>> extends LocalPCAPreprocessor<V> implements Parameterizable {
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
  public KnnQueryBasedLocalPCAPreprocessor(Parameterization config) {
    super(config);
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
  }

  @Override
  protected List<DistanceResultPair<DoubleDistance>> objectsForPCA(Integer id, Database<V> database) {
    if(k == null) {
      V obj = database.get(id);
      k = 3 * obj.getDimensionality();
    }

    pcaDistanceFunction.setDatabase(database);
    return database.kNNQueryForID(id, k, pcaDistanceFunction);
  }
}
