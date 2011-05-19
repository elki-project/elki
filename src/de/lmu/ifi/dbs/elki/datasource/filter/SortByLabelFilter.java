package de.lmu.ifi.dbs.elki.datasource.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * A filter to sort the data set by some label.
 * 
 * @author Erich Schubert
 */
public class SortByLabelFilter implements ObjectFilter {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(SortByLabelFilter.class);

  /**
   * Constructor.
   */
  public SortByLabelFilter() {
    super();
  }

  @Override
  public MultipleObjectsBundle filter(final MultipleObjectsBundle objects) {
    if(logger.isDebugging()) {
      logger.debug("Shuffling the data set");
    }

    // Prepare a reposition array for cheap resorting
    final int size = objects.dataLength();
    final Integer[] offsets = new Integer[size];
    for(int i = 0; i < size; i++) {
      offsets[i] = i;
    }
    // Sory by labels - identify a label column
    final int lblcol;
    {
      int lblc = -1;
      for(int i = 0; i < objects.metaLength(); i++) {
        if(TypeUtil.GUESSED_LABEL.isAssignableFromType(objects.meta(i))) {
          lblc = i;
          break;
        }
      }
      lblcol = lblc; // make static
    }
    Arrays.sort(offsets, new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        String l1 = objects.data(o1, lblcol).toString();
        String l2 = objects.data(o2, lblcol).toString();
        return l1.compareToIgnoreCase(l2);
      }
    });

    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    for(int j = 0; j < objects.metaLength(); j++) {
      // Reorder column accoringly
      List<?> in = objects.getColumn(j);
      List<Object> data = new ArrayList<Object>(size);
      for(int i = 0; i < size; i++) {
        data.add(in.get(offsets[i]));
      }
      bundle.appendColumn(objects.meta(j), data);
    }
    return bundle;
  }
}