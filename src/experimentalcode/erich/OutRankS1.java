package experimentalcode.erich;

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

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.SubspaceClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * OutRank: ranking outliers in high dimensional data.
 * 
 * Algorithm to score outliers based on a subspace clustering result.
 * 
 * Reference:
 * <p>
 * Emmanuel Müller, Ira Assent, Uwe Steinhausen, Thomas Seidl<br />
 * OutRank: ranking outliers in high dimensional data<br />
 * In Proceedings 24th International Conference on Data Engineering (ICDE)
 * Workshop on Ranking in Databases (DBRank), Cancun, Mexico
 * </p>
 * 
 * @author Erich Schubert
 */
@Title("OutRank: ranking outliers in high dimensional data")
@Description("Ranking outliers in high dimensional data")
@Reference(authors = "Emmanuel Müller, Ira Assent, Uwe Steinhausen, Thomas Seidl", title = "OutRank: ranking outliers in high dimensional data", booktitle = "Proc. 24th Int. Conf. on Data Engineering (ICDE) Workshop on Ranking in Databases (DBRank), Cancun, Mexico", url = "http://dx.doi.org/10.1109/ICDEW.2008.4498387")
public class OutRankS1 extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OutRankS1.class);

  /**
   * Clustering algorithm to run.
   */
  protected SubspaceClusteringAlgorithm<? extends SubspaceModel<?>> clusteralg;

  /**
   * Weighting parameter of size vs. dimensionality score.
   */
  double alpha;

  /**
   * Constructor.
   * 
   * @param clusteralg Clustering algorithm to use (must implement
   *        {@link SubspaceClusteringAlgorithm}!)
   * @param alpha Alpha parameter to balance size and dimensionality.
   */
  public OutRankS1(SubspaceClusteringAlgorithm<? extends SubspaceModel<?>> clusteralg, double alpha) {
    super();
    this.clusteralg = clusteralg;
    this.alpha = alpha;
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    DBIDs ids = database.getRelation(TypeUtil.DBID).getDBIDs();
    // Run the primary algorithm
    Clustering<? extends SubspaceModel<?>> clustering = clusteralg.run(database);

    WritableDoubleDataStore score = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT);
    for(DBID id : ids) {
      score.putDouble(id, 0);
    }

    int maxdim = 0, maxsize = 0;
    // Find maximum dimensionality and cluster size
    for(Cluster<? extends SubspaceModel<?>> cluster : clustering.getAllClusters()) {
      maxsize = Math.max(maxsize, cluster.size());
      maxdim = Math.max(maxdim, cluster.getModel().getDimensions().cardinality());
    }
    // Iterate over all clusters:
    DoubleMinMax minmax = new DoubleMinMax();
    for(Cluster<? extends SubspaceModel<?>> cluster : clustering.getAllClusters()) {
      double relsize = cluster.size() / (double) maxsize;
      double reldim = cluster.getModel().getDimensions().cardinality() / (double) maxdim;
      // Process objects in the cluster
      for(DBID id : cluster.getIDs()) {
        double prev = score.doubleValue(id);
        double newscore = prev + alpha * relsize + (1 - alpha) * reldim;
        score.putDouble(id, newscore);
        minmax.put(newscore);
      }
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("OutRank-S1", "OUTRANK_S1", TypeUtil.DOUBLE, score, ids);
    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(meta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return clusteralg.getInputTypeRestriction();
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
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Clustering algorithm to use.
     */
    public static final OptionID ALGORITHM_ID = OptionID.getOrCreateOptionID("outrank.algorithm", "Subspace clustering algorithm to use.");

    /**
     * Alpha parameter for S1
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("outrank.s1.alpha", "Alpha parameter for S1 score.");

    /**
     * Clustering algorithm to run.
     */
    protected SubspaceClusteringAlgorithm<? extends SubspaceModel<?>> algorithm = null;

    /**
     * Alpha parameter to balance parameters
     */
    protected double alpha = 0.25;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<SubspaceClusteringAlgorithm<? extends SubspaceModel<?>>> algP = new ObjectParameter<SubspaceClusteringAlgorithm<? extends SubspaceModel<?>>>(ALGORITHM_ID, SubspaceClusteringAlgorithm.class);
      if(config.grab(algP)) {
        algorithm = algP.instantiateClass(config);
      }
      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, new GreaterConstraint(0), 0.25);
      if(config.grab(alphaP)) {
        alpha = alphaP.getValue();
      }
    }

    @Override
    protected OutRankS1 makeInstance() {
      return new OutRankS1(algorithm, alpha);
    }
  }
}