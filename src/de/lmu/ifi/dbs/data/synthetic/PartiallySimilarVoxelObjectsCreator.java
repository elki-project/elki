package de.lmu.ifi.dbs.data.synthetic;

import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

/**
 * Class to provide randomly a collection of voxel-objects exhibiting partial similarities
 * within different subgroups.
 *
 * @author Arthur Zimek
 */
public class PartiallySimilarVoxelObjectsCreator extends AbstractParameterizable{
  public static final String CUBE_SIZE_MIN_P = "cubesizemin";

  public static final String CUBE_SIZE_MIN_D = "minimum size of cube (number of voxels per dimension)";

  public static final String CUBE_SIZE_MAX_P = "cubesizemax";

  public static final String CUBE_SIZE_MAX_D = "maximum size of cube (number of voxels per dimension)";

  public static final String SIMILARITY_SIZE_P = "simsize";

  public static final String SIMILARITY_SIZE_D = "size of similar region";


  public PartiallySimilarVoxelObjectsCreator(String[] parameters) throws ParameterException {
   
	  // TODO constraints ??

    optionHandler.put(CUBE_SIZE_MIN_P, new IntParameter(CUBE_SIZE_MIN_P,CUBE_SIZE_MIN_D));
    
    optionHandler.put(CUBE_SIZE_MAX_P, new IntParameter(CUBE_SIZE_MAX_P,CUBE_SIZE_MAX_D));
    
    optionHandler.put(SIMILARITY_SIZE_P, new IntParameter(SIMILARITY_SIZE_P,SIMILARITY_SIZE_D));

    optionHandler.grabOptions(parameters);
  }

  public String usage(String message) {
    return optionHandler.usage(message);
  }

  public static void main(String[] args) {
    try {
      PartiallySimilarVoxelObjectsCreator creator = new PartiallySimilarVoxelObjectsCreator(args);
      System.out.println(creator.usage(""));
    }
    catch (ParameterException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }    
  }
}
