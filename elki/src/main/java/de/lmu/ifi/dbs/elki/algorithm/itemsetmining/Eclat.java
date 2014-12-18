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
import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.SparseFeatureVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.result.AprioriResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Eclat is a depth-first discovery algorithm for mining frequent itemsets.
 * 
 * Eclat discovers frequent itemsets by first transforming the data into a
 * (sparse) column-oriented form, then performing a depth-first traversal of the
 * prefix lattice, stopping traversal when the minimum support is no longer
 * satisfied.
 *
 * This implementation is the basic algorithm only, and does not use diffsets.
 * Columns are represented using a sparse representation, which theoretically is
 * beneficial when the density is less than 1/31. This corresponds roughly to a
 * minimum support of 3% for 1-itemsets. When searching for itemsets with a
 * larger minimum support, it may be desirable to use a dense bitset
 * representation instead and/or implement an automatic switching technique!
 *
 * Performance of this implementation is probably surpassed with a low-level C
 * implementation based on SIMD bitset operations as long as support of an
 * itemset is high, which are not easily accessible in Java.
 *
 * Reference:
 * <p>
 * New Algorithms for Fast Discovery of Association Rules<br />
 * M.J. Zaki, S. Parthasarathy, M. Ogihara, and W. Li<br />
 * Proc. 3rd ACM SIGKDD '97 Int. Conf. on Knowledge Discovery and Data Mining
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(title = "New Algorithms for Fast Discovery of Association Rules", //
authors = "M.J. Zaki, S. Parthasarathy, M. Ogihara, and W. Li", //
booktitle = "Proc. 3rd ACM SIGKDD '97 Int. Conf. on Knowledge Discovery and Data Mining", //
url = "http://www.aaai.org/Library/KDD/1997/kdd97-060.php")
public class Eclat extends AbstractAlgorithm<AprioriResult> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(Eclat.class);

  /**
   * Prefix for statistics.
   */
  private static final String STAT = Eclat.class.getName() + ".";

  /**
   * Minimum support.
   */
  private double minsupp;

  /**
   * Constructor.
   *
   * @param minsupp Minimum support
   */
  public Eclat(double minsupp) {
    super();
    this.minsupp = minsupp;
  }

  /**
   * Run the Eclat algorithm
   * 
   * @param db Database to process
   * @param relation Bit vector relation
   * @return Frequent patterns found
   */
  public AprioriResult run(Database db, final Relation<BitVector> relation) {
    // TODO: implement with resizable arrays, to not need dim.
    final int dim = RelationUtil.dimensionality(relation);
    final VectorFieldTypeInformation<BitVector> meta = RelationUtil.assumeVectorField(relation);
    // Compute absolute minsupport
    final int minsupp = (int) Math.ceil(this.minsupp < 1 ? this.minsupp * relation.size() : this.minsupp);

    LOG.verbose("Build 1-dimensional transaction lists.");
    DBIDs[] idx = buildIndex(relation, dim, minsupp);

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Building frequent itemsets", idx.length, LOG) : null;
    final List<Itemset> solution = new ArrayList<>();
    for(int i = 0; i < idx.length; i++) {
      LOG.incrementProcessed(prog);
      extractItemsets(idx, i, minsupp, solution);
    }
    LOG.ensureCompleted(prog);
    Collections.sort(solution);

    LOG.statistics(new LongStatistic(STAT + "frequent-itemsets", solution.size()));
    return new AprioriResult("FP-Growth", "fp-growth", solution, meta);
  }

  // TODO: implement diffsets.
  private void extractItemsets(DBIDs[] idx, int start, int minsupp, List<Itemset> solution) {
    int[] buf = new int[idx.length];
    DBIDs iset = idx[start];
    if(iset == null || iset.size() < minsupp) {
      return;
    }
    solution.add(new OneItemset(start, iset.size()));
    buf[0] = start;
    extractItemsets(iset, idx, buf, 1, start + 1, minsupp, solution);
  }

  private void extractItemsets(DBIDs iset, DBIDs[] idx, int[] buf, int depth, int start, int minsupp, List<Itemset> solution) {
    // TODO: reuse arrays.
    for(int i = start; i < idx.length; i++) {
      if(idx[i] == null) {
        continue;
      }
      DBIDs ids = mergeJoin(iset, idx[i]);
      if(ids.size() < minsupp) {
        continue;
      }
      buf[depth] = i;
      int[] items = Arrays.copyOf(buf, depth + 1);
      solution.add(new SparseItemset(items, ids.size()));
      extractItemsets(ids, idx, buf, depth + 1, i + 1, minsupp, solution);
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
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the minimum support, in absolute or relative terms.
     */
    public static final OptionID MINSUPP_ID = new OptionID("eclat.minsupp", //
    "Threshold for minimum support as minimally required number of transactions (if > 1) " //
        + "or the minimum frequency (if <= 1).");

    /**
     * Parameter for minimum support.
     */
    protected double minsupp;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minsuppP = new DoubleParameter(MINSUPP_ID) //
      .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(minsuppP)) {
        minsupp = minsuppP.getValue();
      }
    }

    @Override
    protected Eclat makeInstance() {
      return new Eclat(minsupp);
    }
  }
}
