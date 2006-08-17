package de.lmu.ifi.dbs.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.math.linearalgebra.Vector;

/**
 * A SparseDoubleVector is to store real values approximately as double values.
 * <p/> <p/> Class for storing a DoubleVector as a sparse vector. A
 * SparseDoubleVector only requires storage for those attribute values that are
 * non-zero.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class SparseDoubleVector extends RealVector<Double>
{

    /**
     * Mapping of indices and corresponding values. Only non-zero values will to
     * be stored.
     */
    private Map<Integer, Double> values;

    /**
     * The dimensionality of this feature vector.
     */
    private final int dimensionality;

    /**
     * Provides a SparseDoubleVector consisting of double values according to
     * the specified mapping of indices and values.
     * 
     * @param values
     *            the values to be set as values of the real vector
     * @param dimensionality
     *            the dimensionality of this feature vector
     */
    public SparseDoubleVector(Map<Integer, Double> values, int dimensionality)
    {
        if (values.size() > dimensionality)
        {
            throw new IllegalArgumentException(
                    "values.size() > dimensionality!");
        }

        this.values = new HashMap<Integer, Double>();
        for (Integer index : values.keySet())
        {
            Double value = values.get(index);
            if (value != 0)
            {
                this.values.put(index, value);
            }
        }
        this.dimensionality = dimensionality;
    }

    /**
     * Provides a SparseDoubleVector consisting of double values according to
     * the specified mapping of indices and values.
     * 
     * @param values
     *            the values to be set as values of the real vector
     */
    public SparseDoubleVector(double[] values)
    {
        this.dimensionality = values.length;

        this.values = new HashMap<Integer, Double>();
        for (int i = 0; i < values.length; i++)
        {
            double value = values[i];
            if (value != 0)
            {
                this.values.put(i, value);
            }
        }
    }

    /**
     * @see RealVector#newInstance(double[])
     */
    public RealVector<Double> newInstance(double[] values)
    {
        return new SparseDoubleVector(values);
    }

    /**
     * @see de.lmu.ifi.dbs.data.FeatureVector#randomInstance(java.util.Random)
     */
    public FeatureVector<Double> randomInstance(Random random)
    {
        return randomInstance(0.0, 1.0, random);
    }

    /**
     * @see de.lmu.ifi.dbs.data.FeatureVector#randomInstance(Number, Number,
     *      java.util.Random)
     */
    public FeatureVector<Double> randomInstance(Double min, Double max,
                                                Random random)
    {
        double[] randomValues = new double[dimensionality];
        for (int i = 0; i < dimensionality; i++)
        {
            randomValues[i] = random.nextDouble() * (max - min) + min;
        }
        return new SparseDoubleVector(randomValues);
    }

    /**
     * @see FeatureVector#getDimensionality()
     */
    public int getDimensionality()
    {
        return dimensionality;
    }

    /**
     * @see FeatureVector#getValue(int)
     */
    public Double getValue(int dimension)
    {
        Double d = values.get(dimension - 1);
        if (d != null)
        {
            return d;
        } else
        {
            return 0.0;
        }
    }

    /**
     * @see de.lmu.ifi.dbs.data.FeatureVector#getColumnVector()
     */
    public Vector getColumnVector()
    {
        double[] values = getValues();
        return new Vector(values);
    }

    /**
     * @see de.lmu.ifi.dbs.data.FeatureVector#getRowVector()
     */
    public Matrix getRowVector()
    {
        double[] values = getValues();
        return new Matrix(new double[][] { values.clone() });
    }

    /**
     * @see FeatureVector#plus(FeatureVector)
     */
    public FeatureVector<Double> plus(FeatureVector<Double> fv)
    {
        if (fv.getDimensionality() != this.getDimensionality())
        {
            throw new IllegalArgumentException("Incompatible dimensionality: "
                                               + this.getDimensionality() + " - " + fv.getDimensionality()
                                               + ".");
        }

        double[] values = new double[dimensionality];
        for (int i = 0; i < dimensionality; i++)
        {
            values[i] = getValue(i + 1) + fv.getValue(i + 1);
        }
        return new SparseDoubleVector(values);
    }

    /**
     * @see FeatureVector#nullVector()
     */
    public FeatureVector<Double> nullVector()
    {
        return new SparseDoubleVector(new HashMap<Integer, Double>(),
                                      dimensionality);
    }

    /**
     * @see FeatureVector#negativeVector()
     */
    public FeatureVector<Double> negativeVector()
    {
        return multiplicate(-1);
    }

    /**
     * @see FeatureVector#multiplicate(double)
     */
    public FeatureVector<Double> multiplicate(double k)
    {
        double[] values = new double[dimensionality];
        for (int i = 0; i < dimensionality; i++)
        {
            values[i] = getValue(i + 1) * k;
        }
        return new SparseDoubleVector(values);
    }

    /**
     * @see de.lmu.ifi.dbs.data.FeatureVector#toString()
     */
    @Override
    public String toString()
    {
        StringBuffer featureLine = new StringBuffer();
        for (int i = 0; i < dimensionality; i++)
        {
            featureLine.append(getValue(i + 1));
            if (i + 1 < dimensionality)
            {
                featureLine.append(ATTRIBUTE_SEPARATOR);
            }
        }
        return featureLine.toString();
    }

    /**
     * Returns an array consisting of the values of this feature vector.
     * 
     * @return an array consisting of the values of this feature vector
     */
    private double[] getValues()
    {
        double[] values = new double[dimensionality];
        for (int i = 0; i < dimensionality; i++)
        {
            values[i] = getValue(i);
        }
        return values;
    }

}
