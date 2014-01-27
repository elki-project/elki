package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.DimensionModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ProxyDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.FilteredLocalPCABasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.IndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ProxyDistanceFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.LocalProjectionIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.LocalProjectionIndex.Factory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
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
 * Reference: Achtert E., Böhm C., Kriegel H.-P., Kröger P., Zimek A.:<br />
 * Robust, Complete, and Efficient Correlation Clustering. <br />
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
public class COPAC<V extends NumberVector> extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(COPAC.class);

  /**
   * Parameter to specify the local PCA preprocessor to derive partition
   * criterion, must extend
   * {@link de.lmu.ifi.dbs.elki.index.preprocessed.localpca.AbstractFilteredPCAIndex}
   * .
   * <p>
   * Key: {@code -copac.preprocessor}
   * </p>
   */
  public static final OptionID PREPROCESSOR_ID = new OptionID("copac.preprocessor", "Local PCA Preprocessor to derive partition criterion.");

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
  public static final OptionID PARTITION_DISTANCE_ID = new OptionID("copac.partitionDistance", "Distance to use for the inner algorithms.");

  /**
   * Parameter to specify the clustering algorithm to apply to each partition,
   * must extend
   * {@link de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm}.
   * <p>
   * Key: {@code -copac.partitionAlgorithm}
   * </p>
   */
  public static final OptionID PARTITION_ALGORITHM_ID = new OptionID("copac.partitionAlgorithm", "Clustering algorithm to apply to each partition.");

  /**
   * Holds the instance of the preprocessed distance function
   * {@link #PARTITION_DISTANCE_ID}.
   */
  private FilteredLocalPCABasedDistanceFunction<V, ?> partitionDistanceFunction;

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
  // FIXME: remove this when migrating to a full Factory pattern!
  // This will not allow concurrent jobs.
  private FilteredLocalPCABasedDistanceFunction.Instance<V, LocalProjectionIndex<V, ?>> partitionDistanceQuery;

  /**
   * Constructor.
   * 
   * @param partitionDistanceFunction Distance function
   * @param partitionAlgorithm Algorithm to use on partitions
   * @param partitionAlgorithmParameters Parameters for Algorithm to run on
   *        partitions
   */
  public COPAC(FilteredLocalPCABasedDistanceFunction<V, ?> partitionDistanceFunction, Class<? extends ClusteringAlgorithm<Clustering<Model>>> partitionAlgorithm, Collection<Pair<OptionID, Object>> partitionAlgorithmParameters) {
    super();
    this.partitionDistanceFunction = partitionDistanceFunction;
    this.partitionAlgorithm = partitionAlgorithm;
    this.partitionAlgorithmParameters = partitionAlgorithmParameters;
  }

  /**
   * Performs the COPAC algorithm on the given database.
   * 
   * @param relation Relation to process
   * @return Clustering result
   */
  @SuppressWarnings("unchecked")
  public Clustering<Model> run(Relation<V> relation) {
    final int dim = RelationUtil.dimensionality(relation);

    partitionDistanceQuery = (FilteredLocalPCABasedDistanceFunction.Instance<V, LocalProjectionIndex<V, ?>>) partitionDistanceFunction.instantiate(relation);
    LocalProjectionIndex<V, ?> preprocin = partitionDistanceQuery.getIndex();

    ModifiableDBIDs[] partitions = partitionByCorrelationDimensionality(relation, preprocin, dim);

    // running partition algorithm
    return runPartitionAlgorithm(relation, partitions, partitionDistanceQuery);
  }

  /**
   * Partition the data set by correlation dimensionality.
   * 
   * @param relation Relation
   * @param preproc Preprocessor
   * @param dim Dimensionality of data set
   * @return Data set partitions
   */
  protected ModifiableDBIDs[] partitionByCorrelationDimensionality(Relation<V> relation, LocalProjectionIndex<V, ?> preproc, final int dim) {
    ModifiableDBIDs[] partitions = new ModifiableDBIDs[dim + 1];
    FiniteProgress partitionProgress = LOG.isVerbose() ? new FiniteProgress("Partitioning", relation.size(), LOG) : null;
    int processed = 1;

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      int corrdim = preproc.getLocalProjection(iditer).getCorrelationDimension();

      if(partitions[corrdim] == null) {
        partitions[corrdim] = DBIDUtil.newArray();
      }
      partitions[corrdim].add(iditer);

      if(partitionProgress != null) {
        partitionProgress.setProcessed(processed++, LOG);
      }
    }
    if(partitionProgress != null) {
      partitionProgress.ensureCompleted(LOG);
    }
    if(LOG.isVerbose()) {
      for(int i = 1; i <= dim; i++) {
        ModifiableDBIDs list = partitions[i];
        if(list != null) {
          LOG.verbose("Partition [corrDim = " + i + "]: " + list.size() + " objects.");
        }
      }
    }
    return partitions;
  }

  /**
   * Runs the partition algorithm and creates the result.
   * 
   * @param relation the database to run this algorithm on
   * @param partitionMap the map of partition IDs to object ids
   * @param query The preprocessor based query function
   */
  private Clustering<Model> runPartitionAlgorithm(Relation<V> relation, ModifiableDBIDs[] partitionMap, DistanceQuery<V> query) {
    final int dim = RelationUtil.dimensionality(relation);
    Clustering<Model> result = new Clustering<>("COPAC clustering", "copac-clustering");

    // TODO: use an extra finite progress for the partitions?
    for(int d = 1; d <= dim; d++) {
      // Skip empty partitions
      if(partitionMap[d] == null) {
        continue;
      }
      // noise partition
      if(d == RelationUtil.dimensionality(relation)) {
        // Make a Noise cluster
        result.addToplevelCluster(new Cluster<Model>(partitionMap[d], true, ClusterModel.CLUSTER));
        continue;
      }
      // Setup context to run the inner algorithm:
      ProxyDatabase proxy = new ProxyDatabase(partitionMap[d], relation);
      ClusteringAlgorithm<Clustering<Model>> partitionAlgorithm = instantiatePartitionAlgorithm(query);
      if(LOG.isVerbose()) {
        LOG.verbose("Running " + partitionAlgorithm.getClass().getName() + " on partition [corrDim = " + d + "]...");
      }
      Clustering<Model> p = partitionAlgorithm.run(proxy);
      // Re-Wrap resulting Clusters as DimensionModel clusters.
      for(Cluster<Model> clus : p.getAllClusters()) {
        if(clus.isNoise()) {
          result.addToplevelCluster(new Cluster<Model>(clus.getIDs(), true, ClusterModel.CLUSTER));
        }
        else {
          result.addToplevelCluster(new Cluster<Model>(clus.getIDs(), new DimensionModel(d)));
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
  protected ClusteringAlgorithm<Clustering<Model>> instantiatePartitionAlgorithm(DistanceQuery<V> query) {
    ListParameterization reconfig = new ListParameterization(partitionAlgorithmParameters);
    ProxyDistanceFunction<V> dist = ProxyDistanceFunction.proxy(query);
    reconfig.addParameter(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, dist);
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
  public FilteredLocalPCABasedDistanceFunction.Instance<V, LocalProjectionIndex<V, ?>> getPartitionDistanceQuery() {
    return partitionDistanceQuery;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    protected LocalProjectionIndex.Factory<V, ?> indexI = null;

    protected FilteredLocalPCABasedDistanceFunction<V, ?> pdistI = null;

    protected Class<? extends ClusteringAlgorithm<Clustering<Model>>> algC = null;

    protected Collection<Pair<OptionID, Object>> algO = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ClassParameter<Factory<V, ?>> indexP = new ClassParameter<>(PREPROCESSOR_ID, LocalProjectionIndex.Factory.class);
      if(config.grab(indexP)) {
        indexI = indexP.instantiateClass(config);
      }

      ObjectParameter<FilteredLocalPCABasedDistanceFunction<V, ?>> pdistP = new ObjectParameter<>(PARTITION_DISTANCE_ID, FilteredLocalPCABasedDistanceFunction.class, LocallyWeightedDistanceFunction.class);
      if(config.grab(pdistP)) {
        ListParameterization predefinedDist = new ListParameterization();
        predefinedDist.addParameter(IndexBasedDistanceFunction.INDEX_ID, indexI);
        ChainedParameterization chainDist = new ChainedParameterization(predefinedDist, config);
        chainDist.errorsTo(config);
        pdistI = pdistP.instantiateClass(chainDist);
        predefinedDist.reportInternalParameterizationErrors(config);
      }

      // Parameterize algorithm:
      ClassParameter<ClusteringAlgorithm<Clustering<Model>>> algP = new ClassParameter<>(PARTITION_ALGORITHM_ID, ClusteringAlgorithm.class);
      if(config.grab(algP)) {
        ListParameterization predefined = new ListParameterization();
        predefined.addParameter(DistanceBasedAlgorithm.DISTANCE_FUNCTION_ID, pdistI);
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
    protected COPAC<V> makeInstance() {
      return new COPAC<>(pdistI, algC, algO);
    }
  }
}