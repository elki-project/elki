package experimentalcode.hettab.outlier;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.math.DimensionMismatchException;
import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.stat.descriptive.MultivariateSummaryStatistics;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate.Factory;

/**
 * 
 * @author Ahmed Hettab
 *
 * @param <V> 
 */
public class MeanMultipleAttributes<V extends NumberVector<?, ?>> extends AbstractAlgorithm<V, OutlierResult> implements OutlierAlgorithm<V, OutlierResult> {
  /**
   * logger
   */
  public static final Logging logger = Logging.getLogger(MeanMultipleAttributes.class);

  /**
   * Parameter to specify the neighborhood predicate to use.
   */
  public static final OptionID NEIGHBORHOOD_ID = OptionID.getOrCreateOptionID("neighborhood", "The neighborhood predicate to use.");

  /**
   * Parameter to specify the dimensions for non spatial attributes to use
   */
  public static final OptionID DIMS_ID = OptionID.getOrCreateOptionID("mma", "dimensions for non spatial atributtes");

  /**
   * Our predicate to obtain the neighbors
   */
  NeighborSetPredicate.Factory<DatabaseObject> npredf = null;

  /**
   * dims Parameter
   */
  private List<Integer> dims;

  /**
   * The association id to associate the SCORE of an object for the algorithm.
   */
  public static final AssociationID<Double> MMA_SCORE = AssociationID.getOrCreateAssociationID("mma-outlier-score", Double.class);

  /**
   * Constructor
   * 
   * @param npredf
   * @param dims
   */
  public MeanMultipleAttributes(Factory<DatabaseObject> npredf, List<Integer> dims) {
    super();
    this.npredf = npredf;
    this.dims = dims;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {

    final NeighborSetPredicate npred = npredf.instantiate(database);
    HashMap<Integer, WritableDataStore<Double>> mapF = new HashMap<Integer, WritableDataStore<Double>>();
    HashMap<Integer, WritableDataStore<Double>> mapG = new HashMap<Integer, WritableDataStore<Double>>();
    MultivariateSummaryStatistics mss = new MultivariateSummaryStatistics(dims.size(), true);
    RealMatrix h_u = MatrixUtils.createRealMatrix(dims.size(), database.size());
    HashMap<Integer, Double> means = new HashMap<Integer, Double>();

    // init

    for(Integer dim : dims) {
      WritableDataStore<Double> store = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, double.class);
      WritableDataStore<Double> store2 = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, double.class);
      mapF.put(dim, store);
      mapG.put(dim, store2);
    }

    // for each dimension
    for(Integer dim : dims) {
      // standardize attribute
      MeanVariance mv = new MeanVariance();
      for(DBID id : database) {
        mv.put(database.get(id).doubleValue(dim));
      }
      for(DBID id : database) {
        double value = database.get(id).doubleValue(dim);
        mapF.get(dim).put(id, (value - mv.getMean()) / mv.getSampleVariance());
      }

      // compute gi and hi and means
      for(DBID id : database) {
        double value = mapF.get(dim).get(id);
        double g = 0;

        DBIDs neighbors = npred.getNeighborDBIDs(id);
        double size = neighbors.size();
        for(DBID n : neighbors) {
          g += mapF.get(dim).get(n);
        }
        g = g / size;
        mapG.get(dim).put(id, value - g);
        mv.put(value - g);
      }
      means.put(dim, mv.getMean());
    }

    //compute variance-covariance Matrix
    int i = 0;
    for(DBID id : database) {
      double[] data = new double[dims.size()];
      int j = 0;
      for(Integer dim : dims) {
        data[j] = mapG.get(dim).get(id);
        h_u.setEntry(j, i, data[j] - means.get(dim));
        j++;
      }
      try {
        mss.addValue(data);
      }
      catch(DimensionMismatchException e) {
        logger.error("Dimension Mismatch");
      }
      i++;
    }
    
    //compute score
    Matrix varCoVar = new Matrix(mss.getCovariance().getData());
    Matrix inv = varCoVar.inverse();
    i = 0;
    MinMax<Double> minmax = new MinMax<Double>();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : database) {
      double[] data = new double[dims.size()];
      for(Integer dim : dims) {
        data[i] = mapG.get(dim).get(id) - means.get(dim);
      }
      Matrix vT = new Matrix(data, 1);
      Matrix v = vT.transpose();
      Matrix s = vT.times(inv.times(v));
      minmax.put(s.get(0, 0));
      scores.put(id, s.get(0, 0));
    }

    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("MOF", "Trimmedmean-outlier", MMA_SCORE, scores);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * 
   * @author Ahmed Hettab
   * 
   * @param <V>
   * 
   */
  public static <V extends NumberVector<?, ?>> MeanMultipleAttributes<V> parameterize(Parameterization config) {
    final NeighborSetPredicate.Factory<DatabaseObject> npredf = getNeighborPredicate(config);
    final List<Integer> dims = getDims(config);
    if(config.hasErrors()) {
      return null;
    }
    return new MeanMultipleAttributes<V>(npredf, dims);
  }

  /**
   * 
   * @param config
   * @return
   */
  public static NeighborSetPredicate.Factory<DatabaseObject> getNeighborPredicate(Parameterization config) {
    final ObjectParameter<NeighborSetPredicate.Factory<DatabaseObject>> param = new ObjectParameter<NeighborSetPredicate.Factory<DatabaseObject>>(NEIGHBORHOOD_ID, NeighborSetPredicate.Factory.class, true);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }

  /**
   * 
   */
  public static List<Integer> getDims(Parameterization config) {
    final IntListParameter param = new IntListParameter(DIMS_ID, false);
    if(config.grab(param)) {
      return param.getValue();
    }
    return null;
  }

}
