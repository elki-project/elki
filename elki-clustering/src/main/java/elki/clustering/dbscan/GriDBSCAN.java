/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package elki.clustering.dbscan;

import java.util.Arrays;

import elki.AbstractDistanceBasedAlgorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.clustering.dbscan.util.Assignment;
import elki.clustering.dbscan.util.Border;
import elki.clustering.dbscan.util.Core;
import elki.clustering.dbscan.util.MultiBorder;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.ClusterModel;
import elki.data.model.Model;
import elki.data.type.CombinedTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.range.RangeQuery;
import elki.database.relation.ProxyView;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.Distance;
import elki.distance.minkowski.LPNormDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.logging.statistics.StringStatistic;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.exceptions.IncompatibleDataException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.jafama.FastMath;

/**
 * Using Grid for Accelerating Density-Based Clustering.
 * <p>
 * An accelerated DBSCAN version for numerical data and Lp-norms only, by
 * partitioning the data set into overlapping grid cells. For best efficiency,
 * the overlap of the grid cells must be chosen well. The authors suggest a grid
 * width of 10 times epsilon.
 * <p>
 * Because of partitioning the data, this version does not make use of indexes.
 * <p>
 * Reference:
 * <p>
 * S. Mahran, K. Mahar<br>
 * Using grid for accelerating density-based clustering<br>
 * In 8th IEEE Int. Conf. on Computer and Information Technology, 2008.
 *
 * @author Erich Schubert
 * @since 0.7.1
 *
 * @composed - - - Instance
 *
 * @param <V> the type of vector the algorithm is applied to
 */
@Title("GriDBSCAN: Using Grid for Accelerating Density-Based Clustering")
@Reference(authors = "S. Mahran, K. Mahar", //
    title = "Using grid for accelerating density-based clustering", //
    booktitle = "8th IEEE Int. Conf. on Computer and Information Technology", //
    url = "https://doi.org/10.1109/CIT.2008.4594646", //
    bibkey = "DBLP:conf/IEEEcit/MahranM08")
