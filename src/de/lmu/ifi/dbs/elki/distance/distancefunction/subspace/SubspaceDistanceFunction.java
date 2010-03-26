package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.SubspaceDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPreprocessorBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocalPCAPreprocessorBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.WeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedLocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.LocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Provides a distance function to determine a kind of correlation distance
 * between two points, which is a pair consisting of the distance between the
 * two subspaces spanned by the strong eigenvectors of the two points and the
 * affine distance between the two subspaces.
 * 
 * @author Elke Achtert
 * @param <V> the type of NumberVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
public class SubspaceDistanceFunction<V extends NumberVector<V, ?>, P extends LocalPCAPreprocessor<V>> extends AbstractPreprocessorBasedDistanceFunction<V, P, SubspaceDistance> implements LocalPCAPreprocessorBasedDistanceFunction<V, P, SubspaceDistance> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public SubspaceDistanceFunction(Parameterization config) {
    super(config, new SubspaceDistance());
  }

  /**
   * @return the name of the default preprocessor, which is
   *         {@link de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedLocalPCAPreprocessor}
   */
  @Override
  public Class<?> getDefaultPreprocessorClass() {
    return KnnQueryBasedLocalPCAPreprocessor.class;
  }

  public final String getPreprocessorDescription() {
    return "Preprocessor class to determine the correlation dimension of each object.";
  }

  /**
   * @return the super class for the preprocessor, which is
   *         {@link de.lmu.ifi.dbs.elki.preprocessing.Preprocessor}
   */
  public Class<P> getPreprocessorSuperClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(LocalPCAPreprocessor.class);
  }

  /**
   * @return the association ID for the association to be set by the
   *         preprocessor, which is {@link AssociationID#LOCAL_PCA}
   */
  public AssociationID<?> getAssociationID() {
    return AssociationID.LOCAL_PCA;
  }

  /**
   * Note, that the pca of o1 must have equal ore more strong eigenvectors than
   * the pca of o2.
   * 
   */
  public SubspaceDistance distance(V o1, V o2) {
    // noinspection unchecked
    PCAFilteredResult pca1 = getDatabase().getAssociation(AssociationID.LOCAL_PCA, o1.getID());
    // noinspection unchecked
    PCAFilteredResult pca2 = getDatabase().getAssociation(AssociationID.LOCAL_PCA, o2.getID());
    return distance(o1, o2, pca1, pca2);
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function. Note, that the first pca must have an equal number of
   * strong eigenvectors than the second pca.
   * 
   * @param o1 first DatabaseObject
   * @param o2 second DatabaseObject
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  public SubspaceDistance distance(V o1, V o2, PCAFilteredResult pca1, PCAFilteredResult pca2) {
    if(pca1.getCorrelationDimension() != pca2.getCorrelationDimension()) {
      throw new IllegalStateException("pca1.getCorrelationDimension() != pca2.getCorrelationDimension()");
    }

    Matrix strong_ev1 = pca1.getStrongEigenvectors();
    Matrix weak_ev2 = pca2.getWeakEigenvectors();
    Matrix m1 = weak_ev2.getColumnDimensionality() == 0 ? strong_ev1.transpose() : strong_ev1.transposeTimes(weak_ev2);
    double d1 = m1.norm2();

    WeightedDistanceFunction<V> df1 = new WeightedDistanceFunction<V>(pca1.similarityMatrix());
    WeightedDistanceFunction<V> df2 = new WeightedDistanceFunction<V>(pca2.similarityMatrix());

    double affineDistance = Math.max(df1.distance(o1, o2).doubleValue(), df2.distance(o1, o2).doubleValue());

    return new SubspaceDistance(d1, affineDistance);
  }
}
