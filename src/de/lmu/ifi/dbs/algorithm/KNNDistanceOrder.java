package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.KNNDistanceOrderResult;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Provides an order of the kNN-distances for all objects within the database.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class KNNDistanceOrder<O extends MetricalObject> extends DistanceBasedAlgorithm<O>
{
    /**
     * Parameter k.
     */
    public static final String K_P = "k";
    
    /**
     * Default value for k.
     */
    public static final int DEFAULT_K = 1;
    
    /**
     * Description for parameter k.
     */
    public static final String K_D = "<int>the distance of the k-distant object is assessed. k >= 1 (default: "+DEFAULT_K+")";
        
    /**
     * Holds the parameter k.
     */
    private int k = DEFAULT_K;
    
    /**
     * Holds the result.
     */
    private KNNDistanceOrderResult result;
    
    /**
     * Provides an algorithm to order the kNN-distances for all objects of the database.
     * 
     */
    public KNNDistanceOrder()
    {
        super();
        parameterToDescription.put(K_P+OptionHandler.EXPECTS_VALUE,K_D);
        optionHandler = new OptionHandler(parameterToDescription,KNNDistanceOrder.class.getName());
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
     */
    @Override
    public void runInTime(Database<O> database) throws IllegalStateException
    {
        List<Distance> knnDistances = new ArrayList<Distance>(database.size());
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            knnDistances.add(((List<QueryResult>) database.kNNQueryForID(iter.next(), k, this.getDistanceFunction())).get(k-1).getDistance());
        }
        Collections.sort(knnDistances);
        result = new KNNDistanceOrderResult(database,knnDistances);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Result<O> getResult()
    {
        return result;
    }

    /**
     * Adds the value of k to the attribute settings as provided
     * by the super class.
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getAttributeSettings()
     */    
    @Override
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> result = super.getAttributeSettings();

        AttributeSettings attributeSettings = new AttributeSettings(this);
        attributeSettings.addSetting(K_P,Integer.toString(k));

        result.add(attributeSettings);
        return result;
    }

    /**
     * Sets the parameter value for parameter k, if specified,
     * additionally to the parameter settings of super classes.
     * Otherwise the default value for k is used.
     * 
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
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
                    throw new NumberFormatException("Parameter k must not be smaller than 1.");
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
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description(KNNDistanceOrder.class.getName(), "KNN-Distance-Order", "Assesses the knn distances for a specified k and orders them.", "");
    }

}
