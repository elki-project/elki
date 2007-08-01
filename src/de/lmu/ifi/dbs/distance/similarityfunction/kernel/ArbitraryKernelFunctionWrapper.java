package de.lmu.ifi.dbs.distance.similarityfunction.kernel;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;

/**
 * Provides a wrapper for arbitrary kernel functions whose kernel matrix has been precomputed.
 *
 * @author Simon Paradies
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
	 * @see DistanceFunction#distance(de.lmu.ifi.dbs.data.DatabaseObject, de.lmu.ifi.dbs.data.DatabaseObject)
	 */
	public DoubleDistance similarity(final O o1, final O o2) {
		return new DoubleDistance(kernelMatrix.getSimilarity(o1.getID(), o2.getID()));
	}

	/**
	 * Returns the distance between the two specified objects.
	 *
	 * @param o1  first DatabaseObject
	 * @param o2  second DatabaseObject
	 * @return the distance between the two objcts specified by their obejct ids
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

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
	 */
	@Override
	public String description() {
		return "Arbitrary kernel function wrapper for FeatureVectors. No parameters required.";
	}

  /**
   * @see de.lmu.ifi.dbs.distance.MeasurementFunction#setDatabase(de.lmu.ifi.dbs.database.Database, boolean, boolean)
   */
  public void setDatabase(Database<O> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);
    //noinspection unchecked
    kernelMatrix = (KernelMatrix) getDatabase().getGlobalAssociation(AssociationID.KERNEL_MATRIX);
  }
}
