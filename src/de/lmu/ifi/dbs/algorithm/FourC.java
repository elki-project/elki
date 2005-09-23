package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.CorrelationDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.CorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.preprocessing.RangeQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourC extends DBSCAN<DoubleVector>
{
    public static final String LAMBDA_P = "lambda";
    
    public static final String LAMBDA_D = "<lambda>(integer) intrinsinc dimensionality of clusters to be found.";
    
    private int lambda;
    
    

    /**
     * 
     */
    public FourC()
    {
        super();
        parameterToDescription.put(LAMBDA_P+OptionHandler.EXPECTS_VALUE,LAMBDA_D);
        optionHandler = new OptionHandler(parameterToDescription, FourC.class.getName());
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    public void runInTime(Database<DoubleVector> database) throws IllegalStateException
    {
        // preprocessing
        if (isVerbose()) {
          System.out.println("\ndb size = " + database.size());
          System.out.println("dimensionality = " + database.dimensionality());
          System.out.println("\npreprocessing... ");
        }
        RangeQueryBasedCorrelationDimensionPreprocessor preprocessor = new RangeQueryBasedCorrelationDimensionPreprocessor();
        // TODO: set parameter big and small...
        String[] preprocessorParams = {};
        preprocessor.setParameters(preprocessorParams);
        preprocessor.run(database, isVerbose());

        super.runInTime(database);
        
    }


    // TODO: expandCluster

    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        try
        {
            lambda = Integer.parseInt(optionHandler.getOptionValue(LAMBDA_P));
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(UnusedParameterException e)
        {
            throw new IllegalArgumentException(e);
        }
        if(!(getDistanceFunction() instanceof CorrelationDistanceFunction))
        {
            throw new IllegalArgumentException();
        }
        return remainingParameters;
    }

    @Override
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> result = new ArrayList<AttributeSettings>();

        AttributeSettings attributeSettings = new AttributeSettings(this);
        attributeSettings.addSetting(LAMBDA_P, Integer.toString(lambda));
        

        result.add(attributeSettings);
        result.addAll(super.getAttributeSettings());
        return result;


    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("4C","Computing Correlation Connected Clusters","4C identifies local subgroups of data objects sharing a uniform correlation. The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).","Christian B�hm, Karin Kailing, Peer Kr�ger, Arthur Zimek: Computing Clusters of Correlation Connected Objects, Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004, 455-466");
    }

}
