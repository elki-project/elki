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
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.result.FrequentItemsetsResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Eclat is a depth-first discovery algorithm for mining frequent itemsets.
 * <p>
 * Eclat discovers frequent itemsets by first transforming the data into a
 * (sparse) column-oriented form, then performing a depth-first traversal of the
 * prefix lattice, stopping traversal when the minimum support is no longer
 * satisfied.
 * <p>
 * This implementation is the basic algorithm only, and does not use diffsets.
 * Columns are represented using a sparse representation, which theoretically is
 * beneficial when the density is less than 1/31. This corresponds roughly to a
 * minimum support of 3% for 1-itemsets. When searching for itemsets with a
 * larger minimum support, it may be desirable to use a dense bitset
 * representation instead and/or implement an automatic switching technique!
 * <p>
 * Performance of this implementation is probably surpassed with a low-level C
 * implementation based on SIMD bitset operations as long as support of an
 * itemset is high, which are not easily accessible in Java.
 * <p>
 * Reference:
 * <p>
 * New Algorithms for Fast Discovery of Association Rules<br>
 * M. J. Zaki, S. Parthasarathy, M. Ogihara, W. Li<br>
 * Proc. 3rd ACM SIGKDD '97 Int. Conf. on Knowledge Discovery and Data Mining
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - Itemset
 * @has - produces - FrequentItemsetsResult
 */
@Reference(authors = "M. J. Zaki, S. Parthasarathy, M. Ogihara, W. Li", //
    title = "New Algorithms for Fast Discovery of Association Rules", //
    booktitle = "Proc. 3rd ACM SIGKDD '97 Int. Conf. on Knowledge Discovery and Data Mining", //
    url = "http://www.aaai.org/Library/KDD/1997/kdd97-060.php", //
    bibkey = "DBLP:conf/kdd/ZakiPOL97")
public class Eclat extends AbstractFrequentItemsetAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(Eclat.class);

  /**
   * Prefix for statistics.
   */
  private static final String STAT = Eclat.class.getName() + ".";

  /**
   * Constructor.
   *
   * @param minsupp Minimum support
   * @param minlength Minimum length
   * @param maxlength Maximum length
   */
  public Eclat(double minsupp, int minlength, int maxlength) {
    super(minsupp, minlength, maxlength);
  }

  /**
   * Run the Eclat algorithm
   * 
   * @param db Database to process
   * @param relation Bit vector relation
   * @return Frequent patterns found
   */
  public FrequentItemsetsResult run(Database db, final Relation<BitVector> relation) {
    // TODO: implement with resizable arrays, to not need dim.
    final int dim = RelationUtil.dimensionality(relation);
    final VectorFieldTypeInformation<BitVector> meta = RelationUtil.assumeVectorField(relation);
    // Compute absolute minsupport
    final int minsupp = getMinimumSupport(relation.size());

    LOG.verbose("Build 1-dimensional transaction lists.");
    Duration ctime = LOG.newDuration(STAT + "eclat.transposition.time").begin();
    DBIDs[] idx = buildIndex(relation, dim, minsupp);
    LOG.statistics(ctime.end());

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Building frequent itemsets", idx.length, LOG) : null;
    Duration etime = LOG.newDuration(STAT + "eclat.extraction.time").begin();
    final List<Itemset> solution = new ArrayList<>();
    for(int i = 0; i < idx.length; i++) {
      LOG.incrementProcessed(prog);
      extractItemsets(idx, i, minsupp, solution);
    }
    LOG.ensureCompleted(prog);
    Collections.sort(solution);
    LOG.statistics(etime.end());

    LOG.statistics(new LongStatistic(STAT + "frequent-itemsets", solution.size()));
    return new FrequentItemsetsResult("Eclat", "eclat", solution, meta, relation.size());
  }

  // TODO: implement diffsets.
  private void extractItemsets(DBIDs[] idx, int start, int minsupp, List<Itemset> solution) {
    int[] buf = new int[idx.length];
    DBIDs iset = idx[start];
    if(iset == null || iset.size() < minsupp) {
      return;
    }
    if(minlength <= 1) {
      solution.add(new OneItemset(start, iset.size()));
    }
    if(maxlength > 1) {
      buf[0] = start;
      extractItemsets(iset, idx, buf, 1, start + 1, minsupp, solution);
    }
  }

  private void extractItemsets(DBIDs iset, DBIDs[] idx, int[] buf, int depth, int start, int minsupp, List<Itemset> solution) {
    // TODO: reuse arrays.
    final int depth1 = depth + 1;
    for(int i = start; i < idx.length; i++) {
      if(idx[i] == null) {
        continue;
      }
      DBIDs ids = mergeJoin(iset, idx[i]);
      if(ids.size() < minsupp) {
        continue;
      }
      buf[depth] = i;
      int[] items = Arrays.copyOf(buf, depth1);
      if(depth1 >= minlength) {
        solution.add(new SparseItemset(items, ids.size()));
      }
      if(depth1 <= maxlength) {
        extractItemsets(ids, idx, buf, depth1, i + 1, minsupp, solution);
      }
    }
  }

  private DBIDs mergeJoin(DBIDs first, DBIDs second) {
    assert (!(first instanceof HashSetDBIDs));
    assert (!(second instanceof HashSetDBIDs));
    ArrayModifiableDBIDs ids = DBIDUtil.newArray();

    DBIDIter i1 = first.iter(), i2 = second.iter();
    while(i1.valid() && i2.valid()) {
      int c = DBIDUtil.compare(i1, i2);
      if(c < 0) {
        i1.advance();
      }
      else if(c > 0) {
        i2.advance();
      }
      else {
        ids.add(i1);
        i1.advance();
        i2.advance();
      }
    }
    return ids;
  }

  private DBIDs[] buildIndex(Relation<BitVector> relation, int dim, int minsupp) {
    ArrayModifiableDBIDs[] idx = new ArrayModifiableDBIDs[dim];
    for(int i = 0; i < dim; i++) {
      idx[i] = DBIDUtil.newArray();
    }
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      SparseFeatureVector<?> bv = relation.get(iter);
      // TODO: only count those which satisfy minlength?
      for(int it = bv.iter(); bv.iterValid(it); it = bv.iterAdvance(it)) {
        idx[bv.iterDim(it)].add(iter);
      }
    }
    // Forget non-frequent 1-itemsets.
    for(int i = 0; i < dim; i++) {
      if(idx[i].size() < minsupp) {
        idx[i] = null;
      }
      else {
        idx[i].sort();
      }
    }
    return idx;
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
    protected Eclat makeInstance() {
      return new Eclat(minsupp, minlength, maxlength);
    }
  }
}
