package de.lmu.ifi.dbs.elki.gui;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.Option;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

public class ParameterPanel extends JPanel {

	private JCheckBox showOptionalParameters;

	private Vector<ParameterEditor> editors;

	private static final long serialVersionUID = 1L;

	public ParameterPanel(Option<?>[] parameters, ParameterChangeListener pl, ItemListener il) {
		this.setLayout(new GridBagLayout());

		getParameterEditors(parameters, pl, il);
		createDisplayPanel(false);
	}

	public void update(boolean showOptionalParameters){
		createDisplayPanel(showOptionalParameters);
	}
	
	private void createDisplayPanel(boolean showOptionalParameters) {

		this.removeAll();
		// parameter editors
		if (editors.size() != 0) {

			GridBagConstraints p_gbc = new GridBagConstraints();
			p_gbc.gridy = 0;
			p_gbc.weighty = 1.0;
			if (this.showOptionalParameters != null) {
				this.showOptionalParameters.setSelected(showOptionalParameters);
				add(this.showOptionalParameters, p_gbc);
				p_gbc.gridy = 2;

			} else {
				p_gbc.gridy = 1;
			}

			for (ParameterEditor e : this.editors) {

				if (e.isOptional() && !showOptionalParameters) {
					continue;
				}
				p_gbc.gridx = 0;
				p_gbc.gridwidth = 1;
				p_gbc.anchor = GridBagConstraints.WEST;
				p_gbc.insets = new Insets(2, 15, 2, 15);
				add(e.getNameLabel(), p_gbc);

				p_gbc.gridx = 1;
				p_gbc.gridwidth = 3;
				// gbc.anchor = GridBagConstraints.EAST;
				add(e.getInputField(), p_gbc);
				p_gbc.gridy = p_gbc.gridy + 1;

			}

			setBorder(BorderFactory.createLineBorder(Color.darkGray));
			setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		}
	}

	private void getParameterEditors(Option<?>[] options, ParameterChangeListener l, ItemListener il) {

		this.editors = new Vector<ParameterEditor>();

		boolean showOpt = false;
		for (Option<?> opt : options) {
			ParameterEditor ed = ParameterEditor.createEditor(opt, (JFrame) getParent(), l);
			if (ed.isOptional()) {
				showOpt = true;
			}
			editors.add(ed);
		}
		if (showOpt) {
			showOptionalParameters = new JCheckBox("Show optional parameters");
			showOptionalParameters.addItemListener(il);
		}
	}
}
