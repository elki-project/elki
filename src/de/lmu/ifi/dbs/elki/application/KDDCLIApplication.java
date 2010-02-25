package de.lmu.ifi.dbs.elki.application;

import de.lmu.ifi.dbs.elki.KDDTask;
import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Provides a KDDCLIApplication that can be used to perform any algorithm implementing
 * {@link Algorithm Algorithm} using any DatabaseConnection implementing
 * {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection
 * DatabaseConnection}.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObjects handled by this Algorithm
 */
public class KDDCLIApplication<O extends DatabaseObject> extends AbstractApplication {
  /**
   * The KDD Task to perform.
   */
  KDDTask<O> task;

  /**
   * Provides a KDDCLIApplication.
   */
  public KDDCLIApplication(Parameterization config) {
    super(config);
    task = new KDDTask<O>(config);
  }

  /**
   * Runs a KDD task accordingly to the specified parameters.
   * 
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    runCLIApplication(KDDCLIApplication.class, args);
  }

  @SuppressWarnings("unused")
  @Override
  public void run() throws UnableToComplyException {
    task.run();
  }
}