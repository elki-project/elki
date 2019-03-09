/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.algorithm.clustering.uncertain;

import java.util.*;

import elki.algorithm.AbstractAlgorithm;
import elki.algorithm.DistanceBasedAlgorithm;
import elki.algorithm.clustering.ClusteringAlgorithm;
import elki.algorithm.clustering.kmeans.KMedoidsPAM;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.model.Model;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.data.uncertain.UncertainObject;
import elki.database.Database;
import elki.database.ProxyDatabase;
import elki.database.datastore.DataStore;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.similarityfunction.cluster.ClusteringAdjustedRandIndexSimilarity;
import elki.distance.similarityfunction.cluster.ClusteringDistanceSimilarity;
import elki.index.distancematrix.PrecomputedDistanceMatrix;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.statistics.distribution.NormalDistribution;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.result.ResultUtil;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.ChainedParameterization;
import elki.utilities.optionhandling.parameterization.ListParameterization;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.*;
import elki.utilities.pairs.DoubleObjPair;
import elki.utilities.random.RandomFactory;

import net.jafama.FastMath;

/**
 * Representative clustering of uncertain data.
 * <p>
 * This algorithm clusters uncertain data by repeatedly sampling a possible
 * world, then running a traditional clustering algorithm on this sample.
 * <p>
 * The resulting "possible" clusterings are then clustered themselves, using a
 * clustering similarity measure. This yields a number of representatives for
 * the set of all possible worlds.
 * <p>
 * Reference:
 * <p>
 * Andreas Züfle, Tobias Emrich, Klaus Arthur Schmid, Nikos Mamoulis,
 * Arthur Zimek, Mathias Renz<br>
 * Representative clustering of uncertain data<br>
 * In Proc. 20th ACM SIGKDD International Conference on Knowledge Discovery and
 * Data Mining
 *
 * @author Alexander Koos
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - RepresentativenessEvaluation
 */
@Reference(authors = "Andreas Züfle, Tobias Emrich, Klaus Arthur Schmid, Nikos Mamoulis, Arthur Zimek, Mathias Renz", //
    title = "Representative clustering of uncertain data", //
    booktitle = "Proc. 20th ACM SIGKDD International Conference on Knowledge Discovery and Data Mining", //
    url = "https://doi.org/10.1145/2623330.2623725", //
    bibkey = "DBLP:conf/kdd/ZufleESMZR14")
