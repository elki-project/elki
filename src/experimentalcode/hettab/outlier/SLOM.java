package experimentalcode.hettab.outlier;

import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.hettab.util.ConverterUtil;

/**
 * SLOM Algorithm
 * @author Ahmed Hettab
 *
 * @param <O> the type of DatabaseObjects handled by the algorithm
 * @param <D> the type of Distance used to discern objects
 */
  @Title("SLOM: a new measure for local spatial outliers")
  @Description("spatial local outlier measure (SLOM), which captures the local behaviour of datum in their spatial neighbourhood")
  @Reference(authors = "Sanjay Chawla and  Pei Sun", title = "SLOM: a new measure for local spatial outliers", booktitle = "Knowledge and Information Systems 2005", url = "http://rp-www.cs.usyd.edu.au/~chawlarg/papers/KAIS_online.pdf")
 

public class SLOM<O extends MultiRepresentedObject<DoubleVector>, D extends DoubleDistance> extends AbstractDistanceBasedAlgorithm<O, DoubleDistance, OutlierResult> implements OutlierAlgorithm<O, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SLOM.class);

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its SLOM_SCORE. must be an integer greater than 2.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("slom.k", "The number of nearest neighbors of an object to be considered for computing its SLOM_SCORE.");

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k ;

  /**
   * The association id to associate the SLOM_SCORE of an object for the SLOM
   * algorithm.
   */
  public static final AssociationID<Double> SLOM_SCORE = AssociationID.getOrCreateAssociationID("slom", Double.class);

  /**
   * 
   * @param config
   */
  protected SLOM(int k) {
    super(new EmptyParameterization());
    this.k = k ;
  }

  /**
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    //get the spatial attributes
    HashMap<DBID,DoubleVector> spatialAttributes = new HashMap<DBID, DoubleVector>();
    //get the non spatial attributes
    HashMap<DBID,DoubleVector> nonSpatialAttributes = new HashMap<DBID,DoubleVector>();
    
    //
    for(DBID id : database){
      spatialAttributes.put(id, database.get(id).getRepresentation(0));
      nonSpatialAttributes.put(id, database.get(id).getRepresentation(1));
    }
    
    HashMap<DBID,Double> modifiedDistance = new HashMap<DBID,Double>();
    //calculate the modified distance
    for(DBID id : database){
      //get spatial neighborhood
      List<DistanceResultPair<DoubleDistance>> neighboors = getKNNNeighborhood(database, id, spatialAttributes);
      DoubleVector nonSpatial = nonSpatialAttributes.get(id);

     //maxd(o)
      double maxDist = 0 ;
      double sum = 0 ;
      for(DistanceResultPair<DoubleDistance> neighboor : neighboors){
          if(neighboor.getID() != id){
          DoubleVector neighboorNonSpatialAttributes = nonSpatialAttributes.get(neighboor.second);
          double d = EuclideanDistanceFunction.STATIC.doubleDistance(nonSpatial, neighboorNonSpatialAttributes);
           if(d>maxDist){maxDist = d ;}
          sum += d ;
        }
      }
      modifiedDistance.put(id,(sum-maxDist)/(k-2));
    }
    
    // calculate beta and avg and avgPlus
    HashMap<DBID,Double> avgModifiedPlusDistance = new HashMap<DBID,Double>();
    HashMap<DBID,Double> avgModifiedDistance = new HashMap<DBID,Double>();
    HashMap<DBID,Double> betaList = new HashMap<DBID,Double>();
    HashMap<DBID,Double> betaAt4 = new HashMap<DBID, Double>();
    for(DBID id : database.getIDs()){
      double avgPlus = 0 ;
      double avg = 0 ;
      double beta = 0 ;
      List<DistanceResultPair<DoubleDistance>> neighboors = getKNNNeighborhood(database, id, spatialAttributes);
      //compute avg and avg plus
      for( DistanceResultPair<DoubleDistance> dResultPair : neighboors){
          if(dResultPair.second != id){
            avgPlus = avgPlus + modifiedDistance.get(dResultPair.getID()).doubleValue() ;
            avg = avg + modifiedDistance.get(dResultPair.getID()).doubleValue();
          }
          else{
          avgPlus = avgPlus + modifiedDistance.get(dResultPair.getID()).doubleValue() ;
          }
        }
      avgPlus = avgPlus/k ;
      avg = avg/(k-1);
      avgModifiedPlusDistance.put(id, avgPlus);
      avgModifiedDistance.put(id , avg) ;
      
      
      
      for(DistanceResultPair<DoubleDistance> dResultPair : neighboors){
        if(modifiedDistance.get(dResultPair.getID()).doubleValue()>avgPlus){
          beta++ ;
        }
        if(modifiedDistance.get(dResultPair.getID()).doubleValue()<avgPlus){
          beta--;
        }    
      }
      beta = Math.abs(beta);
      beta = (Math.max(beta, 1)/(k-2));
      betaAt4.put(id, beta);
      beta = beta/(1+avg);
      betaList.put(id, beta);
   }
    
  //compute SLOM for each Object
    MinMax<Double> minmax = new MinMax<Double>();
    WritableDataStore<Double> sloms = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : database.getIDs()){
      double slom = betaList.get(id)*modifiedDistance.get(id);
      sloms.put(id, slom);
      minmax.put(slom);
    }
    
    
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("SLOM", "SLOM-outlier", SLOM_SCORE, sloms);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.047);
    return new OutlierResult(scoreMeta, scoreResult);
    
  }
  
  
  /**
   * 
   * @param database
   * @return
   * the knn spatial neighborhood of object
   */
  public List<DistanceResultPair<DoubleDistance>> getKNNNeighborhood(Database<O> database,DBID id ,HashMap<DBID,DoubleVector> spatialAttributes){
    DoubleVector spAttribues = spatialAttributes.get(id);
    //
    KNNHeap<DoubleDistance> knnHeap = new KNNHeap<DoubleDistance>(k);
    for(DBID dbid : database){

    
      double d =   ConverterUtil.distance((spAttribues), spatialAttributes.get(dbid));

      DoubleDistance distance = new DoubleDistance(d);
      DistanceResultPair<DoubleDistance> pair = new DistanceResultPair<DoubleDistance>(distance, dbid);
      knnHeap.add(pair);
    }
    return knnHeap.toSortedArrayList() ;
  }
    
  /**
   * 
   */
  //TODO intersect predicat
  //TODO MBR
 public List<Integer> getIntersectNeighborhood(Database<O> database,DBID id,HashMap<Integer,DoubleVector> polygon){
   return null ;
 }
  /**
   * 
   */
  @Override
  protected Logging getLogger() {
    return logger;
  }
  
  /**SLOM Outlier Score
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return SLOM Outlier Algorithm
   */
  public static <O extends MultiRepresentedObject<DoubleVector>, D extends DoubleDistance> SLOM<O, D> parameterize(Parameterization config) {
    int k = getParameterK(config);
    if(config.hasErrors()) {
      return null;
    }
    return new SLOM<O, D>(k);
  }

  /**
   * Get parameter k
   * 
   * @param config Parameterization
   * @return k value
   */
  protected static int getParameterK(Parameterization config) {
    final IntParameter param = new IntParameter(K_ID, new GreaterConstraint(2));
    if(config.grab(param)) {
      return param.getValue();
    }
    return 3;
  }
}