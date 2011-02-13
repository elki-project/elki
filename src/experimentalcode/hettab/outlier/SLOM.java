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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.hettab.textwriter.KMLTextWriter;

/**
 * SLOM Algorithm
 * 
 * @author Ahmed Hettab
 * 
 * @param <O> the type of DatabaseObjects handled by the algorithm
 * @param <D> the type of Distance used for non spatial attributes
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
   * Parameter to specify the neighborhood distance function to use ;
   */
  public static final OptionID NEIGHBORHOOD_FILE_ID = OptionID.getOrCreateOptionID("slom.neighborhoodfile", "The external neighborhood File");

  /**
   * Parameter to specify the non spatial distance function to use
   */
  public static final OptionID NON_SPATIAL_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("slom.nonspatialdistancefunction", "The distance function to use for non spatial attributes");

  /**
   * Parameter to specify the neighborhood distance function to use ;
   */
  public static final OptionID KML_OUTPUT_FILE_ID = OptionID.getOrCreateOptionID("slom.kmloutputpath", "The kml output File for google earth");

  /**
   * Holds the value of {@link #PATH_NEIGHBORHOOD_ID}
   */
  private File neighborhoodFile;

  /**
   * Holds the value of {@link #NON_SPATIAL_DISTANCE_FUNCTION_ID}
   */
  protected PrimitiveDistanceFunction<V, D> nonSpatialDistanceFunction;

  /**
   * Holds the neighborhood for each DBID
   */
  private HashMap<DBID, List<DBID>> neighborhood;

  /**
   * the kml output file
   */
  private File outputKmlFile;

  /**
   * The association id to associate the SLOM_SCORE of an object for the SLOM
   * algorithm.
   */
  public static final AssociationID<Double> SLOM_SCORE = AssociationID.getOrCreateAssociationID("slom", Double.class);

  /**
   * 
   * @param config
   */
  protected SLOM(File neighborhoodFile, PrimitiveDistanceFunction<V, D> nonSpatialDistanceFunction, File outputKmlFile) {
    super(new EmptyParameterization());
    this.neighborhoodFile = neighborhoodFile;
    this.nonSpatialDistanceFunction = nonSpatialDistanceFunction;
    this.outputKmlFile = outputKmlFile;
    neighborhood = new HashMap<DBID, List<DBID>>();

  }

  /**
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {

    WritableDataStore<Double> modifiedDistance = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> avgModifiedDistancePlus = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> avgModifiedDistance = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> betaList = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);

    // get the neighborhood
    try {
      getExternalNeighborhood();
    }
    catch(IOException e) {
      e.printStackTrace();
    }

    double startTime = System.currentTimeMillis();
    if(logger.isVerbose()) {
      logger.verbose("----------------------------------SLOM----------------------------------");
      logger.verbose("----------------------------------Time start:" + startTime + "----------------------------------");
    }

    // calculate D-Tilde
    for(DBID id : database) {
      double sum = 0;
      double maxDist = 0;

      List<DBID> neighbors = neighborhood.get(id);
      System.out.println(id);
      for(DBID neighbor : neighbors) {
        if(id.getIntegerID() == neighbor.getIntegerID()) {
          continue;
        }
        double dist = nonSpatialDistanceFunction.distance(database.get(id), database.get(neighbor)).doubleValue();
        if(maxDist < dist) {
          maxDist = dist;
        }
        sum += dist;
      }
      modifiedDistance.put(id, ((sum - maxDist) / (neighbors.size() - 2)));
    }

    // second step :
    // compute average modified distance of id neighborhood and id it's self
    // compute average modified distance of only id neighborhood

    for(DBID id : database) {
      double avgPlus = 0;
      double avg = 0;

      List<DBID> neighbors = neighborhood.get(id);
      // compute avg
      for(DBID neighbor : neighbors) {
        if(neighbor.getIntegerID() == id.getIntegerID()) {
          avgPlus = avgPlus + modifiedDistance.get(neighbor);
        }
        else {
          avgPlus = avgPlus + modifiedDistance.get(neighbor);
          avg = avg + modifiedDistance.get(neighbor);
        }
      }
      avgPlus = avgPlus / (neighbors.size());
      avg = avg / (neighbors.size() - 1);
      avgModifiedDistancePlus.put(id, avgPlus);
      avgModifiedDistance.put(id, avg);
    }

    // compute beta
    for(DBID id : database) {
      double beta = 0;
      List<DBID> neighbors = neighborhood.get(id);
      for(DBID neighbor : neighbors) {
        if(modifiedDistance.get(neighbor).doubleValue() > avgModifiedDistancePlus.get(id)) {
          beta++;
        }
        if(modifiedDistance.get(neighbor).doubleValue() < avgModifiedDistancePlus.get(id)) {
          beta--;
        }
      }
      beta = Math.abs(beta);
      beta = (Math.max(beta, 1) / (neighbors.size() - 2));
      beta = beta / (1 + avgModifiedDistance.get(id));
      betaList.put(id, beta);
    }

    MinMax<Double> minmax = new MinMax<Double>();
    WritableDataStore<Double> sloms = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : database) {
      double slom = betaList.get(id) * modifiedDistance.get(id);
      sloms.put(id, slom);
      minmax.put(slom);
    }

    //
    KMLTextWriter<V> resu = new KMLTextWriter<V>(outputKmlFile,neighborhood);
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("SLOM", "SLOM-outlier", SLOM_SCORE, sloms);
    resu.processResult(database, sloms);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);

  }

  /**
   * get the external neighborhood
   * 
   * @param path
   */

  public void getExternalNeighborhood() throws IOException {
    FileReader reader = new FileReader(neighborhoodFile);
    BufferedReader br = new BufferedReader(reader);
    int lineNumber = 0;
    for(String line; (line = br.readLine()) != null; lineNumber++) {
      List<DBID> neighboors = new ArrayList<DBID>();
      String[] entries = line.split(" ");
      DBID ID = DBIDUtil.importInteger(Integer.valueOf(entries[0]));
      for(int i = 0; i < entries.length; i++) {
        neighboors.add(DBIDUtil.importInteger(Integer.valueOf(entries[i])));
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
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> SLOM<V, D> parameterize(Parameterization config) {
    File neighborhoodFile = getExternalNeighborhood(config);
    File kmlFile = getKMLOutputPath(config);
    PrimitiveDistanceFunction<V, D> nonSpatialDistanceFunction = getNonSpatialDistanceFunction(config);
    if(config.hasErrors()) {
      return null;
    }
    return new SLOM<V, D>(neighborhoodFile, nonSpatialDistanceFunction, kmlFile);
  }

  /**
   * 
   * @param <F>
   * @param config
   * @return
   */
  protected static File getExternalNeighborhood(Parameterization config) {
    final FileParameter param = new FileParameter(NEIGHBORHOOD_FILE_ID, FileParameter.FileType.INPUT_FILE);
    if(config.grab(param)) {
      return param.getValue();
    }
    return null;
  }

  /**
   * 
   */
  protected static File getKMLOutputPath(Parameterization config) {
    final FileParameter param = new FileParameter(KML_OUTPUT_FILE_ID, FileParameter.FileType.OUTPUT_FILE);
    if(config.grab(param)) {
      return param.getValue();
    }
    return null;
  }

  /**
   * 
   * @param <F>
   * @param config
   * @return
   */
  protected static <F extends PrimitiveDistanceFunction<?, ?>> F getNonSpatialDistanceFunction(Parameterization config) {
    final ObjectParameter<F> param = new ObjectParameter<F>(NON_SPATIAL_DISTANCE_FUNCTION_ID, PrimitiveDistanceFunction.class, true);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }

}