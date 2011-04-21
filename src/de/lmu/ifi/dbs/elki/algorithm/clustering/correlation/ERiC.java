package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationModel;
import de.lmu.ifi.dbs.elki.data.model.DimensionModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ProxyDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.correlation.ERiCDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.BitDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.FirstNEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Performs correlation clustering on the data partitioned according to local
 * correlation dimensionality and builds a hierarchy of correlation clusters
 * that allows multiple inheritance from the clustering result.
 * <p>
 * Reference: E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, and A. Zimek: On
 * Exploring Complex Relationships of Correlation Clusters. <br>
 * In Proc. 19th International Conference on Scientific and Statistical Database
 * Management (SSDBM 2007), Banff, Canada, 2007.
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses COPAC
 * @apiviz.uses DBSCAN
 * @apiviz.uses ERiCDistanceFunction
 * @apiviz.uses FirstNEigenPairFilter
 * @apiviz.uses PCAFilteredRunner
 * @apiviz.has CorrelationModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
// TODO: Re-use PCARunner objects somehow?
@Title("ERiC: Exploring Relationships among Correlation Clusters")
@Description("Performs the DBSCAN algorithm on the data using a special distance function taking into account correlations among attributes and builds " + "a hierarchy that allows multiple inheritance from the correlation clustering result.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, and A. Zimek", title = "On Exploring Complex Relationships of Correlation Clusters", booktitle = "Proc. 19th International Conference on Scientific and Statistical Database Management (SSDBM 2007), Banff, Canada, 2007", url = "http://dx.doi.org/10.1109/SSDBM.2007.21")
public class ERiC<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V> implements ClusteringAlgorithm<Clustering<CorrelationModel<V>>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ERiC.class);

  /**
   * The COPAC clustering algorithm.
   */
  private COPAC<V, IntegerDistance> copacAlgorithm;

  /**
   * Constructor.
   * 
   * @param copacAlgorithm COPAC to use
   */
  public ERiC(COPAC<V, IntegerDistance> copacAlgorithm) {
    super();
    this.copacAlgorithm = copacAlgorithm;
  }

  /**
   * Performs the ERiC algorithm on the given database.
   */
  @Override
  public Clustering<CorrelationModel<V>> run(Database database) throws IllegalStateException {
    Relation<V> dataQuery = getRelation(database);
    int dimensionality = DatabaseUtil.dimensionality(dataQuery);

    StepProgress stepprog = logger.isVerbose() ? new StepProgress(3) : null;

    // run COPAC
    if(stepprog != null) {
      stepprog.beginStep(1, "Preprocessing local correlation dimensionalities and partitioning data", logger);
    }
    Clustering<Model> copacResult = copacAlgorithm.run(database);

    DistanceQuery<V, IntegerDistance> query = copacAlgorithm.getPartitionDistanceQuery();

    // extract correlation clusters
    if(stepprog != null) {
      stepprog.beginStep(2, "Extract correlation clusters", logger);
    }
    SortedMap<Integer, List<Cluster<CorrelationModel<V>>>> clusterMap = extractCorrelationClusters(copacResult, dataQuery, dimensionality);
    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer("Step 2: Extract correlation clusters...");
      for(Integer corrDim : clusterMap.keySet()) {
        List<Cluster<CorrelationModel<V>>> correlationClusters = clusterMap.get(corrDim);
        msg.append("\n\ncorrDim ").append(corrDim);
        for(Cluster<CorrelationModel<V>> cluster : correlationClusters) {
          msg.append("\n  cluster ").append(cluster).append(", ids: ").append(cluster.getIDs().size());
          // .append(", level: ").append(cluster.getLevel()).append(", index: ").append(cluster.getLevelIndex());
          // msg.append("\n  basis " +
          // cluster.getPCA().getWeakEigenvectors().toString("    ", NF) +
          // "  ids " + cluster.getIDs().size());
        }
      }
      logger.debugFine(msg.toString());
    }
    if(logger.isVerbose()) {
      int clusters = 0;
      for(List<Cluster<CorrelationModel<V>>> correlationClusters : clusterMap.values()) {
        clusters += correlationClusters.size();
      }
      logger.verbose(clusters + " clusters extracted.");
    }

    // build hierarchy
    if(stepprog != null) {
      stepprog.beginStep(3, "Building hierarchy", logger);
    }
    buildHierarchy(clusterMap, query);
    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer("Step 3: Build hierarchy");
      for(Integer corrDim : clusterMap.keySet()) {
        List<Cluster<CorrelationModel<V>>> correlationClusters = clusterMap.get(corrDim);
        for(Cluster<CorrelationModel<V>> cluster : correlationClusters) {
          msg.append("\n  cluster ").append(cluster).append(", ids: ").append(cluster.getIDs().size());
          // .append(", level: ").append(cluster.getLevel()).append(", index: ").append(cluster.getLevelIndex());
          for(int i = 0; i < cluster.getParents().size(); i++) {
            msg.append("\n   parent ").append(cluster.getParents().get(i));
          }
          for(int i = 0; i < cluster.numChildren(); i++) {
            msg.append("\n   child ").append(cluster.getChildren().get(i));
          }
        }
      }
      logger.debugFine(msg.toString());
    }
    if(stepprog != null) {
      stepprog.setCompleted(logger);
    }

    Clustering<CorrelationModel<V>> result = new Clustering<CorrelationModel<V>>("ERiC clustering", "eric-clustering");
    for(Cluster<CorrelationModel<V>> rc : clusterMap.get(clusterMap.lastKey())) {
      result.addCluster(rc);
    }
    return result;
  }

  /**
   * Extracts the correlation clusters and noise from the copac result and
   * returns a mapping of correlation dimension to maps of clusters within this
   * correlation dimension. Each cluster is defined by the basis vectors
   * defining the subspace in which the cluster appears.
   * 
   * @param copacResult
   * 
   * @param database the database containing the objects
   * @param dimensionality the dimensionality of the feature space
   * @return a mapping of correlation dimension to maps of clusters
   */
  private SortedMap<Integer, List<Cluster<CorrelationModel<V>>>> extractCorrelationClusters(Clustering<Model> copacResult, Relation<V> database, int dimensionality) {
    // result
    SortedMap<Integer, List<Cluster<CorrelationModel<V>>>> clusterMap = new TreeMap<Integer, List<Cluster<CorrelationModel<V>>>>();

    // noise cluster containing all noise objects over all partitions
    Cluster<Model> noise = null;

    // iterate over correlation dimensions
    for(Cluster<Model> clus : copacResult.getAllClusters()) {
      DBIDs group = clus.getIDs();
      if(clus.getModel() != null && clus.getModel() instanceof DimensionModel) {
        int correlationDimension = ((DimensionModel) clus.getModel()).getDimension();

        ListParameterization parameters = pcaParameters(correlationDimension);
        Class<PCAFilteredRunner<V, DoubleDistance>> cls = ClassGenericsUtil.uglyCastIntoSubclass(PCAFilteredRunner.class);
        PCAFilteredRunner<V, DoubleDistance> pca = parameters.tryInstantiate(cls);
        for(ParameterException e : parameters.getErrors()) {
          logger.warning("Error in internal parameterization: " + e.getMessage());
        }

        // get cluster list for this dimension.
        List<Cluster<CorrelationModel<V>>> correlationClusters = clusterMap.get(correlationDimension);
        if(correlationClusters == null) {
          correlationClusters = new ArrayList<Cluster<CorrelationModel<V>>>();
          clusterMap.put(correlationDimension, correlationClusters);
        }

        PCAFilteredResult pcares = pca.processIds(group, database);

        V centroid = DatabaseUtil.centroid(database, group);
        Cluster<CorrelationModel<V>> correlationCluster = new Cluster<CorrelationModel<V>>("[" + correlationDimension + "_" + correlationClusters.size() + "]", group, new CorrelationModel<V>(pcares, centroid), new ArrayList<Cluster<CorrelationModel<V>>>(), new ArrayList<Cluster<CorrelationModel<V>>>());
        correlationClusters.add(correlationCluster);
      }
      // partition containing noise
      else if(clus.getModel() != null && clus.isNoise()) {
        if(noise == null) {
          noise = clus;
        }
        else {
          ModifiableDBIDs merged = DBIDUtil.newHashSet(noise.getIDs());
          merged.addAll(clus.getIDs().asCollection());
          noise.setIDs(merged);
        }
      }
      else {
        throw new IllegalStateException("Unexpected group returned: " + clus.getClass().getName());
      }
    }

    if(noise != null && noise.size() > 0) {
      // get cluster list for this dimension.
      List<Cluster<CorrelationModel<V>>> correlationClusters = clusterMap.get(dimensionality);
      if(correlationClusters == null) {
        correlationClusters = new ArrayList<Cluster<CorrelationModel<V>>>();
        clusterMap.put(dimensionality, correlationClusters);
      }
      ListParameterization parameters = pcaParameters(dimensionality);
      Class<PCAFilteredRunner<V, DoubleDistance>> cls = ClassGenericsUtil.uglyCastIntoSubclass(PCAFilteredRunner.class);
      PCAFilteredRunner<V, DoubleDistance> pca = parameters.tryInstantiate(cls);
      for(ParameterException e : parameters.getErrors()) {
        logger.warning("Error in internal parameterization: " + e.getMessage());
      }
      PCAFilteredResult pcares = pca.processIds(noise.getIDs(), database);

      V centroid = DatabaseUtil.centroid(database, noise.getIDs());
      Cluster<CorrelationModel<V>> correlationCluster = new Cluster<CorrelationModel<V>>("[noise]", noise.getIDs(), new CorrelationModel<V>(pcares, centroid), new ArrayList<Cluster<CorrelationModel<V>>>(), new ArrayList<Cluster<CorrelationModel<V>>>());
      correlationClusters.add(correlationCluster);
    }

    return clusterMap;
  }

  /**
   * Returns the parameters for the PCA for the specified correlation dimension.
   * 
   * @param correlationDimension the correlation dimension
   * @return the parameters for the PCA for the specified correlation dimension
   */
  private ListParameterization pcaParameters(int correlationDimension) {
    ListParameterization parameters = new ListParameterization();

    parameters.addParameter(PCAFilteredRunner.PCA_EIGENPAIR_FILTER, FirstNEigenPairFilter.class);
    parameters.addParameter(FirstNEigenPairFilter.EIGENPAIR_FILTER_N, correlationDimension);

    return parameters;
  }

  private void buildHierarchy(SortedMap<Integer, List<Cluster<CorrelationModel<V>>>> clusterMap, DistanceQuery<V, IntegerDistance> query) throws IllegalStateException {
    StringBuffer msg = new StringBuffer();

    DBSCAN<V, DoubleDistance> dbscan = ClassGenericsUtil.castWithGenericsOrNull(DBSCAN.class, copacAlgorithm.getPartitionAlgorithm(query));
    if(dbscan == null) {
      // TODO: appropriate exception class?
      throw new IllegalArgumentException("ERiC was run without DBSCAN as COPAC algorithm!");
    }
    DistanceFunction<? super V, ?> dfun = ProxyDistanceFunction.unwrapDistance(dbscan.getDistanceFunction());
    ERiCDistanceFunction distanceFunction = ClassGenericsUtil.castWithGenericsOrNull(ERiCDistanceFunction.class, dfun);
    if(distanceFunction == null) {
      // TODO: appropriate exception class?
      throw new IllegalArgumentException("ERiC was run without ERiCDistanceFunction as distance function: got " + dfun.getClass());
    }
    Integer lambda_max = clusterMap.lastKey();

    for(Integer childCorrDim : clusterMap.keySet()) {
      List<Cluster<CorrelationModel<V>>> children = clusterMap.get(childCorrDim);
      SortedMap<Integer, List<Cluster<CorrelationModel<V>>>> parentMap = clusterMap.tailMap(childCorrDim + 1);
      if(logger.isDebugging()) {
        msg.append("\ncorrdim ").append(childCorrDim);
        msg.append("\nparents ").append(parentMap.keySet());
      }

      for(Cluster<CorrelationModel<V>> child : children) {
        for(Integer parentCorrDim : parentMap.keySet()) {
          List<Cluster<CorrelationModel<V>>> parents = parentMap.get(parentCorrDim);
          for(Cluster<CorrelationModel<V>> parent : parents) {
            int subspaceDim_parent = parent.getModel().getPCAResult().getCorrelationDimension();
            if(subspaceDim_parent == lambda_max && child.getParents().isEmpty()) {
              parent.getChildren().add(child);
              child.getParents().add(parent);
              if(logger.isDebugging()) {
                msg.append("\n").append(parent).append(" is parent of ").append(child);
              }
            }
            else {
              BitDistance dist = distanceFunction.distance(parent.getModel().getCentroid(), child.getModel().getCentroid(), parent.getModel().getPCAResult(), child.getModel().getPCAResult());
              if(!dist.bitValue() && (child.getParents().isEmpty() || !isParent(distanceFunction, parent, child.getParents()))) {
                parent.getChildren().add(child);
                child.getParents().add(parent);
                if(logger.isDebugging()) {
                  msg.append("\n").append(parent).append(" is parent of ").append(child);
                }
              }
            }
          }
        }
      }
    }
    if(logger.isDebugging()) {
      logger.debugFine(msg.toString());
    }

  }

  /**
   * Returns true, if the specified parent cluster is a parent of one child of
   * the children clusters.
   * 
   * @param distanceFunction the distance function for distance computation
   *        between the clusters
   * @param parent the parent to be tested
   * @param children the list of children to be tested
   * @return true, if the specified parent cluster is a parent of one child of
   *         the children clusters, false otherwise
   */
  private boolean isParent(ERiCDistanceFunction distanceFunction, Cluster<CorrelationModel<V>> parent, List<Cluster<CorrelationModel<V>>> children) {

    StringBuffer msg = new StringBuffer();

    for(Cluster<CorrelationModel<V>> child : children) {
      if(parent.getModel().getPCAResult().getCorrelationDimension() == child.getModel().getPCAResult().getCorrelationDimension()) {
        return false;
      }

      BitDistance dist = distanceFunction.distance(parent.getModel().getCentroid(), child.getModel().getCentroid(), parent.getModel().getPCAResult(), child.getModel().getPCAResult());
      if(logger.isDebugging()) {
        msg.append("\ndist(").append(child).append(" - ").append(parent).append(") = ").append(dist);
      }
      if(!dist.bitValue()) {
        if(logger.isDebugging()) {
          logger.debugFine(msg.toString());
        }
        return true;
      }
    }

    if(logger.isDebugging()) {
      logger.debugFine(msg.toString());
    }
    return false;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
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
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    /**
     * The COPAC instance to use
     */
    protected COPAC<V, IntegerDistance> copac;

    @SuppressWarnings("unchecked")
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      copac = config.tryInstantiate(COPAC.class);
    }

    @Override
    protected ERiC<V> makeInstance() {
      return new ERiC<V>(copac);
    }
  }
}