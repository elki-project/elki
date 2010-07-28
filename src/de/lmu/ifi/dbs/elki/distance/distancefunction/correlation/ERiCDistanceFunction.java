package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPreprocessorBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocalProjectionPreprocessorBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.WeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.BitDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.preprocessing.KNNQueryBasedLocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.LocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides a distance function for building the hierarchy in the ERiC
 * algorithm.
 * 
 * @author Elke Achtert
 */
public class ERiCDistanceFunction extends AbstractPreprocessorBasedDistanceFunction<NumberVector<?, ?>, LocalPCAPreprocessor, BitDistance> implements LocalProjectionPreprocessorBasedDistanceFunction<NumberVector<?, ?>, LocalPCAPreprocessor, PCAFilteredResult, BitDistance> {
  /**
   * Logger for debug.
   */
  static Logging logger = Logging.getLogger(PCABasedCorrelationDistanceFunction.class);

  /**
   * OptionID for {@link #DELTA_PARAM}
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("ericdf.delta", "Threshold for approximate linear dependency: " + "the strong eigenvectors of q are approximately linear dependent " + "from the strong eigenvectors p if the following condition " + "holds for all stroneg eigenvectors q_i of q (lambda_q < lambda_p): " + "q_i' * M^check_p * q_i <= delta^2.");

  /**
   * Parameter to specify the threshold for approximate linear dependency: the
   * strong eigenvectors of q are approximately linear dependent from the strong
   * eigenvectors p if the following condition holds for all strong eigenvectors
   * q_i of q (lambda_q < lambda_p): q_i' * M^check_p * q_i <= delta^2, must be
   * a double equal to or greater than 0.
   * <p>
   * Default value: {@code 0.1}
   * </p>
   * <p>
   * Key: {@code -ericdf.delta}
   * </p>
   */
  private final DoubleParameter DELTA_PARAM = new DoubleParameter(DELTA_ID, new GreaterEqualConstraint(0), 0.1);

  /**
   * OptionID for {@link #TAU_PARAM}
   */
  public static final OptionID TAU_ID = OptionID.getOrCreateOptionID("ericdf.tau", "Threshold for the maximum distance between two approximately linear " + "dependent subspaces of two objects p and q " + "(lambda_q < lambda_p) before considering them as parallel.");

  /**
   * Parameter to specify the threshold for the maximum distance between two
   * approximately linear dependent subspaces of two objects p and q (lambda_q <
   * lambda_p) before considering them as parallel, must be a double equal to or
   * greater than 0.
   * <p>
   * Default value: {@code 0.1}
   * </p>
   * <p>
   * Key: {@code -ericdf.tau}
   * </p>
   */
  private final DoubleParameter TAU_PARAM = new DoubleParameter(TAU_ID, new GreaterEqualConstraint(0), 0.1);

  /**
   * Holds the value of {@link #DELTA_PARAM}.
   */
  private double delta;

  /**
   * Holds the value of {@link #TAU_PARAM}.
   */
  private double tau;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public ERiCDistanceFunction(Parameterization config) {
    super(config);

    // delta
    if(config.grab(DELTA_PARAM)) {
      delta = DELTA_PARAM.getValue();
    }

    // tau
    if(config.grab(TAU_PARAM)) {
      tau = TAU_PARAM.getValue();
    }
  }

  @Override
  public BitDistance getDistanceFactory() {
    return BitDistance.FACTORY;
  }

  /**
   * Returns the name of the default preprocessor.
   * 
   * @return the name of the default preprocessor, which is
   *         {@link de.lmu.ifi.dbs.elki.preprocessing.KNNQueryBasedLocalPCAPreprocessor}
   */
  @Override
  public Class<?> getDefaultPreprocessorClass() {
    return KNNQueryBasedLocalPCAPreprocessor.class;
  }

  @Override
  public String getPreprocessorDescription() {
    return "Preprocessor class to determine the correlation dimension of each object.";
  }

