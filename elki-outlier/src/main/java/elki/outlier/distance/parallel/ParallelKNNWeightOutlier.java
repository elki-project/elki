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
package elki.outlier.distance.parallel;

import elki.algorithm.AbstractDistanceBasedAlgorithm;
import elki.outlier.OutlierAlgorithm;
import elki.outlier.distance.KNNWeightOutlier;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDs;
import elki.database.ids.KNNList;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.distancefunction.Distance;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.parallel.ParallelExecutor;
import elki.parallel.processor.DoubleMinMaxProcessor;
import elki.parallel.processor.KNNProcessor;
import elki.parallel.processor.WriteDoubleDataStoreProcessor;
import elki.parallel.variables.SharedDouble;
import elki.parallel.variables.SharedObject;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Parallel implementation of KNN Weight Outlier detection.
 * <p>
 * Reference:
 * <p>
 * F. Angiulli, C. Pizzuti<br>
 * Fast Outlier Detection in High Dimensional Spaces<br>
 * Proc. European Conf. Principles of Knowledge Discovery and Data Mining
 * (PKDD'02)
 * <p>
 * This parallelized implementation is based on the easy-to-parallelize
 * generalized pattern discussed in
 * <p>
 * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br>
 * Local Outlier Detection Reconsidered: a Generalized View on Locality with
 * Applications to Spatial, Video, and Network Outlier Detection<br>
 * Data Mining and Knowledge Discovery 28(1)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - - - KNNWeightProcessor
 *
 * @param <O> Object type
 */
@Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
    title = "Local Outlier Detection Reconsidered: a Generalized View on Locality with Applications to Spatial, Video, and Network Outlier Detection", //
    booktitle = "Data Mining and Knowledge Discovery 28(1)", //
    url = "https://doi.org/10.1007/s10618-012-0300-z", //
    bibkey = "DBLP:journals/datamine/SchubertZK14")
public class ParallelKNNWeightOutlier<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
  /**
   * Parameter k
   */
  private int k;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param k K parameter
   */
  public ParallelKNNWeightOutlier(Distance<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ParallelKNNWeightOutlier.class);

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  /**
   * Run the parallel kNN weight outlier detector.
   * 
   * @param database Database to process
   * @param relation Relation to analyze
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<O> relation) {
    DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore store = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    DistanceQuery<O> distq = database.getDistanceQuery(relation, getDistance());
    KNNQuery<O> knnq = database.getKNNQuery(distq, k + 1);

    // Find kNN
    KNNProcessor<O> knnm = new KNNProcessor<>(k + 1, knnq);
    SharedObject<KNNList> knnv = new SharedObject<>();
    knnm.connectKNNOutput(knnv);
    // Extract outlier score
    KNNWeightProcessor kdistm = new KNNWeightProcessor(k + 1);
    SharedDouble kdistv = new SharedDouble();
    kdistm.connectKNNInput(knnv);
    kdistm.connectOutput(kdistv);
    // Store in output result
    WriteDoubleDataStoreProcessor storem = new WriteDoubleDataStoreProcessor(store);
    storem.connectInput(kdistv);
    // And gather statistics for metadata
    DoubleMinMaxProcessor mmm = new DoubleMinMaxProcessor();
    mmm.connectInput(kdistv);

    ParallelExecutor.run(ids, knnm, kdistm, storem, mmm);

    DoubleMinMax minmax = mmm.getMinMax();
    DoubleRelation scoreres = new MaterializedDoubleRelation("kNN weight Outlier Score", ids, store);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0., Double.POSITIVE_INFINITY, 0.);
    return new OutlierResult(meta, scoreres);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * K parameter
     */
    int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter kP = new IntParameter(KNNWeightOutlier.Parameterizer.K_ID);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected ParallelKNNWeightOutlier<O> makeInstance() {
      return new ParallelKNNWeightOutlier<>(distanceFunction, k);
    }
  }
}
