package experimentalcode.marisa.index.xtree.util;

import java.io.IOException;

import de.lmu.ifi.dbs.elki.persistent.Cache;
import de.lmu.ifi.dbs.elki.persistent.Page;
import de.lmu.ifi.dbs.elki.persistent.PageHeader;
import de.lmu.ifi.dbs.elki.persistent.PersistentPageFile;

public class PersistentXTreePageFile<P extends Page<P>> extends PersistentPageFile<P> {

  public PersistentXTreePageFile(PageHeader header, int cacheSize, Cache<P> cache, String fileName, Class<? extends P> pageclass) {
    super(header, cacheSize, cache, fileName, pageclass);
  }

}
