package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.algorithm.result.ICAResult;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.varianceanalysis.ica.FastICA;

import java.util.List;

/**
 * Provides an Implementation of the Fast ICA Algorithm.
 * <p>Reference:
 * <br>A. Hyvaerinen, J. Karhunen, E. Oja:
 * Independent Component Analysis, John Wiley & Sons, 2001.
 * </p>
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
    private FastICA<V> ica = new FastICA<V>();

    /**
     * Provides an ICA algorithm.
     */
    public ICA() {
        super();
        //this.debug = true;
    }

    /**
     * Performs the ICA algorithm on the given database.
     *
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#run(de.lmu.ifi.dbs.elki.database.Database)
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
     * Calls the super method
     * The remaining parameters are passed to the {@link #ica}.
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // ica
        remainingParameters = ica.setParameters(remainingParameters);
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the {@link #ica}.
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(ica.getAttributeSettings());
        return attributeSettings;
    }

    /**
     * Calls the super method
     * and appends the parameter description of {@link #ica}.
     */
    @Override
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());
        description.append(ica.parameterDescription());
        return description.toString();
    }
}
