package de.lmu.ifi.dbs.elki.index.tree;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * Abstract base class for tree-based indexes.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.index.tree.TreeIndex - - produces
 *
 * @param <O> Object type
 * @param <I> Index type
 */
public abstract class TreeIndexFactory<O extends DatabaseObject, I extends TreeIndex<O, ?, ?>> implements IndexFactory<O, I> {
  /**
   * OptionID for {@link #FILE_PARAM}
   */
  public static final OptionID FILE_ID = OptionID.getOrCreateOptionID("treeindex.file", "The name of the file storing the index. " + "If this parameter is not set the index is hold in the main memory.");

  /**
   * Optional parameter that specifies the name of the file storing the index.
   * If this parameter is not set the index is hold in the main memory.
   * <p>
   * Key: {@code -treeindex.file}
   * </p>
   */
  private final FileParameter FILE_PARAM = new FileParameter(FILE_ID, FileParameter.FileType.OUTPUT_FILE, true);

  /**
   * OptionID for {@link #PAGE_SIZE_PARAM}
   */
  public static final OptionID PAGE_SIZE_ID = OptionID.getOrCreateOptionID("treeindex.pagesize", "The size of a page in bytes.");

  /**
   * Parameter to specify the size of a page in bytes, must be an integer
   * greater than 0.
   * <p>
   * Default value: {@code 4000}
   * </p>
   * <p>
   * Key: {@code -treeindex.pagesize}
   * </p>
   */
  private final IntParameter PAGE_SIZE_PARAM = new IntParameter(PAGE_SIZE_ID, new GreaterConstraint(0), 4000);

  /**
   * OptionID for {@link #CACHE_SIZE_PARAM}
   */
  public static final OptionID CACHE_SIZE_ID = OptionID.getOrCreateOptionID("treeindex.cachesize", "The size of the cache in bytes.");

  /**
   * Parameter to specify the size of the cache in bytes, must be an integer
   * equal to or greater than 0.
   * <p>
   * Default value: {@link Integer#MAX_VALUE}
   * </p>
   * <p>
   * Key: {@code -treeindex.cachesize}
   * </p>
   */
  private final LongParameter CACHE_SIZE_PARAM = new LongParameter(CACHE_SIZE_ID, new GreaterEqualConstraint(0), Integer.MAX_VALUE);

  /**
   * Holds the name of the file storing the index specified by
   * {@link #FILE_PARAM}, null if {@link #FILE_PARAM} is not specified.
   */
  protected String fileName = null;

  /**
   * Holds the value of {@link #PAGE_SIZE_PARAM}.
   */
  protected int pageSize;

  /**
   * Holds the value of {@link #CACHE_SIZE_PARAM}.
   */
  protected long cacheSize;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public TreeIndexFactory(Parameterization config) {
    super();
    config = config.descend(this);

    // file
    if(config.grab(FILE_PARAM)) {
      fileName = FILE_PARAM.getValue().getPath();
    }
    else {
      fileName = null;
    }
    // page size
    if(config.grab(PAGE_SIZE_PARAM)) {
      pageSize = PAGE_SIZE_PARAM.getValue();
    }
    // cache size
    if(config.grab(CACHE_SIZE_PARAM)) {
      cacheSize = CACHE_SIZE_PARAM.getValue();
    }
  }

  @Override
  abstract public I instantiate(Database<O> database);
}