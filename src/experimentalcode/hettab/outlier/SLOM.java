package experimentalcode.hettab.outlier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.ExternalObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.external.FileBasedDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
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
import experimentalcode.hettab.textwriter.KMLTextWriter;

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
public class SLOM<V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<V, DoubleDistance, OutlierResult> implements OutlierAlgorithm<V, OutlierResult> {
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
  protected DistanceFunction<V, DoubleDistance> neighborhoodDistanceFunction;

  /**
   * Holds the value of {@link #NON_SPATIAL_DISTANCE_FUNCTION_ID}
   */
  protected DistanceFunction<V, D> nonSpatialDistanceFunction;

  /**
   * The association id to associate the SLOM_SCORE of an object for the SLOM
   * algorithm.
   */
  public static final AssociationID<Double> SLOM_SCORE = AssociationID.getOrCreateAssociationID("slom", Double.class);

  /**
   * 
   */
  public HashMap<String, List<String>> neighborhood;

  /**
   * 
   * @param config
   */
  protected SLOM(int k, DistanceFunction<V, DoubleDistance> neighborhoodDistanceFunction, DistanceFunction<V, D> nonSpatialDistanceFunction) {
    super(new EmptyParameterization());
    this.k = k;
    this.neighborhoodDistanceFunction = neighborhoodDistanceFunction;
    this.nonSpatialDistanceFunction = nonSpatialDistanceFunction;

  }

  /**
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
   
     /**
     try{
      initNeighboorhood("C:\\Users\\hettab\\Desktop\\countysfull.txt", database);
      }
     catch(Exception exp){System.out.println(exp.toString());}
    **/
 
    double startTime = System.currentTimeMillis();
    if(logger.isVerbose()) {
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------Time start:" + startTime + "----------------------------------");
    }

