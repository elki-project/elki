package de.lmu.ifi.dbs.elki.datasource.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractConversionFilter;

/**
 * Abstract super class for all normalizations.
 * 
 * @author Elke Achtert
 * 
 * @param <O> Object type processed
 */
public abstract class AbstractNormalization<O> extends AbstractConversionFilter<O, O> implements Normalization<O> {
  /**
   * Initializes the option handler and the parameter map.
   */
  protected AbstractNormalization() {
    super();
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public List<O> normalize(List<O> objs) {
    if(objs.size() == 0) {
      return Collections.emptyList();
    }

    if(prepareStart(null)) {
      for(O obj : objs) {
        prepareProcessInstance(obj);
      }
      prepareComplete();
    }

    List<O> normalized = new ArrayList<O>(objs.size());
    for(O obj : objs) {
      O normalizedObj = filterSingleObject(obj);
      normalized.add(normalizedObj);
    }
    return normalized;
  }

  @Override
  protected SimpleTypeInformation<? super O> convertedType(SimpleTypeInformation<O> in) {
    return in;
  }

  @Override
  public final MultipleObjectsBundle normalizeObjects(MultipleObjectsBundle objects) {
    return normalizeObjects(objects);
  }

  @SuppressWarnings("unused")
  @Override
  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) {
    // FIXME: implement.
    throw new UnsupportedOperationException("Not yet implemented!");
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("normalization class: ").append(getClass().getName());
    return result.toString();
  }
}