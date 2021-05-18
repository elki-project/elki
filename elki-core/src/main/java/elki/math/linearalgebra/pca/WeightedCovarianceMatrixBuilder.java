/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2021
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
package elki.math.linearalgebra.pca;

import elki.data.NumberVector;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.ids.DoubleDBIDList;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.PrimitiveDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.math.linearalgebra.Centroid;
import elki.math.linearalgebra.CovarianceMatrix;
import elki.math.linearalgebra.pca.weightfunctions.ConstantWeight;
import elki.math.linearalgebra.pca.weightfunctions.WeightFunction;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * {@link CovarianceMatrixBuilder} with weights.
 * <p>
 * This builder uses a weight function to weight points differently during build
 * a covariance matrix. Covariance can be canonically extended with weights, as
 * shown in the article
 * <p>
 * Reference:
 * <p>
 * A General Framework for Increasing the Robustness of PCA-Based Correlation
 * Clustering Algorithms<br>
 * Hans-Peter Kriegel and Peer Kröger and Erich Schubert and Arthur Zimek<br>
 * Proc. 20th Int. Conf. on Scientific and Statistical Database Management
 * (SSDBM)
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @has - - - WeightFunction
 * @has - - - PrimitiveDistance
 * @assoc - - - CovarianceMatrix
 */
@Title("Weighted Covariance Matrix / PCA")
@Description("A PCA modification by using weights while building the covariance matrix, to obtain more stable results")
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "A General Framework for Increasing the Robustness of PCA-based Correlation Clustering Algorithms", //
    booktitle = "Proc. 20th Intl. Conf. on Scientific and Statistical Database Management (SSDBM)", //
    url = "https://doi.org/10.1007/978-3-540-69497-7_27", //
    bibkey = "DBLP:conf/ssdbm/KriegelKSZ08")
public class WeightedCovarianceMatrixBuilder implements CovarianceMatrixBuilder {
  /**
   * Holds the weight function.
   */
  protected WeightFunction weightfunction;

  /**
   * Holds the distance function used for weight calculation.
   */
  private PrimitiveDistance<? super NumberVector> weightDistance = EuclideanDistance.STATIC;

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
  public double[][] processIds(DBIDs ids, Relation<? extends NumberVector> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    final CovarianceMatrix cmat = new CovarianceMatrix(dim);
    final Centroid centroid = Centroid.make(relation, ids);

    // find maximum distance
    double maxdist = 0.0, stddev = 0.0;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double distance = weightDistance.distance(centroid, relation.get(iter));
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

    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      NumberVector obj = relation.get(iter);
      cmat.put(obj, weightfunction.getWeight(weightDistance.distance(centroid, obj), maxdist, stddev));
    }
    return cmat.destroyToPopulationMatrix();
  }

  /**
   * Compute Covariance Matrix for a QueryResult Collection.
   * 
   * By default it will just collect the ids and run processIds
   * 
   * @param results a collection of QueryResults
   * @param database the database used
   * @param k number of elements to process
   * @return Covariance Matrix
   */
  @Override
  public double[][] processQueryResults(DoubleDBIDList results, Relation<? extends NumberVector> database, int k) {
    final int dim = RelationUtil.dimensionality(database);
    final CovarianceMatrix cmat = new CovarianceMatrix(dim);

    // avoid bad parameters
    k = k <= results.size() ? k : results.size();

    // find maximum distance
    double maxdist = 0.0, stddev = 0.0;
    int i = 0;
    for(DoubleDBIDListIter it = results.iter(); it.valid() && i < k; it.advance(), k++) {
      final double dist = it.doubleValue();
      stddev += dist * dist;
      if(dist > maxdist) {
        maxdist = dist;
      }
    }
    if(maxdist == 0.0) {
      maxdist = 1.0;
    }
    stddev = Math.sqrt(stddev / k);

    // calculate weighted PCA
    for(DoubleDBIDListIter it = results.iter(); it.valid() && it.getOffset() < k; it.advance(), k++) {
      cmat.put(database.get(it), weightfunction.getWeight(it.doubleValue(), maxdist, stddev));
    }
    return cmat.destroyToPopulationMatrix();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the weight function to use in weighted PCA.
     */
    public static final OptionID WEIGHT_ID = new OptionID("pca.weight", "Weight function to use in weighted PCA.");

    /**
     * Weight function.
     */
    protected WeightFunction weightfunction = null;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<WeightFunction>(WEIGHT_ID, WeightFunction.class, ConstantWeight.class) //
          .grab(config, x -> weightfunction = x);
    }

    @Override
    public WeightedCovarianceMatrixBuilder make() {
      return new WeightedCovarianceMatrixBuilder(weightfunction);
    }
  }
}
