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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMedoidsEM;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.cluster.ClusteringAdjustedRandIndexSimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.cluster.ClusteringDistanceSimilarityFunction;
import de.lmu.ifi.dbs.elki.index.distancematrix.PrecomputedDistanceMatrix;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.result.EvaluationResult;
import de.lmu.ifi.dbs.elki.result.EvaluationResult.MeasurementGroup;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Representative clustering of uncertain data.
 *
 * This algorithm clusters uncertain data by repeatedly sampling a possible
 * world, then running a traditional clustering algorithm on this sample.
 *
 * The resulting "possible" clusterings are then clustered themselves, using a
 * clustering similarity measure. This yields a number of representatives for
 * the set of all possible worlds.
 *
 * Reference:
 * <p>
 * Andreas Züfle, Tobias Emrich, Klaus Arthur Schmid, Nikos Mamoulis, Arthur
 * Zimek, Mathias Renz<br />
 * Representative clustering of uncertain data<br />
 * In Proc. 20th ACM SIGKDD International Conference on Knowledge Discovery and
 * Data Mining
 * </p>
 *
 * @author Alexander Koos
 * @author Erich Schubert
 */
@Reference(authors = "Andreas Züfle, Tobias Emrich, Klaus Arthur Schmid, Nikos Mamoulis, Arthur Zimek, Mathias Renz", //
title = "Representative clustering of uncertain data", //
booktitle = "Proc. 20th ACM SIGKDD International Conference on Knowledge Discovery and Data Mining", //
url = "http://dx.doi.org/10.1145/2623330.2623725")
public class RepresentativeUncertainClustering extends AbstractAlgorithm<Clustering<Model>> {
  /**
   * Initialize a Logger.
   */
  private static final Logging LOG = Logging.getLogger(RepresentativeUncertainClustering.class);

  /**
   * Distance function for clusterings.
   */
  protected ClusteringDistanceSimilarityFunction distance;

  /**
   * The algorithm for meta-clustering.
   */
  protected ClusteringAlgorithm<?> metaAlgorithm;

  /**
   * The algorithm to be wrapped and run.
   */
  protected ClusteringAlgorithm<?> samplesAlgorithm;

  /**
   * How many clusterings shall be made for aggregation.
   */
  protected int numsamples;

  /**
   * Random factory for sampling.
   */
  protected RandomFactory random;

  /**
   * Alpha parameter for confidence.
   */
  protected double alpha;

  /**
   * Constructor, quite trivial.
   *
   * @param distance Distance function for meta clustering
   * @param metaAlgorithm Meta clustering algorithm
   * @param samplesAlgorithm Primary clustering algorithm
   * @param numsamples Number of samples
   * @param alpha Alpha confidence
   */
  public RepresentativeUncertainClustering(ClusteringDistanceSimilarityFunction distance, ClusteringAlgorithm<?> metaAlgorithm, ClusteringAlgorithm<?> samplesAlgorithm, int numsamples, RandomFactory random, double alpha) {
    this.samplesAlgorithm = samplesAlgorithm;
    this.numsamples = numsamples;
    this.metaAlgorithm = metaAlgorithm;
    this.distance = distance;
    this.random = random;
    this.alpha = alpha;
  }

