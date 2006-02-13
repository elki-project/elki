package de.lmu.ifi.dbs.data;

import java.util.ArrayList;
import java.util.List;

/**
 * MultiInstanceObject represents a collection of several MetricalObjects of an equal type.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class MultiInstanceObject<O extends DatabaseObject<O>> implements DatabaseObject<MultiInstanceObject>
{
    /**
     * Holds the members of this MultiInstanceObject.
     */
    private List<O> members;
    
    /**
     * Holds the id of the Object.
     */
    private Integer id;
    
    /**
     * Provides a MultiInstanceObject comprising the specified members.
     * 
     * @param members a list of members - the references of the members
     * are kept as given, but in a new list
     */
    public MultiInstanceObject(List<O> members)
    {
        this.members = new ArrayList<O>(members.size());
        this.members.addAll(members);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.DatabaseObject#getID()
     */
    public Integer getID()
    {
        return id;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.DatabaseObject#setID(java.lang.Integer)
     */
    public void setID(Integer id)
    {
        this.id = id;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.DatabaseObject#copy()
     */
    public MultiInstanceObject copy()
    {
        List<O> copyMembers = new ArrayList<O>(this.members.size());
        for(O member : this.members)
        {            
            copyMembers.add(member.copy());
        }
        return new MultiInstanceObject<O>(copyMembers);
    }

}
