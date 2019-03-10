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
package elki.outlier.lof.parallel;

import elki.outlier.OutlierAlgorithm;
import elki.outlier.lof.LOF;
import elki.AbstractDistanceBasedAlgorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDs;
import elki.database.ids.KNNList;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNQuery;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.parallel.ParallelExecutor;
import elki.parallel.processor.DoubleMinMaxProcessor;
import elki.parallel.processor.KNNProcessor;
import elki.parallel.processor.WriteDataStoreProcessor;
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
 * Parallel implementation of Simplified-LOF Outlier detection using processors.
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
 * @has - - - SimplifiedLRDProcessor
 * @has - - - LOFProcessor
 *
 * @param <O> Object type
 */
@Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
    title = "Local Outlier Detection Reconsidered: a Generalized View on Locality with Applications to Spatial, Video, and Network Outlier Detection", //
    booktitle = "Data Mining and Knowledge Discovery 28(1)", //
    url = "https://doi.org/10.1007/s10618-012-0300-z", //
    bibkey = "DBLP:journals/datamine/SchubertZK14")
public class ParallelSimplifiedLOF<O> extends AbstractDistanceBasedAlgorithm<Distance<? super O>, OutlierResult> implements OutlierAlgorithm {
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
  public ParallelSimplifiedLOF(Distance<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ParallelSimplifiedLOF.class);

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistance().getInputTypeRestriction());
  }

  public OutlierResult run(Database database, Relation<O> relation) {
    DBIDs ids = relation.getDBIDs();
    DistanceQuery<O> distq = database.getDistanceQuery(relation, getDistance());
    KNNQuery<O> knnq = database.getKNNQuery(distq, k + 1);

    // Phase one: KNN and k-dist
    WritableDataStore<KNNList> knns = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, KNNList.class);
    {
      // Compute kNN
      KNNProcessor<O> knnm = new KNNProcessor<>(k + 1, knnq);
      SharedObject<KNNList> knnv = new SharedObject<>();
      WriteDataStoreProcessor<KNNList> storek = new WriteDataStoreProcessor<>(knns);
      knnm.connectKNNOutput(knnv);
      storek.connectInput(knnv);

      ParallelExecutor.run(ids, knnm, storek);
    }

    // Phase two: simplified-lrd
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    {
      SimplifiedLRDProcessor lrdm = new SimplifiedLRDProcessor(knns);
      SharedDouble lrdv = new SharedDouble();
      WriteDoubleDataStoreProcessor storelrd = new WriteDoubleDataStoreProcessor(lrds);

      lrdm.connectOutput(lrdv);
      storelrd.connectInput(lrdv);
      ParallelExecutor.run(ids, lrdm, storelrd);
    }

    // Phase three: Simplified-LOF
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    DoubleMinMax minmax;
    {
      LOFProcessor lofm = new LOFProcessor(knns, lrds, true);
      SharedDouble lofv = new SharedDouble();
      DoubleMinMaxProcessor mmm = new DoubleMinMaxProcessor();
      WriteDoubleDataStoreProcessor storelof = new WriteDoubleDataStoreProcessor(lofs);

      lofm.connectOutput(lofv);
      mmm.connectInput(lofv);
      storelof.connectInput(lofv);
      ParallelExecutor.run(ids, lofm, storelof, mmm);

      minmax = mmm.getMinMax();
    }

    DoubleRelation scoreres = new MaterializedDoubleRelation("Simplified Local Outlier Factor", ids, lofs);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
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
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<Distance<? super O>> {
    /**
     * K parameter
     */
    int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter kP = new IntParameter(LOF.Parameterizer.K_ID);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected ParallelSimplifiedLOF<O> makeInstance() {
      return new ParallelSimplifiedLOF<>(distanceFunction, k);
    }
  }
}
