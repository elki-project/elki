package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.PrintStream;
import java.util.BitSet;
import java.util.List;

/**
 * @author Arthur Zimek
 */
public class SubspaceClusterModel<V extends RealVector<V, ?>> extends AbstractResult<V>
{
    private BitSet attributes;

    /**
     * 
     * @param db
     * @param attributes the relevant attributes for this SubspaceClusterModel (first attribute is counted as number 0)
     */
    public SubspaceClusterModel(Database<V> db, BitSet attributes)
    {
        super(db);
        this.attributes = attributes;
    }
    
    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.PrintStream, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
     */
    public void output(PrintStream outStream, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException
    {
        outStream.println("### " + this.getClass().getSimpleName() + ":");
        outStream.println("### relevant attributes (counting starts with 0): "+this.attributes.toString());
        outStream.println("################################################################################");
        outStream.flush();
    }

    /**
     * Returns the relevant attributes for this SubspaceClusterModel.
     * 
     * First attribute counts as number 0.
     * 
     * @return the relevant attributes for this SubspaceClusterModel
     */
    public BitSet getAttributes()
    {
        return this.attributes;
    }

}
