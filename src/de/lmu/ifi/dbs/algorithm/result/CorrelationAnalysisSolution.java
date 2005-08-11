package de.lmu.ifi.dbs.algorithm.result;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;

import de.lmu.ifi.dbs.linearalgebra.Matrix;

/**
 * A solution of correlation analysis is a matrix of equations describing the
 * dependencies.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class CorrelationAnalysisSolution implements Result
{
    /**
     * Matrix to store the solution equations.
     */
    private Matrix solution;
    
    /**
     * Number format for output accuracy.
     */
    private NumberFormat nf;
    
    /**
     * Provides a new CorrelationAnalysisSolution holding the specified matrix.
     * 
     * Same as {@link #CorrelationAnalysisSolution(Matrix, NumberFormat) CorrelationAnalysisSolution(solution, null)}.
     * 
     * @param solution the matrix describing the solution equations
     */
    public CorrelationAnalysisSolution(Matrix solution)
    {
        this.solution = solution;
        this.nf = null;
    }
    
    /**
     * Provides a new CorrelationAnalysisSolution holding the specified matrix and number format.
     * 
     * @param solution the matrix describing the solution equations
     * @param nf the number format for output accuracy
     */
    public CorrelationAnalysisSolution(Matrix solution, NumberFormat nf)
    {
        this(solution);
        this.nf = nf;
    }

    
    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File)
     */
    public void output(File out)
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
        if(this.nf == null)
        {
            outStream.println(solution.toString());    
        }
        else
        {
            outStream.println(solution.toString(nf));
        }
        outStream.flush();
    }

}
