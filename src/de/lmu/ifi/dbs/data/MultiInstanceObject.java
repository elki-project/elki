package de.lmu.ifi.dbs.data;

import java.util.ArrayList;
import java.util.List;

/**
 * MultiInstanceObject represents a collection of several MetricalObjects of an equal type.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class MultiInstanceObject<M extends MetricalObject<M>> implements MetricalObject<MultiInstanceObject>
{
    /**
     * Holds the members of this MultiInstanceObject.
     */
    private List<M> members;
    
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
    public MultiInstanceObject(List<M> members)
    {
        this.members = new ArrayList<M>(members.size());
        this.members.addAll(members);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.MetricalObject#getID()
     */
    public Integer getID()
    {
        return id;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.MetricalObject#setID(java.lang.Integer)
     */
    public void setID(Integer id)
    {
        this.id = id;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.MetricalObject#copy()
     */
    public MultiInstanceObject copy()
    {
        List<M> copyMembers = new ArrayList<M>(this.members.size());
        for(M member : this.members)
        {            
            copyMembers.add(member.copy());
        }
        return new MultiInstanceObject<M>(copyMembers);
    }

}
