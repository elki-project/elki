package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.KNNDistanceOrderResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Provides an order of the kNN-distances for all objects within the database.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KNNDistanceOrder<O extends MetricalObject, D extends Distance<D>> extends DistanceBasedAlgorithm<O, D>
{
    /**
     * Parameter k.
     */
    public static final String K_P = "minpts";

    /**
     * Default value for k.
     */
    public static final int DEFAULT_K = 1;

    /**
     * Description for parameter k.
     */
    public static final String K_D = "<int>the distance of the k-distant object is assessed. k >= 1 (default: " + DEFAULT_K + ")";

    /**
     * Parameter percentage.
     */
    public static final String PERCENTAGE_P = "percent";

    /**
     * Default value for percentage.
     */
    public static final double DEFAULT_PERCENTAGE = 1;

    /**
     * Description for parameter percentage.
     */
    public static final String PERCENTAGE_D = "<double>average percentage p, 0 < p <= 1, of distances randomly choosen to be provided in the result (default: " + DEFAULT_PERCENTAGE + ")";

    /**
     * Holds the parameter k.
     */
    private int k = DEFAULT_K;
    
    /**
     * Holds the parameter percentage.
     */
    private double percentage = DEFAULT_PERCENTAGE;

    /**
     * Holds the result.
     */
    private KNNDistanceOrderResult<O, D> result;

    /**
     * Provides an algorithm to order the kNN-distances for all objects of the
     * database.
     */
    public KNNDistanceOrder()
    {
        super();
        parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
        parameterToDescription.put(PERCENTAGE_P+OptionHandler.EXPECTS_VALUE, PERCENTAGE_D);
        optionHandler = new OptionHandler(parameterToDescription, KNNDistanceOrder.class.getName());
    }

    /**
     * @see AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
     */
    protected @Override void runInTime(Database<O> database) throws IllegalStateException
    {
        Random random = new Random();
        List<D> knnDistances = new ArrayList<D>();
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            Integer id = iter.next();
            if(random.nextDouble() < percentage)
            {
                knnDistances.add((database.kNNQueryForID(id, k, this.getDistanceFunction())).get(k - 1).getDistance());
            }
        }
        Collections.sort(knnDistances, Collections.reverseOrder());
        result = new KNNDistanceOrderResult<O, D>(database, knnDistances);
    }

    /**
     * @see Algorithm#getResult()
     */
    public Result<O> getResult()
    {
        return result;
    }

    /**
     * Adds the value of k to the attribute settings as provided by the super
     * class.
     * 
     * @see Algorithm#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> result = super.getAttributeSettings();

        AttributeSettings attributeSettings = new AttributeSettings(this);
        attributeSettings.addSetting(K_P, Integer.toString(k));
        attributeSettings.addSetting(PERCENTAGE_P, Double.toString(percentage));

        result.add(attributeSettings);
        return result;
    }

    /**
     * Sets the parameter value for parameter k, if specified, additionally to
     * the parameter settings of super classes. Otherwise the default value for
     * k is used.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        if(optionHandler.isSet(K_P))
        {
            try
            {
                k = Integer.parseInt(optionHandler.getOptionValue(K_P));
                if(k < 1)
                {
                    throw new NumberFormatException("Parameter "+K_P+" must not be smaller than 1. Found: "+k+".");
                }
            }
            catch(NumberFormatException e)
            {
                throw new IllegalArgumentException(e);
            }
        }
        if(optionHandler.isSet(PERCENTAGE_P))
        {
            try
            {
                percentage = Double.parseDouble(optionHandler.getOptionValue(PERCENTAGE_P));
                if(percentage <= 0 || percentage > 1)
                {
                    throw new NumberFormatException("Parameter "+PERCENTAGE_P+" is expected to be a number p, 0 < p <= 1. Found: "+percentage+".");
                }
            }
            catch(NumberFormatException e)
            {
                throw new IllegalArgumentException(e);
            }
        }
        return remainingParameters;
    }

    /**
     * @see Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description(KNNDistanceOrder.class.getName(), "KNN-Distance-Order", "Assesses the knn distances for a specified k and orders them.", "");
    }

}
