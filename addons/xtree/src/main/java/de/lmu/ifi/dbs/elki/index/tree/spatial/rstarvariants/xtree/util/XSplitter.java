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
package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.lmu.ifi.dbs.elki.data.HyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialUtil;
import de.lmu.ifi.dbs.elki.index.tree.DirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree.AbstractXTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree.AbstractXTreeNode;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree.XTreeDirectoryEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.xtree.XTreeSettings;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Provides methods for splitting X-tree nodes.
 * 
 * @author Marisa Thoma
 * @since 0.7.5
 */
public class XSplitter<N extends AbstractXTreeNode<N>, T extends AbstractXTree<N>> {
  /** Logger object for the XSplitter. */
  private static final Logging LOG = Logging.getLogger(XSplitter.class);

  private EntryTypeDimensionalComparator etdc;

  /** The split axis. */
  private int splitAxis = 0;

  private N node = null;

  /** Selected maximum overlap strategy. */
  private XTreeSettings.Overlap maxOverlapStrategy = XTreeSettings.Overlap.VOLUME_OVERLAP;

  /** The tree calling this splitter. */
  private T tree;

  /** The overlap calculated in the last minimum overlap split. */
  private double pastOverlap = -1;

  /**
   * Initialize a split helper for <code>entries</code> using the given
   * maxOverlapStrategy.
   * 
   * @param tree Tree
   * @param node Node to split
   */
  public XSplitter(T tree, N node) {
    this.tree = tree;
    this.maxOverlapStrategy = tree.get_overlap_type();
    this.etdc = new EntryTypeDimensionalComparator(0, false);
    this.node = node;
  }

  /**
   * Generates both sets of <code>(maxEntries - minEntries + 1)</code>
   * distributions of the first <code>minEntries</code> entries together with
   * the next <code>0</code> to <code>maxEntries - minEntries</code> entries for
   * each of the orderings in <code>entriesByLB</code> and
   * <code>entriesByUB</code>. <br>
   * For all distributions, the sums of the two resulting MBRs' surfaces are
   * calculated and the overall sum is returned.
   * 
   * @param minEntries minimally allowed subgroup size
   * @param maxEntries maximally allowed subgroup size; if
   *        <code>&lt; node.getNumEntries()</code>, ONLY the first half (
   *        <code>[minEntries,maxEntries]</code>) of the possible partitions is
   *        calculated
   * @param entriesByLB entries sorted by lower bound value of some dimension
   * @param entriesByUB entries sorted by upper bound value of some dimension
   * @return the sum of all resulting MBRs' surface sums of the partitions
   *         ranging in <code>{minEntries, ..., maxEntries</code> in both of the
   *         input orderings
   */
  private double generateDistributionsAndSurfaceSums(int minEntries, int maxEntries, int[] entriesByLB, int[] entriesByUB) {
    // the old variant is minimally slower
    // double surfaceSum = 0;
    // for(int limit = minEntries; limit <= maxEntries; limit++) {
    // HyperBoundingBox mbr1 = mbr(entriesByLB, 0, limit);
    // HyperBoundingBox mbr2 = mbr(entriesByLB, limit, entriesByLB.length);
    // surfaceSum += mbr1.perimeter() + mbr2.perimeter();
    // mbr1 = mbr(entriesByUB, 0, limit);
    // mbr2 = mbr(entriesByUB, limit, entriesByUB.length);
    // surfaceSum += mbr1.perimeter() + mbr2.perimeter();
    // }
    // return surfaceSum;
    int dim = tree.getDimensionality();
    // get surface sum for lower-bound sorting
    double surfaceSum = getSurfaceSums4Sorting(minEntries, maxEntries, entriesByLB, dim);
    // get surface sum for upper-bound sorting
    surfaceSum += getSurfaceSums4Sorting(minEntries, maxEntries, entriesByUB, dim);
    return surfaceSum;
  }

