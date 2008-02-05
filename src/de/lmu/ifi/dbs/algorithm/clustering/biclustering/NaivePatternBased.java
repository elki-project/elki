package de.lmu.ifi.dbs.algorithm.clustering.biclustering;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Associations;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

/**
 * @author Arthur Zimek
 */
public class NaivePatternBased extends AbstractBiclustering<DoubleVector>
{
    private int newColumn(int firstCol, int secondCol)
    {
        return firstCol * (firstCol + 1) / 2 + firstCol * (getColDim() - firstCol - 1) + (secondCol - firstCol - 1);
    }
    
    private int[] oldColumns(int newColumn)
    {
        int[] oldColumns = {};
        return oldColumns;
    }

    @Override
    protected void biclustering() throws IllegalStateException
    {
        Database<DoubleVector> database = new SequentialDatabase<DoubleVector>();
        for(int row = 0; row < getRowDim(); row++)
        {
            int dim = getColDim();
            dim *= dim - 1;
            dim /= 2;
            double[] values = new double[dim];
            int dimIndex = 0;
            for(int col = 0; col < getColDim() - 1; col++)
            {
                for(int col2 = col + 1; col2 < getColDim(); col2++)
                {
                    int index = newColumn(col, col2);
                    assert index == dimIndex : "expected: "+dimIndex+", found: "+index;
                    values[dimIndex] = valueAt(row, col) - valueAt(row, col2);
                    dimIndex++;
                }
            }
            Associations association = new Associations();
            association.put(AssociationID.ROW_ID, row);
            ObjectAndAssociations<DoubleVector> oa = new ObjectAndAssociations<DoubleVector>(new DoubleVector(values),association);
            try
            {
                database.insert(oa);
            }
            catch(UnableToComplyException e)
            {
                exception(e.getMessage(), e);
            }
        }
        
        // TODO Auto-generated method stub
        
    }

    public Description getDescription()
    {
        return new Description("Naive Pattern Based", "", "Efficient complete search for biclusters","");
    }

}
