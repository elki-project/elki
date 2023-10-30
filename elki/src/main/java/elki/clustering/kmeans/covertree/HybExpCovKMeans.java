/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2023
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

package elki.clustering.kmeans.covertree;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Combines Cover Tree k-means with the Exponion algorithm
 *
 * @author Andreas Lang
 *
 * @param <V> vector datatype
 */
public class HybExpCovKMeans<V extends NumberVector> extends HybHamCovKMeans<V> {

  /**
   * Constructor
   * 
   * @param k           Number of clusters
   * @param maxiter     maximum number of iterations
   * @param initializer k-means initialization
   * @param varstat     variance
   * @param expansion   expansion factor of cover tree
   * @param trunc       truncate theshold for cover tree
   * @param switchover  Iteration for switching strategies
   */
    public HybExpCovKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat, double expansion, int trunc, int switchover) {
        super(k, maxiter, initializer, varstat, expansion, trunc, switchover);
    }

    /**
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(HybExpCovKMeans.class);

    @Override
    public Clustering<KMeansModel> run(Relation<V> relation) {
        KMeansCoverTree<V> tree = new KMeansCoverTree<V>(relation, EuclideanDistance.STATIC, expansion, trunc, true);
        tree.initialize();
        Instance instance = new Instance(relation, distance, initialMeans(relation), tree, switchover);
        instance.run(maxiter);
        instance.generateCover();
        instance.printLog();
        return instance.buildResult(varstat, relation);
    }

    @Override
    protected Logging getLogger() {
        return LOG;
    }

    /**
     * Inner Class for k-means
     * 
     * @author Andreas Lang
     */
    protected static class Instance extends HybHamCovKMeans.Instance {

        /**
       * Sorted neighbors
       */
        int[] cnum[];

        /**
         * Constructor
         * 
         * @param relation   Relation
         * @param df         distance function
         * @param means      cluster centers
         * @param tree       cover tree
         * @param switchover point of strategy switch
         */
        public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means, KMeansCoverTree<? extends NumberVector> tree, int switchover) {
            super(relation, df, means, tree, switchover);
            cnum = new int[k][k - 1];
        }

        @Override
        protected int iterate(int iteration) {
            if(iteration == 1) {
                int changed = initialAssignToNearestCluster();
                return changed;
            }
            if(iteration < switchover) {
                meansFromSumsCT(means, nodeManager.getSums(), means);
                int changed = assignToNearestCluster();
                assert (testSizes());
                assert (nodeManager.testTree(tree.getRoot(), false));
                return changed;
            }
            if(iteration == switchover) {
                meansFromSumsCT(means, nodeManager.getSums(), means);
                int changed = assignToClusterBounds();
                assert (testSizes());
                return changed;
            }
            meansFromSumsCT(newmeans, nodeManager.getSums(), means);
            movedDistance(means, newmeans, sep);
            updateBounds(sep);
            copyMeans(newmeans, means);
            return assignPointsToNearestCluster();
        }

        @Override
        protected int assignPointsToNearestCluster() {
            recomputeSeperation(sep, cdist);
            nearestMeans(cdist, cnum);
            int changed = 0;
            for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
              final int orig = assignment.intValue(it);
              // Compute the current bound:
              final double z = lower.doubleValue(it);
              final double sa = sep[orig];
              double u = upper.doubleValue(it);
              if(u <= z || u <= sa) {
                continue;
              }
              // Update the upper bound
              NumberVector fv = relation.get(it);
              double curd2 = distance(fv, means[orig]);
              upper.putDouble(it, u = isSquared ? Math.sqrt(curd2) : curd2);
              if(u <= z || u <= sa) {
                continue;
              }
              double rhalf = u + sa; // Our cdist are scaled 0.5
              // Find closest center, and distance to two closest centers
              double min1 = curd2, min2 = Double.POSITIVE_INFINITY;
              int cur = orig;
              for(int i = 0; i < k - 1; i++) {
                final int c = cnum[orig][i]; // Optimized ordering
                if(cdist[orig][c] > rhalf) {
                  break;
                }
                double dist = distance(fv, means[c]);
                if(dist < min1) {
                  cur = c;
                  min2 = min1;
                  min1 = dist;
                }
                else if(dist < min2) {
                  min2 = dist;
                }
              }
              // Object has to be reassigned.
              if(cur != orig) {
                nodeManager.fChange(it, fv, orig, cur);
                ++changed;
                upper.putDouble(it, min1 == curd2 ? u : isSquared ? Math.sqrt(min1) : min1);
              }
              lower.putDouble(it, min2 == curd2 ? u : isSquared ? Math.sqrt(min2) : min2);
            }
            return changed;
        }

        @Override
        protected Logging getLogger() {
            return LOG;
        }

    }

    /**
     * Parameterization class.
     *
     * @author Andreas Lang
     */
    public static class Par<V extends NumberVector> extends HybHamCovKMeans.Par<V> {

        @Override
        public void configure(Parameterization config) {
            super.configure(config);
        }

        @Override
        public HybExpCovKMeans<V> make() {
            return new HybExpCovKMeans<>(k, maxiter, initializer, varstat, expansion, trunc, switchover);
        }
    }

}
