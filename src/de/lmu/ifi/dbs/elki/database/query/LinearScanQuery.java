package de.lmu.ifi.dbs.elki.database.query;

/**
 * Marker interface for linear scan (slow, non-accelerated) queries.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.database.Database
 */
public interface LinearScanQuery extends DatabaseQuery {
  // Empty marker interface
}
