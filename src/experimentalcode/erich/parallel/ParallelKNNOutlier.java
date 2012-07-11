package experimentalcode.erich.parallel;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
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
import experimentalcode.erich.parallel.mapper.DoubleMinMaxMapper;
import experimentalcode.erich.parallel.mapper.KDoubleDistanceMapper;
import experimentalcode.erich.parallel.mapper.KNNMapper;
import experimentalcode.erich.parallel.mapper.WriteDoubleDataStoreMapper;

/**
 * Parallel implementation of KNN Outlier detection using mappers.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class ParallelKNNOutlier<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm {
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
  public ParallelKNNOutlier(DistanceFunction<? super O, D> distanceFunction, int k) {
    super(distanceFunction);
    this.k = k;
  }

  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(ParallelKNNOutlier.class);

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  public OutlierResult run(Database database, Relation<O> relation) {
    DBIDs ids = relation.getDBIDs();
    WritableDoubleDataStore store = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    DistanceQuery<O, D> distq = database.getDistanceQuery(relation, getDistanceFunction());
    KNNQuery<O, D> knnq = database.getKNNQuery(distq, k);

    KNNMapper<O, D> knnm = new KNNMapper<O, D>(k, knnq);
    SharedObject<KNNResult<D>> knnv = new SharedObject<KNNResult<D>>();
    KDoubleDistanceMapper kdistm = new KDoubleDistanceMapper(k);
    SharedDouble kdistv = new SharedDouble();
    WriteDoubleDataStoreMapper storem = new WriteDoubleDataStoreMapper(store);
    DoubleMinMaxMapper mmm = new DoubleMinMaxMapper();

    knnm.connectKNNOutput(knnv);
    kdistm.connectKNNInput(knnv);
    kdistm.connectDistanceOutput(kdistv);
    storem.connectInput(kdistv);
    mmm.connectInput(kdistv);

    new ParallelMapExecutor().run(ids, knnm, kdistm, storem, mmm);

    DoubleMinMax minmax = mmm.getMinMax();
    Relation<Double> scoreres = new MaterializedRelation<Double>("kNN Outlier Score", "knn-outlier", TypeUtil.DOUBLE, store, ids);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(meta, scoreres);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected ParallelKNNOutlier<O, D> makeInstance() {
      return new ParallelKNNOutlier<O, D>(distanceFunction, 10);
    }

  }
}