    DistanceQuery<V, DoubleDistance> distFunc = database.getDistanceQuery(neighborhoodDistanceFunction);
    WritableDataStore<Double> modifiedDistance = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    // average of modified distance -- o and its neighbors --
    WritableDataStore<Double> avgModifiedDistancePlus = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    // average of modified distance -- neighbors of o --
    WritableDataStore<Double> avgModifiedDistance = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    // beta
    WritableDataStore<Double> betaList = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    //
    // KNNQuery<V, D> knnQuery = database.getKNNQuery(distFunc, k + 1,
    // DatabaseQuery.HINT_EXACT);
    //
    // WritableDataStore<List<DistanceResultPair<D>>> knn =
    // DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP |
    // DataStoreFactory.HINT_HOT, List.class);
    //
    WritableDataStore<List<DistanceResultPair<DoubleDistance>>> neighboors = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, List.class);

    RangeQuery<V, DoubleDistance> range = database.getRangeQuery(distFunc, DatabaseQuery.HINT_EXACT);

    for(DBID id : database) {
      double sum = 0;
      double maxDist = 0;
      DoubleDistance d = new DoubleDistance(0.5);
      List<DistanceResultPair<DoubleDistance>> dResultPairs = range.getRangeForDBID(id, d);
      System.out.println(dResultPairs);
      neighboors.put(id, dResultPairs);
      for(DistanceResultPair<DoubleDistance> resultPair : dResultPairs) {

        double dist = ((PrimitiveDistanceFunction<V, D>) nonSpatialDistanceFunction).distance(database.get(id), database.get(resultPair.second)).doubleValue();
        if(maxDist < dist) {
          maxDist = dist;
        }
        sum += dist;
      }
      modifiedDistance.put(id, ((sum - maxDist) / (k - 1)));
    }

      /*
      for(DBID id : database) {
      double sum = 0; double maxDist = 0; // List<DistanceResultPair<D>>
      dResultPairs = knnQuery.getKNNForDBID(id, k + 1); knn.put(id,
      dResultPairs); for(DistanceResultPair<D> resultPair : dResultPairs) {
      
      double dist = ((PrimitiveDistanceFunction<V,D>)
      nonSpatialDistanceFunction).distance(database.get(id),
      database.get(resultPair.second)).doubleValue(); if(maxDist < dist) {
      maxDist = dist; } sum += dist; } modifiedDistance.put(id, ((sum -
      maxDist) / (k - 1))); }
     */
     
    // second step :
    // compute average modified distance of id neighborhood and id it's self
    // compute average modified distance of only id neighborhood
    
    double ModifiedDistanceTime = System.currentTimeMillis() - startTime;
    System.out.println(ModifiedDistanceTime);
    if(logger.isVerbose()) {
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------ModifiedTime:" + ModifiedDistanceTime + "----------------------------------");
    }

    for(DBID id : database) {
      double avgPlus = 0;
      double avg = 0;
      // compute avg
      for(DistanceResultPair<DoubleDistance> resultPair : neighboors.get(id)) {
        if(resultPair.second == id) {
          avgPlus = avgPlus + modifiedDistance.get(resultPair.second);
        }
        else {
          avgPlus = avgPlus + modifiedDistance.get(resultPair.second);
          avg = avg + modifiedDistance.get(resultPair.second);
        }
      }
      avgPlus = avgPlus / (k + 1);
      avg = avg / (k);
      avgModifiedDistancePlus.put(id, avgPlus);
      avgModifiedDistance.put(id, avg);
    }

    double avgTime = System.currentTimeMillis() - ModifiedDistanceTime;
    System.out.println(avgTime);
    if(logger.isVerbose()) {
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------AVG Time:" + avgTime + "----------------------------------");
    }
    // compute beta
    for(DBID id : database) {
      double beta = 0;
      for(DistanceResultPair<DoubleDistance> resultPair : neighboors.get(id)) {
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
    double betaTime = System.currentTimeMillis() - avgTime;
    System.out.println(betaTime);
    if(logger.isVerbose()) {
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------Time END:" + betaTime + "----------------------------------");
    }
    // compute SLOM for each Object
    MinMax<Double> minmax = new MinMax<Double>();
    WritableDataStore<Double> sloms = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : database) {
      double slom = betaList.get(id) * modifiedDistance.get(id);
      sloms.put(id, slom);
      minmax.put(slom);
    }
    double SlomTime = System.currentTimeMillis() - betaTime;
    System.out.println(SlomTime);
    if(logger.isVerbose()) {
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------Time END:" + SlomTime + "----------------------------------");
    }
    KMLTextWriter<V> resu = new KMLTextWriter<V>();
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("SLOM", "SLOM-outlier", SLOM_SCORE, sloms);
    resu.processResult(database, sloms);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
   
    return new OutlierResult(scoreMeta, scoreResult);
   
  
  }

  /**
   * 
   * @param path
   */
  /**
  public void initNeighboorhood(String path, Database<V> database) throws IOException {
    neighborhood = new HashMap<String, List<String>>(); 
    File input = new File(path);
    FileReader reader = new FileReader(input);
    BufferedReader br = new BufferedReader(reader);
    int lineNumber = 0;
    for(String line; (line = br.readLine()) != null; lineNumber++) {
      List<String> neighboors = new ArrayList<String>();
      String[] entries = line.split(" ");
      String ID = entries[0];
      for(int i = 0; i < entries.length; i++) {
        neighboors.add(entries[i]);
      }
      
      neighborhood.put(ID, neighboors);
      
    }
  
    try {
      FileWriter f = new FileWriter("C:\\Users\\hettab\\Desktop\\knn8\\neighborhoodDistance1.csv");
     
      for(DBID id : database) {
        String idR = database.getObjectLabel(id);
        List<String> neighboors = neighborhood.get(idR);
        //System.out.println(neighboors);
        String lines = "";
       
        for(DBID ids : database) {
          String idsR = database.getObjectLabel(ids);
          System.out.println(idsR +" "+idR);
          int count = 0 ;
             for(int i = 0; i < neighboors.size(); i++) {
              if(idR == idsR) {
              count ++ ;
               }
             }
          if(count == 0 ){lines +=1.0+" ";}
          if(count > 0 ){lines +=0.0+" ";}
          }
        f.write(lines);
        f.write("\n");
        }
      f.close();
    }
    catch(Exception exp) {
      exp.printStackTrace();
    }
  
  }
 **/
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
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> SLOM<V, D> parameterize(Parameterization config) {
    int k = getParameterK(config);
    DistanceFunction<V, DoubleDistance> neighborhooddistanceFunction = getNeighborhoodDistanceFunction(config);
    PrimitiveDistanceFunction<V, D> nonSpatialDistanceFunction = getNonSpatialDistanceFunction(config);
    if(config.hasErrors()) {
      return null;
    }
    return new SLOM<V, D>(k, neighborhooddistanceFunction, nonSpatialDistanceFunction);
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
    final ObjectParameter<F> param = new ObjectParameter<F>(SPATIAL_DISTANCE_FUNCTION_ID, FileBasedDoubleDistanceFunction.class, true);
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