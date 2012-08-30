package experimentalcode.erich.parallel;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.LOF;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.erich.parallel.mapper.DoubleMinMaxMapper;
import experimentalcode.erich.parallel.mapper.KDoubleDistanceMapper;
import experimentalcode.erich.parallel.mapper.KNNMapper;
import experimentalcode.erich.parallel.mapper.LOFMapper;
import experimentalcode.erich.parallel.mapper.LRDMapper;
import experimentalcode.erich.parallel.mapper.WriteDataStoreMapper;
import experimentalcode.erich.parallel.mapper.WriteDoubleDataStoreMapper;

/**
 * Parallel implementation of KNN Outlier detection using mappers.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class ParallelLOF<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
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
  public ParallelLOF(DistanceFunction<? super O, D> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ParallelLOF.class);

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  public OutlierResult run(Database database, Relation<O> relation) {
    DBIDs ids = relation.getDBIDs();
    DistanceQuery<O, D> distq = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O, D> knnq = database.getKNNQuery(distq, k + 1);

    // Phase one: KNN and k-dist
    WritableDoubleDataStore kdists = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    WritableDataStore<KNNResult<D>> knns = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, KNNResult.class);
    {
      // Compute kNN
      KNNMapper<O, D> knnm = new KNNMapper<O, D>(k + 1, knnq);
      SharedObject<KNNResult<D>> knnv = new SharedObject<KNNResult<D>>();
      WriteDataStoreMapper<KNNResult<D>> storek = new WriteDataStoreMapper<KNNResult<D>>(knns);
      knnm.connectKNNOutput(knnv);
      storek.connectInput(knnv);
      // Compute k-dist
      KDoubleDistanceMapper kdistm = new KDoubleDistanceMapper(k + 1);
      SharedDouble kdistv = new SharedDouble();
      WriteDoubleDataStoreMapper storem = new WriteDoubleDataStoreMapper(kdists);
      kdistm.connectKNNInput(knnv);
      kdistm.connectDistanceOutput(kdistv);
      storem.connectInput(kdistv);

      new ParallelMapExecutor().run(ids, knnm, storek, kdistm, storem);
    }

    // Phase two: lrd
    WritableDoubleDataStore lrds = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    {
      LRDMapper<D> lrdm = new LRDMapper<D>(knns, kdists);
      SharedDouble lrdv = new SharedDouble();
      WriteDoubleDataStoreMapper storelrd = new WriteDoubleDataStoreMapper(lrds);

      lrdm.connectOutput(lrdv);
      storelrd.connectInput(lrdv);
      new ParallelMapExecutor().run(ids, lrdm, storelrd);
    }
    kdists.destroy(); // No longer needed.
    kdists = null;

    // Phase three: LOF
    WritableDoubleDataStore lofs = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    DoubleMinMax minmax;
    {
      LOFMapper lofm = new LOFMapper(knns, lrds);
      SharedDouble lofv = new SharedDouble();
      DoubleMinMaxMapper mmm = new DoubleMinMaxMapper();
      WriteDoubleDataStoreMapper storelof = new WriteDoubleDataStoreMapper(lofs);

      lofm.connectOutput(lofv);
      mmm.connectInput(lofv);
      storelof.connectInput(lofv);
      new ParallelMapExecutor().run(ids, lofm, storelof, mmm);

      minmax = mmm.getMinMax();
    }

    Relation<Double> scoreres = new MaterializedRelation<Double>("Local Outlier Factor", "lof-outlier", TypeUtil.DOUBLE, lofs, ids);
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
   * @apiviz.exclude
   * 
   * @param <O> Object type
   * @param <D> Distance type
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    /**
     * K parameter
     */
    int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter kP = new IntParameter(LOF.K_ID);
      if(config.grab(kP)) {
        k = kP.getValue();
      }
    }

    @Override
    protected ParallelLOF<O, D> makeInstance() {
      return new ParallelLOF<O, D>(distanceFunction, k);
    }
  }
}