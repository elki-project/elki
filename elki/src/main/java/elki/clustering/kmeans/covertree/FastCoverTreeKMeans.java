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
package elki.clustering.kmeans.covertree;

import java.util.Arrays;

import elki.clustering.kmeans.covertree.KMeansCoverTree.Node;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.math.MathUtil;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Uses squared inter cluster distances for singleton distance calc and sqrt
 * inter cluster distances for node distance calc.
 *
 * @author Andreas Lang
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
public class FastCoverTreeKMeans<V extends NumberVector> extends AbstractCoverTreeKMeans<V> {

    public FastCoverTreeKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat, double expansion, int trunc) {
        super(k, maxiter, initializer, varstat, expansion, trunc);
    }

    /**
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(FastCoverTreeKMeans.class);

    @Override
    public Clustering<KMeansModel> run(Relation<V> relation) {
        KMeansCoverTree<V> tree = new KMeansCoverTree<V>(relation, EuclideanDistance.STATIC, expansion, trunc, true);
        tree.initialize();
        Instance instance = new Instance(relation, distance, initialMeans(relation), tree);
        instance.run(maxiter);
        instance.generateCover();
        instance.printLog();
        return instance.buildResult(varstat, relation);
    }

    @Override
    protected Logging getLogger() {
        return LOG;
    }

    protected static class Instance extends AbstractCoverTreeKMeans.Instance {

        /**
         * Constructor.
         *
         * @param relation Relation
         * @param df       Distance function
         * @param means    Initial means
         */
        public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means, KMeansCoverTree<? extends NumberVector> tree) {
            super(relation, df, means, tree);
        }

        @Override
        protected int iterate(int iteration) {
            if(iteration == 1) {
                int changed = initialAssignToNearestCluster();
                return changed;
            }
            meansFromSumsCT(means, nodeManager.getSums(), means);
            int changed = assignToNearestCluster();
            assert (testSizes());
            assert (nodeManager.testTree(tree.getRoot(), false));
            return changed;
        }

        @Override
        protected int assignToNearestCluster() {
            combinedSeperation(cdist, scdist);
            Node root = tree.getRoot();

            return assignNode(root, k, -1, Double.POSITIVE_INFINITY, new double[k], MathUtil.sequence(0, k));
        }

        protected int initialAssignToNearestCluster() {
            combinedSeperation(cdist, scdist);
            Node root = tree.getRoot();

            return assignNode(root, k, -2, Double.POSITIVE_INFINITY, new double[k], MathUtil.sequence(0, k));
        }

        protected int assignNode(Node cur, int alive, int oldass, double radius, double[] parentdists, int[] cand) {
            if(oldass == -1) {
                oldass = nodeManager.get(cur);
                assert (nodeManager.testTree(cur, false));
            }
            int changed = 0;
            DBIDIter it = cur.singletons.iter(); // Routing object
            // calculate new bound if node routing element has changed
            double min1 = parentdists[0];
            double min2 = parentdists[1];
            int minInd = 0;
            int min2Ind = 1;
            double[] dists;
            if(cur.parentDist != 0 || radius == Double.POSITIVE_INFINITY) {
                NumberVector fv = relation.get(it); // Routing object number
                                                    // vector
                min1 = distance(fv, means[cand[0]]);
                double newbound = Math.sqrt(min1) + cur.parentDist + 2 * cur.maxDist;
                if(newbound < min2) { // Equation 14
                    assert (nodeManager.testAssign(cur, cand[0], means) == 0);
                    return nodeManager.change(cur, oldass, cand[0]);
                }
                newbound *= newbound;
                // int oldAlive = alive;
                // Equation 15 based on 14
                alive = pruneD(parentdists, cand, newbound, alive);
                dists = new double[alive];
                dists[0] = min1;
                min2 = Double.MAX_VALUE;
                for(int i = 1; i < alive; i++) {
                    // use sqrt values but handle maxdist > cdist + min2
                    double bound = 0;
                    if(cur.maxDist < cdist[cand[minInd]][cand[i]]) {
                        bound = cdist[cand[minInd]][cand[i]] - cur.maxDist;
                        bound *= bound;
                    }
                    if(min1 >= bound) {
                        dists[i] = distance(fv, means[cand[i]]);
                        if(dists[i] < min1) {
                            min2Ind = minInd;
                            minInd = i;
                            min2 = min1;
                            min1 = dists[i];
                        }
                        else if(dists[i] < min2) {
                            min2Ind = i;
                            min2 = dists[i];
                        }
                    }
                    else {
                        nodestatIcDist++;
                        dists[i] = Double.MAX_VALUE;
                    }
                }
                if(isSquared) {
                    dists[minInd] = min1 = Math.sqrt(min1);
                    dists[min2Ind] = min2 = Math.sqrt(min2);
                }
                // end of calculation for new routing element
            }
            else {
                dists = parentdists;
            }
            double fastbound = min1 + 2 * cur.maxDist;
            if(fastbound < min2) {
                nodestatFilter += alive * (cur.size - cur.singletons.size());
                assert (nodeManager.testAssign(cur, cand[minInd], means) == 0);
                return nodeManager.change(cur, oldass, cand[minInd]);
            }
            if(dists != parentdists) {
                cand = Arrays.copyOfRange(cand, 0, alive);
            }
            if(min2Ind != 1) {
                swap(dists, min2Ind, 1, cand);
                if(minInd == 1) {
                    minInd = min2Ind;
                }
            }
            if(minInd != 0) {
                swap(dists, minInd, 0, cand);
            }
            // Prune candidate list
            fastbound *= fastbound;
            int old = alive;
            alive = pruneD(dists, cand, fastbound, alive);
            nodestatPrune += (old - alive) * (cur.size - cur.singletons.size());

            // Assign routing object if in leaf node
            if(cur.children.isEmpty()) {
                int myoldass = oldass != -1 ? oldass : nodeManager.get(it);
                assert (nodeManager.testAssign(it, cand[0], means) == 0);
                changed += nodeManager.change(it, relation.get(it), myoldass, cand[0]);
            }
            // assign other singletons
            it.advance();
            fastbound = 0.5 * (min2 - min1);
            changed += assignSingletons(cur, oldass, fastbound, it, dists, cand, alive);

            // Assign children
            for(Node child : cur.children) {
                int myoldass = oldass != -1 ? oldass : nodeManager.get(child);
                // assign to cluster candidate if patent dist + maxDist <
                // lower_bound
                if(child.parentDist + child.maxDist < fastbound) {
                    nodestatFilter += child.size * alive;
                    changed += nodeManager.change(child, myoldass, cand[0]);
                    assert (nodeManager.testAssign(child, cand[0], means) == 0);
                }
                else {
                    // TODO change radius if necessary
                    changed += assignNode(child, alive, oldass, cur.maxDist, dists, cand);
                    assert (nodeManager.testTree(child, false));
                }
            }
            assert (nodeManager.testTree(cur, false));
            return changed;
        }

        protected int assignSingletons(Node cur, int oldass, double fastbound, DBIDIter it, double[] dists, int[] cand, int alive) {
            int changed = 0;
            for(int j = 1; it.valid(); it.advance(), j++) {
                int myoldass = oldass != -1 ? oldass : nodeManager.get(it);
                if(cur.singletons.doubleValue(j) <= fastbound) {
                    singletonstatFilter += alive;
                    assert (nodeManager.testAssign(it, cand[0], means) == 0);
                    nodeManager.change(it, relation.get(it), myoldass, cand[0]);
                }
                else {
                    NumberVector fv = relation.get(it);
                    double min = distance(fv, means[cand[0]]);
                    double mybound = Math.sqrt(min) + cur.singletons.doubleValue(j);
                    mybound *= mybound;
                    int sal = pruneD(dists, cand, mybound, alive);
                    singletonstatPrune += alive - sal;
                    int sMinInd = 0;
                    for(int i = 1; i < sal; i++) {
                        if(min >= scdist[cand[sMinInd]][cand[i]]) {
                            final double dist = distance(fv, means[cand[i]]);
                            if(dist < min) {
                                sMinInd = i;
                                min = dist;
                            }
                        }
                    }
                    assert (nodeManager.testAssign(it, cand[sMinInd], means) == 0);
                    changed += nodeManager.change(it, fv, myoldass, cand[sMinInd]);
                }
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
    public static class Par<V extends NumberVector> extends AbstractCoverTreeKMeans.Par<V> {
        @Override
        protected boolean needsMetric() {
            return true;
        }

        @Override
        public void configure(Parameterization config) {
            super.configure(config);
        }

        @Override
        public FastCoverTreeKMeans<V> make() {
            return new FastCoverTreeKMeans<>(k, maxiter, initializer, varstat, expansion, trunc);
        }
    }
}
