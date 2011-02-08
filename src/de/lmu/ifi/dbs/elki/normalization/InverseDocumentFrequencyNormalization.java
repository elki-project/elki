package de.lmu.ifi.dbs.elki.normalization;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Normalization for text frequency vectors, using the inverse document
 * frequency.
 * 
 * @author Erich Schubert
 */
public class InverseDocumentFrequencyNormalization extends AbstractNormalization<SparseFloatVector> {
  /**
   * The IDF storage
   */
  Map<Integer, Double> idf = new HashMap<Integer, Double>();

  /**
   * The number of objects in the dataset
   */
  int objcnt = 0;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public InverseDocumentFrequencyNormalization(Parameterization config) {
    super();
    config = config.descend(this);
    // TODO: add an option to also do TF normalization?
  }

  @Override
  protected boolean initNormalization() {
    if(idf.size() > 0) {
      throw new UnsupportedOperationException("This normalization may only be used once!");
    }
    return true;
  }

  @Override
  protected void initProcessInstance(SparseFloatVector featureVector) {
    BitSet b = featureVector.getNotNullMask();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      if(featureVector.doubleValue(i) != 0.0) {
        Double c = idf.get(i);
        if(c == null) {
          idf.put(i, 1.0);
        }
        else {
          idf.put(i, c + 1.0);
        }
      }
    }
  }

  @Override
  protected void initComplete() {
    final double dbsize = objcnt;
    // Compute IDF values
    for(Entry<Integer, Double> ent : idf.entrySet()) {
      // Note: dbsize is a double!
      ent.setValue(Math.log(dbsize / ent.getValue()));
    }
  }

  @Override
  protected SparseFloatVector normalize(SparseFloatVector featureVector) {
    BitSet b = featureVector.getNotNullMask();
    Map<Integer, Float> vals = new HashMap<Integer, Float>();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      vals.put(i, (float) (featureVector.doubleValue(i) * idf.get(i)));
    }
    return new SparseFloatVector(vals, featureVector.getDimensionality());
  }

  @Override
  public SparseFloatVector restore(SparseFloatVector featureVector) {
    BitSet b = featureVector.getNotNullMask();
    Map<Integer, Float> vals = new HashMap<Integer, Float>();
    for(int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i + 1)) {
      vals.put(i, (float) (featureVector.doubleValue(i) / idf.get(i)));
    }
    return new SparseFloatVector(vals, featureVector.getDimensionality());
  }
}