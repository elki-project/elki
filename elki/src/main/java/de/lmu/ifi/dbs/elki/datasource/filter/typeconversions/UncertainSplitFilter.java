package de.lmu.ifi.dbs.elki.datasource.filter.typeconversions;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.uncertain.UniformDiscreteUO;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractConversionFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Filter to transform a single vector into a set of samples to interpret as
 * uncertain observation.
 *
 * @author Erich Schubert
 */
public class UncertainSplitFilter extends AbstractConversionFilter<NumberVector, UniformDiscreteUO> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(UncertainSplitFilter.class);

  /**
   * Data dimensionality.
   */
  private int dims;

  /**
   * Constructor.
   *
   * @param dims Number of dimensions
   */
  public UncertainSplitFilter(int dims) {
    this.dims = dims;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  protected UniformDiscreteUO filterSingleObject(NumberVector vec) {
    final int dim = vec.getDimensionality();
    if(dim % dims != 0) {
      throw new AbortException("Vector length " + dim + " not divisible by the number of dimensions " + dims);
    }
    final int num = dim / dims;
    final DoubleVector[] samples = new DoubleVector[num];
    final double[] val = new double[dims];
    for(int i = 0, j = 0, k = 0; i < dim; i++) {
      val[j++] = vec.doubleValue(i);
      if(j == dims) {
        samples[k++] = new DoubleVector(val);
        j = 0;
      }
    }
    return new UniformDiscreteUO(samples);
  }

  @Override
  protected SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<UniformDiscreteUO> convertedType(SimpleTypeInformation<NumberVector> in) {
    final int dim = ((VectorFieldTypeInformation<NumberVector>) in).getDimensionality();
    if(dim % dims != 0) {
      throw new AbortException("Vector length " + dim + " not divisible by the number of dimensions " + dims);
    }
    final UniformDiscreteUO.Factory factory = new UniformDiscreteUO.Factory(0, 0, 0, 0, 0, 0, RandomFactory.DEFAULT);
    return new VectorFieldTypeInformation<UniformDiscreteUO>(factory, dim);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for specifying the number of dimensions of the sample.
     */
    public static final OptionID DIM_ID = new OptionID("uncertain.dimensionality", "Dimensionality of the data set (used for splitting).");

    /**
     * Field to hold the dimensional constraint.
     */
    protected int dims;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter dimsP = new IntParameter(DIM_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(dimsP)) {
        dims = dimsP.intValue();
      }
    }

    @Override
    protected UncertainSplitFilter makeInstance() {
      return new UncertainSplitFilter(dims);
    }
  }
}
