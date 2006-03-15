package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.index.Index;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.List;

/**
 * IndexDatabase is a database implementation which is supported by an index structure.
 *
 * @author Elke Achtert(<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class IndexDatabase<O extends DatabaseObject> extends AbstractDatabase<O> {

  /**
   * Option string for parameter fileName.
   */
  public static final String FILE_NAME_P = "filename";

  /**
   * Description for parameter filename.
   */
  public static final String FILE_NAME_D = "<name>a file name specifying the name of the file storing the index. " +
                                           "If this parameter is not set the index is hold in the main memory.";

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
  public static final String PAGE_SIZE_D = "<int>a positive integer value specifying the size of a page in bytes " +
                                           "(default is " + DEFAULT_PAGE_SIZE + " Byte)";

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
  public static final String CACHE_SIZE_D = "<int>a positive integer value specifying the size of the cache in bytes " +
                                            "(default is " + DEFAULT_CACHE_SIZE + " Byte)";

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

  public IndexDatabase() {
    super();
    parameterToDescription.put(FILE_NAME_P + OptionHandler.EXPECTS_VALUE, FILE_NAME_D);
    parameterToDescription.put(PAGE_SIZE_P + OptionHandler.EXPECTS_VALUE, PAGE_SIZE_D);
    parameterToDescription.put(CACHE_SIZE_P + OptionHandler.EXPECTS_VALUE, CACHE_SIZE_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Calls the super method and afterwards deletes the specified object from the underlying index structure.
   *
   * @see Database#delete(Integer)
   */
  public O delete(Integer id) {
    O object = super.delete(id);
    getIndex().delete(object);
    return object;
  }

  /**
   * Calls the super method and afterwards deletes the specified object from the underlying index structure.
   *
   * @see Database#delete(de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public void delete(O object) {
    super.delete(object);
    getIndex().delete(object);
  }

  /**
   * Sets the values for the parameter bulk.
   * If the parameters is not specified the default value is set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

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
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(FILE_NAME_P, fileName);
    mySettings.addSetting(PAGE_SIZE_P, Integer.toString(pageSize));
    mySettings.addSetting(CACHE_SIZE_P, Integer.toString(cacheSize));

    return attributeSettings;
  }

  /**
   * Returns the I/O-Access of this database.
   *
   * @return the I/O-Access of this database
   */
  public abstract long getIOAccess();

  /**
   * Resets the I/O-Access of this database.
   */
  public abstract void resetIOAccess();

  /**
   * Returns the underlying index structure.
   *
   * @return the underlying index structure
   */
  public abstract Index<O> getIndex();
}