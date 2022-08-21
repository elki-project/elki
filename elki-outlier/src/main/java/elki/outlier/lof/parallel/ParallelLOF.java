/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDs;
import elki.database.ids.KNNList;
import elki.database.query.QueryBuilder;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.outlier.lof.LOF;
import elki.parallel.ParallelExecutor;
import elki.parallel.processor.*;
import elki.parallel.variables.SharedDouble;
import elki.parallel.variables.SharedObject;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Parallel implementation of Local Outlier Factor using processors.
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
 * @has - - - LRDProcessor
 * @has - - - LOFProcessor
 *
 * @param <O> Object type
 */
@Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
    title = "Local Outlier Detection Reconsidered: a Generalized View on Locality with Applications to Spatial, Video, and Network Outlier Detection", //
    booktitle = "Data Mining and Knowledge Discovery 28(1)", //
    url = "https://doi.org/10.1007/s10618-012-0300-z", //
    bibkey = "DBLP:journals/datamine/SchubertZK14")
public class ParallelLOF<O> implements OutlierAlgorithm {
  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Parameter k + 1 for query point
   */
  protected int kplus;

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param k K parameter
   */
  public ParallelLOF(Distance<? super O> distance, int k) {
    super();
    this.distance = distance;
    this.kplus = k + 1;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Run the LOF algorithm in parallel.
   *
   * @param relation Data relation
   * @return LOF result
   */
  public OutlierResult run(Relation<O> relation) {
    DBIDs ids = relation.getDBIDs();
    QueryBuilder<O> qb = new QueryBuilder<>(relation, distance);

    // Phase one: KNN and k-dist
    WritableDoubleDataStore kdists = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    WritableDataStore<KNNList> knns = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, KNNList.class);
    {
      // Compute kNN
      KNNProcessor knnm = new KNNProcessor(kplus, () -> qb.kNNByDBID(kplus));
      SharedObject<KNNList> knnv = new SharedObject<>();
      WriteDataStoreProcessor<KNNList> storek = new WriteDataStoreProcessor<>(knns);
      knnm.connectKNNOutput(knnv);
      storek.connectInput(knnv);
      // Compute k-dist
      KDistanceProcessor kdistm = new KDistanceProcessor(kplus);
      SharedDouble kdistv = new SharedDouble();
      WriteDoubleDataStoreProcessor storem = new WriteDoubleDataStoreProcessor(kdists);
      kdistm.connectKNNInput(knnv);
      kdistm.connectOutput(kdistv);
      storem.connectInput(kdistv);

      ParallelExecutor.run(ids, knnm, storek, kdistm, storem);
    }

    // Phase two: lrd
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    {
      LRDProcessor lrdm = new LRDProcessor(knns, kdists);
      SharedDouble lrdv = new SharedDouble();
      WriteDoubleDataStoreProcessor storelrd = new WriteDoubleDataStoreProcessor(lrds);

      lrdm.connectOutput(lrdv);
      storelrd.connectInput(lrdv);
      ParallelExecutor.run(ids, lrdm, storelrd);
    }
    kdists.destroy(); // No longer needed.
    kdists = null;

    // Phase three: LOF
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

    DoubleRelation scoreres = new MaterializedDoubleRelation("Local Outlier Factor", ids, lofs);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 1.0);
    return new OutlierResult(meta, scoreres);
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
  public static class Par<O> implements Parameterizer {
    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    /**
     * K parameter
     */
    protected int k;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(LOF.Par.K_ID) //
          .grab(config, x -> k = x);
    }

    @Override
    public ParallelLOF<O> make() {
      return new ParallelLOF<>(distance, k);
    }
  }
}
