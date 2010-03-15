package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.KNNList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionsSelectingEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.ProbabilisticOutlierScore;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * @author Arthur Zimek
 * @param <V> the type of NumberVector handled by this Algorithm
 * @param <D> the type of Distance used by this Algorithm
 */
// todo arthur comment
@Title("SOD: Subspace outlier degree")
@Description("Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data")
// TODO: Add Description!
@Reference(authors = "H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", title = "Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data", booktitle = "Proceedings of the 13th Pacific-Asia Conference on Knowledge Discovery and Data Mining (PAKDD), Bangkok, Thailand, 2009", url="http://dx.doi.org/10.1007/978-3-642-01307-2")
public class SOD<V extends NumberVector<V, ?>, D extends Distance<D>> extends AbstractAlgorithm<V, OutlierResult> {
  /**
   * The association id to associate a subspace outlier degree.
   */
  public static final AssociationID<SODModel<?>> SOD_MODEL = AssociationID.getOrCreateAssociationIDGenerics("SOD", SODModel.class);

  /**
   * The association id for the raw scores.
   */
  public static final AssociationID<Double> SOD_SCORE = AssociationID.getOrCreateAssociationID("SOD_SCORE", Double.class);

  /**
   * OptionID for {@link #KNN_PARAM}
   */
  public static final OptionID KNN_ID = OptionID.getOrCreateOptionID("sod.knn", "The number of shared nearest neighbors to be considered for learning the subspace properties.");

  /**
   * Parameter to specify the number of shared nearest neighbors to be
   * considered for learning the subspace properties., must be an integer
   * greater than 0.
   * <p>
   * Default value: {@code 1}
   * </p>
   * <p>
   * Key: {@code -sod.knn}
   * </p>
   */
  private final IntParameter KNN_PARAM = new IntParameter(KNN_ID, new GreaterConstraint(0), 1);

  /**
   * Holds the value of {@link #KNN_PARAM}.
   */
  private int knn;

