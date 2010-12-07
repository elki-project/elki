package de.lmu.ifi.dbs.elki.result;

/**
 * Marker interface for trivial "adapter" type of results.
 * 
 * Such results can be hidden by a GUI, since they just provide a "view" on the main result.
 * 
 * @author Erich Schubert
 */
public interface ResultAdapter extends Result {
  // Empty
}
