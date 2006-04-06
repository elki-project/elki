package de.lmu.ifi.dbs.index;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Abstract super class for all index classes.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class Index<O extends DatabaseObject> implements Parameterizable {
  /**
   * Option string for parameter fileName.
   */
  public static final String FILE_NAME_P = "filename";

  /**
   * Description for parameter filename.
   */
  public static final String FILE_NAME_D = "<name>a file name specifying the name of the file storing the index. "
                                           + "If this parameter is not set the index is hold in the main memory.";

  /**
   * The default pagesize.
   */
  public static final int DEFAULT_PAGE_SIZE = 4000;

  /**
   * Option string for parameter pagesize.
   */
  public static final String PAGE_SIZE_P = "pagesize";

  /**
   * Description for parameter filename.
   */
  public static final String PAGE_SIZE_D = "<int>a positive integer value specifying the size of a page in bytes "
                                           + "(default is " + DEFAULT_PAGE_SIZE + " Byte)";

  /**
   * The default cachesize.
   */
  public static final int DEFAULT_CACHE_SIZE = 1000000;

  /**
   * Option string for parameter cachesize.
   */
  public static final String CACHE_SIZE_P = "cachesize";

  /**
   * Description for parameter cachesize.
   */
  public static final String CACHE_SIZE_D = "<int>a positive integer value specifying the size of the cache in bytes "
                                            + "(default is " + DEFAULT_CACHE_SIZE + " Byte)";

  /**
   * The name of the file for storing the index.
   */
  protected String fileName;

  /**
   * The size of a page in bytes.
   */
  protected int pageSize;

  /**
   * Tthe size of the cache.
   */
  protected int cacheSize;

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler to handle options, optionHandler should be initialized in
   * any non-abstract class extending this class.
   */
  protected OptionHandler optionHandler;

  /**
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];

  /**
   * Sets parameters file, pageSize and cacheSize.
   */
  public Index() {
    parameterToDescription.put(FILE_NAME_P + OptionHandler.EXPECTS_VALUE, FILE_NAME_D);
    parameterToDescription.put(PAGE_SIZE_P + OptionHandler.EXPECTS_VALUE, PAGE_SIZE_D);
    parameterToDescription.put(CACHE_SIZE_P + OptionHandler.EXPECTS_VALUE, CACHE_SIZE_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    // filename
    if (optionHandler.isSet(FILE_NAME_P)) {
      fileName = optionHandler.getOptionValue(FILE_NAME_P);
    }
    else {
      fileName = null;
    }

    // pagesize
    if (optionHandler.isSet(PAGE_SIZE_P)) {
      try {
        pageSize = Integer.parseInt(optionHandler.getOptionValue(PAGE_SIZE_P));
        if (pageSize <= 0)
          throw new WrongParameterValueException(PAGE_SIZE_P, optionHandler.getOptionValue(PAGE_SIZE_P), PAGE_SIZE_D);
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(PAGE_SIZE_P, optionHandler.getOptionValue(PAGE_SIZE_P), PAGE_SIZE_D, e);
      }
    }
    else {
      pageSize = DEFAULT_PAGE_SIZE;
    }

    // cachesize
    if (optionHandler.isSet(CACHE_SIZE_P)) {
      try {
        cacheSize = Integer.parseInt(optionHandler.getOptionValue(CACHE_SIZE_P));
        if (cacheSize < 0)
          throw new WrongParameterValueException(CACHE_SIZE_P, optionHandler.getOptionValue(CACHE_SIZE_P), CACHE_SIZE_D);
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(CACHE_SIZE_P, optionHandler.getOptionValue(CACHE_SIZE_P), CACHE_SIZE_D, e);
      }
    }
    else {
      cacheSize = DEFAULT_CACHE_SIZE;
    }

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
   */
  public String[] getParameters() {
    String[] param = new String[currentParameterArray.length];
    System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
    return param;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> settings = new ArrayList<AttributeSettings>();

    AttributeSettings mySettings = new AttributeSettings(this);
    mySettings.addSetting(FILE_NAME_P, fileName);
    mySettings.addSetting(PAGE_SIZE_P, Integer.toString(pageSize));
    mySettings.addSetting(CACHE_SIZE_P, Integer.toString(cacheSize));

    return settings;
  }

/**
   * Returns a description of the class and the required parameters.
   * <p/>
   * This description should be suitable for a usage description.
   *
   * @return String a description of the class and the required parameters
   */
  public String description() {
    return optionHandler.usage("", false);
  }

  /**
   * Sets the difference of the first array minus the second array as the
   * currently set parameter array.
   *
   * @param complete the complete array
   * @param part     an array that contains only elements of the first array
   */
  protected void setParameters(String[] complete, String[] part) {
    currentParameterArray = Util.difference(complete, part);
  }

  /**
   * Inserts the specified object into this index.
   *
   * @param o the vector to be inserted
   */
  abstract public void insert(O o);

  /**
   * Inserts the specified objects into this index. If a bulk load mode
   * is implemented, the objects are inserted in one bulk.
   *
   * @param objects the objects to be inserted
   */
  abstract public void insert(List<O> objects);

  /**
   * Deletes the specified obect from this index.
   *
   * @param o the object to be deleted
   * @return true if this index did contain the object, false otherwise
   */
  abstract public boolean delete(O o);

  /**
   * Returns the IO-Access of this index.
   *
   * @return the IO-Access of this index
   */
  abstract public long getIOAccess();

  /**
   * Resets the IO-Access of this index.
   */
  abstract public void resetIOAccess();

}
