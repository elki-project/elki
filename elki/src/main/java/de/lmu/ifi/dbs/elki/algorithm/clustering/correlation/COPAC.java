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

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.COPACNeighborPredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.CorePredicate;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.GeneralizedDBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.MinPtsCorePredicate;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.DimensionModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredRunner;
import de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy.Hierarchy;
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
 * COPAC is an algorithm to partition a database according to the correlation
 * dimension of its objects and to then perform an arbitrary clustering
 * algorithm over the partitions.
 * 
 * Reference:
 * <p>
 * E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, A. Zimek:<br />
 * Robust, Complete, and Efficient Correlation Clustering.<br />
 * In Proc. 7th SIAM International Conference on Data Mining (SDM'07),
 * Minneapolis, MN, 2007
 * </p>
 * 
 * @author Arthur Zimek
 * @since 0.2
 * 
 * @apiviz.has DimensionModel
 * @apiviz.composedOf COPACNeighborPredicate
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("COPAC: COrrelation PArtition Clustering")
@Description("Partitions a database according to the correlation dimension of its objects and performs " //
    + "a clustering algorithm over the partitions.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, A. Zimek", //
title = "Robust, Complete, and Efficient Correlation Clustering", //
booktitle = "Proc. 7th SIAM International Conference on Data Mining (SDM'07), Minneapolis, MN, 2007", //
url = "http://www.siam.org/proceedings/datamining/2007/dm07_037achtert.pdf")
public class COPAC<V extends NumberVector> extends AbstractAlgorithm<Clustering<DimensionModel>> implements ClusteringAlgorithm<Clustering<DimensionModel>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(COPAC.class);

  /**
   * Settings class.
   */
  COPAC.Settings settings;

  /**
   * Constructor.
   * 
   * @param settings COPAC parameters
   */
  public COPAC(COPAC.Settings settings) {
    super();
    this.settings = settings;
  }

  /**
   * Run the COPAC algorithm.
   * 
   * @param database Database
   * @param relation Vector field relation
   * @return COPAC clustering
   */
  public Clustering<DimensionModel> run(Database database, Relation<V> relation) {
    COPACNeighborPredicate.Instance npred = new COPACNeighborPredicate<V>(settings).instantiate(database, relation);
    CorePredicate.Instance<DBIDs> cpred = new MinPtsCorePredicate(settings.minpts).instantiate(database, TypeUtil.DBIDS);
    Clustering<Model> dclusters = new GeneralizedDBSCAN.Instance<>(npred, cpred, false).run();
    // Re-wrap the detected clusters for COPAC:
    Clustering<DimensionModel> result = new Clustering<>("COPAC clustering", "copac-clustering");
    // Generalized DBSCAN clusterings will be flat.
    for(Hierarchy.Iter<Cluster<Model>> iter = dclusters.iterToplevelClusters(); iter.valid(); iter.advance()) {
      Cluster<Model> clus = iter.get();
      if(clus.size() > 0) {
        int dim = npred.dimensionality(clus.getIDs().iter());
        DimensionModel model = new DimensionModel(dim);
        result.addToplevelCluster(new Cluster<>(clus.getIDs(), model));
      }
    }
    return result;
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
   * Class to wrap the COPAC settings.
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
     * Epsilon value for GDBSCAN.
     */
    public double epsilon;

    /**
     * MinPts parameter.
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
       * Size for the kNN neighborhood used in the PCA step of COPAC.
       */
      public static final OptionID K_ID = new OptionID("copac.knn", "Number of neighbors to use for PCA.");

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
        configEpsilon(config);
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
       * Configure the epsilon radius parameter.
       * 
       * @param config Parameter source
       */
      protected void configEpsilon(Parameterization config) {
        DoubleParameter epsilonP = new DoubleParameter(DBSCAN.Parameterizer.EPSILON_ID) //
        .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
        if(config.grab(epsilonP)) {
          settings.epsilon = epsilonP.doubleValue();
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
     * COPAC settings.
     */
    protected COPAC.Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      settings = config.tryInstantiate(COPAC.Settings.class);
    }

    @Override
    protected COPAC<V> makeInstance() {
      return new COPAC<>(settings);
    }
  }
}