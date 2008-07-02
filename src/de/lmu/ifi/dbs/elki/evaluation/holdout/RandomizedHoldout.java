package de.lmu.ifi.dbs.elki.evaluation.holdout;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.Random;

/**
 * A holdout providing a seed for randomized operations.
 * 
 * @author Arthur Zimek 
 */
public abstract class RandomizedHoldout<O extends DatabaseObject,L extends ClassLabel<L>> extends AbstractHoldout<O,L> {
	/**
	 * The parameter seed.
	 */
	public static final String SEED_P = "seed";

	/**
	 * Default seed.
	 */
	public static final long SEED_DEFAULT = 1;

	/**
	 * Desription of parameter seed.
	 */
	public static final String SEED_D = "seed for randomized holdout (>0) - default: " + SEED_DEFAULT;

	/**
	 * Holds the seed for randomized operations.
	 */
	protected long seed = SEED_DEFAULT;

	/**
	 * The random generator.
	 */
	protected Random random;

	/**
	 * Sets the parameter seed to the parameterToDescription map.
	 */
	public RandomizedHoldout() {
		super();

		LongParameter seed = new LongParameter(SEED_P, SEED_D, new GreaterConstraint(0));
		seed.setDefaultValue(SEED_DEFAULT);
		optionHandler.put(seed);
	}

	/**
	 * Sets the parameter seed.
	 * 
	 * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
	 */
	public String[] setParameters(String[] args) throws ParameterException {
		String[] remainingParameters = super.setParameters(args);

		if (optionHandler.isSet(SEED_P)) {
			seed = (Long) optionHandler.getOptionValue(SEED_P);
		}
		random = new Random(seed);

		return remainingParameters;
	}
}
