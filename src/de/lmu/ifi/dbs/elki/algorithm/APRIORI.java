package de.lmu.ifi.dbs.elki.algorithm;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AprioriResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.OneMustBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.OnlyOneIsAllowedToBeSetGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Provides the APRIORI algorithm for Mining Association Rules.
 * <p>
 * Reference: <br>
 * R. Agrawal, R. Srikant: Fast Algorithms for Mining Association Rules in Large
 * Databases. <br>
 * In Proc. 20th Int. Conf. on Very Large Data Bases (VLDB '94), Santiago de
 * Chile, Chile 1994.
 * </p>
 * 
 * @author Arthur Zimek
 */
@Title("APRIORI: Algorithm for Mining Association Rules")
@Description("Searches for frequent itemsets")
@Reference(authors = "R. Agrawal, R. Srikant", title = "Fast Algorithms for Mining Association Rules in Large Databases", booktitle = "Proc. 20th Int. Conf. on Very Large Data Bases (VLDB '94), Santiago de Chile, Chile 1994", url = "http://www.acm.org/sigmod/vldb/conf/1994/P487.PDF")
public class APRIORI extends AbstractAlgorithm<AprioriResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(APRIORI.class);

  /**
   * Optional parameter to specify the threshold for minimum frequency, must be
   * a double greater than or equal to 0 and less than or equal to 1.
   * Alternatively to parameter {@link #MINSUPP_ID}).
   */
  public static final OptionID MINFREQ_ID = OptionID.getOrCreateOptionID("apriori.minfreq", "Threshold for minimum frequency as percentage value " + "(alternatively to parameter apriori.minsupp).");

  /**
   * Parameter to specify the threshold for minimum support as minimally
   * required number of transactions, must be an integer equal to or greater
   * than 0. Alternatively to parameter {@link #MINFREQ_ID} - setting
   * {@link #MINSUPP_ID} is slightly preferable over setting {@link #MINFREQ_ID}
   * in terms of efficiency.
   */
  public static final OptionID MINSUPP_ID = OptionID.getOrCreateOptionID("apriori.minsupp", "Threshold for minimum support as minimally required number of transactions " + "(alternatively to parameter apriori.minfreq" + " - setting apriori.minsupp is slightly preferable over setting " + "apriori.minfreq in terms of efficiency).");

  /**
   * Holds the value of {@link #MINFREQ_ID}.
   */
  private double minfreq = Double.NaN;

  /**
   * Holds the value of {@link #MINSUPP_ID}.
   */
  private int minsupp = Integer.MIN_VALUE;

  /**
   * Constructor with minimum frequency.
   * 
   * @param minfreq Minimum frequency
   */
  public APRIORI(double minfreq) {
    super();
    this.minfreq = minfreq;
  }

  /**
   * Constructor with minimum support.
   * 
   * @param minsupp Minimum support
   */
  public APRIORI(int minsupp) {
    super();
    this.minsupp = minsupp;
  }

  /**
   * Performs the APRIORI algorithm on the given database.
   * 
   * @param database the Database to run APRIORI on
   * @param relation the Relation to process
   * @return the AprioriResult learned by this APRIORI
   */
  public AprioriResult run(Database database, Relation<BitVector> relation) {
    Map<BitSet, Integer> support = new Hashtable<BitSet, Integer>();
    List<BitSet> solution = new ArrayList<BitSet>();
    final int size = relation.size();
    if(size > 0) {
      int dim;
      try {
        dim = DatabaseUtil.dimensionality(relation);
      }
      catch(UnsupportedOperationException e) {
        dim = 0;
      }
      BitSet[] candidates = new BitSet[dim];
      for(int i = 0; i < dim; i++) {
        candidates[i] = new BitSet();
        candidates[i].set(i);
      }
      while(candidates.length > 0) {
        StringBuffer msg = new StringBuffer();
        BitSet[] frequentItemsets = frequentItemsets(support, candidates, relation);
        if(logger.isVerbose()) {
          msg.append("\ncandidates").append(Arrays.asList(candidates));
          msg.append("\nfrequentItemsets").append(Arrays.asList(frequentItemsets));
        }
        for(BitSet bitSet : frequentItemsets) {
          solution.add(bitSet);
        }
        BitSet[] joined = join(frequentItemsets);
        candidates = prune(support, joined, size);
        if(logger.isVerbose()) {
          msg.append("\ncandidates after pruning").append(Arrays.asList(candidates));
          logger.verbose(msg.toString());
        }
      }
    }
    return new AprioriResult("APRIORI", "apriori", solution, support);
  }

  /**
   * Prunes a given set of candidates to keep only those BitSets where all
   * subsets of bits flipping one bit are frequent already.
   * 
   * @param support Support map
   * @param candidates the candidates to be pruned
   * @param size size of the database
   * @return a set of BitSets where all subsets of bits flipping one bit are
   *         frequent already
   */
  protected BitSet[] prune(Map<BitSet, Integer> support, BitSet[] candidates, int size) {
    List<BitSet> candidateList = new ArrayList<BitSet>();
    // MinFreq pruning
    if(minfreq >= 0) {
      for(BitSet bitSet : candidates) {
        boolean unpruned = true;
        for(int i = bitSet.nextSetBit(0); i >= 0 && unpruned; i = bitSet.nextSetBit(i + 1)) {
          bitSet.clear(i);
          if(support.get(bitSet) != null) {
            unpruned = support.get(bitSet).doubleValue() / size >= minfreq;
          }
          else {
            unpruned = false;
            // logger.warning("Support not found for bitSet " + bitSet);
          }
          bitSet.set(i);
        }
        if(unpruned) {
          candidateList.add(bitSet);
        }
      }
    }
    else {
      // Minimum support pruning
      for(BitSet bitSet : candidates) {
        boolean unpruned = true;
        for(int i = bitSet.nextSetBit(0); i >= 0 && unpruned; i = bitSet.nextSetBit(i + 1)) {
          bitSet.clear(i);
          if(support.get(bitSet) != null) {
            unpruned = support.get(bitSet) >= minsupp;
          }
          else {
            unpruned = false;
            // logger.warning("Support not found for bitSet " + bitSet);
          }
          bitSet.set(i);
        }
        if(unpruned) {
          candidateList.add(bitSet);
        }
      }
    }
    return candidateList.toArray(new BitSet[candidateList.size()]);
  }

  /**
   * Returns a set of BitSets generated by joining pairs of given BitSets
   * (relying on the given BitSets being sorted), increasing the length by 1.
   * 
   * @param frequentItemsets the BitSets to be joined
   * @return a set of BitSets generated by joining pairs of given BitSets,
   *         increasing the length by 1
   */
  protected BitSet[] join(BitSet[] frequentItemsets) {
    List<BitSet> joined = new ArrayList<BitSet>();
    for(int i = 0; i < frequentItemsets.length; i++) {
      for(int j = i + 1; j < frequentItemsets.length; j++) {
        BitSet b1 = (BitSet) frequentItemsets[i].clone();
        BitSet b2 = (BitSet) frequentItemsets[j].clone();
        int b1i = b1.length() - 1;
        int b2i = b2.length() - 1;
        b1.clear(b1i);
        b2.clear(b2i);
        if(b1.equals(b2)) {
          b1.set(b1i);
          b1.set(b2i);
          joined.add(b1);
        }
      }
    }
    return joined.toArray(new BitSet[joined.size()]);
  }

  /**
   * Returns the frequent BitSets out of the given BitSets with respect to the
   * given database.
   * 
   * @param support Support map.
   * @param candidates the candidates to be evaluated
   * @param database the database to evaluate the candidates on
   * @return the frequent BitSets out of the given BitSets with respect to the
   *         given database
   */
  protected BitSet[] frequentItemsets(Map<BitSet, Integer> support, BitSet[] candidates, Relation<BitVector> database) {
    for(BitSet bitSet : candidates) {
      if(support.get(bitSet) == null) {
        support.put(bitSet, 0);
      }
    }
    for(DBIDIter iditer = database.iterDBIDs(); iditer.valid(); iditer.advance()) {
      BitVector bv = database.get(iditer);
      for(BitSet bitSet : candidates) {
        if(bv.contains(bitSet)) {
          support.put(bitSet, support.get(bitSet) + 1);
        }
      }
    }
    List<BitSet> frequentItemsets = new ArrayList<BitSet>();
    if(minfreq >= 0.0) {
      // TODO: work with integers?
      double critsupp = minfreq * database.size();
      for(BitSet bitSet : candidates) {
        if(support.get(bitSet).doubleValue() >= critsupp) {
          frequentItemsets.add(bitSet);
        }
      }
    }
    else {
      // Use minimum support
      for(BitSet bitSet : candidates) {
        if(support.get(bitSet) >= minsupp) {
          frequentItemsets.add(bitSet);
        }
      }
    }
    return frequentItemsets.toArray(new BitSet[frequentItemsets.size()]);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.BIT_VECTOR_FIELD);
  }
  
  @Override
  protected Logging getLogger() {
    return logger;
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
     * Parameter for minFreq
     */
    protected Double minfreq = null;

    /**
     * Parameter for minSupp
     */
    protected Integer minsupp = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minfreqP = new DoubleParameter(MINFREQ_ID, true);
      minfreqP.addConstraint(new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.CLOSE, 1, IntervalConstraint.IntervalBoundary.CLOSE));
      if(config.grab(minfreqP)) {
        minfreq = minfreqP.getValue();
      }

      IntParameter minsuppP = new IntParameter(MINSUPP_ID, true);
      minsuppP.addConstraint(new GreaterEqualConstraint(0));
      if(config.grab(minsuppP)) {
        minsupp = minsuppP.getValue();
      }

      // global parameter constraints
      ArrayList<Parameter<?, ?>> globalConstraints = new ArrayList<Parameter<?, ?>>();
      globalConstraints.add(minfreqP);
      globalConstraints.add(minsuppP);
      config.checkConstraint(new OnlyOneIsAllowedToBeSetGlobalConstraint(globalConstraints));
      config.checkConstraint(new OneMustBeSetGlobalConstraint(globalConstraints));
    }

    @Override
    protected APRIORI makeInstance() {
      if(minfreq != null) {
        return new APRIORI(minfreq);
      }
      if(minsupp != null) {
        return new APRIORI(minsupp);
      }
      return null;
    }
  }
}