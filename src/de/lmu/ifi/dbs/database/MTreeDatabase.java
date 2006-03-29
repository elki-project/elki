package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.index.metrical.MetricalIndex;
import de.lmu.ifi.dbs.index.metrical.mtree.MTree;

import java.util.List;

/**
 * MTreeDatabase is a database implementation which is supported by a MTree
 * index structure.
 * 
 * @author Elke Achtert(<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MTreeDatabase<O extends DatabaseObject, D extends Distance<D>>
        extends MetricalIndexDatabase<O, D>
{
    /**
     * Empty constructor, creates a new MTreeDatabase.
     */
    public MTreeDatabase()
    {
        super();
    }

    /**
     * Creates a metrical index object for this database.
     */
    public MetricalIndex<O, D> createMetricalIndex()
    {
        return new MTree<O, D>(fileName, pageSize, cacheSize,
                getDistanceFunction());
    }

    /**
     * Creates a metrical index object for this database.
     * 
     * @param objects
     *            the objects to be indexed
     */
    public MetricalIndex<O, D> createMetricalIndex(List<O> objects)
    {
        return new MTree<O, D>(fileName, pageSize, cacheSize,
                getDistanceFunction(), objects);
    }

    /**
     * @see Database#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(MTreeDatabase.class.getName());
        description
                .append(" holds all the data in an MTree index structure.\n");
        description.append(optionHandler.usage("", false));
        return description.toString();
    }
}
