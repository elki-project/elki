package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.DimensionModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.ProxyView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.FilteredLocalPCABasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.IndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ProxyDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.preprocessed.LocalProjectionIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.LocalProjectionIndex.Factory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides the COPAC algorithm, an algorithm to partition a database according
 * to the correlation dimension of its objects and to then perform an arbitrary
 * clustering algorithm over the partitions.
 * <p>
 * Reference: Achtert E., Böhm C., Kriegel H.-P., Kröger P., Zimek A.: Robust,
 * Complete, and Efficient Correlation Clustering. <br>
 * In Proc. 7th SIAM International Conference on Data Mining (SDM'07),
 * Minneapolis, MN, 2007
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses LocalProjectionIndex
 * @apiviz.uses FilteredLocalPCABasedDistanceFunction
 * @apiviz.has DimensionModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("COPAC: COrrelation PArtition Clustering")
@Description("Partitions a database according to the correlation dimension of its objects and performs " + "a clustering algorithm over the partitions.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger P., A. Zimek", title = "Robust, Complete, and Efficient Correlation Clustering", booktitle = "Proc. 7th SIAM International Conference on Data Mining (SDM'07), Minneapolis, MN, 2007", url = "http://www.siam.org/proceedings/datamining/2007/dm07_037achtert.pdf")
public class COPAC<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractAlgorithm<V, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(COPAC.class);

  /**
   * Parameter to specify the local PCA preprocessor to derive partition
   * criterion, must extend
   * {@link de.lmu.ifi.dbs.elki.index.preprocessed.localpca.AbstractFilteredPCAIndex}.
   * <p>
   * Key: {@code -copac.preprocessor}
   * </p>
   */
  public static final OptionID PREPROCESSOR_ID = OptionID.getOrCreateOptionID("copac.preprocessor", "Local PCA Preprocessor to derive partition criterion.");

  /**
   * Parameter to specify the distance function to use inside the partitions
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractIndexBasedDistanceFunction}
   * .
   * <p>
   * Default value:
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction}
   * </p>
   * <p>
   * Key: {@code -copac.partitionDistance}
   * </p>
   */
  public static final OptionID PARTITION_DISTANCE_ID = OptionID.getOrCreateOptionID("copac.partitionDistance", "Distance to use for the inner algorithms.");

  /**
   * Parameter to specify the clustering algorithm to apply to each partition,
   * must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm}.
   * <p>
   * Key: {@code -copac.partitionAlgorithm}
   * </p>
   */
  public static final OptionID PARTITION_ALGORITHM_ID = OptionID.getOrCreateOptionID("copac.partitionAlgorithm", "Clustering algorithm to apply to each partition.");

  /**
   * Holds the instance of the preprocessed distance function
   * {@link #PARTITION_DISTANCE_ID}.
   */
  private FilteredLocalPCABasedDistanceFunction<V, ?, D> partitionDistanceFunction;

  /**
   * Get the algorithm to run on each partition.
   */
  private Class<? extends ClusteringAlgorithm<Clustering<Model>>> partitionAlgorithm;

  /**
   * Holds the parameters of the algorithm to run on each partition.
   */
  private Collection<Pair<OptionID, Object>> partitionAlgorithmParameters;

  /**
   * The last used distance query
   */
  // FIXME: remove this when migrating to a full Factory pattern! This is
  // non-reentrant!
  private FilteredLocalPCABasedDistanceFunction.Instance<V, LocalProjectionIndex<V, ?>, D> partitionDistanceQuery;

  /**
   * Constructor.
   * 
   * @param partitionDistanceFunction Distance function
   * @param partitionAlgorithm Algorithm to use on partitions
   * @param partitionAlgorithmParameters Parameters for Algorithm to run on
   *        partitions
   */
  public COPAC(FilteredLocalPCABasedDistanceFunction<V, ?, D> partitionDistanceFunction, Class<? extends ClusteringAlgorithm<Clustering<Model>>> partitionAlgorithm, Collection<Pair<OptionID, Object>> partitionAlgorithmParameters) {
    super();
    this.partitionDistanceFunction = partitionDistanceFunction;
    this.partitionAlgorithm = partitionAlgorithm;
    this.partitionAlgorithmParameters = partitionAlgorithmParameters;
  }

  /**
   * Performs the COPAC algorithm on the given database.
   */
  @SuppressWarnings("unchecked")
  @Override
  protected Clustering<Model> runInTime(Database database) throws IllegalStateException {
    Relation<V> dataQuery = getRelation(database);
    if(logger.isVerbose()) {
      logger.verbose("Running COPAC on db size = " + dataQuery.size() + " with dimensionality = " + DatabaseUtil.dimensionality(dataQuery));
    }

    partitionDistanceQuery = (FilteredLocalPCABasedDistanceFunction.Instance<V, LocalProjectionIndex<V, ?>, D>) partitionDistanceFunction.instantiate(dataQuery);
    LocalProjectionIndex<V, ?> preprocin = partitionDistanceQuery.getIndex();

    // partitioning
    Map<Integer, ModifiableDBIDs> partitionMap = new HashMap<Integer, ModifiableDBIDs>();
    FiniteProgress partitionProgress = logger.isVerbose() ? new FiniteProgress("Partitioning", dataQuery.size(), logger) : null;
    int processed = 1;

    for(DBID id : dataQuery.iterDBIDs()) {
      Integer corrdim = preprocin.getLocalProjection(id).getCorrelationDimension();

      if(!partitionMap.containsKey(corrdim)) {
        partitionMap.put(corrdim, DBIDUtil.newArray());
      }

      partitionMap.get(corrdim).add(id);
      if(partitionProgress != null) {
        partitionProgress.setProcessed(processed++, logger);
      }
    }

    if(partitionProgress != null) {
      partitionProgress.ensureCompleted(logger);
    }
    if(logger.isVerbose()) {
      for(Integer corrDim : partitionMap.keySet()) {
        ModifiableDBIDs list = partitionMap.get(corrDim);
        logger.verbose("Partition [corrDim = " + corrDim + "]: " + list.size() + " objects.");
      }
    }

    // convert for partition algorithm.
    // TODO: do this with DynamicDBIDs instead
    Map<Integer, DBIDs> pmap = new HashMap<Integer, DBIDs>();
    for(Entry<Integer, ModifiableDBIDs> ent : partitionMap.entrySet()) {
      pmap.put(ent.getKey(), ent.getValue());
    }
    // running partition algorithm
    return runPartitionAlgorithm(dataQuery, pmap, partitionDistanceQuery);
  }

  /**
   * Runs the partition algorithm and creates the result.
   * 
   * @param database the database to run this algorithm on
   * @param partitionMap the map of partition IDs to object ids
   * @param query The preprocessor based query function
   */
  private Clustering<Model> runPartitionAlgorithm(Relation<V> database, Map<Integer, DBIDs> partitionMap, DistanceQuery<V, D> query) {
    Clustering<Model> result = new Clustering<Model>("COPAC clustering", "copac-clustering");

    // TODO: use an extra finite progress for the partitions?
    for(Entry<Integer, DBIDs> pair : partitionMap.entrySet()) {
      // noise partition
      if(pair.getKey() == DatabaseUtil.dimensionality(database)) {
        // Make a Noise cluster
        result.addCluster(new Cluster<Model>(pair.getValue(), true, ClusterModel.CLUSTER));
      }
      else {
        DBIDs partids = pair.getValue();
        ProxyDatabase proxy = new ProxyDatabase(partids);
        Relation<V> partition = ProxyView.wrap(proxy, partids, database);
        proxy.addRelation(partition);
        
        ClusteringAlgorithm<Clustering<Model>> partitionAlgorithm = getPartitionAlgorithm(query);
        if(logger.isVerbose()) {
          logger.verbose("Running " + partitionAlgorithm.getClass().getName() + " on partition [corrDim = " + pair.getKey() + "]...");
        }
        Clustering<Model> p = partitionAlgorithm.run(proxy);
        // Re-Wrap resulting Clusters as DimensionModel clusters.
        for(Cluster<Model> clus : p.getAllClusters()) {
          if(clus.isNoise()) {
            result.addCluster(new Cluster<Model>(clus.getIDs(), true, ClusterModel.CLUSTER));
          }
          else {
            result.addCluster(new Cluster<Model>(clus.getIDs(), new DimensionModel(pair.getKey())));
          }
        }
      }
    }
    return result;
  }

  /**
   * Returns the partition algorithm.
   * 
   * @return the specified partition algorithm
   */
  public ClusteringAlgorithm<Clustering<Model>> getPartitionAlgorithm(DistanceQuery<V, D> query) {
    ListParameterization reconfig = new ListParameterization(partitionAlgorithmParameters);
    ProxyDistanceFunction<V, D> dist = ProxyDistanceFunction.proxy(query);
    reconfig.addParameter(AbstractDistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, dist);
    ClusteringAlgorithm<Clustering<Model>> instance = reconfig.tryInstantiate(partitionAlgorithm);
    reconfig.failOnErrors();
    return instance;
  }

  /**
   * Get the last used distance query (to expose access to the preprocessor)
   * 
   * Used by ERiC. TODO: migrate to factory pattern!
   * 
   * @return distance query
   */
  public FilteredLocalPCABasedDistanceFunction.Instance<V, LocalProjectionIndex<V, ?>, D> getPartitionDistanceQuery() {
    return partitionDistanceQuery;
  }

  @Override
  public VectorFieldTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractParameterizer {
    protected LocalProjectionIndex.Factory<V, ?> indexI = null;

    protected FilteredLocalPCABasedDistanceFunction<V, ?, D> pdistI = null;

    protected Class<? extends ClusteringAlgorithm<Clustering<Model>>> algC = null;

    protected Collection<Pair<OptionID, Object>> algO = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ClassParameter<Factory<V, ?>> indexP = new ClassParameter<LocalProjectionIndex.Factory<V, ?>>(PREPROCESSOR_ID, LocalProjectionIndex.Factory.class);
      if(config.grab(indexP)) {
        indexI = indexP.instantiateClass(config);
      }

      ObjectParameter<FilteredLocalPCABasedDistanceFunction<V, ?, D>> pdistP = new ObjectParameter<FilteredLocalPCABasedDistanceFunction<V, ?, D>>(PARTITION_DISTANCE_ID, FilteredLocalPCABasedDistanceFunction.class, LocallyWeightedDistanceFunction.class);
      if(config.grab(pdistP)) {
        ListParameterization predefinedDist = new ListParameterization();
        predefinedDist.addParameter(IndexBasedDistanceFunction.INDEX_ID, indexI);
        ChainedParameterization chainDist = new ChainedParameterization(predefinedDist, config);
        chainDist.errorsTo(config);
        pdistI = pdistP.instantiateClass(chainDist);
        predefinedDist.reportInternalParameterizationErrors(config);
      }

      // Parameterize algorithm:
      ClassParameter<ClusteringAlgorithm<Clustering<Model>>> algP = new ClassParameter<ClusteringAlgorithm<Clustering<Model>>>(PARTITION_ALGORITHM_ID, ClusteringAlgorithm.class);
      if(config.grab(algP)) {
        ListParameterization predefined = new ListParameterization();
        predefined.addParameter(AbstractDistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, pdistI);
        TrackParameters trackpar = new TrackParameters(config);
        ChainedParameterization chain = new ChainedParameterization(predefined, trackpar);
        chain.errorsTo(config);
        algP.instantiateClass(chain);
        algC = algP.getValue();
        algO = trackpar.getGivenParameters();
        predefined.reportInternalParameterizationErrors(chain);
      }
    }

    @Override
    protected COPAC<V, D> makeInstance() {
      return new COPAC<V, D>(pdistI, algC, algO);
    }
  }
}