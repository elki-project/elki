package experimentalcode.hettab.outlier;

import java.util.ArrayList;
import java.util.Collections;

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
 * @author Ahmed Hettab
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
   * Holds the p value
   */
  private static final OptionID Y_ID = OptionID.getOrCreateOptionID("position.y", "the position of y attribut");

  /**
   * Holds the position of y attribute
   */
  private int y;

  /**
   * The association id to associate the TR_SCORE of an object for the TR
   * algorithm.
   */
  public static final AssociationID<Double> TR_SCORE = AssociationID.getOrCreateAssociationID("tr", Double.class);

  /**
   * 
   * @param distanceFunction
   */
  protected TrimmedMeanApproach(double p,int y , NeighborSetPredicate.Factory<DatabaseObject> npred) {
    this.p = p;
    this.y = y ;
    this.npredf = npred;
  }

  /**
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    
    WritableDataStore<Double> trimmedMeans = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> error_tilde = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> mad = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);

    // calculate Trimmed mean   
      final NeighborSetPredicate npred = npredf.instantiate(database);
       for(DBID id : database){
         final DBIDs neighbors = npred.getNeighborDBIDs(id);
         ArrayList<Double> ys = new ArrayList<Double>();
          double tmean = 0  ;
          for(DBID n : neighbors){
            ys.add(database.get(n).doubleValue(y));
          }
          int count = 0 ;
          Collections.sort(ys);
          int j = (int) (p*ys.size()) ;
          for(int i = j ; i<(ys.size()-j) ; i++){
            tmean +=tmean + ys.get(i);
            count ++ ;
          }
          trimmedMeans.put(id, tmean/count);
       }
       
      ArrayList<Double> me = new ArrayList<Double>();
      //calculate error by removing spatial trend and dependence
      for(DBID id : database){
        double i_w = database.get(id).doubleValue(y)-trimmedMeans.get(id);
        DBIDs neighbors = npred.getNeighborDBIDs(id);
        int size = neighbors.size();
         for(DBID n : neighbors){
           i_w -= (1/size)*(database.get(n).doubleValue(y)-trimmedMeans.get(n));
         }
         error_tilde.put(id, i_w);
         me.add(i_w);
      }
      Collections.sort(me);
      
      double m = 0 ;
      if(me.size()%2 == 0){
        int k1 = ((database.size())/2) ;
        m = (me.get(k1) + me.get(k1+1))/2 ;
      }
      if(me.size()%2 !=0){
        int k = database.size()/2;
        m = me.get(k);
      }
      
      double MAD = 0 ;
      ArrayList<Double> median_i = new ArrayList<Double>();
      for(DBID id : database){
        mad.put(id,Math.abs(error_tilde.get(id) - m)); 
        median_i.add(Math.abs(error_tilde.get(id) - m));
      }
      Collections.sort(median_i);
      
      if(median_i.size()%2 == 0){
        int k1 = ((database.size())/2) ;
        MAD = (median_i.get(k1) + median_i.get(k1+1))/2 ;
      }
      if(median_i.size()%2 !=0){
        int k = database.size()/2;
        MAD = median_i.get(k);
      }
      //calaculate median_i(e_i)
      
       MinMax<Double> minmax = new MinMax<Double>();
       WritableDataStore<Double> error = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
       for(DBID id : database) {
         error.put(id,(0.6754*Math.abs(error_tilde.get(id)-mad.get(id))/MAD));
         System.out.println(0.6754*Math.abs(error_tilde.get(id)-mad.get(id))/MAD);
         minmax.put(0.6754*Math.abs(error_tilde.get(id)-mad.get(id))/MAD);
       }
      
      
       AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("OTR", "Trimmedmean-outlier", TR_SCORE, error);
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
    final int y = getParameterY(config);
    if(config.hasErrors()) {
      return null;
    }
    return new TrimmedMeanApproach<V>(p,y, npred);
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
   * @return p parameter
   */
  protected static int getParameterY(Parameterization config) {
    final IntParameter param = new IntParameter(Y_ID);
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
