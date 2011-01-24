package experimentalcode.hettab.outlier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.ExternalObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * SLOM Algorithm
 * 
 * @author Ahmed Hettab
 * 
 * @param <O> the type of DatabaseObjects handled by the algorithm
 * @param <D> the type of Distance used to discern objects
 */
@Title("SLOM: a new measure for local spatial outliers")
@Description("spatial local outlier measure (SLOM), which captures the local behaviour of datum in their spatial neighbourhood")
@Reference(authors = "Sanjay Chawla and  Pei Sun", title = "SLOM: a new measure for local spatial outliers", booktitle = "Knowledge and Information Systems 2005", url = "http://rp-www.cs.usyd.edu.au/~chawlarg/papers/KAIS_online.pdf")
public class SLOM<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm<O, OutlierResult> {
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
  private int k;

  /**
   * Parameter to specify the neighborhood distance function to use ;
   */
  public static final OptionID SPATIAL_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("slom.spatialdistancefunction", "The distance function to use for spatial attributes");

  /**
   * Parameter to specify the non spatial distance function to use
   */
  public static final OptionID NON_SPATIAL_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("slom.nonspatialdistancefunction", "The distance function to use for non spatial attributes");

  /**
   * Holds the value of {@link #SPATIAL_DISTANCE_FUNCTION_ID}
   */
  protected PrimitiveDistanceFunction<O, D> neighborhoodDistanceFunction;

  /**
   * Holds the value of {@link #NON_SPATIAL_DISTANCE_FUNCTION_ID}
   */
  protected PrimitiveDistanceFunction<O, D> nonSpatialDistanceFunction;

  /**
   * The association id to associate the SLOM_SCORE of an object for the SLOM
   * algorithm.
   */
  public static final AssociationID<Double> SLOM_SCORE = AssociationID.getOrCreateAssociationID("slom", Double.class);
  
  /**
   * 
   */
  public HashMap<String,List<String>> neighborhood ;
  /**
   * 
   * @param config
   */
  protected SLOM(int k, PrimitiveDistanceFunction<O, D> neighborhoodDistanceFunction, PrimitiveDistanceFunction<O, D> nonSpatialDistanceFunction) {
    super(new EmptyParameterization());
    this.k = k;
    this.neighborhoodDistanceFunction = neighborhoodDistanceFunction;
    this.nonSpatialDistanceFunction = nonSpatialDistanceFunction;
    neighborhood = new HashMap<String,List<String>>();
  }

