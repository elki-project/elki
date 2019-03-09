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
package elki.algorithm.outlier.subspace;

import elki.algorithm.AbstractAlgorithm;
import elki.algorithm.clustering.subspace.SubspaceClusteringAlgorithm;
import elki.algorithm.outlier.OutlierAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.SubspaceModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.result.Metadata;
import elki.result.outlier.InvertedOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * OutRank: ranking outliers in high dimensional data.
 * <p>
 * Algorithm to score outliers based on a subspace clustering result. This class
 * implements score 1 of the OutRank publication, which is a score based on
 * cluster sizes and cluster dimensionality.
 * <p>
 * Reference:
 * <p>
 * E. Müller, I. Assent, U. Steinhausen, T. Seidl<br>
 * OutRank: ranking outliers in high dimensional data<br>
 * In Proceedings 24th International Conference on Data Engineering (ICDE)
 * Workshop on Ranking in Databases (DBRank)
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Title("OutRank: ranking outliers in high dimensional data")
@Description("Ranking outliers in high dimensional data - score 1")
@Reference(authors = "Emmanuel Müller, Ira Assent, Uwe Steinhausen, Thomas Seidl", //
    title = "OutRank: ranking outliers in high dimensional data", //
    booktitle = "Proc. 24th Int. Conf. on Data Engineering (ICDE) Workshop on Ranking in Databases (DBRank)", //
    url = "https://doi.org/10.1109/ICDEW.2008.4498387", //
    bibkey = "DBLP:conf/icde/MullerASS08")
public class OutRankS1 extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OutRankS1.class);

  /**
   * Clustering algorithm to run.
   */
  protected SubspaceClusteringAlgorithm<? extends SubspaceModel> clusteralg;

  /**
   * Weighting parameter of size vs. dimensionality score.
   */
  double alpha;

  /**
   * Constructor.
   * 
   * @param clusteralg {@link SubspaceClusteringAlgorithm} to use
   * @param alpha Alpha parameter to balance size and dimensionality.
   */
  public OutRankS1(SubspaceClusteringAlgorithm<? extends SubspaceModel> clusteralg, double alpha) {
    super();
    this.clusteralg = clusteralg;
    this.alpha = alpha;
  }

  @Override
  public OutlierResult run(Database database) {
    DBIDs ids = database.getRelation(TypeUtil.ANY).getDBIDs();
    // Run the primary algorithm
    Clustering<? extends SubspaceModel> clustering = clusteralg.run(database);

    WritableDoubleDataStore score = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT);
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      score.putDouble(iter, 0);
    }

    int maxdim = 0, maxsize = 0;
    // Find maximum dimensionality and cluster size
    for(Cluster<? extends SubspaceModel> cluster : clustering.getAllClusters()) {
      maxsize = Math.max(maxsize, cluster.size());
      maxdim = Math.max(maxdim, BitsUtil.cardinality(cluster.getModel().getDimensions()));
    }
    // Iterate over all clusters:
    DoubleMinMax minmax = new DoubleMinMax();
    for(Cluster<? extends SubspaceModel> cluster : clustering.getAllClusters()) {
      double relsize = cluster.size() / (double) maxsize;
      double reldim = BitsUtil.cardinality(cluster.getModel().getDimensions()) / (double) maxdim;
      // Process objects in the cluster
      for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
        double newscore = score.doubleValue(iter) + alpha * relsize + (1 - alpha) * reldim;
        score.putDouble(iter, newscore);
        minmax.put(newscore);
      }
    }

    DoubleRelation scoreResult = new MaterializedDoubleRelation("OutRank-S1", ids, score);
    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0, Double.POSITIVE_INFINITY);
    OutlierResult res = new OutlierResult(meta, scoreResult);
    Metadata.hierarchyOf(res).addChild(clustering);
    return res;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return clusteralg.getInputTypeRestriction();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Clustering algorithm to use.
     */
    public static final OptionID ALGORITHM_ID = new OptionID("outrank.algorithm", "Subspace clustering algorithm to use.");

    /**
     * Alpha parameter for S1
     */
    public static final OptionID ALPHA_ID = new OptionID("outrank.s1.alpha", "Alpha parameter for S1 score.");

    /**
     * Clustering algorithm to run.
     */
    protected SubspaceClusteringAlgorithm<? extends SubspaceModel> algorithm = null;

    /**
     * Alpha parameter to balance parameters
     */
    protected double alpha = 0.25;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<SubspaceClusteringAlgorithm<? extends SubspaceModel>> algP = new ObjectParameter<>(ALGORITHM_ID, SubspaceClusteringAlgorithm.class);
      if(config.grab(algP)) {
        algorithm = algP.instantiateClass(config);
      }
      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 0.25) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }
    }

    @Override
    protected OutRankS1 makeInstance() {
      return new OutRankS1(algorithm, alpha);
    }
  }
}
