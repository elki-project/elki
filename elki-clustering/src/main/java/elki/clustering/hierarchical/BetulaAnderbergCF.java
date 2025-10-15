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
package elki.clustering.hierarchical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import elki.Algorithm;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.distance.CFDistance;
import elki.index.tree.betula.distance.CentroidEuclideanDistance;
import elki.index.tree.betula.features.ClusterFeature;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * BIRCH/BETULA-based clustering algorithm that builds on Anderbergs
 * Hierarchical Clustering, using the leaves of the CFTree
 * as smalles elements. This variant uses Cluster Feature 
 * based distances instead of the Lance-Williams Equations.
 * <p>
 * References:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 * 
 * @author Andreas Lang
 *
 * @depend - - - CFTree
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class BetulaAnderbergCF implements HierarchicalClusteringAlgorithm {
    /**
     * CFTree factory.
     */
    CFTree.Factory<?> cffactory;

    protected int end;

    /**
     * Cache: best distance
     */
    protected double[] bestd;

    /**
     * Cache: index of best distance
     */
    protected int[] besti;

    ClusterDistanceMatrix mat;

    ClusterMergeHistoryBuilder builder;

    /**
     * Distance function used.
     */
    protected CFDistance distance;

    private ArrayList<? extends ClusterFeature> cfs;

    /**
     * Class logger
     */
    private static final Logging LOG = Logging.getLogger(BetulaAnderbergCF.class);

    /**
     * Constructor.
     *
     * @param cffactory CFTree Factory
     * @param distance distance function
     */
    public BetulaAnderbergCF(CFTree.Factory<?> cffactory, CFDistance distance) {
        super();
        this.cffactory = cffactory;
        this.distance = distance;
    }

    @Override
    public TypeInformation[] getInputTypeRestriction() {
        return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
    }

    /**
     * Run the algorithm
     *
     * @param relation Relation
     * @return Clustering hierarchy
     */
    public ClusterMergeHistory run(Relation<NumberVector> relation) {
        final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
        CFTree<?> tree = cffactory.newTree(ids, relation, true);
        cfs = tree.getLeaves();

        ArrayList<DBIDs> idList = new ArrayList<>();
        double[] dists = new double[cfs.size()];
        ListIterator<? extends ClusterFeature> lit = cfs.listIterator();
        int i = 0;
        while(lit.hasNext()) {
            ClusterFeature cf = lit.next();
            idList.add(tree.getDBIDs(cf));
            dists[i] = cf.variance() / cf.getWeight();
            i++;
        }

        this.mat = BetulaAnderberg.initializeDistanceMatrix(cfs, distance);
        this.end = cfs.size();
        this.builder = BetulaAnderberg.initializeHistoryBuilder(idList, relation.size(), dists, mat.clustermap, false);
        this.end = cfs.size();
        this.bestd = new double[cfs.size()];
        this.besti = new int[cfs.size()];

        initializeNNCache(mat.matrix, bestd, besti);

        // Repeat until everything merged into 1 cluster
        FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", cfs.size() - 1, LOG) : null;
        // Use end to shrink the matrix virtually as the tailing objects
        // disappear
        for(int j = 1; j < cfs.size(); j++) {
            end = shrinkActiveSet(mat.clustermap, end, findMerge());
            LOG.incrementProcessed(prog);
        }
        LOG.ensureCompleted(prog);

        return builder.complete();
    }

    protected static int shrinkActiveSet(int[] clustermap, int end, int x) {
        if(x == end - 1) { // Can truncate active set.
            while(clustermap[--end - 1] < 0) {
                // decrement happens in while condition already.
            }
        }
        return end;
    }

    /**
     * Initialize the NN cache.
     *
     * @param scratch Scratch space
     * @param bestd Best distance
     * @param besti Best index
     */
    protected static void initializeNNCache(double[] scratch, double[] bestd, int[] besti) {
        final int size = bestd.length;
        Arrays.fill(bestd, Double.POSITIVE_INFINITY);
        Arrays.fill(besti, -1);
        besti[0] = Integer.MAX_VALUE; // invalid, but not deactivated
        int p = 0;
        for(int x = 1; x < size; x++) {
            double bestdx = Double.POSITIVE_INFINITY;
            int bestix = -1;
            for(int y = 0; y < x; y++) {
                final double v = scratch[p++];
                if(v < bestdx) {
                    bestdx = v;
                    bestix = y;
                }
            }
            assert 0 <= bestix && bestix < x;
            bestd[x] = bestdx;
            besti[x] = bestix;
        }
        assert p == ClusterDistanceMatrix.triangleSize(size);
    }

    /**
     * Perform the next merge step in AGNES.
     *
     * @return the index that has disappeared, for shrinking the working set
     */
    private int findMerge() {
        double mindist = Double.POSITIVE_INFINITY;
        int x = -1, y = -1;
        for(int cx = 1; cx < end; cx++) {
            // Skip if object has already joined a cluster:
            final int cy = besti[cx];
            if(cy < 0) {
                continue;
            }
            final double mdist = bestd[cx];
            if(mdist <= mindist) { // Prefer later on ==, to truncate more
                                   // often.
                mindist = mdist;
                x = cx;
                y = cy;
            }
        }
        assert 0 <= y && y < x;
        merge(mindist, x, y);
        return x;
    }

    /**
     * Execute the cluster merge.
     *
     * @param mindist Distance that was used for merging
     * @param x First matrix position
     * @param y Second matrix position
     */
    protected void merge(double mindist, int x, int y) {
        // Avoid allocating memory, by reusing existing iterators:
        if(LOG.isDebuggingFine()) {
            LOG.debugFine("Merging: " + cfs.get(x) + " -> " + cfs.get(y) + " " + mindist);
        }
        // Perform merge in data structure: x -> y
        assert (y < x);
        final int xx = mat.clustermap[x], yy = mat.clustermap[y];
        // final int sizex = builder.getSize(xx), sizey = builder.getSize(yy);
        // Since y < x, prefer keeping y, dropping x.
        int zz = builder.strictAdd(xx, mindist, yy);
        mat.clustermap[y] = zz;
        mat.clustermap[x] = besti[x] = -1; // Deactivate removed cluster.
        // Update cluster size for y:
        cfs.get(y).addToStatistics(cfs.get(x));
        updateMatrix( x, y);
        if(y > 0) {
            findBest(mat.matrix, bestd, besti, y);
        }
    }

    /**
     * Update the scratch distance matrix.
     *
     * @param end Active set size
     * @param mat Matrix view
     * @param builder Hierarchy builder (to get cluster sizes)
     * @param mindist Distance that was used for merging
     * @param x First matrix position
     * @param y Second matrix position
     * @param sizex Old size of first cluster
     * @param sizey Old size of second cluster
     */
    private void updateMatrix(int x, int y) {
        // Update distance matrix. Note: y < x
        final int ybase = ClusterDistanceMatrix.triangleSize(y);
        double[] scratch = mat.matrix;
        // Write to (y, j), with j < y
        int j = 0;
        for(; j < y; j++) {
            if(mat.clustermap[j] < 0) {
                continue;
            }
            assert (j < y); // Otherwise, ybase + j is the wrong position!
            final int yb = ybase + j;
            final double d = scratch[yb] = distance.squaredDistance(cfs.get(y), cfs.get(j));
            updateCache(scratch, bestd, besti, x, y, j, d);
        }
        j++; // Skip y
        // Write to (j, y), with y < j < x
        int jbase = ClusterDistanceMatrix.triangleSize(j);
        for(; j < x; jbase += j++) {
            if(mat.clustermap[j] < 0) {
                continue;
            }
            final int jb = jbase + y;
            final double d = scratch[jb] = distance.squaredDistance(cfs.get(y), cfs.get(j));
            updateCache(scratch, bestd, besti, x, y, j, d);
        }
        jbase += j++; // Skip x
        // Write to (j, y), with y < x < j
        for(; j < end; jbase += j++) {
            if(mat.clustermap[j] < 0) {
                continue;
            }
            final int jb = jbase + y;
            final double d = scratch[jb] = distance.squaredDistance(cfs.get(y), cfs.get(j));
            updateCache(scratch, bestd, besti, x, y, j, d);
        }
    }

    /**
     * Update the cache.
     *
     * @param scratch Scratch matrix
     * @param bestd Best distance
     * @param besti Best index
     * @param x First cluster
     * @param y Second cluster, {@code y < x}
     * @param j Updated value d(y, j)
     * @param d New distance
     */
    protected static void updateCache(double[] scratch, double[] bestd, int[] besti, int x, int y, int j, double d) {
        assert y < x;
        // New best
        if(y < j && d <= bestd[j]) {
            bestd[j] = d;
            besti[j] = y;
            return;
        }
        // Needs slow update.
        if(besti[j] == x || besti[j] == y) {
            findBest(scratch, bestd, besti, j);
        }
    }

    /**
     * Find the best in a row of the triangular matrix.
     *
     * @param scratch Scratch matrix
     * @param bestd Best distances cache
     * @param besti Best indexes cache
     * @param j Row to update
     */
    protected static void findBest(double[] scratch, double[] bestd, int[] besti, int j) {
        // The distance has increased, we may no longer be the best merge.
        double bestdj = Double.POSITIVE_INFINITY;
        int bestij = -1;
        for(int i = 0,
                o = ClusterDistanceMatrix.triangleSize(j); i < j; i++, o++) {
            if(besti[i] < 0) {
                continue;
            }
            final double dist = scratch[o];
            if(dist <= bestdj) {
                bestdj = dist;
                bestij = i;
            }
        }
        assert bestij < j;
        bestd[j] = bestdj;
        besti[j] = bestij;
    }

    /**
     * Parameterization class.
     *
     * @author Andreas Lang
     */
    public static class Par implements Parameterizer {
        /**
         * Ignore cluster weights (naive approach)
         */
        public static final OptionID IGNORE_WEIGHT_ID = new OptionID("betulaAnderbergCF.naive", "Treat leaves as single points, not weighted points.");

        /**
         * The distance function to use.
         */
        protected CFDistance distance;

        /**
         * CFTree factory.
         */
        CFTree.Factory<?> cffactory;

        /**
         * Ignore weight
         */
        // boolean ignoreWeight = false;

        @Override
        public void configure(Parameterization config) {
            cffactory = config.tryInstantiate(CFTree.Factory.class);
            // new Flag(IGNORE_WEIGHT_ID).grab(config, x -> ignoreWeight = x);
            new ObjectParameter<CFDistance>(Algorithm.Utils.DISTANCE_FUNCTION_ID, CFDistance.class, CentroidEuclideanDistance.class) //
                    .grab(config, x -> distance = x);
        }

        @Override
        public BetulaAnderbergCF make() {
            return new BetulaAnderbergCF(cffactory, distance);
        }
    }
}
