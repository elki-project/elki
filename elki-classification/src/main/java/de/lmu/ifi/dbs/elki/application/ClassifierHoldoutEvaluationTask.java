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
package de.lmu.ifi.dbs.elki.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.classification.Classifier;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.MultipleObjectsBundleDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.evaluation.classification.ConfusionMatrix;
import de.lmu.ifi.dbs.elki.evaluation.classification.holdout.AbstractHoldout;
import de.lmu.ifi.dbs.elki.evaluation.classification.holdout.Holdout;
import de.lmu.ifi.dbs.elki.evaluation.classification.holdout.StratifiedCrossValidation;
import de.lmu.ifi.dbs.elki.evaluation.classification.holdout.TrainingAndTestSet;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.Duration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Evaluate a classifier.
 *
 * TODO: split into application and task.
 *
 * TODO: add support for predefined test and training pairs!
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <O> Object type
 */
public class ClassifierHoldoutEvaluationTask<O> extends AbstractApplication {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ClassifierHoldoutEvaluationTask.class);

  /**
   * Holds the database connection to get the initial data from.
   */
  protected DatabaseConnection databaseConnection = null;

  /**
   * Indexes to add.
   */
  protected Collection<IndexFactory<?>> indexFactories;

  /**
   * Classifier to evaluate.
   */
  protected Classifier<O> algorithm;

  /**
   * Holds the holdout.
   */
  protected Holdout holdout;

  /**
   * Constructor.
   *
   * @param databaseConnection Data source
   * @param indexFactories Data indexes
   * @param algorithm Classification algorithm
   * @param holdout Evaluation holdout
   */
  public ClassifierHoldoutEvaluationTask(DatabaseConnection databaseConnection, Collection<IndexFactory<?>> indexFactories, Classifier<O> algorithm, Holdout holdout) {
    this.databaseConnection = databaseConnection;
    this.indexFactories = indexFactories;
    this.algorithm = algorithm;
    this.holdout = holdout;
  }

  @Override
  public void run() {
    Duration ptime = LOG.newDuration("evaluation.time.load").begin();
    MultipleObjectsBundle allData = databaseConnection.loadData();
    holdout.initialize(allData);
    LOG.statistics(ptime.end());

    Duration time = LOG.newDuration("evaluation.time.total").begin();
    ArrayList<ClassLabel> labels = holdout.getLabels();
    int[][] confusion = new int[labels.size()][labels.size()];
    for(int p = 0; p < holdout.numberOfPartitions(); p++) {
      TrainingAndTestSet partition = holdout.nextPartitioning();
      // Load the data set into a database structure (for indexing)
      Duration dur = LOG.newDuration(this.getClass().getName() + ".fold-" + (p + 1) + ".init.time").begin();
      Database db = new StaticArrayDatabase(new MultipleObjectsBundleDatabaseConnection(partition.getTraining()), indexFactories);
      db.initialize();
      LOG.statistics(dur.end());
      // Train the classifier
      dur = LOG.newDuration(this.getClass().getName() + ".fold-" + (p + 1) + ".train.time").begin();
      Relation<ClassLabel> lrel = db.getRelation(TypeUtil.CLASSLABEL);
      algorithm.buildClassifier(db, lrel);
      LOG.statistics(dur.end());
      // Evaluate the test set
      dur = LOG.newDuration(this.getClass().getName() + ".fold-" + (p + 1) + ".evaluation.time").begin();
      // FIXME: this part is still a big hack, unfortunately!
      MultipleObjectsBundle test = partition.getTest();
      int lcol = AbstractHoldout.findClassLabelColumn(test);
      int tcol = (lcol == 0) ? 1 : 0;
      for(int i = 0, l = test.dataLength(); i < l; ++i) {
        @SuppressWarnings("unchecked")
        O obj = (O) test.data(i, tcol);
        ClassLabel truelbl = (ClassLabel) test.data(i, lcol);
        ClassLabel predlbl = algorithm.classify(obj);
        int pred = Collections.binarySearch(labels, predlbl);
        int real = Collections.binarySearch(labels, truelbl);
        confusion[pred][real]++;
      }
      LOG.statistics(dur.end());
    }
    LOG.statistics(time.end());
    ConfusionMatrix m = new ConfusionMatrix(labels, confusion);
    LOG.statistics(m.toString());
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractApplication.Parameterizer {
    /**
     * Parameter to specify the holdout for evaluation, must extend
     * {@link de.lmu.ifi.dbs.elki.evaluation.classification.holdout.Holdout}.
     */
    public static final OptionID HOLDOUT_ID = new OptionID("evaluation.holdout", "Holdout class used in evaluation.");

    /**
     * Holds the database connection to get the initial data from.
     */
    protected DatabaseConnection databaseConnection = null;

    /**
     * Indexes to add.
     */
    protected Collection<IndexFactory<?>> indexFactories;

    /**
     * Classifier to evaluate.
     */
    protected Classifier<O> algorithm;

    /**
     * Holds the holdout.
     */
    protected Holdout holdout;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Get database connection.
      final ObjectParameter<DatabaseConnection> dbcP = new ObjectParameter<>(AbstractDatabase.Parameterizer.DATABASE_CONNECTION_ID, DatabaseConnection.class, FileBasedDatabaseConnection.class);
      if(config.grab(dbcP)) {
        databaseConnection = dbcP.instantiateClass(config);
      }
      // Get indexes.
      final ObjectListParameter<IndexFactory<?>> indexFactoryP = new ObjectListParameter<>(AbstractDatabase.Parameterizer.INDEX_ID, IndexFactory.class, true);
      if(config.grab(indexFactoryP)) {
        indexFactories = indexFactoryP.instantiateClasses(config);
      }
      ObjectParameter<Classifier<O>> algorithmP = new ObjectParameter<>(AbstractAlgorithm.ALGORITHM_ID, Classifier.class);
      if(config.grab(algorithmP)) {
        algorithm = algorithmP.instantiateClass(config);
      }

      ObjectParameter<Holdout> holdoutP = new ObjectParameter<>(HOLDOUT_ID, Holdout.class, StratifiedCrossValidation.class);
      if(config.grab(holdoutP)) {
        holdout = holdoutP.instantiateClass(config);
      }
    }

    @Override
    protected ClassifierHoldoutEvaluationTask<O> makeInstance() {
      return new ClassifierHoldoutEvaluationTask<O>(databaseConnection, indexFactories, algorithm, holdout);
    }
  }

  /**
   * Runs the classifier evaluation task accordingly to the specified
   * parameters.
   *
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    runCLIApplication(ClassifierHoldoutEvaluationTask.class, args);
  }
}