  /**
   * @return the super class for the preprocessor parameter, which is
   *         {@link de.lmu.ifi.dbs.elki.preprocessing.Preprocessor}
   */
  @Override
  public Class<LocalPCAPreprocessor> getPreprocessorSuperClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(LocalPCAPreprocessor.class);
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function. Note, that the first pca must have equal or more strong
   * eigenvectors than the second pca.
   * 
   * @param v1 first DatabaseObject
   * @param v2 second DatabaseObject
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  public BitDistance distance(NumberVector<?, ?> v1, NumberVector<?, ?> v2, PCAFilteredResult pca1, PCAFilteredResult pca2) {
    if(pca1.getCorrelationDimension() < pca2.getCorrelationDimension()) {
      throw new IllegalStateException("pca1.getCorrelationDimension() < pca2.getCorrelationDimension(): " + pca1.getCorrelationDimension() + " < " + pca2.getCorrelationDimension());
    }

    boolean approximatelyLinearDependent;
    if(pca1.getCorrelationDimension() == pca2.getCorrelationDimension()) {
      approximatelyLinearDependent = approximatelyLinearDependent(pca1, pca2) && approximatelyLinearDependent(pca2, pca1);
    }
    else {
      approximatelyLinearDependent = approximatelyLinearDependent(pca1, pca2);
    }

    if(!approximatelyLinearDependent) {
      return new BitDistance(true);
    }

    else {
      double affineDistance;

      if(pca1.getCorrelationDimension() == pca2.getCorrelationDimension()) {
        WeightedDistanceFunction df1 = new WeightedDistanceFunction(pca1.similarityMatrix());
        WeightedDistanceFunction df2 = new WeightedDistanceFunction(pca2.similarityMatrix());
        affineDistance = Math.max(df1.distance(v1, v2).doubleValue(), df2.distance(v1, v2).doubleValue());
      }
      else {
        WeightedDistanceFunction df1 = new WeightedDistanceFunction(pca1.similarityMatrix());
        affineDistance = df1.distance(v1, v2).doubleValue();
      }

      if(affineDistance > tau) {
        return new BitDistance(true);
      }

      return new BitDistance(false);
    }
  }

  /**
   * Returns true, if the strong eigenvectors of the two specified pcas span up
   * the same space. Note, that the first pca must have equal ore more strong
   * eigenvectors than the second pca.
   * 
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @return true, if the strong eigenvectors of the two specified pcas span up
   *         the same space
   */
  private boolean approximatelyLinearDependent(PCAFilteredResult pca1, PCAFilteredResult pca2) {
    Matrix m1_czech = pca1.dissimilarityMatrix();
    Matrix v2_strong = pca2.adapatedStrongEigenvectors();
    for(int i = 0; i < v2_strong.getColumnDimensionality(); i++) {
      Matrix v2_i = v2_strong.getColumn(i);
      // check, if distance of v2_i to the space of pca_1 > delta
      // (i.e., if v2_i spans up a new dimension)
      double dist = Math.sqrt(v2_i.transposeTimes(v2_i).get(0, 0) - v2_i.transposeTimes(m1_czech).times(v2_i).get(0, 0));

      // if so, return false
      if(dist > delta) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Class<? super NumberVector<?, ?>> getInputDatatype() {
    return NumberVector.class;
  }

  @Override
  public <T extends NumberVector<?, ?>> Instance<T> instantiate(Database<T> database) {
    return new Instance<T>(database, getPreprocessor().instantiate(database), this);
  }

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   */
  public static class Instance<V extends NumberVector<?, ?>> extends AbstractPreprocessorBasedDistanceFunction.Instance<V, LocalPCAPreprocessor.Instance<V>, PCAFilteredResult, BitDistance> {
    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     * @param parent Parent distance
     */
    public Instance(Database<V> database, LocalPCAPreprocessor.Instance<V> preprocessor, ERiCDistanceFunction parent) {
      super(database, preprocessor, parent);
    }

    /**
     * Note, that the pca of o1 must have equal ore more strong eigenvectors
     * than the pca of o2.
     */
    @Override
    public BitDistance distance(DBID id1, DBID id2) {
      PCAFilteredResult pca1 = preprocessor.get(id1);
      PCAFilteredResult pca2 = preprocessor.get(id2);
      V v1 = database.get(id1);
      V v2 = database.get(id2);
      // FIXME: remove cast by using generics
      return ((ERiCDistanceFunction) distanceFunction).distance(v1, v2, pca1, pca2);
    }
  }
}