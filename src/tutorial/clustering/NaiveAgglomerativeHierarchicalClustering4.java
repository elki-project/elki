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

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HierarchicalClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.SLINK;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDBIDDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDistanceDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;

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
public class NaiveAgglomerativeHierarchicalClustering4<O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, PointerHierarchyRepresentationResult<DoubleDistance>> implements HierarchicalClusteringAlgorithm<DoubleDistance> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(NaiveAgglomerativeHierarchicalClustering4.class);

  /**
   * Different linkage strategies.
   * 
   * The update formulas here come from:<br />
   * R. M. Cormack, A Review of Classification
   * 
   * @author Erich Schubert
   */
  public static enum Linkage {//
    SINGLE {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        return Math.min(dx, dy);
      }
    }, // single-linkage hierarchical clustering
    COMPLETE {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        return Math.max(dx, dy);
      }
    }, // complete-linkage hierarchical clustering
    GROUP_AVERAGE {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        final double wx = sizex / (double) (sizex + sizey);
        final double wy = sizey / (double) (sizex + sizey);
        return wx * dx + wy * dy;
      }
    }, // average-linkage hierarchical clustering
    WEIGHTED_AVERAGE {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        return .5 * (dx + dy);
      }
    }, // a more naive variant, McQuitty (1966)
    CENTROID {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        final double wx = sizex / (double) (sizex + sizey);
        final double wy = sizey / (double) (sizex + sizey);
        final double beta = (sizex * sizey) / (double) ((sizex + sizey) * (sizex + sizey));
        return wx * dx + wy * dy - beta * dxy;
      }
    }, // Sokal and Michener (1958), Gower (1967)
    MEDIAN {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        return .5 * (dx + dy) - .25 * dxy;
      }
    }, // Gower (1967)
    WARD {
      @Override
      public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
        final double wx = (sizex + sizej) / (double) (sizex + sizey + sizej);
        final double wy = (sizey + sizej) / (double) (sizex + sizey + sizej);
        final double beta = sizej / (double) (sizex + sizey + sizej);
        return wx * dx + wy * dy - beta * dxy;
      }
    }, // Minimum Variance, Wishart (1969), Anderson (1971)
    ;

    abstract public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy);
  }

  /**
   * Current linkage in use.
   */
  Linkage linkage = Linkage.WARD;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function to use
   * @param linkage Linkage strategy
   */
  public NaiveAgglomerativeHierarchicalClustering4(DistanceFunction<? super O, D> distanceFunction, Linkage linkage) {
    super(distanceFunction);
    this.linkage = linkage;
  }

  /**
   * Run the algorithm
   * 
   * @param db Database
   * @param relation Relation
   * @return Clustering hierarchy
   */
  public PointerHierarchyRepresentationResult<DoubleDistance> run(Database db, Relation<O> relation) {
    DistanceQuery<O, D> dq = db.getDistanceQuery(relation, getDistanceFunction());
    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    final int size = ids.size();

    if (size > 0x10000) {
      throw new AbortException("This implementation does not scale to data sets larger than " + 0x10000 + " instances (~17 GB RAM), which results in an integer overflow.");
    }
    if (Linkage.SINGLE.equals(linkage)) {
      LOG.verbose("Notice: SLINK is a much faster algorithm for single-linkage clustering!");
    }

    // Compute the initial (lower triangular) distance matrix.
    double[] scratch = new double[triangleSize(size)];
    DBIDArrayIter ix = ids.iter(), iy = ids.iter(), ij = ids.iter();
    // Position counter - must agree with computeOffset!
    int pos = 0;
    boolean square = Linkage.WARD.equals(linkage) && !(SquaredEuclideanDistanceFunction.class.isInstance(getDistanceFunction()));
    for (int x = 0; ix.valid(); x++, ix.advance()) {
      iy.seek(0);
      for (int y = 0; y < x; y++, iy.advance()) {
        scratch[pos] = dq.distance(ix, iy).doubleValue();
        // Ward uses variances -- i.e. squared values
        if (square) {
          scratch[pos] *= scratch[pos];
        }
        pos++;
      }
    }

    // Initialize space for result:
    WritableDBIDDataStore parent = DataStoreUtil.makeDBIDStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableDoubleDistanceDataStore height = DataStoreUtil.makeDoubleDistanceStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    WritableIntegerDataStore csize = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    for (DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      parent.put(it, it);
      height.put(it, Double.POSITIVE_INFINITY);
      csize.put(it, 1);
    }

    // Repeat until everything merged, except the desired number of clusters:
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Agglomerative clustering", size - 1, LOG) : null;
    for (int i = 1; i < size; i++) {
      double min = Double.POSITIVE_INFINITY;
      int minx = -1, miny = -1;
      for (ix.seek(0); ix.valid(); ix.advance()) {
        if (height.doubleValue(ix) < Double.POSITIVE_INFINITY) {
          continue;
        }
        final int xbase = triangleSize(ix.getOffset());
        for (iy.seek(0); iy.getOffset() < ix.getOffset(); iy.advance()) {
          if (height.doubleValue(iy) < Double.POSITIVE_INFINITY) {
            continue;
          }
          final int idx = xbase + iy.getOffset();
          if (scratch[idx] <= min) {
            min = scratch[idx];
            minx = ix.getOffset();
            miny = iy.getOffset();
          }
        }
      }
      assert (minx >= 0 && miny >= 0);
      // Avoid allocating memory, by reusing existing iterators:
      ix.seek(minx);
      iy.seek(miny);
      // Perform merge in data structure: x -> y
      // Since y < x, prefer keeping y, dropping x.
      int sizex = csize.intValue(ix), sizey = csize.intValue(iy);
      height.put(ix, min);
      parent.put(ix, iy);
      csize.put(iy, sizex + sizey);

      // Update distance matrix. Note: miny < minx
      final int xbase = triangleSize(minx), ybase = triangleSize(miny);
      // Write to (y, j), with j < y
      for (ij.seek(0); ij.getOffset() < miny; ij.advance()) {
        if (height.doubleValue(ij) < Double.POSITIVE_INFINITY) {
          continue;
        }
        final int sizej = csize.intValue(ij);
        scratch[ybase + ij.getOffset()] = linkage.combine(sizex, scratch[xbase + ij.getOffset()], sizey, scratch[ybase + ij.getOffset()], sizej, min);
      }
      // Write to (j, y), with y < j < x
      for (ij.seek(miny + 1); ij.getOffset() < minx; ij.advance()) {
        if (height.doubleValue(ij) < Double.POSITIVE_INFINITY) {
          continue;
        }
        final int jbase = triangleSize(ij.getOffset());
        final int sizej = csize.intValue(ij);
        scratch[jbase + miny] = linkage.combine(sizex, scratch[xbase + ij.getOffset()], sizey, scratch[jbase + miny], sizej, min);
      }
      // Write to (j, y), with y < x < j
      for (ij.seek(minx + 1); ij.valid(); ij.advance()) {
        if (height.doubleValue(ij) < Double.POSITIVE_INFINITY) {
          continue;
        }
        final int jbase = triangleSize(ij.getOffset());
        final int sizej = csize.intValue(ij);
        scratch[jbase + miny] = linkage.combine(sizex, scratch[jbase + minx], sizey, scratch[jbase + miny], sizej, min);
      }
      if (prog != null) {
        prog.incrementProcessed(LOG);
      }
    }
    if (prog != null) {
      prog.ensureCompleted(LOG);
    }

    return new PointerHierarchyRepresentationResult<>(ids, parent, height);
  }

  /**
   * Compute the size of a complete x by x triangle (minus diagonal)
   * 
   * @param x Offset
   * @return Size of complete triangle
   */
  protected static int triangleSize(int x) {
    return (x * (x - 1)) >>> 1;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // The input relation must match our distance function:
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
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
     * Current linkage in use.
     */
    protected Linkage linkage = Linkage.SINGLE;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      EnumParameter<Linkage> linkageP = new EnumParameter<>(LINKAGE_ID, Linkage.class);
      linkageP.setDefaultValue(Linkage.WARD);
      if (config.grab(linkageP)) {
        linkage = linkageP.getValue();
      }
    }

    @Override
    protected NaiveAgglomerativeHierarchicalClustering4<O, D> makeInstance() {
      return new NaiveAgglomerativeHierarchicalClustering4<>(distanceFunction, linkage);
    }
  }
}
