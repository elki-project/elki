package de.lmu.ifi.dbs.index.btree;

import de.lmu.ifi.dbs.distance.Distance;

import java.io.Serializable;

/**
 * TODO: comment
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class LOFTableEntry implements Serializable
{
    private final Integer id;

    private final Distance sum1;

    private final Distance[] sum2s;

    private final double k;

    public LOFTableEntry(Integer id, int k, Distance sum1, Distance[] sum2s)
    {
        this.id = id;
        this.k = k;
        this.sum1 = sum1;
        this.sum2s = sum2s;
    }

    public String toString()
    {
        StringBuffer sum2 = new StringBuffer();
        sum2.append("[");
        for (int i = 0; i < this.sum2s.length; i++)
        {
            if (i < this.sum2s.length - 1)
                sum2.append(this.sum2s[i]).append(", ");
            else
                sum2.append(this.sum2s[i]).append("] ");
        }
        return "(" + id + ", " + sum1 + ", " + sum2 + ")";
    }

    public Integer getID()
    {
        return id;
    }

    public Distance getSum1()
    {
        return sum1;
    }

    public Distance getSum2(int i)
    {
        return sum2s[i];
    }

    public Distance[] getSum2s()
    {
        return sum2s;
    }

    public int getK()
    {
        return (int) k;
    }

    public double getLOF()
    {
        // double sum_1 = ((DoubleDistance) sum1).getValue();
        // double sum_2 = 0;
        // for (int i = 0; i < sum2s.length; i++) {
        // sum2s[i].times()
        // sum_2 += 1 / ((DoubleDistance) sum2s[i]).getValue();
        // }
        // return 1 / k * sum_1 * sum_2;
        return 0;
    }
}