  /**
   * This run method will do the wrapping.
   *
   * Its called from {@link AbstractAlgorithm#run(Database)} and performs the
   * call to the algorithms particular run method as well as the storing and
   * comparison of the resulting Clusterings.
   *
   * @param database Database
   * @param relation Data relation of uncertain objects
   * @return Clustering result
   */
  public Clustering<?> run(Database database, Relation<? extends UncertainObject> relation) {
    ArrayList<Clustering<?>> clusterings = new ArrayList<>();
    final int dim = RelationUtil.dimensionality(relation);
    DBIDs ids = relation.getDBIDs();
    // Add the center of mass result for comparison:
    {
      WritableDataStore<DoubleVector> store1 = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, DoubleVector.class);
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        store1.put(iter, relation.get(iter).getCenterOfMass());
      }
      // Not added to "clusterings", so it does not get aggregated.
      runClusteringAlgorithm(relation, ids, store1, dim, "Uncertain Model: Center of Mass");
    }
    // Step 1: Cluster sampled possible worlds:
    Random rand = random.getSingleThreadedRandom();
    for(int i = 0; i < numsamples; i++) {
      WritableDataStore<DoubleVector> store = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, DoubleVector.class);
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        store.put(iter, relation.get(iter).drawSample(rand));
      }
      clusterings.add(runClusteringAlgorithm(relation, ids, store, dim, "Sample " + i));
    }

    // Step 2: perform the meta clustering (on samples only).
    DBIDRange rids = DBIDFactory.FACTORY.generateStaticDBIDRange(clusterings.size());
    WritableDataStore<Clustering<?>> datastore = DataStoreUtil.makeStorage(rids, DataStoreFactory.HINT_DB, Clustering.class);
    {
      Iterator<Clustering<?>> it2 = clusterings.iterator();
      for(DBIDIter iter = rids.iter(); iter.valid(); iter.advance()) {
        datastore.put(iter, it2.next());
      }
    }
    assert(rids.size() == clusterings.size());

    // Build a relation, and a distance matrix.
    Relation<Clustering<?>> crel = new MaterializedRelation<Clustering<?>>(TypeUtil.CLUSTERING, rids, "Clusterings", datastore);
    PrecomputedDistanceMatrix<Clustering<?>> mat = new PrecomputedDistanceMatrix<>(crel, distance);
    mat.initialize();
    ProxyDatabase d = new ProxyDatabase(rids, crel);
    d.getHierarchy().add(crel, mat);
    Clustering<?> c = metaAlgorithm.run(d);
    d.getHierarchy().remove(d, c); // Detach from database

    // Evaluation
    DistanceQuery<Clustering<?>> dq = mat.getDistanceQuery(distance);
    List<? extends Cluster<?>> cl = c.getAllClusters();
    for(Cluster<?> clus : cl) {
      double besttau = Double.POSITIVE_INFINITY;
      Clustering<?> bestc = null;
      for(DBIDIter it1 = clus.getIDs().iter(); it1.valid(); it1.advance()) {
        double tau = 0.;
        Clustering<?> curc = crel.get(it1);
        for(DBIDIter it2 = clus.getIDs().iter(); it2.valid(); it2.advance()) {
          if(DBIDUtil.equal(it1, it2)) {
            continue;
          }
          double di = dq.distance(curc, it2);
          tau = di > tau ? di : tau;
        }
        // Cluster member with the least maximum distance.
        if(tau < besttau) {
          besttau = tau;
          bestc = curc;
        }
      }
      if(bestc == null) { // E.g. degenerate empty clusters
        continue;
      }
      // Global tau:
      double gtau = 0.;
      for(DBIDIter it2 = crel.iterDBIDs(); it2.valid(); it2.advance()) {
        double di = dq.distance(bestc, it2);
        gtau = di > gtau ? di : gtau;
      }
      EvaluationResult psr = evaluate(bestc, gtau, besttau, clus.size(), crel.size());
      database.getHierarchy().add(bestc, psr);
    }
    return c;
  }

  /**
   * Produce the evaluation result for a particular clustering.
   *
   * @param c Clustering
   * @param gtau Global tau
   * @param tau Cluster tau
   * @param support Cluster size
   * @param samples Total number of samples
   * @return Evaluation result
   */
  protected EvaluationResult evaluate(Clustering<?> c, double gtau, double tau, int support, int samples) {
    EvaluationResult res = new EvaluationResult("Possible-Worlds Evaluation", "representativeness");

    final double z = NormalDistribution.standardNormalQuantile(alpha);
    final double eprob = support / (double) samples;
    final double cprob = Math.max(0., eprob - z * Math.sqrt((eprob * (1 - eprob)) / samples));

    MeasurementGroup g = res.newGroup("Representativeness");
    g.addMeasure("Global Tau", gtau, 0, 1, true);
    g.addMeasure("Cluster Tau", tau, 0, 1, true);
    g.addMeasure("Confidence", cprob, 0, 1, false);
    return res;
  }

  /**
   * Run a clustering algorithm on a single instance.
   *
   * @param database Database
   * @param ids Object IDs to process
   * @param store Input data
   * @param dim Dimensionality
   * @param title Title of relation
   * @return Clustering result
   */
  protected Clustering<?> runClusteringAlgorithm(HierarchicalResult database, DBIDs ids, DataStore<DoubleVector> store, int dim, String title) {
    SimpleTypeInformation<DoubleVector> t = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    Relation<DoubleVector> sample = new MaterializedRelation<>(t, ids, title, store);
    ProxyDatabase d = new ProxyDatabase(ids, sample);
    Clustering<?> clusterResult = samplesAlgorithm.run(d);
    d.getHierarchy().remove(sample);
    d.getHierarchy().remove(clusterResult);
    database.getHierarchy().add(database, sample);
    database.getHierarchy().add(sample, clusterResult);
    return clusterResult;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.UNCERTAIN_OBJECT_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return RepresentativeUncertainClustering.LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Alexander Koos
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Default number of clusterings to run.
     */
    public final static int DEFAULT_ENSEMBLE_DEPTH = 10;

    /**
     * Distance function to measure the similarity of clusterings.
     */
    public final static OptionID CLUSTERDISTANCE_ID = new OptionID("pwc.distance", "Distance measure of clusterings.");

    /**
     * Parameter to hand an algorithm for creating the meta-clustering to our
     * instance of {@link RepresentativeUncertainClustering}.
     *
     * It has to use a metric distance function to work on the
     * sample-clusterings.
     */
    public final static OptionID META_ALGORITHM_ID = new OptionID("pwc.metaclustering", "Algorithm used to aggregate clustering results. Must be a distance-based clustering algorithm.");

    /**
     * Parameter to hand an algorithm to be wrapped and run to our instance of
     * {@link RepresentativeUncertainClustering}.
     */
    public final static OptionID ALGORITHM_ID = new OptionID("pwc.clustering", "Clustering algorithm used on the samples.");

    /**
     * Parameter to specify the amount of clusterings that shall be created and
     * compared.
     *
     * Has a default value.
     */
    public final static OptionID SAMPLES_ID = new OptionID("pwc.samples", "Number of clusterings to produce on samples.");

    /**
     * Parameter to specify the random generator.
     */
    public final static OptionID RANDOM_ID = new OptionID("pwc.random", "Random generator used for sampling.");

    /**
     * Alpha parameter for confidence estimation.
     */
    public static final OptionID ALPHA_ID = new OptionID("pwc.alpha", "Alpha threshold for estimating the confidence probability.");

    /**
     * Distance (dissimilarity) for clusterinogs.
     */
    protected ClusteringDistanceSimilarityFunction distance;

    /**
     * Field to store the inner algorithm for meta-clustering
     */
    protected ClusteringAlgorithm<?> metaAlgorithm;

    /**
     * Field to store the algorithm.
     */
    protected ClusteringAlgorithm<?> samplesAlgorithm;

    /**
     * Field to store parameter the number of samples.
     */
    protected int numsamples;

    /**
     * Random factory for sampling.
     */
    protected RandomFactory random;

    /**
     * Alpha parameter for confidence.
     */
    protected double alpha;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      distance = ClusteringAdjustedRandIndexSimilarityFunction.STATIC;
      ObjectParameter<ClusteringDistanceSimilarityFunction> simP = new ObjectParameter<>(CLUSTERDISTANCE_ID, ClusteringDistanceSimilarityFunction.class, ClusteringAdjustedRandIndexSimilarityFunction.class);
      if(config.grab(simP)) {
        distance = simP.instantiateClass(config);
      }
      // Configure Distance function
      ListParameterization predef = new ListParameterization();
      predef.addParameter(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, distance);
      ChainedParameterization chain = new ChainedParameterization(predef, config);
      ObjectParameter<ClusteringAlgorithm<?>> malgorithm = new ObjectParameter<>(META_ALGORITHM_ID, ClusteringAlgorithm.class, KMedoidsEM.class);
      if(chain.grab(malgorithm)) {
        metaAlgorithm = malgorithm.instantiateClass(chain);
        if(metaAlgorithm != null && metaAlgorithm.getInputTypeRestriction().length > 0 && //
        !metaAlgorithm.getInputTypeRestriction()[0].isAssignableFromType(TypeUtil.CLUSTERING)) {
          config.reportError(new WrongParameterValueException(malgorithm, malgorithm.getValueAsString(), "The meta clustering algorithm (as configured) does not accept clustering results."));
        }
      }
      ObjectParameter<ClusteringAlgorithm<?>> palgorithm = new ObjectParameter<>(ALGORITHM_ID, ClusteringAlgorithm.class);
      if(config.grab(palgorithm)) {
        samplesAlgorithm = palgorithm.instantiateClass(config);
        if(samplesAlgorithm != null && samplesAlgorithm.getInputTypeRestriction().length > 0 && //
        !samplesAlgorithm.getInputTypeRestriction()[0].isAssignableFromType(TypeUtil.NUMBER_VECTOR_FIELD)) {
          config.reportError(new WrongParameterValueException(palgorithm, palgorithm.getValueAsString(), "The inner clustering algorithm (as configured) does not accept numerical vectors: " + samplesAlgorithm.getInputTypeRestriction()[0]));
        }
      }
      IntParameter pdepth = new IntParameter(SAMPLES_ID, DEFAULT_ENSEMBLE_DEPTH);
      if(config.grab(pdepth)) {
        numsamples = pdepth.getValue();
      }
      RandomParameter randomP = new RandomParameter(RANDOM_ID);
      if(config.grab(randomP)) {
        random = randomP.getValue();
      }
      DoubleParameter palpha = new DoubleParameter(ALPHA_ID, 0.95) //
      .addConstraint(CommonConstraints.GREATER_THAN_ONE_DOUBLE) //
      .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
      if(config.grab(palpha)) {
        alpha = palpha.doubleValue();
      }
    }

    @Override
    protected RepresentativeUncertainClustering makeInstance() {
      return new RepresentativeUncertainClustering(distance, metaAlgorithm, samplesAlgorithm, numsamples, random, alpha);
    }
  }
}
