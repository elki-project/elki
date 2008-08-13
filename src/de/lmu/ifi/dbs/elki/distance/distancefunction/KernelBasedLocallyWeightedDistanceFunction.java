package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.LinearKernelFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.util.List;

/**
 * Provides a kernel based locally weighted distance function.
 * It is defined as follows:
 * result = max{dist<sub>P</sub>(P,Q), dist<sub>Q</sub>(Q,P)}, where
 * dist<sub>P</sub>(P,Q) computes the quadratic form distance on the weak eigenvectors of the kernel matrix of P
 * between two vectors P and Q in feature space.
 * Computation of the distance component of the weak eigenvectors is done indirectly by computing the difference
 * between the complete kernel distance and the distance component of the strong eigenvectors:
 * dist<sub>P</sub>(P,Q) = dist<sub>P_weak</sub>(P,Q) = sqrt(dist<sub>P_complete</sub>(P,Q)^2 - dist<sub>P_strong</sub>(P,Q)^2)
 * K<sub>P_complete</sub>(P,Q) is the kernel derived distance between P and Q.
 * The distance component of the strong eigenvectors K<sub>P_strong</sub>(P,Q) is computed as follows:
 * First, the vectors P and Q are projected onto the strong eigenvectors of the kernel matrix of P, which results in
 * the two vectors P<sub>p</sub> and Q<sub>p</sub>. Then, the euclidean distance is used to compute the distance
 * between P<sub>p</sub> and Q<sub>p</sub>.
 * In case of the linear kernel function, this distance is identical to those computed by the LocallyWeightedDistanceFunction with
 * parameters big = 1.0 and small = 0.0
 *
 * @author Simon Paradies
 */
public class KernelBasedLocallyWeightedDistanceFunction<O extends RealVector<O,?>,P extends Preprocessor<O>> extends AbstractLocallyWeightedDistanceFunction<O,P> {
  /**
   * The default kernel function.
   */
  public static final String DEFAULT_KERNEL_FUNCTION_CLASS = LinearKernelFunction.class.getName();

  /**
   * Description for parameter kernel.
   */
  public static final String KERNEL_FUNCTION_CLASS_D = "the kernel function which is used to compute the similarity." +
                                                       "Default: " + DEFAULT_KERNEL_FUNCTION_CLASS;

  /**
   * Parameter for kernel.
   */
  public static final String KERNEL_FUNCTION_CLASS_P = "kernel";

  /**
   * The kernel function that is used.
   */
  private KernelFunction<O, DoubleDistance> kernelFunction;

  /**
   * The global precomputed kernel matrix
   */
  private KernelMatrix<O> kernelMatrix;

  /**
   * Provides a kernel based locally weighted distance function.
   */
  public KernelBasedLocallyWeightedDistanceFunction() {
    super();
    //kernel function
    ClassParameter<KernelFunction<O,DoubleDistance>> kernelFunctionClass = new ClassParameter(KERNEL_FUNCTION_CLASS_P, KERNEL_FUNCTION_CLASS_D, KernelFunction.class);
    kernelFunctionClass.setDefaultValue(DEFAULT_KERNEL_FUNCTION_CLASS);
    optionHandler.put(kernelFunctionClass);
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function.
   *
   * @param o1 first DatabaseObject
   * @param o2 second DatabaseObject
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  public DoubleDistance distance(O o1, O o2) {
    double value;
    if (o1 != o2) {
      value = Math.max(computeDistance(o1, o2), computeDistance(o2, o1));
    }
    else {
      value = 0.0;
    }
    return new DoubleDistance(value);
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // kernel function
    String kernelFunctionClass = (String) optionHandler.getOptionValue(KERNEL_FUNCTION_CLASS_P);
    remainingParameters = kernelFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();
    result.addAll(kernelFunction.getAttributeSettings());
    return result;
  }

  /**
   * Computes the distance between two given real vectors according to this
   * distance function.
   *
   * @param o1 first FeatureVector
   * @param o2 second FeatureVector
   * @return the distance between two given real vectors according to this
   *         distance function
   */
  private double computeDistance(final O o1, final O o2) {
    //get list of neighbor objects
    final List<Integer> neighbors = (List<Integer>) getDatabase().getAssociation(AssociationID.NEIGHBORS, o1.getID());

    //the colums in the kernel matrix corresponding to the two objects o1 and o2
    //maybe kernel_o1 column has already been computed
    Matrix kernel_o1 = (Matrix) getDatabase().getAssociation(AssociationID.CACHED_MATRIX, o1.getID());
    Matrix kernel_o2;
    //has kernel_o1 column been computed yet
    if (kernel_o1 == null) {
      kernel_o1 = kernelMatrix.getSubColumn(o1.getID(), neighbors);
      kernel_o2 = kernelMatrix.getSubColumn(o2.getID(), neighbors);
      //save kernel_o1 column
      getDatabase().associate(AssociationID.CACHED_MATRIX, o1.getID(), kernel_o1);
    }
    else {
      kernel_o2 = kernelMatrix.getSubColumn(o2.getID(), neighbors);
    }

    //get the strong eigenvector matrix of object o1
    final Matrix strongEigenvectorMatrix = (Matrix) getDatabase().getAssociation(AssociationID.STRONG_EIGENVECTOR_MATRIX, o1.getID());

    //compute the delta vector
    final Matrix delta = kernel_o1.minus(kernel_o2);

    //project objects on principal components of kernel space
    final Matrix delta_projected = delta.transpose().times(strongEigenvectorMatrix);

    //compute the squared distance on the principal components of the projected objects
    final double distS = delta_projected.times(delta_projected.transpose()).get(0, 0);

    //compute the square of the complete kernel derived distance
    //final double distC = Math.pow(kernelFunction.distance(o1, o2).getDoubleValue(),2.0);
    final double distC = kernelMatrix.getSquaredDistance(o1.getID(), o2.getID());

    //indirectly compute the distance on the weak components of the projected objects using both other distances
    final double distW = Math.sqrt(Math.abs(distC - distS));
    return distW;
  }

  /**
   * @see DistanceFunction#setDatabase(de.lmu.ifi.dbs.elki.database.Database, boolean, boolean)
   */
  @Override
  public void setDatabase(Database<O> database, boolean verbose, boolean time) {
    //precompute kernelMatrix and store it in the database
    kernelMatrix = new KernelMatrix<O>(kernelFunction, database);
    KernelMatrix.centerKernelMatrix(kernelMatrix);
    database.associateGlobally(AssociationID.KERNEL_MATRIX, kernelMatrix);
    super.setDatabase(database, verbose, time);
  }

  /**
   * Returns the assocoiation ID for the association to be set by the preprocessor.
   */
  AssociationID getAssociationID() {
    return AssociationID.STRONG_EIGENVECTOR_MATRIX;
  }
}
