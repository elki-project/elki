package de.lmu.ifi.dbs.elki.algorithm.result.clustering;

import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique.CLIQUESubspace;
import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

import java.io.PrintStream;
import java.util.List;

/**
 * Represents a cluster model for a cluster in the CLIQUE algorithm. Provides information
 * of the subspace of the cluster.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector handled by this Result
 */
public class CLIQUEModel<V extends RealVector<V, ?>> extends AbstractResult<V> {
    /**
     * The supspace of this model.
     */
    private CLIQUESubspace<V> subspace;

    /**
     * Creates a new cluster model for a cluster in the CLIQUE algorithm
     * with the specified parameters.
     *
     * @param db       the database containing the objects of this model
     * @param subspace the supspace of this model
     */
    public CLIQUEModel(Database<V> db, CLIQUESubspace<V> subspace) {
        super(db);
        this.subspace = subspace;
    }

    public void output(PrintStream outStream,
                       Normalization<V> normalization,
                       List<AttributeSettings> settings) throws UnableToComplyException {
        outStream.println("### " + this.getClass().getSimpleName() + ":");
        outStream.println(this.subspace.toString("### "));
        outStream.println("################################################################################");
        outStream.flush();
    }

    /**
     * Returns the subspace of this model.
     *
     * @return the subspace of this model
     */
    public CLIQUESubspace<V> getSubspace() {
        return subspace;
    }
}
