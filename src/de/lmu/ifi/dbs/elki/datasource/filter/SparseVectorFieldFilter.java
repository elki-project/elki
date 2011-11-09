package de.lmu.ifi.dbs.elki.datasource.filter;

import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;

/**
 * Class that turns sparse float vectors into a proper vector field, by setting
 * the maximum dimensionality for each vector.
 * 
 * @author Erich Schubert
 */
public class SparseVectorFieldFilter extends AbstractConversionFilter<SparseFloatVector, SparseFloatVector> {
  /**
   * Maximum dimension
   */
  int maxdim = -1;

  /**
   * Constructor.
   */
  public SparseVectorFieldFilter() {
    super();
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<SparseFloatVector> in) {
    return true;
  }

  @Override
  protected void prepareProcessInstance(SparseFloatVector obj) {
    maxdim = Math.max(maxdim, obj.getDimensionality());
  }

  @Override
  protected SparseFloatVector filterSingleObject(SparseFloatVector obj) {
    assert(maxdim > 0);
    obj.setDimensionality(maxdim);
    return obj;
  }

  @Override
  protected SimpleTypeInformation<? super SparseFloatVector> getInputTypeRestriction() {
    return TypeUtil.SPARSE_FLOAT_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super SparseFloatVector> convertedType(SimpleTypeInformation<SparseFloatVector> in) {
    return new VectorFieldTypeInformation<SparseFloatVector>(SparseFloatVector.class, maxdim);
  }
}