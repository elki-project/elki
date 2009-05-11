package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Kernel Covariance Matrix Builder.
 * 
 * To use this, you NEED to run an appropriate prepocessor that sets {@link AssociationID#KERNEL_MATRIX}
 * for all objects that you are going to use.
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector class to use.
 */
public class KernelCovarianceMatrixBuilder<V extends RealVector<V, ?>> extends CovarianceMatrixBuilder<V> {
  /**
   * Returns the local kernel matrix of the specified ids.
   */
  @Override
  public Matrix processIds(Collection<Integer> ids, Database<V> database) {
    //get global kernel Matrix
    final KernelMatrix<V> kernelMatrix = ClassGenericsUtil.castWithGenericsOrNull(KernelMatrix.class, database.getGlobalAssociation(AssociationID.KERNEL_MATRIX));
    //get local submatrix
    final Matrix localKernelMatrix = kernelMatrix.getSubMatrix(ids);
    // return centered local kernel matrix
    return KernelMatrix.centerMatrix(localKernelMatrix);
  }
}
