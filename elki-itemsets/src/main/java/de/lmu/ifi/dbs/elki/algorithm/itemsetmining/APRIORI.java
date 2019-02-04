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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.SparseFeatureVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.result.FrequentItemsetsResult;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.InconsistentDataException;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.jafama.FastMath;

/**
 * The APRIORI algorithm for Mining Association Rules.
 * <p>
 * Reference:
 * <p>
 * R. Agrawal, R. Srikant<br>
 * Fast Algorithms for Mining Association Rules<br>
 * In Proc. 20th Int. Conf. on Very Large Data Bases (VLDB '94)
 * <p>
 * This implementation uses some simple optimizations for 1- and 2-itemsets, but
 * does not implement the original hash tree (yet, please contribute).
 * <p>
 * Note: this algorithm scales well to a large number of transactions, but not
 * so well to a large number of frequent itemsets (items). For best results, use
 * domain-specific preprocessing to aggregate items into groups. Use statistics
 * logging to keep track of candidate set sizes.
 *
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.1
 *
 * @has - - - Itemset
 * @assoc - - - BitVector
 * @has - produces - FrequentItemsetsResult
 */
@Title("APRIORI: Algorithm for Mining Association Rules")
@Description("Searches for frequent itemsets")
@Reference(authors = "R. Agrawal, R. Srikant", //
    title = "Fast Algorithms for Mining Association Rules", //
    booktitle = "Proc. 20th Int. Conf. on Very Large Data Bases (VLDB '94)", //
    url = "http://www.vldb.org/conf/1994/P487.PDF", //
    bibkey = "DBLP:conf/vldb/AgrawalS94")
