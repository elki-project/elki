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
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules;

import java.util.*;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.AbstractFrequentItemsetAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.FPGrowth;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.Itemset;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.SparseItemset;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules.interest.Confidence;
import de.lmu.ifi.dbs.elki.algorithm.itemsetmining.associationrules.interest.InterestingnessMeasure;
import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AssociationRuleResult;
import de.lmu.ifi.dbs.elki.result.FrequentItemsetsResult;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.IntegerArray;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Association rule generation from frequent itemsets
 * <p>
 * This algorithm calls a specified frequent itemset algorithm
 * and calculates all association rules, having a interest value between
 * then the specified boundaries form the obtained frequent itemsets
 * <p>
 * Reference:
 * <p>
 * M. J. Zaki, W. Meira Jr<br>
 * Data mining and analysis: fundamental concepts and algorithms<br>
 * Cambridge University Press, 2014
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @composed - - - AbstractFrequentItemsetAlgorithm
 * @composed - - - InterestingnessMeasure
 * @composed - - - ItemsetSearcher
 * @assoc - - - FrequentItemsetsResult
 * @navassoc - - - AssociationRuleResult
 * @navassoc - - - AssociationRule
 */
@Reference(authors = "M. J. Zaki, W. Meira Jr.", //
    title = "Data mining and analysis: fundamental concepts and algorithms", //
    booktitle = "Cambridge University Press, 2014", //
    bibkey = "DBLP:books/cu/ZM2014")
