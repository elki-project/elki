package de.lmu.ifi.dbs.utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

/**
 * @version 0.1
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class OPTICSCorDimFilter
{
    
    public static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public final String LAMBDA_P = "lambda";
    
    public final String LAMBDA_D = "<int>correlation dimension";
    
    public final int WHICH_DEFAULT = 1;
    
    public final String WHICH_P = "number";
    
    public final String WHICH_D = "<int>number of cluster of dimensionality lambda in OPTICS result (default: "+WHICH_DEFAULT+")";
    
    public final String IN_P = "in";
    
    public final String IN_D = "<file>input file";
    
    public final String OUT_P = "out";
    
    public final String OUT_D = "<file>output file (default: System.out)";
    
    private int lambda;
    
    private int which = WHICH_DEFAULT;
    
    private PrintWriter out;
    
    private OptionHandler optionHandler;
    
    public OPTICSCorDimFilter(String[] args) throws IllegalArgumentException
    {
        Map parameterToDescription = new Hashtable();
        parameterToDescription.put(LAMBDA_P+OptionHandler.EXPECTS_VALUE,LAMBDA_D);
        parameterToDescription.put(WHICH_P+OptionHandler.EXPECTS_VALUE,WHICH_D);
        parameterToDescription.put(IN_P+OptionHandler.EXPECTS_VALUE,IN_D);
        parameterToDescription.put(OUT_P+OptionHandler.EXPECTS_VALUE,OUT_D);
        this.optionHandler = new OptionHandler(parameterToDescription, "java "+this.getClass().getName());
        this.optionHandler.grabOptions(args);
        try
        {
            if(optionHandler.isSet(WHICH_P))
            {
                which = Integer.parseInt(optionHandler.getOptionValue(WHICH_P));
            }
            
            if(optionHandler.isSet(OUT_P))
            {
                out = new PrintWriter(new FileWriter(optionHandler.getOptionValue(OUT_P)));
            }
            else
            {
                out = new PrintWriter(System.out);
            }
            lambda = Integer.parseInt(optionHandler.getOptionValue(LAMBDA_P));
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException(optionHandler.usage(e.getMessage()));
        }
    }
    
    public void filter() throws IllegalArgumentException
    {
        int number = 1;
        boolean incremented = true;
        try
        {
            BufferedReader in = new BufferedReader(new FileReader(optionHandler.getOptionValue(IN_P)));
            for(String line; (line = in.readLine()) != null;)
            {
                if(!line.startsWith("#"))
                {
                    String[] entries = WHITESPACE.split(line);
                    if(entries.length > 2)
                    {
                        int cordim = Integer.parseInt(entries[entries.length-2].substring(1));

                        if(cordim <= lambda && number == which)
                        {
                            incremented = false;
                            out.println(line);
                        }
                        else if(cordim > lambda && !incremented)
                        {
                            incremented = true;
                            number++;
                        }
                    }
                }
            }
            out.flush();
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException(optionHandler.usage(e.getMessage()));
        }
        
    }
    
    public static void main(String[] args) throws Exception
    {
        try
        {
            OPTICSCorDimFilter filter = new OPTICSCorDimFilter(args);
            filter.filter();            
        }
        catch(IllegalArgumentException e)
        {
            System.err.println(e.getMessage());
        }
    }
}
