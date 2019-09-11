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
package elki.clustering.correlation;

import java.util.ArrayList;
import java.util.List;

import elki.AbstractAlgorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.clustering.dbscan.*;
import elki.clustering.dbscan.predicates.CorePredicate;
import elki.clustering.dbscan.predicates.ERiCNeighborPredicate;
import elki.clustering.dbscan.predicates.MinPtsCorePredicate;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.CorrelationModel;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.Logging;
import elki.logging.progress.StepProgress;
import elki.math.linearalgebra.Centroid;
import elki.math.linearalgebra.pca.PCAFilteredResult;
import elki.math.linearalgebra.pca.PCAResult;
import elki.math.linearalgebra.pca.PCARunner;
import elki.math.linearalgebra.pca.filter.EigenPairFilter;
import elki.math.linearalgebra.pca.filter.FirstNEigenPairFilter;
import elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import elki.result.Metadata;
import elki.utilities.datastructures.hierarchy.Hierarchy;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Performs correlation clustering on the data partitioned according to local
 * correlation dimensionality and builds a hierarchy of correlation clusters
 * that allows multiple inheritance from the clustering result.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Christian Böhm, Hans-Peter Kriegel, Peer Kröger,
 * Arthur Zimek<br>
 * On Exploring Complex Relationships of Correlation Clusters<br>
 * Proc. 19th Int. Conf. Scientific and Statistical Database Management
 * (SSDBM 2007)
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @composed - - - COPAC
 * @composed - - - DBSCAN
 * @composed - - - FirstNEigenPairFilter
 * @composed - - - PCAFilteredRunner
 * @composed - - - ERiCNeighborPredicate
 * @has - - - CorrelationModel
 *
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("ERiC: Exploring Relationships among Correlation Clusters")
@Description("Performs the DBSCAN algorithm on the data using a special distance function taking into account correlations among attributes and builds " //
    + "a hierarchy that allows multiple inheritance from the correlation clustering result.")
@Reference(authors = "Elke Achtert, Christian Böhm, Hans-Peter Kriegel, Peer Kröger, Arthur Zimek", //
    title = "On Exploring Complex Relationships of Correlation Clusters", //
    booktitle = "Proc. 19th Int. Conf. Scientific and Statistical Database Management (SSDBM 2007)", //
    url = "https://doi.org/10.1109/SSDBM.2007.21", //
    bibkey = "DBLP:conf/ssdbm/AchtertBKKZ07")
