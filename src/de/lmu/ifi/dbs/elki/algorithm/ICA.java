package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.algorithm.result.ICAResult;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.varianceanalysis.ica.FastICA;

/**
 * Provides an ICA algorithm.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector handled by this Algorithm
 */
public class ICA<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> {
    /**
     * The result.
     */
    private ICAResult<V> result;

    /**
     * The independent component analysis.
     */
    private FastICA<V> ica;

    /**
     * Provides an ICA algorithm.
     */
    public ICA() {
        super();
        //this.debug = true;
    }

    /**
     * @see AbstractAlgorithm#runInTime(de.lmu.ifi.dbs.elki.database.Database)
     */
    protected void runInTime(Database<V> database) throws IllegalStateException {
        ica.run(database, isVerbose());
        result = new ICAResult<V>(database, ica);
        if (debug) {
            debugFine(result.toString());
        }
    }

    /**
     * @see Algorithm#getResult()
     */
    public Result<V> getResult() {
        return result;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description("ICA",
            "Independent Component Analysis",
            "Implementation of the Fast ICA Algorithm",
            "A. Hyvaerinen, J. Karhunen, E. Oja: Independent Component Analysis, John Wiley & Sons, 2001");
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // ica
        ica = new FastICA<V>();
        remainingParameters = ica.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }
}
