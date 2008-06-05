package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.BitVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

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
 * @author Arthur Zimek
 */
public class AprioriResult extends AbstractResult<BitVector> {
    /**
     * Number Format for output purposes.
     */
    private static final NumberFormat numberFormat = NumberFormat.getPercentInstance();

    static {
        int fractionDigits = 4;
        numberFormat.setMaximumFractionDigits(fractionDigits);
        numberFormat.setMinimumFractionDigits(fractionDigits);
    }

    /**
     * The frequent itemsets.
     */
    private List<BitSet> solution;

    /**
     * The supports of all itemsets.
     */
    private Map<BitSet, Integer> supports;

    /**
     * Provides a apriori result.
     *
     * @param solution the frequent itemsets
     * @param supports the supports of all itemsets
     * @param database the database, where the itemsets have been evaluated
     */
    public AprioriResult(List<BitSet> solution, Map<BitSet, Integer> supports, Database<BitVector> database) {
        super(database);
        this.solution = solution;
        this.supports = supports;
    }

    /**
     * Prints the frequent itemsets annotating their reqpective frequency.
     * Parameter normalization will remain unused.
     *
     * @see Result#output(File,Normalization,List)
     */
    @Override
    public void output(File out, Normalization<BitVector> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        PrintStream outStream;
        try {
            outStream = new PrintStream(new FileOutputStream(out));
        }
        catch (Exception e) {
            outStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
        }
        output(outStream, normalization, settings);
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.PrintStream,de.lmu.ifi.dbs.normalization.Normalization,java.util.List)
     */
    public void output(PrintStream outStream, Normalization<BitVector> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        writeHeader(outStream, settings, null);

        int dbsize = db.size();
        for (BitSet bitSet : solution) {
            int frq = supports.get(bitSet);
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

    /**
     * Returns the frequent item sets.
     *
     * @return the frequent item sets.
     */
    public List<BitSet> getSolution() {
        return solution;
    }

    /**
     * Returns the frequencies of the frequent item sets.
     *
     * @return the frequencies of the frequent item sets
     */
    public Map<BitSet, Integer> getSupports() {
        return supports;
    }

}
