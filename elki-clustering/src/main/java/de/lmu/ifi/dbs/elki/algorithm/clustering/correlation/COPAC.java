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
package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

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
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCARunner;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.It;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * COPAC is an algorithm to partition a database according to the correlation
 * dimension of its objects and to then perform an arbitrary clustering
 * algorithm over the partitions.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Christian Böhm, Hans-Peter Kriegel, Peer Kröger, Arthur
 * Zimek<br>
 * Robust, Complete, and Efficient Correlation Clustering<br>
 * Proc. 7th SIAM Int. Conf. on Data Mining (SDM'07)
 * 
 * @author Arthur Zimek
 * @since 0.1
 * 
 * @has - - - DimensionModel
 * @composed - - - COPACNeighborPredicate
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("COPAC: COrrelation PArtition Clustering")
@Description("Partitions a database according to the correlation dimension of its objects and performs " //
    + "a clustering algorithm over the partitions.")
@Reference(authors = "Elke Achtert, Christian Böhm, Hans-Peter Kriegel, Peer Kröger, Arthur Zimek", //
    title = "Robust, Complete, and Efficient Correlation Clustering", //
    booktitle = "Proc. 7th SIAM Int. Conf. on Data Mining (SDM'07)", //
    url = "https://doi.org/10.1137/1.9781611972771.37", //
    bibkey = "DBLP:conf/sdm/AchtertBKKZ07")
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
    CorePredicate.Instance<DBIDs> cpred = new MinPtsCorePredicate(settings.minpts).instantiate(database);
    Clustering<Model> dclusters = new GeneralizedDBSCAN.Instance<>(npred, cpred, false).run();
    // Re-wrap the detected clusters for COPAC:
    Clustering<DimensionModel> result = new Clustering<>("COPAC clustering", "copac-clustering");
    // Generalized DBSCAN clusterings will be flat.
    for(It<Cluster<Model>> iter = dclusters.iterToplevelClusters(); iter.valid(); iter.advance()) {
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
     * Eigenpair filter.
     */
    public EigenPairFilter filter;

    /**
     * Epsilon value for GDBSCAN.
     */
    public double epsilon;

    /**
     * MinPts parameter.
     */
    public int minpts;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Size for the kNN neighborhood used in the PCA step of COPAC.
     */
    public static final OptionID K_ID = new OptionID("copac.knn", "Number of neighbors to use for PCA.");

    /**
     * COPAC settings.
     */
    protected COPAC.Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      settings = new Settings();
      IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(kP)) {
        settings.k = kP.intValue();
      }
      ObjectParameter<PCARunner> pcaP = new ObjectParameter<>(PCARunner.Parameterizer.PCARUNNER_ID, PCARunner.class, PCARunner.class);
      if(config.grab(pcaP)) {
        settings.pca = pcaP.instantiateClass(config);
      }
      ObjectParameter<EigenPairFilter> filterP = new ObjectParameter<>(EigenPairFilter.PCA_EIGENPAIR_FILTER, EigenPairFilter.class, PercentageEigenPairFilter.class);
      if(config.grab(filterP)) {
        settings.filter = filterP.instantiateClass(config);
      }
      DoubleParameter epsilonP = new DoubleParameter(DBSCAN.Parameterizer.EPSILON_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(epsilonP)) {
        settings.epsilon = epsilonP.doubleValue();
      }
      IntParameter minptsP = new IntParameter(DBSCAN.Parameterizer.MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minptsP)) {
        settings.minpts = minptsP.intValue();
      }
    }

    @Override
    protected COPAC<V> makeInstance() {
      return new COPAC<>(settings);
    }
  }
}
