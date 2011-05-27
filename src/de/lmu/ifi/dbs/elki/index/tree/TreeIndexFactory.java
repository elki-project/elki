package de.lmu.ifi.dbs.elki.index.tree;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.persistent.LRUCache;
import de.lmu.ifi.dbs.elki.persistent.MemoryPageFile;
import de.lmu.ifi.dbs.elki.persistent.Page;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.persistent.PersistentPageFile;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
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
 * @apiviz.stereotype factory,interface
 * @apiviz.uses TreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 * @param <I> Index type
 */
public abstract class TreeIndexFactory<O, I extends Index> implements IndexFactory<O, I> {
  /**
   * Optional parameter that specifies the name of the file storing the index.
   * If this parameter is not set the index is hold in the main memory.
   * <p>
   * Key: {@code -treeindex.file}
   * </p>
   */
  public static final OptionID FILE_ID = OptionID.getOrCreateOptionID("treeindex.file", "The name of the file storing the index. " + "If this parameter is not set the index is hold in the main memory.");

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
  public static final OptionID PAGE_SIZE_ID = OptionID.getOrCreateOptionID("treeindex.pagesize", "The size of a page in bytes.");

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
  public static final OptionID CACHE_SIZE_ID = OptionID.getOrCreateOptionID("treeindex.cachesize", "The size of the cache in bytes.");

  /**
   * Holds the name of the file storing the index specified by {@link #FILE_ID},
   * null if {@link #FILE_ID} is not specified.
   */
  protected String fileName = null;

  /**
   * Holds the value of {@link #PAGE_SIZE_ID}.
   */
  protected int pageSize;

  /**
   * Holds the value of {@link #CACHE_SIZE_ID}.
   */
  protected long cacheSize;

  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   */
  public TreeIndexFactory(String fileName, int pageSize, long cacheSize) {
    super();
    this.fileName = fileName;
    this.pageSize = pageSize;
    this.cacheSize = cacheSize;
  }

  /**
   * Make the page file for this index.
   * 
   * @param <N> page type
   * @param cls Class information
   * @return Page file
   */
  // FIXME: make this single-shot when filename is set!
  protected <N extends Page<N>> PageFile<N> makePageFile(Class<N> cls) {
    final PageFile<N> inner;
    if(fileName == null) {
      inner = new MemoryPageFile<N>(pageSize);
    }
    else {
      inner = new PersistentPageFile<N>(pageSize, fileName, cls);
    }
    if(cacheSize >= Integer.MAX_VALUE) {
      return inner;
    }
    return new LRUCache<N>(cacheSize, inner);
  }

  @Override
  abstract public I instantiate(Relation<O> relation);

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer<O> extends AbstractParameterizer {
    protected String fileName = null;

    protected int pageSize;

    protected long cacheSize;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter FILE_PARAM = new FileParameter(FILE_ID, FileParameter.FileType.OUTPUT_FILE, true);
      if(config.grab(FILE_PARAM)) {
        fileName = FILE_PARAM.getValue().getPath();
      }
      else {
        fileName = null;
      }

      final IntParameter PAGE_SIZE_PARAM = new IntParameter(PAGE_SIZE_ID, new GreaterConstraint(0), 4000);
      if(config.grab(PAGE_SIZE_PARAM)) {
        pageSize = PAGE_SIZE_PARAM.getValue();
      }

      LongParameter CACHE_SIZE_PARAM = new LongParameter(CACHE_SIZE_ID, new GreaterEqualConstraint(0), Integer.MAX_VALUE);
      if(config.grab(CACHE_SIZE_PARAM)) {
        cacheSize = CACHE_SIZE_PARAM.getValue();
      }
    }

    @Override
    protected abstract TreeIndexFactory<O, ?> makeInstance();
  }
}