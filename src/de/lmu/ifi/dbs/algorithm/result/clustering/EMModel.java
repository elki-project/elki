package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.PrintStream;
import java.util.List;

/**
 * // todo arthur comment
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Result
 */
public class EMModel<V extends RealVector<V, ?>> extends AbstractResult<V> {
    private V mean;

    private Matrix covarianceMatrix;

    /**
     * todo comment
     *
     * @param db
     * @param mean
     * @param covarianceMatrix
     */
    public EMModel(Database<V> db, V mean, Matrix covarianceMatrix) {
        super(db);
        this.mean = mean;
        this.covarianceMatrix = covarianceMatrix;
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.PrintStream,de.lmu.ifi.dbs.normalization.Normalization,java.util.List)
     */
    public void output(PrintStream outStream, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        outStream.println("### " + this.getClass().getSimpleName() + ":");
        outStream.println("### mean = ( " + this.mean.toString() + " )");
        outStream.println("### covariance matrix = \n" + this.covarianceMatrix.toString("###    "));
        outStream.println("################################################################################");
        outStream.flush();
    }

}
