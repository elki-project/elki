package de.lmu.ifi.dbs.preprocessing;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.lmu.ifi.dbs.algorithm.APRIORI;
import de.lmu.ifi.dbs.algorithm.result.AprioriResult;
import de.lmu.ifi.dbs.data.Bit;
import de.lmu.ifi.dbs.data.BitVector;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.distance.DimensionSelectingDistanceFunction;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.EqualStringConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterEqual;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.LessEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.StringParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Preprocessor for DiSH preference vector assignment to objects of a certain
 * database.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DiSHPreprocessor extends AbstractParameterizable implements
		PreferenceVectorPreprocessor {
	/**
	 * Available strategies for determination of the preference vecrtor.
	 */
	public enum Strategy {
		APRIORI, MAX_INTERSECTION
	}

	/**
	 * The default value for epsilon.
	 */
	public static final DoubleDistance DEFAULT_EPSILON = new DoubleDistance(
			0.001);

	/**
	 * Option string for parameter epsilon.
	 */
	public static final String EPSILON_P = "epsilon";

	/**
	 * Description for parameter epsilon.
	 */
	public static String EPSILON_D = "a double between 0 and 1 specifying the "
			+ "maximum radius of the neighborhood to be "
			+ "considered in each dimension for determination of "
			+ "the preference vector " + "(default is " + DEFAULT_EPSILON
			+ ").";

	/**
	 * Parameter minimum points.
	 */
	public static final String MINPTS_P = "minpts";

	/**
	 * Description for the determination of the preference vector.
	 */
	private static final String CONDITION = "The value of the preference vector in dimension d_i is set to 1 "
			+ "if the epsilon neighborhood contains more than "
			+ MINPTS_P
			+ " "
			+ "points and the following condition holds: "
			+ "for all dimensions d_j: "
			+ "|neighbors(d_i) intersection neighbors(d_j)| >= "
			+ MINPTS_P
			+ ".";

	/**
	 * Description for parameter minimum points.
	 */
	public static final String MINPTS_D = "positive threshold for minumum numbers of points in the epsilon-"
			+ "neighborhood of a point. " + CONDITION;

	/**
	 * Parameter strategy.
	 */
	public static final String STRATEGY_P = "strategy";

	/**
	 * Default strategy.
	 */
	public static Strategy DEFAULT_STRATEGY = Strategy.APRIORI;

	/**
	 * Description for parameter strategy.
	 */
	public static final String STRATEGY_D = "the strategy for determination of the preference vector, "
			+ "available strategies are: ["
			+ Strategy.APRIORI
			+ "| "
			+ Strategy.MAX_INTERSECTION
			+ "]"
			+ "(default is "
			+ DEFAULT_STRATEGY + ")";

	/**
	 * The epsilon value for each dimension;
	 */
	private DoubleDistance epsilon;

	/**
	 * Threshold for minimum number of points in the neighborhood.
	 */
	private int minpts;

	/**
	 * The strategy to determine the preference vector.
	 */
	private Strategy strategy;

	/**
	 * Provides a new AdvancedHiSCPreprocessor that computes the preference
	 * vector of objects of a certain database.
	 */
	public DiSHPreprocessor() {
		super();
		optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P, MINPTS_D,
				new GreaterConstraint(0)));

		ArrayList<ParameterConstraint> cons = new ArrayList<ParameterConstraint>();
		cons.add(new GreaterEqual(0));
		cons.add(new LessEqualConstraint(1));
		optionHandler.put(EPSILON_P, new DoubleParameter(EPSILON_P, EPSILON_D,
				cons));

		ArrayList<ParameterConstraint> strategyCons = new ArrayList<ParameterConstraint>();
		strategyCons
				.add(new EqualStringConstraint(Strategy.APRIORI.toString()));
		strategyCons.add(new EqualStringConstraint(Strategy.MAX_INTERSECTION
				.toString()));
		optionHandler.put(STRATEGY_P, new StringParameter(STRATEGY_P,
				STRATEGY_D, strategyCons));
	}

	/**
	 * @see Preprocessor#run(de.lmu.ifi.dbs.database.Database, boolean, boolean)
	 */
	public void run(Database<RealVector> database, boolean verbose, boolean time) {
		if (database == null) {
			throw new IllegalArgumentException("Database must not be null!");
		}

		if (database.size() == 0) {
			return;
		}

		try {
			long start = System.currentTimeMillis();
			Progress progress = new Progress("Preprocessing preference vector",
					database.size());

			String epsString = epsilon.toString();
			int dim = database.dimensionality();
			DimensionSelectingDistanceFunction[] distanceFunctions = initDistanceFunctions(
					database, dim, verbose, time);

			// noinspection unchecked
			final DistanceFunction<RealVector, DoubleDistance> euklideanDistanceFunction = new EuklideanDistanceFunction();
			euklideanDistanceFunction.setDatabase(database, false, false);

			int processed = 1;
			for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
				StringBuffer msg = new StringBuffer();
				final Integer id = it.next();

				if (this.debug) {
					msg.append("\nid = ").append(id);
					msg.append(" ").append(database.get(id));
					msg.append(" ").append(
							database.getAssociation(AssociationID.LABEL, id));
				}

				// determine neighbors in each dimension
				// noinspection unchecked
				Set<Integer>[] allNeighbors = new Set[dim];
				for (int d = 0; d < dim; d++) {
					List<QueryResult<DoubleDistance>> qrList = database
							.rangeQuery(id, epsString, distanceFunctions[d]);
					allNeighbors[d] = new HashSet<Integer>(qrList.size());
					for (QueryResult<DoubleDistance> qr : qrList) {
						allNeighbors[d].add(qr.getID());
					}
				}

				BitSet preferenceVector = determinePreferenceVector(database,
						allNeighbors, msg);
				database.associate(AssociationID.PREFERENCE_VECTOR, id,
						preferenceVector);
				progress.setProcessed(processed++);

				if (this.debug) {
					debugFine(msg.toString());
				}

				if (verbose) {
					verbose("\r" + progress.getTask() + " - "
							+ progress.toString());
				}
			}

			if (verbose) {
				verbose("");
			}

			long end = System.currentTimeMillis();
			if (time) {
				long elapsedTime = end - start;
				verbose(this.getClass().getName() + " runtime: " + elapsedTime
						+ " milliseconds.");
			}
		} catch (ParameterException e) {
			throw new IllegalStateException(e.getMessage(), e);
		} catch (UnableToComplyException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
	 */
	public String description() {
		StringBuffer description = new StringBuffer();
		description.append(DiSHPreprocessor.class.getName());
		description
				.append(" computes the preference vector of objects of a certain database according to the DiSH algorithm.\n");
		description.append(optionHandler.usage("", false));
		return description.toString();
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		// minpts
		String minptsString = optionHandler.getOptionValue(MINPTS_P);
		try {
			minpts = Integer.parseInt(minptsString);
			if (minpts <= 0) {
				throw new WrongParameterValueException(MINPTS_P, minptsString,
						MINPTS_D);
			}
		} catch (NumberFormatException e) {
			throw new WrongParameterValueException(MINPTS_P, minptsString,
					MINPTS_D, e);
		}

		// epsilon
		if (optionHandler.isSet(EPSILON_P)) {
			String epsString = optionHandler.getOptionValue(EPSILON_P);
			try {
				epsilon = new DoubleDistance(Double.parseDouble(epsString));
				if (epsilon.getDoubleValue() < 0
						|| epsilon.getDoubleValue() > 1) {
					throw new WrongParameterValueException(EPSILON_P,
							epsString, EPSILON_D);
				}
			} catch (NumberFormatException e) {
				throw new WrongParameterValueException(EPSILON_P, epsString,
						EPSILON_D, e);
			}
		} else {
			epsilon = DEFAULT_EPSILON;
		}

		if (optionHandler.isSet(STRATEGY_P)) {
			String strategyString = optionHandler.getOptionValue(STRATEGY_P);
			if (strategyString.equals(Strategy.APRIORI.toString())) {
				strategy = Strategy.APRIORI;
			} else if (strategyString.equals(Strategy.MAX_INTERSECTION
					.toString())) {
				strategy = Strategy.MAX_INTERSECTION;
			} else
				throw new WrongParameterValueException(STRATEGY_P,
						strategyString, STRATEGY_D);
		} else
			strategy = DEFAULT_STRATEGY;

		return remainingParameters;
	}

	/**
	 * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
	 */
	public List<AttributeSettings> getAttributeSettings() {
		List<AttributeSettings> attributeSettings = super
				.getAttributeSettings();

		AttributeSettings mySettings = attributeSettings.get(0);
		mySettings.addSetting(MINPTS_P, Double.toString(minpts));
		mySettings.addSetting(EPSILON_P, epsilon.toString());
		mySettings.addSetting(STRATEGY_P, strategy.toString());

		return attributeSettings;
	}

	/**
	 * Determines the preference vector according to the specified neighbor ids.
	 * 
	 * @param database
	 *            the database storing the objects
	 * @param neighborIDs
	 *            the list of ids of the neighbors in each dimension
	 * @param msg
	 *            a string buffer for debug messages
	 * @return the preference vector
	 */
	private BitSet determinePreferenceVector(Database<RealVector> database,
			Set<Integer>[] neighborIDs, StringBuffer msg)
			throws ParameterException, UnableToComplyException {
		if (strategy.equals(Strategy.APRIORI)) {
			return determinePreferenceVectorByApriori(database, neighborIDs,
					msg);
		} else if (strategy.equals(Strategy.MAX_INTERSECTION)) {
			return determinePreferenceVectorByMaxIntersection(neighborIDs, msg);
		} else {
			throw new IllegalStateException("Should never happen!");
		}
	}

	/**
	 * Determines the preference vector with the apriori strategy.
	 * 
	 * @param database
	 *            the database storing the objects
	 * @param neighborIDs
	 *            the list of ids of the neighbors in each dimension
	 * @param msg
	 *            a string buffer for debug messages
	 * @return the preference vector
	 */
	private BitSet determinePreferenceVectorByApriori(
			Database<RealVector> database, Set<Integer>[] neighborIDs,
			StringBuffer msg) throws ParameterException,
			UnableToComplyException {
		int dimensionality = neighborIDs.length;

		// parameters for apriori
		List<String> parameters = new ArrayList<String>();
		parameters.add(OptionHandler.OPTION_PREFIX + APRIORI.MINIMUM_SUPPORT_P);
		parameters.add(Integer.toString(minpts));
		APRIORI apriori = new APRIORI();
		apriori
				.setParameters(parameters
						.toArray(new String[parameters.size()]));

		// database for apriori
		Database<BitVector> apriori_db = new SequentialDatabase<BitVector>();
		for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
			Integer id = it.next();
			Bit[] bits = new Bit[dimensionality];
			boolean allFalse = true;
			for (int d = 0; d < dimensionality; d++) {
				if (neighborIDs[d].contains(id)) {
					bits[d] = new Bit(true);
					allFalse = false;
				} else {
					bits[d] = new Bit(false);
				}
			}
			if (!allFalse) {
				Map<AssociationID, Object> associations = database
						.getAssociations(id);
				if (associations == null) {
					associations = new Hashtable<AssociationID, Object>();
				}
				ObjectAndAssociations<BitVector> oaa = new ObjectAndAssociations<BitVector>(
						new BitVector(bits), associations);
				apriori_db.insert(oaa);
			}
		}
		apriori.run(apriori_db);

		// result of apriori
		AprioriResult aprioriResult = (AprioriResult) apriori.getResult();
		List<BitSet> frequentItemsets = aprioriResult.getSolution();
		Map<BitSet, Integer> supports = aprioriResult.getSupports();
		if (this.debug) {
			msg.append("\nFrequent itemsets: " + frequentItemsets);
			msg.append("\nAll supports: " + supports);
		}
		int maxSupport = 0;
		int maxCardinality = 0;
		BitSet preferenceVector = new BitSet();
		for (BitSet bitSet : frequentItemsets) {
			int cardinality = bitSet.cardinality();
			if ((maxCardinality < cardinality)
					|| (maxCardinality == cardinality && maxSupport == supports
							.get(bitSet))) {
				preferenceVector = bitSet;
				maxCardinality = cardinality;
				maxSupport = supports.get(bitSet);
			}
		}

		if (this.debug) {
			msg.append("\npreference ");
			msg.append(Util.format(dimensionality, preferenceVector));
			msg.append("\n");
			debugFine(msg.toString());
		}

		return preferenceVector;
	}

	/**
	 * Determines the preference vector with the max intersection strategy.
	 * 
	 * @param neighborIDs
	 *            the list of ids of the neighbors in each dimension
	 * @param msg
	 *            a string buffer for debug messages
	 * @return the preference vector
	 */
	private BitSet determinePreferenceVectorByMaxIntersection(
			Set<Integer>[] neighborIDs, StringBuffer msg) {
		int dimensionality = neighborIDs.length;
		BitSet preferenceVector = new BitSet(dimensionality);

		// noinspection unchecked
		Map<Integer, Set<Integer>> candidates = new HashMap<Integer, Set<Integer>>(
				dimensionality);
		for (int i = 0; i < dimensionality; i++) {
			Set<Integer> s_i = neighborIDs[i];
			if (s_i.size() > minpts) {
				candidates.put(i, s_i);
			}
		}
		if (this.debug) {
			msg.append("\ncandidates " + candidates.keySet());
		}

		if (!candidates.isEmpty()) {
			int i = max(candidates);
			Set<Integer> intersection = candidates.remove(i);
			preferenceVector.set(i);
			while (!candidates.isEmpty()) {
				Set<Integer> newIntersection = new HashSet<Integer>();
				i = maxIntersection(candidates, intersection, newIntersection);
				Set<Integer> s_i = candidates.remove(i);
				Util.intersection(intersection, s_i, newIntersection);
				intersection = newIntersection;

				if (intersection.size() < minpts) {
					break;
				} else {
					preferenceVector.set(i);
				}
			}
		}

		if (this.debug) {
			msg.append("\npreference ");
			msg.append(Util.format(dimensionality, preferenceVector));
			msg.append("\n");
			debugFiner(msg.toString());
		}

		return preferenceVector;
	}

	/**
	 * Returns the set with the maximum size contained in the specified map.
	 * 
	 * @param candidates
	 *            the map containing the sets
	 * @return the set with the maximum size
	 */
	private int max(Map<Integer, Set<Integer>> candidates) {
		Set<Integer> maxSet = null;
		Integer maxDim = null;
		for (Integer nextDim : candidates.keySet()) {
			Set<Integer> nextSet = candidates.get(nextDim);
			if (maxSet == null || maxSet.size() < nextSet.size()) {
				maxSet = nextSet;
				maxDim = nextDim;
			}
		}

		return maxDim;
	}

	/**
	 * Returns the index of the set having the maximum intersection set with the
	 * specified set contained in the specified map.
	 * 
	 * @param candidates
	 *            the map containing the sets
	 * @param set
	 *            the set to intersect with
	 * @param result
	 *            the set to put the result in
	 * @return the set with the maximum size
	 */
	private int maxIntersection(Map<Integer, Set<Integer>> candidates,
			Set<Integer> set, Set<Integer> result) {
		Integer maxDim = null;
		for (Integer nextDim : candidates.keySet()) {
			Set<Integer> nextSet = candidates.get(nextDim);
			Set<Integer> nextIntersection = new HashSet<Integer>();
			Util.intersection(set, nextSet, nextIntersection);
			if (result.size() < nextIntersection.size()) {
				result = nextIntersection;
				maxDim = nextDim;
			}
		}

		return maxDim;
	}

	/**
	 * Initializes the dimension selecting distancefunctions to determine the
	 * preference vectors.
	 * 
	 * @param database
	 *            the database storing the objects
	 * @param dimensionality
	 *            the dimensionality of the objects
	 * @param verbose
	 *            flag to allow verbose messages while performing the algorithm
	 * @param time
	 *            flag to request output of performance time
	 * @return the dimension selecting distancefunctions to determine the
	 *         preference vectors
	 * @throws ParameterException
	 */
	private DimensionSelectingDistanceFunction[] initDistanceFunctions(
			Database<RealVector> database, int dimensionality, boolean verbose,
			boolean time) throws ParameterException {
		DimensionSelectingDistanceFunction[] distanceFunctions = new DimensionSelectingDistanceFunction[dimensionality];
		for (int d = 0; d < dimensionality; d++) {
			String[] parameters = new String[2];
			parameters[0] = OptionHandler.OPTION_PREFIX
					+ DimensionSelectingDistanceFunction.DIM_P;
			parameters[1] = Integer.toString(d + 1);
			distanceFunctions[d] = new DimensionSelectingDistanceFunction();
			distanceFunctions[d].setParameters(parameters);
			distanceFunctions[d].setDatabase(database, verbose, time);
		}
		return distanceFunctions;
	}

	/**
	 * Returns the value of the epsilon parameter.
	 * 
	 * @return the value of the epsilon parameter
	 */
	public DoubleDistance getEpsilon() {
		return epsilon;
	}

	/**
	 * Returns minpts.
	 * 
	 * @return minpts
	 */
	public int getMinpts() {
		return minpts;
	}

}