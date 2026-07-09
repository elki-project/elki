/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2026
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
package elki.sequencemining;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

import elki.data.IntegerVector;
import elki.data.type.TypeInformation;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.result.FrequentSubsequencesResult;
import elki.result.Metadata;
import elki.data.type.TypeUtil;
import elki.utilities.datastructures.arraylike.IntegerArray;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * GSP (Generalized Sequential Patterns) algorithm for mining frequent
 * sequential patterns.
 * <p>
 * The input data is a relation of {@link IntegerVector}s, where each vector
 * represents a single sequence.
 * Elements within a sequence are stored as integers. The special values -1 and
 * -2 should not normally
 * appear in real data (they serve delimiters in some file formats but the
 * parser strips them).
 * <p>
 * The algorithm follows the classic Apriori-style level-wise search:
 * <ul>
 * <li><b>F1 counting</b>: Count each distinct integer's support across all
 * sequences.</li>
 * <li><b>F2 generation</b>: Join frequent 1-sequences a &lt; b into candidate
 * [a,b].</li>
 * <li><b>Generalized join</b>: For Fk, two (k-1)-sequences s1 and s2 form a
 * candidate if s1[1:] == s2[:-1], producing s1 + [s2.last()].</li>
 * <li><b>No-subsequence pruning</b>: A candidate is kept only if all its proper
 * (k-1)-subsequences are frequent.</li>
 * <li><b>Support counting</b>: For each data sequence, check subsequence
 * containment.</li>
 * </ul>
 *
 * <p>
 * Reference:
 * <p>
 * R. Srikant, R. Agrawal<br>
 * Mining Sequential Patterns Generalized<br>
 * In Proc. 11th Int. Conf. on Data Engineering (ICDE '95)
 *
 * @author Erich Schubert
 *
 * @has - - - Sequence
 */
@Title("GSP: Generalized Sequential Patterns")
@Reference(authors = "R. Srikant, R. Agrawal", //
    title = "Mining Sequential Patterns Generalized", //
    booktitle = "Proc. 11th Int. Conf. on Data Engineering (ICDE '95)")
public class GSP extends AbstractFrequentSubsequenceAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(GSP.class);

  /**
   * Statistics logging prefix.
   */
  private final String STAT = this.getClass().getName() + ".";

  /**
   * Constructor with raw parameters (for ELKIBuilder/Parameterizer).
   * minSuppRaw is relative if &lt;= 1, absolute if &gt; 1.
   * Resolved at runtime in run().
   */
  public GSP(double minSuppRaw, int minLength, int maxLength) {
    super(minSuppRaw, minLength, maxLength);
  }

  /**
   * Input type restriction for this algorithm.
   */
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.INTEGER_VECTOR);
  }

  /**
   * Run the GSP algorithm on the given relation of IntegerVector sequences.
   *
   * @param relation Relation of IntegerVector, where each vector is a sequence.
   * @return List of all frequent sequences found, sorted by length then
   *         lexicographically.
   */
  public FrequentSubsequencesResult run(Relation<IntegerVector> relation) {
    final List<Sequence> solution = new ArrayList<>();
    DBIDs ids = relation.getDBIDs();
    final int nTransactions = ids.size();
    final int minSupportThreshold = getMinimumSupport(nTransactions);

    if(nTransactions == 0 || maxLength < 1) {
      FrequentSubsequencesResult result = new FrequentSubsequencesResult(solution, nTransactions);
      Metadata.of(result).setLongName("GSP");
      return result;
    }

    final int minLengthEffective = getMinimumLength();
    // ---- F1: Count support for each distinct integer value ----
    Duration timeone = LOG.isStatistics() ? LOG.newDuration(STAT + "F1.time").begin() : null;
    int[] f1counts = countFrequentOnes(relation, ids);
    DenseItems denseItems = buildFrequentOnes(f1counts, minSupportThreshold);
    List<Sequence> frequent = denseItems.ones;
    if(LOG.isStatistics()) {
      LOG.statistics(timeone.end());
      LOG.statistics(new LongStatistic(STAT + "F1.candidates", f1counts.length));
      LOG.statistics(new LongStatistic(STAT + "F1.frequent", frequent.size()));
    }
    if(frequent.isEmpty()) {
      FrequentSubsequencesResult result = new FrequentSubsequencesResult(solution, nTransactions);
      Metadata.of(result).setLongName("GSP");
      return result;
    }

    if(maxLength == 1) {
      for(Sequence seq : frequent) {
        if(itemCount(seq.values) >= minLengthEffective) {
          solution.add(remapSequence(seq, denseItems.denseToOriginal));
        }
      }
      Collections.sort(solution);
      FrequentSubsequencesResult result = new FrequentSubsequencesResult(solution, nTransactions);
      Metadata.of(result).setLongName("GSP");
      return result;
    }

    if(minLengthEffective <= 1) {
      for(Sequence seq : frequent) {
        solution.add(remapSequence(seq, denseItems.denseToOriginal));
      }
    }

    Duration timetwo = LOG.isStatistics() ? LOG.newDuration(STAT + "F2.time").begin() : null;
    frequent = countFrequentTwos(relation, ids, denseItems, minSupportThreshold);
    if(LOG.isStatistics()) {
      LOG.statistics(timetwo.end());
      LOG.statistics(new LongStatistic(STAT + "F2.frequent", frequent.size()));
    }

    if(minLengthEffective <= 2) {
      appendRemapped(solution, frequent, denseItems.denseToOriginal);
    }

    for(int length = 3; length <= maxLength && !frequent.isEmpty(); length++) {
      Duration timel = LOG.isStatistics() ? LOG.newDuration(STAT + "F" + length + ".time").begin() : null;
      List<Sequence> candidates = generateCandidates(frequent);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(STAT + "F" + length + ".candidates", candidates.size()));
      }
      if(candidates.isEmpty()) {
        break;
      }
      frequent = countSupportInData(candidates, relation, ids, minSupportThreshold, denseItems.originalToDense);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(STAT + "F" + length + ".frequent", frequent.size()));
        LOG.statistics(timel.end());
      }
      if(length >= minLengthEffective) {
        appendRemapped(solution, frequent, denseItems.denseToOriginal);
      }
    }

    Collections.sort(solution);
    FrequentSubsequencesResult result = new FrequentSubsequencesResult(solution, nTransactions);
    Metadata.of(result).setLongName("GSP");
    return result;
  }

  private static void appendRemapped(List<Sequence> output, List<Sequence> denseSequences, int[] denseToOriginal) {
    for(Sequence seq : denseSequences) {
      output.add(remapSequence(seq, denseToOriginal));
    }
  }

  private static Sequence remapSequence(Sequence denseSequence, int[] denseToOriginal) {
    int[] values = Arrays.copyOf(denseSequence.values, denseSequence.values.length);
    for(int i = 0; i < values.length; i++) {
      if(values[i] != TIME_STEP) {
        values[i] = denseToOriginal[values[i]];
      }
    }
    Sequence remapped = new Sequence(values);
    remapped.support = denseSequence.support;
    return remapped;
  }

  private static int normalizeSequence(IntegerVector vec, Int2IntOpenHashMap originalToDense, IntegerArray items, IntegerArray itemsetEnds) {
    items.clear();
    itemsetEnds.clear();
    int itemsetStart = 0;

    for(int d = 0; d < vec.getDimensionality(); d++) {
      int value = vec.intValue(d);
      if(value == TIME_STEP) {
        if(itemsetStart < items.size) {
          finishItemset(items, itemsetStart);
          itemsetEnds.add(items.size);
          itemsetStart = items.size;
        }
        continue;
      }
      if(value < 0) {
        continue;
      }
      int dense = originalToDense.get(value);
      if(dense < 0) {
        continue;
      }
      items.add(dense);
    }
    if(itemsetStart < items.size) {
      finishItemset(items, itemsetStart);
      itemsetEnds.add(items.size);
    }
    return itemsetEnds.size;
  }

  private static void finishItemset(IntegerArray items, int itemsetStart) {
    Arrays.sort(items.data, itemsetStart, items.size);
    int write = itemsetStart;
    for(int read = itemsetStart; read < items.size; read++) {
      int value = items.data[read];
      if(write == itemsetStart || items.data[write - 1] != value) {
        items.data[write++] = value;
      }
    }
    items.size = write;
  }

  private static int itemCount(int[] values) {
    int count = 0;
    for(int value : values) {
      if(value != TIME_STEP) {
        count++;
      }
    }
    return count;
  }

  private int[] countFrequentOnes(Relation<IntegerVector> relation, DBIDs ids) {
    // Parse each sequence into time steps (separated by TIME_STEP/-1).
    // Count how many data sequences contain each value in at least one time
    // step.
    // Skip TIME_STEP markers and RECORD_SEP (not data items).
    int maxVal = -1;

    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      IntegerVector vec = relation.get(iditer);
      final int vdim = vec.getDimensionality();
      for(int d = 0; d < vdim; d++) {
        int v = vec.intValue(d);
        if(v == TIME_STEP)
          continue; // skip time step separators, not data items
        assert (v >= TIME_STEP);
        if(v > maxVal)
          maxVal = v;
      }
    }

    final int dim = (maxVal < 0) ? 0 : maxVal + 1;
    int[] counts = new int[dim];
    int[] seenStamp = new int[dim];
    int stamp = 1;

    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      if(stamp == Integer.MAX_VALUE) {
        Arrays.fill(seenStamp, 0);
        stamp = 1;
      }
      IntegerVector vec = relation.get(iditer);
      final int vdim = vec.getDimensionality();

      for(int d = 0; d < vdim; d++) {
        int v = vec.intValue(d);
        if(v == TIME_STEP)
          continue; // skip time step separator, not a data item
        if(v < 0 || v >= dim)
          continue;
        if(seenStamp[v] != stamp) {
          seenStamp[v] = stamp;
          counts[v]++;
        }
      }
      stamp++;
    }

    return counts;
  }

  /**
   * Build list of frequent 1-sequences from support counts and re-enumerate
   * them densely for faster later counting.
   *
   * @param f1counts Support counts array (index = value, content = support
   *        count).
   * @param minSupport Minimum support threshold.
   * @return Dense encoding of frequent 1-sequences.
   */
  private DenseItems buildFrequentOnes(int[] f1counts, int minSupport) {
    int frequentCount = 0;
    for(int count : f1counts) {
      if(count >= minSupport) {
        frequentCount++;
      }
    }

    Int2IntOpenHashMap originalToDense = new Int2IntOpenHashMap(frequentCount * 2 + 1);
    originalToDense.defaultReturnValue(-1);
    int[] denseToOriginal = new int[frequentCount];
    List<Sequence> result = new ArrayList<>(frequentCount);

    int dense = 0;
    for(int v = 0; v < f1counts.length; v++) {
      if(f1counts[v] >= minSupport) {
        originalToDense.put(v, dense);
        denseToOriginal[dense] = v;
        result.add(new Sequence(new int[] { dense }, f1counts[v]));
        dense++;
      }
    }
    return new DenseItems(result, originalToDense, denseToOriginal);
  }

  private static List<Sequence> countFrequentTwos(Relation<IntegerVector> relation, DBIDs ids, DenseItems denseItems, int minSupport) {
    final int dim = denseItems.denseToOriginal.length;
    if(dim == 0) {
      return Collections.emptyList();
    }

    final int sameSize = dim * (dim - 1) / 2;
    final int crossSize = dim * dim;
    final int[] sameCounts = new int[sameSize];
    final int[] sameSeen = new int[sameSize];
    final int[] crossCounts = new int[crossSize];
    final int[] crossSeen = new int[crossSize];
    final int[] prefixSeen = new int[dim];
    IntegerArray prefixItems = new IntegerArray(dim);
    IntegerArray items = new IntegerArray();
    IntegerArray itemsetEnds = new IntegerArray();
    int stamp = 1;

    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance(), stamp++) {
      if(normalizeSequence(relation.get(iditer), denseItems.originalToDense, items, itemsetEnds) == 0) {
        continue;
      }

      prefixItems.clear();
      int start = 0;
      for(int iset = 0; iset < itemsetEnds.size; iset++) {
        final int end = itemsetEnds.data[iset];
        for(int idx = start; idx < end; idx++) {
          final int b = items.data[idx];
          for(int p = 0; p < prefixItems.size; p++) {
            final int a = prefixItems.data[p];
            final int code = a * dim + b;
            if(crossSeen[code] != stamp) {
              crossSeen[code] = stamp;
              crossCounts[code]++;
            }
          }
        }

        for(int i = start; i < end; i++) {
          final int a = items.data[i];
          for(int j = i + 1; j < end; j++) {
            final int code = sameItemsetIndex(a, items.data[j], dim);
            if(sameSeen[code] != stamp) {
              sameSeen[code] = stamp;
              sameCounts[code]++;
            }
          }
        }

        for(int idx = start; idx < end; idx++) {
          final int item = items.data[idx];
          if(prefixSeen[item] != stamp) {
            prefixSeen[item] = stamp;
            prefixItems.add(item);
          }
        }
        start = end;
      }
    }

    List<Sequence> result = new ArrayList<>();
    for(int a = 0; a < dim; a++) {
      for(int b = a + 1; b < dim; b++) {
        int support = sameCounts[sameItemsetIndex(a, b, dim)];
        if(support >= minSupport) {
          result.add(new Sequence(new int[] { a, b }, support));
        }
      }
    }
    for(int a = 0; a < dim; a++) {
      for(int b = 0; b < dim; b++) {
        int support = crossCounts[a * dim + b];
        if(support >= minSupport) {
          result.add(new Sequence(new int[] { a, TIME_STEP, b }, support));
        }
      }
    }
    Collections.sort(result);
    return result;
  }

  private static int sameItemsetIndex(int a, int b, int dim) {
    return a * (2 * dim - a - 1) / 2 + (b - a - 1);
  }

  private static List<Sequence> generateCandidates(List<Sequence> frequent) {
    if(frequent.size() < 2) {
      return Collections.emptyList();
    }
    Set<Sequence> frequentSet = new HashSet<>(frequent);
    List<Sequence> candidates = new ArrayList<>();
    final int fsize = frequent.size();
    for(int i = 0; i < fsize; i++) {
      int[] s1 = frequent.get(i).values;
      for(int j = 0; j < fsize; j++) {
        int[] s2 = frequent.get(j).values;
        if(!canJoin(s1, s2)) {
          continue;
        }
        int[] candidate = joinSequences(s1, s2);
        if(candidate != null && hasAllSubsequences(candidate, frequentSet)) {
          Sequence seq = new Sequence(candidate);
          if(candidates.isEmpty() || candidates.get(candidates.size() - 1).compareTo(seq) != 0) {
            candidates.add(seq);
          }
        }
      }
    }
    return candidates;
  }

  private static List<Sequence> countSupportInData(List<Sequence> candidates, Relation<IntegerVector> relation, DBIDs ids, int minSupport, Int2IntOpenHashMap originalToDense) {
    if(candidates.isEmpty()) {
      return Collections.emptyList();
    }

    // Reset all supports.
    for(Sequence c : candidates) {
      c.support = 0;
    }

    IntegerArray items = new IntegerArray();
    IntegerArray itemsetEnds = new IntegerArray();
    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      if(normalizeSequence(relation.get(iditer), originalToDense, items, itemsetEnds) == 0) {
        continue;
      }

      final int csize = candidates.size();
      for(int cidx = 0; cidx < csize; cidx++) {
        Sequence candidate = candidates.get(cidx);
        if(candidate.support < minSupport && contains(items, itemsetEnds, candidate.values)) {
          candidate.support++;
        }
      }
    }

    // Retain only those with minimum support.
    List<Sequence> frequent = new ArrayList<>();
    for(Sequence c : candidates) {
      if(c.support >= minSupport) {
        frequent.add(c);
      }
    }
    return frequent;
  }

  private static boolean contains(IntegerArray items, IntegerArray itemsetEnds, int[] candidate) {
    int txStart = 0, txItemset = 0;
    for(int cstart = 0; cstart < candidate.length;) {
      int cend = cstart;
      while(cend < candidate.length && candidate[cend] != TIME_STEP) {
        cend++;
      }
      boolean found = false;
      while(txItemset < itemsetEnds.size) {
        int txEnd = itemsetEnds.data[txItemset];
        if(containsItemset(items.data, txStart, txEnd, candidate, cstart, cend)) {
          txStart = txEnd;
          txItemset++;
          found = true;
          break;
        }
        txStart = txEnd;
        txItemset++;
      }
      if(!found) {
        return false;
      }
      cstart = cend + 1;
    }
    return true;
  }

  private static boolean containsItemset(int[] items, int txStart, int txEnd, int[] candidate, int cstart, int cend) {
    int ti = txStart;
    int ci = cstart;
    while(ti < txEnd && ci < cend) {
      int tv = items[ti], cv = candidate[ci];
      if(tv < cv) {
        ti++;
      }
      else if(tv == cv) {
        ti++;
        ci++;
      }
      else {
        return false;
      }
    }
    return ci == cend;
  }

  /**
   * Check whether two (k-1)-sequences can be joined by comparing the first-item
   * drop of s1 with the last-item drop of s2.
   *
   * @param s1 First sequence (sorted order).
   * @param s2 Second sequence (must appear after s1 in sorted order).
   * @return {@code true} if the join condition holds.
   */
  private static boolean canJoin(int[] s1, int[] s2) {
    return Arrays.equals(removeItem(s1, 0), removeItem(s2, itemCount(s2) - 1));
  }

  private static int[] joinSequences(int[] s1, int[] s2) {
    int last = lastItem(s2);
    if(last < 0) {
      return null;
    }
    boolean sameItemset = isLastItemInSameItemset(s2);
    int[] candidate = Arrays.copyOf(s1, s1.length + (sameItemset ? 1 : 2));
    if(sameItemset) {
      if(candidate[candidate.length - 2] == TIME_STEP || candidate[candidate.length - 2] >= last) {
        return null;
      }
      candidate[candidate.length - 1] = last;
      return candidate;
    }
    candidate[candidate.length - 2] = TIME_STEP;
    candidate[candidate.length - 1] = last;
    return candidate;
  }

  /**
   * Verify that all proper (k-1)-subsequences of the candidate are already
   * frequent.
   *
   * @param candidate Candidate of length k.
   * @param frequent Frequent (k-1)-sequences.
   * @return {@code true} if all proper subsequences are in frequent.
   */
  private static boolean hasAllSubsequences(int[] candidate, Set<Sequence> frequent) {
    final int items = itemCount(candidate);
    for(int rm = 0; rm < items; rm++) {
      if(!frequent.contains(new Sequence(removeItem(candidate, rm)))) {
        return false;
      }
    }
    return true;
  }

  private static int[] removeItem(int[] values, int removeIdx) {
    IntegerArray out = new IntegerArray(values.length);
    int itemidx = 0;
    int pos = 0;
    while(pos < values.length) {
      int itemsetStart = out.size;
      while(pos < values.length && values[pos] != TIME_STEP) {
        if(itemidx != removeIdx) {
          out.add(values[pos]);
        }
        itemidx++;
        pos++;
      }
      if(out.size > itemsetStart) {
        out.add(TIME_STEP);
      }
      pos++;
    }
    if(out.size > 0 && out.data[out.size - 1] == TIME_STEP) {
      out.size--;
    }
    return Arrays.copyOf(out.data, out.size);
  }

  private static int lastItem(int[] values) {
    for(int i = values.length - 1; i >= 0; i--) {
      if(values[i] != TIME_STEP) {
        return values[i];
      }
    }
    return -1;
  }

  private static boolean isLastItemInSameItemset(int[] values) {
    int seen = 0;
    for(int i = values.length - 1; i >= 0; i--) {
      if(values[i] == TIME_STEP) {
        return seen > 1;
      }
      seen++;
    }
    return seen > 1;
  }

  private static class DenseItems {
    final List<Sequence> ones;

    final Int2IntOpenHashMap originalToDense;

    final int[] denseToOriginal;

    DenseItems(List<Sequence> ones, Int2IntOpenHashMap originalToDense, int[] denseToOriginal) {
      this.ones = ones;
      this.originalToDense = originalToDense;
      this.denseToOriginal = denseToOriginal;
    }
  }

  /**
   * Time step separator within a sequence.
   */
  public static final int TIME_STEP = -1;

  /**
   * Sequence representation using int[] for efficiency.
   * Implements Comparable to enable binary search on sorted candidate lists.
   * Ordering is lexicographic (standard for Apriori-style algorithms).
   */
  public static class Sequence implements Comparable<Sequence> {
    /**
     * Integer values of this sequence.
     */
    final int[] values;

    /**
     * Support count (set during support counting).
     */
    int support;

    /**
     * Construct from raw int array (does not copy).
     *
     * @param values Sequence elements.
     */
    public Sequence(int[] values) {
      this.values = values;
      this.support = 0;
    }

    /**
     * Construct from raw int array and support.
     *
     * @param values Sequence elements
     * @param support Sequence support
     */
    public Sequence(int[] values, int support) {
      this.values = values;
      this.support = support;
    }

    /**
     * Compare sequences lexicographically for sorting.
     * Shorter sequences come before longer ones only if they are a prefix.
     * Otherwise, compare element-by-element up to the shorter length.
     *
     * @param other Other sequence.
     * @return -1, 0, or +1.
     */
    @Override
    public int compareTo(Sequence other) {
      final int len1 = this.values.length;
      final int len2 = other.values.length;
      for(int i = 0; i < Math.min(len1, len2); i++) {
        if(this.values[i] != other.values[i]) {
          return Integer.compare(this.values[i], other.values[i]);
        }
      }
      return Integer.compare(len1, len2); // Shorter comes first.
    }

    /**
     * Get support count.
     *
     * @return Support count.
     */
    public int getSupport() {
      return support;
    }

    /**
     * Set support count.
     *
     * @param support New support count.
     */
    public void setSupport(int support) {
      this.support = support;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(values);
    }

    @Override
    public boolean equals(Object obj) {
      if(this == obj) {
        return true;
      }
      if(!(obj instanceof Sequence)) {
        return false;
      }
      return Arrays.equals(values, ((Sequence) obj).values);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("[");
      for(int i = 0; i < values.length; i++) {
        if(i > 0)
          sb.append(", ");
        sb.append(values[i]);
      }
      return sb.append("]").toString();
    }
  }

  /**
   * Parameterization class.
   */
    public static class Par extends AbstractFrequentSubsequenceAlgorithm.Par {

    /**
     * Create the GSP algorithm with current raw parameters.
     * The absolute support threshold is resolved at runtime in run().
     *
     * @return Configured GSP instance.
     */
    public GSP make() {
      return new GSP(minSupp, minLength, maxLength);
    }
  }
}
