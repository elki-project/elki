package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ConstantWeight;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.WeightFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * {@link CovarianceMatrixBuilder} with weights.
 * 
 * This builder uses a weight function to weight points differently during build
 * a covariance matrix. Covariance can be canonically extended with weights, as
 * shown in the article
 * 
 * A General Framework for Increasing the Robustness of PCA-Based Correlation
 * Clustering Algorithms Hans-Peter Kriegel and Peer Kr&ouml;ger and Erich
 * Schubert and Arthur Zimek In: Proc. 20th Int. Conf. on Scientific and
 * Statistical Database Management (SSDBM), 2008, Hong Kong Lecture Notes in
 * Computer Science 5069, Springer
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has WeightFunction
 * @apiviz.has PrimitiveDistanceFunction
 * @apiviz.uses CovarianceMatrix
 * 
 * @param <V> Vector class to use
 */
@Title("Weighted Covariance Matrix / PCA")
@Description("A PCA modification by using weights while building the covariance matrix, to obtain more stable results")
@Reference(authors = "H.-P. Kriegel, P. Kröger, E. Schubert, A. Zimek", title = "A General Framework for Increasing the Robustness of PCA-based Correlation Clustering Algorithms", booktitle = "Proceedings of the 20th International Conference on Scientific and Statistical Database Management (SSDBM), Hong Kong, China, 2008", url = "http://dx.doi.org/10.1007/978-3-540-69497-7_27")
public class WeightedCovarianceMatrixBuilder<V extends NumberVector<?>> extends AbstractCovarianceMatrixBuilder<V> {
  /**
   * Parameter to specify the weight function to use in weighted PCA, must
   * implement
   * {@link de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.WeightFunction}
   * .
   * <p>
   * Key: {@code -pca.weight}
   * </p>
   */
  public static final OptionID WEIGHT_ID = OptionID.getOrCreateOptionID("pca.weight", "Weight function to use in weighted PCA.");

  /**
   * Holds the weight function.
   */
  protected WeightFunction weightfunction;

  /**
   * Holds the distance function used for weight calculation.
   */
  // TODO: make configurable?
  private PrimitiveDistanceFunction<? super V, DoubleDistance> weightDistance = EuclideanDistanceFunction.STATIC;

  /**
   * Constructor.
   * 
   * @param weightfunction Weighting function
   */
  public WeightedCovarianceMatrixBuilder(WeightFunction weightfunction) {
    super();
    this.weightfunction = weightfunction;
  }

  /**
   * Weighted Covariance Matrix for a set of IDs. Since we are not supplied any
   * distance information, we'll need to compute it ourselves. Covariance is
   * tied to Euclidean distance, so it probably does not make much sense to add
   * support for other distance functions?
   * 
   * @param ids Database ids to process
   * @param relation Relation to process
   * @return Covariance matrix
   */
  @Override
  public Matrix processIds(DBIDs ids, Relation<? extends V> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    final CovarianceMatrix cmat = new CovarianceMatrix(dim);
    final V centroid = Centroid.make(relation, ids).toVector(relation);

    // find maximum distance
    double maxdist = 0.0;
    double stddev = 0.0;
    {
      for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        V obj = relation.get(iter);
        double distance = weightDistance.distance(centroid, obj).doubleValue();
        stddev += distance * distance;
        if(distance > maxdist) {
          maxdist = distance;
        }
      }
      if(maxdist == 0.0) {
        maxdist = 1.0;
      }
      // compute standard deviation.
      stddev = Math.sqrt(stddev / ids.size());
    }

    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      V obj = relation.get(iter);
      double distance = weightDistance.distance(centroid, obj).doubleValue();
      double weight = weightfunction.getWeight(distance, maxdist, stddev);
      cmat.put(obj, weight);
    }
    return cmat.destroyToNaiveMatrix();
  }

  /**
   * Compute Covariance Matrix for a QueryResult Collection.
   * 
   * By default it will just collect the ids and run processIds
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @param k number of elements to process
   * @param <D> distance type
   * @return Covariance Matrix
   */
  @Override
  public <D extends NumberDistance<D, ?>> Matrix processQueryResults(DistanceDBIDResult<D> results, Relation<? extends V> database, int k) {
    final int dim = RelationUtil.dimensionality(database);
    final CovarianceMatrix cmat = new CovarianceMatrix(dim);

    // avoid bad parameters
    if(k > results.size()) {
      k = results.size();
    }

    // find maximum distance
    double maxdist = 0.0;
    double stddev = 0.0;
    {
      int i = 0;
      for (DistanceDBIDResultIter<D> it = results.iter(); it.valid() && i < k; it.advance(), k++) {
        DistanceDBIDPair<D> res = it.getDistancePair();
        final double dist;
        if(res instanceof DoubleDistanceDBIDPair) {
          dist = ((DoubleDistanceDBIDPair) res).doubleDistance();
        }
        else {
          dist = res.getDistance().doubleValue();
        }
        stddev += dist * dist;
        if(dist > maxdist) {
          maxdist = dist;
        }
      }
      if(maxdist == 0.0) {
        maxdist = 1.0;
      }
      stddev = Math.sqrt(stddev / k);
    }

    // calculate weighted PCA
    int i = 0;
    for (DistanceDBIDResultIter<D> it = results.iter(); it.valid() && i < k; it.advance(), k++) {
      DistanceDBIDPair<D> res = it.getDistancePair();
      final double dist;
      if(res instanceof DoubleDistanceDBIDPair) {
        dist = ((DoubleDistanceDBIDPair) res).doubleDistance();
      }
      else {
        dist = res.getDistance().doubleValue();
      }

      V obj = database.get(res);
      double weight = weightfunction.getWeight(dist, maxdist, stddev);
      cmat.put(obj, weight);
    }
    return cmat.destroyToNaiveMatrix();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Weight function.
     */
    protected WeightFunction weightfunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<WeightFunction> weightfunctionP = new ObjectParameter<WeightFunction>(WEIGHT_ID, WeightFunction.class, ConstantWeight.class);
      if(config.grab(weightfunctionP)) {
        weightfunction = weightfunctionP.instantiateClass(config);
      }
    }

    @Override
    protected WeightedCovarianceMatrixBuilder<V> makeInstance() {
      return new WeightedCovarianceMatrixBuilder<V>(weightfunction);
    }
  }
}