package de.lmu.ifi.dbs.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Provides a Distance for a float-valued distance.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class FloatDistance extends NumberDistance<FloatDistance>
{
    
    /**
     * Generated serialVersionUID.
     */
    private static final long serialVersionUID = -5702250266358369075L;
    
    /**
     * The float value of this distance.
     */
    float value;

    /**
     * Empty constructor for serialization purposes.
     */
    public FloatDistance()
    {
        super();
    }

    /**
     * Constructs a new FloatDistance object that represents the float argument.
     * 
     * @param value
     *            the value to be represented by the FloatDistance.
     */
    public FloatDistance(float value)
    {
        super();
        this.value = value;
    }

    /**
     * @see de.lmu.ifi.dbs.distance.Distance#plus(Distance)
     */
    public FloatDistance plus(FloatDistance distance)
    {

        return new FloatDistance(this.value + distance.value);
    }

    /**
     * @see de.lmu.ifi.dbs.distance.Distance#minus(Distance)
     */
    public FloatDistance minus(FloatDistance distance)
    {
        return new FloatDistance(this.value - distance.value);
    }

    /**
     * Returns a new distance as the product of this distance and the given
     * distance.
     * 
     * @param distance
     *            the distancce to be multiplied with this distance
     * @return a new distance as the product of this distance and the given
     *         distance
     */
    public FloatDistance times(FloatDistance distance)
    {
        return new FloatDistance(this.value * distance.value);
    }

    /**
     * Returns a new distance as the product of this distance and the given
     * double value.
     * 
     * @param lambda
     *            the double value this distance should be multiplied with
     * @return a new distance as the product of this distance and the given
     *         double value
     */
    public FloatDistance times(float lambda)
    {
        return new FloatDistance(this.value * lambda);
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(FloatDistance d)
    {   return Float.compare(this.value, d.value);
    }

    /**
     * The object implements the writeExternal method to save its contents by
     * calling the methods of DataOutput for its primitive values or calling the
     * writeObject method of ObjectOutput for objects, strings, and arrays.
     * 
     * @param out
     *            the stream to write the object to
     * @throws java.io.IOException
     *             Includes any I/O exceptions that may occur
     * @serialData Overriding methods should use this tag to describe the data
     *             layout of this Externalizable object. List the sequence of
     *             element types and, if possible, relate the element to a
     *             public/protected field and/or method of this Externalizable
     *             class.
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeFloat(value);
    }

    /**
     * The object implements the readExternal method to restore its contents by
     * calling the methods of DataInput for primitive types and readObject for
     * objects, strings and arrays. The readExternal method must read the values
     * in the same sequence and with the same types as were written by
     * writeExternal.
     * 
     * @param in
     *            the stream to read data from in order to restore the object
     * @throws java.io.IOException
     *             if I/O errors occur
     * @throws ClassNotFoundException
     *             If the class for an object being restored cannot be found.
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        value = in.readFloat();
    }

    /**
     * Retuns the number of Bytes this distance uses if it is written to an
     * external file.
     * 
     * @return 4 (4 Byte for a double value)
     */
    public int externalizableSize()
    {
        return 4;
    }

    /**
     * Returns the double value of this distance.
     * 
     * @return the double value of this distance
     */
    public double getDoubleValue()
    {
        return value;
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
        if(!super.equals(o))
            return false;

        final FloatDistance that = (FloatDistance) o;

        return Float.compare(that.value, value) == 0;
    }

    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        return value != +0.0f ? Float.floatToIntBits(value) : 0;
    }

    /**
     * Returns a string representation of this distance.
     * 
     * @return a string representation of this distance.
     */
    public String toString()
    {
        return Float.toString(value);
    }
}