  /**
   * OptionID for {@link #ALPHA_PARAM}
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("sod.alpha", "The multiplier for the discriminance value for discerning small from large variances.");

  /**
   * Parameter to indicate the multiplier for the discriminance value for
   * discerning small from large variances.
   * <p/>
   * <p>
   * Default value: 1.1
   * </p>
   * <p/>
   * <p>
   * Key: {@code -sod.alpha}
   * </p>
   */
  public final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID, new GreaterConstraint(0), 1.1);

  /**
   * Holds the value of {@link #ALPHA_PARAM}.
   */
  private double alpha;

  /**
   * The similarity function.
   */
  private SharedNearestNeighborSimilarityFunction<V, D> similarityFunction;

  /**
   * Provides the SOD algorithm, adding parameters {@link #KNN_PARAM} and
   * {@link #ALPHA_PARAM} to the option handler additionally to parameters of
   * super class.
   */
  public SOD(Parameterization config) {
    super(config);
    if(config.grab(KNN_PARAM)) {
      knn = KNN_PARAM.getValue();
    }
    if(config.grab(ALPHA_PARAM)) {
      alpha = ALPHA_PARAM.getValue();
    }

    similarityFunction = new SharedNearestNeighborSimilarityFunction<V, D>(config);
  }

  /**
   * Performs the SOD algorithm on the given database.
   */
  @Override
  protected OutlierResult runInTime(Database<V> database) throws IllegalStateException {
    FiniteProgress progress = new FiniteProgress("assigning SOD", database.size());
    int processed = 0;
    similarityFunction.setDatabase(database, isVerbose(), isTime());
    if(logger.isVerbose()) {
      logger.verbose("assigning subspace outlier degree:");
    }
    HashMap<Integer, SODModel<?>> sod_models = new HashMap<Integer, SODModel<?>>(database.size());
    for(Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      Integer queryObject = iter.next();
      processed++;
      if(logger.isVerbose()) {
        progress.setProcessed(processed);
        logger.progress(progress);
      }
      List<Integer> knnList = getKNN(database, queryObject).idsToList();
      SODModel<V> model = new SODModel<V>(database, knnList, alpha, database.get(queryObject));
      sod_models.put(queryObject, model);
    }
    // combine results.
    AnnotationResult<SODModel<?>> models = new AnnotationFromHashMap<SODModel<?>>(SOD_MODEL, sod_models);
    OrderingResult ordering = new OrderingFromHashMap<SODModel<?>>(sod_models, true);
    OutlierScoreMeta meta = new ProbabilisticOutlierScore();
    OutlierResult sodResult = new OutlierResult(meta, new SODProxyScoreResult(models), ordering);
    // also add the models.
    sodResult.addResult(models);
    return sodResult;
  }

  /**
   * Provides the k nearest neighbors in terms of the shared nearest neighbor
   * distance.
   * <p/>
   * The query object is excluded from the knn list.
   * 
   * @param database the database holding the objects
   * @param queryObject the query object for which the kNNs should be determined
   * @return the k nearest neighbors in terms of the shared nearest neighbor
   *         distance without the query object
   */
  private KNNList<DoubleDistance> getKNN(Database<V> database, Integer queryObject) {
    // similarityFunction.getPreprocessor().getParameters();
    KNNList<DoubleDistance> kNearestNeighbors = new KNNList<DoubleDistance>(knn, new DoubleDistance(Double.POSITIVE_INFINITY));
    for(Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      Integer id = iter.next();
      if(!id.equals(queryObject)) {
        DoubleDistance distance = new DoubleDistance(1.0 / similarityFunction.similarity(queryObject, id).doubleValue());
        kNearestNeighbors.add(new DistanceResultPair<DoubleDistance>(distance, id));
      }
    }
    return kNearestNeighbors;
  }

  /**
   * 
   * 
   * @author Arthur Zimek
   * @param <O> the type of DatabaseObjects handled by this Result
   */
  //TODO: arthur comment
  public static class SODModel<O extends NumberVector<O, ?>> implements TextWriteable, Comparable<SODModel<?>> {
    private double[] centerValues;

    private O center;

    private double[] variances;

    private double expectationOfVariance;

    private BitSet weightVector;

    private double sod;

    /**
     * 
     * 
     * @param database
     * @param neighborhood
     * @param alpha
     * @param queryObject
     */
    public SODModel(Database<O> database, List<Integer> neighborhood, double alpha, O queryObject) {
      // TODO: store database link?
      centerValues = new double[database.dimensionality()];
      variances = new double[centerValues.length];
      for(Integer id : neighborhood) {
        O databaseObject = database.get(id);
        for(int d = 0; d < centerValues.length; d++) {
          centerValues[d] += databaseObject.doubleValue(d + 1);
        }
      }
      for(int d = 0; d < centerValues.length; d++) {
        centerValues[d] /= neighborhood.size();
      }
      for(Integer id : neighborhood) {
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
      center = queryObject.newInstance(centerValues);
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
      //params.addParameter(AbstractDimensionsSelectingDoubleDistanceFunction.DIMS_ID, weightVector);
      final DimensionsSelectingEuclideanDistanceFunction<O> DISTANCE_FUNCTION = new DimensionsSelectingEuclideanDistanceFunction<O>(params);
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
      try {
        out.commentPrintLn("center of neighborhood: " + out.normalizationRestore(center).toString());
      }
      catch(NonNumericFeaturesException e) {
        e.printStackTrace();
      }
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
   */
  protected static class SODProxyScoreResult implements AnnotationResult<Double> {
    /**
     * Model result this is a proxy for.
     */
    AnnotationResult<SODModel<?>> models;

    /**
     * Constructor.
     * 
     * @param models
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
    public Double getValueFor(Integer objID) {
      return models.getValueFor(objID).getSod();
    }

    @Override
    public String getName() {
      return "sodscores";
    }
  }
}