package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.AbstractMeasurementFunction;
import de.lmu.ifi.dbs.elki.distance.Distance;

import java.util.regex.Pattern;

/**
 * @author Arthur Zimek
 */
public abstract class AbstractSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractMeasurementFunction<O, D> implements SimilarityFunction<O, D>
{
    
    protected AbstractSimilarityFunction(Pattern pattern)
    {
        super(pattern);
    }
    
    public D similarity(Integer id1, O o2)
    {
        return similarity(id1, o2.getID());
    }

    public D similarity(O o1, O o2)
    {
        return similarity(o1.getID(),o2.getID());
    }
}
