package de.lmu.ifi.dbs.elki.datasource.filter;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Attribute-wise Normalization using the error function. This mostly makes
 * sense when you have data that has been mean-variance normalized before.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class AttributeWiseErfNormalization<O extends NumberVector<O, ?>> extends AbstractNormalization<O> {
  /**
   * Constructor.
   */
  public AttributeWiseErfNormalization() {
    super();
  }

  @SuppressWarnings("unused")
  @Override
  public O restore(O featureVector) throws NonNumericFeaturesException {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  protected O filterSingleObject(O obj) {
    double[] val = new double[obj.getDimensionality()];
    for(int i = 0; i < val.length; i++) {
      val[i] = MathUtil.erf(obj.doubleValue(i + 1));
    }
    return obj.newInstance(val);
  }

  @Override
  protected SimpleTypeInformation<? super O> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }
}
