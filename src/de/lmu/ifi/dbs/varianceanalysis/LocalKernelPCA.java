package de.lmu.ifi.dbs.varianceanalysis;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.similarityfunction.kernel.KernelMatrix;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;

import java.util.Collection;

/**
 * Performs a local kernel PCA based on the kernel matrices of given
 * objects.
 *
 * @author Simon Paradies
 */
public class LocalKernelPCA<V extends RealVector<V,? >> extends LocalPCA<V> {

  /**
   * Returns the local kernel matrix of the specified ids.
   *
   * @see LocalPCA#pcaMatrix(de.lmu.ifi.dbs.database.Database, java.util.Collection)
   */
  @Override
  protected Matrix pcaMatrix(final Database<V> database, final Collection<Integer> ids) {
    //get global kernel Matrix
    final KernelMatrix<V> kernelMatrix = (KernelMatrix<V>) database.getGlobalAssociation(AssociationID.KERNEL_MATRIX);
    //get local submatrix
    final Matrix localKernelMatrix = kernelMatrix.getSubMatrix(ids);
    // return centered local kernel matrix
    return KernelMatrix.centerMatrix(localKernelMatrix);
  }
}
