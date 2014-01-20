package experimentalcode.erich.parallel;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.erich.parallel.mapper.DoubleMinMaxMapper;
import experimentalcode.erich.parallel.mapper.KDistanceMapper;
import experimentalcode.erich.parallel.mapper.KNNMapper;
import experimentalcode.erich.parallel.mapper.WriteDoubleDataStoreMapper;

/**
 * Parallel implementation of KNN Outlier detection using mappers.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class ParallelKNNOutlier<O> extends AbstractDistanceBasedAlgorithm<O, OutlierResult> implements OutlierAlgorithm {
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
  public ParallelKNNOutlier(DistanceFunction<? super O> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ParallelKNNOutlier.class);

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  public OutlierResult run(Database database, Relation<O> relation) {
    DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore store = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    DistanceQuery<O> distq = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O> knnq = database.getKNNQuery(distq, k);

    KNNMapper<O> knnm = new KNNMapper<>(k, knnq);
    SharedObject<KNNList> knnv = new SharedObject<>();
    KDistanceMapper kdistm = new KDistanceMapper(k);
    SharedDouble kdistv = new SharedDouble();
    WriteDoubleDataStoreMapper storem = new WriteDoubleDataStoreMapper(store);
    DoubleMinMaxMapper mmm = new DoubleMinMaxMapper();

    knnm.connectKNNOutput(knnv);
    kdistm.connectKNNInput(knnv);
    kdistm.connectOutput(kdistv);
    storem.connectInput(kdistv);
    mmm.connectInput(kdistv);

    ParallelMapExecutor.run(ids, knnm, kdistm, storem, mmm);

    DoubleMinMax minmax = mmm.getMinMax();
    Relation<Double> scoreres = new MaterializedRelation<>("kNN Outlier Score", "knn-outlier", TypeUtil.DOUBLE, store, ids);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
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
   * @apiviz.exclude
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
      
      IntParameter kP = new IntParameter(KNNOutlier.K_ID);
      if (config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected ParallelKNNOutlier<O> makeInstance() {
      return new ParallelKNNOutlier<>(distanceFunction, k);
    }
  }
}