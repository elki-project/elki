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
        String[] remainingParameters = setParameters(args);
        int inputIndex = -1;
        int outputIndex = -1;
        for(int i = 0; i < remainingParameters.length; i++)
        {
            if(remainingParameters[i].equals(OptionHandler.OPTION_PREFIX+INPUT_P))
            {
                inputIndex = i+1;
            }
            if(remainingParameters[i].equals(OptionHandler.OPTION_PREFIX+OUTPUT_P))
            {
                outputIndex = i+1;
            }
        }
        if(inputIndex < 0 || inputIndex >= remainingParameters.length)
        {
            throw new IllegalArgumentException("Invalid parameter array: value of "+INPUT_P+" out of range.");
        }
        if(outputIndex < 0 || outputIndex >= remainingParameters.length)
        {
            throw new IllegalArgumentException("Invalid parameter array: value of "+OUTPUT_P+" out of range.");
        }
        
        File inputDir = new File(remainingParameters[inputIndex]);
        if(!inputDir.isDirectory())
        {
            throw new IllegalArgumentException(remainingParameters[inputIndex]+" is not a directory");
        }
        File[] inputFiles = inputDir.listFiles();
        for(File inputFile : inputFiles)
        {
            try
            {
                String[] parameterCopy = Util.copy(remainingParameters);
                parameterCopy[inputIndex] = remainingParameters[inputIndex]+File.separator+inputFile.getName();
                parameterCopy[outputIndex] = remainingParameters[outputIndex]+File.separator+inputFile.getName();
                
                Wrapper newWrapper = wrapper.getClass().newInstance();
                newWrapper.run(parameterCopy);
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