  /**
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
    double startTime = System.currentTimeMillis();
    if(logger.isVerbose()){
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------Time start:"+startTime+"----------------------------------");
    }
    DistanceQuery<O, D> distFunc = database.getDistanceQuery(neighborhoodDistanceFunction);
    WritableDataStore<Double> modifiedDistance = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    // average of modified distance -- o and its neighbors --
    WritableDataStore<Double> avgModifiedDistancePlus = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    // average of modified distance -- neighbors of o --
    WritableDataStore<Double> avgModifiedDistance = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    // beta
    WritableDataStore<Double> betaList = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    //
    KNNQuery<O, D> knnQuery = database.getKNNQuery(distFunc, k + 1, DatabaseQuery.HINT_EXACT);
    //
    WritableDataStore<List<DistanceResultPair<D>>> knn = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, List.class);
    
      
      for(DBID id : database) {
        double sum = 0; 
        double maxDist = 0 ;
        List<DistanceResultPair<D>> dResultPairs = knnQuery.getKNNForDBID(id, k+1);
        knn.put(id, dResultPairs);
         for(DistanceResultPair<D> resultPair : dResultPairs) {
          double dist = nonSpatialDistanceFunction.distance(database.get(id), database.get(resultPair.second)).doubleValue();
          if(maxDist<dist){maxDist = dist ;}
          sum += dist;   
      }
      modifiedDistance.put(id, ((sum - maxDist) / (k - 1)));
      System.out.println(modifiedDistance.get(id));
    }
    
    // second step :
    // compute average modified distance of id neighborhood and id it's self
    // compute average modified distance of only id neighborhood
    double ModifiedDistanceTime = System.currentTimeMillis()- startTime;
    System.out.println(ModifiedDistanceTime);
    if(logger.isVerbose()){
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------ModifiedTime:"+ModifiedDistanceTime+"----------------------------------");
    }
   
    for(DBID id : database) {
      double avgPlus = 0;
      double avg = 0;
       // compute avg
      for(DistanceResultPair<D> resultPair : knn.get(id)) {
         if(resultPair.second == id){
           avgPlus = avgPlus + modifiedDistance.get(resultPair.second);
         }
         else{
           avgPlus = avgPlus + modifiedDistance.get(resultPair.second);
           avg = avg + modifiedDistance.get(resultPair.second);
         }
        }
      avgPlus = avgPlus / (k + 1);
      avg = avg / (k);
      avgModifiedDistancePlus.put(id, avgPlus);
      avgModifiedDistance.put(id, avg);
      }
      
    
    
    double avgTime = System.currentTimeMillis()-ModifiedDistanceTime;
    System.out.println(avgTime);
    if(logger.isVerbose()){
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------AVG Time:"+avgTime+"----------------------------------");
    }
    // compute beta
    for(DBID id : database) {
      double beta = 0;
      for(DistanceResultPair<D> resultPair : knn.get(id)) {
        if(modifiedDistance.get(resultPair.second).doubleValue() > avgModifiedDistancePlus.get(id)) {
          beta++;
        }
        if(modifiedDistance.get(resultPair.second).doubleValue() < avgModifiedDistancePlus.get(id)) {
          beta--;
        }
      }
      beta = Math.abs(beta);
      beta = (Math.max(beta, 1) / (k - 1));
      beta = beta / (1 + avgModifiedDistance.get(id));
      betaList.put(id, beta);

    }
    double betaTime = System.currentTimeMillis()- avgTime;
    System.out.println(betaTime);
    if(logger.isVerbose()){
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------Time END:"+betaTime+"----------------------------------");
    }
    // compute SLOM for each Object
    MinMax<Double> minmax = new MinMax<Double>();
    WritableDataStore<Double> sloms = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : database) {
      double slom = betaList.get(id) * modifiedDistance.get(id);
      sloms.put(id, slom);
      minmax.put(slom);
    }
    double SlomTime = System.currentTimeMillis()- betaTime;
    System.out.println(SlomTime);
    if(logger.isVerbose()){
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------Time END:"+SlomTime+"----------------------------------");
    }
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("SLOM", "SLOM-outlier", SLOM_SCORE, sloms);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.48);
    return new OutlierResult(scoreMeta, scoreResult);

  }
  
  /**
   * 
   * @param path
   */
  public void initNeighboorhood(String path,Database<O> database) throws IOException{   
    File input = new File(path);
    FileReader reader = new FileReader(input);
    BufferedReader br = new BufferedReader(reader);
    int lineNumber = 0 ;
    for (String line; (line = br.readLine()) != null; lineNumber++) {
      List<String> neighboors = new ArrayList<String>();
     String[] entries = line.split(" ");
       String ID = "ID="+entries[0];
        for(int i = 1 ; i<entries.length ; i++){
          neighboors.add("ID="+entries[i]);
        }
       neighborhood.put(ID, neighboors);
    }
  }

  /**
   * 
   */
  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * SLOM Outlier Score Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return SLOM Outlier Algorithm
   */
  public static <O extends ExternalObject, D extends NumberDistance<D, ?>> SLOM<O, D> parameterize(Parameterization config) {
    int k = getParameterK(config);
    PrimitiveDistanceFunction<O, D> neighborhooddistanceFunction = getNeighborhoodDistanceFunction(config);
    PrimitiveDistanceFunction<O, D> nonSpatialDistanceFunction = getNonSpatialDistanceFunction(config);
    if(config.hasErrors()) {
      return null;
    }

    return new SLOM<O, D>(k, neighborhooddistanceFunction, nonSpatialDistanceFunction);
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
    return -1;
  }

  /**
   * 
   * @param <F>
   * @param config
   * @return
   */
  protected static <F extends DistanceFunction<?, ?>> F getNeighborhoodDistanceFunction(Parameterization config) {
    final ObjectParameter<F> param = new ObjectParameter<F>(SPATIAL_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }

  /**
   * 
   * @param <F>
   * @param config
   * @return
   */
  protected static <F extends DistanceFunction<?, ?>> F getNonSpatialDistanceFunction(Parameterization config) {
    final ObjectParameter<F> param = new ObjectParameter<F>(NON_SPATIAL_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }

}