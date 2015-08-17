package de.lmu.ifi.dbs.elki.datasource.filter.typeconversions;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier.Uncertainifier;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractConversionFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayLikeUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Filter class to transform a database containing vector fields (TODO I need to
 * express this more correctly) into a database containing
 * {@link UncertainObject} fields.
 *
 * The purpose for that is to use those transformed databases in experiments
 * regarding uncertain data in some way.
 *
 * @author Alexander Koos
 *
 * @param <UO> Uncertainty model
 * @param <U> Uncertain object type
 */
public class UncertainifyFilter<UO extends UncertainObject> extends AbstractConversionFilter<NumberVector, UO> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(UncertainifyFilter.class);

  /**
   * Class to derive an uncertain object from a certain vector.
   */
  protected Uncertainifier<UO> generator;

  /**
   * Random generator.
   */
  protected Random rand;

  /**
   * Constructor.
   *
   * @param generator Generator for uncertain objects
   * @param randf Random factory
   */
  public UncertainifyFilter(Uncertainifier<UO> generator, RandomFactory randf) {
    this.generator = generator;
    this.rand = randf.getSingleThreadedRandom();
  }

  @Override
  protected UO filterSingleObject(NumberVector obj) {
    return generator.newFeatureVector(rand, obj, ArrayLikeUtil.NUMBERVECTORADAPTER);
  }

  @Override
  protected SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<UO> convertedType(SimpleTypeInformation<NumberVector> in) {
    final int dim = ((VectorFieldTypeInformation<NumberVector>) in).getDimensionality();
    return new VectorFieldTypeInformation<UO>(generator.getFactory(), dim);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Alexander Koos
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<UO extends UncertainObject> extends AbstractParameterizer {
    /**
     * Parameter to specify the uncertainityModel used for the
     * uncertainification.
     */
    public static final OptionID UNCERTAINITY_MODEL_ID = new OptionID("uofilter.generator", //
    "Generator to derive uncertain objects from certain vectors.");

    /**
     * Seed for random generation.
     */
    public static final OptionID SEED_ID = new OptionID("uofilter.seed", "Seed for uncertainification.");

    /**
     * Field to hold the generator to produce uncertain objects.
     */
    protected Uncertainifier<UO> generator;

    /**
     * Random generator.
     */
    protected RandomFactory rand;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Uncertainifier<UO>> generatorP = new ObjectParameter<>(UNCERTAINITY_MODEL_ID, Uncertainifier.class);
      if(config.grab(generatorP)) {
        generator = generatorP.instantiateClass(config);
      }
      RandomParameter randomP = new RandomParameter(SEED_ID);
      if(config.grab(randomP)) {
        rand = randomP.getValue();
      }
    }

    @Override
    protected UncertainifyFilter<UO> makeInstance() {
      return new UncertainifyFilter<UO>(generator, rand);
    }
  }
}
