package experimentalcode.hettab.outlier;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * GLSBackwardSearchAlgorithm provides the GLS-SOD Algorithm, an Algorithm to detect
 * Spatial Outlier
 * 
 * @author Ahmed Hettab
 * 
 * @param <V> DatabaseObject to use
 * @param <D> Distance function to use
 */
public class GLSBackwardSearchAlgorithm<V extends NumberVector<?, ?>, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(GLSBackwardSearchAlgorithm.class);

  /**
   * 
   * Holds the Y value
   */
  private static final OptionID Y_ID = OptionID.getOrCreateOptionID("dim.y", "the dimension of non spatial attribut");

  /**
   * parameter y
   */
  private int y;

  /**
   * 
   * Holds the x1 value
   */
  private static final OptionID X1_ID = OptionID.getOrCreateOptionID("dim.x1", "the dimension of first spatial attribut");

  /**
   * parameter x1
   */
  private int x1;

  /**
   * 
   * Holds the x2 value
   */
  private static final OptionID X2_ID = OptionID.getOrCreateOptionID("dim.x2", "the dimension of second spatial attribut");

  /**
   * parameter x2
   */
  private int x2;

  /**
   * 
   * Holds the alpha value
   */
  private static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("glsbs.alpha", "the alpha parameter");

  /**
   * parameter x2
   */
  private double alpha;

  /**
   * Parameter to specify the spatial distance function to use
   */
  public static final OptionID SPATIAL_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("glsbs.spatialdistancefunction", "The distance function to use for non spatial attributes");

  /**
   * Holds the value of {@link #SPATIAL_DISTANCE_FUNCTION_ID}
   */
  protected PrimitiveDistanceFunction<V, D> spatialDistanceFunction;

  /**
   * Parameter to specify the k nearest neighbor
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("glsbs.k", "k nearest neighbor");

  /**
   * The parameter k
   */
  private int k;
  
  /**
   * Parameter to specify the k nearest neighbor
   */
  public static final OptionID M_ID = OptionID.getOrCreateOptionID("glsbs.m", "the number of outliers to be detected");

  /**
   * The parameter k
   */
  private int m;
  
  /**
   * Parameter to specify the vector of trend parameters
   */
  public static final OptionID BETA_ID = OptionID.getOrCreateOptionID("glsbs.beta","vector of parameters for the trends model");
  
  /**
   * Holds the trend parameters
   */
  private List<Integer> beta ;
  /**
   * The association id to associate the SCORE of an object for the algorithm.
   */
  public static final AssociationID<Double> GLSBS_SCORE = AssociationID.getOrCreateAssociationID("glsbs", Double.class);

  /**
   * Constructor
   * 
   * @param y
   * @param x1
   * @param x2
   * @param alpha
   * @param k
   */
  public GLSBackwardSearchAlgorithm(int x1, int x2, int y,List<Integer> beta , PrimitiveDistanceFunction<V, D> spatialDistanceFunction, int k, double alpha , int m) {
    super();
    this.y = y;
    this.x1 = x1;
    this.x2 = x2;
    this.alpha = alpha;
    this.k = k;
    this.beta = beta ;
    this.spatialDistanceFunction = spatialDistanceFunction;
    this.m = m ;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * 
   * @param database
   * @param relation
   * @return
   */
  public OutlierResult run(Database database, Relation<V> relation) {
    
    HashMap<DBID,SingleObjectBundle> outliers = new HashMap<DBID,SingleObjectBundle>();
    //
    Pair<DBID,Double> candidate = getCandidate(relation,database);
    
    // Note: removing/inserting is rather expensive - can't this be done *virtually* only?
    int outlierNumber = 0 ;
    while(candidate.second > alpha && m > outlierNumber){
      outlierNumber ++ ;
      outliers.put(candidate.first, database.getBundle(candidate.first));
      database.delete(candidate.first);
      candidate = getCandidate(relation , database);
    }
    
    //add removed Objects to database
    Collection<DBID> ids = outliers.keySet();
    System.out.println(ids);
    for(DBID id : ids){
      try {
        database.insert(outliers.get(id));
      }
      catch(UnableToComplyException e) {
        logger.verbose("insert removed Objects failed");
      }
    }
     
    System.out.println(relation.size());
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : relation.getDBIDs()){
      if(outliers.containsKey(id)){
        scores.put(id, 1.0);
      }
      else{
        scores.put(id, 0.0);
      }
    }
    System.out.println(relation.getDBIDs());
    //
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("GLSSODBackward", "GLSSODbackward-outlier",GLSBS_SCORE, scores);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(0.0, 1.0, 0.1, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }
   
  /**
   * 
   * @param database
   * @param outlier
   */
  //TODO test
  private Pair<DBID, Double> getCandidate(Relation<V> relation , Database database) {
    KNNQuery<V, D> knnQuery = QueryUtil.getKNNQuery(relation, spatialDistanceFunction, k); 
    WritableDataStore<Double> error = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);

    // init F,X,Z
    Matrix X = new Matrix(relation.size(), 6);
    Matrix F = new Matrix(relation.size(), relation.size());
    Matrix Y = new Matrix(relation.size(), 1);
    int i = 0;
    for(DBID id : relation.getDBIDs()) {
      int j = 0;

      // remove id from his neighborhood
      List<DistanceResultPair<D>> neighbors = knnQuery.getKNNForDBID(id, k);
      double neighborhodSize;
      HashSet<DBID> neighborhood = new HashSet<DBID>();
      for(DistanceResultPair<D> dpair : neighbors) {
        neighborhood.add(dpair.getDBID());
      }
      neighborhood.remove(id);
      neighborhodSize = neighborhood.size();

      // construct matrix
      double idx1 = relation.get(id).doubleValue(x1);
      double idx2 = relation.get(id).doubleValue(x2);
      double idy = relation.get(id).doubleValue(y);
      X.set(i, 0, beta.get(0));
      X.set(i, 1, beta.get(1)*idx1);
      X.set(i, 2, beta.get(2)*idx2);
      X.set(i, 3, beta.get(3)*idx1 * idx2);
      X.set(i, 4, beta.get(4)*idx1 * idx1);
      X.set(i, 5, beta.get(5)*idx2 * idx2);
      Y.set(i, 0, idy);
      for(DBID n : relation.getDBIDs()) {
        double weight = 0;
        if(n.getIntegerID() == id.getIntegerID()) {
          weight = 1;
        }
        else {
          if(neighborhood.contains(n)) {
            weight = -1 / neighborhodSize;
          }
          else {
            weight = 0;
          }
        }
        F.set(i, j, weight);
        j++;
      }
      i++;
    }
    // Estimate the parameter beta
    Matrix b = X.transpose().times(F.transpose());
    b = b.times(F.times(X));
    b = b.inverse();
    b = b.times(X.transpose());
    b = b.times(F.transpose());
    b = b.times(F);
    b = b.times(Y);
    // Estimate sigma_0 and sigma : sigma_sum_square = sigma_0*sigma_0 +
    // sigma*sigma
    Matrix SIGMA = F.times(X);
    SIGMA = SIGMA.times(b);
    SIGMA = SIGMA.minus(F.times(Y));
    double sigma_sum_square = SIGMA.normF() / (relation.size() - 6 - 1);

    // calculate the absolute values of standart residuals
    Matrix E = new Matrix(relation.size(), 1);
    Matrix T1 = Matrix.unitMatrix(relation.size()).times(1 / Math.sqrt(sigma_sum_square));
    Matrix T2 = F.times(X);
    T2 = T2.times(b);
    T2 = T2.minus(F.times(Y));
    E = T1.times(T2);

    int c = 0;
    Map<Double, DBID> invError = new HashMap<Double, DBID>();
    Vector<Double> errors = new Vector<Double>();
    for(DBID id : relation.getDBIDs()) {
      error.put(id, Math.abs(E.get(c, 0)));
      invError.put(Math.abs(E.get(c, 0)), id);
      errors.add(Math.abs(E.get(c, 0)));
      c++;
    }
    Collections.sort(errors);
    Collections.reverse(errors);

    //
    Pair<DBID, Double> pair;
    double outCandScore = errors.get(0);
    DBID id = invError.get(outCandScore);
    pair = new Pair<DBID, Double>(id, outCandScore);
    return pair;
  }

  /**
   * 
   * @param <V>
   * @param config
   * @return
   */
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> GLSBackwardSearchAlgorithm<V, D> parameterize(Parameterization config) {
    final double alpha = getParameterALPHA(config);
    final int x1 = getParameterX1(config);
    final int x2 = getParameterX2(config);
    final int y = getParameterY(config);
    final List<Integer> beta = getParameterBeta(config);
    final int k = getParameterK(config);
    final int m = getParameterM(config);
    PrimitiveDistanceFunction<V, D> spatialDistanceFunction = getSpatialDistanceFunction(config);
    if(config.hasErrors()) {
      return null;
    }
    return new GLSBackwardSearchAlgorithm<V, D>(x1, x2, y,beta, spatialDistanceFunction, k, alpha,m);
  }

  /**
   * Get the alpha parameter
   * 
   * @param config Parameterization
   * @return alpha parameter
   */
  protected static double getParameterALPHA(Parameterization config) {
    final DoubleParameter param = new DoubleParameter(ALPHA_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0;
  }

  /**
   * Get the x1 parameter
   * 
   * @param config Parameterization
   * @return x1 parameter
   */
  protected static int getParameterX1(Parameterization config) {
    final IntParameter param = new IntParameter(X1_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0;
  }

  /**
   * Get the X2 parameter
   * 
   * @param config Parameterization
   * @return X2 parameter
   */
  protected static int getParameterX2(Parameterization config) {
    final IntParameter param = new IntParameter(X2_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0;
  }

  /**
   * Get the y parameter
   * 
   * @param config Parameterization
   * @return y parameter
   */
  protected static int getParameterY(Parameterization config) {
    final IntParameter param = new IntParameter(Y_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0;
  }

  /**
   * Get the k parameter
   * 
   * @param config Parameterization
   * @return k parameter
   */
  protected static int getParameterK(Parameterization config) {
    final IntParameter param = new IntParameter(K_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0;
  }
  
  /**
   * Get the m parameter
   * 
   * @param config Parameterization
   * @return m parameter
   */
  protected static int getParameterM(Parameterization config) {
    final IntParameter param = new IntParameter(M_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 1;
  }

  /**
   * 
   * @param <F>
   * @param config
   * @return
   */
  protected static <F extends PrimitiveDistanceFunction<?, ?>> F getSpatialDistanceFunction(Parameterization config) {
    final ObjectParameter<F> param = new ObjectParameter<F>(SPATIAL_DISTANCE_FUNCTION_ID, PrimitiveDistanceFunction.class, EuclideanDistanceFunction.class);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }
  
  protected static List<Integer> getParameterBeta (Parameterization config){
    final IntListParameter param = new IntListParameter(BETA_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return null ;
  }

}