  /**
   * Compute the surfaces of the <code>2 * (maxEntries - minEntries + 1)</code>
   * split MBRs resulting for the sorting <code>entrySorting</code>.
   * 
   * @param minEntries minimally allowed subgroup size
   * @param maxEntries maximally allowed subgroup size for the first entry set
   * @param entrySorting a permutation of the indices
   * @param dim the dimension of the tree
   * @return the sum of all first and second MBRs' surfaces for the tested entry
   *         distributions
   */
  private double getSurfaceSums4Sorting(int minEntries, int maxEntries, int[] entrySorting, int dim) {
    // avoid multiple MBR calculations by updating min/max-logs for the two
    // collections' bounds:

    // the first entries' maximum upper bounds
    double[] pqUBFirst = new double[dim];
    Arrays.fill(pqUBFirst, Double.NEGATIVE_INFINITY);
    // maintain the second entries' upper bounds
    List<Heap<DoubleIntPair>> pqUBSecond = new ArrayList<>(dim);
    for(int i = 0; i < dim; i++) {
      // Descending heap
      pqUBSecond.add(new TopBoundedHeap<DoubleIntPair>(maxEntries, Collections.reverseOrder()));
    }
    // the first entries' minimum lower bounds
    double[] pqLBFirst = new double[dim];
    Arrays.fill(pqLBFirst, Double.POSITIVE_INFINITY);
    // maintain the second entries' minimum lower bounds
    List<Heap<DoubleIntPair>> pqLBSecond = new ArrayList<>(dim);
    for(int i = 0; i < dim; i++) {
      // Ascending heap
      pqLBSecond.add(new TopBoundedHeap<DoubleIntPair>(maxEntries));
    }
    // initialize bounds for first entry collection
    for(int index = 0; index < minEntries; index++) {
      add2MBR(entrySorting, pqUBFirst, pqLBFirst, index);
    }
    HyperBoundingBox mbr1 = new HyperBoundingBox(pqLBFirst, pqUBFirst);

    // fill bounding queues for the second entry collection
    double[] minSecond = new double[dim];
    double[] maxSecond = new double[dim];
    Arrays.fill(maxSecond, Double.NEGATIVE_INFINITY);
    Arrays.fill(minSecond, Double.POSITIVE_INFINITY);

    assert entrySorting.length - maxEntries == minEntries;
    // initialize min/max entries of the second collections' tail
    for(int index = maxEntries; index < entrySorting.length; index++) {
      add2MBR(entrySorting, maxSecond, minSecond, index);
    }
    for(int i = 0; i < dim; i++) {
      // with index entrySorting.length => never to be removed
      pqLBSecond.get(i).add(new DoubleIntPair(minSecond[i], entrySorting.length));
      pqUBSecond.get(i).add(new DoubleIntPair(maxSecond[i], entrySorting.length));
    }
    // add the entries to be removed later on
    for(int index = minEntries; index < maxEntries; index++) {
      add2MBR(entrySorting, pqUBSecond, pqLBSecond, index);
    }
    for(int i = 0; i < minSecond.length; i++) {
      minSecond[i] = pqLBSecond.get(i).peek().first;
      maxSecond[i] = pqUBSecond.get(i).peek().first;
    }
    ModifiableHyperBoundingBox mbr2 = new ModifiableHyperBoundingBox(minSecond, maxSecond);
    double surfaceSum = SpatialUtil.perimeter(mbr1) + SpatialUtil.perimeter(mbr2);

    // generate the other distributions and file the surface sums
    for(int limit = minEntries; limit < maxEntries; limit++) {
      // extend first MBR by entry at position entrySorting[limit]:
      add2MBR(entrySorting, pqUBFirst, pqLBFirst, limit);
      // shrink entry at position entrySorting[limit] from second MBR:
      removeFromMBR(pqUBSecond, pqLBSecond, limit, mbr2);
      surfaceSum += SpatialUtil.perimeter(mbr1) + SpatialUtil.perimeter(mbr2);
    }

    return surfaceSum;
  }

