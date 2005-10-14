package de.lmu.ifi.dbs.utilities;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * ConstantObject provides a parent class for constant objects, that are immutable and unique by class and name.
 *  
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class ConstantObject implements Serializable
{
    /**
     * Index of constant objects.
     */
    private static final Map<Class,Map<String,ConstantObject>> CONSTANT_OBJECTS_INDEX = new HashMap<Class,Map<String,ConstantObject>>();
    
    /**
     * Holds the value of the property's name.
     */
    private final String name;
    
    /**
     * Provides a ConstantObject of the given name.
     * 
     * @param name name of the ConstantObject
     */
    protected ConstantObject(final String name)
    {
        if(name == null)
        {
            throw new IllegalArgumentException("The name of a constant object must not be null.");
        }
        Map<String,ConstantObject> index = CONSTANT_OBJECTS_INDEX.get(this.getClass());
        if(index == null)
        {
            index = new HashMap<String,ConstantObject>();
            CONSTANT_OBJECTS_INDEX.put(this.getClass(),index);
        }
        if(index.containsKey(name))
        {
            throw new IllegalArgumentException("A constant object of type \""+this.getClass().getName()+"\" with value \""+name+"\" is existing already.");
        }
        this.name = new String(name);
        index.put(name,this);
    }
    
    /**
     * Returns the name of the ConstantObject.
     * 
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
     * 
     * @param type the type of the desired ConstantObject
     * @param name the name of the desired ConstantObject
     * @return the ConstantObject of designated type and name if it exists, null otherwise
     */
    public static final ConstantObject lookup(final Class type, final String name)
    {
        return CONSTANT_OBJECTS_INDEX.get(type).get(name);
    }
    
    /**
     * Method for use by the serialization mechanism
     * to ensure identity of ConstantObjects.
     * 
     * 
     * @return the ConstantObject that already exists in the virtual machine
     * rather than a new instance as created by the serialization mechanism
     * 
     * @throws ObjectStreamException
     */
    protected Object readResolve() throws ObjectStreamException
    {
        Object result = lookup(getClass(),getName());
        if(result == null)
        {
            throw new NullPointerException("No constant object of type \""+getClass().getName()+"\" found for name \""+getName()+"\".");
        }
        return result;
    }
}
