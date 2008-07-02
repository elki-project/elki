package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.IntegerDistance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.SharedNearestNeighborsPreprocessor;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.regex.Pattern;

/**
 * @author Arthur Zimek
 */
public class SharedNearestNeighborSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractPreprocessorBasedSimilarityFunction<O, SharedNearestNeighborsPreprocessor<O,D>, IntegerDistance>
{
    /**
     * The Assocoiation ID for the association to be set by the preprocessor.
     */
    @SuppressWarnings("unchecked")
    public static final AssociationID<SortedSet> ASSOCIATION_ID = AssociationID.SHARED_NEAREST_NEIGHBORS_SET;

    /**
     * The super class for the preprocessor.
     */
    @SuppressWarnings("unchecked")
    public static final Class<SharedNearestNeighborsPreprocessor> PREPROCESSOR_SUPER_CLASS = SharedNearestNeighborsPreprocessor.class;

    /**
     * The default preprocessor class name.
     */
    public static final String DEFAULT_PREPROCESSOR_CLASS = SharedNearestNeighborsPreprocessor.class.getName();

    /**
     * Description for parameter preprocessor.
     */
    public static final String PREPROCESSOR_CLASS_D = "the preprocessor to determine the neighbors of the objects";
    
    //private SharedNearestNeighborsPreprocessor<O,D> preprocessor = new SharedNearestNeighborsPreprocessor<O,D>();
    
    
    public SharedNearestNeighborSimilarityFunction()
    {
        super(Pattern.compile("\\d+"));
    }

    public IntegerDistance similarity(Integer id1, Integer id2)
    {
        SortedSet<Integer> neighbors1 = (SortedSet<Integer>) getDatabase().getAssociation(getAssociationID(), id1);
        SortedSet<Integer> neighbors2 = (SortedSet<Integer>) getDatabase().getAssociation(getAssociationID(), id2);
        int intersection = 0;
        Iterator<Integer> iter1 = neighbors1.iterator();
        Iterator<Integer> iter2 = neighbors2.iterator();
        Integer neighbors1ID = null;
        Integer neighbors2ID = null;
        if(iter1.hasNext())
        {
            neighbors1ID = iter1.next();
        }
        if(iter2.hasNext())
        {
            neighbors2ID = iter2.next();
        }
        while((iter1.hasNext() || iter2.hasNext()) && neighbors1ID != null && neighbors2ID != null)
        {
            if(neighbors1ID.equals(neighbors2ID))
            {
                intersection++;
                if(iter1.hasNext())
                {
                    neighbors1ID = iter1.next();
                }
                else
                {
                    neighbors1ID = null;
                }
                if(iter2.hasNext())
                {
                    neighbors2ID = iter2.next();
                }
                else
                {
                    neighbors2ID = null;
                }
            }
            else if(neighbors1ID < neighbors2ID)
            {
                if(iter1.hasNext())
                {
                    neighbors1ID = iter1.next();
                }
                else
                {
                    neighbors1ID = null;
                }
            }
            else // neighbors1ID > neighbors2ID
            {
                if(iter2.hasNext())
                {
                    neighbors2ID = iter2.next();
                }
                else
                {
                    neighbors2ID = null;
                }
            }
        }
        return new IntegerDistance(intersection);
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

    public boolean isUndefinedDistance(IntegerDistance distance)
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

    @Override
    AssociationID<SortedSet> getAssociationID()
    {
        return ASSOCIATION_ID;
    }

    @Override
    String getDefaultPreprocessorClassName()
    {
        return DEFAULT_PREPROCESSOR_CLASS;
    }

    @Override
    String getPreprocessorClassDescription()
    {
        return PREPROCESSOR_CLASS_D;
    }

    @Override
    Class<? extends Preprocessor> getPreprocessorSuperClassName()
    {
        return PREPROCESSOR_SUPER_CLASS;
    }




}