  /**
   * Update operation for maintaining the second entry distribution. Removes the
   * entries associated with indices <code>&le; index</code> from the
   * dimensions' lower and upper bound priority queues (<code>pqLB</code> and
   * <code>pqUB</code>). Whenever this causes a change in a dimension's lower or
   * upper bound, <code>mbr</code> is updated accordingly.
   * 
   * @param pqUB One priority queue for each dimension. They are sorted by upper
   *        bound in descending order and consist of entry indices for the
   *        entries belonging to <code>mbr</code> (and possibly others, which
   *        may first have to be removed).
   * @param pqLB One priority queue for each dimension. They are sorted by lower
   *        bound in ascending order and consist of entry indices for the
   *        entries belonging to <code>mbr</code> (and possibly others, which
   *        may first have to be removed).
   * @param index All indices <code>&le; index</code> must no longer be part of
   *        <code>mbr</code>.
   * @param mbr The MBR to be adapted to a smaller entry list.
   */
  private void removeFromMBR(List<Heap<DoubleIntPair>> pqUB, List<Heap<DoubleIntPair>> pqLB, int index, ModifiableHyperBoundingBox mbr) {
    boolean change = false;
    DoubleIntPair pqPair;
    for(int d = 0; d < mbr.getDimensionality(); d++) {
      // remove all relevant upper bound entries belonging to the first set
      pqPair = pqUB.get(d).peek();
      while(pqPair.second <= index) {
        change = true;
        pqUB.get(d).poll();
        pqPair = pqUB.get(d).peek();
      }
      if(change) { // there probably was a change, as an entry has been removed
        mbr.setMax(d, pqPair.first);
      }
      change = false;
      // remove all relevant lower bound entries belonging to the first set
      pqPair = pqLB.get(d).peek();
      while(pqPair.second <= index) {
        change = true;
        pqLB.get(d).poll();
        pqPair = pqLB.get(d).peek();
      }
      if(change) { // there probably was a change, as an entry has been removed
        mbr.setMin(d, pqPair.first);
      }
      change = false;
    }
  }

  /**
   * Adds the minimum and maximum bounds of the MBR of entry number
   * <code>entrySorting[index]</code> to the dimension-wise
   * upper and lower bounds, <code>ub</code> and <code>lb</code>. Note that if
   * this method is called for <code>ub</code> and <code>lb</code> which are
   * already owned by an MBR, this update operation also updates the MBR defined
   * by those bounds.
   * 
   * @param entrySorting a sorting providing the mapping of <code>index</code>
   *        to the entry to be added
   * @param ub the upper bound of the MBR to be extended
   * @param lb the lower bound of the MBR to be extended
   * @param index the index in the sorting referencing the entry to be added
   */
  private void add2MBR(int[] entrySorting, double[] ub, double[] lb, int index) {
    SpatialComparable currMBR = node.getEntry(entrySorting[index]);
    for(int d = 0; d < currMBR.getDimensionality(); d++) {
      double max = currMBR.getMax(d);
      if(max > ub[d]) {
        ub[d] = max;
      }
      double min = currMBR.getMin(d);
      if(min < lb[d]) {
        lb[d] = min;
      }
    }
  }

  /**
   * Adds the minimum and maximum bounds of the MBR of entry number
   * <code>entrySorting[index]</code> to the dimension-wise
   * upper and lower bound priority queues <code>pqUBFirst</code> and
   * <code>pqLBFirst</code>.
   * 
   * @param entrySorting a sorting providing the mapping of <code>index</code>
   *        to the entry to be added
   * @param pqUB One priority queue for each dimension. They are sorted by upper
   *        bound in descending order and consist of entry indices in
   *        <code>entrySorting</code> for the entries belonging to
   *        <code>mbr</code>.
   * @param pqLB One priority queue for each dimension. They are sorted by lower
   *        bound in ascending order and consist of entry indices in
   *        <code>entrySorting</code> for the entries belonging to
   *        <code>mbr</code>.
   * @param index the index in the sorting referencing the entry to be added
   */
  private void add2MBR(int[] entrySorting, List<Heap<DoubleIntPair>> pqUB, List<Heap<DoubleIntPair>> pqLB, int index) {
    SpatialComparable currMBR = node.getEntry(entrySorting[index]);
    for(int d = 0; d < currMBR.getDimensionality(); d++) {
      double max = currMBR.getMax(d);
      pqUB.get(d).add(new DoubleIntPair(max, index));
      double min = currMBR.getMin(d);
      pqLB.get(d).add(new DoubleIntPair(min, index));
    }
  }

