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
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.SparseFeatureVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.result.FrequentItemsetsResult;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * FP-Growth is an algorithm for mining the frequent itemsets by using a
 * compressed representation of the database called {@link FPTree}.
 * <p>
 * FP-Growth first sorts items by the overall frequency, since having high
 * frequent items appear first in the tree leads to a much smaller tree since
 * frequent subsets will likely share the same path in the tree. FP-Growth is
 * beneficial when you have a lot of (near-) duplicate transactions, and are
 * using a not too high support threshold, as it only prunes single items, not
 * item combinations.
 * <p>
 * This implementation is in-memory only, and has not yet been carefully
 * optimized.
 * <p>
 * The worst case memory use probably is \(O(\min(n\cdot l,i^l))\) where i is the
 * number of items, l the average itemset length, and n the number of items. The
 * worst case scenario is when every item is frequent, and every transaction is
 * unique. The resulting tree will then be larger than the original data.
 * <p>
 * Reference:
 * <p>
 * J. Han, J. Pei, Y. Yin<br>
 * Mining frequent patterns without candidate generation<br>
 * In Proc. ACM SIGMOD Int. Conf. Management of Data (SIGMOD 2000)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - - - FPTree
 * @has - - - Itemset
 * @has - produces - FrequentItemsetsResult
 */
@Reference(authors = "J. Han, J. Pei, Y. Yin", //
    title = "Mining frequent patterns without candidate generation", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. Management of Data (SIGMOD 2000)", //
    url = "https://doi.org/10.1145/342009.335372", //
    bibkey = "DBLP:conf/sigmod/HanPY00")
