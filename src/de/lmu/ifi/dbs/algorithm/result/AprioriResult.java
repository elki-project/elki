package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.BitVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Stores a apriori result.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class AprioriResult implements Result<BitVector>
{
    public static final NumberFormat numberFormat = NumberFormat.getPercentInstance();
    static
    {
        int fractionDigits = 4;
        numberFormat.setMaximumFractionDigits(fractionDigits);
        numberFormat.setMinimumFractionDigits(fractionDigits);
    }
    
    /**
     * The frequent itemsets.
     */
    private List<BitSet> solution;
    
    /**
     * The frequencies of all itemsets.
     */
    private Map<BitSet, Integer> frequencies;
    
    /**
     * The database, where the itemsets have been evaluated.
     */
    private Database<BitVector> database;
    
    
    /**
     * Provides a apriori result.
     * 
     * @param solution the frequent itemsets
     * @param frequencies the frequencies of all itemsets
     * @param database the database, where the itemsets have been evaluated
     */
    public AprioriResult(List<BitSet> solution, Map<BitSet, Integer> frequencies, Database<BitVector> database)
    {
        this.solution = solution;
        this.frequencies = frequencies;
        this.database = database;
    }

    /**
     * Prints the frequent itemsets annotating their reqpective frequency.
     * Parameter normalization will remain unused.
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization)
     */
    public void output(File out, Normalization<BitVector> normalization) throws UnableToComplyException
    {
        PrintStream outStream;
        try
        {
            outStream = new PrintStream(new FileOutputStream(out));
        }
        catch(Exception e)
        {
            outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        int dbsize = database.size();
        for(BitSet bitSet : solution)
        {
            int frq = frequencies.get(bitSet);
            outStream.print(bitSet.toString());
            outStream.print(SEPARATOR);
            outStream.print("(frequency: ");
            outStream.print(frq);
            outStream.print(" [");
            outStream.print(numberFormat.format((double) frq / dbsize));
            outStream.println("])");
        }
        outStream.flush();
    }

}
