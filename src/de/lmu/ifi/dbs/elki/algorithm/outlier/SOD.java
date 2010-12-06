package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.BitSet;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionsSelectingEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNList;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * @author Arthur Zimek
 * 
 * @apiviz.has SODModel oneway - - computes
 * @apiviz.has SharedNearestNeighborSimilarityFunction
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
// todo arthur comment
@Title("SOD: Subspace outlier degree")
@Description("Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data")
@Reference(authors = "H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", title = "Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data", booktitle = "Proceedings of the 13th Pacific-Asia Conference on Knowledge Discovery and Data Mining (PAKDD), Bangkok, Thailand, 2009", url = "http://dx.doi.org/10.1007/978-3-642-01307-2")
public class SOD<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractAlgorithm<V, OutlierResult> implements OutlierAlgorithm<V, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SOD.class);
  
  /**
   * The association id to associate a subspace outlier degree.
   */
  public static final AssociationID<SODModel<?>> SOD_MODEL = AssociationID.getOrCreateAssociationIDGenerics("SOD", SODModel.class);

  /**
   * The association id for the raw scores.
   */
  public static final AssociationID<Double> SOD_SCORE = AssociationID.getOrCreateAssociationID("SOD_SCORE", Double.class);

  /**
   * Parameter to specify the number of shared nearest neighbors to be
   * considered for learning the subspace properties., must be an integer
   * greater than 0.
   */
  public static final OptionID KNN_ID = OptionID.getOrCreateOptionID("sod.knn", "The number of shared nearest neighbors to be considered for learning the subspace properties.");

  /**
   * Holds the value of {@link #KNN_ID}.
   */
  private int knn;

  /**
   * Parameter to indicate the multiplier for the discriminance value for
   * discerning small from large variances.
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("sod.alpha", "The multiplier for the discriminance value for discerning small from large variances.");

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private double alpha;

  /**
   * The similarity function.
   */
  private SharedNearestNeighborSimilarityFunction<V, D> similarityFunction;

  /**
   * Constructor with parameters.
   * 
   * @param knn knn value
   * @param alpha Alpha parameter
   * @param similarityFunction Shared nearest neighbor similarity function
   */
  public SOD(int knn, double alpha, SharedNearestNeighborSimilarityFunction<V, D> similarityFunction) {
    super();
    this.knn = knn;
    this.alpha = alpha;
    this.similarityFunction = similarityFunction;
  }

  /**
   * Performs the SOD algorithm on the given database.
   */
  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    SimilarityQuery<V, IntegerDistance> snnInstance = similarityFunction.instantiate(database);
    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Assigning Subspace Outlier Degree", database.size(), logger) : null;
    int processed = 0;
    WritableDataStore<SODModel<?>> sod_models = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, SODModel.class);
    DoubleMinMax minmax = new DoubleMinMax();
    for(Iterator<DBID> iter = database.iterator(); iter.hasNext();) {
      DBID queryObject = iter.next();
      processed++;
      if(progress != null) {
        progress.setProcessed(processed, logger);
      }
      DBIDs knnList = getKNN(database, snnInstance, queryObject).asDBIDs();
      SODModel<V> model = new SODModel<V>(database, knnList, alpha, database.get(queryObject));
      sod_models.put(queryObject, model);
      minmax.put(model.getSod());
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
    }
    // combine results.
    AnnotationResult<SODModel<?>> models = new AnnotationFromDataStore<SODModel<?>>("Subspace Outlier Model", "sod-outlier", SOD_MODEL, sod_models);
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    OutlierResult sodResult = new OutlierResult(meta, new SODProxyScoreResult(models));
    // also add the models.
    sodResult.addPrimaryResult(models);
    return sodResult;
  }

  /**
   * Provides the k nearest neighbors in terms of the shared nearest neighbor
   * distance.
   * <p/>
   * The query object is excluded from the knn list.
   * 
   * @param database the database holding the objects
   * @param snnInstance similarity function
   * @param queryObject the query object for which the kNNs should be determined
   * @return the k nearest neighbors in terms of the shared nearest neighbor
   *         distance without the query object
   */
  private KNNList<DoubleDistance> getKNN(Database<V> database, SimilarityQuery<V, IntegerDistance> snnInstance, DBID queryObject) {
    // similarityFunction.getPreprocessor().getParameters();
    KNNHeap<DoubleDistance> kNearestNeighbors = new KNNHeap<DoubleDistance>(knn, new DoubleDistance(Double.POSITIVE_INFINITY));
    for(Iterator<DBID> iter = database.iterator(); iter.hasNext();) {
      DBID id = iter.next();
      if(!id.equals(queryObject)) {
        DoubleDistance distance = new DoubleDistance(1.0 / snnInstance.similarity(queryObject, id).doubleValue());
        kNearestNeighbors.add(new DistanceResultPair<DoubleDistance>(distance, id));
      }
    }
    return kNearestNeighbors.toKNNList();
  }

  /**
   * 
   * 
   * @author Arthur Zimek
   * @param <O> the type of DatabaseObjects handled by this Result
   */
  // TODO: arthur comment
  public static class SODModel<O extends NumberVector<O, ?>> implements TextWriteable, Comparable<SODModel<?>> {
    private double[] centerValues;

    private O center;

    private double[] variances;

    private double expectationOfVariance;

    private BitSet weightVector;

    private double sod;

    /**
     * Initialize SOD Model
     * 
     * @param database Database
     * @param neighborhood Neighborhood
     * @param alpha Alpha value
     * @param queryObject Query object
     */
    public SODModel(Database<O> database, DBIDs neighborhood, double alpha, O queryObject) {
      // TODO: store database link?
      centerValues = new double[DatabaseUtil.dimensionality(database)];
      variances = new double[centerValues.length];
      for(DBID id : neighborhood) {
        O databaseObject = database.get(id);
        for(int d = 0; d < centerValues.length; d++) {
          centerValues[d] += databaseObject.doubleValue(d + 1);
        }
      }
      for(int d = 0; d < centerValues.length; d++) {
        centerValues[d] /= neighborhood.size();
      }
      for(DBID id : neighborhood) {
        O databaseObject = database.get(id);
        for(int d = 0; d < centerValues.length; d++) {
          // distance
          double distance = centerValues[d] - databaseObject.doubleValue(d + 1);
          // variance
          variances[d] += distance * distance;
        }
      }
      expectationOfVariance = 0;
      for(int d = 0; d < variances.length; d++) {
        variances[d] /= neighborhood.size();
        expectationOfVariance += variances[d];
      }
      expectationOfVariance /= variances.length;
      weightVector = new BitSet(variances.length);
      for(int d = 0; d < variances.length; d++) {
        if(variances[d] < alpha * expectationOfVariance) {
          weightVector.set(d, true);
        }
      }
      center = database.getObjectFactory().newInstance(centerValues);
      sod = subspaceOutlierDegree(queryObject, center, weightVector);
    }

    /**
     * 
     * 
     * @param queryObject
     * @param center
     * @param weightVector
     * @return sod value
     */
    private double subspaceOutlierDegree(O queryObject, O center, BitSet weightVector) {
      ListParameterization params = new ListParameterization();
      // params.addParameter(AbstractDimensionsSelectingDoubleDistanceFunction.DIMS_ID,
      // weightVector);
      final DimensionsSelectingEuclideanDistanceFunction DISTANCE_FUNCTION = new DimensionsSelectingEuclideanDistanceFunction(params);
      params.logAndClearReportedErrors();
      DISTANCE_FUNCTION.setSelectedDimensions(weightVector);
      double distance = DISTANCE_FUNCTION.distance(queryObject, center).doubleValue();
      distance /= weightVector.cardinality();
      return distance;
    }

    /**
     * Return the SOD of the point.
     * 
     * @return sod value
     */
    public double getSod() {
      return this.sod;
    }

    @Override
    public void writeToText(TextWriterStream out, String label) {
      out.inlinePrint(label + "=" + this.sod);
      out.commentPrintLn(this.getClass().getSimpleName() + ":");
      out.commentPrintLn("relevant attributes (counting starts with 0): " + this.weightVector.toString());
      out.commentPrintLn("center of neighborhood: " + out.normalizationRestore(center).toString());
      out.commentPrintLn("subspace outlier degree: " + this.sod);
      out.commentPrintSeparator();
    }

    @Override
    public int compareTo(SODModel<?> o) {
      return Double.compare(this.getSod(), o.getSod());
    }

  }

  /**
   * Proxy class that converts a model result to an actual SOD score result.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected static class SODProxyScoreResult implements AnnotationResult<Double> {
    /**
     * Model result this is a proxy for.
     */
    AnnotationResult<SODModel<?>> models;

    /**
     * Constructor.
     * 
     * @param models Models result
     */
    public SODProxyScoreResult(AnnotationResult<SODModel<?>> models) {
      super();
      this.models = models;
    }

    @Override
    public AssociationID<Double> getAssociationID() {
      return SOD_SCORE;
    }

    @Override
    public Double getValueFor(DBID objID) {
      return models.getValueFor(objID).getSod();
    }

    @Override
    public String getLongName() {
      return "Subspace Outlier Degree";
    }

    @Override
    public String getShortName() {
      return "sod-outlier";
    }
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static <V extends NumberVector<V, ?>, D extends NumberDistance<D, ?>> SOD<V, D> parameterize(Parameterization config) {
    int knn = getParameterKNN(config);

    double alpha = getParameterAlpha(config);

    SharedNearestNeighborSimilarityFunction<V, D> similarityFunction = new SharedNearestNeighborSimilarityFunction<V, D>(config);

    if(config.hasErrors()) {
      return null;
    }
    return new SOD<V, D>(knn, alpha, similarityFunction);
  }

  /**
   * Get the alpha parameter for the knn query
   * 
   * @param config Parameterization
   * @return alpha parameter
   */
  protected static double getParameterAlpha(Parameterization config) {
    final DoubleParameter param = new DoubleParameter(ALPHA_ID, new GreaterConstraint(0), 1.1);
    if(config.grab(param)) {
      return param.getValue();
    }
    return Double.NaN;
  }

  /**
   * Get the k parameter for the knn query
   * 
   * @param config Parameterization
   * @return k parameter
   */
  protected static int getParameterKNN(Parameterization config) {
    final IntParameter param = new IntParameter(KNN_ID, new GreaterConstraint(0), 1);
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}