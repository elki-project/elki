package de.lmu.ifi.dbs.algorithm.clustering;

import java.util.*;

import de.lmu.ifi.dbs.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.AttributeWiseRealVectorNormalization;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Provides the k-means algorithm.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KMeans<D extends Distance<D>> extends DistanceBasedAlgorithm<RealVector, D> implements Clustering<RealVector> {

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
  private Clusters<RealVector> result;

  /**
   * Provides the k-means algorithm.
   */
  public KMeans() {
    super();
    optionHandler.put(K_P, new IntParameter(K_P,K_D,new GreaterConstraint(0)));
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
  public Clusters<RealVector> getResult() {
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  protected void runInTime(Database<RealVector> database)
      throws IllegalStateException {
    Random random = new Random();
    if (database.size() > 0) {
      // needs normalization to ensure the randomly generated means
      // are in the same range as the vectors in the database
      // XXX perhaps this can be done more conveniently?
      RealVector randomBase = database.get(database.iterator().next());
      AttributeWiseRealVectorNormalization normalization = new AttributeWiseRealVectorNormalization();
      List<RealVector> list = new ArrayList<RealVector>(database.size());
      for (Iterator<Integer> dbIter = database.iterator(); dbIter
          .hasNext();) {
        list.add((RealVector) database.get(dbIter.next()));
      }
      try {
        normalization.normalize(list);
      }
      catch (NonNumericFeaturesException e) {
        warning(e.getMessage());
      }
      List<RealVector> means = new ArrayList<RealVector>(k);
      List<RealVector> oldMeans;
      List<List<Integer>> clusters;
      if (isVerbose()) {
        verbose("initializing random vectors");
      }
      for (int i = 0; i < k; i++) {
        RealVector randomVector = (RealVector) randomBase
            .randomInstance(random);
        try {
          means.add((RealVector) normalization.restore(randomVector));
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
        oldMeans = new ArrayList<RealVector>(k);
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
      result = new Clusters<RealVector>(resultClusters, database);
    }
    else {
      result = new Clusters<RealVector>(new Integer[0][0], database);
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
  protected List<RealVector> means(List<List<Integer>> clusters,
                                   List<RealVector> means, Database<RealVector> database) {
    List<RealVector> newMeans = new ArrayList<RealVector>(k);
    for (int i = 0; i < k; i++) {
      List<Integer> list = clusters.get(i);
      RealVector mean = null;
      for (Iterator<Integer> clusterIter = list.iterator(); clusterIter.hasNext();) {
        if (mean == null) {
          mean = database.get(clusterIter.next());
        }
        else {
          mean = (RealVector) mean.plus(database.get(clusterIter.next()));
        }
      }
      if (list.size() > 0) {
        mean = (RealVector) mean.multiplicate(1.0 / list.size());
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
  protected List<List<Integer>> sort(List<RealVector> means,
                                     Database<RealVector> database) {
    List<List<Integer>> clusters = new ArrayList<List<Integer>>(k);
    for (int i = 0; i < k; i++) {
      clusters.add(new ArrayList<Integer>());
    }

    for (Iterator<Integer> dbIter = database.iterator(); dbIter.hasNext();) {
      List<D> distances = new ArrayList<D>(k);
      Integer id = dbIter.next();
      RealVector fv = database.get(id);
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

    k = (Integer)optionHandler.getOptionValue(K_P);
   
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of this algorithm.
   *
   * @return the parameter setting of this algorithm
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super
        .getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(K_P, Integer.toString(k));

    return attributeSettings;
  }

}