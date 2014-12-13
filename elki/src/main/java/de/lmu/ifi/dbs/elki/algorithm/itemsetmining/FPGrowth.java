package de.lmu.ifi.dbs.elki.algorithm.itemsetmining;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.FPGrowth.FPNode.Translator;
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
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.result.AprioriResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerComparator;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * FP-Growth is an algorithm for mining the frequent itemsets by using a
 * compressed representation of the database called {@link #FPTree}.
 * 
 * FP-Growth first sorts items by the overall frequency, since having high
 * frequent items appear first in the tree leads to a much smaller tree since
 * frequent subsets will likely share the same path in the tree. FP-Growth is
 * beneficial when you have a lot of (near-) duplicate transactions, and are
 * using a not too high support threshold, as it only prunes single items, not
 * item combinations.
 * 
 * This implementation is in-memory only, and has not yet been carefully
 * optimized.
 * 
 * The worst case memory use probably is O(min(n*l,i^l)) where i is the number
 * of items, l the average itemset length, and n the number of items. The worst
 * case scenario is when every item is frequent, and every transaction is
 * unique. The resulting tree will then be larger than the original data.
 * 
 * Reference:
 * <p>
 * J. Han, J. Pei, Y. Yin<br />
 * Mining frequent patterns without candidate generation<br />
 * In proceedings of the 2000 ACM SIGMOD international conference on Management
 * of data.
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "J. Han, J. Pei, Y. Yin", //
title = "Mining frequent patterns without candidate generation", //
booktitle = "Proceedings of the 2000 ACM SIGMOD international conference on Management of data ", //
url = "http://dx.doi.org/10.1145/342009.335372")
public class FPGrowth extends AbstractAlgorithm<AprioriResult> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(FPGrowth.class);

  /**
   * Prefix for statistics.
   */
  private static final String STAT = FPGrowth.class.getName() + ".";

  /**
   * Minimum support.
   */
  private double minsupp;

  /**
   * Constructor.
   *
   * @param minsupp Minimum support (relative or absolute)
   */
  public FPGrowth(double minsupp) {
    this.minsupp = minsupp;
  }

  public AprioriResult run(Database db, final Relation<BitVector> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    final VectorFieldTypeInformation<BitVector> meta = RelationUtil.assumeVectorField(relation);
    // Compute absolute minsupport
    final int minsupp = (int) Math.ceil(this.minsupp < 1 ? this.minsupp * relation.size() : this.minsupp);

    LOG.verbose("Finding item frequencies for ordering.");
    // TODO: implement with resizable array, to not need dim.
    final int[] counts = new int[dim];
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      SparseFeatureVector<?> bv = relation.get(iditer);
      for(int it = bv.iter(); bv.iterValid(it); it = bv.iterAdvance(it)) {
        counts[bv.iterDim(it)]++;
      }
    }
    // Forward and backward indexes
    int[] iidx = new int[dim];
    final int[] idx = buildIndex(counts, iidx, minsupp);
    final int items = idx.length;

    LOG.verbose("Building FP-Tree.");
    // FIXME: no header table yet!
    FPTree tree = new FPTree(items);
    {
      int[] buf = new int[dim];
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
        Arrays.sort(buf, 0, l); // Sort ascending
        tree.insert(buf, 0, l, 1);
      }
    }
    if(LOG.isStatistics()) {
      tree.logStatistics();
    }

    if(LOG.isDebuggingFinest()) {
      StringBuilder buf = new StringBuilder();
      tree.appendTo(buf, new FPNode.Translator() {
        @Override
        public void appendTo(StringBuilder buf, int i) {
          String l = meta.getLabel(idx[i]);
          buf.append(l != null ? l : Integer.toString(i));
        }
      });
      LOG.debugFinest(buf.toString());
    }

    LOG.verbose("Extracting frequent patterns.");
    final List<Itemset> solution = new ArrayList<>();
    // Start extraction with the least frequent items
    tree.extract(minsupp, new FPTree.Collector() {
      @Override
      public void collect(int support, int[] data, int start, int plen) {
        // Always translate the indexes back to the original values via 'idx'!
        if(plen - start == 1) {
          solution.add(new OneItemset(idx[data[start]], support));
        }
        else {
          int[] indices = new int[plen - start];
          for(int i = start, j = 0; i < plen; i++) {
            indices[j++] = idx[data[i]]; // Translate to original items
          }
          Arrays.sort(indices);
          solution.add(new SparseItemset(indices, support));
        }
      }
    });
    Collections.sort(solution);

    return new AprioriResult("FP-Growth", "fp-growth", solution, meta);
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
    IntegerArrayQuickSort.sort(idx, new IntegerComparator() {
      @Override
      public int compare(int x, int y) {
        return Integer.compare(counts[y], counts[x]);
      }
    });
    Arrays.fill(positions, -1);
    for(int i = 0; i < idx.length; i++) {
      positions[idx[i]] = i;
    }
    return idx;
  }

  public static class FPTree {
    FPNode root = new FPNode(null, -1);

    FPNode[] header;

    int nodes = 1;

    public FPTree(int items) {
      super();
      header = new FPNode[items];
    }

    public void insert(int[] buf, int i, int l, int weight) {
      root.insert(this, buf, i, l, weight);
    }

    public void appendTo(StringBuilder buf, Translator translator) {
      root.appendTo(buf, translator);
    }

    public FPNode newNode(FPNode parent, int label) {
      FPNode node = new FPNode(parent, label);
      // Update header table:
      FPNode prev = header[label];
      if(prev == null) {
        header[label] = node;
      }
      else {
        while(prev.sibling != null) {
          prev = prev.sibling;
        }
        prev.sibling = node;
      }
      ++nodes;
      return node;
    }

    /**
     * Extract itemsets ending in the given item.
     * 
     * @param minsupp Minimum support
     * @param col Itemset collector
     */
    public void extract(int minsupp, Collector col) {
      int[] buf = new int[header.length], buf2 = new int[header.length];
      for(int j = header.length - 1; j >= 0; --j) {
        extract(minsupp, j, buf, 0, buf2, col);
      }
    }

    /**
     * Extract itemsets ending in the given item.
     * 
     * @param minsupp Minimum support
     * @param item Current item
     * @param postfix Items to append
     * @param plen Postfix length
     * @param buf2 Scratch buffer
     * @param col Itemset collector
     */
    private void extract(int minsupp, int item, int[] postfix, int plen, int[] buf2, Collector col) {
      final int last = item - 1;
      FPTree proj = new FPTree(item);
      int support = 0;
      for(FPNode cur = header[item]; cur != null; cur = cur.sibling) {
        int j = buf2.length;
        support += cur.count;
        for(FPNode parent = cur.parent; parent.key >= 0; parent = parent.parent) {
          buf2[--j] = parent.key;
        }
        proj.insert(buf2, j, buf2.length, cur.count);
      }
      if(support < minsupp) {
        return;
      }
      // TODO: other pruning techniques we should have employed here?
      postfix[plen++] = item;
      col.collect(support, postfix, 0, plen);
      for(int j = last; j >= 0; j--) {
        // TODO: use a total count in the header table to skip now-non-frequent
        // items at this point?
        proj.extract(minsupp, j, postfix, plen, buf2, col);
      }
    }

    public static interface Collector {
      public void collect(int support, int[] buf, int start, int plen);
    }

    public void logStatistics() {
      LOG.statistics(new LongStatistic(STAT + "items", header.length));
      LOG.statistics(new LongStatistic(STAT + "nodes", nodes));
      LOG.statistics(new LongStatistic(STAT + "transactions", root.count));
    }
  }

  // FIXME: keep keys sorted? Even use a hashmap?
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
     */
    public void appendTo(StringBuilder buf, Translator t) {
      appendTo(buf, t, 0);
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
     */
    private void appendTo(StringBuilder buf, Translator t, int depth) {
      if(key > 0) {
        t.appendTo(buf, key);
        buf.append(": ");
      }
      buf.append(count).append("\n");
      for(int i = 0; i < numchildren; i++) {
        for(int j = depth; j > 0; j -= SPACES.length) {
          buf.append(SPACES, 0, Math.min(j, SPACES.length));
        }
        children[i].appendTo(buf, t, depth + 1);
      }
    }

    /**
     * Translator class for tree printing.
     * 
     * @author Erich Schubert
     */
    public static interface Translator {
      /**
       * Append a single item to a buffer.
       * 
       * @param buf Buffer to append to
       * @param i Item number
       */
      public void appendTo(StringBuilder buf, int i);
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the minimum support, in absolute or relative terms.
     */
    public static final OptionID MINSUPP_ID = new OptionID("fpgrowth.minsupp", //
    "Threshold for minimum support as minimally required number of transactions (if > 1) " //
        + "or the minimum frequency (if <= 1).");

    /**
     * Parameter for minimum support.
     */
    protected double minsupp;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minsuppP = new DoubleParameter(MINSUPP_ID);
      minsuppP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(minsuppP)) {
        minsupp = minsuppP.getValue();
      }
    }

    @Override
    protected FPGrowth makeInstance() {
      return new FPGrowth(minsupp);
    }
  }
}
