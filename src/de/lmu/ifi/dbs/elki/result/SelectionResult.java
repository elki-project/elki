package de.lmu.ifi.dbs.elki.result;


/**
 * Selection result wrapper.
 * 
 * Note: we did not make the DBIDSelection a result in itself. Instead, the
 * DBIDSelection object should be seen as static contents of this result.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has DBIDSelection
 */
public class SelectionResult implements Result {
  /**
   * The actual selection
   */
  DBIDSelection selection = null;

  /**
   * Constructor.
   */
  public SelectionResult() {
    super();
  }

  /**
   * @return the selection
   */
  public DBIDSelection getSelection() {
    return selection;
  }

  /**
   * @param selection the selection to set
   */
  public void setSelection(DBIDSelection selection) {
    this.selection = selection;
  }

  @Override
  public String getLongName() {
    return "Selection";
  }

  @Override
  public String getShortName() {
    return "selection";
  }
}