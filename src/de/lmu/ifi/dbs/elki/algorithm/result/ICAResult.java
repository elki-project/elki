package de.lmu.ifi.dbs.elki.algorithm.result;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.varianceanalysis.ica.FastICA;

import java.io.PrintStream;
import java.util.List;

/**
 * TODO: comment
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector handled by this Result
 */
public class ICAResult<V extends RealVector<V, ?>> extends AbstractResult<V> {
    /**
     * The independent component analysis.
     */
    private FastICA<V> ica;

    /**
     * todo
     *
     * @param db
     * @param ica
     */
    public ICAResult(Database<V> db, FastICA<V> ica) {
        super(db);
        this.ica = ica;
    }

    /**
     * Writes the clustering result to the given stream.
     * todo
     *
     * @param outStream     the stream to write to
     * @param normalization Normalization to restore original values according to, if this action is supported
     *                      - may remain null.
     * @param settings      the settings to be written into the header, if this parameter is <code>null</code>,
     *                      no header will be written
     * @throws de.lmu.ifi.dbs.elki.utilities.UnableToComplyException
     *          if any feature vector is not compatible with values initialized during normalization
     */
    public void output(PrintStream outStream, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        StringBuffer msg = new StringBuffer();

        Matrix mix = ica.getMixingMatrix().copy();
        mix.normalizeColumns();

        Matrix sep = ica.getSeparatingMatrix().copy();
        sep.normalizeColumns();


        msg.append("\nMixing matrix\n").append(ica.getMixingMatrix());
        msg.append("\nSeparating matrix\n").append(ica.getSeparatingMatrix());
        msg.append("\nWeight matrix\n").append(ica.getWeightMatrix());
        msg.append("\nNormalo Mixing matrix\n").append(mix);
        msg.append("\nNormalo Sep matrix\n").append(sep);

        return msg.toString();
    }
}
