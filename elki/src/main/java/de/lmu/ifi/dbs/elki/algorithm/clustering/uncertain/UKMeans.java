package de.lmu.ifi.dbs.elki.algorithm.clustering.uncertain;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.RandomlyChosenInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.uncertain.DiscreteUncertainObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.StringStatistic;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Uncertain K-Means clustering, using the average deviation from the center.
 *
 * Note: this method is, essentially, useless. It was shown to be equivalent to
 * doing regular K-means on the object centroids instead (see {@link CKMeans}
 * for the reference and an implemenation). This is only for completeness.
 *
 * Reference:
 * <p>
 * M. Chau, R. Cheng, B. Kao, J. Ng<br />
 * Uncertain data mining: An example in clustering location data<br />
 * Proc. of the 10th Pacific-Asia Conference on Knowledge Discovery and Data
 * Mining (PAKDD 2006)
 * </p>
 */
@Reference(authors = "M. Chau, R. Cheng, B. Kao, J. Ng", //
title = "Uncertain data mining: An example in clustering location data", //
booktitle = "Proc. 10th Pacific-Asia Conference on Knowledge Discovery and Data Mining (PAKDD 2006)", //
url = "http://dx.doi.org/10.1007/11731139_24")
public class UKMeans extends AbstractAlgorithm<Clustering<KMeansModel>>implements ClusteringAlgorithm<Clustering<KMeansModel>> {
  /**
   * CLass logger.
   */
  protected static final Logging LOG = Logging.getLogger(UKMeans.class);

  /**
   * Key for statistics logging.
   */
  protected static final String KEY = UKMeans.class.getName();

  /**
   * Number of cluster centers to initialize.
   */
  protected int k;

  /**
   * Maximum number of iterations
   */
  protected int maxiter;

  /**
   * Method to choose initial means.
   */
  protected KMeansInitialization<? super NumberVector> initializer;

  /**
   * Our Random factory
   */
  protected RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param k
   * @param maxiter
   * @param initializer
   */
  public UKMeans(int k, int maxiter, KMeansInitialization<? super NumberVector> initializer, RandomFactory rnd) {
    this.k = k;
    this.maxiter = maxiter;
    this.initializer = initializer;
    this.rnd = rnd;
  }

  /**
   * Run the clustering.
   *
   * @param database the Database
   * @param relation the Relation
   * @return
   */
  public Clustering<?> run(final Database database, final Relation<DiscreteUncertainObject> relation) {
    if(relation.size() <= 0) {
      return new Clustering<>("Uk-Means Clustering", "ukmeans-clustering");
    }
    // Choose initial means
    if(LOG.isStatistics()) {
      LOG.statistics(new StringStatistic(KEY + ".initialization", initializer.toString()));
    }

    DBIDs sampleids = DBIDUtil.randomSample(relation.getDBIDs(), k, rnd);
    List<Vector> means = new ArrayList<>(k);
    for(DBIDIter iter = sampleids.iter(); iter.valid(); iter.advance()) {
      means.add(new Vector(ArrayLikeUtil.toPrimitiveDoubleArray(relation.get(iter).getCenterOfMass())));
    }

    // Setup cluster assignment store
    List<ModifiableDBIDs> clusters = new ArrayList<>();
    for(int i = 0; i < k; i++) {
      clusters.add(DBIDUtil.newHashSet((int) (relation.size() * 2. / k)));
    }
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    double[] varsum = new double[k];

    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("UK-Means iteration", LOG) : null;
    DoubleStatistic varstat = LOG.isStatistics() ? new DoubleStatistic(this.getClass().getName() + ".variance-sum") : null;
    int iteration = 0;
    for(; maxiter <= 0 || iteration < maxiter; iteration++) {
      LOG.incrementProcessed(prog);
      boolean changed = assignToNearestCluster(relation, means, clusters, assignment, varsum);
      logVarstat(varstat, varsum);
      // Stop if no cluster assignment changed.
      if(!changed) {
        break;
      }
      // Recompute means.
      means = means(clusters, means, relation);
    }
    LOG.setCompleted(prog);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
    }

