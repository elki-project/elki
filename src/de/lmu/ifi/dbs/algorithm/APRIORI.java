package de.lmu.ifi.dbs.algorithm;

import java.util.*;

import de.lmu.ifi.dbs.algorithm.result.AprioriResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.BitVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.*;

/**
 * Provides the apriori algorithm.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class APRIORI extends AbstractAlgorithm<BitVector> {

	/**
	 * Parameter minimum frequency.
	 */
	public static final String MINIMUM_FREQUENCY_P = "minfreq";

	/**
	 * Parameter minimum support.
	 */
	public static final String MINIMUM_SUPPORT_P = "minsupp";

	/**
	 * Description for parameter frequency.
	 */
	public static final String MINIMUM_FREQUENCY_D = "minimum frequency (as percentage, i.e.: 0 <= minfreq <= 1) (alternatively to parameter "
			+ MINIMUM_SUPPORT_P + ")";

	/**
	 * The minimum frequency.
	 */
	private double minfreq = -1;

	/**
	 * Description for parameter minimum support.
	 */
	public static final String MINIMUM_SUPPORT_D = "minimum support as minimally required number of transactions (alternatively to parameter "
			+ MINIMUM_FREQUENCY_P
			+ " - setting "
			+ MINIMUM_SUPPORT_P
			+ " is slightly preferable over setting "
			+ MINIMUM_FREQUENCY_P
			+ " in terms of efficiency)";

	/**
	 * The minimum support.
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
	 * Provides the apriori algorithm.
	 */
	public APRIORI() {
		super();
	
		// constraint list for minFrequence-parameter
		ArrayList<ParameterConstraint<Number>> minFreqConstraints = new ArrayList<ParameterConstraint<Number>>();
		minFreqConstraints.add(new GreaterEqualConstraint(0));
		minFreqConstraints.add(new LessEqualConstraint(1));
		DoubleParameter minFreq = new DoubleParameter(MINIMUM_FREQUENCY_P,MINIMUM_FREQUENCY_D,minFreqConstraints);
		// optional parameter 
		minFreq.setOptional(true);
		optionHandler.put(MINIMUM_FREQUENCY_P, minFreq);
		
		// minimum support parameter
		IntParameter minSupp = new IntParameter(MINIMUM_SUPPORT_P,MINIMUM_SUPPORT_D,new GreaterEqualConstraint(0));
		minSupp.setOptional(true);
		optionHandler.put(MINIMUM_SUPPORT_P, minSupp);
		
		// global parameter constraints
		ArrayList<Parameter> globalConstraints = new ArrayList<Parameter>();
		globalConstraints.add(minFreq);
		globalConstraints.add(minSupp);
		optionHandler.setGlobalParameterConstraint(new OnlyOneIsAllowedToBeSetGlobalConstraint(globalConstraints));		
		optionHandler.setGlobalParameterConstraint(new OneMustBeSetGlobalConstraint(globalConstraints));
	}

	/**
	 * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
	 */
	protected void runInTime(Database<BitVector> database)
			throws IllegalStateException {
		support = new Hashtable<BitSet, Integer>();
		List<BitSet> solution = new ArrayList<BitSet>();
		int size = database.size();
		if (size > 0) {
			int dim;
			try {
				dim = database.dimensionality();
			} catch (UnsupportedOperationException e) {
				dim = 0;
			}
			BitSet[] candidates = new BitSet[dim];
			for (int i = 0; i < dim; i++) {
				candidates[i] = new BitSet();
				candidates[i].set(i);
			}
			while (candidates.length > 0) {
				StringBuffer msg = new StringBuffer();
				BitSet[] frequentItemsets = frequentItemsets(candidates,
						database);
				if (this.debug) {
					msg.append("\ncandidates" + Arrays.asList(candidates));
					msg.append("\nfrequentItemsets"
							+ Arrays.asList(frequentItemsets));
				}
				for (BitSet bitSet : frequentItemsets) {
					solution.add(bitSet);
				}
				BitSet[] joined = join(frequentItemsets);
				candidates = prune(joined, size);
				if (this.debug) {
					msg.append("\npruned candidates"
							+ Arrays.asList(candidates));
					verbose(msg.toString());
				}
			}
		}
		result = new AprioriResult(solution, support, database);
	}

	/**
	 * Prunes a given set of candidates to keep only those BitSets where all
	 * subsets of bits flipping one bit are frequent already.
	 * 
	 * @param candidates
	 *            the candidates to be pruned
	 * @param size
	 *            size of the database
	 * @return a set of BitSets where all subsets of bits flipping one bit are
	 *         frequent already
	 */
	protected BitSet[] prune(BitSet[] candidates, int size) {
		List<BitSet> candidateList = new ArrayList<BitSet>();
		for (BitSet bitSet : candidates) {
			boolean unpruned = true;
			for (int i = bitSet.nextSetBit(0); i <= 0 && unpruned; i = bitSet
					.nextSetBit(i + 1)) {
				bitSet.clear(i);
				unpruned = (minfreq > -1 && support.get(bitSet).doubleValue()
						/ size >= minfreq)
						|| support.get(bitSet) >= minsupp;
				bitSet.set(i);
			}
			if (unpruned) {
				candidateList.add(bitSet);	
			}
		}
		return candidateList.toArray(new BitSet[candidateList.size()]);
	}

	/**
	 * Returns a set of BitSets generated by joining pairs of given BitSets
	 * (relying on the given BitSets being sorted), increasing the length by 1.
	 * 
	 * @param frequentItemsets
	 *            the BitSets to be joined
	 * @return a set of BitSets generated by joining pairs of given BitSets,
	 *         increasing the length by 1
	 */
	protected BitSet[] join(BitSet[] frequentItemsets) {
		List<BitSet> joined = new ArrayList<BitSet>();
		for (int i = 0; i < frequentItemsets.length; i++) {
			for (int j = i + 1; j < frequentItemsets.length; j++) {
				BitSet b1 = (BitSet) frequentItemsets[i].clone();
				BitSet b2 = (BitSet) frequentItemsets[j].clone();
				int b1i = b1.length() - 1;
				int b2i = b2.length() - 1;
				b1.clear(b1i);
				b2.clear(b2i);
				if (b1.equals(b2)) {
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
	 * @param candidates
	 *            the candidates to be evaluated
	 * @param database
	 *            the database to evaluate the candidates on
	 * @return the frequent BitSets out of the given BitSets with respect to the
	 *         given database
	 */
	protected BitSet[] frequentItemsets(BitSet[] candidates,
			Database<BitVector> database) {
		for (BitSet bitSet : candidates) {
			if (support.get(bitSet) == null) {
				support.put(bitSet, 0);
			}
		}
		for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
			BitVector bv = database.get(iter.next());
			for (BitSet bitSet : candidates) {
				if (bv.contains(bitSet)) {
					support.put(bitSet, support.get(bitSet) + 1);
				}
			}
		}
		List<BitSet> frequentItemsets = new ArrayList<BitSet>();
		for (BitSet bitSet : candidates) {
			if ((minfreq > -1 && support.get(bitSet).doubleValue()
					/ (double) database.size() >= minfreq)
					|| support.get(bitSet) >= minsupp) {
				frequentItemsets.add(bitSet);
			}
		}
		return frequentItemsets.toArray(new BitSet[frequentItemsets.size()]);
	}

	/**
	 * @see Algorithm#getResult()
	 */
	public Result<BitVector> getResult() {
		return result;
	}

	/**
	 * @see Algorithm#getDescription()
	 */
	public Description getDescription() {
		// TODO reference
		return new Description("APRIORI", "APRIORI",
				"search for frequent itemsets", "...");
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	@Override
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		
		if(optionHandler.isSet(MINIMUM_FREQUENCY_P)){
			minfreq = (Double)optionHandler.getOptionValue(MINIMUM_FREQUENCY_P);
			
		}
		else{
			minsupp = (Integer)optionHandler.getOptionValue(MINIMUM_SUPPORT_P);
		}
		
		setParameters(args, remainingParameters);
		return remainingParameters;
	}
}
