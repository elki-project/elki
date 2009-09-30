package experimentalcode.erich.minigui;

import javax.swing.table.AbstractTableModel;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;

class ParametersModel extends AbstractTableModel {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(ParametersModel.class);
  
  /**
   * Parameter storage
   */
  private DynamicParameters parameters;

  /**
   * Column headers in model
   */
  public static final String[] columns = { "Parameter", "Value" };

  public ParametersModel(DynamicParameters parameters) {
    super();
    this.parameters = parameters;
  }

  @Override
  public int getColumnCount() {
    return columns.length;
  }

  @Override
  public int getRowCount() {
    return parameters.size();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if(rowIndex < parameters.size()) {
      if(columnIndex == 0) {
        return parameters.getOption(rowIndex);
      }
      else if(columnIndex == 1) {
        String ret = parameters.getValue(rowIndex);
        if(ret == null) {
          ret = "";
        }
        return ret;
      }
      return "";
    }
    else {
      return "";
    }
  }

  @Override
  public String getColumnName(int column) {
    return columns[column];
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if(columnIndex == 0) {
      return Option.class;
    }
    return String.class;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return (columnIndex == 1) || (rowIndex > parameters.size());
  }

  @Override
  public void setValueAt(Object value, int rowIndex, int columnIndex) {
    if(value instanceof String) {
      String s = (String) value;
      if(columnIndex == 1) {
        parameters.setValue(rowIndex, s);
        fireTableCellUpdated(rowIndex, columnIndex);
      }
    }
    else {
      logger.warning("Edited value is not a String!");
    }
  }
}