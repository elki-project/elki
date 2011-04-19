package experimentalcode.hettab.outlier;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.rank.Median;

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
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;

/**
 * 
 * @author Ahmed Hettab A Trimmed Mean Approach to finding Spatial Outliers
 * 
 * @param <V>
 */
public class TrimmedMeanApproach<V extends NumberVector<?, ?>> extends AbstractAlgorithm<V, OutlierResult> implements OutlierAlgorithm<V, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(TrimmedMeanApproach.class);

  /**
   * Parameter to specify the neighborhood predicate to use.
   */
  public static final OptionID NEIGHBORHOOD_ID = OptionID.getOrCreateOptionID("neighborhood", "The neighborhood predicate to use in comparison step.");

  /**
   * Our predicate to obtain the neighbors
   */
  NeighborSetPredicate.Factory<DatabaseObject> npredf = null;

  /**
   * 
   * Holds the p value
   */
  private static final OptionID P_ID = OptionID.getOrCreateOptionID("trimmed.p", "the highest and lowest p precent");

  /**
   * 
   */
  private double p;

  /**
   * 
   * Holds the Z value
   */
  private static final OptionID Z_ID = OptionID.getOrCreateOptionID("position.y", "the position of y attribut");

  /**
   * Holds the position of z attribute
   */
  private int z;

  /**
   * The association id to associate the TR_SCORE of an object for the TR
   * algorithm.
   */
  public static final AssociationID<Double> TR_SCORE = AssociationID.getOrCreateAssociationID("tr", Double.class);

  /**
   * 
   * @param distanceFunction
   */
  protected TrimmedMeanApproach(double p, int z, NeighborSetPredicate.Factory<DatabaseObject> npredf) {
    this.p = p;
    this.z = z;
    this.npredf = npredf;
  }

  /**
   * 
   */
  @Override
  protected OutlierResult runInTime(Database database) throws IllegalStateException {

    WritableDataStore<Double> tMeans = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> error = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);

    final NeighborSetPredicate npred = npredf.instantiate(database);

    // calculate Trimmed mean
    Matrix tMeanMatrix = new Matrix(database.size(), 1);
    Matrix Y = new Matrix(database.size(), 1);
    for(DBID id : database) {
      final DBIDs neighbors = npred.getNeighborDBIDs(id);
      int size = neighbors.size();
      double[] zi = new double[size];
      ArrayList<Double> ziList = new ArrayList<Double>();
      int i = 0;
      double tmean = 0;
      for(DBID n : neighbors) {
        ziList.add(database.get(n).doubleValue(z));
        i++;
      }
      Collections.sort(ziList);
      for(int j = 0; j < size; j++) {
        zi[j] = ziList.get(j);
      }
      tmean = StatUtils.mean(zi, (int) (p * size), (int) (size - 2 * p * size));
      tMeans.put(id, tmean);
      tMeanMatrix.set(i, 0, tmean);
      Y.set(i, 0, database.get(id).doubleValue(z));
    }

    // Estimate error by removing spatial trend and dependence
    Matrix E = new Matrix(1, database.size());
    Matrix I = Matrix.unitMatrix(database.size());
    Matrix W = new Matrix(database.size(), database.size());
    int k = 0;
    for(DBID id : database) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      int l = 0;
      for(DBID n : database) {
        if(neighbors.contains(n) && n.getIntegerID() != id.getIntegerID()) {
          W.set(k, l, (double) 1 / neighbors.size());
        }
        if(n.getIntegerID() == id.getIntegerID()) {
          W.set(k, l, 1.0);
        }
        else {
          W.set(k, l, 0.0);
        }
        l++;
      }
      k++;
    }
    Matrix A = I.minus(W);
    Matrix B = Y.minus(tMeanMatrix);
    System.out.println(A.dimensionInfo());
    System.out.println(B.dimensionInfo());
    E = A.times(B);

    // calculate median_i
    int m = 0;
    double[] ei = new double[database.size()];
    Median median = new Median();
    for(DBID id : database) {
      error.put(id, E.get(m, 0));
      ei[m] = E.get(m, 0);
      m++;
    }
    double median_i = median.evaluate(ei);

    // calculate MAD
    double MAD;
    m = 0;
    double[] temp = new double[database.size()];
    for(DBID id : database) {
      temp[m] = Math.abs(error.get(id) - median_i);
      m++;
    }
    MAD = median.evaluate(temp);

    // calculate score
    MinMax<Double> minmax = new MinMax<Double>();
    m = 0;
    for(DBID id : database) {
      double score = temp[m] * 0.6745 / MAD;
      scores.put(id, score);
      minmax.put(score);
    }
    //
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("OTR", "Trimmedmean-outlier", TR_SCORE, scores);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);

  }

  /**
   * 
   * @param <V>
   * @param config
   * @return
   */
  public static <V extends NumberVector<V, ?>> TrimmedMeanApproach<V> parameterize(Parameterization config) {
    final NeighborSetPredicate.Factory<DatabaseObject> npred = getNeighborPredicate(config);
    final double p = getParameterP(config);
    final int z = getParameterZ(config);
    if(config.hasErrors()) {
      return null;
    }
    return new TrimmedMeanApproach<V>(p, z, npred);
  }

  /**
   * Get the p parameter
   * 
   * @param config Parameterization
   * @return p parameter
   */
  protected static double getParameterP(Parameterization config) {
    final DoubleParameter param = new DoubleParameter(P_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0;
  }

  /**
   * Get the y parameter
   * 
   * @param config Parameterization
   * @return z parameter
   */
  protected static int getParameterZ(Parameterization config) {
    final IntParameter param = new IntParameter(Z_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0;
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
  @Override
  protected Logging getLogger() {
    return logger;
  }

}
