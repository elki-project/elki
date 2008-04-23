package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.clustering.clique.Subspace;
import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.PrintStream;
import java.util.List;

/**
 * Represents a cluster model for a cluster in the CLIQUE algorithm. Provides information
 * of the subspace of the cluster.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CLIQUEModel<V extends RealVector<V, ?>> extends AbstractResult<V> {
  /**
   * The supspace of this model.
   */
  private Subspace<V> subspace;

  /**
   * Creates a new cluster model for a cluster in the CLIQUE algorithm
   * with the specified parameters.
   * @param db       the database containing the objects of this model
   * @param subspace the supspace of this model
   */
  public CLIQUEModel(Database<V> db, Subspace<V> subspace) {
    super(db);
    this.subspace = subspace;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.PrintStream,de.lmu.ifi.dbs.normalization.Normalization,java.util.List)
   */
  public void output(PrintStream outStream,
                     Normalization<V> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException {
    outStream.println("### " + this.getClass().getSimpleName() + ":");
    outStream.println(this.subspace.toString("### "));
    outStream.println("################################################################################");
    outStream.flush();
  }

}
