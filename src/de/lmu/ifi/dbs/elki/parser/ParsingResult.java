package de.lmu.ifi.dbs.elki.parser;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.util.List;

/**
 * Provides a list of database objects and labels associated with these objects.
 * 
 * @author Elke Achtert 
 */
public class ParsingResult<O extends DatabaseObject>
{
    /**
     * The list of database objects and labels associated with these objects.
     */
    private final List<Pair<O,List<String>>> objectAndLabelList;

    /**
     * Provides a list of database objects and labels associated with these
     * objects.
     * 
     * @param objectAndLabelList
     *            the list of database objects and labels associated with these
     *            objects
     */
    public ParsingResult(List<Pair<O,List<String>>> objectAndLabelList)
    {
        this.objectAndLabelList = objectAndLabelList;
    }

    /**
     * Returns the list of database objects and labels associated with these
     * objects.
     * 
     * @return the list of database objects and labels associated with these
     *         objects
     */
    public List<Pair<O,List<String>>> getObjectAndLabelList()
    {
        return objectAndLabelList;
    }

    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object.
     */
    @Override
    public String toString()
    {
        return objectAndLabelList.toString();
    }
}
