package de.lmu.ifi.dbs.elki.algorithm.outlier;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.DimensionsSelectingEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.IntegerDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SharedNearestNeighborSimilarityFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriteable;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriterStream;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNList;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
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
 */
// todo arthur comment
@Title("SOD: Subspace outlier degree")
@Description("Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data")
@Reference(authors = "H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", title = "Outlier Detection in Axis-Parallel Subspaces of High Dimensional Data", booktitle = "Proceedings of the 13th Pacific-Asia Conference on Knowledge Discovery and Data Mining (PAKDD), Bangkok, Thailand, 2009", url = "http://dx.doi.org/10.1007/978-3-642-01307-2")
public class SOD<V extends NumberVector<V, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SOD.class);

  /**
   * Parameter to specify the number of shared nearest neighbors to be
   * considered for learning the subspace properties., must be an integer
   * greater than 0.
   */
  public static final OptionID KNN_ID = OptionID.getOrCreateOptionID("sod.knn", "The number of shared nearest neighbors to be considered for learning the subspace properties.");

  /**
   * Parameter to indicate the multiplier for the discriminance value for
   * discerning small from large variances.
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("sod.alpha", "The multiplier for the discriminance value for discerning small from large variances.");

  /**
   * Holds the value of {@link #KNN_ID}.
   */
  private int knn;

  /**
   * Holds the value of {@link #ALPHA_ID}.
   */
  private double alpha;

  /**
   * The similarity function.
   */
  private SharedNearestNeighborSimilarityFunction<V> similarityFunction;

  /**
   * Constructor with parameters.
   * 
   * @param knn knn value
   * @param alpha Alpha parameter
   * @param similarityFunction Shared nearest neighbor similarity function
   */
  public SOD(int knn, double alpha, SharedNearestNeighborSimilarityFunction<V> similarityFunction) {
    super();
    this.knn = knn;
    this.alpha = alpha;
    this.similarityFunction = similarityFunction;
  }

  /**
   * Performs the SOD algorithm on the given database.
   * 
   * @param relation Data relation to process
   */
  public OutlierResult run(Relation<V> relation) throws IllegalStateException {
    SimilarityQuery<V, IntegerDistance> snnInstance = similarityFunction.instantiate(relation);
    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Assigning Subspace Outlier Degree", relation.size(), logger) : null;
    WritableDataStore<SODModel<?>> sod_models = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, SODModel.class);
    DoubleMinMax minmax = new DoubleMinMax();
    for(Iterator<DBID> iter = relation.iterDBIDs(); iter.hasNext();) {
      DBID queryObject = iter.next();
      if(progress != null) {
        progress.incrementProcessed(logger);
      }
      DBIDs knnList = getKNN(relation, snnInstance, queryObject).asDBIDs();
      SODModel<V> model = new SODModel<V>(relation, knnList, alpha, relation.get(queryObject));
      sod_models.put(queryObject, model);
      minmax.put(model.getSod());
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
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
   * @param database the database holding the objects
   * @param snnInstance similarity function
   * @param queryObject the query object for which the kNNs should be determined
   * @return the k nearest neighbors in terms of the shared nearest neighbor
   *         distance without the query object
   */
  private KNNList<DoubleDistance> getKNN(Relation<V> database, SimilarityQuery<V, IntegerDistance> snnInstance, DBID queryObject) {
    // similarityFunction.getPreprocessor().getParameters();
    KNNHeap<DoubleDistance> kNearestNeighbors = new KNNHeap<DoubleDistance>(knn, new DoubleDistance(Double.POSITIVE_INFINITY));
    for(Iterator<DBID> iter = database.iterDBIDs(); iter.hasNext();) {
      DBID id = iter.next();
      if(!id.equals(queryObject)) {
        double distance = 1.0 / snnInstance.similarity(queryObject, id).doubleValue();
        kNearestNeighbors.add(new DoubleDistanceResultPair(distance, id));
      }
    }
    return kNearestNeighbors.toKNNList();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger;
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
    public SODModel(Relation<O> database, DBIDs neighborhood, double alpha, O queryObject) {
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
      center = DatabaseUtil.assumeVectorField(database).getFactory().newInstance(centerValues);
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
      final DimensionsSelectingEuclideanDistanceFunction df = new DimensionsSelectingEuclideanDistanceFunction(weightVector);
      double distance = df.distance(queryObject, center).doubleValue();
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
  protected static class SODProxyScoreResult implements Relation<Double> {
    /**
     * Model result this is a proxy for.
     */
    Relation<SODModel<?>> models;
    
    /**
     * The IDs we are defined for
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
    public Double get(DBID objID) {
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
    public IterableIterator<DBID> iterDBIDs() {
      return IterableUtil.fromIterator(dbids.iterator());
    }

    @Override
    public Database getDatabase() {
      return null; // FIXME
    }

    @Override
    public void set(DBID id, Double val) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(DBID id) {
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
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    /**
     * Holds the value of {@link #KNN_ID}.
     */
    private int knn = 1;

    /**
     * Holds the value of {@link #ALPHA_ID}.
     */
    private double alpha = 1.1;

    /**
     * The similarity function.
     */
    private SharedNearestNeighborSimilarityFunction<V> similarityFunction;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter knnP = new IntParameter(KNN_ID, new GreaterConstraint(0), 1);
      if(config.grab(knnP)) {
        knn = knnP.getValue();
      }

      final DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, new GreaterConstraint(0), 1.1);
      if(config.grab(alphaP)) {
        alpha = alphaP.getValue();
      }

      Class<SharedNearestNeighborSimilarityFunction<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(SharedNearestNeighborSimilarityFunction.class);
      similarityFunction = config.tryInstantiate(cls);
    }

    @Override
    protected SOD<V> makeInstance() {
      return new SOD<V>(knn, alpha, similarityFunction);
    }
  }
}