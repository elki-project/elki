package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.rstar.FlatRTree;
import de.lmu.ifi.dbs.index.spatial.rstar.RTree;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.List;

/**
 * RTreeDatabase is a database implementation which is supported by a
 * RTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RTreeDatabase<O extends NumberVector> extends SpatialIndexDatabase<O> {
  /**
   * Option string for parameter flat.
   */
  public static final String FLAT_DIRECTORY_F = "flat";

  /**
   * Description for parameter flat.
   */
  public static final String FLAT_DIRECTORY_D = "flag to specify a flat directory (default is a not flat directory)";

  /**
   * If true, the RTree will have a flat directory
   */
  private boolean flatDirectory;

  /**
   * Empty constructor, creates a new RTreeDatabase.
   */
  public RTreeDatabase() {
    super();
    parameterToDescription.put(FLAT_DIRECTORY_F, FLAT_DIRECTORY_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Returns the specific spatial index object for this database.
   *
   * @return the spatial index for this database
   */
  public SpatialIndex<O> createSpatialIndex(final List<O> objects) {
    if (flatDirectory) {
      return new FlatRTree<O>(objects, fileName, pageSize, cacheSize);
    }
    else {
      return new RTree<O>(objects, fileName, pageSize, cacheSize);
    }
  }

  /**
   * Returns the spatial index object with the specified parameters
   * for this database.
   *
   * @param dimensionality the dimensionality of the objects to be indexed
   */
  public SpatialIndex<O> createSpatialIndex(int dimensionality) {
    if (flatDirectory) {
      return new FlatRTree<O>(dimensionality, fileName, pageSize, cacheSize);
    }
    else {
      return new RTree<O>(dimensionality, fileName, pageSize, cacheSize);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(RTreeDatabase.class.getName());
    description.append(" holds all the data in an RTree index structure.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * Sets the values for the parameters filename, pagesize, cachesize and flat
   * if specified. If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    flatDirectory = optionHandler.isSet(FLAT_DIRECTORY_F);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(FLAT_DIRECTORY_F, Boolean.toString(flatDirectory));
    return attributeSettings;
  }
}
