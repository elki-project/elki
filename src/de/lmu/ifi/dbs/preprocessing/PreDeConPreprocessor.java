package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.Matrix;

import java.util.*;

/**
 * Preprocessor for 4C correlation dimension assignment to objects of a certain
 * database.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PreDeConPreprocessor extends VarianceAnalysisPreprocessor
{

    /*
     * The kapp value for generating the variance vector.
     */
    private final int kappa = 50;
    
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

    /**
     * This method perfoms the variance analysis of a given point w.r.t. a given reference set and a database.
     * The varainace analysis is done by exploring the variance of each dimension of the reference set
     * 
     * @param id        the point
     * @param ids       the reference set
     * @param database  the database
     */
    protected void runSpecialVarianceAnalysis(Integer id, List<Integer> ids, Database<RealVector> database)
    {
        int referenceSetSize = ids.size();
        RealVector obj = database.get(id);
        
        if(referenceSetSize == 0)
        {
            throw new RuntimeException("Reference Set Size = 0. This should never happen!");
        }
        
        // prepare similarity matrix
        int dim = obj.getDimensionality();
        Matrix simMatrix = new Matrix(dim, dim, 0);
        for(int i=0; i<dim; i++)
        {
            simMatrix.set(i,i,1);
        }
        
        // prepare projected dimensionality
        int projDim = 0;
        
        // start variance analyis
        double[] sum = new double[dim];
        ListIterator<Integer> nIter = ids.listIterator();
        while(nIter.hasNext())
        {
            RealVector neighbor = database.get(nIter.next());
            for(int d=0;d<dim;d++)
            {
                sum[d] =+ Math.pow(obj.getValue(d+1).doubleValue() - neighbor.getValue(d+1).doubleValue(), 2.0);
            }
        }

        for(int d=0; d<dim; d++)
        {
            if(Math.sqrt(sum[d])/referenceSetSize <= delta)
            {
                projDim++;
                simMatrix.set(d, d, kappa);
            }
        }
    
        if(projDim == 0)
        {
            projDim = dim;
        }
        
        // set the associations
        database.associate(AssociationID.LOCAL_DIMENSIONALITY, id, new Integer(projDim));
        database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, simMatrix);
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(PreDeConPreprocessor.class.getName());
        description.append(" computes the projected dimension of objects of a certain database according to the PreDeCon algorithm.\n");
        description.append("The variance analysis is based on epsilon range queries.\n");
        description.append(optionHandler.usage("", false));
        return description.toString();
    }

}
