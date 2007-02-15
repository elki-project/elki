package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.math.linearalgebra.EigenvalueDecomposition;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.*;

/**
 * Abstract superclass for PROCLUS and ORCLUS.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public abstract class ProjectedClustering extends AbstractAlgorithm<RealVector> implements Clustering<RealVector> {
	/**
	 * Parameter k.
	 */
	public static final String K_P = "k";

	/**
	 * Description for parameter k.
	 */
	public static final String K_D = "positive integer value to specify the number of clusters to be found";

	/**
	 * Parameter k_i.
	 */
	public static final String K_I_P = "k_i";

	/**
	 * Default value for k_i.
	 */
	public static final int K_I_DEFAULT = 15;

	/**
	 * Description for parameter k_i.
	 */
	public static final String K_I_D = "positive integer value to specify the multiplier for " + "the initial number of seeds, default: "
			+ ProjectedClustering.K_I_DEFAULT;

	/**
	 * Parameter l.
	 */
	public static final String L_P = "l";

	/**
	 * Description for parameter l.
	 */
	public static final String L_D = "positive integer value to specify the dimensionality of the clusters to be found";

	/**
	 * Number of clusters.
	 */
	private int k;

	/**
	 * Multiplier for initial number of seeds.
	 */
	private int k_i;

	/**
	 * Dimensionality of the clusters.
	 */
	private int l;

	/**
	 * The euklidean distance function.
	 */
	private EuklideanDistanceFunction<RealVector> distanceFunction = new EuklideanDistanceFunction<RealVector>();

	/**
	 * The result.
	 */
	private Clusters<RealVector> result;

	/**
	 * Sets the parameter k and l the optionhandler additionally to the
	 * parameters provided by super-classes.
	 */
	public ProjectedClustering() {
		super();
		// parameter k
		optionHandler.put(ProjectedClustering.K_P, new IntParameter(ProjectedClustering.K_P, ProjectedClustering.K_D,
				new GreaterConstraint(0)));

		// parameter k_i
		IntParameter ki = new IntParameter(ProjectedClustering.K_I_P, ProjectedClustering.K_I_D, new GreaterConstraint(0));
		ki.setDefaultValue(ProjectedClustering.K_I_DEFAULT);
		optionHandler.put(ProjectedClustering.K_I_P, ki);

		// parameter dim
		optionHandler.put(ProjectedClustering.L_P, new IntParameter(ProjectedClustering.L_P, ProjectedClustering.L_D,
				new GreaterConstraint(0)));
	}

	/**
	 * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
	 */
	protected void runInTime(Database<RealVector> database) throws IllegalStateException {
		/*
		 * 
		 * try { if (database.dimensionality() < dim) throw new
		 * IllegalStateException( "Dimensionality of data < parameter l! " + "(" +
		 * database.dimensionality() + " < " + dim + ")");
		 *  // current number of seeds int k_c = Math.min(database.size(), k_i *
		 * k);
		 *  // current dimensionality associated with each seed int dim_c =
		 * database.dimensionality();
		 *  // pick k0 > k points from the database List<Cluster> clusters =
		 * initialSeeds(database, k_c);
		 * 
		 * double beta = Math .exp(-Math.log((double) dim_c / (double) dim)
		 * Math.log(1 / alpha) / Math.log((double) k_c / (double) k));
		 * 
		 * while (k_c > k) { if (isVerbose()) { verbose("\rCurrent number of
		 * clusters: " + clusters.size() + ". "); }
		 *  // find partitioning induced by the seeds of the clusters
		 * assign(database, clusters);
		 *  // determine current subspace associated with each cluster for
		 * (ProjectedClustering.Cluster cluster : clusters) { if
		 * (cluster.objectIDs.size() > 0) cluster.basis = findBasis(database,
		 * cluster, dim_c); }
		 *  // reduce number of seeds and dimensionality associated with // each
		 * seed k_c = (int) Math.max(k, k_c * alpha); dim_c = (int)
		 * Math.max(dim, dim_c * beta); merge(database, clusters, k_c, dim_c); }
		 * assign(database, clusters);
		 * 
		 * if (isVerbose()) { verbose("\nNumber of clusters: " + clusters.size() + ".
		 * "); }
		 *  // get the result Integer[][] ids = new Integer[clusters.size()][];
		 * int i = 0; for (ProjectedClustering.Cluster c : clusters) { ids[i++] =
		 * c.objectIDs.toArray(new Integer[c.objectIDs.size()]); } this.result =
		 * new Clusters<RealVector>(ids, database); } catch (Exception e) {
		 * e.printStackTrace(); throw new IllegalStateException(e); }
		 */
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		// k
		k = (Integer) optionHandler.getOptionValue(ProjectedClustering.K_P);

		// l
		l = (Integer) optionHandler.getOptionValue(ProjectedClustering.L_P);

		// k_i
		k_i = (Integer) optionHandler.getOptionValue(ProjectedClustering.K_I_P);

		return remainingParameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.algorithm.clustering.Clustering#getResult()
	 */
	public Clusters<RealVector> getResult() {
		return result;
	}

	/**
	 * Returns the distance function.
	 * 
	 * @return the distance function
	 */
	protected EuklideanDistanceFunction<RealVector> getDistanceFunction() {
		return distanceFunction;
	}

	/**
	 * Returns the value of parameter k.
	 * 
	 * @return the number of clusters to be found
	 */
	protected int getK() {
		return k;
	}

	/**
	 * Returns the value of parameter k_i.
	 * 
	 * @return the initial number of clusters
	 */
	protected int getK_i() {
		return k_i;
	}

	/**
	 * Returns the value of parameter l.
	 * 
	 * @return the average dimesnionality of the clusters to be found
	 */
	protected int getL() {
		return l;
	}

	/**
	 * Sets the result of this algorithm.
	 * 
	 * @param result
	 *            the result to be set
	 */
	protected void setResult(Clusters<RealVector> result) {
		this.result = result;
	}

	/**
	 * Creates a partitioning of the database by assigning each object to its
	 * closest seed.
	 * 
	 * @param database
	 *            the database holding the objects
	 * @param clusters
	 *            the array of clusters to which the objects should be assigned
	 *            to
	 */
	private void assign(Database<RealVector> database, List<ProjectedClustering.Cluster> clusters) {
		// clear the current clusters
		for (ProjectedClustering.Cluster cluster : clusters) {
			cluster.objectIDs.clear();
		}

		// projected centroids of the clusters
		RealVector[] projectedCentroids = new RealVector[clusters.size()];
		for (int i = 0; i < projectedCentroids.length; i++) {
			ProjectedClustering.Cluster c = clusters.get(i);
			projectedCentroids[i] = projection(c, c.centroid);
		}

		// for each data point o do
		Iterator<Integer> it = database.iterator();
		while (it.hasNext()) {
			Integer id = it.next();
			RealVector o = database.get(id);

			DoubleDistance minDist = null;
			ProjectedClustering.Cluster minCluster = null;

			// determine projected distance between o and cluster
			for (int i = 0; i < projectedCentroids.length; i++) {
				ProjectedClustering.Cluster c = clusters.get(i);
				RealVector o_proj = projection(c, o);
				DoubleDistance dist = distanceFunction.distance(o_proj, projectedCentroids[i]);
				if (minDist == null || minDist.compareTo(dist) > 0) {
					minDist = dist;
					minCluster = c;
				}
			}
			// add p to the cluster with the least value of projected distance
			assert minCluster != null;
			minCluster.objectIDs.add(id);
		}

		// recompute the seed in each clusters
		for (ProjectedClustering.Cluster cluster : clusters) {
			if (cluster.objectIDs.size() > 0)
				cluster.centroid = Util.centroid(database, cluster.objectIDs);
		}
	}

	/**
	 * Finds the basis of the subspace of dimensionality
	 * <code> for the specified cluster.
	 *
	 * @param cluster the cluster
	 * @param dim     the dimensionality of the subspace
	 * @return matrix defining the basis of the subspace for the specified cluster
	 */
	private Matrix findBasis(Database<RealVector> database, ProjectedClustering.Cluster cluster, int dim) {
		// covariance matrix of cluster
		Matrix covariance = Util.covarianceMatrix(database, cluster.objectIDs);

		// eigenvectors in ascending order
		EigenvalueDecomposition evd = covariance.eig();
		SortedEigenPairs eigenPairs = new SortedEigenPairs(evd, true);

		// eigenvectors corresponding to the smallest dim eigenvalues
		return eigenPairs.eigenVectors(dim);
	}

	/**
	 * Reduces the number of seeds to k_new
	 * 
	 * @param database
	 *            the database holding the objects
	 * @param clusters
	 *            the set of current seeds
	 * @param k_new
	 *            the new number of seeds
	 * @param d_new
	 *            the new dimensionality of the subspaces for each seed
	 */
	private void merge(Database<RealVector> database, List<ProjectedClustering.Cluster> clusters, int k_new, int d_new) {
		ArrayList<ProjectedClustering.ProjectedEnergy> projectedEnergies = new ArrayList<ProjectedClustering.ProjectedEnergy>();
		for (int i = 0; i < clusters.size(); i++) {
			for (int j = 0; j < clusters.size(); j++) {
				if (i >= j)
					continue;
				// projected energy of c_ij in subspace e_ij
				ProjectedClustering.Cluster c_i = clusters.get(i);
				ProjectedClustering.Cluster c_j = clusters.get(j);

				ProjectedClustering.ProjectedEnergy pe = projectedEnergy(database, c_i, c_j, i, j, d_new);
				projectedEnergies.add(pe);
			}
		}

		while (clusters.size() > k_new) {
			if (isVerbose()) {
				verbose("\rCurrent number of clusters: " + clusters.size() + ".                           ");
			}
			// find the smallest value of r_ij
			ProjectedClustering.ProjectedEnergy minPE = Collections.min(projectedEnergies);

			// renumber the clusters by replacing cluster c_i with cluster c_ij
			// and discarding cluster c_j
			for (int c = 0; c < clusters.size(); c++) {
				if (c == minPE.i) {
					clusters.remove(c);
					clusters.add(c, minPE.cluster);
				}
				if (c == minPE.j) {
					clusters.remove(c);
				}
			}

			// remove obsolete projected energies and renumber the others ...
			int i = minPE.i;
			int j = minPE.j;
			Iterator<ProjectedClustering.ProjectedEnergy> it = projectedEnergies.iterator();
			while (it.hasNext()) {
				ProjectedClustering.ProjectedEnergy pe = it.next();
				if (pe.i == i || pe.i == j || pe.j == i || pe.j == j) {
					it.remove();
				} else {
					if (pe.i > j) {
						pe.i -= 1;
					}
					if (pe.j > j) {
						pe.j -= 1;
					}
				}
			}

			// ... and recompute them
			ProjectedClustering.Cluster c_ij = minPE.cluster;
			for (int c = 0; c < clusters.size(); c++) {
				if (c < i) {
					projectedEnergies.add(projectedEnergy(database, clusters.get(c), c_ij, c, i, d_new));
				} else if (c > i) {
					projectedEnergies.add(projectedEnergy(database, clusters.get(c), c_ij, i, c, d_new));
				}
			}
		}
	}

	/**
	 * Computes the projected energy of the specified clusters. The projected
	 * energy is given by the mean square distance of the points to the centroid
	 * of the union cluster c, when all points in c are projected to the
	 * subspace of c.
	 * 
	 * @param database
	 *            the database holding the objects
	 * @param c_i
	 *            the first cluster
	 * @param c_j
	 *            the second cluster
	 * @param i
	 *            the index of cluster c_i in the cluster list
	 * @param j
	 *            the index of cluster c_j in the cluster list
	 * @return the projected energy of the specified cluster
	 */
	private ProjectedClustering.ProjectedEnergy projectedEnergy(Database<RealVector> database, ProjectedClustering.Cluster c_i,
			ProjectedClustering.Cluster c_j, int i, int j, int dim) {
		// union of cluster c_i and c_j
		ProjectedClustering.Cluster c_ij = union(database, c_i, c_j, dim);

		DoubleDistance sum = distanceFunction.nullDistance();
		RealVector c_proj = projection(c_ij, c_ij.centroid);
		for (Integer id : c_ij.objectIDs) {
			RealVector o = database.get(id);
			RealVector o_proj = projection(c_ij, o);
			DoubleDistance dist = distanceFunction.distance(o_proj, c_proj);
			sum = sum.plus(dist.times(dist));
		}
		DoubleDistance projectedEnergy = sum.times(1.0 / c_ij.objectIDs.size());

		return new ProjectedClustering.ProjectedEnergy(i, j, c_ij, projectedEnergy);
	}

	/**
	 * Returns the union of the two specified clusters.
	 * 
	 * @param database
	 *            the database holding the objects
	 * @param c1
	 *            the first cluster
	 * @param c2
	 *            the second cluster
	 * @param dim
	 *            the dimensionality of the union cluster
	 * @return the union of the two specified clusters
	 */
	private ProjectedClustering.Cluster union(Database<RealVector> database, ProjectedClustering.Cluster c1,
			ProjectedClustering.Cluster c2, int dim) {
		ProjectedClustering.Cluster c = new ProjectedClustering.Cluster();

		HashSet<Integer> ids = new HashSet<Integer>(c1.objectIDs);
		ids.addAll(c2.objectIDs);

		c.objectIDs = new ArrayList<Integer>(ids);

		if (c.objectIDs.size() > 0) {
			c.centroid = Util.centroid(database, c.objectIDs);
			c.basis = findBasis(database, c, dim);
		} else {
			// noinspection unchecked
			c.centroid = (RealVector) c1.centroid.plus(c2.centroid).multiplicate(0.5);

			double[][] doubles = new double[c1.basis.getRowDimensionality()][dim];
			for (int i = 0; i < dim; i++) {
				doubles[i][i] = 1;
			}
			c.basis = new Matrix(doubles);
		}

		return c;
	}

	/**
	 * Returns the projection of real vector o in the subspace of cluster c.
	 * 
	 * @param c
	 *            the cluster
	 * @param o
	 *            the double vector
	 * @return the projection of double vector o in the subspace of cluster c
	 */
	private RealVector projection(ProjectedClustering.Cluster c, RealVector o) {
		Matrix o_proj = o.getRowVector().times(c.basis);
		double[] values = o_proj.getColumnPackedCopy();
		return o.newInstance(values);
	}

	/**
	 * Encapsulates the attributes of a cluster.
	 */
	private class Cluster {
		/**
		 * The ids of the objects belonging to this cluster.
		 */
		List<Integer> objectIDs = new ArrayList<Integer>();

		/**
		 * The matrix defining the subspace of this cluster.
		 */
		Matrix basis;

		/**
		 * The centroid of this cluster.
		 */
		RealVector centroid;

		/**
		 * Creates a new empty cluster.
		 */
		Cluster() {
		}

		/**
		 * Creates a new cluster containing the specified object o.
		 * 
		 * @param o
		 *            the object belonging to this cluster.
		 */
		Cluster(RealVector o) {
			this.objectIDs.add(o.getID());

			// initially the basis ist the original axis-system
			int dim = o.getDimensionality();
			double[][] doubles = new double[dim][dim];
			for (int i = 0; i < dim; i++) {
				doubles[i][i] = 1;
			}
			this.basis = new Matrix(doubles);

			// initially the centroid is the value array of o
			double[] values = new double[o.getDimensionality()];
			for (int d = 1; d <= o.getDimensionality(); d++)
				values[d - 1] = o.getValue(d).doubleValue();
			this.centroid = o.newInstance(values);
		}
	}

	private class ProjectedEnergy implements Comparable<ProjectedClustering.ProjectedEnergy> {
		int i;

		int j;

		ProjectedClustering.Cluster cluster;

		DoubleDistance projectedEnergy;

		ProjectedEnergy(int i, int j, ProjectedClustering.Cluster cluster, DoubleDistance projectedEnergy) {
			this.i = i;
			this.j = j;
			this.cluster = cluster;
			this.projectedEnergy = projectedEnergy;
		}

		/**
		 * Compares this object with the specified object for order.
		 * 
		 * @param o
		 *            the Object to be compared.
		 * @return a negative integer, zero, or a positive integer as this
		 *         object is less than, equal to, or greater than the specified
		 *         object.
		 */
		public int compareTo(ProjectedClustering.ProjectedEnergy o) {
			return this.projectedEnergy.compareTo(o.projectedEnergy);
		}
	}
}
