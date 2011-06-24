package experimentalcode.hettab.outlier;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;

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
public class SLOM<V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> extends DistanceBasedSpatialOutlier<V, D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SLOM.class);

   /**
   * The association id to associate the SLOM_SCORE of an object for the SLOM
   * algorithm.
   */
  public static final AssociationID<Double> SLOM_SCORE = AssociationID.getOrCreateAssociationID("slom", Double.class);

  /**
   * 
   * @param config
   */
  protected SLOM(NeighborSetPredicate.Factory<V> npred, PrimitiveDistanceFunction<V, D> nonSpatialDistanceFunction) {
    super(npred,nonSpatialDistanceFunction);
  }
  
  /**
   * 
   * @param database
   * @param relation
   * @return
   */
  public OutlierResult run(Database database, Relation<V> relation) {
    WritableDataStore<Double> modifiedDistance = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> avgModifiedDistancePlus = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> avgModifiedDistance = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
    WritableDataStore<Double> betaList = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Double.class);
  
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(relation);

    // calculate D-Tilde
    for(DBID id : relation.getDBIDs()) {
      double sum = 0;
      double maxDist = 0;

     final DBIDs  neighbors= npred.getNeighborDBIDs(id);
      for(DBID neighbor : neighbors) {
        if(id.getIntegerID() == neighbor.getIntegerID()) {
          continue;
        }
        double dist = getNonSpatialDistanceFunction().distance(relation.get(id), relation.get(neighbor)).doubleValue();
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

    for(DBID id : relation.getDBIDs()) {
      double avgPlus = 0;
      double avg = 0;

      final DBIDs  neighbors= npred.getNeighborDBIDs(id);
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
    for(DBID id : relation.getDBIDs()) {
      double beta = 0;
      final DBIDs  neighbors= npred.getNeighborDBIDs(id);
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
    WritableDataStore<Double> sloms = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : relation.getDBIDs()) {
      double slom = betaList.get(id) * modifiedDistance.get(id);
      sloms.put(id, slom);
      minmax.put(slom);
    }

    //
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("SLOM", "SLOM-outlier", SLOM_SCORE, sloms);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(Double.NaN, minmax.getMax(), 0.0, Double.POSITIVE_INFINITY);
    return new OutlierResult(scoreMeta, scoreResult);

  }

  /**
   * 
   */
  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }
  /**
   * 
   */
  public static class Parameterizer<V extends NumberVector<V,?>,D extends NumberDistance<D, ?>> extends DistanceBasedSpatialOutlier.Parameterizer<V, D>{

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }
   
    @Override
    protected SLOM<V,D> makeInstance() {
      return new SLOM<V,D>(npredf,distanceFunction);
    }
    
  }
}