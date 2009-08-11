package de.lmu.ifi.dbs.elki.result;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;

public class HistogramResult<O extends DatabaseObject> extends CollectionResult<O> {
  public HistogramResult(Collection<O> col) {
    super(col);
  }

  public HistogramResult(Collection<O> col, Collection<String> header) {
    super(col, header);
  }

  @Override
  public String getName() {
    return "histogram";
  }
}
