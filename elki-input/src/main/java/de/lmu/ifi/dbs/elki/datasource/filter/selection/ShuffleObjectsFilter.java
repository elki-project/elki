/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.datasource.filter.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * A filter to shuffle the dataset.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.ShuffleObjectsFilter" })
public class ShuffleObjectsFilter implements ObjectFilter {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(ShuffleObjectsFilter.class);

  /**
   * Random generator.
   */
  final RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param rnd Random generator
   */
  public ShuffleObjectsFilter(RandomFactory rnd) {
    super();
    this.rnd = rnd;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    if(LOG.isDebugging()) {
      LOG.debug("Shuffling the data set");
    }
    final Random random = rnd.getSingleThreadedRandom();

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
      List<Object> data = new ArrayList<>(size);
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
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Optional parameter to specify a seed for randomly shuffling the rows of
     * the database. If not set, a random seed will be used. Shuffling takes
     * time linearly dependent from the size of the database.
     */
    public static final OptionID SEED_ID = new OptionID("shuffle.seed", "Seed for randomly shuffling the rows for the database. If the parameter is not set, a random seed will be used.");

    /**
     * Random generator
     */
    RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      RandomParameter rndP = new RandomParameter(SEED_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected ShuffleObjectsFilter makeInstance() {
      return new ShuffleObjectsFilter(rnd);
    }
  }
}
