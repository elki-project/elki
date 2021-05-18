/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.outlier.subspace;

import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.similarity.SimilarityQuery;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.DoubleMinMax;
import elki.math.Mean;
import elki.math.linearalgebra.Centroid;
import elki.math.linearalgebra.VMath;
import elki.outlier.OutlierAlgorithm;
import elki.result.Metadata;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.result.textwriter.TextWriteable;
import elki.result.textwriter.TextWriterStream;
import elki.similarity.SharedNearestNeighborSimilarity;
import elki.similarity.Similarity;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Subspace Outlier Degree: Outlier Detection in Axis-Parallel Subspaces of High
 * Dimensional Data.
 * <p>
 * Reference:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek:<br>
 * Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data<br>
 * Proc. Pacific-Asia Conf. on Knowledge Discovery and Data Mining (PAKDD 2009)
 * 
 * @author Arthur Zimek
 * @since 0.2
 * 
 * @navhas - computes - SODModel
 * @has - - - SharedNearestNeighborSimilarity
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("SOD: Subspace outlier degree")
@Description("Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data")
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data", //
    booktitle = "Proc. Pacific-Asia Conf. on Knowledge Discovery and Data Mining (PAKDD 2009)", //
    url = "https://doi.org/10.1007/978-3-642-01307-2_86", //
    bibkey = "DBLP:conf/pakdd/KriegelKSZ09")
