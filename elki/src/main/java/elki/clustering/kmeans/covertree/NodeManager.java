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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import elki.clustering.kmeans.covertree.KMeansCoverTree.Node;
import elki.data.NumberVector;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;

public class NodeManager {
    protected List<List<Node>> tmpClusters;

    protected int[] tmpAssignment;

    /**
     * Store the elements per cluster.
     */
    protected List<ModifiableDBIDs> clusters;

    /**
     * A mapping of elements to cluster ids.
     */
    protected WritableIntegerDataStore assignment;

    /**
     * Sum aggregate for the new mean.
     */
    double[][] sums;

    int[] sizes;

    int nodecount;

    Relation<? extends NumberVector> relation;

    KMeansCoverTree<?> tree;

    public NodeManager(int k, int dim, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, KMeansCoverTree<?> tree, Relation<? extends NumberVector> relation) {
        this.clusters = clusters;
        this.assignment = assignment;
        this.relation = relation;
        this.tree = tree;
        tmpClusters = new ArrayList<>(k);
        for(int i = 0; i < k; i++) {
            tmpClusters.add(new ArrayList<>(100));
        }
        tmpAssignment = new int[tree.size];
        Arrays.fill(tmpAssignment, -1);
        sums = new double[k][dim];
        sizes = new int[k];
        initIds(tree.getRoot());
    }

    /**
     * @return previous cluster
     */
    public int add(int cluster, Node n) {
        plusEquals(sums[cluster], n.meansum);
        tmpClusters.get(cluster).add(n);
        sizes[cluster] += n.size;
        int oldass = tmpAssignment[n.id];
        tmpAssignment[n.id] = cluster;
        return oldass == cluster ? 0 : n.size;
    }

    public int check(int cluster, Node n) {
        return tmpAssignment[n.id] == cluster ? 0 : n.size;
    }

    /**
     * @return previous cluster
     */
    public int add(int cluster, DBIDRef id, NumberVector fv) {
        plusEquals(sums[cluster], fv);
        clusters.get(cluster).add(id);
        sizes[cluster]++;
        return assignment.put(id, cluster) == cluster ? 0 : 1;
    }

    /**
     * @return Iterator over all nodes of the cluster
     */
    public Iterator<Node> getNodes(int cluster) {
        return tmpClusters.get(cluster).iterator();
    }

    public void reset() {
        Arrays.fill(sizes, 0);
        for(int i = 0; i < sums.length; i++) {
            Arrays.fill(sums[i], 0.);
        }
        for(ModifiableDBIDs cluster : clusters) {
            cluster.clear();
        }
        for(List<Node> list : tmpClusters) {
            list.clear();
        }
    }

    public double[][] getSums() {
        return sums;
    }

    public int getSize(int i) {
        return sizes[i];
    }

    protected static void plusEquals(double[] sum, double[] meansum) {
        for(int i = 0; i < sum.length; i++) {
            sum[i] += meansum[i];
        }
    }

    /**
     * Similar to VMath.plusEquals, but accepts a number vector.
     *
     * @param sum Aggregation array
     * @param vec Vector to add
     */
    private static void plusEquals(double[] sum, NumberVector vec) {
        for(int d = 0; d < sum.length; d++) {
            sum[d] += vec.doubleValue(d);
        }
    }

    private void initIds(Node cur) {
        cur.id = nodecount++;
        for(Node child : cur.children) {
            initIds(child);
        }
    }

    public int testAssign(DBIDRef id, int clu, double[][] means) {
        NumberVector fv = relation.get(id);
        double min = dist(fv, means[0]);
        // distance(fv, means[0]);
        int sMinInd = 0;
        for(int i = 1; i < means.length; i++) {
            final double d = dist(fv, means[i]);
            if(d < min) {
                sMinInd = i;
                min = d;
            }
        }
        return sMinInd == clu ? 0 : 1;
    }

    public int testAssign(Node n, int clu, double[][] means) {
        int invalid = 0;
        ModifiableDBIDs collect = DBIDUtil.newHashSet(n.size);
        tree.collectSubtree(n, collect);
        for(DBIDIter it = collect.iter(); it.valid(); it.advance()) {
            NumberVector fv = relation.get(it);
            double min = dist(fv, means[0]);
            // distance(fv, means[0]);
            int sMinInd = 0;
            for(int i = 1; i < means.length; i++) {
                final double d = dist(fv, means[i]);
                if(d < min) {
                    sMinInd = i;
                    min = d;
                }
            }
            if(sMinInd != clu) {
                invalid++;
            }
        }
        return invalid;
    }

    public int testExclude(Node n, int clu, double[][] means) {
        int invalid = 0;
        ModifiableDBIDs collect = DBIDUtil.newHashSet(n.size);
        tree.collectSubtree(n, collect);
        for(DBIDIter it = collect.iter(); it.valid(); it.advance()) {
            NumberVector fv = relation.get(it);
            double min = dist(fv, means[0]);
            // distance(fv, means[0]);
            int sMinInd = 0;
            for(int i = 1; i < means.length; i++) {
                final double d = dist(fv, means[i]);
                if(d < min) {
                    sMinInd = i;
                    min = d;
                }
            }
            if(sMinInd == clu) {
                invalid++;
            }
        }
        return invalid;
    }

    private double dist(NumberVector x, double[] y) {
        double v = 0;
        for(int i = 0; i < y.length; i++) {
            double d = x.doubleValue(i) - y[i];
            v += d * d;
        }
        return v;
    }
}
