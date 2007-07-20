package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.*;

/**
 * Provides the k-means algorithm.
 *
 * @author Arthur Zimek 
 */
public class KMeans<D extends Distance<D>,V extends RealVector<V, ? extends Number>> extends DistanceBasedAlgorithm<V, D> implements Clustering<V> {

  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "k - the number of clusters to find (positive integer)";

  /**
   * Keeps k - the number of clusters to find.
   */
  private int k;

  /**
   * Keeps the result.
   */
  private Clusters<V> result;

  /**
   * Provides the k-means algorithm.
   */
  public KMeans() {
    super();
    optionHandler.put(K_P, new IntParameter(K_P, K_D, new GreaterConstraint(0)));
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
   */
  public Description getDescription() {
    // TODO reference
    return new Description("K-Means", "K-Means",
                           "finds a partitioning into k clusters", "...");
  }

  /**
   * @see Clustering#getResult()
   */
  public Clusters<V> getResult() {
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  protected void runInTime(Database<V> database)
      throws IllegalStateException {
    Random random = new Random();
    if (database.size() > 0) {
      // needs normalization to ensure the randomly generated means
      // are in the same range as the vectors in the database
      // XXX perhaps this can be done more conveniently?
      V randomBase = database.get(database.iterator().next());
      AttributeWiseRealVectorNormalization<V> normalization = new AttributeWiseRealVectorNormalization<V>();
      List<V> list = new ArrayList<V>(database.size());
      for (Iterator<Integer> dbIter = database.iterator(); dbIter
          .hasNext();) {
        list.add(database.get(dbIter.next()));
      }
      try {
        normalization.normalize(list);
      }
      catch (NonNumericFeaturesException e) {
        warning(e.getMessage());
      }
      List<V> means = new ArrayList<V>(k);
      List<V> oldMeans;
      List<List<Integer>> clusters;
      if (isVerbose()) {
        verbose("initializing random vectors");
      }
      for (int i = 0; i < k; i++) {
        V randomVector = randomBase
            .randomInstance(random);
        try {
          means.add( normalization.restore(randomVector));
        }
        catch (NonNumericFeaturesException e) {
          warning(e.getMessage());
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
      Integer[][] resultClusters = new Integer[clusters.size()][];
      for (int i = 0; i < clusters.size(); i++) {
        List<Integer> cluster = clusters.get(i);
        resultClusters[i] = cluster
            .toArray(new Integer[cluster.size()]);
      }
      result = new Clusters<V>(resultClusters, database);
    }
    else {
      result = new Clusters<V>(new Integer[0][0], database);
    }
  }

  /**
   * Returns the mean vectors of the given clusters in the given database.
   *
   * @param clusters the clusters to compute the means
   * @param means    the recent means
   * @param database the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  protected List<V> means(List<List<Integer>> clusters,
                                   List<V> means, Database<V> database) {
    List<V> newMeans = new ArrayList<V>(k);
    for (int i = 0; i < k; i++) {
      List<Integer> list = clusters.get(i);
      V mean = null;
      for (Iterator<Integer> clusterIter = list.iterator(); clusterIter.hasNext();) {
        if (mean == null) {
          mean = database.get(clusterIter.next());
        }
        else {
          mean =  mean.plus(database.get(clusterIter.next()));
        }
      }
      if (list.size() > 0) {
        mean =  mean.multiplicate(1.0 / list.size());
      }
      else // mean == null
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
  protected List<List<Integer>> sort(List<V> means,
                                     Database<V> database) {
    List<List<Integer>> clusters = new ArrayList<List<Integer>>(k);
    for (int i = 0; i < k; i++) {
      clusters.add(new ArrayList<Integer>());
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
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    k = (Integer) optionHandler.getOptionValue(K_P);

    setParameters(args, remainingParameters);
    return remainingParameters;
  }
}