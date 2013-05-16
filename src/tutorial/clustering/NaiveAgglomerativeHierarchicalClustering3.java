package tutorial.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.SLINK;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * This tutorial will step you through implementing a well known clustering
 * algorithm, agglomerative hierarchical clustering, in multiple steps.
 * 
 * This is the third step, where we add support for different linkage
 * strategies.
 * 
 * This is the naive O(n^3) algorithm. See {@link SLINK} for a much faster
 * algorithm (however, only for single-linkage).
 * 
 * Reference (for the update formulas):
 * <p>
 * A Review of Classification<br />
 * R. M. Cormack<br />
 * Journal of the Royal Statistical Society. Series A, Vol. 134, No. 3
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
@Reference(title = "A Review of Classification", authors = "R. M. Cormack", booktitle = "Journal of the Royal Statistical Society. Series A, Vol. 134, No. 3", url = "http://www.jstor.org/stable/2344237")
public class NaiveAgglomerativeHierarchicalClustering3<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, Result> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(NaiveAgglomerativeHierarchicalClustering3.class);

  /**
   * Different linkage strategies.
   * 
   * @author Erich Schubert
   */
  public enum Linkage {//
    SINGLE, // single-linkage hiearchical clustering
    COMPLETE, // complete-linkage hiearchical clustering
    GROUP_AVERAGE, // average-linkage hiearchical clustering
    WEIGHTED_AVERAGE, // a more naive variant, McQuitty (1966)
    CENTROID, // Sokal and Michener (1958), Gower (1967)
    MEDIAN, // Gower (1967)
    WARD, // Minimum Variance, Wishart (1969), Anderson (1971)
  }

  /**
   * Threshold, how many clusters to extract.
   */
  int numclusters;

  /**
   * Current linkage in use.
   */
  Linkage linkage = Linkage.SINGLE;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function to use
   * @param numclusters Number of clusters
   * @param linkage Linkage strategy
   */
  public NaiveAgglomerativeHierarchicalClustering3(DistanceFunction<? super O, D> distanceFunction, int numclusters, Linkage linkage) {
    super(distanceFunction);
    this.numclusters = numclusters;
    this.linkage = linkage;
  }

  /**
   * Run the algorithm
   * 
   * @param db Database
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public Result run(Database db, Relation<O> relation) {
    DistanceQuery<O, D> dq = db.getDistanceQuery(relation, getDistanceFunction());
    ArrayDBIDs ids = DBIDUtil.newArray(relation.getDBIDs());
    final int size = ids.size();

    if (size > 0x10000) {
      throw new AbortException("This implementation does not scale to data sets larger than " + 0x10000 + " instances (~17 GB RAM), which results in an integer overflow.");
    }
    if (Linkage.SINGLE.equals(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }

    // Compute the initial (lower triangular) distance matrix.
    double[] scratch = new double[(size * (size - 1)) >>> 1];
    DBIDArrayIter ix = ids.iter(), iy = ids.iter();
    // Position counter - must agree with computeOffset!
    int pos = 0;
    for (int x = 0; ix.valid(); x++, ix.advance()) {
      iy.seek(0);
      for (int y = 0; y < x; y++, iy.advance()) {
        scratch[pos] = dq.distance(ix, iy).doubleValue();
        // Ward uses variances -- i.e. squared values
        if (Linkage.WARD.equals(linkage)) {
          scratch[pos] *= scratch[pos];
        }
        pos++;
      }
    }

    // Initialize space for result:
    double[] height = new double[size];
    Arrays.fill(height, -1);
    // Parent node, to track merges
    // have every object point to itself initially
    ArrayModifiableDBIDs parent = DBIDUtil.newArray(ids);
    // Active clusters, when not trivial.
    TIntObjectMap<ModifiableDBIDs> clusters = new TIntObjectHashMap<>();

    // Repeat until everything merged:
    final int stop = size - numclusters;
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", stop, LOG) : null;
    for (int i = 0; i < stop; i++) {
      double min = Double.POSITIVE_INFINITY;
      int minx = -1, miny = -1;
      for (int x = 0; x < size; x++) {
        if (height[x] > 0) {
          continue;
        }
        final int xbase = (x * (x - 1)) >> 1;
        for (int y = 0; y < x; y++) {
          if (height[y] > 0) {
            continue;
          }
          final int idx = xbase + y;
          if (scratch[idx] < min) {
            min = scratch[idx];
            minx = x;
            miny = y;
          }
        }
      }
      assert (minx >= 0 && miny >= 0);
      // Perform merge in data structure: x -> y
      // Since y < x, prefer keeping y, dropping x.
      height[minx] = min;
      iy.seek(miny);
      parent.set(minx, iy);
      // Merge into cluster (TODO: skip for single link etc.?)
      ModifiableDBIDs cx = clusters.get(minx);
      ModifiableDBIDs cy = clusters.get(miny);
      int sizex = 1, sizey = 1; // cluster sizes, for averaging
      if (cy == null) {
        cy = DBIDUtil.newHashSet();
        cy.add(iy);
      } else {
        sizey = cy.size();
      }
      if (cx == null) {
        ix.seek(minx);
        cy.add(ix);
      } else {
        sizex = cx.size();
        cy.addDBIDs(cx);
        clusters.remove(minx);
      }
      clusters.put(miny, cy);
      // Update distance matrix. Note: miny < minx

      // The update formulas here come from:
      // R. M. Cormack, A Review of Classification
      switch(linkage) {
      case SINGLE: {
        final int xbase = (minx * (minx - 1)) >> 1, ybase = (miny * (miny - 1)) >> 1;
        // Write to (y, j), with j < y
        for (int j = 0; j < miny; j++) {
          if (height[j] < 0) {
            scratch[ybase + j] = Math.min(scratch[xbase + j], scratch[ybase + j]);
          }
        }
        // Write to (j, y), with y < j < x
        for (int j = miny + 1; j < minx; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = Math.min(scratch[xbase + j], scratch[jbase + miny]);
          }
        }
        // Write to (j, y), with y < x < j
        for (int j = minx + 1; j < size; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = Math.min(scratch[jbase + minx], scratch[jbase + miny]);
          }
        }
        break;
      }
      case COMPLETE: {
        final int xbase = (minx * (minx - 1)) >> 1, ybase = (miny * (miny - 1)) >> 1;
        // Write to (y, j), with j < y
        for (int j = 0; j < miny; j++) {
          if (height[j] < 0) {
            scratch[ybase + j] = Math.max(scratch[xbase + j], scratch[ybase + j]);
          }
        }
        // Write to (j, y), with y < j < x
        for (int j = miny + 1; j < minx; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = Math.max(scratch[xbase + j], scratch[jbase + miny]);
          }
        }
        // Write to (j, y), with y < x < j
        for (int j = minx + 1; j < size; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = Math.max(scratch[jbase + minx], scratch[jbase + miny]);
          }
        }
        break;
      }
      case GROUP_AVERAGE: {
        final double wx = sizex / (double) (sizex + sizey);
        final double wy = sizey / (double) (sizex + sizey);
        final int xbase = (minx * (minx - 1)) >> 1, ybase = (miny * (miny - 1)) >> 1;
        // Write to (y, j), with j < y
        for (int j = 0; j < miny; j++) {
          if (height[j] < 0) {
            scratch[ybase + j] = wx * scratch[xbase + j] + wy * scratch[ybase + j];
          }
        }
        // Write to (j, y), with y < j < x
        for (int j = miny + 1; j < minx; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = wx * scratch[xbase + j] + wy * scratch[jbase + miny];
          }
        }
        // Write to (j, y), with y < x < j
        for (int j = minx + 1; j < size; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = wx * scratch[jbase + minx] + wy * scratch[jbase + miny];
          }
        }
        break;
      }
      case WEIGHTED_AVERAGE: {
        final int xbase = (minx * (minx - 1)) >> 1, ybase = (miny * (miny - 1)) >> 1;
        // Write to (y, j), with j < y
        for (int j = 0; j < miny; j++) {
          if (height[j] < 0) {
            scratch[ybase + j] = .5 * (scratch[xbase + j] + scratch[ybase + j]);
          }
        }
        // Write to (j, y), with y < j < x
        for (int j = miny + 1; j < minx; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = .5 * (scratch[xbase + j] + scratch[jbase + miny]);
          }
        }
        // Write to (j, y), with y < x < j
        for (int j = minx + 1; j < size; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = .5 * (scratch[jbase + minx] + scratch[jbase + miny]);
          }
        }
        break;
      }
      case CENTROID: {
        final double wx = sizex / (double) (sizex + sizey);
        final double wy = sizey / (double) (sizex + sizey);
        final double bias = (min * sizex * sizey) / ((sizex + sizey) * (sizex + sizey));
        final int xbase = (minx * (minx - 1)) >> 1, ybase = (miny * (miny - 1)) >> 1;
        // Write to (y, j), with j < y
        for (int j = 0; j < miny; j++) {
          if (height[j] < 0) {
            scratch[ybase + j] = wx * scratch[xbase + j] + wy * scratch[ybase + j] - bias;
          }
        }
        // Write to (j, y), with y < j < x
        for (int j = miny + 1; j < minx; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = wx * scratch[xbase + j] + wy * scratch[jbase + miny] - bias;
          }
        }
        // Write to (j, y), with y < x < j
        for (int j = minx + 1; j < size; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = wx * scratch[jbase + minx] + wy * scratch[jbase + miny] - bias;
          }
        }
        break;
      }
      case MEDIAN: {
        final double bias = .25 * min;
        final int xbase = (minx * (minx - 1)) >> 1, ybase = (miny * (miny - 1)) >> 1;
        // Write to (y, j), with j < y
        for (int j = 0; j < miny; j++) {
          if (height[j] < 0) {
            scratch[ybase + j] = .5 * (scratch[xbase + j] + scratch[ybase + j]) - bias;
          }
        }
        // Write to (j, y), with y < j < x
        for (int j = miny + 1; j < minx; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = .5 * (scratch[xbase + j] + scratch[jbase + miny]) - bias;
          }
        }
        // Write to (j, y), with y < x < j
        for (int j = minx + 1; j < size; j++) {
          if (height[j] < 0) {
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = .5 * (scratch[jbase + minx] + scratch[jbase + miny]) - bias;
          }
        }
        break;
      }
      case WARD: {
        final int xbase = (minx * (minx - 1)) >> 1, ybase = (miny * (miny - 1)) >> 1;
        // Write to (y, j), with j < y
        for (int j = 0; j < miny; j++) {
          if (height[j] < 0) {
            final DBIDs idsj = clusters.get(j);
            final int sizej = (idsj == null) ? 1 : idsj.size();
            final double wx = (sizex + sizej) / (double) (sizex + sizey + sizej);
            final double wy = (sizey + sizej) / (double) (sizex + sizey + sizej);
            final double bias = (min * sizej) / (sizex + sizey + sizej);
            scratch[ybase + j] = wx * scratch[xbase + j] + wy * scratch[ybase + j] - bias;
          }
        }
        // Write to (j, y), with y < j < x
        for (int j = miny + 1; j < minx; j++) {
          if (height[j] < 0) {
            final DBIDs idsj = clusters.get(j);
            final int sizej = (idsj == null) ? 1 : idsj.size();
            final double wx = (sizex + sizej) / (double) (sizex + sizey + sizej);
            final double wy = (sizey + sizej) / (double) (sizex + sizey + sizej);
            final double bias = (min * sizej) / (sizex + sizey + sizej);
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = wx * scratch[xbase + j] + wy * scratch[jbase + miny] - bias;
          }
        }
        // Write to (j, y), with y < x < j
        for (int j = minx + 1; j < size; j++) {
          if (height[j] < 0) {
            final DBIDs idsj = clusters.get(j);
            final int sizej = (idsj == null) ? 1 : idsj.size();
            final double wx = (sizex + sizej) / (double) (sizex + sizey + sizej);
            final double wy = (sizey + sizej) / (double) (sizex + sizey + sizej);
            final double bias = (min * sizej) / (sizex + sizey + sizej);
            final int jbase = (j * (j - 1)) >> 1;
            scratch[jbase + miny] = wx * scratch[jbase + minx] + wy * scratch[jbase + miny] - bias;
          }
        }
        break;
      }
      default:
        throw new AbortException("Implementation incomplete.");
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    if (prog != null) {
      prog.ensureCompleted(LOG);
    }

    final Clustering<DendrogramModel<D>> dendrogram = new Clustering<>("Cluster-Dendrogram", "dendrogram");
    for (int x = 0; x < size; x++) {
      if (height[x] < 0) {
        DBIDs cids = clusters.get(x);
        if (cids == null) {
          ix.seek(x);
          cids = DBIDUtil.deref(ix);
        }
        Cluster<DendrogramModel<D>> cluster = new Cluster<>("Cluster", cids);
        dendrogram.addToplevelCluster(cluster);
      }
    }

    return dendrogram;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @param <O> Object type
   * @param <D> Distance type
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    /**
     * Option ID for linkage parameter.
     */
    private static final OptionID LINKAGE_ID = new OptionID("hierarchical.linkage", "Parameter to choose the linkage strategy.");

    /**
     * Desired number of clusters.
     */
    int numclusters = 0;

    /**
     * Current linkage in use.
     */
    protected Linkage linkage = Linkage.SINGLE;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter numclustersP = new IntParameter(SLINK.Parameterizer.SLINK_MINCLUSTERS_ID);
      if (config.grab(numclustersP)) {
        numclusters = numclustersP.intValue();
      }

      EnumParameter<Linkage> linkageP = new EnumParameter<>(LINKAGE_ID, Linkage.class);
      if (config.grab(linkageP)) {
        linkage = linkageP.getValue();
      }
    }

    @Override
    protected NaiveAgglomerativeHierarchicalClustering3<O, D> makeInstance() {
      return new NaiveAgglomerativeHierarchicalClustering3<>(distanceFunction, numclusters, linkage);
    }
  }
}
