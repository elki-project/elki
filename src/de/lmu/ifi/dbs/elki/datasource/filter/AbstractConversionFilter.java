package de.lmu.ifi.dbs.elki.datasource.filter;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract base class for simple conversion filters such as normalizations and projections.
 * 
 * @author Erich Schubert
 * 
 * @param <I> Input object type
 * @param <O> Input object type
 */
public abstract class AbstractConversionFilter<I, O> implements ObjectFilter {
  /**
   * A standard implementation of the filter process. First of all, all suitable
   * representations are found. Then (if {@link #prepareStart()} returns true),
   * the data is processed read-only in a first pass.
   * 
   * In the main pass, each object is then filtered using
   * {@link #filterSingleObject}.
   */
  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    if(objects.dataLength() == 0) {
      return objects;
    }

    List<Object> folded = new ArrayList<Object>(objects.getFolded());
    BundleMeta meta = new BundleMeta();
    for(int r = 0; r < objects.metaLength(); r++) {
      SimpleTypeInformation<?> type = objects.meta(r);
      if(!getInputTypeRestriction().isAssignableFromType(type)) {
        meta.add(type);
        continue;
      }
      // Get the replacement type information
      @SuppressWarnings("unchecked")
      final SimpleTypeInformation<I> castType = (SimpleTypeInformation<I>) type;
      meta.add(convertedType(castType));

      // When necessary, perform an initialization scan
      if(prepareStart(castType)) {
        for(int i = 0; i < objects.dataLength(); i++) {
          @SuppressWarnings("unchecked")
          final I obj = (I) objects.data(i, r);
          prepareProcessInstance(obj);
        }
        prepareComplete();
      }

      // Normalization scan
      for(int i = 0; i < objects.dataLength(); i++) {
        @SuppressWarnings("unchecked")
        final I obj = (I) folded.get(i * objects.metaLength() + r);
        final O normalizedObj = filterSingleObject(obj);
        folded.set(i * objects.metaLength() + r, normalizedObj);
      }
    }
    return new MultipleObjectsBundle(meta, folded);
  }

  /**
   * Normalize a single instance.
   * 
   * You can implement this as UnsupportedOperationException if you override
   * both public "normalize" functions!
   * 
   * @param obj Database object to normalize
   * @return Normalized database object
   */
  abstract protected O filterSingleObject(I obj);

  /**
   * Get the input type restriction used for negotiating the data query.
   * 
   * @return Type restriction
   */
  abstract protected SimpleTypeInformation<? super I> getInputTypeRestriction();

  /**
   * Get the output type from the input type after conversion.
   * 
   * @param input type restriction
   * @return output type restriction
   */
  abstract protected SimpleTypeInformation<? super O> convertedType(SimpleTypeInformation<I> in);

  /**
   * Return "true" when the normalization needs initialization
   * 
   * @param in Input type information
   * @return true or false
   */
  protected boolean prepareStart(SimpleTypeInformation<I> in) {
    return false;
  }

  /**
   * Process a single object during initialization.
   * 
   * @param obj Object to process
   */
  protected void prepareProcessInstance(I obj) {
    throw new AbortException("ProcessInstance not implemented, but prepareStart true?");
  }

  /**
   * Complete the initialization phase
   */
  protected void prepareComplete() {
    // optional - default NOOP.
  }
}