package de.lmu.ifi.dbs.evaluation.holdout;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractHoldout<M extends MetricalObject> implements Holdout<M>
{

    /**
     * The association id for the class label.
     */
    public static final AssociationID CLASS = AssociationID.CLASS;
    
    protected ClassLabel[] labels;

    /**
     * The parameterToDescription map.
     */
    protected Map<String,String> parameterToDescription = new HashMap<String,String>();

    protected OptionHandler optionHandler;

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        return optionHandler.grabOptions(args);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        return new ArrayList<AttributeSettings>();

    }
    
    /**
     * Checks whether the database has classes annotated and collects the available classes.
     * 
     * @param database the database to collect classes from
     * @return sorted array of ClassLabels available in the specified database 
     */
    public void setClassLabels(Database<M> database)
    {
        this.labels = Util.getClassLabels(database).toArray(new ClassLabel[0]);
        Arrays.sort(this.labels);
    }


}
