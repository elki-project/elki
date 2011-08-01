package de.lmu.ifi.dbs.elki.datasource.filter;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * A filter to remove entries that have missing values.
 * 
 * @author Erich Schubert
 */
public class FilterNoMissingValuesFilter implements ObjectFilter {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(FilterNoMissingValuesFilter.class);

  /**
   * Constructor.
   */
  public FilterNoMissingValuesFilter() {
    super();
  }

  @Override
  public MultipleObjectsBundle filter(final MultipleObjectsBundle objects) {
    if(logger.isDebugging()) {
      logger.debug("Filtering the data set");
    }

    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    for(int j = 0; j < objects.metaLength(); j++) {
      bundle.appendColumn(objects.meta(j), new ArrayList<Object>());
    }
    for(int i = 0; i < objects.dataLength(); i++) {
      boolean good = true;
      for(int j = 0; j < objects.metaLength(); j++) {
        if(objects.data(i, j) == null) {
          good = false;
          break;
        }
      }
      if(good) {
        bundle.appendSimple(objects.getRow(i));
      }
    }
    return bundle;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected Object makeInstance() {
      return new FilterNoMissingValuesFilter();
    }
  }
}