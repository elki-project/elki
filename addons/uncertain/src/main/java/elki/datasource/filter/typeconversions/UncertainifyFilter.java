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
package elki.datasource.filter.typeconversions;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.data.uncertain.UncertainObject;
import elki.data.uncertain.uncertainifier.Uncertainifier;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.datasource.filter.ObjectFilter;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Filter class to transform a database containing vector fields into a database
 * containing {@link UncertainObject} fields by invoking a
 * {@link Uncertainifier} on each vector.
 * <p>
 * The purpose for that is to use those transformed databases in experiments
 * regarding uncertain data in some way.
 *
 * @author Alexander Koos
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <UO> Uncertain object type
 */
public class UncertainifyFilter<UO extends UncertainObject> implements ObjectFilter {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(UncertainifyFilter.class);

  /**
   * Class to derive an uncertain object from a certain vector.
   */
  protected Uncertainifier<UO> generator;

  /**
   * Flag to keep the original data.
   */
  protected boolean keep;

  /**
   * Random generator.
   */
  protected Random rand;

  /**
   * Constructor.
   *
   * @param generator Generator for uncertain objects
   * @param keep Flag to keep the original data
   * @param randf Random factory
   */
  public UncertainifyFilter(Uncertainifier<UO> generator, boolean keep, RandomFactory randf) {
    this.generator = generator;
    this.keep = keep;
    this.rand = randf.getSingleThreadedRandom();
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    if(objects.dataLength() == 0) {
      return objects;
    }
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();

    for(int r = 0; r < objects.metaLength(); r++) {
      SimpleTypeInformation<?> type = objects.meta(r);
      @SuppressWarnings("unchecked")
      final List<Object> column = (List<Object>) objects.getColumn(r);
      if(!TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(type)) {
        bundle.appendColumn(type, column);
        continue;
      }
      // Get the replacement type information
      @SuppressWarnings("unchecked")
      final VectorFieldTypeInformation<NumberVector> castType = (VectorFieldTypeInformation<NumberVector>) type;
      final int dim = castType.getDimensionality();

      if(keep) {
        bundle.appendColumn(type, column);
      }
      // Uncertain objects produced
      final List<UO> uos = new ArrayList<>(column.size());

      // Normalization scan
      FiniteProgress nprog = LOG.isVerbose() ? new FiniteProgress("Derive uncertain objects", objects.dataLength(), LOG) : null;
      for(int i = 0; i < objects.dataLength(); i++) {
        final NumberVector obj = (NumberVector) column.get(i);
        final UO normalizedObj = generator.newFeatureVector(rand, obj, ArrayLikeUtil.NUMBERVECTORADAPTER);
        uos.add(normalizedObj);
        LOG.incrementProcessed(nprog);
      }
      LOG.ensureCompleted(nprog);
      // Add column with uncertain objects
      bundle.appendColumn(new VectorFieldTypeInformation<UO>(generator.getFactory(), dim), uos);
    }
    return bundle;
  }

  /**
   * Parameterization class.
   *
   * @author Alexander Koos
   */
  public static class Par<UO extends UncertainObject> implements Parameterizer {
    /**
     * Parameter to specify the uncertainityModel used for the
     * uncertainification.
     */
    public static final OptionID UNCERTAINITY_MODEL_ID = new OptionID("uofilter.generator", //
        "Generator to derive uncertain objects from certain vectors.");

    /**
     * Flag to keep the original data.
     */
    public static final OptionID KEEP_ID = new OptionID("uofilter.keep", //
        "Keep the original data as well.");

    /**
     * Seed for random generation.
     */
    public static final OptionID SEED_ID = new OptionID("uofilter.seed", //
        "Random seed for uncertainification.");

    /**
     * Field to hold the generator to produce uncertain objects.
     */
    protected Uncertainifier<UO> generator;

    /**
     * Flag to keep the original data.
     */
    protected boolean keep;

    /**
     * Random generator.
     */
    protected RandomFactory rand;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Uncertainifier<UO>>(UNCERTAINITY_MODEL_ID, Uncertainifier.class) //
          .grab(config, x -> generator = x);
      new Flag(KEEP_ID).grab(config, x -> keep = x);
      new RandomParameter(SEED_ID).grab(config, x -> rand = x);
    }

    @Override
    public UncertainifyFilter<UO> make() {
      return new UncertainifyFilter<UO>(generator, keep, rand);
    }
  }
}
