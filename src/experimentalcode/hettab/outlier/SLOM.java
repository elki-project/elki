package experimentalcode.hettab.outlier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.ABOD;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.erich.newdblayer.DBUtil;

/**
 * 
 * @author hettab
 * 
 * @param <O>
 * @param <D>
 */

public class SLOM<V extends NumberVector<V, ?>> extends AbstractDistanceBasedAlgorithm<V, DoubleDistance, OutlierResult> implements OutlierAlgorithm<V, OutlierResult> {

  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SLOM.class);

  /**
   * The association id to associate the KNNO_KNNDISTANCE of an object for the
   * KNN outlier detection algorithm.
   */
  public static final AssociationID<Double> KNNO_KNNDISTANCE = AssociationID.getOrCreateAssociationID("knno_knndistance", Double.class);

  /**
   * Parameter to specify the k nearest neighbor
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("knno.k", "k nearest neighbor");

  /**
   * Parameter to specify the position of non spatial attribute
   */
  public static final OptionID NONSPATIAL_ID = OptionID.getOrCreateOptionID("nonspatial.a", "position of non spatial attribut");

  /**
   * Parameter to specify the position of spatial attribute
   */
  public static final OptionID SPATIAL_ID = OptionID.getOrCreateOptionID("spatial.a", "position of non spatial attribut");

  /**
   * The parameter k
   */
  private int k;

  /**
   * position of spatial attribute
   */
  private List<Integer> spatialDim;

  /**
   * position of non spatial attribute
   */
  private List<Integer> nonSpatialDim;

  /**
   * The association id to associate the SLOM_SCORE of an object for the INFLO
   * algorithm.
   */
  public static final AssociationID<Double> SLOM_SCORE = AssociationID.getOrCreateAssociationID("SLOM", Double.class);

  /**
   * 
   * @param distanceFunction
   * @param k
   * @param nonSpatialDim
   * @param spatialDim
   */
  protected SLOM(DistanceFunction<V, DoubleDistance> distanceFunction, int k, List<Integer> nonSpatialDim, List<Integer> spatialDim) {
    super(distanceFunction);
    this.k = k;
    this.nonSpatialDim = nonSpatialDim;
    this.spatialDim = spatialDim;
  }

  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
     
    for(DBID id : database){
      
    }
    
    return null ;
  }

  /**
   * 
   * @param database
   * @param id
   * @param dim
   * @return
   */
  public ArrayList<DistanceResultPair<DoubleDistance>> getProjectedKnnQuery(Database<V> database, DBID id, List<Integer> dim) {
    
    Heap<DistanceResultPair<DoubleDistance>> heap = new Heap<DistanceResultPair<DoubleDistance>>(k);
    
    Vector<Double> projectedDimV1 = new Vector<Double>(spatialDim.size());
    V v1 = database.get(id);
    //
    for(int i = 0 ; i < spatialDim.size() ; i++){
        projectedDimV1.add(v1.getValue(spatialDim.get(i)).doubleValue());
    }
    //
    for(DBID dbid : database){
      V v2 = database.get(dbid);
      Vector<Double> projectedDimV2 = new Vector<Double>(spatialDim.size());
      for(int i = 0 ; i < spatialDim.size() ; i++){
        projectedDimV2.add(v2.getValue(spatialDim.get(i)).doubleValue());
     }
      DistanceResultPair<DoubleDistance> dPair = new DistanceResultPair<DoubleDistance>(EuclideanDistanceFunction.STATIC.distance(v1, v2), dbid); 
      heap.add(dPair);
    }
   return heap.toSortedArrayList();        
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <V extends NumberVector<V, ?>> SLOM<V> parameterize(Parameterization config) {
    int k = getParameterK(config);
    List<Integer> nonSpatialDim = getParameterNonSpatialDim(config);
    List<Integer> spatialDim = getParameterNonSpatialDim(config);
    DistanceFunction<V, DoubleDistance> distanceFunction = getParameterDistanceFunction(config);
    if(config.hasErrors()) {
      return null;
    }
    return new SLOM<V>(distanceFunction, k, nonSpatialDim, spatialDim);
  }

  /**
   * Get the k Parameter for the knn query
   * 
   * @param config Parameterization
   * @return k parameter
   */
  protected static int getParameterK(Parameterization config) {
    final IntParameter param = new IntParameter(K_ID, new GreaterConstraint(1));
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }

  /**
   * 
   * @param config
   * @return
   */
  protected static List<Integer> getParameterNonSpatialDim(Parameterization config) {
    final IntListParameter param = new IntListParameter(NONSPATIAL_ID, false);
    if(config.grab(param)) {
      return param.getValue();
    }
    return null;
  }

  /**
   * 
   * @param config
   * @return
   */
  protected static List<Integer> getParameterSpatialDim(Parameterization config) {
    final IntListParameter param = new IntListParameter(SPATIAL_ID, false);
    if(config.grab(param)) {
      return param.getValue();
    }
    return null;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

}