  /**
   * Indirectly sort the entries into the arrays
   * <code>entriesByLB</code> and <code>entriesByUB</code> by lower and upper
   * bound of dimension <code>d</code>.
   * 
   * @param d selected dimension
   * @param entriesByLB entry array to be sorted by the lower bound of
   *        <code>d</code> (must be initialized to a valid selection of indices
   * @param entriesByUB entry array to be sorted by the upper bound of
   *        <code>d</code> (must be initialized to a valid selection of indices
   */
  private void sortEntriesForDimension(int d, int[] entriesByLB, int[] entriesByUB) {
    // Lists which hold entries sorted by their lower and
    // upper bounds in the current dimension.
    etdc.set(d, true);
    IntegerArrayQuickSort.sort(entriesByLB, etdc);
    etdc.setLb(false);
    IntegerArrayQuickSort.sort(entriesByUB, etdc);
  }

  /**
   * Compares two EntryType objects by lower or upper bound.
   * 
   * @author Marisa Thoma
   */
  private final class EntryTypeDimensionalComparator implements IntComparator {
    private int dimension;

    private boolean lb;

    public EntryTypeDimensionalComparator(int dimension, boolean lb) {
      this.dimension = dimension;
      this.lb = lb;
    }

    @Override
    public int compare(int o1, int o2) {
      final double d1, d2;
      if(lb) {
        d1 = node.getEntry(o1).getMin(dimension);
        d2 = node.getEntry(o2).getMin(dimension);
      }
      else {
        d1 = node.getEntry(o1).getMax(dimension);
        d2 = node.getEntry(o2).getMax(dimension);
      }
      // ignore NaN case
      return (d1 > d2 ? 1 : (d1 < d2 ? -1 : 0));
    }

    public void set(int dimension, boolean lb) {
      this.lb = lb;
      this.dimension = dimension;
    }

    public void setLb(boolean lb) {
      this.lb = lb;
    }
  }

  /**
   * Determine the common split dimensions from a list of entries.
   * 
   * @param node node for which to determine the common split
   *        dimensions
   * @return common split dimensions
   */
  private IntIterator getCommonSplitDimensions(N node) {
    Collection<SplitHistory> splitHistories = new ArrayList<>(node.getNumEntries());
    for(int i = 0; i < node.getNumEntries(); i++) {
      SpatialEntry entry = node.getEntry(i);
      if(!(entry instanceof XTreeDirectoryEntry)) {
        throw new RuntimeException("Wrong entry type to derive split dimension from: " + entry.getClass().getName());
      }
      splitHistories.add(((XTreeDirectoryEntry) entry).getSplitHistory());
    }
    return SplitHistory.getCommonDimensions(splitHistories);
  }

  /**
   * Get the dimension with the minimum surface sum.
   * <p>
   * This is done by calculating every possible split in each given dimension
   * and the sum of surfaces of all splits for every dimension.
   * 
   * @param dimensionIterator The dimensions that should be tested
   * @param minEntries Minimum number of entries in each sub group; in case of a
   *        data node, this is the minimum leaf capacity, for directory nodes,
   *        this is either the minimum directory capacity or, if a topological
   *        split has already failed, the tree's minimum fan-out parameter.
   * @param maxEntries Maximum number of entries in each sub group
   * @return The dimension with the minimum surface sum, or <code>null</code> if
   *         dimensionIterable yielded nothing
   */
  private int chooseSplitAxis(IntIterator dimensionIterator, int minEntries, int maxEntries) {
    // assert that there ARE dimensions to be tested
    if(!dimensionIterator.hasNext()) {
      return -1;
    }

    int numOfEntries = node.getNumEntries();

    assert minEntries >= 1 && minEntries <= maxEntries && maxEntries <= numOfEntries;

    double optSurfaceSum = Double.POSITIVE_INFINITY;
    int optAxis = -1;

    int[] entriesByLB = new int[node.getNumEntries()];
    for(int i = 0; i < entriesByLB.length; i++) {
      entriesByLB[i] = i;
    }
    int[] entriesByUB = Arrays.copyOf(entriesByLB, node.getNumEntries());

    int[] entriesByLBRev = null, entriesByUBRev = null;
    if(maxEntries <= node.getNumEntries() / 2) {
      System.out.println("THIS HAPPENS!!");
      // initialize backwards direction
      entriesByLBRev = new int[node.getNumEntries()];
      entriesByUBRev = new int[node.getNumEntries()];
    }

    while(dimensionIterator.hasNext()) {
      int d = dimensionIterator.nextInt();
      sortEntriesForDimension(d, entriesByLB, entriesByUB);
      double surfaceSum = generateDistributionsAndSurfaceSums(minEntries, maxEntries, entriesByLB, entriesByUB);
      if(maxEntries <= node.getNumEntries() / 2) { // add opposite ranges
        System.out.println("THIS HAPPENS intern");
        for(int j = 0; j < entriesByUB.length; j++) {
          // reverse sorting
          entriesByUBRev[node.getNumEntries() - 1 - j] = entriesByUB[j];
          entriesByLBRev[node.getNumEntries() - 1 - j] = entriesByLB[j];
        }
        surfaceSum += generateDistributionsAndSurfaceSums(minEntries, maxEntries, entriesByLBRev, entriesByUBRev);
      }
      if(surfaceSum < optSurfaceSum) {
        optSurfaceSum = surfaceSum;
        optAxis = d;
      }
    }
    // file the split axis
    this.splitAxis = optAxis;
    return optAxis;
  }

