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

package elki.clustering.kmeans.covertree;

import java.util.Iterator;

import elki.clustering.kmeans.AbstractKMeans;
import elki.clustering.kmeans.covertree.KMeansCoverTree.Node;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.statistics.LongStatistic;
import elki.math.MathUtil;
import elki.math.linearalgebra.VMath;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * TODO
 *
 * @author Andreas Lang
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
public abstract class AbstractCoverTreeKMeans<V extends NumberVector> extends AbstractKMeans<V, KMeansModel> {

    /**
     * Flag whether to compute the final variance statistic.
     */
    protected boolean varstat = false;

    protected double expansion = 1.3;

    protected int trunc = 10;

    public AbstractCoverTreeKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat, double expansion, int trunc) {
        super(SquaredEuclideanDistance.STATIC, k, maxiter, initializer);
        this.varstat = varstat;
        this.expansion = expansion;
        this.trunc = trunc;
    }

    protected abstract static class Instance extends AbstractKMeans.Instance {
        KMeansCoverTree<? extends NumberVector> tree;

        double[][] cdist;

        double[][] scdist;

        long singletonstatPrune;

        long singletonstatFilter;

        long singletonstatIcDist;

        long nodestatPrune;

        long nodestatFilter;

        long nodestatIcDist;

        NodeManager nodeManager;

        /**
         * Cluster candidate indexes
         */
        // int[] cand;

        /**
         * Constructor.
         *
         * @param relation Relation
         * @param df       Distance function
         * @param means    Initial means
         */
        public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means, KMeansCoverTree<? extends NumberVector> tree) {
            super(relation, df, means);
            this.tree = tree;
            cdist = new double[means.length][means.length];
            scdist = new double[means.length][means.length];
            nodeManager = new NodeManager(k, means[0].length, clusters, assignment, tree, relation);
        }

        protected int pruneD(double[] dists, int[] cand, double fastbound, int alive) {
            // candidate 0 is the neaest and can not be pruned
            for(int i = 1; i < alive;) {
                if(dists[i] > fastbound) {
                    --alive;
                    while(alive > i && dists[alive] > fastbound) {
                        --alive;
                    }
                    swap(dists, i, alive, cand);
                }
                i++;
            }
            return alive;
        }

        protected void swap(int i, int j, int[] cand) {
            final int swap = cand[i];
            cand[i] = cand[j];
            cand[j] = swap;
        }

        protected void swap(double[] dists, int i, int j, int[] cand) {
            final int swap = cand[i];
            cand[i] = cand[j];
            cand[j] = swap;

            final double swapD = dists[i];
            dists[i] = dists[j];
            dists[j] = swapD;
        }

        /**
         * Separation of means.
         *
         * @param cdist  Pairwise separation output (as sqrt/2)
         * @param scdist Pairwise separation output (as squared/4)
         */
        protected void combinedSeperation(double[][] cdist, double[][] scdist) {
            final int k = means.length;
            for(int i = 1; i < k; i++) {
                double[] mi = means[i];
                for(int j = 0; j < i; j++) {
                    scdist[i][j] = scdist[j][i] = 0.25 * distance(mi, means[j]);
                    cdist[i][j] = cdist[j][i] = Math.sqrt(scdist[i][j]);
                }
            }
        }

        public void generateCover() {
            Node root = tree.getRoot();
            generateCover(root);
        }

        public void generateCover(Node cur) {
            int clu = nodeManager.get(cur);
            if(clu >= 0) {
                addNode(cur, clu);
                return;
            }
            for(Node n : cur.children) {
                generateCover(n);
            }
            DBIDIter it = cur.singletons.iter();
            if(cur.children.isEmpty()) {
                clusters.get(nodeManager.get(it)).add(it);
            }
            it.advance();
            for(; it.valid(); it.advance()) {
                clusters.get(nodeManager.get(it)).add(it);
            }
        }

        /**
         * Compute means from cluster sums by averaging.
         * 
         * @param dst  Output means
         * @param sums Input sums
         * @param prev Previous means, to handle empty clusters
         */
        protected void meansFromSumsCT(double[][] dst, double[][] sums, double[][] prev) {
            for(int i = 0; i < k; i++) {
                final int size = nodeManager.getSize(i);
                if(size == 0) {
                    System.arraycopy(prev[i], 0, dst[i], 0, prev[i].length);
                    continue;
                }
                VMath.overwriteTimes(dst[i], sums[i], 1. / size);
            }
        }

        protected int addNode(Node n, int cluster) {
            int changed = 0;
            ModifiableDBIDs collect = DBIDUtil.newHashSet(100); // size
            tree.collectSubtree(n, collect);

            // TODO calc varsum
            ModifiableDBIDs cluList = clusters.get(cluster);
            for(DBIDIter it = collect.iter(); it.valid(); it.advance()) {
                cluList.add(it);
                if(assignment.putInt(it, cluster) != cluster) {
                    ++changed;
                }
            }

            return changed;
        }

        public boolean testSizes() {
            int count = 0;
            for(int i = 0; i < k; i++) {
                count += nodeManager.getSize(i);
            }
            int size = relation.size();
            return count == size;
        }

        public void printLog() {
            Logging log = getLogger();
            log.statistics(new LongStatistic(key + ".Singleton.filter", singletonstatFilter));
            log.statistics(new LongStatistic(key + ".Singleton.prune", singletonstatPrune));
            log.statistics(new LongStatistic(key + ".Singleton.icDist", singletonstatIcDist));

            log.statistics(new LongStatistic(key + ".Node.filter", nodestatFilter));
            log.statistics(new LongStatistic(key + ".Node.prune", nodestatPrune));
            log.statistics(new LongStatistic(key + ".Node.icDist", nodestatIcDist));
        }
    }

    @Override
    protected abstract Logging getLogger();

    /**
     * Parameterization class.
     *
     * @author Andreas Lang
     */
    public abstract static class Par<V extends NumberVector> extends AbstractKMeans.Par<V> {
        /**
         * Truncate branches when they have less than this number of instances.
         */
        public static final OptionID TRUNCATE_ID = new OptionID("covertree.truncate", "Truncate tree when branches have less than this number of instances.");

        /**
         * Expansion rate of the tree (going upward).
         */
        public static final OptionID EXPANSION_ID = new OptionID("covertree.expansionrate", "Expansion rate of the tree (Default: 1.3).");

        double expansion = 1.4;

        int trunc = 10;

        @Override
        protected boolean needsMetric() {
            return true;
        }

        protected void getParameterSlack(Parameterization config) {
            new DoubleParameter(EXPANSION_ID, 1.3) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE) //
                    .grab(config, x -> expansion = x);
            new IntParameter(TRUNCATE_ID, 10) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
                    .grab(config, x -> trunc = x);
        }

        @Override
        public void configure(Parameterization config) {
            super.configure(config);
            super.getParameterVarstat(config);
            getParameterSlack(config);
        }

        @Override
        public abstract AbstractCoverTreeKMeans<V> make();

    }

}
