package de.lmu.ifi.dbs.utilities;

/**
 * @author Arthur Zimek
 */
public class IntegerIntegerPair
{
    private int firstInteger;

    private int secondInteger;

    public IntegerIntegerPair(int firstInteger, int secondInteger)
    {
        this.firstInteger = firstInteger;
        this.secondInteger = secondInteger;
    }

    public int getFirstInteger()
    {
        return this.firstInteger;
    }

    public int getSecondInteger()
    {
        return this.secondInteger;
    }

    @Override
    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + this.firstInteger;
        result = PRIME * result + this.secondInteger;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
        {
            return true;
        }
        if(obj == null)
        {
            return false;
        }
        if(getClass() != obj.getClass())
        {
            return false;
        }
        final IntegerIntegerPair other = (IntegerIntegerPair) obj;
        return this.firstInteger == other.firstInteger && this.secondInteger == other.secondInteger;
    }

}
