package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.util.List;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CoDeC<D extends DoubleVector> extends AbstractAlgorithm<D>
{

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.database.Database)
     */
    @Override
    protected void runInTime(Database<D> database) throws IllegalStateException
    {
        // TODO Auto-generated method stub

    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Result<D> getResult()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String description()
    {
        // TODO Auto-generated method stub
        return super.description();
    }

    @Override
    public List<AttributeSettings> getAttributeSettings()
    {
        // TODO Auto-generated method stub
        return super.getAttributeSettings();
    }

    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        // TODO Auto-generated method stub
        return super.setParameters(args);
    }

}
