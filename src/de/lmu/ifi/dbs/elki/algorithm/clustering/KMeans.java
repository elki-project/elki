package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.logging.LogLevel;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Provides the k-means algorithm.
 * <p>Reference:
 * J. McQueen: Some Methods for Classification and Analysis of Multivariate Observations.
 * <br>In 5th Berkeley Symp. Math. Statist. Prob., Vol. 1, 1967, pp 281-297.
 * </p>
 * 
 * @author Arthur Zimek
 * @param <D> a type of {@link Distance} as returned by the used distance function
 * @param <V> a type of {@link RealVector} as a suitable datatype for this algorithm
 */
public class KMeans<D extends Distance<D>, V extends RealVector<V, ?>> extends DistanceBasedAlgorithm<V, D, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>,V> {

    /**
     * OptionID for {@link #K_PARAM}
     */
    public static final OptionID K_ID = OptionID.getOrCreateOptionID(
        "kmeans.k",
        "The number of clusters to find."
    );

    /**
     * Parameter to specify the number of clusters to find,
     * must be an integer greater than 0.
     * <p>Key: {@code -kmeans.k} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0));

    /**
     * Holds the value of {@link #K_PARAM}.
     */
    private int k;

    /**
     * Keeps the result.
     */
    private Clustering<Model> result;

    /**
     * Provides the k-means algorithm,
     * adding parameter
     * {@link #K_PARAM}
     * to the option handler
     * additionally to parameters of super class.
     */
    public KMeans() {
        super();
        addOption(K_PARAM);
    }

    public Description getDescription() {
        return new Description("K-Means",
            "K-Means",
            "Finds a partitioning into k clusters.",
            "J. McQueen: Some Methods for Classification and Analysis of Multivariate Observations. " +
                "In 5th Berkeley Symp. Math. Statist. Prob., Vol. 1, 1967, pp 281-297");
    }

    public Clustering<Model> getResult() {
        return result;
    }

    /**
     * Performs the k-means algorithm on the given database.
     */
    @Override
    protected Clustering<Model> runInTime(Database<V> database) throws IllegalStateException {
        Random random = new Random();
        if (database.size() > 0) {
            // needs normalization to ensure the randomly generated means
            // are in the same range as the vectors in the database
            // XXX perhaps this can be done more conveniently?
            V randomBase = database.get(database.iterator().next());
            AttributeWiseRealVectorNormalization<V> normalization = new AttributeWiseRealVectorNormalization<V>();
            List<V> list = new ArrayList<V>(database.size());
            for (Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();) {
                list.add(database.get(dbIter.next()));
            }
            try {
                normalization.normalize(list);
            }
            catch (NonNumericFeaturesException e) {
              LoggingUtil.logExpensive(LogLevel.WARNING, e.getMessage());
            }
            List<V> means = new ArrayList<V>(k);
            List<V> oldMeans;
            List<List<Integer>> clusters;
            if (isVerbose()) {
                verbose("initializing random vectors");
            }
            for (int i = 0; i < k; i++) {
                V randomVector = randomBase.randomInstance(random);
                try {
                    means.add(normalization.restore(randomVector));
                }
                catch (NonNumericFeaturesException e) {
                  logger.log(LogLevel.WARNING, e.getMessage());
                    means.add(randomVector);
                }
            }
            clusters = sort(means, database);
            boolean changed = true;
            int iteration = 1;
            while (changed) {
                if (isVerbose()) {
                    verbose("iteration " + iteration);
                }
                oldMeans = new ArrayList<V>(k);
                oldMeans.addAll(means);
                means = means(clusters, means, database);
                clusters = sort(means, database);
                changed = !means.equals(oldMeans);
                iteration++;
            }
            result = new Clustering<Model>();
            for (int i = 0; i < clusters.size(); i++) {
              DatabaseObjectGroup group = new DatabaseObjectGroupCollection<List<Integer>>(clusters.get(i));
              result.addCluster(new Cluster<Model>(group, ClusterModel.CLUSTER));
            }
        }
        else {
            result = new Clustering<Model>();
        }
        return result;
    }

    /**
     * Returns the mean vectors of the given clusters in the given database.
     *
     * @param clusters the clusters to compute the means
     * @param means    the recent means
     * @param database the database containing the vectors
     * @return the mean vectors of the given clusters in the given database
     */
    protected List<V> means(List<List<Integer>> clusters, List<V> means, Database<V> database) {
        List<V> newMeans = new ArrayList<V>(k);
        for (int i = 0; i < k; i++) {
            List<Integer> list = clusters.get(i);
            V mean = null;
            for (Iterator<Integer> clusterIter = list.iterator(); clusterIter.hasNext();) {
                if (mean == null) {
                    mean = database.get(clusterIter.next());
                }
                else {
                    mean = mean.plus(database.get(clusterIter.next()));
                }
            }
            if (list.size() > 0) {
                assert mean != null;
                mean = mean.multiplicate(1.0 / list.size());
            }
            else
            // mean == null
            {
                mean = means.get(i);
            }
            newMeans.add(mean);
        }
        return newMeans;
    }

    /**
     * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids
     * of those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
     *
     * @param means    a list of k means
     * @param database the database to cluster
     * @return list of k clusters
     */
    protected List<List<Integer>> sort(List<V> means, Database<V> database) {
        List<List<Integer>> clusters = new ArrayList<List<Integer>>(k);
        for (int i = 0; i < k; i++) {
            clusters.add(new LinkedList<Integer>());
        }

        for (Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();) {
            List<D> distances = new ArrayList<D>(k);
            Integer id = dbIter.next();
            V fv = database.get(id);
            int minIndex = 0;
            for (int d = 0; d < k; d++) {
                distances.add(getDistanceFunction().distance(fv, means.get(d)));
                if (distances.get(d).compareTo(distances.get(minIndex)) < 0) {
                    minIndex = d;
                }
            }
            clusters.get(minIndex).add(id);
        }
        for (List<Integer> cluster : clusters) {
            Collections.sort(cluster);
        }
        return clusters;
    }

    /**
     * Calls the super method
     * and sets additionally the value of the parameter
     * {@link #K_PARAM}.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // k
        k = K_PARAM.getValue();

        return remainingParameters;
    }
}