public class AssociationRuleGeneration extends AbstractAlgorithm<AssociationRuleResult> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(AssociationRuleGeneration.class);

  /**
   * Frequent Itemset Algorithm to be used.
   */
  protected AbstractFrequentItemsetAlgorithm frequentItemAlgo;

  /**
   * Interestingness measure to be used.
   */
  protected InterestingnessMeasure interestingness;

  /**
   * Parameter for minimum interestingness measure.
   */
  protected double minmeasure = Double.MIN_VALUE;

  /**
   * Parameter for maximum interestingness measure.
   */
  protected double maxmeasure = Double.MAX_VALUE;

  /**
   * Constructor.
   *
   * @param frequentItemAlgo FrequentItemset mining Algorithm
   * @param interestMeasure Interestingness measure
   * @param minmeasure Minimum threshold for interestingness measure
   * @param maxmeasure Maximum threshold for interestingness measure
   */
  public AssociationRuleGeneration(AbstractFrequentItemsetAlgorithm frequentItemAlgo, InterestingnessMeasure interestMeasure, double minmeasure, double maxmeasure) {
    super();
    this.frequentItemAlgo = frequentItemAlgo;
    this.interestingness = interestMeasure;
    this.minmeasure = minmeasure;
    this.maxmeasure = maxmeasure;
  }

  /**
   * Constructor
   * 
   * @param frequentItemAlgo FrequentItemset mining Algorithm
   * @param interestMeasure Interestingness measure
   * @param minmeasure Minimum threshold for interestingness measure
   */
  public AssociationRuleGeneration(AbstractFrequentItemsetAlgorithm frequentItemAlgo, InterestingnessMeasure interestMeasure, double minmeasure) {
    this(frequentItemAlgo, interestMeasure, minmeasure, Double.POSITIVE_INFINITY);
  }

  /**
   * Run on a database
   * 
   * @param database Database
   * @return Association rules
   */
  public AssociationRuleResult run(Database database) {
    // Run frequent itemset mining
    return new Instance().run(frequentItemAlgo.run(database));
  }

  /**
   * Class to run
   * 
   * @author Erich Schubert
   */
  public class Instance {
    /**
     * Total number of transactions, needed by some measures.
     */
    private int totalTransactions;

    /**
     * Scratch itemset: antecedent.
     */
    private PartialItemset scratch1;

    /**
     * Scratch itemset: consequent.
     */
    private PartialItemset scratch2;

    /**
     * List of generated rules.
     */
    private ArrayList<AssociationRule> rules;

    /**
     * Class to search for itemsets via binary search.
     */
    private ItemsetSearcher searcher;

    /**
     * Metadata for printing.
     */
    private VectorFieldTypeInformation<BitVector> meta;

    /**
     * Process a set of frequent itemsets.
     * 
     * @param frequentResult Frequent itemsets.
     * @return Association rules
     */
    public AssociationRuleResult run(FrequentItemsetsResult frequentResult) {
      // Itemsets
      List<Itemset> itemsets = frequentResult.getItemsets();

      // Shortcut.
      if(itemsets.isEmpty()) {
        LOG.warning("No frequent itemsets found.");
        return new AssociationRuleResult("association rules", "arules", Collections.emptyList(), frequentResult.getMeta());
      }

      // Ensure it is an array list for efficiency:
      itemsets = itemsets instanceof ArrayList ? itemsets : new ArrayList<>(itemsets);

      // Meta data for item tags
      this.meta = frequentResult.getMeta();

      // Longest itemset
      int maxlen = itemsets.get(itemsets.size() - 1).length();

      // Number of transactions
      this.totalTransactions = frequentResult.getTotal();

      // Output List
      this.rules = new ArrayList<AssociationRule>();

      // Item search helper
      this.searcher = new ItemsetSearcher(itemsets);

      // Scratch itemset candidate.
      int[] ind = new int[maxlen];
      Arrays.fill(ind, -1);
      this.scratch1 = new PartialItemset(ind);
      this.scratch2 = new PartialItemset(ind);

      // Iterate through all frequent itemsets
      for(int i = searcher.getOffset(2), e = itemsets.size(); i < e && i >= 0; i++) {
        final Itemset itemset = itemsets.get(i);
        final int len = itemset.length();
        assert (len > 1);
        if(LOG.isDebuggingFine()) {
          LOG.fine("Searching for rules based on: " + itemset);
        }
        // Copy itemset to scratch buffer:
        for(int it = itemset.iter(), j = 0; itemset.iterValid(it); it = itemset.iterAdvance(it), j++) {
          ind[j] = itemset.iterDim(it);
        }
        scratch1.begin = 0;
        scratch1.len = len;
        scratch2.begin = len;
        scratch2.len = 0;

        processSubsets(itemset, len, len - 1);
      }
      return new AssociationRuleResult("association rules", "arules", rules, meta);
    }

    private void processSubsets(Itemset itemset, final int len, int cur) {
      // TODO: allow cur == 0, i.e. empty head?
      while(cur >= 0 && scratch1.len > 1) {
        assert (cur < scratch1.len);
        int[] indices = scratch1.indices;
        // Option 1: move last entry to consequent.
        int elemMoved = indices[cur];
        System.arraycopy(indices, cur + 1, indices, cur, scratch1.len - cur - 1);
        // Adjust lengths:
        scratch1.len--;
        scratch2.len++;
        scratch2.begin--;
        indices[scratch1.len] = elemMoved;

        // Find the actual itemsets
        Itemset antecedent = searcher.search(scratch1);
        Itemset consequent = searcher.search(scratch2);

        if(antecedent == null) {
          LOG.warning(scratch1.appendItemsTo(new StringBuilder(100).append("Antecedent not found: "), meta));
        }
        if(consequent == null) {
          LOG.warning(scratch2.appendItemsTo(new StringBuilder(100).append("Consequent not found: "), meta));
        }
        boolean prune = false;
        if(antecedent != null && consequent != null) {
          double measure = interestingness.measure(totalTransactions, antecedent.getSupport(), consequent.getSupport(), itemset.getSupport());

          if(measure >= minmeasure && measure <= maxmeasure) {
            rules.add(new AssociationRule(itemset, consequent, antecedent, measure));
          }
          else {
            // TODO: other monotonic measures?
            if(interestingness instanceof Confidence) {
              prune = true;
              // prune(antecedent, pruning, subsets);
            }
          }
        }
        // TODO: recursion!
        if(!prune) {
          processSubsets(itemset, len, cur - 1);
        }
        // Undo option 1:
        scratch1.len++;
        scratch2.len--;
        scratch2.begin++;
        System.arraycopy(indices, cur, indices, cur + 1, scratch1.len - cur - 1);
        indices[cur] = elemMoved;

        // Option 2: try with next element to the left.
        --cur;
      }
    }
  }

  /**
   * Mutable scatch itemset for finding itemsets, based on
   * {@link SparseItemset}.
   * 
   * <b>Do not use this for storage.</b>
   * 
   * @author Erich Schubert
   */
  protected static class PartialItemset extends Itemset {
    /**
     * Fake length and offset.
     */
    public int len, begin;

    /**
     * Scratch storage.
     */
    public int[] indices;

    /**
     * Constructor.
     *
     * @param indices Indices
     */
    public PartialItemset(int[] indices) {
      super();
      this.indices = indices;
    }

    @Override
    public int length() {
      return len;
    }

    @Override
    public int iter() {
      return 0;
    }

    @Override
    public boolean iterValid(int iter) {
      return iter < len;
    }

    @Override
    public int iterAdvance(int iter) {
      return ++iter;
    }

    @Override
    public int iterDim(int iter) {
      return indices[begin + iter];
    }
  }

  /**
   * Class to find itemsets in a sorted list.
   * 
   * TODO: optimize case of length 1.
   * 
   * @author Erich Schubert
   */
  public static class ItemsetSearcher {
    /**
     * Itemsets to search.
     */
    List<Itemset> itemsets;

    /**
     * Offsets into above array, based on length.
     */
    IntegerArray offsets;

    /**
     * Constructor.
     *
     * @param itemsets Itemsets
     */
    public ItemsetSearcher(List<Itemset> itemsets) {
      this.itemsets = itemsets;
      this.offsets = new IntegerArray();
      offsets.add(0); // Offset for length 0.
      offsets.add(0); // Offset for length 1.
      buildIndex(0, itemsets.size());
      offsets.add(itemsets.size());
    }

    /**
     * Find an itemset, using binary search.
     * 
     * @param c Itemset to search
     * @return Found itemset
     */
    public Itemset search(Itemset c) {
      int l = c.length(), lp1 = l + 1;
      if(l == 0 || lp1 >= offsets.size()) {
        return null;
      }
      int b = offsets.get(l), e = offsets.get(lp1);
      while(b < e) {
        int m = (b + e) >>> 1;
        Itemset mi = itemsets.get(m);
        int cmp = c.compareTo(mi);
        if(cmp == 0) {
          return mi;
        }
        if(cmp < 0) {
          e = m;
        }
        else {
          b = m + 1;
        }
      }
      return null;
    }

    /**
     * Length of longest itemset.
     * 
     * @return Length
     */
    public int maxLength() {
      return itemsets.size() - 1;
    }

    /**
     * Offset for itemsets of length i.
     * 
     * @param i Length
     * @return Offset.
     */
    public int getOffset(int i) {
      return offsets.get(i);
    }

    /**
     * Build a length index by binary search.
     * 
     * @param begin Begin
     * @param end End
     */
    private void buildIndex(int begin, int end) {
      while(begin < end) {
        int half = (begin + end) >>> 1;
        int len = itemsets.get(half).length();
        // Single-point interval:
        if(begin == half) {
          assert (begin + 1 == end);
          // Insert cutoff points into offsets.
          while(len >= offsets.size) {
            // Add border to index:
            offsets.add(begin);
          }
          return;
        }
        // Recursion to the left.
        if(len >= offsets.size) {
          // Search left via recursion.
          buildIndex(begin, half);
        }
        // Search right via loop.
        begin = half;
      }
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return frequentItemAlgo.getInputTypeRestriction();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Frederic Sautter
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the frequentItemsetAlgorithm to be used.
     */
    public static final OptionID FREQUENTITEMALGO_ID = new OptionID("associationrules.algorithm", //
        "Algorithm to be used for frequent itemset mining.");

    /**
     * Parameter to specify the interestingness measure to be used.
     */
    public static final OptionID INTERESTMEASURE_ID = new OptionID("associationrules.interestingness", //
        "Interestingness measure to be used");

    /**
     * Parameter to specify the minimum threshold for the interestingness
     * measure.
     */
    public static final OptionID MINMEASURE_ID = new OptionID("associationrules.minmeasure", //
        "Minimum threshold for specified interstingness measure");

    /**
     * Parameter to specify the maximum threshold for the interestingness
     * measure.
     */
    public static final OptionID MAXMEASURE_ID = new OptionID("associationrules.maxmeasure", //
        "Maximum threshold for specified interstingness measure");

    /**
     * Parameter for frequent itemset mining.
     */
    protected AbstractFrequentItemsetAlgorithm frequentItemAlgo;

    /**
     * Parameter for interestingness measure.
     */
    protected InterestingnessMeasure interestMeasure;

    /**
     * Parameter for minimum interestingness measure.
     */
    protected double minmeasure = Double.MIN_VALUE;

    /**
     * Parameter for maximum interestingness measure.
     */
    protected double maxmeasure = Double.MAX_VALUE;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<AbstractFrequentItemsetAlgorithm> frequentItemAlgoP = new ObjectParameter<>(FREQUENTITEMALGO_ID, AbstractFrequentItemsetAlgorithm.class, FPGrowth.class);
      if(config.grab(frequentItemAlgoP)) {
        frequentItemAlgo = frequentItemAlgoP.instantiateClass(config);
      }
      ObjectParameter<InterestingnessMeasure> interestMeasureP = new ObjectParameter<>(INTERESTMEASURE_ID, InterestingnessMeasure.class, Confidence.class);
      if(config.grab(interestMeasureP)) {
        interestMeasure = interestMeasureP.instantiateClass(config);
      }
      DoubleParameter minmeasureP = new DoubleParameter(MINMEASURE_ID);
      if(config.grab(minmeasureP)) {
        minmeasure = minmeasureP.getValue();
      }
      DoubleParameter maxmeasureP = new DoubleParameter(MAXMEASURE_ID)//
          .setOptional(true);
      if(config.grab(maxmeasureP)) {
        maxmeasure = maxmeasureP.getValue();
      }
    }

    @Override
    protected AssociationRuleGeneration makeInstance() {
      return new AssociationRuleGeneration(frequentItemAlgo, interestMeasure, minmeasure, maxmeasure);
    }
  }
}
