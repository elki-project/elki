package experimentalcode.hettab.outlier;



import org.apache.commons.math.stat.descriptive.rank.Median;


import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
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
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate.Factory;
/**
 * 
 * @author Ahmed Hettab
 *
 * @param <V>
 */
public class MedianAlgorithm<V extends NumberVector<?, ?>> extends AbstractAlgorithm<V> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MedianAlgorithm.class);

  /**
   * Parameter to specify the neighborhood predicate to use.
   */
  public static final OptionID NEIGHBORHOOD_ID = OptionID.getOrCreateOptionID("neighborhood", "The neighborhood predicate to use in comparison step.");

  /**
   * Our predicate to obtain the neighbors
   */
  NeighborSetPredicate.Factory<Object> npredf = null;

  /**
   * 
   * Holds the z value
   */
  private static final OptionID Z_ID = OptionID.getOrCreateOptionID("position.z", "the position of z attribut");

  /**
   * parameter z
   */
  private int z;

  /**
   * The association id to associate the SCORE of an object for the 
   * algorithm.
   */
  public static final AssociationID<Double> MEDIAN_SCORE = AssociationID.getOrCreateAssociationID("score", Double.class);

  /**
   * Constructor
   * 
   * @param npredf
   * @param z
   */
  public MedianAlgorithm(Factory<Object> npredf, int z) {
    super();
    this.npredf = npredf;
    this.z = z;
  }

  /**
   * 
   * @param <V>
   * @param config
   * @return
   */
  public static <V extends NumberVector<V, ?>> MedianAlgorithm<V> parameterize(Parameterization config) {
    final NeighborSetPredicate.Factory<Object> npred = getNeighborPredicate(config);
    final int y = getParameterY(config);
    if(config.hasErrors()) {
      return null;
    }
    return new MedianAlgorithm<V>(npred, y);
  }

  protected static int getParameterY(Parameterization config) {
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
  public static NeighborSetPredicate.Factory<Object> getNeighborPredicate(Parameterization config) {
    final ObjectParameter<NeighborSetPredicate.Factory<Object>> param = new ObjectParameter<NeighborSetPredicate.Factory<Object>>(NEIGHBORHOOD_ID, NeighborSetPredicate.Factory.class, true);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    
    Relation<V> relation = getRelation(database);
    WritableDataStore<Double> gi = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    WritableDataStore<Double> hi = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    
    
    final NeighborSetPredicate npred = npredf.instantiate(relation);

    for(DBID id : relation.getDBIDs()) {
      //
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      int size = neighbors.size();
      double[] fi = new double[size];
      Median m = new Median();
      int i = 0;
      // calculate and store mean
      for(DBID n : neighbors) {
        fi[i] = relation.get(n).doubleValue(z);
        i++;
      }
      double median = m.evaluate(fi);
      gi.put(id, median);
      // store hi
      hi.put(id, relation.get(id).doubleValue(z) - median);
    }
    // calculate mean and variance
    MeanVariance mv = new MeanVariance();
    for(DBID id : relation.getDBIDs()) {
      mv.put(hi.get(id));
    }
    double mean = mv.getMean();
    double variance = mv.getSampleVariance();

    MinMax<Double> minmax = new MinMax<Double>();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    for(DBID id : relation.getDBIDs()) {
      double score = Math.abs((hi.get(id) - mean) / variance);
      minmax.put(score);
      scores.put(id, score);
    }

    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("MOF", "Trimmedmean-outlier", MEDIAN_SCORE, scores);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

}
