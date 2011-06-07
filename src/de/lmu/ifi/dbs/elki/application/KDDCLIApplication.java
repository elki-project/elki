package de.lmu.ifi.dbs.elki.application;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.workflow.OutputStep;

/**
 * Provides a KDDCLIApplication that can be used to perform any algorithm
 * implementing {@link Algorithm Algorithm} using any DatabaseConnection
 * implementing
 * {@link de.lmu.ifi.dbs.elki.datasource.DatabaseConnection
 * DatabaseConnection}.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.composedOf KDDTask
 */
public class KDDCLIApplication extends AbstractApplication {
  /**
   * The KDD Task to perform.
   */
  KDDTask task;

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param task Task to run
   */
  public KDDCLIApplication(boolean verbose, KDDTask task) {
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
  public static class Parameterizer extends AbstractApplication.Parameterizer {
    /**
     * The KDD Task to perform.
     */
    protected KDDTask task;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      task = config.tryInstantiate(KDDTask.class);
    }

    @Override
    protected KDDCLIApplication makeInstance() {
      return new KDDCLIApplication(verbose, task);
    }
  }

  /**
   * Runs a KDD task accordingly to the specified parameters.
   * 
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    OutputStep.setDefaultHandlerWriter();
    runCLIApplication(KDDCLIApplication.class, args);
  }
}