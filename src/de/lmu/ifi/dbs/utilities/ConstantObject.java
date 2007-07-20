package de.lmu.ifi.dbs.utilities;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.logging.AbstractLoggable;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

/**
 * ConstantObject provides a parent class for constant objects, that are
 * immutable and unique by class and name.
 * 
 * @author Arthur Zimek 
 */
public abstract class ConstantObject extends AbstractLoggable implements Comparable<ConstantObject>, Serializable
{
    /**
     * Index of constant objects.
     */
    private static final Map<Class<?>, Map<String, ConstantObject>> CONSTANT_OBJECTS_INDEX = new HashMap<Class<?>, Map<String, ConstantObject>>();

    /**
     * Holds the value of the property's name.
     */
    private final String name;

    /**
     * The cached hash code of this object.
     */
    private final int hashCode;

    /**
     * Provides a ConstantObject of the given name.
     * 
     * @param name
     *            name of the ConstantObject
     */
    protected ConstantObject(final String name)
    {
    	super(LoggingConfiguration.DEBUG);
        if(name == null)
        {
            throw new IllegalArgumentException("The name of a constant object must not be null.");
        }
        Map<String, ConstantObject> index = CONSTANT_OBJECTS_INDEX.get(this.getClass());
        if(index == null)
        {
            index = new HashMap<String, ConstantObject>();
            CONSTANT_OBJECTS_INDEX.put(this.getClass(), index);
        }
        if(index.containsKey(name))
        {
            throw new IllegalArgumentException("A constant object of type \"" + this.getClass().getName() + "\" with value \"" + name + "\" is existing already.");
        }
        this.name = new String(name);
        index.put(name, this);
        this.hashCode = name.hashCode();
    }

    /**
     * Returns the name of the ConstantObject.
     * 
     * @return the name of hte ConstantObject
     */
    public String getName()
    {
        return new String(name);
    }

    /**
     * Provides a ConstantObject of specified class and name if it exists.
     * 
     * @param type
     *            the type of the desired ConstantObject
     * @param name
     *            the name of the desired ConstantObject
     * @return the ConstantObject of designated type and name if it exists, null
     *         otherwise
     */
    public static final ConstantObject lookup(final Class<?> type, final String name)
    {
        return CONSTANT_OBJECTS_INDEX.get(type).get(name);
    }

    /**
     * Method for use by the serialization mechanism to ensure identity of
     * ConstantObjects.
     * 
     * @return the ConstantObject that already exists in the virtual machine
     *         rather than a new instance as created by the serialization
     *         mechanism
     * @throws ObjectStreamException
     */
    protected Object readResolve() throws ObjectStreamException
    {
        Object result = lookup(getClass(), getName());
        if(result == null)
        {
            throw new NullPointerException("No constant object of type \"" + getClass().getName() + "\" found for name \"" + getName() + "\".");
        }
        return result;
    }

    /**
     * @see Object#equals(Object)
     */
    public boolean equals(Object o)
    {
        if(this == o)
            return true;
        if(o == null || getClass() != o.getClass())
            return false;

        final ConstantObject that = (ConstantObject) o;

        if(hashCode != that.hashCode)
            return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        return hashCode;
    }

    /**
     * Two constant objects are generally compared by their name.
     * The result reflects the lexicographical order of the names
     * by {@link String#compareTo(String) this.getName().compareTo(o.getName()}.
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ConstantObject o)
    {
        return this.getName().compareTo(o.getName());
    }
    
    
}
