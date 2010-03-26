package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocalPCAPreprocessorBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PCACorrelationDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedLocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.LocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides the correlation distance for real valued vectors.
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
// TODO: can we spec D differently so we don't get the unchecked warnings below?
public class PCABasedCorrelationDistanceFunction<V extends NumberVector<V, ?>, P extends LocalPCAPreprocessor<V>> extends AbstractCorrelationDistanceFunction<V, P, PCACorrelationDistance> implements LocalPCAPreprocessorBasedDistanceFunction<V, P, PCACorrelationDistance> {
  /**
   * OptionID for {@link #DELTA_PARAM}
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("pcabasedcorrelationdf.delta", "Threshold of a distance between a vector q and a given space that indicates that " + "q adds a new dimension to the space.");

  /**
   * Parameter to specify the threshold of a distance between a vector q and a
   * given space that indicates that q adds a new dimension to the space, must
   * be a double equal to or greater than 0.
   * <p>
   * Default value: {@code 0.25}
   * </p>
   * <p>
   * Key: {@code -pcabasedcorrelationdf.delta}
   * </p>
   */
  private final DoubleParameter DELTA_PARAM = new DoubleParameter(DELTA_ID, new GreaterEqualConstraint(0), 0.25);

  /**
   * Holds the value of {@link #DELTA_PARAM}.
   */
  private double delta;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public PCABasedCorrelationDistanceFunction(Parameterization config) {
    super(config, new PCACorrelationDistance());
    if(config.grab(DELTA_PARAM)) {
      delta = DELTA_PARAM.getValue();
    }
  }

  @Override
  protected PCACorrelationDistance correlationDistance(V dv1, V dv2) {
    PCAFilteredResult pca1 = getDatabase().getAssociation(AssociationID.LOCAL_PCA, dv1.getID());
    PCAFilteredResult pca2 = getDatabase().getAssociation(AssociationID.LOCAL_PCA, dv2.getID());

    int correlationDistance = correlationDistance(pca1, pca2, dv1.getDimensionality());
    double euclideanDistance = euclideanDistance(dv1, dv2);

    return new PCACorrelationDistance(correlationDistance, euclideanDistance);
  }

  /**
   * Computes the correlation distance between the two subspaces defined by the
   * specified PCAs.
   * 
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @param dimensionality the dimensionality of the data space
   * @return the correlation distance between the two subspaces defined by the
   *         specified PCAs
   */
  public int correlationDistance(PCAFilteredResult pca1, PCAFilteredResult pca2, int dimensionality) {
    // TODO nur in eine Richtung?
    // pca of rv1
    Matrix v1 = pca1.getEigenvectors();
    Matrix v1_strong = pca1.adapatedStrongEigenvectors();
    Matrix e1_czech = pca1.selectionMatrixOfStrongEigenvectors();
    int lambda1 = pca1.getCorrelationDimension();

    // pca of rv2
    Matrix v2 = pca2.getEigenvectors();
    Matrix v2_strong = pca2.adapatedStrongEigenvectors();
    Matrix e2_czech = pca2.selectionMatrixOfStrongEigenvectors();
    int lambda2 = pca2.getCorrelationDimension();

    // for all strong eigenvectors of rv2
    Matrix m1_czech = pca1.dissimilarityMatrix();
    for(int i = 0; i < v2_strong.getColumnDimensionality(); i++) {
      Matrix v2_i = v2_strong.getColumn(i);
      // check, if distance of v2_i to the space of rv1 > delta
      // (i.e., if v2_i spans up a new dimension)
      double dist = Math.sqrt(v2_i.transposeTimes(v2_i).get(0, 0) - v2_i.transposeTimes(m1_czech).times(v2_i).get(0, 0));

      // if so, insert v2_i into v1 and adjust v1
      // and compute m1_czech new, increase lambda1
      if(lambda1 < dimensionality && dist > delta) {
        adjust(v1, e1_czech, v2_i, lambda1++);
        m1_czech = v1.times(e1_czech).timesTranspose(v1);
      }
    }

    // for all strong eigenvectors of rv1
    Matrix m2_czech = pca2.dissimilarityMatrix();
    for(int i = 0; i < v1_strong.getColumnDimensionality(); i++) {
      Matrix v1_i = v1_strong.getColumn(i);
      // check, if distance of v1_i to the space of rv2 > delta
      // (i.e., if v1_i spans up a new dimension)
      double dist = Math.sqrt(v1_i.transposeTimes(v1_i).get(0, 0) - v1_i.transposeTimes(m2_czech).times(v1_i).get(0, 0));

      // if so, insert v1_i into v2 and adjust v2
      // and compute m2_czech new , increase lambda2
      if(lambda2 < dimensionality && dist > delta) {
        adjust(v2, e2_czech, v1_i, lambda2++);
        m2_czech = v2.times(e2_czech).timesTranspose(v2);
      }
    }

    int correlationDistance = Math.max(lambda1, lambda2);

    // TODO delta einbauen
    // Matrix m_1_czech = pca1.dissimilarityMatrix();
    // double dist_1 = normalizedDistance(dv1, dv2, m1_czech);
    // Matrix m_2_czech = pca2.dissimilarityMatrix();
    // double dist_2 = normalizedDistance(dv1, dv2, m2_czech);
    // if (dist_1 > delta || dist_2 > delta) {
    // correlationDistance++;
    // }

    return correlationDistance;
  }

