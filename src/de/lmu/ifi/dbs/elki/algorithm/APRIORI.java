package de.lmu.ifi.dbs.elki.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.AprioriResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
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
public class APRIORI extends AbstractAlgorithm<BitVector, AprioriResult> {
  /**
   * OptionID for {@link #MINFREQ_PARAM}
   */
  public static final OptionID MINFREQ_ID = OptionID.getOrCreateOptionID("apriori.minfreq", "Threshold for minimum frequency as percentage value " + "(alternatively to parameter apriori.minsupp).");

  /**
   * Optional parameter to specify the threshold for minimum frequency, must be
   * a double greater than or equal to 0 and less than or equal to 1.
   * Alternatively to parameter {@link APRIORI#MINSUPP_PARAM}).
   * <p>
   * Key: {@code -apriori.minfreq}
   * </p>
   */
  private final DoubleParameter MINFREQ_PARAM = new DoubleParameter(MINFREQ_ID, new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.CLOSE, 1, IntervalConstraint.IntervalBoundary.CLOSE), true);

  /**
   * Holds the value of {@link #MINFREQ_PARAM}.
   */
  private double minfreq = -1;

  /**
   * OptionID for {@link #MINSUPP_PARAM}
   */
  public static final OptionID MINSUPP_ID = OptionID.getOrCreateOptionID("apriori.minsupp", "Threshold for minimum support as minimally required number of transactions " + "(alternatively to parameter apriori.minfreq" + " - setting apriori.minsupp is slightly preferable over setting " + "apriori.minfreq in terms of efficiency).");

  /**
   * Parameter to specify the threshold for minimum support as minimally
   * required number of transactions, must be an integer equal to or greater
   * than 0. Alternatively to parameter {@link APRIORI#MINFREQ_PARAM} - setting
   * {@link APRIORI#MINSUPP_PARAM} is slightly preferable over setting
   * {@link APRIORI#MINFREQ_PARAM} in terms of efficiency.
   * <p>
   * Key: {@code -apriori.minsupp}
   * </p>
   */
  private final IntParameter MINSUPP_PARAM = new IntParameter(MINSUPP_ID, new GreaterEqualConstraint(0), true);

  /**
   * Holds the value of {@link #MINSUPP_PARAM}.
   */
  private int minsupp;

  /**
   * The result.
   */
  private AprioriResult result;

  /**
   * Keeps the support of all evaluated bitsets.
   */
  private Map<BitSet, Integer> support;

  /**
   * Provides the apriori algorithm, adding parameters {@link #MINFREQ_PARAM}
   * and {@link #MINSUPP_PARAM} to the option handler additionally to parameters
   * of super class.
   */
  public APRIORI(Parameterization config) {
    super(config);

    // minimum frequency parameter
    if(config.grab(MINFREQ_PARAM)) {
      minfreq = MINFREQ_PARAM.getValue();
    }

    // minimum support parameter
    if(config.grab(MINSUPP_PARAM)) {
      minsupp = MINSUPP_PARAM.getValue();
    }

    // global parameter constraints
    ArrayList<Parameter<?,?>> globalConstraints = new ArrayList<Parameter<?,?>>();
    globalConstraints.add(MINFREQ_PARAM);
    globalConstraints.add(MINSUPP_PARAM);
    config.checkConstraint(new OnlyOneIsAllowedToBeSetGlobalConstraint(globalConstraints));
    config.checkConstraint(new OneMustBeSetGlobalConstraint(globalConstraints));
  }

  /**
   * Performs the APRIORI algorithm on the given database.
   * 
   * @param database the Database to run APRIORI on
   * @return the AprioriResult learned by this APRIORI
   */
  @Override
  protected AprioriResult runInTime(Database<BitVector> database) throws IllegalStateException {
    support = new Hashtable<BitSet, Integer>();
    List<BitSet> solution = new ArrayList<BitSet>();
    int size = database.size();
    if(size > 0) {
      int dim;
      try {
        dim = database.dimensionality();
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
        BitSet[] frequentItemsets = frequentItemsets(candidates, database);
        if(logger.isVerbose()) {
          msg.append("\ncandidates").append(Arrays.asList(candidates));
          msg.append("\nfrequentItemsets").append(Arrays.asList(frequentItemsets));
        }
        for(BitSet bitSet : frequentItemsets) {
          solution.add(bitSet);
        }
        BitSet[] joined = join(frequentItemsets);
        candidates = prune(joined, size);
        if(logger.isVerbose()) {
          msg.append("\npruned candidates").append(Arrays.asList(candidates));
          logger.verbose(msg.toString());
        }
      }
    }
    result = new AprioriResult(solution, support);
    return result;
  }

  /**
   * Prunes a given set of candidates to keep only those BitSets where all
   * subsets of bits flipping one bit are frequent already.
   * 
   * @param candidates the candidates to be pruned
   * @param size size of the database
   * @return a set of BitSets where all subsets of bits flipping one bit are
   *         frequent already
   */
  protected BitSet[] prune(BitSet[] candidates, int size) {
    List<BitSet> candidateList = new ArrayList<BitSet>();
    for(BitSet bitSet : candidates) {
      boolean unpruned = true;
      for(int i = bitSet.nextSetBit(0); i >= 0 && unpruned; i = bitSet.nextSetBit(i + 1)) {
        bitSet.clear(i);
        unpruned = minfreq > -1 ? support.get(bitSet).doubleValue() / size >= minfreq : support.get(bitSet) >= minsupp;
        bitSet.set(i);
      }
      if(unpruned) {
        candidateList.add(bitSet);
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
   * @param candidates the candidates to be evaluated
   * @param database the database to evaluate the candidates on
   * @return the frequent BitSets out of the given BitSets with respect to the
   *         given database
   */
  protected BitSet[] frequentItemsets(BitSet[] candidates, Database<BitVector> database) {
    for(BitSet bitSet : candidates) {
      if(support.get(bitSet) == null) {
        support.put(bitSet, 0);
      }
    }
    for(Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
      BitVector bv = database.get(iter.next());
      for(BitSet bitSet : candidates) {
        if(bv.contains(bitSet)) {
          support.put(bitSet, support.get(bitSet) + 1);
        }
      }
    }
    List<BitSet> frequentItemsets = new ArrayList<BitSet>();
    for(BitSet bitSet : candidates) {
      if((minfreq > -1 && support.get(bitSet).doubleValue() / database.size() >= minfreq) || support.get(bitSet) >= minsupp) {
        frequentItemsets.add(bitSet);
      }
    }
    return frequentItemsets.toArray(new BitSet[frequentItemsets.size()]);
  }

  public AprioriResult getResult() {
    return result;
  }

  public Description getDescription() {
    return new Description("APRIORI", "Algorithm for Mining Association Rules", "Searches for frequent itemsets", "R. Agrawal, R. Srikant: " + "Fast Algorithms for Mining Association Rules in Large Databases. " + "In Proc. 20th Int. Conf. on Very Large Data Bases (VLDB '94), Santiago de Chile, Chile 1994.");
  }
}
