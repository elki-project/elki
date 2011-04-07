package de.lmu.ifi.dbs.elki.application;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Provides a KDDCLIApplication that can be used to perform any algorithm
 * implementing {@link Algorithm Algorithm} using any DatabaseConnection
 * implementing
 * {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection
 * DatabaseConnection}.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.composedOf KDDTask
 * 
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class KDDCLIApplication<O extends DatabaseObject> extends AbstractApplication {
  /**
   * The KDD Task to perform.
   */
  KDDTask<O> task;

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param task Task to run
   */
  public KDDCLIApplication(boolean verbose, KDDTask<O> task) {
    super(verbose);
    this.task = task;
  }

  @Override
  public void run() {
    task.run();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject> extends AbstractApplication.Parameterizer {
    /**
     * The KDD Task to perform.
     */
    protected KDDTask<O> task;

    @SuppressWarnings("unchecked")
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      task = config.tryInstantiate(KDDTask.class);
    }

    @Override
    protected KDDCLIApplication<O> makeInstance() {
      return new KDDCLIApplication<O>(verbose, task);
    }
  }

  /**
   * Runs a KDD task accordingly to the specified parameters.
   * 
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    runCLIApplication(KDDCLIApplication.class, args);
  }
}