@Priority(Priority.RECOMMENDED)
public class FPGrowth extends AbstractFrequentItemsetAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(FPGrowth.class);

  /**
   * Prefix for statistics.
   */
  private static final String STAT = FPGrowth.class.getName() + ".";

  /**
   * Constructor.
   *
   * @param minsupp Minimum support (relative or absolute)
   * @param minlength Minimum length
   * @param maxlength Maximum length
   */
  public FPGrowth(double minsupp, int minlength, int maxlength) {
    super(minsupp, minlength, maxlength);
  }

  /**
   * Run the FP-Growth algorithm
   *
   * @param db Database to process
   * @param relation Bit vector relation
   * @return Frequent patterns found
   */
  public FrequentItemsetsResult run(Database db, final Relation<BitVector> relation) {
    // TODO: implement with resizable array, to not need dim.
    final int dim = RelationUtil.dimensionality(relation);
    final VectorFieldTypeInformation<BitVector> meta = RelationUtil.assumeVectorField(relation);
    // Compute absolute minsupport
    final int minsupp = getMinimumSupport(relation.size());

    LOG.verbose("Finding item frequencies for ordering.");
    final int[] counts = countItemSupport(relation, dim);
    // Forward and backward indexes
    int[] iidx = new int[dim];
    final int[] idx = buildIndex(counts, iidx, minsupp);
    final int items = idx.length;

    LOG.statistics(new LongStatistic(STAT + "raw-items", dim));
    LOG.statistics(new LongStatistic(STAT + "raw-transactions", relation.size()));
    LOG.statistics(new DoubleStatistic(STAT + "minsupp-relative", minsupp / (double) relation.size()));
    LOG.statistics(new LongStatistic(STAT + "minsupp-absolute", minsupp));

    LOG.verbose("Building FP-Tree.");
    Duration ctime = LOG.newDuration(STAT + "fp-tree.construction.time").begin();
    FPTree tree = buildFPTree(relation, iidx, items);
    if(LOG.isStatistics()) {
      tree.logStatistics();
    }
    if(LOG.isDebuggingFinest()) {
      StringBuilder buf = new StringBuilder(10000).append("FP-tree:\n");
      tree.appendTo(buf, new FPNode.Translator() {
        @Override
        public StringBuilder appendTo(StringBuilder buf, int i) {
          String l = meta.getLabel(idx[i]);
          return (l != null) ? buf.append(l) : buf.append(i);
        }
      });
      LOG.debugFinest(buf.toString());
    }
    // Reduce memory usage:
    tree.reduceMemory();
    LOG.statistics(ctime.end());

    LOG.verbose("Extracting frequent patterns.");
    Duration etime = LOG.newDuration(STAT + "fp-growth.extraction.time").begin();
    final IndefiniteProgress itemp = LOG.isVerbose() ? new IndefiniteProgress("Frequent itemsets", LOG) : null;
    final List<Itemset> solution = new ArrayList<>();
    // Start extraction with the least frequent items
    tree.extract(minsupp, minlength, maxlength, true, new FPTree.Collector() {
      @Override
      public void collect(int support, int[] data, int start, int plen) {
        // Always translate the indexes back to the original values via 'idx'!
        if(plen - start == 1) {
          solution.add(new OneItemset(idx[data[start]], support));
          LOG.incrementProcessed(itemp);
          return;
        }
        // Copy from buffer to a permanent storage
        int[] indices = new int[plen - start];
        for(int i = start, j = 0; i < plen; i++) {
          indices[j++] = idx[data[i]]; // Translate to original items
        }
        Arrays.sort(indices);
        solution.add(new SparseItemset(indices, support));
        LOG.incrementProcessed(itemp);
      }
    });
    LOG.setCompleted(itemp);
    Collections.sort(solution);
    LOG.statistics(etime.end());
    LOG.statistics(new LongStatistic(STAT + "frequent-itemsets", solution.size()));

    return new FrequentItemsetsResult("FP-Growth", "fp-growth", solution, meta, relation.size());
  }

  /**
   * Count the support of each 1-item.
   *
   * @param relation Data
   * @param dim Maximum dimensionality
   * @return Item counts
   */
  private int[] countItemSupport(final Relation<BitVector> relation, final int dim) {
    final int[] counts = new int[dim];
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Finding frequent 1-items", relation.size(), LOG) : null;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      SparseFeatureVector<?> bv = relation.get(iditer);
      // TODO: only count those which satisfy minlength?
      for(int it = bv.iter(); bv.iterValid(it); it = bv.iterAdvance(it)) {
        counts[bv.iterDim(it)]++;
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    return counts;
  }

  /**
   * Build the actual FP-tree structure.
   *
   * @param relation Data
   * @param iidx Inverse index (dimension to item rank)
   * @param items Number of items
   * @return FP-tree
   */
  private FPTree buildFPTree(final Relation<BitVector> relation, int[] iidx, final int items) {
    FPTree tree = new FPTree(items);
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Building FP-tree", relation.size(), LOG) : null;
    int[] buf = new int[items];
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      // Convert item to index representation:
      int l = 0;
      SparseFeatureVector<?> bv = relation.get(iditer);
      for(int it = bv.iter(); bv.iterValid(it); it = bv.iterAdvance(it)) {
        int i = iidx[bv.iterDim(it)];
        if(i < 0) {
          continue; // Skip non-frequent items
        }
        buf[l++] = i;
      }
      // Skip too short entries
      if(l >= minlength) {
        Arrays.sort(buf, 0, l); // Sort ascending
        tree.insert(buf, 0, l, 1);
      }
      LOG.incrementProcessed(prog);
    }
    LOG.ensureCompleted(prog);
    return tree;
  }

  /**
   * Build a forward map, item id (dimension) to frequency position
   *
   * @param counts Item counts
   * @param positions Position index (output)
   * @param minsupp Minimum support
   * @return Forward index
   */
  private int[] buildIndex(final int[] counts, int[] positions, int minsupp) {
    // Count the number of frequent items:
    int numfreq = 0;
    for(int i = 0; i < counts.length; i++) {
      if(counts[i] >= minsupp) {
        ++numfreq;
      }
    }
    // Build the index table
    int[] idx = new int[numfreq];
    for(int i = 0, j = 0; i < counts.length; i++) {
      if(counts[i] >= minsupp) {
        idx[j++] = i;
      }
    }
    IntegerArrayQuickSort.sort(idx, (x, y) -> Integer.compare(counts[y], counts[x]));
    Arrays.fill(positions, -1);
    for(int i = 0; i < idx.length; i++) {
      positions[idx[i]] = i;
    }
    return idx;
  }

  /**
   * FP-Tree data structure
   *
   * @author Erich Schubert
   *
   * @composed - - - FPNode
   * @assoc - - - Collector
   */
  public static class FPTree extends FPNode {
    /**
     * Header table
     */
    FPNode[] header;

    /**
     * Number of nodes in the tree (statistics only).
     */
    int nodes = 1;

    /**
     * Constructor.
     *
     * @param items Number of items in header table
     */
    public FPTree(int items) {
      super(null, -1);
      header = new FPNode[items];
    }

    /**
     * Insert an itemset into the tree.
     *
     * @param buf Buffer
     * @param i Start position in buffer
     * @param l End position in buffer
     * @param weight Weight
     */
    public void insert(int[] buf, int i, int l, int weight) {
      insert(this, buf, i, l, weight);
    }

    /**
     * Create a new node of the FP-tree, linking it into the header table.
     *
     * @param parent Parent node
     * @param label Node label
     * @return New node
     */
    public FPNode newNode(FPNode parent, int label) {
      FPNode node = new FPNode(parent, label);
      // Prepend to linked list - there is no benefit in keeping a particular
      // order, as far as I can tell.
      node.sibling = header[label];
      header[label] = node;
      ++nodes; // Statistics tracking only
      return node;
    }

    /**
     * Extract itemsets ending in the given item.
     *
     * @param minsupp Minimum support
     * @param minlength Minimum length
     * @param maxlength Maximum length
     * @param destruct Remove nodes
     * @param col Itemset collector
     */
    public void extract(int minsupp, int minlength, int maxlength, boolean destruct, Collector col) {
      int[] buf = new int[header.length], buf2 = new int[header.length],
          buf3 = new int[header.length];
      int stop = (minlength > 1) ? minlength - 1 : 0;
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Extracting itemsets", header.length - stop, LOG) : null;
      for(int j = header.length - 1; j >= stop; --j) {
        extract(minsupp, minlength, maxlength, j, buf, 0, buf2, buf3, destruct, col);
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
    }

    /**
     * Extract itemsets ending in the given item.
     *
     * @param minsupp Minimum support
     * @param minlength Minimum length
     * @param maxlength Maximum length
     * @param item Current item
     * @param postfix Items to append
     * @param plen Postfix length
     * @param buf2 Scratch buffer
     * @param buf3 Scratch buffer
     * @param destruct Remove nodes
     * @param col Itemset collector
     */
    private void extract(int minsupp, int minlength, int maxlength, int item, int[] postfix, int plen, int[] buf2, int[] buf3, boolean destruct, Collector col) {
      // Skip items that have disappeared from the tree
      if(header[item] == null) {
        return;
      }
      // No siblings, no children: single path only.
      if(header[item].sibling == null && header[item].numchildren == 0) {
        if(header[item].count >= minsupp) {
          extractLinear(header[item].count, minlength, maxlength, item, postfix, plen, buf2, col);
        }
        if(destruct) {
          header[item].parent.numchildren--;
          header[item] = null;
        }
        return;
      }
      // Count total support.
      int support = 0;
      for(FPNode cur = header[item]; cur != null; cur = cur.sibling) {
        support += cur.count;
      }
      if(support < minsupp) {
        return;
      }
      // Check which parent items to keep in the projection.
      Arrays.fill(buf3, 0);
      for(FPNode cur = header[item]; cur != null; cur = cur.sibling) {
        for(FPNode parent = cur.parent; parent.key >= 0; parent = parent.parent) {
          buf3[parent.key] += cur.count;
        }
      }
      // For testing minimum length:
      final int mminlength = minlength - (plen + 1);
      if(mminlength > 0) {
        int fparents = 0;
        for(int i = 0; i < item; i++) {
          if(buf3[i] >= minsupp) {
            fparents += 1;
          }
        }
        if(fparents < mminlength) {
          return; // Not enough parents that are still frequent.
        }
      }
      // Build projected tree:
      final int last = item - 1;
      FPTree proj = new FPTree(item);
      for(FPNode cur = header[item]; cur != null; cur = cur.sibling) {
        int j = buf2.length;
        for(FPNode parent = cur.parent; parent.key >= 0; parent = parent.parent) {
          if(buf3[parent.key] >= minsupp) {
            buf2[--j] = parent.key;
          }
        }
        if(buf2.length - j >= mminlength) {
          proj.insert(proj, buf2, j, buf2.length, cur.count);
        }
      }
      // Release memory:
      proj.reduceMemory();
      // TODO: other pruning techniques we should have employed here?
      postfix[plen++] = item;
      if(plen >= minlength && plen <= maxlength) {
        col.collect(support, postfix, 0, plen);
      }
      for(int j = last; j >= 0; j--) {
        proj.extract(minsupp, minlength, maxlength, j, postfix, plen, buf2, buf3, destruct, col);
      }
      if(destruct) {
        header[item] = null;
      }
    }

    /**
     * Extract itemsets from a linear tree.
     *
     * @param supp Current support
     * @param minlength Minimum length
     * @param maxlength Maximum length
     * @param item Current item
     * @param postfix Postfix for extracted itemsets
     * @param plen Postfix length
     * @param buf2 Scratch buffer
     * @param col Output collector
     */
    private void extractLinear(int supp, int minlength, int maxlength, int item, int[] postfix, int plen, int[] buf2, Collector col) {
      // For testing minimum length:
      final int mminlength = minlength - plen;
      // Unsatisfiable even with current item:
      if(item + 1 < mminlength) {
        return;
      }
      // Add current item:
      postfix[plen++] = item;
      if(plen >= minlength && plen <= maxlength) {
        col.collect(supp, postfix, 0, plen);
      }
      // Any more parents will exceed the maximum length:
      if(plen == maxlength) {
        return;
      }
      assert (header[item] != null);
      // Look at parent nodes:
      for(FPNode p = header[item].parent; p.key >= 0; p = p.parent) {
        if(p.key < mminlength) {
          break; // Too short.
        }
        extractLinear(supp, minlength, maxlength, p.key, postfix, plen, buf2, col);
      }
    }

    /**
     * Interface for collecting frequent itemsets found.
     *
     * @author Erich Schubert
     */
    interface Collector {
      /**
       * Collect a single frequent itemset
       *
       * @param support Support of the itemset
       * @param buf Buffer
       * @param start First valid buffer position
       * @param end End of valid buffer
       */
      void collect(int support, int[] buf, int start, int end);
    }

    /**
     * Output some statistics to logging.
     */
    public void logStatistics() {
      LOG.statistics(new LongStatistic(STAT + "items", header.length));
      LOG.statistics(new LongStatistic(STAT + "nodes", nodes));
      LOG.statistics(new LongStatistic(STAT + "transactions", count));
    }
  }

  /**
   * A single node of the FP tree.
   *
   * @author Erich Schubert
   * 
   * @assoc - - - Translator
   */
  // FIXME: keep children sorted, and use binary search for faster construction?
  // Or even use a hashset?
  public static class FPNode {
    /**
     * Parent node and next in sequence.
     */
    FPNode parent, sibling;

    /**
     * Key, weight, and number of children.
     */
    int key, count = 0, numchildren = 0;

    /**
     * Children.
     */
    FPNode[] children = EMPTY_CHILDREN;

    /**
     * Constant for leaf nodes.
     */
    final static FPNode[] EMPTY_CHILDREN = new FPNode[0];

    /**
     * Initial size, after sizes 0 and 1.
     */
    final int INITIAL_SIZE = 7;

    /**
     * Constructor.
     *
     * @param parent Parent node
     * @param key Key
     */
    public FPNode(FPNode parent, int key) {
      super();
      this.parent = parent;
      this.key = key;
    }

    /**
     * Insert an itemset into the tree.
     *
     * @param buf Itemset buffer
     * @param i Current index
     * @param l Length
     * @param weight Weight
     */
    public void insert(FPTree tree, int[] buf, int i, int l, int weight) {
      count += weight;
      if(i == l) {
        return;
      }
      final int label = buf[i];
      for(int j = 0; j < numchildren; j++) {
        FPNode child = children[j];
        if(child.key == label) {
          child.insert(tree, buf, i + 1, l, weight);
          return;
        }
      }
      // Make sure we have enough room to insert.
      if(numchildren == children.length) {
        ensureSize();
      }
      FPNode sub = children[numchildren++] = tree.newNode(this, label);
      sub.insert(tree, buf, i + 1, l, weight);
    }

    /**
     * Ensure we have enough storage.
     */
    private void ensureSize() {
      if(children == EMPTY_CHILDREN) {
        children = new FPNode[1];
        return;
      }
      int newsize = children.length == 1 ? INITIAL_SIZE : (children.length << 1);
      children = Arrays.copyOf(children, newsize);
    }

    /**
     * Debugging function: build a text representation of the tree.
     *
     * @param buf Output buffer
     * @param t Translator to user-understandable items
     * @return Buffer
     */
    public StringBuilder appendTo(StringBuilder buf, Translator t) {
      return appendTo(buf, t, 0);
    }

    /**
     * Buffer for indentation.
     */
    private static final char[] SPACES = "                ".toCharArray();

    /**
     * Debugging function: build a text representation of the tree.
     *
     * @param buf Output buffer
     * @param t Translator to user-understandable items
     * @param depth Current depth
     * @return Buffer
     */
    private StringBuilder appendTo(StringBuilder buf, Translator t, int depth) {
      if(key >= 0) {
        t.appendTo(buf, key).append(": ");
      }
      buf.append(count).append('\n');
      for(int i = 0; i < numchildren; i++) {
        for(int j = depth; j > 0; j -= SPACES.length) {
          buf.append(SPACES, 0, Math.min(j, SPACES.length));
        }
        children[i].appendTo(buf, t, depth + 1);
      }
      return buf;
    }

    /**
     * Release the memory occupied for the parent-to-child navigation, which is
     * no longer needed after building the tree (only child-to-parent is needed
     * in extracting itemsets).
     */
    public void reduceMemory() {
      if(children == null) {
        return;
      }
      for(int i = 0; i < numchildren; ++i) {
        children[i].reduceMemory();
      }
      children = null;
    }

    /**
     * Translator class for tree printing.
     *
     * @author Erich Schubert
     */
    interface Translator {
      /**
       * Append a single item to a buffer.
       *
       * @param buf Buffer to append to
       * @param i Item number
       * @return Buffer
       */
      StringBuilder appendTo(StringBuilder buf, int i);
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.BIT_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractFrequentItemsetAlgorithm.Parameterizer {
    @Override
    protected FPGrowth makeInstance() {
      return new FPGrowth(minsupp, minlength, maxlength);
    }
  }
}
