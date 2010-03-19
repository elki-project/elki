package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.LinearKernelFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.preprocessing.LocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides a kernel based locally weighted distance function. It is defined as
 * follows: result = max{dist<sub>P</sub>(P,Q), dist<sub>Q</sub>(Q,P)}, where
 * dist<sub>P</sub>(P,Q) computes the quadratic form distance on the weak
 * eigenvectors of the kernel matrix of P between two vectors P and Q in feature
 * space. Computation of the distance component of the weak eigenvectors is done
 * indirectly by computing the difference between the complete kernel distance
 * and the distance component of the strong eigenvectors: dist<sub>P</sub>(P,Q)
 * = dist<sub>P_weak</sub>(P,Q) = sqrt(dist<sub>P_complete</sub>(P,Q)^2 -
 * dist<sub>P_strong</sub>(P,Q)^2) K<sub>P_complete</sub>(P,Q) is the kernel
 * derived distance between P and Q. The distance component of the strong
 * eigenvectors K<sub>P_strong</sub>(P,Q) is computed as follows: First, the
 * vectors P and Q are projected onto the strong eigenvectors of the kernel
 * matrix of P, which results in the two vectors P<sub>p</sub> and
 * Q<sub>p</sub>. Then, the euclidean distance is used to compute the distance
 * between P<sub>p</sub> and Q<sub>p</sub>. In case of the linear kernel
 * function, this distance is identical to those computed by the
 * LocallyWeightedDistanceFunction with parameters big = 1.0 and small = 0.0
 * 
 * @author Simon Paradies
 * @param <V> the type of NumberVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 * 
 */
public class KernelBasedLocallyWeightedDistanceFunction<V extends NumberVector<V, ?>, P extends LocalPCAPreprocessor<V>> extends AbstractLocallyWeightedDistanceFunction<V, P> {
  /**
   * The default kernel function.
   */
  public static final Class<?> DEFAULT_KERNEL_FUNCTION_CLASS = LinearKernelFunction.class;

  /**
   * OptionID for {@link #KERNEL_FUNCTION_PARAM}
   */
  public static final OptionID KERNEL_FUNCTION_ID = OptionID.getOrCreateOptionID("kernel", "the kernel function which is used to compute the similarity." + "Default: " + DEFAULT_KERNEL_FUNCTION_CLASS);

  /**
   * Parameter for the kernel function
   */
  private ObjectParameter<KernelFunction<V, DoubleDistance>> KERNEL_FUNCTION_PARAM = new ObjectParameter<KernelFunction<V, DoubleDistance>>(KERNEL_FUNCTION_ID, KernelFunction.class, DEFAULT_KERNEL_FUNCTION_CLASS);

  /**
   * The kernel function that is used.
   */
  private KernelFunction<V, DoubleDistance> kernelFunction;

  /**
   * The global precomputed kernel matrix
   */
  private KernelMatrix<V> kernelMatrix;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public KernelBasedLocallyWeightedDistanceFunction(Parameterization config) {
    super(config);
    // kernel function
    if (config.grab(KERNEL_FUNCTION_PARAM)) {
      kernelFunction = KERNEL_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function.
   * 
   * @param v1 first DatabaseObject
   * @param v2 second DatabaseObject
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  public DoubleDistance distance(V v1, V v2) {
    double value;
    if(v1 != v2) {
      value = Math.max(computeDistance(v1, v2), computeDistance(v2, v1));
    }
    else {
      value = 0.0;
    }
    return new DoubleDistance(value);
  }

  /**
   * Computes the distance between two given real vectors according to this
   * distance function.
   * 
   * @param v1 first FeatureVector
   * @param v2 second FeatureVector
   * @return the distance between two given real vectors according to this
   *         distance function
   */
  private double computeDistance(final V v1, final V v2) {
    // get list of neighbor objects
    final List<Integer> neighbors = getDatabase().getAssociation(AssociationID.NEIGHBOR_IDS, v1.getID());

    // the columns in the kernel matrix corresponding to the two objects o1 and
    // o2
    // maybe kernel_o1 column has already been computed
    Matrix kernel_o1 = getDatabase().getAssociation(AssociationID.CACHED_MATRIX, v1.getID());
    Matrix kernel_o2;
    // has kernel_o1 column been computed yet
    if(kernel_o1 == null) {
      kernel_o1 = kernelMatrix.getSubColumn(v1.getID(), neighbors);
      kernel_o2 = kernelMatrix.getSubColumn(v2.getID(), neighbors);
      // save kernel_o1 column
      getDatabase().associate(AssociationID.CACHED_MATRIX, v1.getID(), kernel_o1);
    }
    else {
      kernel_o2 = kernelMatrix.getSubColumn(v2.getID(), neighbors);
    }

    // get the strong eigenvector matrix of object o1
    final Matrix strongEigenvectorMatrix = getDatabase().getAssociation(AssociationID.STRONG_EIGENVECTOR_MATRIX, v1.getID());

    // compute the delta vector
    final Matrix delta = kernel_o1.minus(kernel_o2);

    // project objects on principal components of kernel space
    final Matrix delta_projected = delta.transposeTimes(strongEigenvectorMatrix);

    // compute the squared distance on the principal components of the projected
    // objects
    final double distS = delta_projected.timesTranspose(delta_projected).get(0, 0);

    // compute the square of the complete kernel derived distance
    // final double distC = Math.pow(kernelFunction.distance(o1,
    // o2).getDoubleValue(),2.0);
    final double distC = kernelMatrix.getSquaredDistance(v1.getID(), v2.getID());

    // indirectly compute the distance on the weak components of the projected
    // objects using both other distances
    final double distW = Math.sqrt(Math.abs(distC - distS));
    return distW;
  }

  @Override
  public void setDatabase(Database<V> database, boolean verbose, boolean time) {
    // precompute kernelMatrix and store it in the database
    kernelMatrix = new KernelMatrix<V>(kernelFunction, database);
    KernelMatrix.centerKernelMatrix(kernelMatrix);
    database.associateGlobally(AssociationID.KERNEL_MATRIX, kernelMatrix);
    super.setDatabase(database, verbose, time);
  }

  /**
   * Returns the association ID for the association to be set by the
   * preprocessor.
   * 
   * @return the association ID for the association to be set by the
   *         preprocessor, which is
   *         {@link de.lmu.ifi.dbs.elki.database.AssociationID#STRONG_EIGENVECTOR_MATRIX}
   *         .
   */
  public AssociationID<Matrix> getAssociationID() {
    return AssociationID.STRONG_EIGENVECTOR_MATRIX;
  }
}
