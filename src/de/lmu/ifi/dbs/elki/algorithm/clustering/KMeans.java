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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.normalization.AttributeWiseMinMaxNormalization;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Provides the k-means algorithm.
 * <p>
 * Reference: J. McQueen: Some Methods for Classification and Analysis of
 * Multivariate Observations. <br>
 * In 5th Berkeley Symp. Math. Statist. Prob., Vol. 1, 1967, pp 281-297.
 * </p>
 * 
 * @author Arthur Zimek
 * @param <D> a type of {@link Distance} as returned by the used distance
 *        function
 * @param <V> a type of {@link NumberVector} as a suitable datatype for this
 *        algorithm
 */
@Title("K-Means")
@Description("Finds a partitioning into k clusters.")
@Reference(authors = "J. McQueen", title = "Some Methods for Classification and Analysis of Multivariate Observations", booktitle = "5th Berkeley Symp. Math. Statist. Prob., Vol. 1, 1967, pp 281-297")
public class KMeans<D extends Distance<D>, V extends NumberVector<V, ?>> extends DistanceBasedAlgorithm<V, D, Clustering<MeanModel<V>>> implements ClusteringAlgorithm<Clustering<MeanModel<V>>, V> {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("kmeans.k", "The number of clusters to find.");

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater than 0.
   * <p>
   * Key: {@code -kmeans.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0));

  /**
   * OptionID for {@link #MAXITER_PARAM}
   */
  public static final OptionID MAXITER_ID = OptionID.getOrCreateOptionID("kmeans.maxiter", "The maximum number of iterations to do. 0 means no limit.");

  /**
   * Parameter to specify the number of clusters to find, must be an integer
   * greater or equal to 0, where 0 means no limit.
   * <p>
   * Key: {@code -kmeans.maxiter}
   * </p>
   */
  private final IntParameter MAXITER_PARAM = new IntParameter(MAXITER_ID, new GreaterEqualConstraint(0), 0);

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  private int k;

  /**
   * Holds the value of {@link #MAXITER_PARAM}.
   */
  private int maxiter;

  /**
   * Provides the k-means algorithm, adding parameter {@link #K_PARAM} to the
   * option handler additionally to parameters of super class.
   */
  public KMeans(Parameterization config) {
    super(config);
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
    if(config.grab(MAXITER_PARAM)) {
      maxiter = MAXITER_PARAM.getValue();
    }
  }

  /**
   * Performs the k-means algorithm on the given database.
   */
  @Override
  protected Clustering<MeanModel<V>> runInTime(Database<V> database) throws IllegalStateException {
    Random random = new Random();
    if(database.size() > 0) {
      // needs normalization to ensure the randomly generated means
      // are in the same range as the vectors in the database
      // XXX perhaps this can be done more conveniently?
      V randomBase = database.get(database.iterator().next());
      EmptyParameterization parameters = new EmptyParameterization();
      AttributeWiseMinMaxNormalization<V> normalization = new AttributeWiseMinMaxNormalization<V>(parameters);
      for(ParameterException e : parameters.getErrors()) {
        logger.warning("Error in internal parameterization: " + e.getMessage());
      }
      List<V> list = new ArrayList<V>(database.size());
      for(Integer id : database) {
        list.add(database.get(id));
      }
      try {
        normalization.normalize(list);
      }
      catch(NonNumericFeaturesException e) {
        logger.warning(e.getMessage());
      }
      List<V> means = new ArrayList<V>(k);
      List<V> oldMeans;
      List<List<Integer>> clusters;
      if(logger.isVerbose()) {
        logger.verbose("initializing random vectors");
      }
      for(int i = 0; i < k; i++) {
        V randomVector = randomBase.randomInstance(random);
        try {
          means.add(normalization.restore(randomVector));
        }
        catch(NonNumericFeaturesException e) {
          logger.warning(e.getMessage());
          means.add(randomVector);
        }
      }
      clusters = sort(means, database);
      boolean changed = true;
      int iteration = 1;
      while(changed) {
        if(logger.isVerbose()) {
          logger.verbose("iteration " + iteration);
        }
        oldMeans = new ArrayList<V>(means);
        means = means(clusters, means, database);
        clusters = sort(means, database);
        changed = !means.equals(oldMeans);
        iteration++;

        if(maxiter > 0 && iteration > maxiter) {
          break;
        }
      }
      Clustering<MeanModel<V>> result = new Clustering<MeanModel<V>>();
      for(int i = 0; i < clusters.size(); i++) {
        DatabaseObjectGroup group = new DatabaseObjectGroupCollection<List<Integer>>(clusters.get(i));
        MeanModel<V> model = new MeanModel<V>(means.get(i));
        result.addCluster(new Cluster<MeanModel<V>>(group, model));
      }
      return result;
    }
    else {
      return new Clustering<MeanModel<V>>();
    }
  }

  /**
   * Returns the mean vectors of the given clusters in the given database.
   * 
   * @param clusters the clusters to compute the means
   * @param means the recent means
   * @param database the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  protected List<V> means(List<List<Integer>> clusters, List<V> means, Database<V> database) {
    List<V> newMeans = new ArrayList<V>(k);
    for(int i = 0; i < k; i++) {
      List<Integer> list = clusters.get(i);
      V mean = null;
      for(Iterator<Integer> clusterIter = list.iterator(); clusterIter.hasNext();) {
        if(mean == null) {
          mean = database.get(clusterIter.next());
        }
        else {
          mean = mean.plus(database.get(clusterIter.next()));
        }
      }
      if(list.size() > 0) {
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
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   * 
   * @param means a list of k means
   * @param database the database to cluster
   * @return list of k clusters
   */
  protected List<List<Integer>> sort(List<V> means, Database<V> database) {
    List<List<Integer>> clusters = new ArrayList<List<Integer>>(k);
    for(int i = 0; i < k; i++) {
      clusters.add(new LinkedList<Integer>());
    }

    for(Integer id : database) {
      List<D> distances = new ArrayList<D>(k);
      V fv = database.get(id);
      int minIndex = 0;
      for(int d = 0; d < k; d++) {
        distances.add(getDistanceFunction().distance(fv, means.get(d)));
        if(distances.get(d).compareTo(distances.get(minIndex)) < 0) {
          minIndex = d;
        }
      }
      clusters.get(minIndex).add(id);
    }
    for(List<Integer> cluster : clusters) {
      Collections.sort(cluster);
    }
    return clusters;
  }
}