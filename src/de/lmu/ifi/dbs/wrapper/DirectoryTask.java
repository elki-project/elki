package de.lmu.ifi.dbs.wrapper;


import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper to run another wrapper for all files in the directory given as input.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class DirectoryTask extends AbstractWrapper implements Parameterizable
{
    public static final String WRAPPER_P = "wrapper";
    
    public static final String WRAPPER_D = "<class>wrapper to run over all files in a specified directory";

    private Wrapper wrapper;
    
    private OptionHandler directoryWrapperOptionHandler;
    
    public DirectoryTask()
    {
        Map<String,String> ptd = new HashMap<String,String>();
        ptd.put(WRAPPER_P+OptionHandler.EXPECTS_VALUE, WRAPPER_D);
        directoryWrapperOptionHandler = new OptionHandler(ptd, this.getClass().getName());        
    }
    
    
    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = directoryWrapperOptionHandler.grabOptions(args);
        try
        {
            wrapper = (Wrapper) Class.forName(directoryWrapperOptionHandler.getOptionValue(WRAPPER_P)).newInstance();
            
        }
        catch(UnusedParameterException e)
        {
            throw new IllegalArgumentException("Parameter "+WRAPPER_P+" required.");
        }
        catch(NoParameterValueException e)
        {
            throw new IllegalArgumentException("Value for parameter "+WRAPPER_P+" required.");
        }
        catch(InstantiationException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(IllegalAccessException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(ClassNotFoundException e)
        {
            throw new IllegalArgumentException(e);
        }
        return remainingParameters;
    }


    /**
     * Runs the specified wrapper with given arguiments for all files in directory given as input.
     * 
     * @see de.lmu.ifi.dbs.wrapper.Wrapper#run(java.lang.String[])
     */
    public void run(String[] args)
    {
        setParameters(args);
        int input = -1;
        int output = -1;
        for(int i = 0; i < args.length; i++)
        {
            if(args[i].equals(OptionHandler.OPTION_PREFIX+INPUT_P))
            {
                input = i+1;
            }
            if(args[i].equals(OptionHandler.OPTION_PREFIX+OUTPUT_P))
            {
                output = i+1;
            }
        }
        if(input < 0 || input >= args.length)
        {
            throw new IllegalArgumentException("Invalid parameter array: value of "+INPUT_P+" out of range.");
        }
        if(output < 0 || output >= args.length)
        {
            throw new IllegalArgumentException("Invalid parameter array: value of "+OUTPUT_P+" out of range.");
        }
        File inputDir = new File(args[input]);
        if(!inputDir.isDirectory())
        {
            throw new IllegalArgumentException(args[input]+" is not a directory");
        }
        File[] inputFiles = inputDir.listFiles();
        for(File inputFile : inputFiles)
        {
            try
            {
                String[] parameterCopy = Util.copy(args);
                parameterCopy[input] = parameterCopy[input]+File.separator+inputFile.getName();
                parameterCopy[output] = parameterCopy[output]+File.separator+inputFile.getName();
                wrapper.run(parameterCopy);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        DirectoryTask task = new DirectoryTask();
        task.run(args);        
    }

}
