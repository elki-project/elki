package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.GridBagConstraints;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.gui.icons.StockIcon;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Abstract class to produce a configurator for a particular parameter.
 * 
 * @author Erich Schubert
 * 
 * @param <T>
 */
public abstract class AbstractParameterConfigurator<T extends Parameter<?, ?>> implements ParameterConfigurator {
  /**
   * The parameter to configure
   */
  final T param;

  /**
   * The parent container
   */
  final JComponent parent;

  /**
   * The event listeners for this parameter.
   */
  protected EventListenerList listenerList = new EventListenerList();

  /**
   * Constructor.
   * 
   * @param param Parameter
   * @param parent Parent
   */
  public AbstractParameterConfigurator(T param, JComponent parent) {
    super();
    this.param = param;
    this.parent = parent;
  }

  public void finishGridRow() {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = GridBagConstraints.REMAINDER;
    constraints.weightx = 0;
    final JLabel icon;
    if(param.isOptional()) {
      if(param.isDefined() && param.tookDefaultValue()) {
        // TODO: better icon for default value?
        icon = new JLabel(StockIcon.getStockIcon(StockIcon.DIALOG_INFORMATION));
        icon.setToolTipText("Default value");
      }
      else {
        icon = new JLabel();
      }
    }
    else {
      if(!param.isDefined()) {
        icon = new JLabel(StockIcon.getStockIcon(StockIcon.DIALOG_WARNING));
        icon.setToolTipText("Missing value.");
      }
      else {
        icon = new JLabel();
      }
    }
    parent.add(icon, constraints);
  }

  @Override
  public void addParameter(@SuppressWarnings("unused") Object owner, @SuppressWarnings("unused") Parameter<?, ?> param) {
    LoggingUtil.warning(this.getClass() + " does not support sub-parameters!");
  }

  @Override
  public void addChangeListener(ChangeListener listener) {
    listenerList.add(ChangeListener.class, listener);
  }

  @Override
  public void removeChangeListener(ChangeListener listener) {
    listenerList.remove(ChangeListener.class, listener);
  }

  protected void fireValueChanged() {
    // FIXME: compare with previous value?
    ChangeEvent evt = new ChangeEvent(this);
    for(ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
      listener.stateChanged(evt);
    }
  }

  @Override
  public void appendParameters(ListParameterization params) {
    Object val = getUserInput();
    if(val instanceof String && ((String) val).length() == 0) {
      val = null;
    }
    if(val != null) {
      params.addParameter(param.getOptionID(), val);
    }
  }

  public abstract Object getUserInput();
}