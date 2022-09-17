/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.clustering.kmeans.spherical;

import java.util.Arrays;
import java.util.List;

import elki.clustering.kmeans.AbstractKMeans;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.model.KMeansModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.CosineDistance;
import elki.logging.Logging;
import elki.math.linearalgebra.VMath;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * The standard spherical k-means algorithm.
 * <p>
 * Reference:
 * <p>
 * I. S. Dhillon, D. S. Modha<br>
 * Concept Decompositions for Large Sparse Text Data Using Clustering<br>
 * Machine Learning 42
 *
 * @author Alexander Voß
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
@Reference(authors = "I. S. Dhillon, D. S. Modha", //
    title = "Concept Decompositions for Large Sparse Text Data Using Clustering", //
    booktitle = "Machine Learning 42", url = "https://doi.org/10.1023/A:1007612920971", //
    bibkey = "DBLP:journals/ml/DhillonM01")
public class SphericalKMeans<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SphericalKMeans.class);

  /**
   * Constructor.
   *
   * @param k Number of clusters
   * @param maxiter Maximum number of iterations
   * @param initializer Initialization class
   */
  public SphericalKMeans(int k, int maxiter, KMeansInitialization initializer) {
    super(CosineDistance.STATIC, k, maxiter, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> relation) {
    Instance instance = new Instance(relation, initialMeans(relation));
    instance.run(maxiter);
    return instance.buildResult();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Alexander Voß
   */
  public static class Instance extends AbstractKMeans.Instance {
    /**
     * Constructor.
     *
     * @param relation Data relation
     * @param means Initial cluster means
     */
    public Instance(Relation<? extends NumberVector> relation, double[][] means) {
      super(relation, CosineDistance.STATIC, means);
      // ensure the initial means have the full dimensionality:
      final int dim = RelationUtil.maxDimensionality(relation);
      for(int i = 0; i < means.length; i++) {
        if(means[i].length < dim) {
          means[i] = Arrays.copyOf(means[i], dim);
        }
      }
    }

    @Override
    public int iterate(int iteration) {
      means = iteration == 1 ? means : means(clusters, means, relation);
      return assignToNearestCluster();
    }

    @Override
    protected int assignToNearestCluster() {
      assert k == means.length;
      int changed = 0;
      // Reset all clusters
      Arrays.fill(varsum, 0.);
      for(ModifiableDBIDs cluster : clusters) {
        cluster.clear();
      }
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        NumberVector fv = relation.get(iditer);
        double maxSim = VectorUtil.dot(fv, means[0]);
        ++diststat;
        int maxIndex = 0;
        for(int i = 1; i < k; i++) {
          double sim = VectorUtil.dot(fv, means[i]);
          ++diststat;
          if(sim > maxSim) {
            maxIndex = i;
            maxSim = sim;
          }
        }
        varsum[maxIndex] += maxSim < 1 ? 2 * (1 - maxSim) : 0;
        clusters.get(maxIndex).add(iditer);
        if(assignment.putInt(iditer, maxIndex) != maxIndex) {
          ++changed;
        }
      }
      return changed;
    }

    /**
     * Compute the similarity of two objects (and count this operation).
     *
     * @param vec1 First vector
     * @param vec2 Second vector
     * @return Similarity (dot product)
     */
    protected double similarity(NumberVector vec1, double[] vec2) {
      diststat++;
      return Math.min(1, VectorUtil.dot(vec1, vec2));
    }

    /**
     * Compute the similarity of two objects (and count this operation).
     *
     * @param vec1 First vector
     * @param vec2 Second vector
     * @return Similarity (dot product)
     */
    protected double similarity(double[] vec1, double[] vec2) {
      diststat++;
      return Math.min(1, VMath.dot(vec1, vec2));
    }

    @Override
    protected double distance(double[] x, double[] y) {
      ++diststat;
      double d = 0;
      for(int i = 0; i < x.length; i++) {
        double v = x[i] - y[i];
        d += v * v;
      }
      return d > 0 ? d : 0;
    }

    @Override
    protected double distance(NumberVector x, double[] y) {
      ++diststat;
      final double s = VectorUtil.dot(x, y);
      return s < 1 ? 2 - 2 * s : 0;
    }

    @Override
    protected double distance(NumberVector x, NumberVector y) {
      ++diststat;
      final double s = VectorUtil.dot(x, y);
      return s < 1 ? 2 - 2 * s : 0;
    }

    @Override
    protected double sqrtdistance(NumberVector x, double[] y) {
      ++diststat;
      final double s = VectorUtil.dot(x, y);
      return s < 1 ? Math.sqrt(2 - 2 * s) : 0;
    }

    @Override
    protected double sqrtdistance(NumberVector x, NumberVector y) {
      ++diststat;
      final double s = VectorUtil.dot(x, y);
      return s < 1 ? Math.sqrt(2 - 2 * s) : 0;
    }

    /**
     * Initial separation of means. Used by Elkan and Hamerly.
     *
     * @param ccsim Output square root of pairwise separation
     */
    protected void initialSeparation(double[][] ccsim) {
      final int k = means.length;
      for(int i = 1; i < k; i++) {
        double[] mi = means[i];
        for(int j = 0; j < i; j++) {
          double s = similarity(mi, means[j]);
          ccsim[i][j] = ccsim[j][i] = s > -1 ? Math.sqrt((s + 1) * 0.5) : 0;
        }
      }
    }

    /**
     * Similarity to previous locations.
     * <p>
     * Used by Hamerly, (Simplified)Elkan.
     *
     * @param means Old means
     * @param newmeans New means
     * @param sims Similarities moved (output)
     */
    protected void movedSimilarity(double[][] means, double[][] newmeans, double[] sims) {
      assert newmeans.length == means.length && sims.length == means.length;
      for(int i = 0; i < means.length; i++) {
        sims[i] = similarity(means[i], newmeans[i]);
      }
    }

    /**
     * Compute means from cluster sums by adding and normalizing.
     * 
     * @param dst Output means
     * @param sums Input sums
     * @param prev Previous means (to handle empty clusters)
     */
    @Override
    protected void meansFromSums(double[][] dst, double[][] sums, double[][] prev) {
      for(int i = 0; i < dst.length; i++) {
        final double w = VMath.euclideanLength(sums[i]);
        if(!(w > 1e-7)) { // Empty cluster
          System.arraycopy(prev[i], 0, dst[i], 0, prev[i].length);
          continue;
        }
        VMath.overwriteTimes(dst[i], sums[i], 1. / w);
      }
    }

    @Override
    protected void recomputeVariance(Relation<? extends NumberVector> relation) {
      Arrays.fill(varsum, 0.);
      for(int i = 0; i < clusters.size(); i++) {
        DBIDs ids = clusters.get(i);
        double ssum = 0;
        double[] mean = means[i];
        for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
          ssum += Math.min(1, VectorUtil.dot(relation.get(it), mean));
          ++diststat;
        }
        varsum[i] = 2 * (ids.size() - ssum);
      }
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }

    /**
     * Returns the mean vectors of the given clusters in the given database.
     *
     * @param clusters the clusters to compute the means
     * @param means the recent means
     * @param relation the database containing the vectors
     * @return the mean vectors of the given clusters in the given database
     */
    protected static double[][] means(List<? extends DBIDs> clusters, double[][] means, Relation<? extends NumberVector> relation) {
      final int k = means.length, dim = means[0].length;
      double[][] newMeans = new double[k][];
      for(int i = 0; i < k; i++) {
        DBIDs list = clusters.get(i);
        if(list.isEmpty()) {
          // Keep degenerated means as-is for now.
          newMeans[i] = means[i];
          continue;
        }
        double[] sum = new double[dim];
        for(DBIDIter iter = list.iter(); iter.valid(); iter.advance()) {
          plusEquals(sum, relation.get(iter));
        }
        // normalize to unit length
        newMeans[i] = VMath.normalizeEquals(sum);
      }
      return newMeans;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Alexander Voß
   */
  public static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {
    @Override
    public void configure(Parameterization config) {
      getParameterK(config);
      getParameterInitialization(config);
      getParameterMaxIter(config);
    }

    @Override
    public SphericalKMeans<V> make() {
      return new SphericalKMeans<V>(k, maxiter, initializer);
    }
  }
}