public class SOD<V extends NumberVector> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SOD.class);

  /**
   * Neighborhood size.
   */
  private int knn;

  /**
   * Alpha (discriminance value).
   */
  private double alpha;

  /**
   * Similarity function to use.
   */
  private Similarity<V> similarityFunction;

  /**
   * Report models.
   */
  private boolean models;

  /**
   * Constructor with parameters.
   * 
   * @param knn knn value
   * @param alpha Alpha parameter
   * @param similarityFunction Shared nearest neighbor similarity function
   * @param models Report generated models
   */
  public SOD(int knn, double alpha, Similarity<V> similarityFunction, boolean models) {
    super();
    this.knn = knn;
    this.alpha = alpha;
    this.similarityFunction = similarityFunction;
    this.models = models;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Performs the SOD algorithm on the given database.
   * 
   * @param relation Data relation to process
   * @return Outlier result
   */
  public OutlierResult run(Relation<V> relation) {
    SimilarityQuery<V> snnInstance = similarityFunction.instantiate(relation);
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Assigning Subspace Outlier Degree", relation.size(), LOG) : null;
    WritableDoubleDataStore sod_scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    WritableDataStore<SODModel> sod_models = models ? DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, SODModel.class) : null;
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      DBIDs neighborhood = getNearestNeighbors(relation, snnInstance, iter);

      double[] center;
      long[] weightVector = null;
      double sod = 0.;
      if(neighborhood.size() > 0) {
        center = Centroid.make(relation, neighborhood).getArrayRef();
        // Note: per-dimension variances; no covariances.
        double[] variances = computePerDimensionVariances(relation, center, neighborhood);
        double expectationOfVariance = Mean.of(variances);
        weightVector = BitsUtil.zero(variances.length);
        for(int d = 0; d < variances.length; d++) {
          if(variances[d] < alpha * expectationOfVariance) {
            BitsUtil.setI(weightVector, d);
          }
        }
        sod = subspaceOutlierDegree(relation.get(iter), center, weightVector);
      }
      else {
        center = relation.get(iter).toArray();
      }

      if(sod_models != null) {
        sod_models.put(iter, new SODModel(center, weightVector));
      }
      sod_scores.putDouble(iter, sod);
      minmax.put(sod);
      LOG.incrementProcessed(progress);
    }
    LOG.ensureCompleted(progress);
    // combine results.
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    OutlierResult sodResult = new OutlierResult(meta, new MaterializedDoubleRelation("Subspace Outlier Degree", relation.getDBIDs(), sod_scores));
    if(sod_models != null) {
      Metadata.hierarchyOf(sodResult).addChild(new MaterializedRelation<>("Subspace Outlier Model", new SimpleTypeInformation<>(SODModel.class), relation.getDBIDs(), sod_models));
    }
    return sodResult;
  }

  /**
   * Get the k nearest neighbors in terms of the shared nearest neighbor
   * distance.
   * <p>
   * The query object is excluded from the knn list.
   * <p>
   * FIXME: move this to the database layer.
   * 
   * @param relation the database holding the objects
   * @param simQ similarity function
   * @param queryObject the query object for which the kNNs should be determined
   * @return the k nearest neighbors in terms of the shared nearest neighbor
   *         distance without the query object
   */
  private DBIDs getNearestNeighbors(Relation<V> relation, SimilarityQuery<V> simQ, DBIDRef queryObject) {
    KNNHeap nearestNeighbors = DBIDUtil.newHeap(knn);
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      if(DBIDUtil.equal(iter, queryObject)) {
        continue;
      }
      nearestNeighbors.insert(-simQ.similarity(queryObject, iter), iter);
    }
    // Reduce result to DBIDs
    return nearestNeighbors.unorderedIterator().addTo(DBIDUtil.newArray(nearestNeighbors.size()));
  }

  /**
   * Compute the per-dimension variances for the given neighborhood and center.
   * 
   * @param relation Data relation
   * @param center Center vector
   * @param neighborhood Neighbors
   * @return Per-dimension variances.
   */
  private static double[] computePerDimensionVariances(Relation<? extends NumberVector> relation, double[] center, DBIDs neighborhood) {
    final int dim = center.length;
    double[] variances = new double[dim];
    for(DBIDIter iter = neighborhood.iter(); iter.valid(); iter.advance()) {
      NumberVector databaseObject = relation.get(iter);
      for(int d = 0; d < dim; d++) {
        final double deviation = databaseObject.doubleValue(d) - center[d];
        variances[d] += deviation * deviation;
      }
    }
    return VMath.timesEquals(variances, 1. / neighborhood.size());
  }

  /**
   * Compute SOD score.
   * 
   * @param queryObject Query object
   * @param center Center vector
   * @param weightVector Weight vector
   * @return sod score
   */
  private double subspaceOutlierDegree(V queryObject, double[] center, long[] weightVector) {
    double sqrDist = 0;
    int card = 0;
    for(int d = BitsUtil.nextSetBit(weightVector, 0); d >= 0; d = BitsUtil.nextSetBit(weightVector, d + 1)) {
      final double delta = queryObject.doubleValue(d) - center[d];
      sqrDist += delta * delta;
      card++;
    }
    return sqrDist > 0 ? Math.sqrt(sqrDist) / card : 0.;
  }

  /**
   * SOD Model class
   * 
   * @author Arthur Zimek
   */
  public static class SODModel implements TextWriteable {
    /**
     * Center vector
     */
    private double[] center;

    /**
     * Relevant dimensions.
     */
    private long[] weightVector;

    /**
     * Initialize SOD Model
     * 
     * @param center Center vector
     * @param weightVector Selected dimensions
     */
    public SODModel(double[] center, long[] weightVector) {
      this.center = center;
      this.weightVector = weightVector;
    }

    @Override
    public void writeToText(TextWriterStream out, String label) {
      out.commentPrintLn(this.getClass().getSimpleName() + ":");
      out.commentPrintLn("relevant attributes (starting with 0): " + BitsUtil.toString(weightVector, ", ", 0));
      out.commentPrintLn("center of neighborhood: " + center.toString());
      out.commentPrintSeparator();
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Parameter to specify the number of shared nearest neighbors to be
     * considered for learning the subspace properties, must be an integer
     * greater than 0.
     */
    public static final OptionID KNN_ID = new OptionID("sod.knn", "The number of most snn-similar objects to use as reference set for learning the subspace properties.");

    /**
     * Parameter to indicate the multiplier for the discriminance value for
     * discerning small from large variances.
     */
    public static final OptionID ALPHA_ID = new OptionID("sod.alpha", "The multiplier for the discriminance value for discerning small from large variances.");

    /**
     * Parameter for the similarity function.
     */
    public static final OptionID SIM_ID = new OptionID("sod.similarity", "The similarity function used for the neighborhood set.");

    /**
     * Parameter for keeping the models.
     */
    public static final OptionID MODELS_ID = new OptionID("sod.models", "Report the models computed by SOD (default: report only scores).");

    /**
     * Neighborhood size.
     */
    private int knn = 1;

    /**
     * Alpha (discriminance value).
     */
    private double alpha = 1.1;

    /**
     * The similarity function.
     */
    private Similarity<V> similarityFunction;

    /**
     * Track models.
     */
    private boolean models = false;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Similarity<V>>(SIM_ID, Similarity.class, SharedNearestNeighborSimilarity.class) //
          .grab(config, x -> similarityFunction = x);
      new IntParameter(KNN_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> knn = x);
      new DoubleParameter(ALPHA_ID, 1.1) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> alpha = x);
      new Flag(MODELS_ID).grab(config, x -> models = x);
    }

    @Override
    public SOD<V> make() {
      return new SOD<>(knn, alpha, similarityFunction, models);
    }
  }
}
