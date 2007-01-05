package de.lmu.ifi.dbs.distance;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * TODO comment
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class BitDistance extends NumberDistance<BitDistance>
{
    /**
     * Generated serial version UID 
     */
    private static final long serialVersionUID = 6514853467081717551L;
    
    /**
     * The bit value of this distance.
     */
    private boolean bit;
    
    /**
     * 
     */
    public BitDistance()
    {
        super();
    }

    /**
     * 
     */
    public BitDistance(boolean bit)
    {
        super();
        this.bit = bit;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.NumberDistance#getDoubleValue()
     */
    @Override
    public double getDoubleValue()
    {
        return bit ? 1 : 0;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.AbstractDistance#hashCode()
     */
    @Override
    public int hashCode()
    {
        return this.isBit() ? 1231 : 1237;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.Distance#plus(de.lmu.ifi.dbs.distance.Distance)
     */
    public BitDistance plus(BitDistance distance)
    {
        return new BitDistance(this.isBit() || distance.isBit());
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.Distance#minus(de.lmu.ifi.dbs.distance.Distance)
     */
    public BitDistance minus(BitDistance distance)
    {
        return new BitDistance(this.isBit() ^ distance.isBit() );
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.distance.Distance#externalizableSize()
     */
    public int externalizableSize()
    {
        return 1;
    }

    /**
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(BitDistance o)
    {
        return ((Boolean) this.isBit()).compareTo(o.isBit());
    }

    /**
     * 
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeBoolean(this.isBit());
    }

    /**
     * 
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        this.bit = in.readBoolean();
    }

    public boolean isBit()
    {
        return this.bit;
    }

    

}