  /**
   * Select the distribution with minimal intersection volume from a Collection
   * of distributions. If there are several equal minimum intersection volumes,
   * the distribution with the minimum volume is selected.
   * 
   * @param splitAxis Split axis to be tested
   * @param minEntries The minimum number of entries to be tested; in case of a
   *        data node, this is the minimum leaf capacity, for directory nodes,
   *        this is either the minimum directory capacity or, if a topological
   *        split has already failed, the tree's minimum fan-out parameter.
   * @param maxEntries The maximum number of entries to be tested
   * @param revert if <code>maxEntries &lt; numEntries</code> and
   *        <code>revert</code> is <code>true</code>, the upper range of the
   *        sorting is tested, i.e. not
   *        <code>{minEntries, ..., maxEntries}</code> but
   *        <code>{numEntries - maxEntries + 1, ..., numEntries - minEntries + 1}</code>
   * @return The distribution with the minimal intersection volume or
   *         <code>null</code>, if the minimum overlap split has a volume which
   *         is larger than the allowed <code>maxOverlap</code> ratio
   */
  private SplitSorting chooseMinimumOverlapSplit(int splitAxis, int minEntries, int maxEntries, boolean revert) {
    if(splitAxis == -1) {
      pastOverlap = Double.MAX_VALUE;
      return null;
    }
    double optXVolume = Double.POSITIVE_INFINITY;
    double optVolume = Double.POSITIVE_INFINITY;
    SplitSorting optDistribution = null;
    HyperBoundingBox[] optMBRs = null;

    // generate sortings for the mbr's extrema
    int[] entrySorting = new int[node.getNumEntries()];
    for(int i = 0; i < entrySorting.length; i++) {
      entrySorting[i] = i;
    }
    int[] lbSorting = Arrays.copyOf(entrySorting, entrySorting.length);
    int[] ubSorting = Arrays.copyOf(entrySorting, entrySorting.length);
    sortEntriesForDimension(splitAxis, entrySorting, entrySorting);
    if(revert && maxEntries < node.getNumEntries() / 2) {
      // test reverted sortings
      int[][] reverted = new int[2][node.getNumEntries()]; // temp array
      for(int i = 0; i < lbSorting.length; i++) {
        reverted[0][reverted[0].length - 1 - i] = lbSorting[i];
        reverted[1][reverted[1].length - 1 - i] = ubSorting[i];
      }
      for(int i = 0; i < lbSorting.length; i++) {
        lbSorting[i] = reverted[0][i];
        ubSorting[i] = reverted[1][i];
      }
    }
    for(int i = 0; i < 2; i++) { // test lower and upper bound sortings
      if(i == 0) { // lower-bound sorting
        entrySorting = lbSorting;
      }
      else {
        // upper-bound sorting
        entrySorting = ubSorting;
      }
      for(int limit = minEntries; limit <= maxEntries; limit++) {
        HyperBoundingBox mbr1 = mbr(entrySorting, 0, limit);
        HyperBoundingBox mbr2 = mbr(entrySorting, limit, entrySorting.length);
        double xVolume = SpatialUtil.overlap(mbr1, mbr2);
        if(xVolume < optXVolume) {
          optXVolume = xVolume;
          optDistribution = generateSplitSorting(entrySorting, limit);
          optMBRs = new HyperBoundingBox[] { mbr1, mbr2 };
          optVolume = Double.NaN;
        }
        else if(xVolume == optXVolume) {
          double vol = SpatialUtil.volume(mbr1);
          vol += SpatialUtil.volume(mbr2);
          if(Double.isNaN(optVolume)) {
            // calculate when necessary
            optVolume = SpatialUtil.volume(optMBRs[0]);
            optVolume += SpatialUtil.volume(optMBRs[1]);
          }
          if(vol < optVolume) {
            optXVolume = xVolume;
            optVolume = vol;
            optDistribution = generateSplitSorting(entrySorting, limit);
          }
        }
      }
    }
    if(node.getEntry(0) instanceof LeafEntry || tree.get_max_overlap() >= 1) {
      pastOverlap = Double.NaN; // overlap is not computed
      return optDistribution;
    }
    // test overlap
    switch(maxOverlapStrategy){
    case DATA_OVERLAP:
      pastOverlap = getRatioOfDataInIntersectionVolume(generateDistribution(optDistribution), optMBRs);
      if(tree.get_max_overlap() < pastOverlap) {
        LOG.finest(String.format(Locale.ENGLISH, "No %s split found%s; best data overlap was %.3f", (minEntries == tree.get_min_fanout() ? "minimum overlap" : "topological"), (maxEntries < node.getNumEntries() / 2 ? " in " + (revert ? "second" : "first") + " range" : ""), pastOverlap));
        return null;
      }
      break;
    case VOLUME_OVERLAP:
      if(Double.isNaN(optVolume)) {
        optVolume = SpatialUtil.volume(optMBRs[0]);
        optVolume += SpatialUtil.volume(optMBRs[1]);
      }
      pastOverlap = optXVolume / optVolume;
      if(tree.get_max_overlap() < pastOverlap) {
        LOG.finest(String.format(Locale.ENGLISH, "No %s split found%s; best volume overlap was %.3f", (minEntries == tree.get_min_fanout() ? "minimum overlap" : "topological"), (maxEntries < node.getNumEntries() / 2 ? " in " + (revert ? "second" : "first") + " range" : ""), pastOverlap));
        return null;
      }
      break;
    }

    return optDistribution;
  }

