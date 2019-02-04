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
package de.lmu.ifi.dbs.elki.evaluation.classification.holdout;

import java.util.ArrayList;
import java.util.Random;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * RandomizedCrossValidation provides a set of partitions of a database
 * to perform cross-validation. The test sets are not guaranteed to be disjoint.
 * 
 * @author Arthur Zimek
 * @since 0.7.0
 */
public class RandomizedCrossValidation extends RandomizedHoldout {
  /**
   * Holds the number of folds, current fold.
   */
  protected int nfold, fold;

  /**
   * Constructor for n-fold cross-validation.
   * 
   * @param random Random seed
   * @param nfold Number of folds
   */
  public RandomizedCrossValidation(RandomFactory random, int nfold) {
    super(random);
    this.nfold = nfold;
  }

  @Override
  public void initialize(MultipleObjectsBundle bundle) {
    super.initialize(bundle);
    this.fold = 0;
  }

  @Override
  public int numberOfPartitions() {
    return nfold;
  }

  @Override
  public TrainingAndTestSet nextPartitioning() {
    if(fold >= nfold) {
      return null;
    }
    MultipleObjectsBundle training = new MultipleObjectsBundle();
    MultipleObjectsBundle test = new MultipleObjectsBundle();
    Random rnd = random.getRandom();
    int datalen = bundle.dataLength();

    boolean[] assignment = new boolean[datalen];
    int trsize = 0, tesize = 0;
    for(int i = 0; i < assignment.length; ++i) {
      boolean p = rnd.nextInt(nfold) < nfold - 1;
      assignment[i] = p;
      @SuppressWarnings("unused")
      int discard = p ? ++trsize : ++tesize;
    }
    // Process column-wise.
    for(int c = 0, cs = bundle.metaLength(); c < cs; ++c) {
      ArrayList<Object> tr = new ArrayList<>(trsize);
      ArrayList<Object> te = new ArrayList<>(tesize);
      for(int i = 0; i < datalen; ++i) {
        (assignment[i] ? tr : te).add(bundle.data(i, c));
      }
      training.appendColumn(bundle.meta(c), tr);
      test.appendColumn(bundle.meta(c), te);
    }

    ++fold;
    return new TrainingAndTestSet(training, test, labels);
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends RandomizedHoldout.Parameterizer {
    /**
     * Parameter for number of folds.
     */
    public static final OptionID NFOLD_ID = new OptionID("nfold", "positive number of folds for cross-validation");

    /**
     * Default number of folds.
     */
    public static final int N_DEFAULT = 10;

    /**
     * Holds the number of folds.
     */
    protected int nfold;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter nfoldP = new IntParameter(NFOLD_ID)//
          .setDefaultValue(N_DEFAULT) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(nfoldP)) {
        nfold = nfoldP.intValue();
      }
    }

    @Override
    protected RandomizedCrossValidation makeInstance() {
      return new RandomizedCrossValidation(random, nfold);
    }
  }
}
