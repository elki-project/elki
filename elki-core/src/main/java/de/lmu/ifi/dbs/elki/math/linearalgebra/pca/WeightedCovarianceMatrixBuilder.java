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
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ConstantWeight;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.WeightFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import net.jafama.FastMath;

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
 * @has - - - PrimitiveDistanceFunction
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
  private PrimitiveDistanceFunction<? super NumberVector> weightDistance = EuclideanDistanceFunction.STATIC;

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
    {
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
      stddev = FastMath.sqrt(stddev / ids.size());
    }

    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      NumberVector obj = relation.get(iter);
      double distance = weightDistance.distance(centroid, obj);
      double weight = weightfunction.getWeight(distance, maxdist, stddev);
      cmat.put(obj, weight);
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
    {
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
      stddev = FastMath.sqrt(stddev / k);
    }

    // calculate weighted PCA
    int i = 0;
    for(DoubleDBIDListIter it = results.iter(); it.valid() && i < k; it.advance(), k++) {
      final double dist = it.doubleValue();
      NumberVector obj = database.get(it);
      double weight = weightfunction.getWeight(dist, maxdist, stddev);
      cmat.put(obj, weight);
    }
    return cmat.destroyToPopulationMatrix();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the weight function to use in weighted PCA.
     */
    public static final OptionID WEIGHT_ID = new OptionID("pca.weight", "Weight function to use in weighted PCA.");

    /**
     * Weight function.
     */
    protected WeightFunction weightfunction = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<WeightFunction> weightfunctionP = new ObjectParameter<>(WEIGHT_ID, WeightFunction.class, ConstantWeight.class);
      if(config.grab(weightfunctionP)) {
        weightfunction = weightfunctionP.instantiateClass(config);
      }
    }

    @Override
    protected WeightedCovarianceMatrixBuilder makeInstance() {
      return new WeightedCovarianceMatrixBuilder(weightfunction);
    }
  }
}
