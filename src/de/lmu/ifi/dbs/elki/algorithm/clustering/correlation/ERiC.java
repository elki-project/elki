package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ProxyDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.correlation.ERiCDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.BitDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.FirstNEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
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
 * @apiviz.composedOf COPAC
 * @apiviz.composedOf DBSCAN
 * @apiviz.composedOf ERiCDistanceFunction
 * @apiviz.composedOf FirstNEigenPairFilter
 * @apiviz.composedOf PCAFilteredRunner
 * @apiviz.has CorrelationModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
// TODO: Re-use PCARunner objects somehow?
@Title("ERiC: Exploring Relationships among Correlation Clusters")
@Description("Performs the DBSCAN algorithm on the data using a special distance function taking into account correlations among attributes and builds " + "a hierarchy that allows multiple inheritance from the correlation clustering result.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, and A. Zimek", title = "On Exploring Complex Relationships of Correlation Clusters", booktitle = "Proc. 19th International Conference on Scientific and Statistical Database Management (SSDBM 2007), Banff, Canada, 2007", url = "http://dx.doi.org/10.1109/SSDBM.2007.21")
public class ERiC<V extends NumberVector<?>> extends AbstractAlgorithm<Clustering<CorrelationModel<V>>> implements ClusteringAlgorithm<Clustering<CorrelationModel<V>>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ERiC.class);

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
   * 
   * @param relation Relation to process
   * @return Clustering result
   */
  public Clustering<CorrelationModel<V>> run(Relation<V> relation) {
    final int dimensionality = RelationUtil.dimensionality(relation);

    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(3) : null;

    // run COPAC
    if(stepprog != null) {
      stepprog.beginStep(1, "Preprocessing local correlation dimensionalities and partitioning data", LOG);
    }
    Clustering<Model> copacResult = copacAlgorithm.run(relation);

    DistanceQuery<V, IntegerDistance> query = copacAlgorithm.getPartitionDistanceQuery();

    // extract correlation clusters
    if(stepprog != null) {
      stepprog.beginStep(2, "Extract correlation clusters", LOG);
    }
    SortedMap<Integer, List<Cluster<CorrelationModel<V>>>> clusterMap = extractCorrelationClusters(copacResult, relation, dimensionality);
    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder("Step 2: Extract correlation clusters...");
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
      LOG.debugFine(msg.toString());
    }
    if(LOG.isVerbose()) {
      int clusters = 0;
      for(List<Cluster<CorrelationModel<V>>> correlationClusters : clusterMap.values()) {
        clusters += correlationClusters.size();
      }
      LOG.verbose(clusters + " clusters extracted.");
    }

    // build hierarchy
    if(stepprog != null) {
      stepprog.beginStep(3, "Building hierarchy", LOG);
    }
    buildHierarchy(clusterMap, query);
    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder("Step 3: Build hierarchy");
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
      LOG.debugFine(msg.toString());
    }
    if(stepprog != null) {
      stepprog.setCompleted(LOG);
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
        Class<PCAFilteredRunner<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(PCAFilteredRunner.class);
        PCAFilteredRunner<V> pca = parameters.tryInstantiate(cls);
        for(ParameterException e : parameters.getErrors()) {
          LOG.warning("Error in internal parameterization: " + e.getMessage());
        }

        // get cluster list for this dimension.
        List<Cluster<CorrelationModel<V>>> correlationClusters = clusterMap.get(correlationDimension);
        if(correlationClusters == null) {
          correlationClusters = new ArrayList<Cluster<CorrelationModel<V>>>();
          clusterMap.put(correlationDimension, correlationClusters);
        }

        PCAFilteredResult pcares = pca.processIds(group, database);

        V centroid = Centroid.make(database, group).toVector(database);
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
          merged.addDBIDs(clus.getIDs());
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
      Class<PCAFilteredRunner<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(PCAFilteredRunner.class);
      PCAFilteredRunner<V> pca = parameters.tryInstantiate(cls);
      for(ParameterException e : parameters.getErrors()) {
        LOG.warning("Error in internal parameterization: " + e.getMessage());
      }
      PCAFilteredResult pcares = pca.processIds(noise.getIDs(), database);

      V centroid = Centroid.make(database, noise.getIDs()).toVector(database);
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

  private void buildHierarchy(SortedMap<Integer, List<Cluster<CorrelationModel<V>>>> clusterMap, DistanceQuery<V, IntegerDistance> query) {
    StringBuilder msg = new StringBuilder();

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
      if(LOG.isDebugging()) {
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
              if(LOG.isDebugging()) {
                msg.append('\n').append(parent).append(" is parent of ").append(child);
              }
            }
            else {
              BitDistance dist = distanceFunction.distance(parent.getModel().getCentroid(), child.getModel().getCentroid(), parent.getModel().getPCAResult(), child.getModel().getPCAResult());
              if(!dist.bitValue() && (child.getParents().isEmpty() || !isParent(distanceFunction, parent, child.getParents()))) {
                parent.getChildren().add(child);
                child.getParents().add(parent);
                if(LOG.isDebugging()) {
                  msg.append('\n').append(parent).append(" is parent of ").append(child);
                }
              }
            }
          }
        }
      }
    }
    if(LOG.isDebugging()) {
      LOG.debugFine(msg.toString());
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

    StringBuilder msg = new StringBuilder();

    for(Cluster<CorrelationModel<V>> child : children) {
      if(parent.getModel().getPCAResult().getCorrelationDimension() == child.getModel().getPCAResult().getCorrelationDimension()) {
        return false;
      }

      BitDistance dist = distanceFunction.distance(parent.getModel().getCentroid(), child.getModel().getCentroid(), parent.getModel().getPCAResult(), child.getModel().getPCAResult());
      if(LOG.isDebugging()) {
        msg.append("\ndist(").append(child).append(" - ").append(parent).append(") = ").append(dist);
      }
      if(!dist.bitValue()) {
        if(LOG.isDebugging()) {
          LOG.debugFine(msg.toString());
        }
        return true;
      }
    }

    if(LOG.isDebugging()) {
      LOG.debugFine(msg.toString());
    }
    return false;
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
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
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