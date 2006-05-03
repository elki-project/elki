package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class HiSCPreprocessor implements Preprocessor
{
    private double alpha;

    /**
     * 
     * @see de.lmu.ifi.dbs.preprocessing.Preprocessor#run(de.lmu.ifi.dbs.database.Database, boolean, boolean)
     */
    public void run(Database<RealVector> database, boolean verbose, boolean time)
    {
        // TODO Auto-generated method stub

    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws ParameterException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
     */
    public String[] getParameters()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public double getAlpha()
    {
        return alpha;
    }

}
