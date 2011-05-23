package de.lmu.ifi.dbs.elki.database;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DBIDView;
import de.lmu.ifi.dbs.elki.database.relation.ProxyView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * A proxy database to use e.g. for projections and partitions.
 * 
 * @author Erich Schubert
 */
public class ProxyDatabase extends AbstractDatabase {
  /**
   * Logger class.
   */
  private static final Logging logger = Logging.getLogger(ProxyDatabase.class);

  /**
   * Our DBIDs
   */
  final protected DBIDs ids;

  /**
   * Our DBID representation
   */
  final protected DBIDView idrep;

  /**
   * Constructor.
   * 
   * @param ids DBIDs to use
   */
  public ProxyDatabase(DBIDs ids) {
    super();
    this.ids = ids;
    this.idrep = new DBIDView(this, this.ids);
    this.relations.add(idrep);
    this.addChildResult(idrep);
  }

  /**
   * Constructor.
   * 
   * @param ids DBIDs to use
   * @param relations Relations to contain
   */
  public ProxyDatabase(DBIDs ids, Iterable<Relation<?>> relations) {
    super();
    this.ids = ids;
    this.idrep = new DBIDView(this, this.ids);
    this.relations.add(idrep);
    this.addChildResult(idrep);
    for(Relation<?> orel : relations) {
      Relation<?> relation = ProxyView.wrap(this, ids, orel);
      this.relations.add(relation);
      this.addChildResult(relation);
    }
  }

  /**
   * Constructor.
   * 
   * @param ids DBIDs to use
   * @param relations Relations to contain
   */
  public ProxyDatabase(DBIDs ids, Relation<?>... relations) {
    this(ids, Arrays.asList(relations));
  }

  /**
   * Constructor, proxying all relations of an existing database.
   * 
   * @param ids ids to proxy
   * @param database Database to wrap
   */
  public ProxyDatabase(DBIDs ids, Database database) {
    this(ids, database.getRelations());
  }

  @Override
  public void initialize() {
    // Nothing to do - we were initialized on construction time.
  }

  /**
   * Add a new representation.
   * 
   * @param relation Representation to add.
   */
  public void addRelation(Relation<?> relation) {
    this.relations.add(relation);
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public int size() {
    return ids.size();
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public StaticDBIDs getDBIDs() {
    return DBIDUtil.makeUnmodifiable(ids);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}