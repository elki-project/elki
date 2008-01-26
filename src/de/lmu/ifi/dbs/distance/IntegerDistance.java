package de.lmu.ifi.dbs.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author Arthur Zimek
 */
public class IntegerDistance extends NumberDistance<IntegerDistance>
{
    /**
     * Created serial version UID.
     */
    private static final long serialVersionUID = 5583821082931825810L;

    private int value;
    
    public IntegerDistance()
    {
        super();
    }
    
    public IntegerDistance(int value)
    {
        super();
        this.value = value;
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.distance.NumberDistance#getDoubleValue()
     */
    @Override
    public double getDoubleValue()
    {
        return value;
    }

    /**
     * The hashcode is the internal integer value of this distance.
     * 
     * @see de.lmu.ifi.dbs.distance.AbstractDistance#hashCode()
     */
    @Override
    public int hashCode()
    {
        return value;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.Distance#externalizableSize()
     */
    public int externalizableSize()
    {
        return 4;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.Distance#minus(de.lmu.ifi.dbs.distance.Distance)
     */
    public IntegerDistance minus(IntegerDistance distance)
    {
        return new IntegerDistance(this.value - distance.value);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.Distance#plus(de.lmu.ifi.dbs.distance.Distance)
     */
    public IntegerDistance plus(IntegerDistance distance)
    {
        return new IntegerDistance(this.value + distance.value);
    }

    /**
     * Compares this distance with the specified distance
     * for order. Returns a negative integer, zero, or a positive integer
     * as this distance is less than, equal to, or greater than the specified distance.
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(IntegerDistance o)
    {
        return this.value - o.value;
    }

    /**
     * 
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException
    {
        value = in.readInt();
    }

    /**
     * 
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(value);
    }

}