    // Wrap result
    Clustering<KMeansModel> result = new Clustering<>("Uk-Means Clustering", "ukmeans-clustering");
    for(int i = 0; i < clusters.size(); i++) {
      DBIDs ids = clusters.get(i);
      if(ids.size() == 0) {
        continue;
      }
      KMeansModel model = new KMeansModel(means.get(i), varsum[i]);
      result.addToplevelCluster(new Cluster<>(ids, model));
    }
    return result;
  }

  /**
   * Get expected distance between a Vector and an uncertain object
   *
   * @param rep A vector, e.g. a cluster representative
   * @param uo A discrete uncertain object
   * @return The distance
   */
  protected double getExpectedRepDistance(Vector rep, DiscreteUncertainObject uo) {
    int counter = 0;
    double avgDist = 0.0;

    EuclideanDistanceFunction euclidean = EuclideanDistanceFunction.STATIC;

    for(int i = 0; i < uo.getNumberSamples(); i++) {
      avgDist += euclidean.distance(rep, uo.getSample(i));
      counter++;
    }

    return avgDist / counter;
  }

  /**
   * Returns a list of clusters. The k<sup>th</sup> cluster contains the ids of
   * those FeatureVectors, that are nearest to the k<sup>th</sup> mean.
   *
   * @param relation the database to cluster
   * @param means a list of k means
   * @param clusters cluster assignment
   * @param assignment Current cluster assignment
   * @param varsum Variance sum output
   * @return true when the object was reassigned
   */
  protected boolean assignToNearestCluster(Relation<DiscreteUncertainObject> relation, List<Vector> means, List<? extends ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, double[] varsum) {
    assert(k == means.size());
    boolean changed = false;
    Arrays.fill(varsum, 0.);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double mindist = Double.POSITIVE_INFINITY;
      DiscreteUncertainObject fv = relation.get(iditer);
      int minIndex = 0;
      for(int i = 0; i < k; i++) {
        double dist = getExpectedRepDistance(means.get(i), fv);
        if(dist < mindist) {
          minIndex = i;
          mindist = dist;
        }
      }
      varsum[minIndex] += mindist;
      changed |= updateAssignment(iditer, clusters, assignment, minIndex);
    }
    return changed;
  }

  protected boolean updateAssignment(DBIDIter iditer, List<? extends ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, int newA) {
    final int oldA = assignment.intValue(iditer);
    if(oldA == newA) {
      return false;
    }
    clusters.get(newA).add(iditer);
    assignment.putInt(iditer, newA);
    if(oldA >= 0) {
      clusters.get(oldA).remove(iditer);
    }
    return true;
  }

  /**
   * Returns the mean vectors of the given clusters in the given database.
   *
   * @param clusters the clusters to compute the means
   * @param means the recent means
   * @param database the database containing the vectors
   * @return the mean vectors of the given clusters in the given database
   */
  protected List<Vector> means(List<? extends ModifiableDBIDs> clusters, List<? extends NumberVector> means, Relation<DiscreteUncertainObject> database) {
    List<Vector> newMeans = new ArrayList<>(k);
    for(int i = 0; i < k; i++) {
      ModifiableDBIDs list = clusters.get(i);
      Vector mean = null;
      if(list.size() > 0) {
        DBIDIter iter = list.iter();
        // Initialize with first.
        mean = new Vector(ArrayLikeUtil.toPrimitiveDoubleArray(database.get(iter).getCenterOfMass()));
        double[] raw = mean.getArrayRef();
        iter.advance();
        // Update with remaining instances
        for(; iter.valid(); iter.advance()) {
          Vector vec = new Vector(ArrayLikeUtil.toPrimitiveDoubleArray(database.get(iter).getCenterOfMass()));
          for(int j = 0; j < mean.getDimensionality(); j++) {
            raw[j] += vec.doubleValue(j);
          }
        }
        mean.timesEquals(1.0 / list.size());
      }
      else {
        // Keep degenerated means as-is for now.
        mean = means.get(i).getColumnVector();
      }
      newMeans.add(mean);
    }
    return newMeans;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.UNCERTAIN_OBJECT_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return UKMeans.LOG;
  }

  /**
   * Log statistics on the variance sum.
   *
   * @param varstat Statistics log instance
   * @param varsum Variance sum per cluster
   */
  protected void logVarstat(DoubleStatistic varstat, double[] varsum) {
    if(varstat == null) {
      return;
    }
    double s = 0.;
    for(double v : varsum) {
      s += v;
    }
    varstat.setDouble(s);
    getLogger().statistics(varstat);
  }

  public static class Parameterizer extends AbstractParameterizer {
    protected KMeansInitialization<? super NumberVector> initializer;

    protected int k;

    protected int maxiter;

    protected RandomFactory rnd;

    public static final OptionID INIT_ID = new OptionID("ukmeans.initialization", "Method to choose the initial means.");

    public final static OptionID K_ID = new OptionID("ukmeans.k", "The number of clusters to find.");

    public final static OptionID MAXITER_ID = new OptionID("ukmeans.maxiter", "The maximum number of iterations to do. 0 means no limit.");

    public final static OptionID RANDOM_ID = new OptionID("ukmeans.rnd", "The Random Factory");

    @Override
    public void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<KMeansInitialization<? super NumberVector>> initialP = new ObjectParameter<>(INIT_ID, KMeansInitialization.class, RandomlyChosenInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }
      IntParameter maxiterP = new IntParameter(MAXITER_ID, 0);
      maxiterP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(maxiterP)) {
        maxiter = maxiterP.getValue();
      }
      IntParameter kP = new IntParameter(K_ID);
      kP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        k = kP.getValue();
      }

      RandomParameter rndP = new RandomParameter(RANDOM_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected UKMeans makeInstance() {
      return new UKMeans(k, maxiter, initializer, rnd);
    }

  }
}
