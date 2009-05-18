package experimentalcode.arthur;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * <p>A SparseFloatVector is to store real values approximately as double values.</p>
 * 
 * A SparseFloatVector only requires storage for those attribute values that are
 * non-zero.
 *
 * @author Arthur Zimek
 */
public class SparseFloatVector extends RealVector<SparseFloatVector, Float> {

    /**
     * Mapping of indices and corresponding values. Only non-zero values will to
     * be stored.
     */
    private Map<Integer, Float> values;

    /**
     * The dimensionality of this feature vector.
     */
    private int dimensionality;

    /**
     * Provides a SparseFloatVector consisting of double values according to
     * the specified mapping of indices and values.
     *
     * @param values         the values to be set as values of the real vector
     * @param dimensionality the dimensionality of this feature vector
     */
    public SparseFloatVector(Map<Integer, Float> values, int dimensionality) {
        if (values.size() > dimensionality) {
            throw new IllegalArgumentException(
                "values.size() > dimensionality!");
        }

        this.values = new HashMap<Integer, Float>(values.size(),1);
        for (Integer index : values.keySet()) {
            Float value = values.get(index);
            if (value != 0) {
                this.values.put(index, value);
            }
        }
        this.dimensionality = dimensionality;
    }

    /**
     * Provides a SparseFloatVector consisting of double values according to
     * the specified mapping of indices and values.
     *
     * @param values the values to be set as values of the real vector
     */
    public SparseFloatVector(float[] values) {
        this.dimensionality = values.length;

        this.values = new HashMap<Integer, Float>();
        for (int i = 0; i < values.length; i++) {
            float value = values[i];
            if (value != 0) {
                this.values.put(i+1, value);
            }
        }
    }

    /**
     * @see RealVector#newInstance(double[])
     */
    @Override
    public SparseFloatVector newInstance(double[] values) {
        return new SparseFloatVector(Util.convertToFloat(values));
    }

    
    
    /**
     * @see de.lmu.ifi.dbs.elki.data.FeatureVector#newInstance(java.util.List)
     */
    @Override
    public SparseFloatVector newInstance(List<Float> values) {
      return new SparseFloatVector(Util.unboxToFloat(ClassGenericsUtil.toArray(values, Float.class)));
    }

    public SparseFloatVector newInstance(Float[] values) {
      return new SparseFloatVector(Util.unboxToFloat(values));
    }


    public SparseFloatVector randomInstance(Random random) {
        return randomInstance(0.0f, 1.0f, random);
    }

    public SparseFloatVector randomInstance(Float min, Float max,
                                             Random random) {
        float[] randomValues = new float[dimensionality];
        for (int i = 0; i < dimensionality; i++) {
            randomValues[i] = random.nextFloat() * (max - min) + min;
        }
        return new SparseFloatVector(randomValues);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.elki.data.FeatureVector#randomInstance(de.lmu.ifi.dbs.elki.data.FeatureVector, de.lmu.ifi.dbs.elki.data.FeatureVector, java.util.Random)
     */
    public SparseFloatVector randomInstance(SparseFloatVector min, SparseFloatVector max, Random random) {
      float[] randomValues = new float[dimensionality];
      for (int i = 0; i < dimensionality; i++) {
        randomValues[i] = random.nextFloat() * (max.getValue(i+1) - min.getValue(i+1)) + min.getValue(i+1);
      }
      return new SparseFloatVector(randomValues);
    }
    
    public int getDimensionality() {
        return dimensionality;
    }
    
    public void setDimensionality(int dimensionality){
      this.dimensionality = dimensionality;
    }

    public Float getValue(int dimension) {
        Float d = values.get(dimension);
        if (d != null) {
            return d;
        }
        else {
            return 0.0f;
        }
    }

    public Vector getColumnVector() {
        double[] values = getValues();
        return new Vector(values);
    }

    public Matrix getRowVector() {
        double[] values = getValues();
        return new Matrix(new double[][]{values.clone()});
    }

    public SparseFloatVector plus(SparseFloatVector fv) {
        if (fv.getDimensionality() != this.getDimensionality()) {
            throw new IllegalArgumentException("Incompatible dimensionality: "
                + this.getDimensionality() + " - " + fv.getDimensionality()
                + ".");
        }

        float[] values = new float[dimensionality];
        for (int i = 0; i < dimensionality; i++) {
            values[i] = getValue(i + 1) + fv.getValue(i + 1);
        }
        return new SparseFloatVector(values);
    }

    public SparseFloatVector nullVector() {
        return new SparseFloatVector(new HashMap<Integer, Float>(),
            dimensionality);
    }

    public SparseFloatVector negativeVector() {
        return multiplicate(-1);
    }

    public SparseFloatVector multiplicate(double k) {
        float[] values = new float[dimensionality];
        for (int i = 0; i < dimensionality; i++) {
            values[i] = (float) (getValue(i + 1) * k);
        }
        return new SparseFloatVector(values);
    }


    public String toCompleteString() {
        StringBuffer featureLine = new StringBuffer();
        for (int i = 0; i < dimensionality; i++) {
            featureLine.append(getValue(i + 1));
            if (i + 1 < dimensionality) {
                featureLine.append(ATTRIBUTE_SEPARATOR);
            }
        }
        return featureLine.toString();
    }
    
    @Override
    public String toString(){
      StringBuilder featureLine = new StringBuilder();
      featureLine.append(this.values.size());
      List<Integer> keys = new ArrayList<Integer>(this.values.keySet());
      Collections.sort(keys);
      for(Integer key : keys){
        featureLine.append(ATTRIBUTE_SEPARATOR);
        featureLine.append(key);
        featureLine.append(ATTRIBUTE_SEPARATOR);
        featureLine.append(this.values.get(key));
      }
      
      return featureLine.toString();
    }

    /**
     * Returns an array consisting of the values of this feature vector.
     *
     * @return an array consisting of the values of this feature vector
     */
    private double[] getValues() {
        double[] values = new double[dimensionality];
        for (int i = 0; i < dimensionality; i++) {
            values[i] = getValue(i+1);
        }
        return values;
    }

    

}
