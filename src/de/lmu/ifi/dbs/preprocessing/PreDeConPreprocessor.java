package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.pca.LinearLocalPCA;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;

/**
 * Preprocessor for PreDeCon projected dimension assignment to objects of a certain
 * database.
 * 
 * @author Peer Kr&ouml;ger (<a
 *         href="mailto:kroegerp@dbs.ifi.lmu.de">kroegerp@dbs.ifi.lmu.de</a>)
 */
public class PreDeConPreprocessor extends VarianceAnalysisPreprocessor
{

    /**
     * The parameter settings for the PCA.
     */
    private String[] pcaParameters;

    /**
     * Provides a new Preprocessor that computes the correlation dimension of
     * objects of a certain database.
     */
    public PreDeConPreprocessor()
    {
        super();
    }

    protected void runSpecialVarianceAnalysis(Integer id, List<Integer> ids, Database<RealVector> database)
    {
 
            LinearLocalPCA pca = new LinearLocalPCA();
            try
            {
                pca.setParameters(pcaParameters);
            } catch (ParameterException e)
            {
                // tested before
                throw new RuntimeException("This should never happen!");
            }
            pca.run4CPCA(ids, database, delta);

            database.associate(AssociationID.LOCAL_PCA, id, pca);
            database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pca
                    .getSimilarityMatrix());
    }

    /**
     * Sets the values for the parameters alpha, pca and pcaDistancefunction if
     * specified. If the parameters are not specified default values are set.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);

        // save parameters for pca
        LinearLocalPCA tmpPCA = new LinearLocalPCA();

        remainingParameters = tmpPCA.setParameters(remainingParameters);

        pcaParameters = tmpPCA.getParameters();
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Returns the parameter setting of the attributes.
     * 
     * @return the parameter setting of the attributes
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();

        LinearLocalPCA tmpPCA = new LinearLocalPCA();
        try
        {
            tmpPCA.setParameters(pcaParameters);
        } catch (ParameterException e)
        {
            // tested before
            throw new RuntimeException("This should never happen!");
        }
        attributeSettings.addAll(tmpPCA.getAttributeSettings());

        return attributeSettings;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(PreDeConPreprocessor.class.getName());
        description
                .append(" computes the projected dimension of objects of a certain database according to the PreDeCon algorithm.\n");
        description.append("The variance analysis is based on epsilon range queries.\n");
        description.append(optionHandler.usage("", false));
        return description.toString();
    }

}
