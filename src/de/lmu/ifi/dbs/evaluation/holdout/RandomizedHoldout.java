package de.lmu.ifi.dbs.evaluation.holdout;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.List;
import java.util.Random;

/**
 * A holdout providing a seed for randomized operations.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
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
    public static final String SEED_D = "<int>seed for randomized holdout - default: "+SEED_DEFAULT;
    
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
        parameterToDescription.put(SEED_P+OptionHandler.EXPECTS_VALUE,SEED_D);
    }

    /**
     * Sets the parameter seed.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        if(optionHandler.isSet(SEED_P))
        {
            try
            {
                seed = Long.parseLong(optionHandler.getOptionValue(SEED_P));
            }
            catch(NumberFormatException e)
            {
                throw new IllegalArgumentException("Parameter "+SEED_P+" requires an integer. Found: "+optionHandler.getOptionValue(SEED_P),e);
            }
        }
        random = new Random(seed);
        return remainingParameters;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> settings = super.getAttributeSettings();
        AttributeSettings attributeSettings = settings.get(0);
        attributeSettings.addSetting(SEED_P,Long.toString(seed));
        settings.add(attributeSettings);
        return settings;
    }

}
