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
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.utilities.documentation.Reference;

/**
 * Combines Cover Tree k-means with the Shallot algorithm
 * <p>
 * Reference:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * Accelerating k-Means Clustering with Cover Trees<br>
 * Int. Conf. on Similarity Search and Applications, SISAP 2023
 *
 * @author Andreas Lang
 *
 * @param <V> vector datatype
 */
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "Accelerating k-Means Clustering with Cover Trees", //
    booktitle = "Int. Conf. on Similarity Search and Applications, SISAP 2023", //
    url = "https://doi.org/10.1007/978-3-031-46994-7_13", //
    bibkey = "DBLP:conf/sisap/LangS23")
public class HybShaCovKMeans<V extends NumberVector> extends HybExpCovKMeans<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HybShaCovKMeans.class);

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
  public HybShaCovKMeans(int k, int maxiter, KMeansInitialization initializer, boolean varstat, double expansion, int trunc, int switchover) {
    super(k, maxiter, initializer, varstat, expansion, trunc, switchover);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> relation) {
    KMeansCoverTree<V> tree = new KMeansCoverTree<V>(relation, EuclideanDistance.STATIC, expansion, trunc, true);
    tree.initialize();
    Instance instance = new Instance(relation, distance, initialMeans(relation), tree, switchover);
    instance.run(maxiter);
    instance.generateCover();
    instance.logStatistics();
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
  protected static class Instance extends HybExpCovKMeans.Instance {
    /**
     * Second nearest cluster.
     */
    WritableIntegerDataStore second;

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
      super(relation, df, means, tree, switchover);
      second = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, -1);
    }

    @Override
    protected int iterate(int iteration) {
      if(iteration == 1) {
        return initialAssignToNearestCluster();
      }
      if(iteration < switchover) {
        meansFromSumsCT(means, nodeManager.getSums(), means);
        int changed = assignToNearestCluster();
        assert testSizes();
        assert nodeManager.testTree(tree.getRoot(), false);
        return changed;
      }
      if(iteration == switchover) {
        meansFromSumsCT(means, nodeManager.getSums(), means);
        int changed = assignToClusterBounds();
        assert testSizes();
        return changed;
      }
      meansFromSumsCT(newmeans, nodeManager.getSums(), means);
      movedDistance(means, newmeans, sep);
      updateBounds(sep);
      copyMeans(newmeans, means);
      return assignPointsToNearestCluster();
    }

    /**
     * Assign node to cluster (recursively) and add bounds + store id of second
     * closest cluster
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
      double min1 = parentdists[0], min2 = parentdists[1];
      int minInd = 0, min2Ind = 1;
      double[] dists;
      if(cur.parentDist != 0 || radius == Double.POSITIVE_INFINITY) {
        NumberVector fv = relation.get(it); // Routing object vector
        min1 = distance(fv, means[cand[0]]);
        double newbound = Math.sqrt(min1) + cur.parentDist + 2 * cur.maxDist;
        if(newbound < min2) { // Equation 14
          assert nodeManager.testAssign(cur, cand[0], means) == 0;
          return addBound(cur, oldass, cand[0], cand[1], Math.sqrt(min1) + cur.maxDist, min2 - radius);
        }
        newbound = min2 + 2 * radius;
        newbound *= newbound;
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
          assert nodeManager.testAssign(cur, cand[minInd], means) == 0;
          return addBound(cur, oldass, cand[minInd], cand[min2Ind], min1 + cur.maxDist, min2 - cur.maxDist);
        }
      }
      else {
        dists = parentdists;
        fastbound = min1 + 2 * cur.maxDist;
        if(fastbound < min2) {
          nodestatFilter += alive * (cur.size - cur.singletons.size());
          assert nodeManager.testAssign(cur, cand[minInd], means) == 0;
          return addBound(cur, oldass, cand[minInd], cand[min2Ind], min1 + cur.maxDist, min2 - radius);
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
        assert nodeManager.testAssign(it, cand[0], means) == 0;
        changed += addBound(it, relation.get(it), myoldass, cand[0], cand[1], min1, min2);
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
          changed += addBound(child, myoldass, cand[0], cand[1], min1 + child.parentDist + child.maxDist, min2 - child.parentDist - child.maxDist);
          assert nodeManager.testAssign(child, cand[0], means) == 0;
        }
        else {
          // TODO change radius if necessary
          changed += assignNodeBounds(child, alive, oldass, cur.maxDist, dists, cand);
        }
      }
      return changed;
    }

    /**
     * Assign Singleton points and set bounds + store i of second closest
     * cluster
     * 
     * @param cur Current Node
     * @param oldass Parent old assignment when applicable
     * @param fastbound bound for assignment
     * @param it singleton iterator
     * @param dists distances from parent to cluster candidates
     * @param cand list of cluster candidates
     * @param alive number of cluster candidates
     * @param min1 distance from parent to the closest cluster center
     * @param min2 distance from parent to the second closest cluster
     *        center
     * @return number of reassigned points
     */
    protected int assignSingletonsBounds(Node cur, int oldass, double fastbound, DBIDIter it, double[] dists, int[] cand, int alive, double min1, double min2) {
      int changed = 0;
      for(int j = 1; it.valid(); it.advance(), j++) {
        int myoldass = oldass != -1 ? oldass : nodeManager.get(it);
        if(cur.singletons.doubleValue(j) <= fastbound) {
          singletonstatFilter += alive;
          assert nodeManager.testAssign(it, cand[0], means) == 0;
          changed += addBound(it, relation.get(it), myoldass, cand[0], cand[1], min1 + cur.singletons.doubleValue(j), min2 - cur.singletons.doubleValue(j));
          continue;
        }
        NumberVector fv = relation.get(it);
        double minS1 = distance(fv, means[cand[0]]);
        double minS2 = distance(fv, means[cand[1]]);
        int sMinInd = 0, sMin2Ind = 1;
        if(minS2 < minS1) {
          double t = minS2;
          minS2 = minS1;
          minS1 = t;
          sMinInd = 1;
          sMin2Ind = 0;
        }
        double mybound = Math.sqrt(minS2) + cur.singletons.doubleValue(j);
        mybound *= mybound;
        int sal = pruneD(dists, cand, mybound, alive);

        singletonstatPrune += alive - sal;

        for(int i = 2; i < sal; i++) {
          if(minS2 >= scdist[cand[sMinInd]][cand[i]]) {
            final double dist = distance(fv, means[cand[i]]);
            if(dist < minS1) {
              sMin2Ind = sMinInd;
              sMinInd = i;
              minS2 = minS1;
              minS1 = dist;
            }
            if(dist < minS2) {
              sMin2Ind = i;
              minS2 = dist;
            }
          }
          else {
            singletonstatIcDist++;
          }
        }
        assert nodeManager.testAssign(it, cand[sMinInd], means) == 0;
        changed += addBound(it, fv, myoldass, cand[sMinInd], cand[sMin2Ind], Math.sqrt(minS1), Math.sqrt(minS2));
      }
      return changed;
    }

    @Override
    protected int assignPointsToNearestCluster() {
      recomputeSeperation(sep, cdist);
      nearestMeans(cdist, cnum);
      int changed = 0;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final int orig = assignment.intValue(it);
        final double z = lower.doubleValue(it);
        final double so = sep[orig];
        double u = upper.doubleValue(it);
        if(u <= z || u <= so) {
          continue;
        }
        // Make the upper bound tight first:
        final NumberVector fv = relation.get(it);
        double curd2 = distance(fv, means[orig]);
        upper.putDouble(it, u = isSquared ? Math.sqrt(curd2) : curd2);
        if(u <= z || u <= so) {
          continue;
        }
        // Our cdist are scaled 0.5, so we need half r:
        if(cdist[orig][cnum[orig][0]] > u + so) {
          continue;
        }
        // Shallot modification #1: try old second-nearest first:
        final int osecn = second.intValue(it);
        // Exact distance to previous second nearest
        double secd2 = distance(fv, means[osecn]);
        int ref = orig, secn = osecn; // closest center "z" in Borgelts paper
        if(secd2 < curd2) {
          // Previous second closest is closer, swap:
          final double tmp = secd2;
          secd2 = curd2;
          curd2 = tmp;
          ref = secn;
          secn = orig;
          // Update u
          u = isSquared ? Math.sqrt(curd2) : curd2;
        }
        // Shallot improvement 1.5:
        // note that secd2 is still squared, cdist is half the distance
        // 0.5*(u+l), with l=min(u+d(x,p), 2u+2*cdist[z])
        double lp = u + (isSquared ? Math.sqrt(secd2) : secd2); // l for p
        double lv = 2 * (u + cdist[ref][cnum[ref][0]]); // l for v2(z)y
        double l = lp < lv ? lp : lv;
        double rhalf = Math.min(u + sep[ref], 0.5 * (u + l));
        // Find closest center, and distance to two closest centers
        double min1 = curd2, min2 = l * l;
        int cur = ref, minId2 = lp < lv ? secn : cnum[ref][0];
        for(int i = 0; i < k - 1; i++) {
          int c = cnum[ref][i];
          if(cdist[ref][c] > rhalf) {
            break;
          }
          final double dist = c == secn ? secd2 : distance(fv, means[c]);
          if(dist < min1) {
            minId2 = cur;
            cur = c;
            min2 = min1;
            min1 = dist;
            if(min2 < l * l) {
              l = isSquared ? Math.sqrt(min2) : min2;
              // Second Shallot improvement: r shrinking
              rhalf = Math.min(rhalf, 0.5 * (u + l));
            }
          }
          else if(dist < min2) {
            minId2 = c;
            min2 = dist;
            l = isSquared ? Math.sqrt(min2) : min2;
            // Second Shallot improvement: r shrinking
            rhalf = Math.min(rhalf, 0.5 * (u + l));
          }
        }
        // Object has to be reassigned.
        if(cur != orig) {
          nodeManager.fChange(it, fv, orig, cur);
          ++changed;
          upper.putDouble(it, min1 == curd2 ? u : isSquared ? Math.sqrt(min1) : min1);
        }
        lower.putDouble(it, l);
        if(osecn != minId2) { // second might have changed
          second.putInt(it, minId2);
        }
      }
      return changed;
    }

    /**
     * Add Node to cluster and sore bounds + id of second closest cluster
     * 
     * @param n Node
     * @param oldass Old assignment
     * @param clu closest cluster
     * @param clu2 second closest cluster
     * @param u upper bound
     * @param l lower bound
     * @return number of changed elements
     */
    protected int addBound(Node n, int oldass, int clu, int clu2, double u, double l) {
      int changed = nodeManager.change(n, oldass, clu);
      ModifiableDBIDs collect = DBIDUtil.newArray(n.size);
      tree.collectSubtree(n, collect);
      if(l == Double.POSITIVE_INFINITY) {
        l = 0.;
      }
      for(DBIDIter it = collect.iter(); it.valid(); it.advance()) {
        upper.putDouble(it, u);
        lower.putDouble(it, l);
        second.putInt(it, clu2);
        assignment.put(it, clu);
        assert testUpper(it, u) == 0;
        assert testLower(it, u, l, clu) == 0;
      }
      return changed;
    }

    /**
     * Add element to cluster and store bounds, and store the id of second
     * closest cluster
     * 
     * @param id Element Id
     * @param fv Object
     * @param oldass old assignment
     * @param clu closest cluster
     * @param clu2 second closest cluster
     * @param u upper bound
     * @param l lower bound
     * @return 1 if assignment changed else 0
     */
    protected int addBound(DBIDRef id, NumberVector fv, int oldass, int clu, int clu2, double u, double l) {
      int changed = nodeManager.change(id, fv, oldass, clu);
      if(l == Double.POSITIVE_INFINITY) {
        l = 0;
      }
      upper.putDouble(id, u);
      lower.putDouble(id, l);
      second.putInt(id, clu2);
      assert testUpper(id, u) == 0;
      assert testLower(id, u, l, clu) == 0;
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
  public static class Par<V extends NumberVector> extends HybExpCovKMeans.Par<V> {
    @Override
    public HybShaCovKMeans<V> make() {
      return new HybShaCovKMeans<>(k, maxiter, initializer, varstat, expansion, trunc, switchover);
    }
  }
}
