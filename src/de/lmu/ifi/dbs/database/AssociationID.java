package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.ClassLabel;
import de.lmu.ifi.dbs.pca.CorrelationPCA;
import de.lmu.ifi.dbs.pca.LinearCorrelationPCA;
import de.lmu.ifi.dbs.utilities.ConstantObject;

import java.util.List;

/**
 * An AssociationID is used by databases as a unique identifier
 * for specific associations to single objects.
 * Such as label, local similarity measure.
 * There is no association possible without a specific
 * AssociationID defined within this class.
 * 
 * An AssociationID provides also information concerning the class
 * of the associated objects.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class AssociationID extends ConstantObject
{
    /**
     * The standard association id to associate a label to an object. 
     */
    public static final AssociationID LABEL = new AssociationID("associationIDLabel",String.class);

    /**
     * The association id to associate a class to an object.
     */
    public static final AssociationID CLASS = new AssociationID("associationIDClass",ClassLabel.class);
    
    /**
     * The association id to associate a correlation pca to an object.
     */
    public static final AssociationID PCA = new AssociationID("associationIDPCA",CorrelationPCA.class);

    /**
     * The association id to associate a Linear CorrelationPCA for the 4C algorithm.
     */
    public static final AssociationID FOUR_C_PCA = new AssociationID("associationID_4C_PCA",LinearCorrelationPCA.class);

    /**
     * The association id to associate the neighbors of an object.
     */
    public static final AssociationID NEIGHBORS = new AssociationID("associationIDNeighbors",List.class);

    /**
     * The association id to associate the LRD of an object for the LOF algorithm.
     */
    public static final AssociationID LRD = new AssociationID("associationIDLRD",Double.class);

    /**
     * The serial version UID. 
     */
    private static final long serialVersionUID = 8115554038339292192L;


    /**
     * The Class type related to this AssociationID.
     */
    private Class type;

    /**
     * Provides a new AssociationID of the given name and type.
     * 
     * All AssociationIDs are unique w.r.t. their name.
     * An AssociationID provides information of which class
     * the associated objects are.
     * 
     * @param name name of the association
     * @param type class of the objects that are associated under this AssociationID
     */
    private AssociationID(final String name, final Class type)
    {
        super(name);
        try
        {
            this.type = Class.forName(type.getName());
        }
        catch(ClassNotFoundException e)
        {
            throw new IllegalArgumentException("Invalid class name \""+type.getName()+"\" for property \""+name+"\".");
        }
    }

    /**
     * Returns the type of the AssociationID.
     * 
     * 
     * @return the type of the AssociationID
     */
    public Class getType()
    {
        try
        {
            return Class.forName(type.getName());
        }
        catch(ClassNotFoundException e)
        {
            throw new IllegalStateException("Invalid class name \""+type.getName()+"\" for property \""+this.getName()+"\".");
        }
    }

    /**
     * Returns the AssociationID for the given name if it exists,
     * null otherwise.
     * 
     * @param name the name of the desired AssociationID
     * @return the AssociationID for the given name if it exists,
     * null otherwise
     */
    public AssociationID getAssociationID(final String name)
    {
        return (AssociationID) AssociationID.lookup(AssociationID.class,name);
    }

}
