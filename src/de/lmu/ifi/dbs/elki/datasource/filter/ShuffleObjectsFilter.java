package de.lmu.ifi.dbs.elki.datasource.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * A filter to shuffle the dataset.
 * 
 * @author Erich Schubert
 */
public class ShuffleObjectsFilter implements ObjectFilter {
  /**
   * Class logger
   */
  private static final Logging logger = Logging.getLogger(ShuffleObjectsFilter.class);

  /**
   * Optional parameter to specify a seed for randomly shuffling the rows of the
   * database. If unused, no shuffling will be performed. Shuffling takes time
   * linearly dependent from the size of the database.
   * <p>
   * Key: {@code -dbc.seed}
   * </p>
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("dbc.seed", "Seed for randomly shuffling the rows for the database. If the parameter is not set, no shuffling will be performed.");

  /**
   * Seed for randomly shuffling the rows of the database. If null, no shuffling
   * will be performed. Shuffling takes time linearly dependent from the size of
   * the database.
   */
  final Long seed;

  /**
   * Constructor.
   * 
   * @param seed Seed value, may be {@code null} for a random seed.
   */
  public ShuffleObjectsFilter(Long seed) {
    super();
    this.seed = seed;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    if(logger.isDebugging()) {
      logger.debug("Shuffling the data set");
    }
    final Random random = (seed == null) ? new Random() : new Random(seed);

    final int size = objects.dataLength();
    final int[] offsets = new int[size];
    for(int i = 0; i < size; i++) {
      offsets[i] = i;
    }
    // Randomize the offset array
    for(int i = size; i > 1; i--) {
      final int j = random.nextInt(i);
      // Swap the elements at positions j and i - 1:
      final int temp = offsets[j];
      offsets[j] = offsets[i - 1];
      offsets[i - 1] = temp;
    }

    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    for(int j = 0; j < objects.metaLength(); j++) {
      // Reorder column accordingly
      List<?> in = objects.getColumn(j);
      List<Object> data = new ArrayList<Object>(size);
      for(int i = 0; i < size; i++) {
        data.add(in.get(offsets[i]));
      }
      bundle.appendColumn(objects.meta(j), data);
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
    Long seed = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      LongParameter seedParam = new LongParameter(SEED_ID, true);
      if(config.grab(seedParam)) {
        seed = seedParam.getValue();
      }
    }

    @Override
    protected Object makeInstance() {
      return new ShuffleObjectsFilter(seed);
    }
  }
}