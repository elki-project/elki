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
import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * A stratified n-fold crossvalidation to distribute the data to n buckets where
 * each bucket exhibits approximately the same distribution of classes as does
 * the complete data set. The buckets are disjoint. The distribution is
 * deterministic.
 *
 * @author Arthur Zimek
 * @since 0.7.0
 */
public class StratifiedCrossValidation extends AbstractHoldout {
  /**
   * Holds the number of folds, current fold.
   */
  protected int nfold, fold;

  /**
   * Partition assignment, sizes
   */
  protected int[] assignment, sizes;

  /**
   * Provides a stratified crossvalidation. Setting parameter N_P to the
   * OptionHandler.
   */
  public StratifiedCrossValidation(int nfold) {
    super();
    this.nfold = nfold;
  }

  @Override
  public int numberOfPartitions() {
    return nfold;
  }

  @Override
  public void initialize(MultipleObjectsBundle bundle) {
    super.initialize(bundle);
    fold = 0;
    IntArrayList[] classBuckets = new IntArrayList[this.labels.size()];
    for(int i = 0; i < this.labels.size(); i++) {
      classBuckets[i] = new IntArrayList();
    }
    for(int i = 0, l = bundle.dataLength(); i < l; ++i) {
      ClassLabel label = (ClassLabel) bundle.data(i, labelcol);
      if(label == null) {
        throw new AbortException("Unlabeled instances currently not supported.");
      }
      int classIndex = Collections.binarySearch(labels, label);
      if(classIndex < 0) {
        throw new AbortException("Label not in label list: " + label);
      }
      classBuckets[classIndex].add(i);
    }
    // TODO: shuffle the class buckets?
    sizes = new int[nfold];
    assignment = new int[bundle.dataLength()];
    for(IntArrayList bucket : classBuckets) {
      for(int i = 0; i < bucket.size(); i++) {
        assignment[bucket.getInt(i)] = i % nfold;
      }
    }
  }

  @Override
  public TrainingAndTestSet nextPartitioning() {
    if(fold >= nfold) {
      return null;
    }
    final int tesize = sizes[fold], trsize = bundle.dataLength() - tesize;
    MultipleObjectsBundle training = new MultipleObjectsBundle();
    MultipleObjectsBundle test = new MultipleObjectsBundle();
    // Process column-wise.
    for(int c = 0, cs = bundle.metaLength(); c < cs; ++c) {
      ArrayList<Object> tr = new ArrayList<>(trsize);
      ArrayList<Object> te = new ArrayList<>(tesize);
      for(int i = 0; i < bundle.dataLength(); ++i) {
        ((assignment[i] != fold) ? tr : te).add(bundle.data(i, c));
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
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Default number of folds.
     */
    public static final int N_DEFAULT = 10;

    /**
     * Parameter for number of folds.
     */
    public static final OptionID NFOLD_ID = new OptionID("nfold", "Number of folds for cross-validation");

    /**
     * Holds the number of folds.
     */
    protected int nfold;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter nfoldP = new IntParameter(NFOLD_ID, N_DEFAULT)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(nfoldP)) {
        nfold = nfoldP.intValue();
      }
    }

    @Override
    protected StratifiedCrossValidation makeInstance() {
      return new StratifiedCrossValidation(nfold);
    }
  }
}
