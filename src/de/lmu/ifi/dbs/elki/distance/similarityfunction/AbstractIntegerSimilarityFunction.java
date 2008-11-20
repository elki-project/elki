package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.IntegerDistance;

import java.util.regex.Pattern;

/**
 * @author Arthur Zimek
 */
public abstract class AbstractIntegerSimilarityFunction<O extends DatabaseObject> extends AbstractSimilarityFunction<O, IntegerDistance>
{
    protected AbstractIntegerSimilarityFunction()
    {
        super(Pattern.compile("\\d+"));
    }


    public IntegerDistance infiniteDistance()
    {
        return new IntegerDistance(Integer.MAX_VALUE);
    }

    public boolean isInfiniteDistance(IntegerDistance distance)
    {
        return distance.equals(new IntegerDistance(Integer.MAX_VALUE));
    }

    public boolean isNullDistance(IntegerDistance distance)
    {
        return distance.equals(new IntegerDistance(0));
    }

    public boolean isUndefinedDistance(@SuppressWarnings("unused") IntegerDistance distance)
    {
        throw new UnsupportedOperationException("Undefinded distance not supported!");
    }

    public IntegerDistance nullDistance()
    {
        return new IntegerDistance(0);
    }

    public IntegerDistance undefinedDistance()
    {
        throw new UnsupportedOperationException("Undefinded distance not supported!");
    }

    public IntegerDistance valueOf(String pattern) throws IllegalArgumentException
    {
        if(matches(pattern))
        {
            return new IntegerDistance(Integer.parseInt(pattern));
        }
        else
        {
            throw new IllegalArgumentException("Given pattern \"" + pattern + "\" does not match required pattern \"" + requiredInputPattern() + "\"");
        }
    }

}