public class GriDBSCAN<V extends NumberVector> extends AbstractDistanceBasedAlgorithm<Distance<? super V>, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(GriDBSCAN.class);

  /**
   * Holds the epsilon radius threshold.
   */
  protected double epsilon;

  /**
   * Holds the minimum cluster size.
   */
  protected int minpts;

  /**
   * Width of the grid cells. Must be at least 2 epsilon!
   */
  protected double gridwidth;

  /**
   * Constructor with parameters.
   *
   * @param distance Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts parameter
   * @param gridwidth Grid width
   */
  public GriDBSCAN(Distance<? super V> distance, double epsilon, int minpts, double gridwidth) {
    super(distance);
    this.epsilon = epsilon;
    this.minpts = minpts;
    this.gridwidth = gridwidth;
  }

  /**
   * Performs the DBSCAN algorithm on the given database.
   */
  public Clustering<Model> run(Relation<V> relation) {
    final DBIDs ids = relation.getDBIDs();

    // Degenerate result:
    if(ids.size() < minpts) {
      Clustering<Model> result = new Clustering<>();
      Metadata.of(result).setLongName("DBSCAN Clustering");
      result.addToplevelCluster(new Cluster<Model>(ids, true, ClusterModel.CLUSTER));
      return result;
    }

    double gridwidth = this.gridwidth; // local copy.
    if(gridwidth < 2. * epsilon) {
      LOG.warning("Invalid grid width (less than 2*epsilon, recommended 10*epsilon). Increasing grid width automatically.");
      gridwidth = 2. * epsilon;
    }
    return new Instance<V>(getDistance(), epsilon, minpts, gridwidth).run(relation);
  }

  /**
   * Instance, for a single run.
   *
   * @author Erich Schubert
   *
   * @param <V> Vector type
   */
  protected static class Instance<V extends NumberVector> {
    /**
     * Unprocessed IDs.
     */
    protected static final int UNPROCESSED = 0;

    /**
     * Noise IDs.
     */
    protected static final int NOISE = 1;

    /**
     * Distance function used.
     */
    protected Distance<? super V> distance;

    /**
     * Holds the epsilon radius threshold.
     */
    protected double epsilon;

    /**
     * Holds the minimum cluster size.
     */
    protected int minpts;

    /**
     * Width of the grid cells. Must be at least 2 epsilon!
     */
    protected double gridwidth;

    /**
     * Value domain.
     */
    protected double[][] domain;

    /**
     * Dimensionality.
     */
    protected int dim;

    /**
     * Grid offset.
     */
    protected double[] offset;

    /**
     * Number of cells per dimension.
     */
    protected int[] cells;

    /**
     * Data grid partitioning.
     */
    Long2ObjectOpenHashMap<ModifiableDBIDs> grid;

    /**
     * Core identifier objects (shared to conserve memory).
     */
    private Core[] cores;

    /**
     * Border identifier objects (shared to conserve memory).
     */
    private Border[] borders;

    /**
     * Cluster assignments.
     */
    private WritableDataStore<Assignment> clusterids;

    /**
     * Temporary assignments of a single run.
     */
    private WritableIntegerDataStore temporary;

    /**
     * Indicates that the number of grid cells has overflown.
     */
    private boolean overflown;

    /**
     * Constructor.
     *
     * @param distance Distance function
     * @param epsilon Epsilon
     * @param minpts MinPts
     * @param gridwidth Grid width
     */
    public Instance(Distance<? super V> distance, double epsilon, int minpts, double gridwidth) {
      this.distance = distance;
      this.epsilon = epsilon;
      this.minpts = minpts;
      this.gridwidth = gridwidth;
    }

    /**
     * Performs the DBSCAN algorithm on the given database.
     *
     * @param relation Relation to process
     */
    public Clustering<Model> run(Relation<V> relation) {
      final DBIDs ids = relation.getDBIDs();
      final int size = ids.size();

      // Domain of the database
      this.domain = RelationUtil.computeMinMax(relation);
      this.dim = domain[0].length;
      this.offset = new double[dim];
      this.cells = new int[dim];
      // Compute the grid start, and the number of cells in each dimension.
      long numcells = computeGridBaseOffsets(size);

      // Build the data grid.
      buildGrid(relation, (int) numcells, offset);
      if(grid.size() <= dim) {
        LOG.warning("There are only " + grid.size() + " occupied cells. This will likely be slower than regular DBSCAN!");
      }

      // Check grid cell counts:
      int mincells = checkGridCellSizes(size, numcells);

      // (Temporary) store the cluster ID assigned.
      clusterids = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP, Assignment.class);
      temporary = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP, UNPROCESSED);
      final ArrayModifiableDBIDs activeSet = DBIDUtil.newArray();
      // Reserve the first two cluster ids:
      int clusterid = NOISE + 1;
      this.cores = new Core[2];
      this.borders = new Border[2];

      // Reused storage for neighbors:
      ModifiableDoubleDBIDList neighbors = DBIDUtil.newDistanceDBIDList(minpts << 1);
      // Run DBSCAN on each cell that has enough objects.
      FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Processing grid cells", mincells, LOG) : null;
      for(ModifiableDBIDs cellids : grid.values()) {
        if(cellids.size() < minpts) {
          continue; // Too few objects.
        }
        clusterid += runDBSCANOnCell(cellids, relation, neighbors, activeSet, clusterid);
        // Post-process DBSCAN clustering result:
        updateCoreBorderObjects(clusterid);
        mergeClusterInformation(cellids, temporary, clusterids);
        LOG.incrementProcessed(cprog);
      }
      LOG.ensureCompleted(cprog);
      temporary.destroy();

      return buildResult(ids, clusterid);
    }

    private int runDBSCANOnCell(DBIDs cellids, Relation<V> relation, ModifiableDoubleDBIDList neighbors, ArrayModifiableDBIDs activeSet, int clusterid) {
      temporary.clear(); // Reset to "UNPROCESSED"
      ProxyView<V> rel = new ProxyView<>(cellids, relation);
      RangeQuery<V> rq = new QueryBuilder<>(rel, distance).rangeQuery(epsilon);
      FiniteProgress pprog = LOG.isVerbose() ? new FiniteProgress("Running DBSCAN", cellids.size(), LOG) : null;
      for(DBIDIter id = cellids.iter(); id.valid(); id.advance()) {
        // Skip already processed ids.
        if(temporary.intValue(id) != UNPROCESSED) {
          continue;
        }
        rq.getRangeForDBID(id, epsilon, neighbors.clear());
        if(neighbors.size() >= minpts) {
          expandCluster(id, clusterid, temporary, neighbors, activeSet, rq, pprog);
          ++clusterid;
        }
        else {
          temporary.putInt(id, NOISE);
          LOG.incrementProcessed(pprog);
        }
      }
      LOG.ensureCompleted(pprog);
      return clusterid;
    }

    /**
     * Update the shared arrays for core points (to conserve memory)
     *
     * @param clusterid Number of clusters
     */
    private void updateCoreBorderObjects(int clusterid) {
      int i = cores.length;
      cores = Arrays.copyOf(cores, clusterid);
      borders = Arrays.copyOf(borders, clusterid);
      while(i < clusterid) {
        cores[i] = new Core(i);
        borders[i] = new Border(cores[i]);
        ++i;
      }
    }

    /**
     * Compute the grid base offset.
     *
     * @param size Data set size
     * @return Total number of grid cells
     */
    private long computeGridBaseOffsets(int size) {
      StringBuffer buf = LOG.isDebuggingFinest() ? new StringBuffer() : null;
      double[] min = domain[0], max = domain[1];
      long total = 1;
      for(int d = 0; d < dim; d++) {
        final double mi = min[d], ma = max[d], wi = ma - mi;
        if(mi == Double.NEGATIVE_INFINITY || ma == Double.POSITIVE_INFINITY || mi != mi || ma != ma) {
          throw new IncompatibleDataException("Dimension " + d + " contains non-finite values.");
        }
        int c = cells[d] = Math.max(1, (int) FastMath.ceil(wi / gridwidth));
        offset[d] = mi - (c * gridwidth - wi) * .5;
        assert (offset[d] <= mi) : "Grid inconsistent.";
        assert (offset[d] + c * gridwidth >= ma) : "Grid inconsistent.";
        total *= c;
        if(total < 0) {
          LOG.warning("Excessive amount of grid cells (long overflow)! Use larger grid cells.");
          overflown = true;
          total &= 0x7FFF_FFFF_FFFF_FFFFL;
        }
        if(buf != null) {
          buf.append(d).append(": min=").append(mi).append(" max=").append(ma);
          double s = offset[d];
          for(int i = 0; i <= c; i++) {
            buf.append(' ').append(s);
            s += gridwidth;
          }
          buf.append('\n');
        }
      }
      if(buf != null) {
        LOG.debugFinest(buf);
      }
      if(total > size) {
        LOG.warning("The generated grid has more cells than data points. This may need excessive amounts of memory.");
      }
      else if(total == 1) {
        LOG.warning("All data is in a single cell. This has degenerated to a non-indexed DBSCAN!");
      }
      else if(total <= dim * dim) {
        LOG.warning("There are only " + total + " cells. This will likely be slower than regular DBSCAN!");
      }
      return total;
    }

    /**
     * Build the data grid.
     *
     * @param relation Data relation
     * @param numcells Total number of cells
     * @param offset Offset
     */
    protected void buildGrid(Relation<V> relation, int numcells, double[] offset) {
      grid = new Long2ObjectOpenHashMap<ModifiableDBIDs>(numcells >>> 2);
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        V obj = relation.get(it);
        insertIntoGrid(it, obj, 0, 0);
      }
    }

    /**
     * Insert a single object into the grid; potentially into multiple cells (at
     * most 2^d) via recursion.
     *
     * @param id Object ID
     * @param obj Object
     * @param d Current dimension
     * @param v Current cell value
     */
    private void insertIntoGrid(DBIDRef id, V obj, int d, int v) {
      final int cn = cells[d]; // Number of cells in this dimension
      final int nd = d + 1; // Next dimension
      int mi = Math.max(0, (int) FastMath.floor((obj.doubleValue(d) - offset[d] - epsilon) / gridwidth));
      int ma = Math.min(cn - 1, (int) FastMath.floor((obj.doubleValue(d) - offset[d] + epsilon) / gridwidth));
      assert (mi <= ma) : "Grid inconsistent.";
      for(int i = mi; i <= ma; i++) {
        int c = v * cn + i;
        if(nd == cells.length) {
          ModifiableDBIDs ids = grid.get(c);
          if(ids == null) {
            grid.put(c, ids = DBIDUtil.newArray());
          }
          ids.add(id);
        }
        else {
          insertIntoGrid(id, obj, nd, c);
        }
      }
    }

    /**
     * Perform some sanity checks on the grid cells.
     *
     * @param numcell Number of cells
     * @param size Relation size
     * @return Number of cells with minPts points
     */
    protected int checkGridCellSizes(int size, long numcell) {
      int tcount = 0;
      int hasmin = 0;
      double sqcount = 0;
      for(ModifiableDBIDs cell : grid.values()) {
        final int s = cell.size();
        if(s >= size >> 1) {
          LOG.warning("A single cell contains half of the database (" + s//
              + " objects). This will not scale very well.");
        }
        tcount += s;
        sqcount += s * (long) s;
        if(s >= minpts) {
          hasmin++;
        }
      }
      double savings = sqcount / size / size;
      if(savings >= 1) {
        LOG.warning("Pairwise distances within each cells are more expensive than a full DBSCAN run due to overlap!");
      }
      if(overflown) {
        LOG.statistics(new StringStatistic(GriDBSCAN.class.getName() + ".all-cells", "overflow"));
      }
      else {
        LOG.statistics(new LongStatistic(GriDBSCAN.class.getName() + ".all-cells", numcell));
      }
      LOG.statistics(new LongStatistic(GriDBSCAN.class.getName() + ".used-cells", grid.size()));
      LOG.statistics(new LongStatistic(GriDBSCAN.class.getName() + ".minpts-cells", hasmin));
      LOG.statistics(new DoubleStatistic(GriDBSCAN.class.getName() + ".redundancy", tcount / (double) size));
      LOG.statistics(new DoubleStatistic(GriDBSCAN.class.getName() + ".relative-cost", savings));
      return hasmin;
    }

    /**
     * Set-based expand cluster implementation.
     *
     * @param clusterid ID of the current cluster.
     * @param clusterids Current object to cluster mapping.
     * @param neighbors Neighbors acquired by initial getNeighbors call.
     * @param activeSet Set to manage active candidates.
     * @param rq Range query
     * @param pprog Object progress
     * @return cluster size
     */
    protected int expandCluster(final DBIDRef seed, final int clusterid, final WritableIntegerDataStore clusterids, final ModifiableDoubleDBIDList neighbors, ArrayModifiableDBIDs activeSet, RangeQuery<V> rq, FiniteProgress pprog) {
      assert (activeSet.size() == 0);
      int clustersize = 1 + processCorePoint(seed, neighbors, clusterid, clusterids, activeSet);
      LOG.incrementProcessed(pprog);
      // run expandCluster as long as there is another seed
      final DBIDVar id = DBIDUtil.newVar();
      while(!activeSet.isEmpty()) {
        activeSet.pop(id);
        // Evaluate Neighborhood predicate
        rq.getRangeForDBID(id, epsilon, neighbors.clear());
        // Evaluate Core-Point predicate
        if(neighbors.size() >= minpts) {
          clustersize += processCorePoint(id, neighbors, clusterid, clusterids, activeSet);
        }
        LOG.incrementProcessed(pprog);
      }
      return clustersize;
    }

    /**
     * Process a single core point.
     *
     * @param seed Point to process
     * @param newneighbors New neighbors
     * @param clusterid Cluster to add to
     * @param clusterids Cluster assignment storage.
     * @param activeSet Active set of cluster seeds
     * @return Number of new points added to cluster
     */
    protected int processCorePoint(final DBIDRef seed, DoubleDBIDList newneighbors, final int clusterid, final WritableIntegerDataStore clusterids, ArrayModifiableDBIDs activeSet) {
      clusterids.putInt(seed, clusterid); // Core point now
      int clustersize = 0;
      // The recursion is unrolled into iteration over the active set.
      for(DoubleDBIDListIter it = newneighbors.iter(); it.valid(); it.advance()) {
        final int oldassign = clusterids.intValue(it);
        if(oldassign == UNPROCESSED) {
          if(it.doubleValue() > 0.) { // We can skip points at distance 0.
            activeSet.add(it);
          }
        }
        else if(oldassign != NOISE) {
          continue; // Member of some cluster.
        }
        clustersize++;
        clusterids.putInt(it, -clusterid);
      }
      return clustersize;
    }

    /**
     * Merge cluster information.
     *
     * @param cellids IDs in current cell
     * @param temporary Temporary assignments
     * @param clusterids Merged cluster assignment
     */
    protected void mergeClusterInformation(ModifiableDBIDs cellids, WritableIntegerDataStore temporary, WritableDataStore<Assignment> clusterids) {
      FiniteProgress mprog = LOG.isVerbose() ? new FiniteProgress("Collecting result", cellids.size(), LOG) : null;
      for(DBIDIter id = cellids.iter(); id.valid(); id.advance()) {
        int nclus = temporary.intValue(id);
        if(nclus > NOISE) { // Core point
          Core core = cores[nclus];
          assert (core.num > NOISE);
          Assignment oclus = clusterids.get(id);
          if(oclus == null) { // No assignment yet (= NOISE)
            clusterids.put(id, core);
          }
          else if(oclus instanceof Core) { // Core and core - merge!
            core.mergeWith((Core) oclus);
          }
          else if(oclus instanceof Border) { // Core and border point, merge!
            core.mergeWith(((Border) oclus).core);
            clusterids.put(id, core);
          }
          else { // Point is border for multiple clusters
            assert (oclus instanceof MultiBorder);
            if(LOG.isDebuggingFinest()) {
              LOG.debugFinest("Multi-Merge: " + nclus + " - " + oclus + " -> " + core);
            }
            // Find minimum:
            int m = core.num, m2 = ((MultiBorder) oclus).getCore().num;
            m = m < m2 ? m : m2;
            assert (m > NOISE);
            // Execute all merges:
            for(Border b : ((MultiBorder) oclus).cs) {
              cores[b.core.num].num = m;
            }
            core.num = m;
            clusterids.put(id, core);
          }
        }
        else if(nclus < 0) { // Border point
          Border border = borders[-nclus];
          Assignment oclus = clusterids.get(id);
          if(oclus == null) { // No assignment yet.
            clusterids.put(id, border);
          }
          else if(oclus instanceof Core) { // Border and core point - merge
            ((Core) oclus).mergeWith(border.core);
          }
          else if(oclus instanceof Border) { // Border and border
            if(((Border) oclus).core.num != border.core.num) {
              clusterids.put(id, new MultiBorder((Border) oclus, border));
            }
          }
          else {
            assert (oclus instanceof MultiBorder);
            clusterids.put(id, ((MultiBorder) oclus).update(border));
          }
        }
        else {
          assert (nclus == NOISE); // Ignore noise.
        }
        LOG.incrementProcessed(mprog);
      }
      LOG.ensureCompleted(mprog);
    }

    /**
     * Assemble the clustering result.
     *
     * @param ids Object IDs
     * @param clusterid Largest valid cluster number
     * @return Clustering
     */
    protected Clustering<Model> buildResult(final DBIDs ids, int clusterid) {
      FiniteProgress pprog = LOG.isVerbose() ? new FiniteProgress("Building final result", ids.size(), LOG) : null;
      ModifiableDBIDs[] clusters = new ModifiableDBIDs[clusterid];
      ModifiableDBIDs noise = DBIDUtil.newArray();
      for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
        Assignment cids = clusterids.get(it);
        if(cids == null) {
          noise.add(it);
        }
        else {
          if(cids instanceof MultiBorder) {
            cids = ((MultiBorder) cids).getCore();
          }
          else if(cids instanceof Border) {
            cids = ((Border) cids).core;
          }
          assert (cids instanceof Core);
          Core co = (Core) cids;
          while(cores[co.num].num != co.num) {
            co = cores[co.num = cores[co.num].num];
          }
          ModifiableDBIDs clu = clusters[co.num];
          if(clu == null) {
            clu = clusters[co.num] = DBIDUtil.newArray();
          }
          clu.add(it);
        }
        LOG.incrementProcessed(pprog);
      }
      LOG.ensureCompleted(pprog);
      clusterids.destroy();

      Clustering<Model> result = new Clustering<>();
      Metadata.of(result).setLongName("DBSCAN Clustering");
      for(int i = NOISE + 1; i < clusters.length; i++) {
        if(clusters[i] != null) {
          result.addToplevelCluster(new Cluster<Model>(clusters[i], ClusterModel.CLUSTER));
        }
      }
      if(noise.size() > 0) {
        result.addToplevelCluster(new Cluster<Model>(noise, true, ClusterModel.CLUSTER));
      }
      return result;
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // We strictly need a vector field of fixed dimensionality!
    TypeInformation type = new CombinedTypeInformation(TypeUtil.NUMBER_VECTOR_FIELD, getDistance().getInputTypeRestriction());
    return TypeUtil.array(type);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Vector type to use
   */
  public static class Par<O extends NumberVector> extends AbstractDistanceBasedAlgorithm.Par<Distance<? super O>> {
    /**
     * Parameter to control the grid width.
     *
     * Must be at least two times epsilon.
     */
    public static final OptionID GRID_ID = new OptionID("gridbscan.gridwidth", "Width of the grid used, must be at least two times epsilon.");

    /**
     * Holds the epsilon radius threshold.
     */
    protected double epsilon;

    /**
     * Holds the minimum cluster size.
     */
    protected int minpts;

    /**
     * Width of the grid cells. Must be at least 2 epsilon!
     */
    protected double gridwidth;

    @Override
    public Class<?> getDistanceRestriction() {
      return LPNormDistance.class;
    }

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new DoubleParameter(DBSCAN.Par.EPSILON_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> epsilon = x);

      new IntParameter(DBSCAN.Par.MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> {
            minpts = x;
            if(minpts <= 2) {
              LOG.warning("DBSCAN with minPts <= 2 is equivalent to single-link clustering at a single height. Consider using larger values of minPts.");
            }
          });

      new DoubleParameter(GRID_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .setDefaultValue(epsilon > 0 ? 10. * epsilon : 1.) //
          .addConstraint(new GreaterEqualConstraint(2. * epsilon)) //
          .grab(config, x -> gridwidth = x);
    }

    @Override
    public GriDBSCAN<O> make() {
      return new GriDBSCAN<>(distance, epsilon, minpts, gridwidth);
    }
  }
}
