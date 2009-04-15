package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;

/**
 * Provides a wrapper for arbitrary kernel functions whose kernel matrix has been precomputed.
 *
 * @author Simon Paradies
 * @param <O> object type
 */
public class ArbitraryKernelFunctionWrapper<O extends RealVector<O, ? >> extends AbstractDoubleKernelFunction<O>{

	/**
	 * The global kernel Matrix.
	 */
	private KernelMatrix<O> kernelMatrix;

	/**
	 * Provides a wrapper for arbitrary kernel functions whose kernel matrix has already been precomputed.
	 */
	public ArbitraryKernelFunctionWrapper() {
		super();
  }

	/**
	 * Provides a wrapper for arbitrary kernel functions whose kernel matrix has already been precomputed.
	 * Returns the value that is stored in the kernel matrix which itself is stored in the database.
	 * @param o1 first vector
	 * @param o2 second vector
	 * @return the linear kernel similarity which is stored in the Global Database
	 */
	public DoubleDistance similarity(final O o1, final O o2) {
		return new DoubleDistance(kernelMatrix.getSimilarity(o1.getID(), o2.getID()));
	}

	/**
	 * Returns the distance between the two specified objects.
	 *
	 * @param o1  first DatabaseObject
	 * @param o2  second DatabaseObject
	 * @return the distance between the two object specified by their object ids
	 */
	@Override
	public DoubleDistance distance(final O o1, final O o2) {
		return distance(o1.getID(), o2.getID());
	}

	/**
	 * Returns the distance between the two objects specified by their object ids.
	 *
	 * @param id1 first object id
	 * @param id2 second object id
	 * @return the distance between the two objects specified by their object ids
	 */
	@Override
	public DoubleDistance distance(final Integer id1, final Integer id2) {
		return new DoubleDistance(kernelMatrix.getDistance(id1, id2));
	}

	@Override
	public String parameterDescription() {
		return "Arbitrary kernel function wrapper for FeatureVectors. No parameters required.";
	}

  @Override
  @SuppressWarnings("unchecked")
  public void setDatabase(Database<O> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);
    kernelMatrix = (KernelMatrix<O>) getDatabase().getGlobalAssociation(AssociationID.KERNEL_MATRIX);
  }
}