public class ERiC<V extends NumberVector> extends AbstractAlgorithm<Clustering<CorrelationModel>> implements ClusteringAlgorithm<Clustering<CorrelationModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ERiC.class);

  /**
   * ERiC Settings.
   */
  private ERiC.Settings settings;

  /**
   * Constructor.
   * 
   * @param settings ERiC clustering settings
   */
  public ERiC(ERiC.Settings settings) {
    super();
    this.settings = settings;
  }

  /**
   * Performs the ERiC algorithm on the given database.
   * 
   * @param relation Relation to process
   * @return Clustering result
   */
  public Clustering<CorrelationModel> run(Database database, Relation<V> relation) {
    final int dim = RelationUtil.dimensionality(relation);

    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(3) : null;

    // Run Generalized DBSCAN
    LOG.beginStep(stepprog, 1, "Preprocessing local correlation dimensionalities and partitioning data");
    // FIXME: how to ensure we are running on the same relation?
    ERiCNeighborPredicate<V>.Instance npred = new ERiCNeighborPredicate<V>(settings).instantiate(relation);
    CorePredicate.Instance<DBIDs> cpred = new MinPtsCorePredicate(settings.minpts).instantiate(database);
    Clustering<Model> copacResult = new GeneralizedDBSCAN.Instance<>(npred, cpred, false).run();

    // extract correlation clusters
    LOG.beginStep(stepprog, 2, "Extract correlation clusters");
    List<List<Cluster<CorrelationModel>>> clusterMap = extractCorrelationClusters(copacResult, relation, dim, npred);
    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder("Step 2: Extract correlation clusters...");
      for(int corrDim = 0; corrDim < clusterMap.size(); corrDim++) {
        List<Cluster<CorrelationModel>> correlationClusters = clusterMap.get(corrDim);
        msg.append("\n\ncorrDim ").append(corrDim);
        for(Cluster<CorrelationModel> cluster : correlationClusters) {
          msg.append("\n  cluster ").append(cluster).append(", ids: ").append(cluster.getIDs().size());
          // .append(", level: ").append(cluster.getLevel()).append(", index:
          // ").append(cluster.getLevelIndex());
          // msg.append("\n basis " +
          // cluster.getPCA().getWeakEigenvectors().toString(" ", NF) +
          // " ids " + cluster.getIDs().size());
        }
      }
      LOG.debugFine(msg.toString());
    }
    if(LOG.isVerbose()) {
      int clusters = 0;
      for(List<Cluster<CorrelationModel>> correlationClusters : clusterMap) {
        clusters += correlationClusters.size();
      }
      LOG.verbose(clusters + " clusters extracted.");
    }

    // build hierarchy
    LOG.beginStep(stepprog, 3, "Building hierarchy");
    Clustering<CorrelationModel> clustering = new Clustering<>();
    Metadata.of(clustering).setLongName("ERiC Clustering");
    buildHierarchy(clustering, clusterMap, npred);
    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder("Step 3: Build hierarchy");
      for(int corrDim = 0; corrDim < clusterMap.size(); corrDim++) {
        List<Cluster<CorrelationModel>> correlationClusters = clusterMap.get(corrDim);
        for(Cluster<CorrelationModel> cluster : correlationClusters) {
          msg.append("\n  cluster ").append(cluster).append(", ids: ").append(cluster.getIDs().size());
          // .append(", level: ").append(cluster.getLevel()).append(", index:
          // ").append(cluster.getLevelIndex());
          for(It<Cluster<CorrelationModel>> iter = clustering.getClusterHierarchy().iterParents(cluster); iter.valid(); iter.advance()) {
            msg.append("\n   parent ").append(iter.get());
          }
          for(It<Cluster<CorrelationModel>> iter = clustering.getClusterHierarchy().iterChildren(cluster); iter.valid(); iter.advance()) {
            msg.append("\n   child ").append(iter.get());
          }
        }
      }
      LOG.debugFine(msg.toString());
    }
    LOG.setCompleted(stepprog);

    for(Cluster<CorrelationModel> rc : clusterMap.get(clusterMap.size() - 1)) {
      clustering.addToplevelCluster(rc);
    }
    return clustering;
  }

  /**
   * Extracts the correlation clusters and noise from the copac result and
   * returns a mapping of correlation dimension to maps of clusters within this
   * correlation dimension. Each cluster is defined by the basis vectors
   * defining the subspace in which the cluster appears.
   * 
   * @param dbscanResult DBSCAN clustering to use
   * @param relation the database containing the objects
   * @param dimensionality the dimensionality of the feature space
   * @param npred ERiC predicate
   * @return a list of clusters for each dimensionality
   */
  private List<List<Cluster<CorrelationModel>>> extractCorrelationClusters(Clustering<Model> dbscanResult, Relation<V> relation, int dimensionality, ERiCNeighborPredicate<V>.Instance npred) {
    // result
    List<List<Cluster<CorrelationModel>>> clusterMap = new ArrayList<>();
    for(int i = 0; i <= dimensionality; i++) {
      clusterMap.add(new ArrayList<Cluster<CorrelationModel>>());
    }

    // noise cluster containing all noise objects over all partitions
    Cluster<Model> noise = null;

    // iterate over correlation dimensions
    for(Cluster<Model> clus : dbscanResult.getAllClusters()) {
      DBIDs group = clus.getIDs();
      int dim = clus.isNoise() ? dimensionality : npred.dimensionality(clus.getIDs().iter());

      if(dim < dimensionality) {
        EigenPairFilter filter = new FirstNEigenPairFilter(dim);

        // get cluster list for this dimension.
        List<Cluster<CorrelationModel>> correlationClusters = clusterMap.get(dim);
        PCAResult epairs = settings.pca.processIds(group, relation);
        int numstrong = filter.filter(epairs.getEigenvalues());
        PCAFilteredResult pcares = new PCAFilteredResult(epairs.getEigenPairs(), numstrong, 1., 0.);

        double[] centroid = Centroid.make(relation, group).getArrayRef();
        Cluster<CorrelationModel> correlationCluster = new Cluster<>("[" + dim + "_" + correlationClusters.size() + "]", group, new CorrelationModel(pcares, centroid));
        correlationClusters.add(correlationCluster);
      }
      // partition containing noise
      else {
        if(noise == null) {
          noise = clus;
          continue;
        }
        ModifiableDBIDs merged = DBIDUtil.newHashSet(noise.getIDs());
        merged.addDBIDs(clus.getIDs());
        noise.setIDs(merged);
      }
    }

    if(noise != null && noise.size() > 0) {
      // get cluster list for this dimension.
      List<Cluster<CorrelationModel>> correlationClusters = clusterMap.get(dimensionality);
      EigenPairFilter filter = new FirstNEigenPairFilter(dimensionality);
      PCAResult epairs = settings.pca.processIds(noise.getIDs(), relation);
      int numstrong = filter.filter(epairs.getEigenvalues());
      PCAFilteredResult pcares = new PCAFilteredResult(epairs.getEigenPairs(), numstrong, 1., 0.);

      double[] centroid = Centroid.make(relation, noise.getIDs()).getArrayRef();
      Cluster<CorrelationModel> correlationCluster = new Cluster<>("[noise]", noise.getIDs(), new CorrelationModel(pcares, centroid));
      correlationClusters.add(correlationCluster);
    }

    // Delete dimensionalities not found.
    for(int i = dimensionality; i > 0; i--) {
      if(!clusterMap.get(i).isEmpty()) {
        break;
      }
      clusterMap.remove(i);
    }
    return clusterMap;
  }

  private void buildHierarchy(Clustering<CorrelationModel> clustering, List<List<Cluster<CorrelationModel>>> clusterMap, ERiCNeighborPredicate<V>.Instance npred) {
    StringBuilder msg = LOG.isDebuggingFine() ? new StringBuilder() : null;
    Hierarchy<Cluster<CorrelationModel>> hier = clustering.getClusterHierarchy();

    // Find maximum dimensionality found:
    int lambda_max = clusterMap.size() - 1;

    for(int childCorrDim = 0; childCorrDim < lambda_max; childCorrDim++) {
      List<Cluster<CorrelationModel>> children = clusterMap.get(childCorrDim);
      if(msg != null) {
        msg.append("\ncorrdim ").append(childCorrDim);
      }

      for(Cluster<CorrelationModel> child : children) {
        for(int parentCorrDim = childCorrDim + 1; parentCorrDim <= lambda_max; parentCorrDim++) {
          List<Cluster<CorrelationModel>> parents = clusterMap.get(parentCorrDim);
          for(Cluster<CorrelationModel> parent : parents) {
            int subspaceDim_parent = parent.getModel().getPCAResult().getCorrelationDimension();
            if(subspaceDim_parent == lambda_max && hier.numParents(child) == 0) {
              clustering.addChildCluster(parent, child);
              if(msg != null) {
                msg.append('\n').append(parent).append(" is parent of ").append(child);
              }
            }
            else {
              boolean dist = npred.weakNeighbors(parent.getModel().getPrototype(), child.getModel().getPrototype(), parent.getModel().getPCAResult(), child.getModel().getPCAResult());
              if(!dist && (hier.numParents(child) == 0 || !isParent(npred, parent, hier.iterParents(child)))) {
                clustering.addChildCluster(parent, child);
                if(msg != null) {
                  msg.append('\n').append(parent).append(" is parent of ").append(child);
                }
              }
            }
          }
        }
      }
    }
    if(msg != null) {
      LOG.debugFine(msg.toString());
    }
  }

  /**
   * Returns true, if the specified parent cluster is a parent of one child of
   * the children clusters.
   * 
   * @param npred Neighborhood predicate
   * @param parent the parent to be tested
   * @param iter the list of children to be tested
   * @return true, if the specified parent cluster is a parent of one child of
   *         the children clusters, false otherwise
   */
  private boolean isParent(ERiCNeighborPredicate<V>.Instance npred, Cluster<CorrelationModel> parent, It<Cluster<CorrelationModel>> iter) {
    StringBuilder msg = LOG.isDebugging() ? new StringBuilder() : null;

    for(; iter.valid(); iter.advance()) {
      Cluster<CorrelationModel> child = iter.get();
      if(parent.getModel().getPCAResult().getCorrelationDimension() == child.getModel().getPCAResult().getCorrelationDimension()) {
        return false;
      }

      boolean dist = npred.weakNeighbors(parent.getModel().getPrototype(), child.getModel().getPrototype(), parent.getModel().getPCAResult(), child.getModel().getPCAResult());
      if(msg != null) {
        msg.append("\ndist(").append(child).append(" - ").append(parent).append(") = ").append(dist);
      }
      if(dist) {
        if(msg != null) {
          LOG.debugFine(msg);
        }
        return true;
      }
    }

    if(msg != null) {
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
   * Class to wrap the ERiC settings.
   * 
   * @author Erich Schubert
   */
  public static class Settings {
    /**
     * Neighborhood size.
     */
    public int k;

    /**
     * Class to compute PCA.
     */
    public PCARunner pca;

    /**
     * Filter for Eigenvectors.
     */
    public EigenPairFilter filter;

    /**
     * Parameter to specify the threshold for approximate linear dependency: the
     * strong eigenvectors of q are approximately linear dependent from the
     * strong eigenvectors p if the following condition holds for all strong
     * eigenvectors q_i of q (lambda_q &lt; lambda_p): q_i' * M^check_p * q_i
     * &lt;= delta^2, must be a double equal to or greater than 0.
     */
    public double delta;

    /**
     * Parameter to specify the threshold for the maximum distance between two
     * approximately linear dependent subspaces of two objects p and q (lambda_q
     * &lt; lambda_p) before considering them as parallel, must be a double
     * equal to or greater than 0.
     */
    public double tau;

    /**
     * Minimum neighborhood size (density).
     */
    public int minpts;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Size for the kNN neighborhood used in the PCA step of ERiC.
     */
    public static final OptionID K_ID = new OptionID("eric.k", "Number of neighbors to use for PCA.");

    /**
     * Parameter to specify the threshold for approximate linear dependency: the
     * strong eigenvectors of q are approximately linear dependent from the
     * strong eigenvectors p if the following condition holds for all strong
     * eigenvectors q_i of q (lambda_q &lt; lambda_p): q_i' * M^check_p * q_i
     * &lt;= delta^2, must be a double equal to or greater than 0.
     */
    public static final OptionID DELTA_ID = new OptionID("ericdf.delta", "Threshold for approximate linear dependency: " + "the strong eigenvectors of q are approximately linear dependent " + "from the strong eigenvectors p if the following condition " + "holds for all stroneg eigenvectors q_i of q (lambda_q < lambda_p): " + "q_i' * M^check_p * q_i <= delta^2.");

    /**
     * Parameter to specify the threshold for the maximum distance between two
     * approximately linear dependent subspaces of two objects p and q (lambda_q
     * &lt; lambda_p) before considering them as parallel, must be a double
     * equal to or greater than 0.
     */
    public static final OptionID TAU_ID = new OptionID("ericdf.tau", "Threshold for the maximum distance between two approximately linear " + "dependent subspaces of two objects p and q " + "(lambda_q < lambda_p) before considering them as parallel.");

    /**
     * The settings to use.
     */
    protected ERiC.Settings settings;

    @Override
    public void configure(Parameterization config) {
      settings = new Settings();
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> settings.k = x);
      new ObjectParameter<PCARunner>(PCARunner.Par.PCARUNNER_ID, PCARunner.class, PCARunner.class) //
          .grab(config, x -> settings.pca = x);
      new ObjectParameter<EigenPairFilter>(EigenPairFilter.PCA_EIGENPAIR_FILTER, EigenPairFilter.class, PercentageEigenPairFilter.class) //
          .grab(config, x -> settings.filter = x);
      new DoubleParameter(DELTA_ID, 0.1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> settings.delta = x);
      new DoubleParameter(TAU_ID, 0.1) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> settings.tau = x);
      new IntParameter(DBSCAN.Par.MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> settings.minpts = x);
    }

    @Override
    public ERiC<V> make() {
      return new ERiC<>(settings);
    }
  }
}