  /**
   * Generate the split distribution for a given sorting of entry positions
   * using the given split position <code>limit</code>. All
   * entries referenced by <code>entrySorting</code> from <code>0</code> to
   * <code>limit-1</code> are put into the first list (<code>ret[0]</code>), the
   * other entries are put into the second list (<code>ret[1]</code>).
   *
   * @param sorting this splitDistribution
   * @return the split distribution for the given sorting and split point
   */
  @SuppressWarnings("unchecked")
  private List<SpatialEntry>[] generateDistribution(SplitSorting sorting) {
    List<SpatialEntry>[] distibution;
    distibution = new List[2];
    distibution[0] = new ArrayList<>();
    distibution[1] = new ArrayList<>();
    List<SpatialEntry> sorted_entries = sorting.getSortedEntries();
    for(int i = 0; i < sorting.getSplitPoint(); i++) {
      distibution[0].add(sorted_entries.get(i));
    }
    for(int i = sorting.getSplitPoint(); i < node.getNumEntries(); i++) {
      distibution[1].add(sorted_entries.get(i));
    }
    return distibution;
  }

  /**
   * Generate the split sorting for a given sorting of entry positions
   * using the given split position <code>limit</code>.
   * All entries referenced by <code>entrySorting</code> from <code>0</code> to
   * <code>limit-1</code> are put into the first list (<code>ret[0]</code>), the
   * other entries are put into the second list (<code>ret[1]</code>).
   *
   * @param entrySorting entry sorting
   * @param limit split point
   * @return the split sorting for the given sorting and split point
   */
  private SplitSorting generateSplitSorting(int[] entrySorting, int limit) {
    List<SpatialEntry> sorting = new ArrayList<>();
    for(int i = 0; i < node.getNumEntries(); i++) {
      sorting.add(node.getEntry(entrySorting[i]));
    }
    return new SplitSorting(sorting, limit, splitAxis);
  }

