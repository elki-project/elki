package experimentalcode.hettab.outlier;

import java.util.List;

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
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
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
public class MedianMultipleAttributes<V extends NumberVector<?, ?>> extends AbstractAlgorithm<V, OutlierResult> implements OutlierAlgorithm<V, OutlierResult> {
  /**
   * logger
   */
  public static final Logging logger = Logging.getLogger(MedianMultipleAttributes.class);

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
  public MedianMultipleAttributes(Factory<DatabaseObject> npredf, List<Integer> dims) {
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
    Matrix hMatrix = new Matrix(dims.size(),database.size());
    Matrix hMeansMatrix = new Matrix(dims.size(),1);
    double dbSize = database.size();
     
    int i = 0 ;
    for(Integer dim : dims){
        int j = 0 ;
        //h mean for each dim
        double hMeans = 0 ;
         for(DBID id : database){
            // f value
            double f = database.get(id).doubleValue(dim);
            DBIDs neighbors = npred.getNeighborDBIDs(id);
            int nSize = neighbors.size() ;
            //g value 
            double g[] = new double[nSize] ;
            Median m = new Median();
            int k = 0 ;
            for(DBID n : neighbors){
                 g[k] = database.get(n).doubleValue(dim);
                 k++;
            }
            double gm = m.evaluate(g) ;
            double h = f-gm;          
            //add to h Matrix
            hMatrix.set(i, j, h);
            hMeans += h ;
            j++ ;
         }
         
         hMeans = hMeans/dbSize;
         //add mean to h means hMeansMatrix
         hMeansMatrix.set(i,0 , hMeans);
         i++;
    }
    
    Matrix sigma = DatabaseUtil.covarianceMatrix(hMatrix);
    Matrix invSigma = sigma.inverse() ;
       
    MinMax<Double> minmax = new MinMax<Double>();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    i = 0 ;
    for(DBID id : database) {
      Matrix h_i = hMatrix.getColumn(i).minus(hMeansMatrix) ;
      Matrix h_iT = h_i.transpose();
      Matrix m = h_iT.times(invSigma);
      Matrix sM = m.times(h_i);
      double score = sM.get(0, 0);
      minmax.put(score);
      scores.put(id, score);
      i++;
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
  public static <V extends NumberVector<?, ?>> MedianMultipleAttributes<V> parameterize(Parameterization config) {
    final NeighborSetPredicate.Factory<DatabaseObject> npredf = getNeighborPredicate(config);
    final List<Integer> dims = getDims(config);
    if(config.hasErrors()) {
      return null;
    }
    return new MedianMultipleAttributes<V>(npredf, dims);
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
 

