package experimentalcode.marisa.index.xtree.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeNode;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;
import experimentalcode.marisa.index.xtree.XDirectoryEntry;
import experimentalcode.marisa.index.xtree.XNode;
import experimentalcode.marisa.index.xtree.XTreeBase;

/**
 * Provides methods for splitting X-tree nodes.
 * 
 * @author Marisa Thoma
 */
public class XSplitter<E extends SpatialEntry, ET extends E, N extends XNode<E, N>, T extends XTreeBase<?, N, E>> {

  /** Logger object for the XSplitter. */
  private transient Logger logger = Logger.getLogger(XSplitter.class.getName());

  private EntryTypeDimensionalComparator etdc;

  /** The split axis. */
  private int splitAxis = 0;

  private List<ET> entries = null;

  /** Selected maximum overlap strategy. */
  private int maxOverlapStrategy = XTreeBase.VOLUME_OVERLAP;

  /** The tree calling this splitter. */
  private T tree;

  /** The overlap calculated in the last minimum overlap split. */
  private double pastOverlap = -1;

  /**
   * Initialize a split helper for <code>entries</code> using the given
   * maxOverlapStrategy.
   * 
   * @param entries
   * @param maxOverlapStrategy
   */
  public XSplitter(T tree, List<ET> entries) {
    this.tree = tree;
    this.maxOverlapStrategy = tree.get_overlap_type();
    etdc = new EntryTypeDimensionalComparator(0, false, null);
    this.entries = entries;
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
   * @param <T>
   * @param minEntries minimally allowed subgroup size
   * @param maxEntries maximally allowed subgroup size; if
   *        <code>&lt; entries.size()</code>, ONLY the first half (
   *        <code>[minEntries,maxEntries]</code>) of the possible partitions is
   *        calculated
   * @param entriesByLB entries sorted by lower bound value of some dimension
   * @param entriesByUB entries sorted by upper bound value of some dimension
   * @return the sum of all resulting MBRs' surface sums of the partitions
   *         ranging in <code>{minEntries, ..., maxEntries</code> in both of the
   *         input orderings
   */
  private double generateDistributionsAndSurfaceSums(int minEntries, int maxEntries, Integer[] entriesByLB, Integer[] entriesByUB) {
    double surfaceSum = 0;
    for(int limit = minEntries; limit <= maxEntries; limit++) {
      HyperBoundingBox mbr1 = mbr(entriesByLB, 0, limit);
      HyperBoundingBox mbr2 = mbr(entriesByLB, limit, entriesByLB.length);
      surfaceSum += mbr1.perimeter() + mbr2.perimeter();
      mbr1 = mbr(entriesByUB, 0, limit);
      mbr2 = mbr(entriesByUB, limit, entriesByUB.length);
      surfaceSum += mbr1.perimeter() + mbr2.perimeter();
    }
    return surfaceSum;
  }

  /**
   * Sort the entries of {@link #entries} into the arrays
   * <code>entriesByLB</code> and <code>entriesByUB</code> by lower and upper
   * bound of dimension <code>d</code>.
   * 
   * @param d selected dimension
   * @param entriesByLB entry array to be sorted by the lower bound of
   *        <code>d</code> (must be initialized to a valid selection of indices
   *        of {@link #entries}
   * @param entriesByUB entry array to be sorted by the upper bound of
   *        <code>d</code> (must be initialized to a valid selection of indices
   *        of {@link #entries}
   */
  private void sortEntriesForDimension(int d, Integer[] entriesByLB, Integer[] entriesByUB) {
    // Lists which hold entries sorted by their lower and
    // upper bounds in the current dimension.
    etdc.set(d, true, entries);
    Arrays.sort(entriesByLB, etdc);
    etdc.setLb(false);
    Arrays.sort(entriesByUB, etdc);
  }

  /**
   * Compares two EntryType objects by lower or upper bound.
   * 
   * @author Marisa Thoma
   */
  private final class EntryTypeDimensionalComparator implements Comparator<Integer> {
    private int dimension;

    private boolean lb;

    private double d1, d2;

    private List<ET> entries;

    public EntryTypeDimensionalComparator(int dimension, boolean lb, List<ET> entries) {
      this.dimension = dimension;
      this.lb = lb;
      this.entries = entries;
    }

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Integer o1, Integer o2) {
      if(lb) {
        d1 = entries.get(o1).getMBR().getMin(dimension + 1);
        d2 = entries.get(o2).getMBR().getMin(dimension + 1);
      }
      else {
        d1 = entries.get(o1).getMBR().getMax(dimension + 1);
        d2 = entries.get(o2).getMBR().getMax(dimension + 1);
      }
      // ignore NaN case
      return (d1 > d2 ? 1 : (d1 < d2 ? -1 : 0));
    }

    public final void set(int dimension, boolean lb, List<ET> entries) {
      this.lb = lb;
      this.dimension = dimension;
      this.entries = entries;
    }

    public final void setLb(boolean lb) {
      this.lb = lb;
    }
  }

  /**
   * Determine the common split dimensions from a list of entries.
   * 
   * @param entries directory entries for which to determine the common split
   *        dimensions
   * @return common split dimensions
   */
  private Collection<Integer> getCommonSplitDimensions(Collection<ET> entries) {
    Collection<SplitHistory> splitHistories = new ArrayList<SplitHistory>(entries.size());
    for(ET entry : entries) {
      if(entry instanceof XDirectoryEntry) {
        splitHistories.add(((XDirectoryEntry) entry).getSplitHistory());
      }
      else {
        throw new RuntimeException("Wrong entry type to derive split dimension from: " + entry.getClass().getName());
      }
    }
    return SplitHistory.getCommonDimensions(splitHistories);
  }

  /**
   * <p>
   * Get the dimension with the minimum surface sum.
   * </p>
   * <p>
   * This is done by calculating every possible split in each given dimension
   * and the sum of surfaces of all splits for every dimension.
   * </p>
   * 
   * @param dimensionIterable The dimensions that should be tested
   * @param minEntries Minimum number of entries in each sub group; in case of a
   *        data node, this is the minimum leaf capacity, for directory nodes,
   *        this is either the minimum directory capacity or, if a topological
   *        split has already failed, the tree's minimum fan-out parameter.
   * @param maxEntries Maximum number of entries in each sub group
   * @return The dimension with the minimum surface sum, or null if
   *         dimensionIterable yielded nothing
   */
  private int chooseSplitAxis(Iterable<Integer> dimensionIterable, int minEntries, int maxEntries) {
    // assert that there ARE dimensions to be tested
    if(!dimensionIterable.iterator().hasNext())
      return -1;

    int numOfEntries = entries.size();

    assert minEntries >= 1 && minEntries < maxEntries && maxEntries <= numOfEntries;

    double optSurfaceSum = Double.POSITIVE_INFINITY;
    int optAxis = -1;

    Integer[] entriesByLB = new Integer[entries.size()];
    for(int i = 0; i < entriesByLB.length; i++) {
      entriesByLB[i] = i;
    }
    Integer[] entriesByUB = Arrays.copyOf(entriesByLB, entries.size());

    Integer[] entriesByLBRev = null, entriesByUBRev = null;
    if(maxEntries <= entries.size() / 2) {
      // initialize backwards direction
      entriesByLBRev = Arrays.copyOf(entriesByLB, entries.size());
      entriesByUBRev = Arrays.copyOf(entriesByLB, entries.size());
    }

    for(Integer d : dimensionIterable) {
      sortEntriesForDimension(d, entriesByLB, entriesByUB);
      double surfaceSum = generateDistributionsAndSurfaceSums(minEntries, maxEntries, entriesByLB, entriesByUB);
      if(maxEntries <= entries.size() / 2) { // add opposite ranges
        for(int j = 0; j < entriesByUB.length; j++) {
          // reverse sorting
          entriesByUBRev[entries.size() - 1 - j] = entriesByUB[j];
          entriesByLBRev[entries.size() - 1 - j] = entriesByLB[j];
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
   * @param revert if <code>maxEntrie &lt; {@link #entries}.size()</code> and <code>revert</code>
   *        is <code>true</code>, the upper range of the sorting is tested, i.e.
   *        not <code>{minEntries, ..., maxEntries}</code> but
   * 
   *        <code>{{@link #entries}.size() - maxEntries + 1, ..., {@link #entries}.size() - minEntries + 1}</code>
   * @return The distribution with the minimal intersection volume or
   *         <code>null</code>, if the minimum overlap split has a volume which
   *         is larger than the allowed <code>maxOverlap</code> ratio
   */
  private SplitSorting chooseMinimumOverlapSplit(int splitAxis, int minEntries, int maxEntries, boolean revert) {
    if(splitAxis != -1) {
      double optXVolume = Double.POSITIVE_INFINITY;
      double optVolume = Double.POSITIVE_INFINITY;
      SplitSorting optDistribution = null;
      HyperBoundingBox[] optMBRs = null;

      // generate sortings for the mbr's extrema
      Integer[] entrySorting = new Integer[entries.size()];
      for(int i = 0; i < entrySorting.length; i++) {
        entrySorting[i] = i;
      }
      Integer[] lbSorting = Arrays.copyOf(entrySorting, entrySorting.length);
      Integer[] ubSorting = Arrays.copyOf(entrySorting, entrySorting.length);
      sortEntriesForDimension(splitAxis, entrySorting, entrySorting);
      if(revert && maxEntries < entries.size() / 2) {
        // test reverted sortings
        int[][] reverted = new int[2][entries.size()]; // temp array
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
          double xVolume = getIntersectionVolume(mbr1, mbr2);
          if(xVolume < optXVolume) {
            optXVolume = xVolume;
            optDistribution = generateSplitSorting(entrySorting, limit);
            optMBRs = new HyperBoundingBox[] { mbr1, mbr2 };
            optVolume = Double.NaN;
          }
          else if(xVolume == optXVolume) {
            double vol = mbr1.volume();
            vol += mbr2.volume();
            if(Double.isNaN(optVolume)) {
              // calculate when necessary
              optVolume = optMBRs[0].volume();
              optVolume += optMBRs[1].volume();
            }
            if(vol < optVolume) {
              optXVolume = xVolume;
              optVolume = vol;
              optDistribution = generateSplitSorting(entrySorting, limit);
            }
          }
        }
      }
      if(!entries.get(0).isLeafEntry() && tree.get_max_overlap() < 1) { // test
        // overlap
        if(maxOverlapStrategy == XTreeBase.DATA_OVERLAP) {
          double overlap = getRatioOfDataInIntersectionVolume(this.generateDistribution(optDistribution), optMBRs);
          if(tree.get_max_overlap() < overlap) {
            logger.finest(String.format(Locale.ENGLISH, "No %s split found%s; best data overlap was %.3f", (minEntries == tree.get_min_fanout() ? "minimum overlap" : "topological"), (maxEntries < entries.size() / 2 ? " in " + (revert ? "second" : "first") + " range" : ""), overlap));
            return null;
          }
        }
        else { // strategy == VOLUME_OVERLAP
          if(Double.isNaN(optVolume)) {
            optVolume = optMBRs[0].volume();
            optVolume += optMBRs[1].volume();
          }
          if(tree.get_max_overlap() < optXVolume / optVolume) {
            logger.finest(String.format(Locale.ENGLISH, "No %s split found%s; best volume overlap was %.3f", (minEntries == tree.get_min_fanout() ? "minimum overlap" : "topological"), (maxEntries < entries.size() / 2 ? " in " + (revert ? "second" : "first") + " range" : ""), optXVolume / optVolume));
            return null;
          }
        }
      }

      return optDistribution;
    }
    else {
      return null;
    }
  }

  /**
   * Generate the split distribution for a given sorting of entry positions in
   * {@link #entries} using the given split position <code>limit</code>. All
   * entries referenced by <code>entrySorting</code> from <code>0</code> to
   * <code>limit-1</code> are put into the first list (<code>ret[0]</code>), the
   * other entries are put into the second list (<code>ret[1]</code>).
   * 
   * @param entrySorting this splitDistribution
   * @return the split distribution for the given sorting and split point
   */
  private List<ET>[] generateDistribution(SplitSorting sorting) {
    List<ET>[] distibution;
    distibution = new List[2];
    distibution[0] = new ArrayList<ET>();
    distibution[1] = new ArrayList<ET>();
    List<ET> sorted_entries = sorting.getSortedEntries();
    for(int i = 0; i < sorting.getSplitPoint(); i++)
      distibution[0].add(sorted_entries.get(i));
    for(int i = sorting.getSplitPoint(); i < entries.size(); i++)
      distibution[1].add(sorted_entries.get(i));
    return distibution;
  }

  /**
   * Generate the split sorting for a given sorting of entry positions in
   * {@link #entries} using the given split position <code>limit</code>. All
   * entries referenced by <code>entrySorting</code> from <code>0</code> to
   * <code>limit-1</code> are put into the first list (<code>ret[0]</code>), the
   * other entries are put into the second list (<code>ret[1]</code>).
   * 
   * @param entrySorting entry sorting
   * @param limit split point
   * @return the split sorting for the given sorting and split point
   */
  private SplitSorting generateSplitSorting(Integer[] entrySorting, int limit) {
    List<ET> sorting = new ArrayList<ET>();
    for(int i = 0; i < entries.size(); i++)
      sorting.add(entries.get(entrySorting[i]));
    return new SplitSorting(sorting, limit);
  }

  /**
   * Perform an minimum overlap split. The minimum overlap split calculates the
   * minimum overlap split for all legal partitions for the dimensions fitting
   * the split dimension of range
   * <code>{minFanout, ..., maxEntries - minFanout + 1}</code>. In part, this
   * range has been tested before, but for the minimum overlap test we need to
   * test that anew. Note that this method returns <code>null</code>, if the
   * minimum overlap split has a volume which is larger than the allowed
   * <code>maxOverlap</code> ratio
   * 
   * @return distribution resulting from the minimum overlap split
   */
  @SuppressWarnings("unchecked")
  public SplitSorting minimumOverlapSplit() {
    if(entries.get(0).isLeafEntry())
      throw new IllegalArgumentException("The minimum overlap split will only be performed on directory nodes");
    if(entries.size() < 2) {
      throw new IllegalArgumentException("Splitting less than two entries is pointless.");
    }
    int maxEntries = tree.getDirCapacity();
    int minFanout = tree.get_min_fanout();
    if(entries.size() < maxEntries)
      throw new IllegalArgumentException("This entry list has not yet reached the maximum limit: " + entries.size() + "<=" + maxEntries);
    assert !entries.get(0).isLeafEntry();

    if((minFanout < tree.getDirMinimum())) {
      Iterable<Integer> dimensionListing;
      if(entries.get(0) instanceof XDirectoryEntry) {
        // filter common split dimensions
        dimensionListing = getCommonSplitDimensions(entries);
      }
      else {
        // test all dimensions
        dimensionListing = new Range(0, entries.get(0).getDimensionality());
      }
      int formerSplitAxis = this.splitAxis;
      maxEntries = maxEntries + 1 - minFanout; // = maximum left-hand size
      int splitAxis = chooseSplitAxis(dimensionListing, minFanout, maxEntries);
      // find the best split point
      if(formerSplitAxis == splitAxis && tree.getDirMinimum() > minFanout) {
        // remember: this follows an unsuccessful topological split
        // avoid duplicate computations of {minEntries, ..., maxEntries}
        double minOverlap = Double.MAX_VALUE;
        // test {minFanout, ..., minEntries - 1}
        SplitSorting ret1 = chooseMinimumOverlapSplit(splitAxis, minFanout, tree.getDirMinimum() - 1, false);
        if(ret1 != null && pastOverlap < minOverlap)
          minOverlap = pastOverlap; // this is a valid choice
        // test {maxEntries - minEntries + 2, ..., maxEntries - minFanout + 1}
        SplitSorting ret2 = chooseMinimumOverlapSplit(splitAxis, minFanout, tree.getDirMinimum(), true);
        if(ret2 == null) {
          // accept first range regardless of whether or not there is one
          pastOverlap = minOverlap;
          return ret1;
        }
        if(pastOverlap < minOverlap) // the second range is better
          return ret2;
        pastOverlap = minOverlap; // the first range is better
        return ret1;
      }
      else {
        return chooseMinimumOverlapSplit(splitAxis, minFanout, maxEntries, false);
      }
    }
    else { // minFanout not set for allowing underflowing nodes
      return null;
    }
  }

  /**
   * <p>
   * Perform a topological (R*-Tree) split of a list of node entries.
   * </p>
   * <p>
   * Only distributions that have between <code>m</code> and <code>M-m+1</code>
   * entries in the first group will be tested.
   * </p>
   * 
   * @see "Beckmann, Kriegel, Schneider, Seeger: The R*-tree: An Efficient and
   *      Robust Access Method for Points and Rectangles, ACM SIGMOD Int. Conf.
   *      on Management of Data (SIGMOD'90), Atlantic City, NJ, 1990, pp.
   *      322-331"
   * @param entries list of at least 2 entries to be split
   * @return chosen split distribution; note that this method returns null, if
   *         the minimum overlap split has a volume which is larger than the
   *         allowed <code>maxOverlap</code> ratio of #tree
   */
  public SplitSorting topologicalSplit() {
    if(entries.size() < 2) {
      throw new IllegalArgumentException("Splitting less than two entries is pointless.");
    }
    int minEntries = (entries.get(0).isLeafEntry() ? tree.getLeafMinimum() : tree.getDirMinimum());
    int maxEntries = (entries.get(0).isLeafEntry() ? tree.getLeafCapacity() : tree.getDirCapacity());
    if(entries.size() < maxEntries)
      throw new IllegalArgumentException("This entry list has not yet reached the maximum limit: " + entries.size() + "<=" + maxEntries);

    maxEntries = maxEntries + 1 - minEntries;

    int splitAxis = chooseSplitAxis(new Range(0, entries.get(0).getDimensionality()), minEntries, maxEntries);
    return chooseMinimumOverlapSplit(splitAxis, minEntries, maxEntries, false);
  }

  /**
   * Computes and returns the mbr of the specified nodes, only the nodes between
   * from and to index are considered.
   * 
   * @param entries the array of node indices in {@link #entries}
   * @param from the start index
   * @param to the end index
   * @return the mbr of the specified nodes
   */
  private HyperBoundingBox mbr(final Integer[] entries, final int from, final int to) {
    double[] min = new double[this.entries.get(entries[from]).getMBR().getDimensionality()];
    double[] max = new double[this.entries.get(entries[from]).getMBR().getDimensionality()];

    HyperBoundingBox currMBR = this.entries.get(entries[from]).getMBR();

    for(int d = 1; d <= min.length; d++) {
      min[d - 1] = currMBR.getMin(d);
      max[d - 1] = currMBR.getMax(d);
    }

    for(int i = from + 1; i < to; i++) {
      currMBR = this.entries.get(entries[i]).getMBR();
      for(int d = 1; d <= min.length; d++) {
        if(min[d - 1] > currMBR.getMin(d)) {
          min[d - 1] = currMBR.getMin(d);
        }
        if(max[d - 1] < currMBR.getMax(d)) {
          max[d - 1] = currMBR.getMax(d);
        }
      }
    }
    return new HyperBoundingBox(min, max);
  }

  /**
   * Calculate the intersection volume of the two MBRs.
   * 
   * @param m1
   * @param m2
   * @return intersection volume
   */
  public static double getIntersectionVolume(final HyperBoundingBox m1, final HyperBoundingBox m2) {
    final int dimensionality = m1.getDimensionality();

    double volume = 1.0, len;
    for(int d = 1; d <= dimensionality; d++) {
      len = (m1.getMax(d) < m2.getMax(d) ? m1.getMax(d) : m2.getMax(d)) - (m1.getMin(d) > m2.getMin(d) ? m1.getMin(d) : m2.getMin(d));
      if(len <= 0.) {
        return 0.;
      }
      volume *= len;
    }
    assert volume >= 0;
    return volume;
  }

  /**
   * Calculate the intersection of the two MBRs or <code>null</code> if they do
   * not intersect. <em>Note</em>: if the given MBRs intersect in only one point
   * of any dimension, this method still returns a result!
   * 
   * @param m1
   * @param m2
   * @return intersection volume
   */
  public static HyperBoundingBox getIntersection(final HyperBoundingBox m1, final HyperBoundingBox m2) {
    final int dimensionality = m1.getDimensionality();
    double[] min = new double[dimensionality];
    double[] max = new double[dimensionality];
    for(int d = 1; d <= dimensionality; d++) {
      min[d - 1] = (m1.getMin(d) > m2.getMin(d) ? m1.getMin(d) : m2.getMin(d));
      max[d - 1] = (m1.getMax(d) < m2.getMax(d) ? m1.getMax(d) : m2.getMax(d));
      if(min[d - 1] > max[d - 1]) {
        return null;
      }
    }
    return new HyperBoundingBox(min, max);
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
  public double getRatioOfDataInIntersectionVolume(List<ET>[] split, HyperBoundingBox[] mbrs) {
    final HyperBoundingBox xMBR = getIntersection(mbrs[0], mbrs[1]);
    if(xMBR == null) {
      return 0.;
    }
    // Total number of entries, intersecting entries
    int[] numOf = { 0, 0 };
    countXingDataEntries(split[0], xMBR, numOf);
    countXingDataEntries(split[1], xMBR, numOf);

    return ((double) numOf[1]) / ((double) numOf[0]);
  }

  /**
   * Count all data objects under entries and whether they intersect the given
   * MBR <code>mbr</code>.
   * 
   * @param entries
   * @param mbr
   * @return array of two integers, the first one is the total number of data
   *         objects, the second one the number of data objects intersecting MBR
   */
  private <ET2 extends SpatialEntry> int[] countXingDataEntries(final Collection<ET2> entries, final HyperBoundingBox mbr, int[] numOf) {
    for(ET2 entry : entries) {
      if(entry.isLeafEntry()) {
        numOf[0]++;
        if(mbr.intersects(entry.getMBR())) {
          numOf[1]++;
        }
      }
      else {
        N node = tree.getNode(entry.getID());
         countXingDataEntries(node.getChildren(), mbr, numOf);
      }
    }
    return numOf;
  }

  /**
   * Iterator provider for an integer range from <code>from</code> to
   * <code>to</code>. Covers <code>[from,to[</code>.
   */
  class Range implements Iterable<Integer> {

    private int from, to;

    public Range(int from, int to) {
      this.from = from;
      this.to = to;
    }

    @Override
    public Iterator<Integer> iterator() {
      return new Iterator<Integer>() {
        int i = from;

        @Override
        public boolean hasNext() {
          return i < to;
        }

        @Override
        public Integer next() {
          return i++;
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }

      };
    }
  }

  /**
   * Container for a split result, consisting of a list of sorted entries and
   * the determined split point.
   * 
   * @author Marisa Thoma
   * 
   */
  public class SplitSorting {
    private List<ET> sortedEntries;

    private int splitPoint;

    public SplitSorting(List<ET> sortedEntries, int splitPoint) {
      this.sortedEntries = sortedEntries;
      this.splitPoint = splitPoint;
    }

    public List<ET> getSortedEntries() {
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