  /**
   * Inserts the specified vector into the given orthonormal matrix
   * <code>v</code> at column <code>corrDim</code>. After insertion the matrix
   * <code>v</code> is orthonormalized and column <code>corrDim</code> of matrix
   * <code>e_czech</code> is set to the <code>corrDim</code>-th unit vector..
   * 
   * @param v the orthonormal matrix of the eigenvectors
   * @param e_czech the selection matrix of the strong eigenvectors
   * @param vector the vector to be inserted
   * @param corrDim the column at which the vector should be inserted
   */
  private void adjust(Matrix v, Matrix e_czech, Matrix vector, int corrDim) {
    int dim = v.getRowDimensionality();

    // set e_czech[corrDim][corrDim] := 1
    e_czech.set(corrDim, corrDim, 1);

    // normalize v
    Matrix v_i = vector.copy();
    Matrix sum = new Matrix(dim, 1);
    for(int k = 0; k < corrDim; k++) {
      Matrix v_k = v.getColumn(k);
      sum = sum.plus(v_k.times(v_i.scalarProduct(0, v_k, 0)));
    }
    v_i = v_i.minus(sum);
    v_i = v_i.times(1.0 / v_i.euclideanNorm(0));
    v.setColumn(corrDim, v_i);
  }

  /**
   * Computes the Euclidean distance between the given two vectors.
   * 
   * @param dv1 first FeatureVector
   * @param dv2 second FeatureVector
   * @return the Euclidean distance between the given two vectors
   */
  private double euclideanDistance(V dv1, V dv2) {
    if(dv1.getDimensionality() != dv2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + dv1.toString() + "\n  second argument: " + dv2.toString());
    }

    double sqrDist = 0;
    for(int i = 1; i <= dv1.getDimensionality(); i++) {
      double manhattanI = dv1.doubleValue(i) - dv2.doubleValue(i);
      sqrDist += manhattanI * manhattanI;
    }
    return Math.sqrt(sqrDist);
  }

  /**
   * @return the name of the default preprocessor, which is
   *         {@link de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedLocalPCAPreprocessor}
   */
  @Override
  public Class<?> getDefaultPreprocessorClass() {
    return KnnQueryBasedLocalPCAPreprocessor.class;
  }

  public String getPreprocessorDescription() {
    return "Preprocessor class to determine the correlation dimension of each object.";
  }

  /**
   * @return the super class for the preprocessor parameter, which is
   *         {@link LocalPCAPreprocessor}
   */
  public Class<P> getPreprocessorSuperClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(LocalPCAPreprocessor.class);
  }

  /**
   * @return the association ID for the association to be set by the
   *         preprocessor, which is
   *         {@link de.lmu.ifi.dbs.elki.database.AssociationID#LOCAL_PCA}
   */
  public AssociationID<?> getAssociationID() {
    return AssociationID.LOCAL_PCA;
  }
}