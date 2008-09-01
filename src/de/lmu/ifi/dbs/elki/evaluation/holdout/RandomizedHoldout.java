package de.lmu.ifi.dbs.elki.evaluation.holdout;

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * A holdout providing a seed for randomized operations.
 *
 * @author Arthur Zimek
 */
public abstract class RandomizedHoldout<O extends DatabaseObject, L extends ClassLabel> extends AbstractHoldout<O, L> {
    /**
     * Default seed.
     */
    public static final long SEED_DEFAULT = 1;

    /**
     * OptionID for {@link #SEED_PARAM}
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID(
        "seed", "seed for randomized holdout (>0)");

    /**
     * Parameter for number of folds.
     */
    private final LongParameter SEED_PARAM = new LongParameter(SEED_ID, new GreaterConstraint(0.0), SEED_DEFAULT);
    

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
        addOption(SEED_PARAM);
    }

    /**
     * Sets the parameter seed.
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        if (SEED_PARAM.isSet()) {
            seed = SEED_PARAM.getValue();
        }
        random = new Random(seed);

        return remainingParameters;
	}
}
