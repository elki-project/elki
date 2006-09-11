package de.lmu.ifi.dbs.evaluation.holdout;

import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * A holdout providing a seed for randomized operations.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class RandomizedHoldout<O extends DatabaseObject> extends AbstractHoldout<O>
{
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
    public static final String SEED_D = "seed for randomized holdout (>0) - default: "
            + SEED_DEFAULT;

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
    public RandomizedHoldout()
    {
    	super();
//        optionHandler.put(SEED_P, new Parameter(SEED_P,SEED_D,Parameter.Types.INT));
        optionHandler.put(SEED_P, new IntParameter(SEED_P,SEED_D,new GreaterConstraint(0)));
    }

    /**
     * Sets the parameter seed.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);
        if (optionHandler.isSet(SEED_P))
        {
            String seedString = optionHandler.getOptionValue(SEED_P);
            try
            {
                seed = Long.parseLong(seedString);
                if (seed <= 0)
                {
                    throw new WrongParameterValueException(SEED_P, seedString,
                            SEED_D);
                }
            } catch (NumberFormatException e)
            {
                throw new WrongParameterValueException(SEED_P, seedString,
                        SEED_D, e);
            }
        }
        random = new Random(seed);
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> attributeSettings = super
                .getAttributeSettings();

        AttributeSettings mySettings = attributeSettings.get(0);
        mySettings.addSetting(SEED_P, Long.toString(seed));

        return attributeSettings;
    }

}
