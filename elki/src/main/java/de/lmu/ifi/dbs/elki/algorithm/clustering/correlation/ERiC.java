package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

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
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.CorePredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.ERiCNeighborPredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.GeneralizedDBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.MinPtsCorePredicate;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.CorrelationModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.FirstNEigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.StandardCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy.Iter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Performs correlation clustering on the data partitioned according to local
 * correlation dimensionality and builds a hierarchy of correlation clusters
 * that allows multiple inheritance from the clustering result.
 * 
 * Reference:
 * <p>
 * E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, and A. Zimek:<br />
 * On Exploring Complex Relationships of Correlation Clusters.<br />
 * In Proc. 19th International Conference on Scientific and Statistical Database
 * Management (SSDBM 2007), Banff, Canada, 2007.
 * </p>
 * 
 * @author Elke Achtert
 * @since 0.2
 * 
 * @apiviz.composedOf COPAC
 * @apiviz.composedOf DBSCAN
 * @apiviz.composedOf FirstNEigenPairFilter
 * @apiviz.composedOf PCAFilteredRunner
 * @apiviz.composedOf ERiCNeighborPredicate
 * @apiviz.has CorrelationModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("ERiC: Exploring Relationships among Correlation Clusters")
@Description("Performs the DBSCAN algorithm on the data using a special distance function taking into account correlations among attributes and builds " //
    + "a hierarchy that allows multiple inheritance from the correlation clustering result.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, and A. Zimek",//
title = "On Exploring Complex Relationships of Correlation Clusters", //
booktitle = "Proc. 19th International Conference on Scientific and Statistical Database Management (SSDBM 2007), Banff, Canada, 2007", //
url = "http://dx.doi.org/10.1109/SSDBM.2007.21")
public class ERiC<V extends NumberVector> extends AbstractAlgorithm<Clustering<CorrelationModel<V>>> implements ClusteringAlgorithm<Clustering<CorrelationModel<V>>> {
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
  public Clustering<CorrelationModel<V>> run(Database database, Relation<V> relation) {
    final int dimensionality = RelationUtil.dimensionality(relation);

    StepProgress stepprog = LOG.isVerbose() ? new StepProgress(3) : null;

    // Run Generalized DBSCAN
    LOG.beginStep(stepprog, 1, "Preprocessing local correlation dimensionalities and partitioning data");
    // FIXME: how to ensure we are running on the same relation?
    ERiCNeighborPredicate<V>.Instance npred = new ERiCNeighborPredicate<V>(settings).instantiate(database, relation);
    CorePredicate.Instance<DBIDs> cpred = new MinPtsCorePredicate(settings.minpts).instantiate(database, TypeUtil.DBIDS);
    Clustering<Model> copacResult = new GeneralizedDBSCAN.Instance<>(npred, cpred, false).run();

    // extract correlation clusters
    LOG.beginStep(stepprog, 2, "Extract correlation clusters");
    List<List<Cluster<CorrelationModel<V>>>> clusterMap = extractCorrelationClusters(copacResult, relation, dimensionality, npred);
    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder("Step 2: Extract correlation clusters...");
      for(int corrDim = 0; corrDim < clusterMap.size(); corrDim++) {
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
      for(List<Cluster<CorrelationModel<V>>> correlationClusters : clusterMap) {
        clusters += correlationClusters.size();
      }
      LOG.verbose(clusters + " clusters extracted.");
    }

    // build hierarchy
    LOG.beginStep(stepprog, 3, "Building hierarchy");
    Clustering<CorrelationModel<V>> clustering = new Clustering<>("ERiC clustering", "eric-clustering");
    buildHierarchy(clustering, clusterMap, npred);
    if(LOG.isDebugging()) {
      StringBuilder msg = new StringBuilder("Step 3: Build hierarchy");
      for(int corrDim = 0; corrDim < clusterMap.size(); corrDim++) {
        List<Cluster<CorrelationModel<V>>> correlationClusters = clusterMap.get(corrDim);
        for(Cluster<CorrelationModel<V>> cluster : correlationClusters) {
          msg.append("\n  cluster ").append(cluster).append(", ids: ").append(cluster.getIDs().size());
          // .append(", level: ").append(cluster.getLevel()).append(", index: ").append(cluster.getLevelIndex());
          for(Iter<Cluster<CorrelationModel<V>>> iter = clustering.getClusterHierarchy().iterParents(cluster); iter.valid(); iter.advance()) {
            msg.append("\n   parent ").append(iter.get());
          }
          for(Iter<Cluster<CorrelationModel<V>>> iter = clustering.getClusterHierarchy().iterChildren(cluster); iter.valid(); iter.advance()) {
            msg.append("\n   child ").append(iter.get());
          }
        }
      }
      LOG.debugFine(msg.toString());
    }
    LOG.setCompleted(stepprog);

    for(Cluster<CorrelationModel<V>> rc : clusterMap.get(clusterMap.size() - 1)) {
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
   * @param dbscanResult
   * 
   * @param database the database containing the objects
   * @param dimensionality the dimensionality of the feature space
   * @param npred ERiC predicate
   * @return a list of clusters for each dimensionality
   */
  private List<List<Cluster<CorrelationModel<V>>>> extractCorrelationClusters(Clustering<Model> dbscanResult, Relation<V> database, int dimensionality, ERiCNeighborPredicate<V>.Instance npred) {
    // result
    List<List<Cluster<CorrelationModel<V>>>> clusterMap = new ArrayList<>();
    for(int i = 0; i <= dimensionality; i++) {
      clusterMap.add(new ArrayList<Cluster<CorrelationModel<V>>>());
    }

    // noise cluster containing all noise objects over all partitions
    Cluster<Model> noise = null;

    // iterate over correlation dimensions
    for(Cluster<Model> clus : dbscanResult.getAllClusters()) {
      DBIDs group = clus.getIDs();
      int dim = clus.isNoise() ? dimensionality : npred.dimensionality(clus.getIDs().iter());

      if(dim < dimensionality) {
        PCAFilteredRunner pca = new PCAFilteredRunner(new StandardCovarianceMatrixBuilder(), new FirstNEigenPairFilter(dim), 1., 0.);

        // get cluster list for this dimension.
        List<Cluster<CorrelationModel<V>>> correlationClusters = clusterMap.get(dim);
        PCAFilteredResult pcares = pca.processIds(group, database);

        V centroid = Centroid.make(database, group).toVector(database);
        Cluster<CorrelationModel<V>> correlationCluster = new Cluster<>("[" + dim + "_" + correlationClusters.size() + "]", group, new CorrelationModel<>(pcares, centroid));
        correlationClusters.add(correlationCluster);
      }
      // partition containing noise
      else {
        if(noise == null) {
          noise = clus;
        }
        else {
          ModifiableDBIDs merged = DBIDUtil.newHashSet(noise.getIDs());
          merged.addDBIDs(clus.getIDs());
          noise.setIDs(merged);
        }
      }
    }

    if(noise != null && noise.size() > 0) {
      // get cluster list for this dimension.
      List<Cluster<CorrelationModel<V>>> correlationClusters = clusterMap.get(dimensionality);
      PCAFilteredRunner pca = new PCAFilteredRunner(new StandardCovarianceMatrixBuilder(), new FirstNEigenPairFilter(dimensionality), 1., 0.);
      PCAFilteredResult pcares = pca.processIds(noise.getIDs(), database);

      V centroid = Centroid.make(database, noise.getIDs()).toVector(database);
      Cluster<CorrelationModel<V>> correlationCluster = new Cluster<>("[noise]", noise.getIDs(), new CorrelationModel<>(pcares, centroid));
      correlationClusters.add(correlationCluster);
    }

    // Delete dimensionalities not found.
    for(int i = dimensionality; i > 0; i--) {
      if(clusterMap.get(i).size() > 0) {
        break;
      }
      clusterMap.remove(i);
    }

    return clusterMap;
  }

  private void buildHierarchy(Clustering<CorrelationModel<V>> clustering, List<List<Cluster<CorrelationModel<V>>>> clusterMap, ERiCNeighborPredicate<V>.Instance npred) {
    StringBuilder msg = LOG.isDebuggingFine() ? new StringBuilder() : null;
    Hierarchy<Cluster<CorrelationModel<V>>> hier = clustering.getClusterHierarchy();

    // Find maximum dimensionality found:
    int lambda_max = clusterMap.size() - 1;

    for(int childCorrDim = 0; childCorrDim < lambda_max; childCorrDim++) {
      List<Cluster<CorrelationModel<V>>> children = clusterMap.get(childCorrDim);
      if(msg != null) {
        msg.append("\ncorrdim ").append(childCorrDim);
      }

      for(Cluster<CorrelationModel<V>> child : children) {
        for(int parentCorrDim = childCorrDim + 1; parentCorrDim <= lambda_max; parentCorrDim++) {
          List<Cluster<CorrelationModel<V>>> parents = clusterMap.get(parentCorrDim);
          for(Cluster<CorrelationModel<V>> parent : parents) {
            int subspaceDim_parent = parent.getModel().getPCAResult().getCorrelationDimension();
            if(subspaceDim_parent == lambda_max && hier.numParents(child) == 0) {
              clustering.addChildCluster(parent, child);
              if(msg != null) {
                msg.append('\n').append(parent).append(" is parent of ").append(child);
              }
            }
            else {
              boolean dist = npred.weakNeighbors(parent.getModel().getCentroid(), child.getModel().getCentroid(), parent.getModel().getPCAResult(), child.getModel().getPCAResult());
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
  private boolean isParent(ERiCNeighborPredicate<V>.Instance npred, Cluster<CorrelationModel<V>> parent, Iter<Cluster<CorrelationModel<V>>> iter) {
    StringBuilder msg = LOG.isDebugging() ? new StringBuilder() : null;

    for(; iter.valid(); iter.advance()) {
      Cluster<CorrelationModel<V>> child = iter.get();
      if(parent.getModel().getPCAResult().getCorrelationDimension() == child.getModel().getPCAResult().getCorrelationDimension()) {
        return false;
      }

      boolean dist = npred.weakNeighbors(parent.getModel().getCentroid(), child.getModel().getCentroid(), parent.getModel().getPCAResult(), child.getModel().getPCAResult());
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
   * 
   * @apiviz.exclude
   */
  public static class Settings {
    /**
     * Neighborhood size.
     */
    public int k;

    /**
     * Class to compute PCA.
     */
    public PCAFilteredRunner pca;

    /**
     * Parameter to specify the threshold for approximate linear dependency: the
     * strong eigenvectors of q are approximately linear dependent from the
     * strong eigenvectors p if the following condition holds for all strong
     * eigenvectors q_i of q (lambda_q < lambda_p): q_i' * M^check_p * q_i <=
     * delta^2, must be a double equal to or greater than 0.
     */
    public double delta;

    /**
     * Parameter to specify the threshold for the maximum distance between two
     * approximately linear dependent subspaces of two objects p and q (lambda_q
     * < lambda_p) before considering them as parallel, must be a double equal
     * to or greater than 0.
     */
    public double tau;

    /**
     * Minimum neighborhood size (density).
     */
    public int minpts;

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer extends AbstractParameterizer {
      /**
       * Size for the kNN neighborhood used in the PCA step of ERiC.
       */
      public static final OptionID K_ID = new OptionID("eric.k", "Number of neighbors to use for PCA.");

      /**
       * Parameter to specify the threshold for approximate linear dependency:
       * the strong eigenvectors of q are approximately linear dependent from
       * the strong eigenvectors p if the following condition holds for all
       * strong eigenvectors q_i of q (lambda_q < lambda_p): q_i' * M^check_p *
       * q_i <= delta^2, must be a double equal to or greater than 0.
       * <p>
       * Default value: {@code 0.1}
       * </p>
       * <p>
       * Key: {@code -ericdf.delta}
       * </p>
       */
      public static final OptionID DELTA_ID = new OptionID("ericdf.delta", "Threshold for approximate linear dependency: " + "the strong eigenvectors of q are approximately linear dependent " + "from the strong eigenvectors p if the following condition " + "holds for all stroneg eigenvectors q_i of q (lambda_q < lambda_p): " + "q_i' * M^check_p * q_i <= delta^2.");

      /**
       * Parameter to specify the threshold for the maximum distance between two
       * approximately linear dependent subspaces of two objects p and q
       * (lambda_q < lambda_p) before considering them as parallel, must be a
       * double equal to or greater than 0.
       * <p>
       * Default value: {@code 0.1}
       * </p>
       * <p>
       * Key: {@code -ericdf.tau}
       * </p>
       */
      public static final OptionID TAU_ID = new OptionID("ericdf.tau", "Threshold for the maximum distance between two approximately linear " + "dependent subspaces of two objects p and q " + "(lambda_q < lambda_p) before considering them as parallel.");

      /**
       * Settings to build.
       */
      Settings settings;

      @Override
      public void makeOptions(Parameterization config) {
        settings = new Settings();
        configK(config);
        // TODO: allow using other PCA runners?
        settings.pca = config.tryInstantiate(PCAFilteredRunner.class);
        configDelta(config);
        configTau(config);
        configMinPts(config);
      }

      /**
       * Configure the kNN parameter.
       * 
       * @param config Parameter source
       */
      protected void configK(Parameterization config) {
        IntParameter kP = new IntParameter(K_ID) //
        .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(kP)) {
          settings.k = kP.intValue();
        }
      }

      /**
       * Configure the delta parameter.
       * 
       * @param config Parameter source
       */
      protected void configDelta(Parameterization config) {
        final DoubleParameter deltaP = new DoubleParameter(DELTA_ID, 0.1);
        deltaP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
        if(config.grab(deltaP)) {
          settings.delta = deltaP.doubleValue();
        }
      }

      /**
       * Configure the tau parameter.
       * 
       * @param config Parameter source
       */
      protected void configTau(Parameterization config) {
        final DoubleParameter tauP = new DoubleParameter(TAU_ID, 0.1);
        tauP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
        if(config.grab(tauP)) {
          settings.tau = tauP.doubleValue();
        }
      }

      /**
       * Configure the minPts aka "mu" parameter.
       * 
       * @param config Parameter source
       */
      protected void configMinPts(Parameterization config) {
        IntParameter minptsP = new IntParameter(DBSCAN.Parameterizer.MINPTS_ID) //
        .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(minptsP)) {
          settings.minpts = minptsP.intValue();
        }
      }

      @Override
      public Settings makeInstance() {
        return settings;
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * The settings to use.
     */
    protected ERiC.Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      settings = config.tryInstantiate(ERiC.Settings.class);
    }

    @Override
    protected ERiC<V> makeInstance() {
      return new ERiC<>(settings);
    }
  }
}