public class RepresentativeUncertainClustering extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Initialize a Logger.
   */
  private static final Logging LOG = Logging.getLogger(RepresentativeUncertainClustering.class);

  /**
   * Distance function for clusterings.
   */
  protected ClusteringDistanceSimilarity distance;

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
   * Keep all samples (not only the representative results)
   */
  protected boolean keep;

  /**
   * Constructor, quite trivial.
   *
   * @param distance Distance function for meta clustering
   * @param metaAlgorithm Meta clustering algorithm
   * @param samplesAlgorithm Primary clustering algorithm
   * @param numsamples Number of samples
   * @param alpha Alpha confidence
   * @param keep Keep all samples (not only the representative results).
   */
  public RepresentativeUncertainClustering(ClusteringDistanceSimilarity distance, ClusteringAlgorithm<?> metaAlgorithm, ClusteringAlgorithm<?> samplesAlgorithm, int numsamples, RandomFactory random, double alpha, boolean keep) {
    this.samplesAlgorithm = samplesAlgorithm;
    this.numsamples = numsamples;
    this.metaAlgorithm = metaAlgorithm;
    this.distance = distance;
    this.random = random;
    this.alpha = alpha;
    this.keep = keep;
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
    // "Result" to group our samples
    Object samples = new Object();
    Metadata.of(samples).setLongName("Samples"); // For UI

    // Step 1: Cluster sampled possible worlds:
    Random rand = random.getSingleThreadedRandom();
    FiniteProgress sampleP = LOG.isVerbose() ? new FiniteProgress("Clustering samples", numsamples, LOG) : null;
    for(int i = 0; i < numsamples; i++) {
      WritableDataStore<DoubleVector> store = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, DoubleVector.class);
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        store.put(iter, relation.get(iter).drawSample(rand));
      }
      clusterings.add(runClusteringAlgorithm(samples, ids, store, dim, "Sample " + i));
      LOG.incrementProcessed(sampleP);
    }
    LOG.ensureCompleted(sampleP);

    // Step 2: perform the meta clustering (on samples only).
    DBIDRange rids = DBIDFactory.FACTORY.generateStaticDBIDRange(clusterings.size());
    WritableDataStore<Clustering<?>> datastore = DataStoreUtil.makeStorage(rids, DataStoreFactory.HINT_DB, Clustering.class);
    {
      Iterator<Clustering<?>> it2 = clusterings.iterator();
      for(DBIDIter iter = rids.iter(); iter.valid(); iter.advance()) {
        datastore.put(iter, it2.next());
      }
    }
    assert (rids.size() == clusterings.size());

    // Build a relation, and a distance matrix.
    Relation<Clustering<?>> crel = new MaterializedRelation<Clustering<?>>("Clusterings", Clustering.TYPE, rids, datastore);
    PrecomputedDistanceMatrix<Clustering<?>> mat = new PrecomputedDistanceMatrix<>(crel, rids, distance);
    mat.initialize();
    ProxyDatabase d = new ProxyDatabase(rids, crel);
    Metadata.hierarchyOf(crel).addChild(mat);
    Clustering<?> c = metaAlgorithm.run(d);
    Metadata.hierarchyOf(d).removeChild(c); // Detach from database

    // "Result" to group or representative results
    Object reps = new Object();
    Metadata.of(reps).setLongName("Representants");
    Metadata.hierarchyOf(relation).addChild(reps);

    DistanceQuery<Clustering<?>> dq = mat.getDistanceQuery(distance);
    List<? extends Cluster<?>> cl = c.getAllClusters();
    List<DoubleObjPair<Clustering<?>>> evaluated = new ArrayList<>(cl.size());
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
      final double cprob = computeConfidence(clus.size(), crel.size());

      // Build an evaluation result
      Metadata.hierarchyOf(bestc).addChild(new RepresentativenessEvaluation(gtau, besttau, cprob));

      evaluated.add(new DoubleObjPair<Clustering<?>>(cprob, bestc));
    }
    // Sort evaluated results by confidence:
    Collections.sort(evaluated, Collections.reverseOrder());
    for(DoubleObjPair<Clustering<?>> pair : evaluated) {
      // Attach parent relation (= sample) to the representative samples.
      for(It<Relation<?>> it = Metadata.hierarchyOf(pair.second).iterParents().filter(Relation.class); it.valid(); it.advance()) {
        Metadata.hierarchyOf(reps).addChild(it.get());
      }
    }
    // Add the random samples below the representative results only:
    if(keep) {
      Metadata.hierarchyOf(relation).addChild(samples);
    }
    else {
      ResultUtil.removeRecursive(samples);
    }
    return c;
  }

  /**
   * Estimate the confidence probability of a clustering.
   *
   * @param support Number of supporting samples
   * @param samples Total samples
   * @return Probability
   */
  private double computeConfidence(int support, int samples) {
    final double z = NormalDistribution.standardNormalQuantile(alpha);
    final double eprob = support / (double) samples;
    return Math.max(0., eprob - z * FastMath.sqrt((eprob * (1 - eprob)) / samples));
  }

  /**
   * Run a clustering algorithm on a single instance.
   * 
   * @param parent Parent result to attach to
   * @param ids Object IDs to process
   * @param store Input data
   * @param dim Dimensionality
   * @param title Title of relation
   *
   * @return Clustering result
   */
  protected Clustering<?> runClusteringAlgorithm(Object parent, DBIDs ids, DataStore<DoubleVector> store, int dim, String title) {
    SimpleTypeInformation<DoubleVector> t = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    Relation<DoubleVector> sample = new MaterializedRelation<>(title, t, ids, store);
    ProxyDatabase d = new ProxyDatabase(ids, sample);
    Clustering<?> clusterResult = samplesAlgorithm.run(d);
    ResultUtil.removeRecursive(sample);
    ResultUtil.removeRecursive(clusterResult);
    Metadata.hierarchyOf(parent).addChild(sample);
    Metadata.hierarchyOf(sample).addChild(clusterResult);
    return clusterResult;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(UncertainObject.UNCERTAIN_OBJECT_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return RepresentativeUncertainClustering.LOG;
  }

  /**
   * Representativeness evaluation result.
   *
   * @author Erich Schubert
   */
  public static class RepresentativenessEvaluation extends EvaluationResult {
    /**
     * Constructor.
     *
     * @param gtau Global tau
     * @param besttau Within cluster Tau
     * @param cprob Confidence probability
     */
    public RepresentativenessEvaluation(double gtau, double besttau, double cprob) {
      super();
      Metadata.of(this).setLongName("Possible-Worlds Evaluation");
      MeasurementGroup g = newGroup("Representativeness");
      g.addMeasure("Confidence", cprob, 0, 1, false);
      g.addMeasure("Global Tau", gtau, 0, 1, true);
      g.addMeasure("Cluster Tau", besttau, 0, 1, true);
    }

    @Override
    public boolean visualizeSingleton() {
      return true;
    }
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
     */
    public final static OptionID SAMPLES_ID = new OptionID("pwc.samples", "Number of clusterings to produce on samples.");

    /**
     * Flag to keep all samples.
     */
    public final static OptionID KEEP_SAMPLES_ID = new OptionID("pwc.samples.keep", "Retain all sampled relations, not only the representative results.");

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
    protected ClusteringDistanceSimilarity distance;

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

    /**
     * Keep all samples (not only the representative results).
     */
    protected boolean keep;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      distance = ClusteringAdjustedRandIndexSimilarity.STATIC;
      ObjectParameter<ClusteringDistanceSimilarity> simP = new ObjectParameter<>(CLUSTERDISTANCE_ID, ClusteringDistanceSimilarity.class, ClusteringAdjustedRandIndexSimilarity.class);
      if(config.grab(simP)) {
        distance = simP.instantiateClass(config);
      }
      // Configure Distance function
      ListParameterization predef = new ListParameterization();
      predef.addParameter(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, distance);
      ChainedParameterization chain = new ChainedParameterization(predef, config);
      chain.errorsTo(config);
      ObjectParameter<ClusteringAlgorithm<?>> malgorithm = new ObjectParameter<>(META_ALGORITHM_ID, ClusteringAlgorithm.class, KMedoidsPAM.class);
      if(chain.grab(malgorithm)) {
        metaAlgorithm = malgorithm.instantiateClass(chain);
        if(metaAlgorithm != null && metaAlgorithm.getInputTypeRestriction().length > 0 && //
            !metaAlgorithm.getInputTypeRestriction()[0].isAssignableFromType(Clustering.TYPE)) {
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
      Flag keepF = new Flag(KEEP_SAMPLES_ID);
      if(config.grab(keepF)) {
        keep = keepF.isTrue();
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
      return new RepresentativeUncertainClustering(distance, metaAlgorithm, samplesAlgorithm, numsamples, random, alpha, keep);
    }
  }
}
