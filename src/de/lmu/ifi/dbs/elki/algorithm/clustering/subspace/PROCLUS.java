package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparablePair;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparableTriple;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntDoublePair;

/**
 * <p/>
 * Provides the PROCLUS algorithm, an algorithm to find subspace clusters in high dimensional spaces.
 * </p>
 * <p/>
 * Reference:
 * <br>C. C. Aggrawal, C. Procopiuc, J. L. Wolf, P. S. Yu, J. S. Park:
 * Fast Algorithms for Projected Clustering.
 * <br>In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).
 * </p>
 *
 * @author Elke Achtert
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class PROCLUS<V extends RealVector<V, ?>> extends ProjectedClustering<V> {
    /**
     * OptionID for {@link #M_I_PARAM}
     */
    public static final OptionID M_I_ID = OptionID.getOrCreateOptionID(
        "proclus.mi",
        "The multiplier for the initial number of medoids."
    );

    /**
     * Parameter to specify the multiplier for the initial number of medoids,
     * must be an integer greater than 0.
     * <p>Default value: {@code 10} </p>
     * <p>Key: {@code -proclus.mi} </p>
     */
    private final IntParameter M_I_PARAM = new IntParameter(M_I_ID,
        new GreaterConstraint(0), 10);

    /**
     * Holds the value of {@link #M_I_PARAM}.
     */
    private int m_i;

    /**
     * Provides the PROCLUS algorithm,
     * adding parameter
     * {@link #M_I_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public PROCLUS() {
        super();
        // parameter m_i
        addOption(M_I_PARAM);

//    this.debug = true;
    }

    /**
     * Performs the PROCLUS algorithm on the given database.
     *
     */
    @Override
    protected Clustering<Model> runInTime(Database<V> database) throws IllegalStateException {

        try {
            getDistanceFunction().setDatabase(database, false, false);
            final int dim = getL();
            final int k = getK();
            final int k_i = getK_i();

            if (database.dimensionality() < dim)
                throw new IllegalStateException("Dimensionality of data < parameter l! " +
                    "(" + database.dimensionality() + " < " + dim + ")");

            // initialization phase
            int sampleSize = Math.min(database.size(), k_i * k);
            Set<Integer> sampleSet = database.randomSample(sampleSize, 1);

            int medoidSize = Math.min(database.size(), m_i * k);
            Set<Integer> medoids = greedy(sampleSet, medoidSize);

            if (debug) {
                debugFine("m " + medoids);
            }

            // iterative phase
            double bestObjective = Double.POSITIVE_INFINITY;
            Set<Integer> m_best = null;
            Set<Integer> m_bad = null;
            Set<Integer> m_current = initialSet(medoids, k);

            if (debug) {
                debugFine("m_c " + m_current);
            }

            Map<Integer, PROCLUSCluster> clusters = null;
            int loops = 0;
            while (loops < 10) {
                Map<Integer, Set<Integer>> dimensions = findDimensions(m_current, database);
                clusters = assignPoints(dimensions, database);
                double objectiveFunction = evaluateClusters(clusters, dimensions, database);

                if (objectiveFunction < bestObjective) {
                    loops = 0;
                    bestObjective = objectiveFunction;
                    m_best = m_current;
                    m_bad = computeBadMedoids(clusters, (int) (database.size() * 0.1 / getK()));
                }

                m_current = computeM_current(medoids, m_best, m_bad);
                loops++;
                if (isVerbose()) {
                    verbose("\rCurrent number of clusters: " + clusters.size() + ".                           ");
                }
            }

            if (isVerbose()) {
                verbose("\nNumber of clusters: " + clusters.size() + ".                           ");
            }

            //todo refinement phase ?
            
            Clustering<Model> res = new Clustering<Model>();
            for (Integer m_i : clusters.keySet()) {
                PROCLUSCluster c = clusters.get(m_i);
                DatabaseObjectGroup group = new DatabaseObjectGroupCollection<Set<Integer>>(c.objectIDs);
                res.addCluster(new Cluster<Model>(group, ClusterModel.CLUSTER));
            }
            setResult(res);
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return getResult();
    }

    public Description getDescription() {
        return new Description(
            "PROCLUS",
            "PROjected CLUStering",
            "Algorithm to find subspace clusters in high dimensional spaces.",
            "C. C. Aggrawal, C. Procopiuc, J. L. Wolf, P. S. Yu, J. S. Park: "
                + "Fast Algorithms for Projected Clustering. "
                + "In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).");
    }

    /**
     * Calls the super method
     * and sets additionally the value of the parameter
     * {@link #M_I_PARAM}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // m_i
        m_i = M_I_PARAM.getValue();

        return remainingParameters;
    }

    /**
     * Returns a piercing set of k medoids from the specified sample set.
     *
     * @param sampleSet the sample set
     * @param m         the number of medoids to be returned
     * @return a piercing set of m medoids from the specified sample set
     */
    private Set<Integer> greedy(Set<Integer> sampleSet, int m) {
        List<Integer> s = new ArrayList<Integer>(sampleSet);
        Set<Integer> medoids = new HashSet<Integer>();

        // m_1 is random point of S
        Random random = new Random(1);
        Integer m_i = s.remove(random.nextInt(s.size()));
        medoids.add(m_i);

        // compute distances between each point in S and m_i
        Map<Integer, IntDoublePair> distances = new HashMap<Integer, IntDoublePair>();
        for (Integer id : s) {
            DoubleDistance dist = getDistanceFunction().distance(id, m_i);
            distances.put(id, new IntDoublePair(id, dist.getValue()));
        }


        for (int i = 1; i < m; i++) {
            // choose medoid m_i to be far from prevois medoids
            List<IntDoublePair> d = new ArrayList<IntDoublePair>(distances.values());
            Collections.sort(d);

            m_i = d.get(d.size() - 1).getFirst();
            medoids.add(m_i);
            s.remove(m_i);
            distances.remove(m_i);

            // compute distances of each point to closest medoid
            for (Integer id : s) {
                DoubleDistance dist_new = getDistanceFunction().distance(id, m_i);
                double dist_old = distances.get(id).getSecond();
                double dist = Math.min(dist_new.getValue(), dist_old);
                distances.put(id, new IntDoublePair(id, dist));
            }
        }

        return medoids;
    }

    /**
     * Returns a set of k elements from the specified sample set.
     *
     * @param sampleSet the sample set
     * @param k         the number of samples to be returned
     * @return a set of k elements from the specified sample set
     */
    private Set<Integer> initialSet(Set<Integer> sampleSet, int k) {
        Random random = new Random(1);
        List<Integer> s = new ArrayList<Integer>(sampleSet);
        Set<Integer> initialSet = new HashSet<Integer>();
        while (initialSet.size() < k) {
            Integer next = s.remove(random.nextInt(s.size()));
            initialSet.add(next);
        }
        return initialSet;
    }

    /**
     * Computes the set of medoids in current iteration.
     *
     * @param m      the medoids
     * @param m_best the best set of medoids found so far
     * @param m_bad  the bad medoids
     * @return m_current, the set of medoids in current iteration
     */
    private Set<Integer> computeM_current(Set<Integer> m, Set<Integer> m_best, Set<Integer> m_bad) {
        Random random = new Random(1);
        List<Integer> m_list = new ArrayList<Integer>(m);
        for (Integer m_i : m_best) {
            m_list.remove(m_i);
        }

        Set<Integer> m_current = new HashSet<Integer>();
        for (Integer m_i : m_best) {
            if (m_bad.contains(m_i)) {
                int currentSize = m_current.size();
                while (m_current.size() == currentSize) {
                    Integer next = m_list.remove(random.nextInt(m_list.size()));
                    m_current.add(next);
                }
            }
            else {
                m_current.add(m_i);
            }
        }

        return m_current;
    }

    /**
     * Computes the localities of the specified medoids.
     *
     * @param m_c      the ids of the medoids
     * @param database the database holding the objects
     * @return a mapping of the medoid's id to its locality
     */
    private Map<Integer, List<ComparablePair<DoubleDistance, Integer>>> getLocalities(Set<Integer> m_c, Database<V> database) {
        Map<Integer, List<ComparablePair<DoubleDistance, Integer>>> result = new HashMap<Integer, List<ComparablePair<DoubleDistance, Integer>>>();
        for (Integer m : m_c) {
            // determine minimum distance between each point in m_c and m
            IntDoublePair minDist = null;
            for (Integer m_i : m_c) {
                if (m_i == m) continue;
                DoubleDistance dist = getDistanceFunction().distance(m, m_i);
                IntDoublePair currentDist = new IntDoublePair(m_i, dist.getValue());
                if (minDist == null || currentDist.compareTo(minDist) < 0)
                    minDist = currentDist;
            }

            // determine points in sphere centered at m with radius minDist
            assert minDist != null;
            List<ComparablePair<DoubleDistance, Integer>> qr = database.rangeQuery(m, Double.toString(minDist.getSecond()), getDistanceFunction());
            result.put(m, qr);
        }
        return result;
    }

    /**
     * Determines the set of correlated dimensions for each medoid in the specified medoid set.
     *
     * @param medoids  the set of medoids
     * @param database the database containing the objects
     * @return the set of correlated dimensions for each medoid in the specified medoid set
     */
    private Map<Integer, Set<Integer>> findDimensions(Set<Integer> medoids, Database<V> database) {
        Map<Integer, List<ComparablePair<DoubleDistance, Integer>>> localities = getLocalities(medoids, database);

        int dim = database.dimensionality();
        Map<Integer, double[]> averageDistances = new HashMap<Integer, double[]>();

        // compute x_ij = avg distance from points in l_i to medoid m_i
        for (Integer m_i : localities.keySet()) {
            V medoid_i = database.get(m_i);
            List<ComparablePair<DoubleDistance, Integer>> l_i = localities.get(m_i);
            double[] x_i = new double[dim];
            for (ComparablePair<DoubleDistance, Integer> qr : l_i) {
                V o = database.get(qr.getSecond());
                for (int d = 0; d < dim; d++) {
                    x_i[d] += Math.abs(medoid_i.getValue(d + 1).doubleValue() - o.getValue(d + 1).doubleValue());
                }
            }
            for (int j = 0; j < dim; j++) {
                x_i[j] /= l_i.size();
            }
            averageDistances.put(m_i, x_i);
        }

        Map<Integer, Set<Integer>> dimensionMap = new HashMap<Integer, Set<Integer>>();
        List<ComparableTriple<Double,Integer,Integer>> z_ijs = new ArrayList<ComparableTriple<Double,Integer,Integer>>();
        for (Integer m_i : medoids) {
            Set<Integer> dims_i = new HashSet<Integer>();
            dimensionMap.put(m_i, dims_i);

            double[] x_i = averageDistances.get(m_i);
            // y_i
            double y_i = 0;
            for (int j = 0; j < dim; j++) {
                y_i += x_i[j];
            }
            y_i /= dim;

            // sigma_i
            double sigma_i = 0;
            for (int j = 0; j < dim; j++) {
                double diff = x_i[j] - y_i;
                sigma_i += diff * diff;
            }
            sigma_i /= (dim - 1);
            sigma_i = Math.sqrt(sigma_i);

            for (int j = 0; j < dim; j++) {
                z_ijs.add(new ComparableTriple<Double,Integer,Integer>((x_i[j] - y_i) / sigma_i, m_i, j + 1));
            }
        }
        Collections.sort(z_ijs);

        int max = Math.max(getK() * getL(), 2);
        for (int m = 0; m < max; m++) {
          ComparableTriple<Double,Integer,Integer> z_ij = z_ijs.get(m);

            if (debug) {
                debugFine("z_ij " + z_ij);
            }

            Set<Integer> dims_i = dimensionMap.get(z_ij.getSecond());
            dims_i.add(z_ij.getThird());
        }
        return dimensionMap;
    }

    /**
     * Assigns the objects to the clusters.
     *
     * @param dimensions set of correlated dimensions for each medoid of the cluster
     * @param database   the database containing the objects
     * @return the assignments of the object to the clusters
     */
    private Map<Integer, PROCLUSCluster> assignPoints(Map<Integer, Set<Integer>> dimensions, Database<V> database) {
        Map<Integer, Set<Integer>> clusterIDs = new HashMap<Integer, Set<Integer>>();
        for (Integer m_i : dimensions.keySet()) {
            clusterIDs.put(m_i, new HashSet<Integer>());
        }

        for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
            Integer p_id = it.next();
            V p = database.get(p_id);
            IntDoublePair minDist = null;
            for (Integer m_i : dimensions.keySet()) {
                V m = database.get(m_i);
                double d_i = manhattanSegmentalDistance(p, m, dimensions.get(m_i));
                IntDoublePair currentDist = new IntDoublePair(m_i, d_i);
                if (minDist == null || currentDist.compareTo(minDist) < 0) {
                    minDist = currentDist;
                }
            }
            // add p to cluster with mindist
            assert minDist != null;
            Set<Integer> ids = clusterIDs.get(minDist.getFirst());
            ids.add(p_id);
        }

        Map<Integer, PROCLUSCluster> clusters = new HashMap<Integer, PROCLUSCluster>();
        for (Integer m_i : dimensions.keySet()) {
            Set<Integer> objectIDs = clusterIDs.get(m_i);
            if (!objectIDs.isEmpty()) {
                Set<Integer> clusterDimensions = dimensions.get(m_i);
                V centroid = DatabaseUtil.centroid(database, objectIDs);
                clusters.put(m_i, new PROCLUSCluster(objectIDs, clusterDimensions, centroid));
            }
        }
        return clusters;
    }

    /**
     * Returns the Manhattan segmental distance between o1 and o2 realtive to the specified dimensions.
     *
     * @param o1         the first object
     * @param o2         the second object
     * @param dimensions the dimensions to be considered
     * @return the Manhattan segmental distance between o1 and o2 realtive to the specified dimensions
     */
    private double manhattanSegmentalDistance(V o1, V o2, Set<Integer> dimensions) {
        double result = 0;
        for (Integer d : dimensions) {
            result += Math.abs(o1.getValue(d).doubleValue() - o2.getValue(d).doubleValue());
        }
        result /= dimensions.size();
        return result;
    }

    /**
     * Evaluates the quality of the clusters.
     *
     * @param clusters   the clusters to be evaluated
     * @param dimensions the dimensions associated with each cluster
     * @param database   the database holding the objects
     * @return a measure for the cluster quality
     */
    private double evaluateClusters(Map<Integer, PROCLUSCluster> clusters, Map<Integer, Set<Integer>> dimensions, Database<V> database) {
        double result = 0;
        for (Integer m_i : clusters.keySet()) {
            PROCLUSCluster c_i = clusters.get(m_i);
            V centroid_i = c_i.centroid;

            Set<Integer> dims_i = dimensions.get(m_i);
            double w_i = 0;
            for (Integer j : dims_i) {
                w_i += avgDistance(centroid_i, c_i.objectIDs, database, j);
            }

            w_i /= dimensions.keySet().size();
            result += c_i.objectIDs.size() * w_i;
        }

        return result / database.size();
    }

    /**
     * Computes the average distance of the objects to the centroid along the specified dimension.
     *
     * @param centroid  the centroid
     * @param objectIDs the set of objects ids
     * @param database  the database holding the objects
     * @param dimension the dimension for which the average distance is computed
     * @return the average distance of the objects to the centroid along the specified dimension
     */
    private double avgDistance(V centroid, Set<Integer> objectIDs, Database<V> database, int dimension) {
        double avg = 0;
        for (Integer objectID : objectIDs) {
            V o = database.get(objectID);
            avg += Math.abs(centroid.getValue(dimension).doubleValue() - o.getValue(dimension).doubleValue());
        }
        return avg / objectIDs.size();
    }

    /**
     * Computes the bad medoids, where the medoid of a cluster with less than the specified threshold of
     * objects is bad.
     *
     * @param clusters  the clusters
     * @param threshold the threshold
     * @return the bad medoids
     */
    private Set<Integer> computeBadMedoids(Map<Integer, PROCLUSCluster> clusters, int threshold) {
        Set<Integer> badMedoids = new HashSet<Integer>();
        for (Integer m_i : clusters.keySet()) {
            PROCLUSCluster c_i = clusters.get(m_i);
            if (c_i.objectIDs.size() < threshold) {
                badMedoids.add(m_i);
            }
        }
        return badMedoids;
    }

    /**
     * Encapsulates the attributes of a cluster.
     */
    private class PROCLUSCluster {
        /**
         * The ids of the objects belonging to this cluster.
         */
        Set<Integer> objectIDs;

        /**
         * The correlated dimensions of this cluster.
         */
        Set<Integer> dimensions;

        /**
         * The centroids of this cluster along each dimension.
         */
        V centroid;

        /**
         * Provides a new cluster with the specified parameters.
         *
         * @param objectIDs  the ids of the objects belonging to this cluster
         * @param dimensions the correlated dimensions of this cluster
         * @param centroid   the centroid of this cluster
         */
        public PROCLUSCluster(Set<Integer> objectIDs, Set<Integer> dimensions, V centroid) {
            this.objectIDs = objectIDs;
            this.dimensions = dimensions;
            this.centroid = centroid;
        }
    }
}
