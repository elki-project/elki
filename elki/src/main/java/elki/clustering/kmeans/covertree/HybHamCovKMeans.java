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

import java.util.Arrays;

import elki.clustering.kmeans.covertree.KMeansCoverTree.Node;
import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.math.MathUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Combines Cover Tree k-means with the Hamerly algorithm
 *
 * @author Andreas Lang
 *
 * @param <V> vector datatype
 */
public class HybHamCovKMeans<V extends NumberVector> extends CoverTreeKMeans<V> {

    /**
     * stores iteration of th estrategy switch
     */
    int switchover;

    /**
     * Constructor
     * 
     * @param k Number of clusters
     * @param maxiter maximum number of iterations
     * @param initializer k-means initialization
     * @param varstat variance 
     * @param expansion expansion factor of cover tree
     * @param trunc truncate theshold for cover tree
     * @param switchover Iteration for switching strategies
     */
    public HybHamCovKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat, double expansion, int trunc, int switchover) {
        super(k, maxiter, initializer, varstat, expansion, trunc);
        this.switchover = switchover;
    }

    /**
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(HybHamCovKMeans.class);

    @Override
    public Clustering<KMeansModel> run(Relation<V> relation) {
        KMeansCoverTree<V> tree = new KMeansCoverTree<V>(relation, EuclideanDistance.STATIC, expansion, trunc, true);
        tree.initialize();
        Instance instance = new Instance(relation, distance, initialMeans(relation), tree, switchover);
        instance.run(maxiter);
        instance.materializeClusters();
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
    protected static class Instance extends CoverTreeKMeans.Instance {

        /**
         * Upper bounds
         */
        WritableDoubleDataStore upper;

        /**
         * Lower bounds
         */
        WritableDoubleDataStore lower;

        /**
         * tmp storage for calc of new mean
         */
        double[][] newmeans;

        /**
         * Separation of means / distance moved.
         */
        double[] sep;

        /**
         * iteration for switch of strategy
         */
        int switchover;

        /**
         * Constructor
         * 
         * @param relation Relation
         * @param df distance function
         * @param means cluster centers
         * @param tree cover tree
         * @param switchover point of strategy switch
         */
        public Instance(Relation<? extends NumberVector> relation, NumberVectorDistance<?> df, double[][] means, KMeansCoverTree<? extends NumberVector> tree, int switchover) {
            super(relation, df, means, tree);
            this.switchover = switchover;
            upper = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.POSITIVE_INFINITY);
            lower = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, 0.);
            newmeans = new double[k][means[0].length];
            sep = new double[means.length];
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

        /**
         * K-means iteration on the tree with assignment of bounds for Hamerly's algorithm
         * 
         * @return Number of points that switched clusters
         */
        protected int assignToClusterBounds() {
            combinedSeperation(cdist, scdist);
            Node root = tree.getRoot();
            return assignNodeBounds(root, k, -1, Double.POSITIVE_INFINITY, new double[k], MathUtil.sequence(0, k));
        }

        /**
         * Assign node to cluster (recursively) and add bounds
         * 
         * @param cur Node
         * @param alive Number of cluster candidates for the nodes
         * @param oldass old assignment of the node
         * @param radius radius of parent
         * @param parentdists distances from parent to candidates
         * @param cand list of candidate clusters
         * @return Number of newly assigned elements
         */
        protected int assignNodeBounds(Node cur, int alive, int oldass, double radius, double[] parentdists, int[] cand) {
            if(oldass == -1) {
                oldass = nodeManager.get(cur);
            }
            int changed = 0;
            double fastbound = 0.;
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
                    return addBound(cur, oldass, cand[0], Math.sqrt(min1) + cur.maxDist, min2 - radius);
                }
                newbound = min2 + 2 * radius;
                newbound *= newbound;
                // int oldAlive = alive;
                // Equation 15 based on 14
                alive = pruneD(parentdists, cand, newbound, alive);
                dists = new double[alive];
                dists[0] = min1;
                min2 = Double.POSITIVE_INFINITY;
                for(int i = 1; i < alive; i++) {
                    // use sqrt values but handle maxdist > cdist + min2
                    double bound = 0;
                    if(cur.maxDist < cdist[cand[minInd]][cand[i]]) {
                        bound = cdist[cand[minInd]][cand[i]] - cur.maxDist;
                        bound *= bound;
                    }
                    if(min2 >= bound) {
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
                        dists[i] = Double.POSITIVE_INFINITY;
                    }
                }
                if(isSquared) {
                    dists[minInd] = min1 = Math.sqrt(min1);
                    dists[min2Ind] = min2 = Math.sqrt(min2);
                }
                // end of calculation for new routing element
                fastbound = min1 + 2 * cur.maxDist;
                if(fastbound < min2) {
                    nodestatFilter += alive * (cur.size - cur.singletons.size());
                    assert (nodeManager.testAssign(cur, cand[minInd], means) == 0);
                    return addBound(cur, oldass, cand[minInd], min1 + cur.maxDist, min2 - cur.maxDist);
                }
            }
            else {
                dists = parentdists;
                fastbound = min1 + 2 * cur.maxDist;
                if(fastbound < min2) {
                    nodestatFilter += alive * (cur.size - cur.singletons.size());
                    assert (nodeManager.testAssign(cur, cand[minInd], means) == 0);
                    return addBound(cur, oldass, cand[minInd], min1 + cur.maxDist, min2 - radius);
                }
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
            fastbound = min2 + 2 * cur.maxDist;
            fastbound *= fastbound;
            int old = alive;
            alive = pruneD(dists, cand, fastbound, alive);
            nodestatPrune += (old - alive) * (cur.size - cur.singletons.size());

            // Assign routing object if in leaf node
            if(cur.children.isEmpty()) {
                int myoldass = oldass != -1 ? oldass : nodeManager.get(it);
                assert (nodeManager.testAssign(it, cand[0], means) == 0);
                changed += addBound(it, relation.get(it), myoldass, cand[0], min1, min2);
            }
            // assign other singletons
            it.advance();
            fastbound = 0.5 * (min2 - min1);
            changed += assignSingletonsBounds(cur, oldass, fastbound, it, dists, cand, alive, min1, min2);

            // Assign children
            for(Node child : cur.children) {
                int myoldass = oldass != -1 ? oldass : nodeManager.get(child);
                // assign to cluster candidate if patent dist + maxDist <
                // lower_bound
                if(child.parentDist + child.maxDist < fastbound) {
                    nodestatFilter += child.size * alive;
                    changed += addBound(child, myoldass, cand[0], min1 + child.parentDist + child.maxDist, min2 - child.parentDist - child.maxDist);
                    assert (nodeManager.testAssign(child, cand[0], means) == 0);
                }
                else {
                    // TODO change radius if necessary
                    changed += assignNodeBounds(child, alive, oldass, cur.maxDist, dists, cand);
                }
            }
            return changed;
        }

        /**
         * Assign Singleton points and set bounds
         * 
         * @param cur Current Node
         * @param oldass Parent old assignment when applicable 
         * @param fastbound bound for assignment
         * @param it singleton iterator
         * @param dists distances from parent to cluster candidates
         * @param cand list of cluster candidates
         * @param alive number of cluster candidates
         * @param min1 distance from parent to the closest cluster center
         * @param min2 distance from parent to the second closest cluster center
         * @return number of reassigned points
         */
        protected int assignSingletonsBounds(Node cur, int oldass, double fastbound, DBIDIter it, double[] dists, int[] cand, int alive, double min1, double min2) {
            int changed = 0;
            for(int j = 1; it.valid(); it.advance(), j++) {
                int myoldass = oldass != -1 ? oldass : nodeManager.get(it);
                if(cur.singletons.doubleValue(j) <= fastbound) {
                    singletonstatFilter += alive;
                    assert (nodeManager.testAssign(it, cand[0], means) == 0);
                    changed += addBound(it, relation.get(it), myoldass, cand[0], min1 + cur.singletons.doubleValue(j), min2 - cur.singletons.doubleValue(j));
                    continue;
                }
                NumberVector fv = relation.get(it);
                double minS1 = distance(fv, means[cand[0]]);
                double minS2 = distance(fv, means[cand[1]]);
                int sMinInd = 0;
                if(minS2 < minS1) {
                    double t = minS2;
                    minS2 = minS1;
                    minS1 = t;
                    sMinInd = 1;
                }
                double mybound = Math.sqrt(minS2) + cur.singletons.doubleValue(j);
                mybound *= mybound;
                int sal = pruneD(dists, cand, mybound, alive);

                singletonstatPrune += alive - sal;

                for(int i = 2; i < sal; i++) {
                    if(minS2 >= scdist[cand[sMinInd]][cand[i]]) {
                        final double dist = distance(fv, means[cand[i]]);
                        if(dist < minS1) {
                            sMinInd = i;
                            minS2 = minS1;
                            minS1 = dist;
                        }
                        if(dist < minS2) {
                            minS2 = dist;
                        }
                    }
                    else {
                        singletonstatIcDist++;
                    }
                }
                assert (nodeManager.testAssign(it, cand[sMinInd], means) == 0);
                changed += addBound(it, fv, myoldass, cand[sMinInd], Math.sqrt(minS1), Math.sqrt(minS2));
            }
            return changed;
        }

        /**
         * Use of the hybrid strategy for all emements
         * 
         * @return Number of reassigned points
         */
        protected int assignPointsToNearestCluster() {
            recomputeSeperation(sep);
            int changed = 0;
            for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
                final int orig = assignment.intValue(it);
                // Compute the current bound:
                final double l = lower.doubleValue(it);
                final double sa = sep[orig];
                double u = upper.doubleValue(it);
                if(u <= l || u <= sa) {
                    continue;
                }
                // Update the upper bound
                NumberVector fv = relation.get(it);
                double curd2 = distance(fv, means[orig]);
                upper.putDouble(it, u = isSquared ? Math.sqrt(curd2) : curd2);
                if(u <= l || u <= sa) {
                    continue;
                }
                // Find closest center, and distance to the second closest
                // center
                double min1 = curd2, min2 = Double.POSITIVE_INFINITY;
                int cur = orig;
                for(int i = 0; i < k; i++) {
                    if(i == orig) {
                        continue;
                    }
                    double dist = distance(fv, means[i]);
                    if(dist < min1) {
                        cur = i;
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

        /**
         * Add Node to Cluster and set bound
         * 
         * @param n Node
         * @param oldass Old Assignment
         * @param clu target cluster
         * @param u upper bound
         * @param l lower bound
         * @return return number of changed elements
         */
        protected int addBound(Node n, int oldass, int clu, double u, double l) {
            // nodemanager first
            int changed = nodeManager.change(n, oldass, clu);
            ModifiableDBIDs collect = DBIDUtil.newHashSet(n.size); // size
            tree.collectSubtree(n, collect);
            if(l == Double.POSITIVE_INFINITY) {
                l = 0.;
            }
            for(DBIDIter it = collect.iter(); it.valid(); it.advance()) {
                upper.putDouble(it, u);
                lower.putDouble(it, l);
                assignment.put(it, clu);
                assert (testUpper(it, u) == 0);
                assert (testLower(it, u, l, clu) == 0);
            }
            return changed;
        }

        /**
         * Add Element to Cluster and store bounds
         * 
         * @param id Element Id
         * @param fv Object
         * @param oldass old assignment
         * @param clu target cluster
         * @param u upper bound
         * @param l lower bound
         * @return 1 if assignment changed else 0
         */
        protected int addBound(DBIDRef id, NumberVector fv, int oldass, int clu, double u, double l) {
            int changed = nodeManager.change(id, fv, oldass, clu);
            if(l == Double.POSITIVE_INFINITY) {
                l = 0;
            }
            upper.putDouble(id, u);
            lower.putDouble(id, l);
            assert (testUpper(id, u) == 0);
            assert (testLower(id, u, l, clu) == 0);
            return changed;
        }

        /**
         * Update Bounds based on cluser movement
         * 
         * @param move The distances all cluster centers moved
         */
        protected void updateBounds(double[] move) {
            // Find the maximum and second largest movement.
            int most = 0;
            double delta = move[0], delta2 = 0;
            for(int i = 1; i < move.length; i++) {
                final double m = move[i];
                if(m > delta) {
                    delta2 = delta;
                    delta = move[most = i];
                }
                else if(m > delta2) {
                    delta2 = m;
                }
            }
            for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
                final int a = assignment.intValue(it);
                upper.increment(it, move[a]);
                lower.increment(it, a == most ? -delta2 : -delta);
            }
        }

        /**
         * Recompute the separation of cluster means.
         * <p>
         * Used by Hamerly.
         *
         * @param sep Output array of separation (half-sqrt scaled)
         */
        protected void recomputeSeperation(double[] sep) {
            final int k = means.length;
            assert sep.length == k;
            Arrays.fill(sep, Double.POSITIVE_INFINITY);
            for(int i = 1; i < k; i++) {
                double[] m1 = means[i];
                for(int j = 0; j < i; j++) {
                    double d = distance(m1, means[j]);
                    sep[i] = (d < sep[i]) ? d : sep[i];
                    sep[j] = (d < sep[j]) ? d : sep[j];
                }
            }
            // We need half the Euclidean distance
            for(int i = 0; i < k; i++) {
                sep[i] = .5 * (isSquared ? Math.sqrt(sep[i]) : sep[i]);
            }
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
    public static class Par<V extends NumberVector> extends CoverTreeKMeans.Par<V> {

        public static final OptionID SWITCH_ID = new OptionID("covertree.switch", "Switches from covertree to Hamerly.");

        int switchover;

        @Override
        protected boolean needsMetric() {
            return true;
        }

        protected void getParameterSwitch(Parameterization config) {
            new IntParameter(SWITCH_ID, 5) //
                    .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
                    .grab(config, x -> switchover = x);
        }

        @Override
        public void configure(Parameterization config) {
            super.configure(config);
            getParameterSwitch(config);
        }

        @Override
        public HybHamCovKMeans<V> make() {
            return new HybHamCovKMeans<>(k, maxiter, initializer, varstat, expansion, trunc, switchover);
        }
    }

}
