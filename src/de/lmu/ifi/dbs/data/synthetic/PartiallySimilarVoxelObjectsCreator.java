package de.lmu.ifi.dbs.data.synthetic;

import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.Hashtable;
import java.util.Map;

/**
 * Class to provide randomly a collection of voxel-objects exhibiting partial similarities
 * within different subgroups.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class PartiallySimilarVoxelObjectsCreator
{
    public static final String CUBE_SIZE_MIN_P = "cubesizemin";
    
    public static final String CUBE_SIZE_MIN_D = "<int>minimum size of cube (number of voxels per dimension)";
    
    public static final String CUBE_SIZE_MAX_P = "cubesizemax";
    
    public static final String CUBE_SIZE_MAX_D = "<int>maximum size of cube (number of voxels per dimension)";
    
    public static final String SIMILARITY_SIZE_P = "simsize";
    
    public static final String SIMILARITY_SIZE_D = "<int>size of similar region";
    
    
    private OptionHandler optionHandler;
    
    public PartiallySimilarVoxelObjectsCreator(String[] parameters) throws NoParameterValueException
    {
        Map<String,String> parameterToDescription = new Hashtable<String,String>();
        parameterToDescription.put(CUBE_SIZE_MIN_P+OptionHandler.EXPECTS_VALUE, CUBE_SIZE_MIN_D);
        parameterToDescription.put(CUBE_SIZE_MAX_P+OptionHandler.EXPECTS_VALUE, CUBE_SIZE_MAX_D);
        parameterToDescription.put(SIMILARITY_SIZE_P+OptionHandler.EXPECTS_VALUE, SIMILARITY_SIZE_D);
        
        optionHandler = new OptionHandler(parameterToDescription, "java "+this.getClass().getName());
        optionHandler.grabOptions(parameters);
    }
    
    public String usage(String message)
    {
        return optionHandler.usage(message);
    }
    
    public static void main(String[] args)
    {
        PartiallySimilarVoxelObjectsCreator creator = new PartiallySimilarVoxelObjectsCreator(args);
        System.out.println(creator.usage(""));
    }
}
