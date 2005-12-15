package de.lmu.ifi.dbs.data;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * RealVector is an abstract implementation of FeatureVector. Provided is an
 * attribute separator (space), and the ID-methods as required for a
 * MetricalObject.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class RealVector<T extends Number> implements FeatureVector<T>
{

    /**
     * The String to separate attribute values in a String that represents the
     * values.
     */
    public final static String ATTRIBUTE_SEPARATOR = " ";

    /**
     * The unique id of this object.
     */
    private Integer id;

    /**
     * Returns the unique id of this RealVector object.
     * 
     * @return the unique id of this RealVector object
     * @see MetricalObject#getID()
     */
    public Integer getID()
    {
        return id;
    }

    /**
     * Sets the id of this RealVector object. The id must be unique within one
     * database.
     * 
     * @param id
     *            the id of the object
     * @see MetricalObject#setID(Integer)
     */
    public void setID(Integer id)
    {
        this.id = id;
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.data.FeatureVector#newInstance(null[])
     */
    public FeatureVector<T> newInstance(T[] values) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        Class[] parameterClasses = { values.getClass() };
        Object[] parameterValues = { values };
        Constructor c = this.getClass().getConstructor(parameterClasses);
        return (FeatureVector<T>) c.newInstance(parameterValues);
    }

    /**
     * An Object obj is equal to this RealVector
     * if it is an instance of the same runtime class
     * and is of the identical dimensionality
     * and the values of this RealVector are equal
     * to the values of obj in all dimensions, respectively.
     * 
     * @param obj another Object
     * @return true if the specified Object is an instance of the same runtime class
     * and is of the identical dimensionality
     * and the values of this RealVector are equal
     * to the values of obj in all dimensions, respectively 
     * 
     * @see MetricalObject#equals(Object)
     */
    public boolean equals(Object obj)
    {
        if(this.getClass().isInstance(obj))
        {
            RealVector rv = (RealVector) obj;
            boolean equal = (this.getDimensionality() == rv.getDimensionality());
            for(int i = 1; i <= getDimensionality() && equal; i++)
            {
                equal &= this.getValue(i).equals(rv.getValue(i));
            }
            return equal;
        }
        else
        {
            return false;
        }
    }

}
