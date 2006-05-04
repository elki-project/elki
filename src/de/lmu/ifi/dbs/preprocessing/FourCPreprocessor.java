package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.*;

/**
 * Preprocessor for 4C correlation dimension assignment to objects of a certain
 * database.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCPreprocessor extends VarianceAnalysisPreprocessor
{

    /**
     * The parameter settings for the PCA.
     */
    private String[] pcaParameters;

    /**
     * Provides a new Preprocessor that computes the correlation dimension of
     * objects of a certain database.
     */
    public FourCPreprocessor()
    {
        super();
    }

    /**
     * This method perfoms the variance analysis of a given point w.r.t. a given reference set and a database.
     * This variance analysis is done by PCA applied to the reference set.
     * 
     * @param id        the point
     * @param ids       the reference set
     * @param database  the database
     */
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

            database.associate(AssociationID.LOCAL_DIMENSIONALITY, id, pca.getCorrelationDimension());
            database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pca.getSimilarityMatrix());
    }

    /**
     * Sets the values for the parameters alpha, varianceanalysis and pcaDistancefunction if
     * specified. If the parameters are not specified default values are set.
     *
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);

        // save parameters for varianceanalysis
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
        description.append(FourCPreprocessor.class.getName());
        description
                .append(" computes the correlation dimension of objects of a certain database according to the 4C algorithm.\n");
        description.append("The PCA is based on epsilon range queries.\n");
        description.append(optionHandler.usage("", false));
        return description.toString();
    }

}
