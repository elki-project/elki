package de.lmu.ifi.dbs.index;

/**
 * Represents a component in a tree path.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class TreePathComponent
{
    /**
     * The identifier of the component.
     */
    private Identifier identifier;

    /**
     * The index of the component in its parent.
     */
    private Integer index;

    /**
     * Creates a new TreePathComponent.
     * 
     * @param identifier
     *            the identifier of the component
     * @param index
     *            index of the component in its parent
     */
    public TreePathComponent(Identifier identifier, Integer index)
    {
        this.identifier = identifier;
        this.index = index;
    }

    /**
     * Returns the identifier of the component.
     * 
     * @return the identifier of the component
     */
    public Identifier getIdentifier()
    {
        return identifier;
    }

    /**
     * Returns the index of the component in its parent.
     * 
     * @return the index of the component in its parent
     */
    public Integer getIndex()
    {
        return index;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * 
     * @param o
     *            the reference object with which to compare
     * @return <code>true</code> if the identifier of this component equals
     *         the identifier of the o argument; <code>false</code> otherwise.
     */
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final TreePathComponent that = (TreePathComponent) o;
        return (identifier.equals(that.identifier));
    }

    /**
     * Returns a hash code for this object.
     * 
     * @return a hash code for this object
     */
    public int hashCode()
    {
        return identifier.hashCode();
    }
}