@Alias("de.lmu.ifi.dbs.elki.algorithm.APRIORI")
public class APRIORI extends AbstractFrequentItemsetAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(APRIORI.class);

  /**
   * Statistics logging prefix.
   */
  private final String STAT = this.getClass().getName() + ".";

  /**
   * Constructor with minimum frequency.
   *
   * @param minfreq Minimum frequency
   * @param minlength Minimum length
   * @param maxlength Maximum length
   */
  public APRIORI(double minfreq, int minlength, int maxlength) {
    super(minfreq, minlength, maxlength);
  }

  /**
   * Constructor with minimum frequency.
   *
   * @param minfreq Minimum frequency
   */
  public APRIORI(double minfreq) {
    super(minfreq);
  }

  /**
   * Performs the APRIORI algorithm on the given database.
   *
   * @param relation the Relation to process
   * @return the AprioriResult learned by this APRIORI
   */
  public FrequentItemsetsResult run(Relation<BitVector> relation) {
    DBIDs ids = relation.getDBIDs();
    List<Itemset> solution = new ArrayList<>();
    final int size = ids.size();
    final int needed = getMinimumSupport(size);

    // TODO: we don't strictly require a vector field.
    // We could work with knowing just the maximum dimensionality beforehand.
    VectorFieldTypeInformation<BitVector> meta = RelationUtil.assumeVectorField(relation);
    if(size > 0) {
      final int dim = meta.getDimensionality();
      Duration timeone = LOG.newDuration(STAT + "1-items.time").begin();
      List<OneItemset> oneitems = buildFrequentOneItemsets(relation, dim, needed);
      LOG.statistics(timeone.end());
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(STAT + "1-items.frequent", oneitems.size()));
        LOG.statistics(new LongStatistic(STAT + "1-items.transactions", ids.size()));
      }
      if(LOG.isDebuggingFine()) {
        LOG.debugFine(debugDumpCandidates(new StringBuilder(), oneitems, meta));
      }
      if(minlength <= 1) {
        solution.addAll(oneitems);
      }
      if(oneitems.size() >= 2 && maxlength >= 2) {
        Duration timetwo = LOG.newDuration(STAT + "2-items.time").begin();
        ArrayModifiableDBIDs survivors = DBIDUtil.newArray(ids.size());
        List<? extends Itemset> candidates = buildFrequentTwoItemsets(oneitems, relation, dim, needed, ids, survivors);
        ids = survivors; // Continue with reduced set of transactions.
        LOG.statistics(timetwo.end());
        if(LOG.isStatistics()) {
          LOG.statistics(new LongStatistic(STAT + "2-items.frequent", candidates.size()));
          LOG.statistics(new LongStatistic(STAT + "2-items.transactions", ids.size()));
        }
        if(LOG.isDebuggingFine()) {
          LOG.debugFine(debugDumpCandidates(new StringBuilder(), candidates, meta));
        }
        if(minlength <= 2) {
          solution.addAll(candidates);
        }
        for(int length = 3; length <= maxlength && candidates.size() >= length; length++) {
          Duration timel = LOG.newDuration(STAT + length + "-items.time").begin();
          // Join to get the new candidates
          candidates = aprioriGenerate(candidates, length, dim);
          if(LOG.isDebuggingFinest()) {
            LOG.debugFinest(debugDumpCandidates(new StringBuilder().append("Before pruning: "), candidates, meta));
          }
          survivors = DBIDUtil.newArray(ids.size());
          candidates = frequentItemsets(candidates, relation, needed, ids, survivors, length);
          ids = survivors; // Continue with reduced set of transactions.
          LOG.statistics(timel.end());
          if(LOG.isStatistics()) {
            LOG.statistics(new LongStatistic(STAT + length + "-items.frequent", candidates.size()));
            LOG.statistics(new LongStatistic(STAT + length + "-items.transactions", ids.size()));
          }
          if(LOG.isDebuggingFine()) {
            LOG.debugFine(debugDumpCandidates(new StringBuilder(), candidates, meta));
          }
          solution.addAll(candidates);
        }
      }
    }
    return new FrequentItemsetsResult("APRIORI", "apriori", solution, meta, size);
  }

  /**
   * Build the 1-itemsets.
   *
   * @param relation Data relation
   * @param dim Maximum dimensionality
   * @param needed Minimum support needed
   * @return 1-itemsets
   */
  protected List<OneItemset> buildFrequentOneItemsets(final Relation<? extends SparseFeatureVector<?>> relation, final int dim, final int needed) {
    // TODO: use TIntList and prefill appropriately to avoid knowing "dim"
    // beforehand?
    int[] counts = new int[dim];
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      SparseFeatureVector<?> bv = relation.get(iditer);
      for(int it = bv.iter(); bv.iterValid(it); it = bv.iterAdvance(it)) {
        counts[bv.iterDim(it)]++;
      }
    }
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(STAT + "1-items.candidates", dim));
    }
    // Generate initial candidates of length 1.
    List<OneItemset> frequent = new ArrayList<>(dim);
    for(int i = 0; i < dim; i++) {
      if(counts[i] >= needed) {
        frequent.add(new OneItemset(i, counts[i]));
      }
    }
    return frequent;
  }

  /**
   * Build the 2-itemsets.
   *
   * @param oneitems Frequent 1-itemsets
   * @param relation Data relation
   * @param dim Maximum dimensionality
   * @param needed Minimum support needed
   * @param ids Objects to process
   * @param survivors Output: objects that had at least two 1-frequent items.
   * @return Frequent 2-itemsets
   */
  protected List<SparseItemset> buildFrequentTwoItemsets(List<OneItemset> oneitems, final Relation<BitVector> relation, final int dim, final int needed, DBIDs ids, ArrayModifiableDBIDs survivors) {
    int f1 = 0;
    long[] mask = BitsUtil.zero(dim);
    for(OneItemset supported : oneitems) {
      BitsUtil.setI(mask, supported.item);
      f1++;
    }
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(STAT + "2-items.candidates", f1 * (long) (f1 - 1)));
    }
    // We quite aggressively size the map, assuming that almost each combination
    // is present somewhere. If this won't fit into memory, we're likely running
    // OOM somewhere later anyway!
    Long2IntOpenHashMap map = new Long2IntOpenHashMap((f1 * (f1 - 1)) >>> 1);
    final long[] scratch = BitsUtil.zero(dim);
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      BitsUtil.setI(scratch, mask);
      relation.get(iditer).andOnto(scratch);
      int lives = 0;
      for(int i = BitsUtil.nextSetBit(scratch, 0); i >= 0; i = BitsUtil.nextSetBit(scratch, i + 1)) {
        for(int j = BitsUtil.nextSetBit(scratch, i + 1); j >= 0; j = BitsUtil.nextSetBit(scratch, j + 1)) {
          long key = (((long) i) << 32) | j;
          map.put(key, 1 + map.get(key));
          ++lives;
        }
      }
      if(lives > 2) {
        survivors.add(iditer);
      }
    }
    // Generate candidates of length 2.
    List<SparseItemset> frequent = new ArrayList<>(f1 * (int) FastMath.sqrt(f1));
    for(ObjectIterator<Long2IntMap.Entry> iter = map.long2IntEntrySet().fastIterator(); iter.hasNext();) {
      Long2IntMap.Entry entry = iter.next();
      if(entry.getIntValue() >= needed) {
        int ii = (int) (entry.getLongKey() >>> 32);
        int ij = (int) (entry.getLongKey() & -1L);
        frequent.add(new SparseItemset(new int[] { ii, ij }, entry.getIntValue()));
      }
    }
    // The hashmap may produce them out of order.
    Collections.sort(frequent);
    if(LOG.isStatistics()) {
      LOG.statistics(new LongStatistic(STAT + "2-items.frequent", frequent.size()));
    }
    return frequent;
  }

  /**
   * Prunes a given set of candidates to keep only those BitSets where all
   * subsets of bits flipping one bit are frequent already.
   *
   * @param supported Support map
   * @param length Itemset length
   * @param dim Dimensionality
   * @return itemsets that cannot be pruned by apriori
   */
  protected List<Itemset> aprioriGenerate(List<? extends Itemset> supported, int length, int dim) {
    if(supported.size() < length) {
      return Collections.emptyList();
    }
    long joined = 0L;
    final int ssize = supported.size();
    List<Itemset> candidateList = new ArrayList<>();

    Itemset ref = supported.get(0);
    if(ref instanceof SparseItemset) {
      // TODO: we currently never switch to DenseItemSet. This may however be
      // beneficial when we have few dimensions and many candidates.
      // E.g. when length > 32 and dim < 100. But this needs benchmarking!
      // For length < 5 and dim > 3000, SparseItemset unsurprisingly was faster

      // Scratch item to use for searching.
      SparseItemset scratch = new SparseItemset(new int[length - 1]);

      for(int i = 0; i < ssize; i++) {
        SparseItemset ii = (SparseItemset) supported.get(i);
        prefix: for(int j = i + 1; j < ssize; j++) {
          SparseItemset ij = (SparseItemset) supported.get(j);
          if(!ii.prefixTest(ij)) {
            break prefix; // Prefix doesn't match
          }
          joined++;
          // Test subsets (re-) using scratch object
          System.arraycopy(ii.indices, 1, scratch.indices, 0, length - 2);
          scratch.indices[length - 2] = ij.indices[length - 2];
          for(int k = length - 3; k >= 0; k--) {
            scratch.indices[k] = ii.indices[k + 1];
            int pos = Collections.binarySearch(supported, scratch);
            if(pos < 0) {
              // Prefix was okay, but one other subset was not frequent
              continue prefix;
            }
          }
          int[] items = new int[length];
          System.arraycopy(ii.indices, 0, items, 0, length - 1);
          items[length - 1] = ij.indices[length - 2];
          candidateList.add(new SparseItemset(items));
        }
      }
    }
    else if(ref instanceof DenseItemset) {
      // Scratch item to use for searching.
      DenseItemset scratch = new DenseItemset(BitsUtil.zero(dim), length - 1);

      for(int i = 0; i < ssize; i++) {
        DenseItemset ii = (DenseItemset) supported.get(i);
        prefix: for(int j = i + 1; j < ssize; j++) {
          DenseItemset ij = (DenseItemset) supported.get(j);
          // Prefix test via "|i1 ^ i2| = 2"
          System.arraycopy(ii.items, 0, scratch.items, 0, ii.items.length);
          BitsUtil.xorI(scratch.items, ij.items);
          if(BitsUtil.cardinality(scratch.items) != 2) {
            break prefix; // No prefix match; since sorted, no more can follow!
          }
          ++joined;
          // Ensure that the first difference is the last item in ii:
          int first = BitsUtil.nextSetBit(scratch.items, 0);
          if(BitsUtil.nextSetBit(ii.items, first + 1) > -1) {
            break prefix; // Different overlap by chance?
          }
          BitsUtil.orI(scratch.items, ij.items);

          // Test subsets.
          for(int l = length, b = BitsUtil.nextSetBit(scratch.items, 0); l > 2; l--, b = BitsUtil.nextSetBit(scratch.items, b + 1)) {
            BitsUtil.clearI(scratch.items, b);
            int pos = Collections.binarySearch(supported, scratch);
            if(pos < 0) {
              continue prefix;
            }
            BitsUtil.setI(scratch.items, b);
          }
          candidateList.add(new DenseItemset(scratch.items.clone(), length));
        }
      }
    }
    else {
      throw new InconsistentDataException("Unexpected itemset type " + ref.getClass());
    }
    if(LOG.isStatistics()) {
      // Naive pairwise approach
      LOG.statistics(new LongStatistic(STAT + length + "-items.pairwise", (ssize * ((long) ssize - 1))));
      LOG.statistics(new LongStatistic(STAT + length + "-items.joined", joined));
      LOG.statistics(new LongStatistic(STAT + length + "-items.candidates", candidateList.size()));
    }
    // Note: candidates should have been generated in strictly ascending order
    // So we do not need to sort here.
    return candidateList;
  }

  /**
   * Returns the frequent BitSets out of the given BitSets with respect to the
   * given database.
   *
   * @param candidates the candidates to be evaluated
   * @param relation the database to evaluate the candidates on
   * @param needed Minimum support needed
   * @param ids Objects to process
   * @param survivors Output: objects that had at least two 1-frequent items.
   * @param length Itemset length
   * @return Itemsets with sufficient support
   */
  protected List<? extends Itemset> frequentItemsets(List<? extends Itemset> candidates, Relation<BitVector> relation, int needed, DBIDs ids, ArrayModifiableDBIDs survivors, int length) {
    if(candidates.isEmpty()) {
      return Collections.emptyList();
    }
    Itemset first = candidates.get(0);
    // We have an optimized codepath for large and sparse itemsets.
    // It probably pays off when #cands >> (avlen choose length) but we do not
    // currently have the average number of items. These thresholds yield
    // 2700, 6400, 12500, ... and thus will almost always be met until the
    // number of frequent itemsets is about to break down to 0.
    if(candidates.size() > length * length * length * 100 && first instanceof SparseItemset) {
      // Assume that all itemsets are sparse itemsets!
      @SuppressWarnings("unchecked")
      List<SparseItemset> sparsecand = (List<SparseItemset>) candidates;
      return frequentItemsetsSparse(sparsecand, relation, needed, ids, survivors, length);
    }
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      BitVector bv = relation.get(iditer);
      // TODO: exploit that the candidate set it sorted?
      int lives = 0;
      for(Itemset candidate : candidates) {
        if(candidate.containedIn(bv)) {
          candidate.increaseSupport();
          ++lives;
        }
      }
      if(lives > length) {
        survivors.add(iditer);
      }
    }
    // Retain only those with minimum support:
    List<Itemset> frequent = new ArrayList<>(candidates.size());
    for(Iterator<? extends Itemset> iter = candidates.iterator(); iter.hasNext();) {
      final Itemset candidate = iter.next();
      if(candidate.getSupport() >= needed) {
        frequent.add(candidate);
      }
    }
    return frequent;
  }

  /**
   * Returns the frequent BitSets out of the given BitSets with respect to the
   * given database. Optimized implementation for SparseItemset.
   *
   * @param candidates the candidates to be evaluated
   * @param relation the database to evaluate the candidates on
   * @param needed Minimum support needed
   * @param ids Objects to process
   * @param survivors Output: objects that had at least two 1-frequent items.
   * @param length Itemset length
   * @return Itemsets with sufficient support
   */
  protected List<SparseItemset> frequentItemsetsSparse(List<SparseItemset> candidates, Relation<BitVector> relation, int needed, DBIDs ids, ArrayModifiableDBIDs survivors, int length) {
    // Current search interval:
    int begin = 0, end = candidates.size();
    int[] scratchi = new int[length], iters = new int[length];
    SparseItemset scratch = new SparseItemset(scratchi);
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      BitVector bv = relation.get(iditer);
      if(!initializeSearchItemset(bv, scratchi, iters)) {
        continue;
      }
      int lives = 0;
      while(begin < end) {
        begin = binarySearch(candidates, scratch, begin, end);
        if(begin > 0) {
          candidates.get(begin).increaseSupport();
          ++lives;
        }
        else {
          begin = (-begin) - 1;
        }
        if(begin >= end || !nextSearchItemset(bv, scratchi, iters)) {
          break;
        }
      }
      for(Itemset candidate : candidates) {
        if(candidate.containedIn(bv)) {
          candidate.increaseSupport();
          ++lives;
        }
      }
      if(lives > length) {
        survivors.add(iditer);
      }
    }
    // Retain only those with minimum support:
    List<SparseItemset> frequent = new ArrayList<>(candidates.size());
    for(Iterator<SparseItemset> iter = candidates.iterator(); iter.hasNext();) {
      final SparseItemset candidate = iter.next();
      if(candidate.getSupport() >= needed) {
        frequent.add(candidate);
      }
    }
    return frequent;
  }

  /**
   * Initialize the scratch itemset.
   *
   * @param bv Bit vector data source
   * @param scratchi Scratch itemset
   * @param iters Iterator array
   * @return {@code true} if the itemset had minimum length
   */
  private boolean initializeSearchItemset(BitVector bv, int[] scratchi, int[] iters) {
    for(int i = 0; i < scratchi.length; i++) {
      iters[i] = (i == 0) ? bv.iter() : bv.iterAdvance(iters[i - 1]);
      if(iters[i] < 0) {
        return false;
      }
      scratchi[i] = bv.iterDim(iters[i]);
    }
    return true;
  }

  /**
   * Advance scratch itemset to the next.
   *
   * @param bv Bit vector data source
   * @param scratchi Scratch itemset
   * @param iters Iterator array
   * @return {@code true} if the itemset had minimum length
   */
  private boolean nextSearchItemset(BitVector bv, int[] scratchi, int[] iters) {
    final int last = scratchi.length - 1;
    for(int j = last; j >= 0; j--) {
      int n = bv.iterAdvance(iters[j]);
      if(n >= 0 && (j == last || n != iters[j + 1])) {
        iters[j] = n;
        scratchi[j] = bv.iterDim(n);
        return true; // Success
      }
    }
    return false;
  }

  /**
   * Binary-search for the next-larger element.
   *
   * @param candidates Candidates to search for
   * @param scratch Scratch space
   * @param begin Search interval begin
   * @param end Search interval end
   * @return Position of first equal-or-larger element
   */
  private int binarySearch(List<SparseItemset> candidates, SparseItemset scratch, int begin, int end) {
    --end;
    while(begin < end) {
      final int mid = (begin + end) >>> 1;
      SparseItemset midVal = candidates.get(mid);
      int cmp = midVal.compareTo(scratch);

      if(cmp < 0) {
        begin = mid + 1;
      }
      else if(cmp > 0) {
        end = mid - 1;
      }
      else {
        return mid; // key found
      }
    }
    return -(begin + 1); // key not found, return next
  }

  /**
   * Debug method: output all itemsets.
   *
   * @param msg Output buffer
   * @param candidates Itemsets to dump
   * @param meta Metadata for item labels
   * @return Output buffer
   */
  private StringBuilder debugDumpCandidates(StringBuilder msg, List<? extends Itemset> candidates, VectorFieldTypeInformation<BitVector> meta) {
    msg.append(':');
    for(Itemset itemset : candidates) {
      msg.append(" [");
      itemset.appendTo(msg, meta);
      msg.append(']');
    }
    return msg;
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
    protected APRIORI makeInstance() {
      return new APRIORI(minsupp, minlength, maxlength);
    }
  }
}
