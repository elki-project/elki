package de.lmu.ifi.dbs.elki.algorithm.outlier.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TiedTopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Subspace Outlier Degree. Outlier detection method for axis-parallel
 * subspaces.
 * 
 * Reference:
 * <p>
 * * H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek:<br />
 * Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data<br />
 * In: Proceedings of the 13th Pacific-Asia Conference on Knowledge Discovery
 * and Data Mining (PAKDD), Bangkok, Thailand, 2009
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.has SODModel oneway - - computes
 * @apiviz.has SharedNearestNeighborSimilarityFunction
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 * @param <D> distance type
 */
// todo arthur comment
@Title("SOD: Subspace outlier degree")
@Description("Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data")
@Reference(authors = "H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", title = "Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data", booktitle = "Proceedings of the 13th Pacific-Asia Conference on Knowledge Discovery and Data Mining (PAKDD), Bangkok, Thailand, 2009", url = "http://dx.doi.org/10.1007/978-3-642-01307-2")
public class SOD<V extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(SOD.class);

  /**
   * Parameter to specify the number of shared nearest neighbors to be
   * considered for learning the subspace properties., must be an integer
   * greater than 0.
   */
  public static final OptionID KNN_ID = OptionID.getOrCreateOptionID("sod.knn", "The number of most snn-similar objects to use as reference set for learning the subspace properties.");

  /**
   * Parameter to indicate the multiplier for the discriminance value for
   * discerning small from large variances.
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("sod.alpha", "The multiplier for the discriminance value for discerning small from large variances.");

  /**
   * Parameter for the similarity function.
   */
  public static final OptionID SIM_ID = OptionID.getOrCreateOptionID("sod.similarity", "The similarity function used for the neighborhood set.");

  /**
   * Holds the value of {@link #KNN_ID}.
   */
  private int knn;

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private double alpha;

  /**
   * The similarity function {@link #SIM_ID}.
   */
  private SimilarityFunction<V, D> similarityFunction;

  /**
   * Constructor with parameters.
   * 
   * @param knn knn value
   * @param alpha Alpha parameter
   * @param similarityFunction Shared nearest neighbor similarity function
   */
  public SOD(int knn, double alpha, SimilarityFunction<V, D> similarityFunction) {
    super();
    this.knn = knn;
    this.alpha = alpha;
    this.similarityFunction = similarityFunction;
  }

  /**
   * Performs the SOD algorithm on the given database.
   * 
   * @param relation Data relation to process
   * @return Outlier result
   */
  public OutlierResult run(Relation<V> relation) {
    SimilarityQuery<V, D> snnInstance = similarityFunction.instantiate(relation);
    FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Assigning Subspace Outlier Degree", relation.size(), LOG) : null;
    WritableDataStore<SODModel<?>> sod_models = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, SODModel.class);
    DoubleMinMax minmax = new DoubleMinMax();
    for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      if (progress != null) {
        progress.incrementProcessed(LOG);
      }
      DBIDs knnList = getNearestNeighbors(relation, snnInstance, iter);
      SODModel<V> model = new SODModel<V>(relation, knnList, alpha, relation.get(iter));
      sod_models.put(iter, model);
      minmax.put(model.getSod());
    }
    if (progress != null) {
      progress.ensureCompleted(LOG);
    }
    // combine results.
    Relation<SODModel<?>> models = new MaterializedRelation<SODModel<?>>("Subspace Outlier Model", "sod-outlier", new SimpleTypeInformation<SODModel<?>>(SODModel.class), sod_models, relation.getDBIDs());
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax());
    OutlierResult sodResult = new OutlierResult(meta, new SODProxyScoreResult(models, relation.getDBIDs()));
    // also add the models.
    sodResult.addChildResult(models);
    return sodResult;
  }

  /**
   * Provides the k nearest neighbors in terms of the shared nearest neighbor
   * distance.
   * <p/>
   * The query object is excluded from the knn list.
   * 
   * @param relation the database holding the objects
   * @param simQ similarity function
   * @param queryObject the query object for which the kNNs should be determined
   * @return the k nearest neighbors in terms of the shared nearest neighbor
   *         distance without the query object
   */
  private DBIDs getNearestNeighbors(Relation<V> relation, SimilarityQuery<V, D> simQ, DBIDRef queryObject) {
    // similarityFunction.getPreprocessor().getParameters();
    Heap<DoubleDBIDPair> nearestNeighbors = new TiedTopBoundedHeap<DoubleDBIDPair>(knn);
    for (DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      if (!DBIDUtil.equal(iter, queryObject)) {
        double sim = simQ.similarity(queryObject, iter).doubleValue();
        if (sim > 0) {
          nearestNeighbors.add(DBIDUtil.newPair(sim, iter));
        }
      }
    }
    // Collect DBIDs
    ArrayModifiableDBIDs dbids = DBIDUtil.newArray(nearestNeighbors.size());
    while (nearestNeighbors.size() > 0) {
      dbids.add(nearestNeighbors.poll());
    }
    return dbids;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * SOD Model class
   * 
   * @author Arthur Zimek
   * @param <V> the type of DatabaseObjects handled by this Result
   */
  // TODO: arthur comment
  public static class SODModel<V extends NumberVector<?>> implements TextWriteable, Comparable<SODModel<?>> {
    private double[] centerValues;

    private V center;

    private double[] variances;

    private double expectationOfVariance;

    private BitSet weightVector;

    private double sod;

    /**
     * Initialize SOD Model
     * 
     * @param relation Database
     * @param neighborhood Neighborhood
     * @param alpha Alpha value
     * @param queryObject Query object
     */
    public SODModel(Relation<V> relation, DBIDs neighborhood, double alpha, V queryObject) {
      if (neighborhood.size() > 0) {
        // TODO: store database link?
        centerValues = new double[RelationUtil.dimensionality(relation)];
        variances = new double[centerValues.length];
        for (DBIDIter iter = neighborhood.iter(); iter.valid(); iter.advance()) {
          V databaseObject = relation.get(iter);
          for (int d = 0; d < centerValues.length; d++) {
            centerValues[d] += databaseObject.doubleValue(d);
          }
        }
        for (int d = 0; d < centerValues.length; d++) {
          centerValues[d] /= neighborhood.size();
        }
        for (DBIDIter iter = neighborhood.iter(); iter.valid(); iter.advance()) {
          V databaseObject = relation.get(iter);
          for (int d = 0; d < centerValues.length; d++) {
            // distance
            double distance = centerValues[d] - databaseObject.doubleValue(d);
            // variance
            variances[d] += distance * distance;
          }
        }
        expectationOfVariance = 0;
        for (int d = 0; d < variances.length; d++) {
          variances[d] /= neighborhood.size();
          expectationOfVariance += variances[d];
        }
        expectationOfVariance /= variances.length;
        weightVector = new BitSet(variances.length);
        for (int d = 0; d < variances.length; d++) {
          if (variances[d] < alpha * expectationOfVariance) {
            weightVector.set(d, true);
          }
        }
        center = RelationUtil.getNumberVectorFactory(relation).newNumberVector(centerValues);
        sod = subspaceOutlierDegree(queryObject, center, weightVector);
      } else {
        center = queryObject;
        sod = 0.0;
      }
    }

    /**
     * Compute SOD score.
     * 
     * @param queryObject Query object
     * @param center Center vector
     * @param weightVector Weight vector
     * @return sod score
     */
    private double subspaceOutlierDegree(V queryObject, V center, BitSet weightVector) {
      final SubspaceEuclideanDistanceFunction df = new SubspaceEuclideanDistanceFunction(weightVector);
      final int card = weightVector.cardinality();
      if (card == 0) {
        return 0;
      }
      double distance = df.distance(queryObject, center).doubleValue();
      distance /= card;
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
  protected static class SODProxyScoreResult implements Relation<Double> {
    /**
     * Model result this is a proxy for.
     */
    Relation<SODModel<?>> models;

    /**
     * The IDs we are defined for.
     */
    DBIDs dbids;

    /**
     * Constructor.
     * 
     * @param models Models result
     * @param dbids IDs we are defined for
     */
    public SODProxyScoreResult(Relation<SODModel<?>> models, DBIDs dbids) {
      super();
      this.models = models;
      this.dbids = dbids;
    }

    @Override
    public Double get(DBIDRef objID) {
      return models.get(objID).getSod();
    }

    @Override
    public String getLongName() {
      return "Subspace Outlier Degree";
    }

    @Override
    public String getShortName() {
      return "sod-outlier";
    }

    @Override
    public DBIDs getDBIDs() {
      return dbids;
    }

    @Override
    public DBIDIter iterDBIDs() {
      return dbids.iter();
    }

    @Override
    public Database getDatabase() {
      return null; // FIXME
    }

    @Override
    public void set(DBIDRef id, Double val) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(DBIDRef id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SimpleTypeInformation<Double> getDataTypeInformation() {
      return TypeUtil.DOUBLE;
    }

    @Override
    public int size() {
      return dbids.size();
    }

    @Override
    public ResultHierarchy getHierarchy() {
      return models.getHierarchy();
    }

    @Override
    public void setHierarchy(ResultHierarchy hierarchy) {
      models.setHierarchy(hierarchy);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>, D extends NumberDistance<D, ?>> extends AbstractParameterizer {
    /**
     * Holds the value of {@link #KNN_ID}.
     */
    private int knn = 1;

    /**
     * Holds the value of {@link #ALPHA_ID}.
     */
    private double alpha = 1.1;

    /**
     * The similarity function - {@link #SIM_ID}.
     */
    private SimilarityFunction<V, D> similarityFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<SimilarityFunction<V, D>> simP = new ObjectParameter<SimilarityFunction<V, D>>(SIM_ID, SimilarityFunction.class, SharedNearestNeighborSimilarityFunction.class);
      if (config.grab(simP)) {
        similarityFunction = simP.instantiateClass(config);
      }

      final IntParameter knnP = new IntParameter(KNN_ID);
      knnP.addConstraint(new GreaterConstraint(0));
      if (config.grab(knnP)) {
        knn = knnP.getValue();
      }

      final DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 1.1);
      alphaP.addConstraint(new GreaterConstraint(0));
      if (config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }
    }

    @Override
    protected SOD<V, D> makeInstance() {
      return new SOD<V, D>(knn, alpha, similarityFunction);
    }
  }
}
