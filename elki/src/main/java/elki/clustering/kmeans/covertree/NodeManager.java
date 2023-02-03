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

import java.util.List;

import elki.clustering.kmeans.covertree.KMeansCoverTree.Node;
import elki.data.NumberVector;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;

public class NodeManager {
    /**
     * A mapping of elements to cluster ids.
     */
    protected WritableIntegerDataStore assignment;

    protected WritableIntegerDataStore nodeSize;

    /**
     * Sum aggregate for the new mean.
     */
    double[][] sums;

    int[] sizes;

    int nodecount;

    Relation<? extends NumberVector> relation;

    KMeansCoverTree<?> tree;

    public NodeManager(int k, int dim, List<ModifiableDBIDs> clusters, WritableIntegerDataStore assignment, KMeansCoverTree<?> tree, Relation<? extends NumberVector> relation) {
        this.assignment = assignment;
        this.nodeSize = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
        this.relation = relation;
        this.tree = tree;
        sums = new double[k][dim];
        sizes = new int[k];
    }

    /**
     * 
     * @param n Node
     * @return current assignment
     */
    public int get(Node n) {
        return n.size == nodeSize.intValue(n.singletons.iter()) ? get(n.singletons.iter()) : -1;
    }

    /**
     * 
     * @param n Node
     * @return current assignment
     */
    public int getF(Node n) {
        return get(n.singletons.iter());
    }

    /**
     * 
     * @param id DBID
     * @return current assignment
     */
    public int get(DBIDRef id) {
        return assignment.intValue(id);
    }

    public int add(int cluster, Node n) {
        assert (cluster != get(n.singletons.iter()) && cluster >= 0);
        assert (get(n) == -1);
        assert (testClean(n, -1) == 0);
        plusEquals(sums[cluster], n.meansum);
        sizes[cluster] += n.size;
        nodeSize.putInt(n.singletons.iter(), n.size);
        assignment.put(n.singletons.iter(), cluster);
        assert (testTree(n, false));
        return n.size;
    }

    public int add(int cluster, DBIDRef id, NumberVector fv) {
        assert (cluster != get(id) && cluster >= 0);
        assert (get(id) == -1);
        plusEquals(sums[cluster], fv);
        // clusters.get(cluster).add(id);
        sizes[cluster]++;
        nodeSize.putInt(id, 1);
        assignment.put(id, cluster);
        return 1;
    }

    public void clean(Node n) {
        assignment.put(n.singletons.iter(), -1);
    }

    public void clean(DBIDRef id) {
        assignment.put(id, -1);
    }

    /**
     * @return previous cluster
     */
    public void remove(int cluster, Node n) {
        if(cluster == -2) {
            return;
        }
        // externer check ob child nodes noch Zugewiesen sind.
        if(cluster == -1) {
            removeRec(n);
            assert (testClean(n, -1) == 0);
            return;
        }
        minusEquals(sums[cluster], n.meansum);
        sizes[cluster] -= n.size;
        // nodeSize.putInt(n.singletons.iter(), n.size);
        assignment.put(n.singletons.iter(), -1);
        assert (testClean(n, -1) == 0);
        assert (testTree(n, true));
        return;
    }

    public void removeRec(Node n) {
        // Check
        for(Node c : n.children) {
            int oldass = get(c);
            if(oldass >= 0) {
                remove(oldass, c);
                continue;
            }
            removeRec(c);
        }
        DBIDIter it = n.singletons.iter();
        if(n.children.isEmpty()) {
            remove(get(it), it, relation.get(it));
        }
        it.advance();
        for(; it.valid(); it.advance()) {
            remove(get(it), it, relation.get(it));
        }
        assert (testClean(n, -1) == 0);
    }

    public void remove(int cluster, DBIDRef id, NumberVector fv) {
        if(cluster < 0) {
            return;
        }
        minusEquals(sums[cluster], fv);
        sizes[cluster]--;
        // TODO: return value nutzen
        assignment.put(id, -1);
        // nodeSize.putInt(id, 1);
        return;
    }

    public void reset() {
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

    protected static void minusEquals(double[] sum, double[] meansum) {
        for(int i = 0; i < sum.length; i++) {
            sum[i] -= meansum[i];
        }
    }

    private static void minusEquals(double[] sum, NumberVector vec) {
        for(int d = 0; d < sum.length; d++) {
            sum[d] -= vec.doubleValue(d);
        }
    }

    public boolean testTree(Node cur, boolean assigned) {
        boolean intact = true;
        if(get(cur) != -1) {
            if(assigned) {
                intact = false;
            }
            assigned = true;
        }
        DBIDIter it = cur.singletons.iter();
        if(get(it) != -1 && nodeSize.intValue(it) == 1) {
            if(assigned) {
                intact = false;
            }
        }
        it.advance();
        for(; it.valid(); it.advance()) {
            if(get(it) != -1) {
                if(assigned) {
                    intact = false;
                }
            }
        }
        for(Node child : cur.children) {
            intact = intact & testTree(child, assigned);
        }

        return intact;
    }

    public int testClean(Node n, int clu) {
        int fails = 0;
        if(get(n) != clu) {
            fails++;
        }
        if(fails > 0) {
            int x = 10;
        }
        for(Node c : n.children) {
            fails += testClean(c, clu);
        }
        if(fails > 0) {
            int x = 10;
        }
        for(DBIDIter it = n.singletons.iter().advance(); it.valid(); it.advance()) {
            if(get(it) != clu) {
                fails++;
            }
        }
        if(fails > 0) {
            int x = 10;
        }
        return fails;
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
