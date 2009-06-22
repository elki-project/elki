package de.lmu.ifi.dbs.elki.data.synthetic;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Class to provide randomly a collection of voxel-objects exhibiting partial similarities
 * within different subgroups.
 *
 * @author Arthur Zimek
 * todo parameter
 */
public class PartiallySimilarVoxelObjectsCreator extends AbstractParameterizable{
  /**
   * OptionID for {@link #CUBE_SIZE_MIN_PARAM}
   */
  public static final OptionID CUBE_SIZE_MIN_ID = OptionID.getOrCreateOptionID("cubesizemin",
      "minimum size of cube (number of voxels per dimension)");

  /**
   * Option for minimum cube size
   */
  // TODO: constraints
  private final IntParameter CUBE_SIZE_MIN_PARAM = new IntParameter(CUBE_SIZE_MIN_ID);

  /**
   * OptionID for {@link #CUBE_SIZE_MAX_PARAM}
   */
  public static final OptionID CUBE_SIZE_MAX_ID = OptionID.getOrCreateOptionID("cubesizemax",
      "maximum size of cube (number of voxels per dimension)");

  /**
   * Option for maximum cube size
   */
  // TODO: constraints
  private final IntParameter CUBE_SIZE_MAX_PARAM = new IntParameter(CUBE_SIZE_MAX_ID);

  /**
   * OptionID for {@link #SIMILARITY_SIZE_PARAM}
   */
  public static final OptionID SIMILARITY_SIZE_ID = OptionID.getOrCreateOptionID("simsize",
      "size of similar region");

  /**
   * Option for similarity size
   */
  // TODO: constraints
  private final IntParameter SIMILARITY_SIZE_PARAM = new IntParameter(SIMILARITY_SIZE_ID);

  /**
   * @param parameters
   * @throws ParameterException
   */
  public PartiallySimilarVoxelObjectsCreator(String[] parameters) throws ParameterException {
    addOption(CUBE_SIZE_MIN_PARAM);
    addOption(CUBE_SIZE_MAX_PARAM);
    addOption(SIMILARITY_SIZE_PARAM);

    // FIXME: deprecated API use?
    optionHandler.grabOptions(parameters);
  }

  // FIXME: unify main method with other wrappers?
  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      new PartiallySimilarVoxelObjectsCreator(args);
    }
    catch (ParameterException e) {
      // todo exception werfen
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }    
  }
}
