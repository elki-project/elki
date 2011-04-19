package de.lmu.ifi.dbs.elki.datasource.filter;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * <p>
 * Parser to project the ParsingResult obtained by a suitable base parser onto a
 * randomly selected subset of attributes.
 * </p>
 * 
 * @author Arthur Zimek
 */
public class DoubleVectorRandomProjectionFilter extends AbstractRandomFeatureSelectionFilter<DoubleVector> {
  /**
   * Constructor.
   * 
   * @param dim
   */
  public DoubleVectorRandomProjectionFilter(int dim) {
    super(dim);
  }

  @Override
  protected DoubleVector filterSingleObject(DoubleVector obj) {
    return Util.project(obj, selectedAttributes);
  }

  @Override
  protected SimpleTypeInformation<? super DoubleVector> getInputTypeRestriction() {
    return TypeUtil.DOUBLE_VECTOR_FIELD;
  }

  @SuppressWarnings("unused")
  @Override
  protected SimpleTypeInformation<? super DoubleVector> convertedType(SimpleTypeInformation<DoubleVector> in) {
    return new VectorFieldTypeInformation<DoubleVector>(DoubleVector.class, k, new DoubleVector(new double[k]));
  }
  
  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractRandomFeatureSelectionFilter.Parameterizer<DoubleVector> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected DoubleVectorRandomProjectionFilter makeInstance() {
      return new DoubleVectorRandomProjectionFilter(k);
    }
  }
}