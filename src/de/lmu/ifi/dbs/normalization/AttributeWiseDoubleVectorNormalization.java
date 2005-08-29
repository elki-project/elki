package de.lmu.ifi.dbs.normalization;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.linearalgebra.Matrix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



/**
 * Class to perform and undo a normalization on DoubleVectors with respect to minimum and maximum in each dimension
 * independently from other dimensions.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class AttributeWiseDoubleVectorNormalization implements Normalization<DoubleVector>
{
    /**
     * Stores the maximum in each dimension.
     */
    private double[] maxima = new double[0];
    
    /**
     * Stores the minimum in each dimension.
     */
    private double[] minima = new double[0];

    /**
     * 
     * @see de.lmu.ifi.dbs.normalization.Normalization#normalize(java.util.List)
     */
    public List<DoubleVector> normalize(List<DoubleVector> featureVectors) throws NonNumericFeaturesException
    {
        try
        {
            int dim = -1;
            for(Iterator<DoubleVector> iter = featureVectors.iterator(); iter.hasNext();)
            {
                DoubleVector dv = iter.next();
                if(dim == -1)
                {
                    dim = dv.getDimensionality();
                    minima = new double[dim];
                    maxima = new double[dim];
                    for(int i = 0; i < dim; i++)
                    {
                        maxima[i] = Double.MIN_VALUE;
                        minima[i] = Double.MAX_VALUE;
                    }
                }
                if(dim != dv.getDimensionality())
                {
                    throw new IllegalArgumentException("FeatureVectors differ in length.");
                }
                for(int d = 0; d < dv.getDimensionality(); d++)
                {
                    if(dv.getValue(d).doubleValue() > maxima[d])
                    {
                        maxima[d] = dv.getValue(d).doubleValue();
                    }
                    if(dv.getValue(d).doubleValue() < minima[d])
                    {
                        minima[d] = dv.getValue(d).doubleValue();
                    }
                }
            }
            List<DoubleVector> normalized = new ArrayList<DoubleVector>();
            for(Iterator<DoubleVector> iter = featureVectors.iterator(); iter.hasNext();)
            {
                DoubleVector dv = iter.next();
                double[] v = new double[dv.getDimensionality()];
                for(int d = 0; d < dv.getDimensionality(); d++)
                {
                    v[d] = (dv.getValue(d)-minima[d]) / factor(d);
                }
                DoubleVector ndv = new DoubleVector(v);
                ndv.setID(dv.getID());
                normalized.add(ndv);
            }
            return normalized;
        }
        catch(Exception e)
        {
            minima = new double[0];
            maxima = new double[0];
            throw new NonNumericFeaturesException("Attributes cannot be normalized.", e);
        }
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.normalization.Normalization#restore(java.util.List)
     */
    public List<DoubleVector> restore(List<DoubleVector> featureVectors) throws NonNumericFeaturesException
    {
        try
        {
            List<DoubleVector> restored = new ArrayList<DoubleVector>();
            for(Iterator<DoubleVector> iter = featureVectors.iterator(); iter.hasNext();)
            {
                DoubleVector dv = iter.next();
                if(dv.getDimensionality() == maxima.length)
                {
                    double[] v = new double[dv.getDimensionality()];
                    for(int d = 0; d < dv.getDimensionality(); d++)
                    {
                        v[d] = (dv.getValue(d) * (factor(d)) + minima[d]);
                    }
                    DoubleVector rdv = new DoubleVector(v);
                    rdv.setID(dv.getID());
                    restored.add(rdv);
                }
                else
                {
                    throw new NonNumericFeaturesException("Attributes cannot be resized: current dimensionality: "+dv.getDimensionality()+" former dimensionality: "+maxima.length);
                }
            }
            return restored;
        }
        catch(Exception e)
        {
            throw new NonNumericFeaturesException("Attributes cannot be resized.", e);
        }
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.normalization.Normalization#transform(de.lmu.ifi.dbs.linearalgebra.Matrix)
     */
    public Matrix transform(Matrix matrix) throws NonNumericFeaturesException
    {
        Matrix transformed = new Matrix(matrix.getRowDimension(),matrix.getColumnDimension());
        for(int row = 0; row < matrix.getRowDimension(); row++)
        {
            double sum = 0.0;
            for(int col = 0; col < matrix.getColumnDimension()-1; col++)
            {
                sum += minima[col] * matrix.get(row, col) / factor(col);
                transformed.set(row, col, matrix.get(row, col) / factor(col));
            }
            transformed.set(row, matrix.getColumnDimension()-1, matrix.get(row, matrix.getColumnDimension()-1) + sum);
        }
        return transformed;
    }
    
    /**
     * Returns a factor for normalization in a certain dimension.
     * 
     * The provided factor is the maximum-minimum in the specified dimension, if these two values differ,
     * otherwise it is the maximum if this value differs from 0,
     * otherwise it is 1.
     * 
     * @param dimension the dimension to get a factor for normalization
     * @return a factor for normalization in a certain dimension
     */
    protected double factor(int dimension)
    {
        return maxima[dimension] != minima[dimension] ? maxima[dimension]-minima[dimension] : maxima[dimension]!= 0 ? maxima[dimension] : 1;
    }
}
