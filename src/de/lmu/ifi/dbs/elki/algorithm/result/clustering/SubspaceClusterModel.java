package de.lmu.ifi.dbs.elki.algorithm.result.clustering;

import de.lmu.ifi.dbs.elki.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

import java.io.PrintStream;
import java.util.BitSet;
import java.util.List;

/**
 * todo arthur comment
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Result
 */
public class SubspaceClusterModel<V extends RealVector<V, ?>> extends AbstractResult<V> {
    private BitSet attributes;

    /**
     * @param db
     * @param attributes the relevant attributes for this SubspaceClusterModel (first attribute is counted as number 0)
     */
    public SubspaceClusterModel(Database<V> db, BitSet attributes) {
        super(db);
        this.attributes = attributes;
    }

    public void output(PrintStream outStream, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        outStream.println("### " + this.getClass().getSimpleName() + ":");
        outStream.println("### relevant attributes (counting starts with 0): " + this.attributes.toString());
        outStream.println("################################################################################");
        outStream.flush();
    }

    /**
     * Returns the relevant attributes for this SubspaceClusterModel.
     * <p/>
     * First attribute counts as number 0.
     *
     * @return the relevant attributes for this SubspaceClusterModel
     */
    public BitSet getAttributes() {
        return this.attributes;
    }

}
