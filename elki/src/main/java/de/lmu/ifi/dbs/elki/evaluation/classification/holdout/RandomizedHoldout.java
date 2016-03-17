package de.lmu.ifi.dbs.elki.evaluation.classification.holdout;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * A holdout providing a seed for randomized operations.
 * 
 * @author Arthur Zimek
 * @since 0.3
 */
public abstract class RandomizedHoldout extends AbstractHoldout {
  /**
   * The random generator.
   */
  protected RandomFactory random;

  /**
   * Sets the parameter seed to the parameterToDescription map.
   */
  public RandomizedHoldout(RandomFactory random) {
    super();
    this.random = random;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer extends AbstractParameterizer {
    /**
     * Random seeding for holdout evaluation.
     */
    public static final OptionID SEED_ID = new OptionID("holdout.seed", "Random generator seed for holdout evaluation.");

    /**
     * The random generator.
     */
    protected RandomFactory random;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      RandomParameter seedP = new RandomParameter(SEED_ID);
      if(config.grab(seedP)) {
        random = seedP.getValue();
      }
    }
  }
}