  /**
   * Perform an minimum overlap split. The {@link #chooseMinimumOverlapSplit}
   * calculates the partition for the split dimension determined by
   * {@link #chooseSplitAxis}
   * <code>(common split history, minFanout, maxEntries - minFanout + 1)</code>
   * with the minimum overlap. This range may have been tested before (by the
   * {@link #topologicalSplit}), but for the minimum overlap test we need to
   * test that anew. Note that this method returns <code>null</code>, if the
   * minimum overlap split has a volume which is larger than the allowed
   * <code>maxOverlap</code> ratio or if the tree's minimum fanout is not larger
   * than the minimum directory size.
   *
   * @return distribution resulting from the minimum overlap split
   */
  public SplitSorting minimumOverlapSplit() {
    if(node.getEntry(0) instanceof LeafEntry) {
      throw new IllegalArgumentException("The minimum overlap split will only be performed on directory nodes");
    }
    if(node.getNumEntries() < 2) {
      throw new IllegalArgumentException("Splitting less than two entries is pointless.");
    }
    int maxEntries = tree.getDirCapacity() - 1;
    int minFanout = tree.get_min_fanout();
    if(node.getNumEntries() < maxEntries) {
      throw new IllegalArgumentException("This entry list has not yet reached the maximum limit: " + node.getNumEntries() + "<=" + maxEntries);
    }
    assert !(node.getEntry(0) instanceof LeafEntry);

    if(minFanout >= tree.getDirMinimum()) {
      // minFanout not set for allowing underflowing nodes
      return null;
    }
    IntIterator dimensionListing;
    if(node.getEntry(0) instanceof XTreeDirectoryEntry) {
      // filter common split dimensions
      dimensionListing = getCommonSplitDimensions(node);
      if(!dimensionListing.hasNext()) { // no common dimensions
        return null;
      }
    }
    else {
      // test all dimensions
      dimensionListing = new IntegerRangeIterator(0, node.getEntry(0).getDimensionality());
    }
    int formerSplitAxis = this.splitAxis;
    maxEntries = maxEntries + 1 - minFanout; // = maximum left-hand size
    chooseSplitAxis(dimensionListing, minFanout, maxEntries);
    // find the best split point
    if(formerSplitAxis == this.splitAxis && tree.getDirMinimum() > minFanout) {
      // remember: this follows an unsuccessful topological split
      // avoid duplicate computations of {minEntries, ..., maxEntries}
      double minOverlap = pastOverlap;
      // test {minFanout, ..., minEntries - 1}
      SplitSorting ret1 = chooseMinimumOverlapSplit(this.splitAxis, minFanout, tree.getDirMinimum() - 1, false);
      if(ret1 != null && pastOverlap < minOverlap) {
        minOverlap = pastOverlap; // this is a valid choice
      }
      // test {maxEntries - minEntries + 2, ..., maxEntries - minFanout + 1}
      SplitSorting ret2 = chooseMinimumOverlapSplit(this.splitAxis, minFanout, tree.getDirMinimum() - 1, true);
      if(ret2 == null) {
        // accept first range regardless of whether or not there is one
        pastOverlap = minOverlap;
        return ret1;
      }
      if(pastOverlap < minOverlap) { // the second range is better
        return ret2;
      }
      pastOverlap = minOverlap; // the first range is better
      return ret1;
    }
    else {
      return chooseMinimumOverlapSplit(this.splitAxis, minFanout, maxEntries, false);
    }
  }

