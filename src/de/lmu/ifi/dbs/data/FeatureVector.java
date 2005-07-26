package de.lmu.ifi.dbs.data;

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.linearalgebra.Matrix;

/**
 * A FeatureVector is to store real values approximately as double values.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FeatureVector implements RealVector
{
    /**
     * The String to separate attribute values in a String that represents the values.
     */
    public final static String ATTRIBUTE_SEPARATOR = " ";
    
    /**
     * Keeps the values of the feature vector
     */
    private double[] values;
    
    /**
     * Provides a feature vector consisting of double values
     * according to the given Double values.
     * 
     * @param values the values to be set as values of the feature vector
     */
    public FeatureVector(List<Double> values)
    {
        int i = 0;
        this.values = new double[values.size()];
        for(Iterator<Double> iter = values.iterator(); iter.hasNext(); i++)
        {
            this.values[i] = iter.next();
        }
    }
    
    /**
     * Provides a feature vector consisting of the given double values.
     * 
     * @param values the values to be set as values of the feature vector
     */
    public FeatureVector(double[] values)
    {
        this.values = new double[values.length];
        for(int i = 0; i < values.length; i++)
        {
            this.values[i] = values[i];
        }
    }
    
    /**
     * Expects a matrix of one column.
     * 
     * @param columnMatrix a matrix of one column
     */
    public FeatureVector(Matrix columnMatrix)
    {
        values = new double[columnMatrix.getRowDimension()];
        for(int i = 0; i < values.length; i++)
        {
            values[i] = columnMatrix.get(i, 0);
        }
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.RealVector#getDimensionality()
     */
    public int getDimensionality()
    {
        return values.length;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.RealVector#getValue(int)
     */
    public double getValue(int dimension)
    {
        if(dimension < 1 || dimension > values.length)
        {
            throw new IllegalArgumentException("Dimension "+dimension+" out of range.");
        }
        return values[dimension-1];
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.RealVector#getVector()
     */
    public Matrix getVector()
    {
        return new Matrix(values, values.length);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.RealVector#plus(de.lmu.ifi.dbs.data.RealVector)
     */
    public RealVector plus(RealVector rv)
    {
        if(rv.getDimensionality() != this.getDimensionality())
        {
            throw new IllegalArgumentException("Incompatible dimensionality: "+this.getDimensionality()+" - "+rv.getDimensionality()+".");
        }
        double[] values = new double[this.values.length];
        for(int i = 0; i < values.length; i++)
        {
            values[i] = this.values[i] + rv.getValue(i+1);
        }
        return new FeatureVector(values);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.RealVector#nullVector()
     */
    public RealVector nullVector()
    {
        return new FeatureVector(new double[this.values.length]);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.RealVector#negativeVector()
     */
    public RealVector negativeVector()
    {
        return multiplicate(-1);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.data.RealVector#multiplicate(double)
     */
    public RealVector multiplicate(double k)
    {
        double[] values = new double[this.values.length];
        for(int i = 0; i < values.length; i++)
        {
            values[i] = this.values[i] * -1;
        }
        return new FeatureVector(values);
    }

    /**
     * 
     * @see RealVector#toString()
     */
    @Override
    public String toString()
    {
        StringBuffer featureLine = new StringBuffer();
        for(int i = 0; i < values.length; i++)
        {
            featureLine.append(values[i]);
            if(i + 1 < values.length)
            {
                featureLine.append(ATTRIBUTE_SEPARATOR);
            }
        }
        return featureLine.toString();
    }

}
