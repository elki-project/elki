package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalFractalDimensionCluster;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.Iterator;
import java.util.List;

/**
 * @author Arthur Zimek
 */
public class FracClusPreprocessor<V extends RealVector<V,?>> extends AbstractParameterizable implements Preprocessor<V>
{
    public static final String NUMBER_OF_SUPPORTERS_P = "supporters";
    
    public static final String NUMBER_OF_SUPPORTERS_D = "number of supporters (at least 2)";
    
    private int k;
    
    private IntParameter kParameter = new IntParameter(NUMBER_OF_SUPPORTERS_P,NUMBER_OF_SUPPORTERS_D,new GreaterEqualConstraint(2));
    
    public FracClusPreprocessor()
    {
        super();        
        optionHandler.put(kParameter);
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.preprocessing.Preprocessor#run(de.lmu.ifi.dbs.database.Database, boolean, boolean)
     */
    public void run(Database<V> database, boolean verbose, boolean time)
    {
        if(verbose)
        {
            verbose("assigning database objects to base clusters");
        }
        for(Iterator<Integer> iter = database.iterator(); iter.hasNext();)
        {
            Integer id = iter.next();
            HierarchicalFractalDimensionCluster<V> point = new HierarchicalFractalDimensionCluster<V>(id, database, k);
            point.setLevel(0);
            point.setLabel("Level="+0+"_ID="+id+"_"+point.getLabel());
            database.associate(AssociationID.FRACTAL_DIMENSION_CLUSTER, id, point);
        }
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);
        k = optionHandler.getParameterValue(kParameter);
        return remainingParameters;
    }
    
    @Override
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        AttributeSettings myAttributeSettings = new AttributeSettings(this);
        myAttributeSettings.addSetting(NUMBER_OF_SUPPORTERS_P, Integer.toString(k));
        attributeSettings.add(myAttributeSettings);
        return attributeSettings;
    }
    
    public int getK()
    {
        return this.k;
    }

}