  /**
   * Perform a topological (R*-Tree) split of a list of node entries.
   * <p>
   * Only distributions that have between <code>m</code> and <code>M-m+1</code>
   * entries in the first group will be tested.
   * 
   * @return chosen split distribution; note that this method returns null, if
   *         the minimum overlap split has a volume which is larger than the
   *         allowed <code>maxOverlap</code> ratio of #tree
   */
  public SplitSorting topologicalSplit() {
    if(node.getNumEntries() < 2) {
      throw new IllegalArgumentException("Splitting less than two entries is pointless.");
    }
    int minEntries = (node.getEntry(0) instanceof LeafEntry ? tree.getLeafMinimum() : tree.getDirMinimum());
    int maxEntries = (node.getEntry(0) instanceof LeafEntry ? tree.getLeafCapacity() - 1 : tree.getDirCapacity() - 1);
    if(node.getNumEntries() < maxEntries) {
      throw new IllegalArgumentException("This entry list has not yet reached the maximum limit: " + node.getNumEntries() + "<=" + maxEntries);
    }

    maxEntries = maxEntries + 1 - minEntries;

    chooseSplitAxis(new IntegerRangeIterator(0, node.getEntry(0).getDimensionality()), minEntries, maxEntries);
    return chooseMinimumOverlapSplit(splitAxis, minEntries, maxEntries, false);
  }

  /**
   * Computes and returns the mbr of the specified nodes, only the nodes between
   * from and to index are considered.
   *
   * @param entries the array of node indices
   * @param from the start index
   * @param to the end index
   * @return the mbr of the specified nodes
   */
  private HyperBoundingBox mbr(final int[] entries, final int from, final int to) {
    SpatialEntry first = this.node.getEntry(entries[from]);
    ModifiableHyperBoundingBox mbr = new ModifiableHyperBoundingBox(first);
    for(int i = from + 1; i < to; i++) {
      mbr.extend(this.node.getEntry(entries[i]));
    }
    return mbr;
  }

  /**
   * Get the ratio of data objects in the intersection volume (weighted
   * overlap).
   *
   * @param split two entry lists representing the given split
   * @param mbrs the MBRs for the given split
   * @return the ration of data objects in the intersection volume as value
   *         between 0 and 1
   */
  public double getRatioOfDataInIntersectionVolume(List<SpatialEntry>[] split, HyperBoundingBox[] mbrs) {
    final ModifiableHyperBoundingBox xMBR = SpatialUtil.intersection(mbrs[0], mbrs[1]);
    if(xMBR == null) {
      return 0.;
    }
    // Total number of entries, intersecting entries
    int[] numOf = { 0, 0 };
    countXingDataEntries(split[0], xMBR, numOf);
    countXingDataEntries(split[1], xMBR, numOf);

    return numOf[1] / (double) numOf[0];
  }

  /**
   * Count all data objects under entries and whether they intersect the given
   * MBR <code>mbr</code> into <code>numOf</code>.
   * 
   * @param entries
   * @param mbr
   * @param numOf array of two integers, the first one is to be filled with the
   *        total number of data objects, the second one with the number of data
   *        objects intersecting <code>mbr</code>
   * @return == the (probably modified) integer array <code>numOf</code>: the
   *         first field is the total number of data objects, the second the
   *         number of data objects intersecting <code>mbr</code>
   */
  private int[] countXingDataEntries(final Collection<SpatialEntry> entries, final HyperBoundingBox mbr, int[] numOf) {
    for(SpatialEntry entry : entries) {
      if(entry instanceof LeafEntry) {
        numOf[0]++;
        if(SpatialUtil.intersects(mbr, entry)) {
          numOf[1]++;
        }
      }
      else {
        N node = tree.getNode(((DirectoryEntry) entry).getPageID());
        countXingDataEntries(node.getEntries(), mbr, numOf);
      }
    }
    return numOf;
  }

  /**
   * Container for a split result, consisting of a list of sorted entries and
   * the determined split point.
   * 
   * @author Marisa Thoma
   */
  public static class SplitSorting {
    private List<SpatialEntry> sortedEntries;

    private int splitPoint, splitAxis;

    public SplitSorting(List<SpatialEntry> sortedEntries, int splitPoint, int splitAxis) {
      this.sortedEntries = sortedEntries;
      this.splitPoint = splitPoint;
      this.splitAxis = splitAxis;
    }

    public List<SpatialEntry> getSortedEntries() {
      return sortedEntries;
    }

    public int getSplitPoint() {
      return splitPoint;
    }

    public int getSplitAxis() {
      return splitAxis;
    }
  }

  // getters and setters

  public double getPastOverlap() {
    return pastOverlap;
  }
}
