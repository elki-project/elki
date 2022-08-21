/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import elki.Algorithm;
import elki.classification.Classifier;
import elki.data.ClassLabel;
import elki.data.type.TypeUtil;
import elki.database.AbstractDatabase;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.datasource.DatabaseConnection;
import elki.datasource.FileBasedDatabaseConnection;
import elki.datasource.MultipleObjectsBundleDatabaseConnection;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.evaluation.classification.ConfusionMatrix;
import elki.evaluation.classification.holdout.Holdout;
import elki.evaluation.classification.holdout.StratifiedCrossValidation;
import elki.evaluation.classification.holdout.TrainingAndTestSet;
import elki.index.IndexFactory;
import elki.logging.Logging;
import elki.logging.statistics.Duration;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectListParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Evaluate a classifier.
 * <p>
 * TODO: split into application and task.
 * <p>
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
  protected Collection<? extends IndexFactory<?>> indexFactories;

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
  public ClassifierHoldoutEvaluationTask(DatabaseConnection databaseConnection, Collection<? extends IndexFactory<?>> indexFactories, Classifier<O> algorithm, Holdout holdout) {
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
      final String fold = this.getClass().getName() + ".fold-" + (p + 1);
      // Load the data set into a database structure (for indexing)
      Duration dur = LOG.newDuration(fold + ".train.init").begin();
      Database db = new StaticArrayDatabase(new MultipleObjectsBundleDatabaseConnection(partition.getTraining()), indexFactories);
      db.initialize();
      LOG.statistics(dur.end());
      // Train the classifier
      dur = LOG.newDuration(fold + ".train.time").begin();
      Relation<ClassLabel> lrel = db.getRelation(TypeUtil.CLASSLABEL);
      algorithm.buildClassifier(db, lrel);
      LOG.statistics(dur.end());

      // Evaluate the test set
      dur = LOG.newDuration(fold + ".test.init").begin();
      Database testdb = new StaticArrayDatabase(new MultipleObjectsBundleDatabaseConnection(partition.getTest()));
      testdb.initialize();
      Relation<O> testdata = testdb.getRelation(algorithm.getInputTypeRestriction()[0]);
      Relation<ClassLabel> testlabels = testdb.getRelation(TypeUtil.CLASSLABEL);
      LOG.statistics(dur.end());
      dur = LOG.newDuration(fold + ".evaluation.time").begin();
      for(DBIDIter iter = testdata.iterDBIDs(); iter.valid(); iter.advance()) {
        ClassLabel predlbl = algorithm.classify(testdata.get(iter));
        ClassLabel truelbl = testlabels.get(iter);
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
  public static class Par<O> extends AbstractApplication.Par {
    /**
     * Parameter to specify the holdout for evaluation, must extend
     * {@link elki.evaluation.classification.holdout.Holdout}.
     */
    public static final OptionID HOLDOUT_ID = new OptionID("evaluation.holdout", "Holdout class used in evaluation.");

    /**
     * Holds the database connection to get the initial data from.
     */
    protected DatabaseConnection databaseConnection;

    /**
     * Indexes to add.
     */
    protected Collection<? extends IndexFactory<?>> indexFactories;

    /**
     * Classifier to evaluate.
     */
    protected Classifier<O> algorithm;

    /**
     * Holds the holdout.
     */
    protected Holdout holdout;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      // Get database connection.
      new ObjectParameter<DatabaseConnection>(AbstractDatabase.Par.DATABASE_CONNECTION_ID, DatabaseConnection.class, FileBasedDatabaseConnection.class) //
          .grab(config, x -> databaseConnection = x);
      // Get indexes.
      new ObjectListParameter<IndexFactory<?>>(AbstractDatabase.Par.INDEX_ID, IndexFactory.class) //
          .setOptional(true) //
          .grab(config, x -> indexFactories = x);
      new ObjectParameter<Classifier<O>>(Algorithm.Utils.ALGORITHM_ID, Classifier.class) //
          .grab(config, x -> algorithm = x);
      new ObjectParameter<Holdout>(HOLDOUT_ID, Holdout.class, StratifiedCrossValidation.class) //
          .grab(config, x -> holdout = x);
    }

    @Override
    public ClassifierHoldoutEvaluationTask<O> make() {
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
