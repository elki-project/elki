package de.lmu.ifi.dbs.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.sun.org.apache.bcel.internal.generic.ALOAD;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

public class KDDTaskFrame extends JFrame implements PropertyChangeListener {

	private KDDTask task;

	private String[] dbConnection;

	private String[] algorithm;

	private SelectionPanel dbConnectionPanel;

	private SelectionPanel algorithmPanel;

	public KDDTaskFrame() {
		setTitle("KDD Task");

		task = new KDDTask();

		JPanel mainPanel = new JPanel(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		// task name
		mainPanel.add(taskNamePanel(), gbc);
		// database connection
		gbc.gridy = 1;
		gbc.gridx = 1;

		dbConnectionPanel = new SelectionPanel("Select a database connection",
				"Database Connection", FileBasedDatabaseConnection.class
						.getName(), DatabaseConnection.class);
		dbConnectionPanel.select.setEnabled(true);
		dbConnectionPanel.addPropertyChangeListener(this);
		dbConnectionPanel.propName = KDDTask.DATABASE_CONNECTION_P;
		mainPanel.add(dbConnectionPanel, gbc);

		// algorithm panel
		gbc.gridy = 2;
		algorithmPanel = new SelectionPanel("Select an algorithm", "Algorithm",
				"", Algorithm.class);
		algorithmPanel.addPropertyChangeListener(this);
		algorithmPanel.propName = KDDTask.ALGORITHM_P;
		mainPanel.add(algorithmPanel, gbc);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setSize(new Dimension(300, 270));
		add(mainPanel);
		pack();
		dbConnectionPanel.select.requestFocusInWindow();
		setVisible(true);
	}

	// task name panel
	private JComponent taskNamePanel() {

		JPanel taskName = new JPanel();

		// label
		taskName.add(new JLabel("Task Name"));

		JTextField nameField = new JTextField();
		// default name ist date_time
		nameField.setColumns(15);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat formater = new SimpleDateFormat(
				"yyyy-MM-dd_HH:mm:ss.S");
		// System.out.println( formater.format(cal.getTime()) );
		nameField.setText(formater.format(cal.getTime()));
		nameField.setCaretPosition(0);

		taskName.add(nameField);

		return taskName;
	}

	private String[] getPropertyFileInfo(Class type) {

		// check if we got a property file
		if (Properties.KDD_FRAMEWORK_PROPERTIES != null) {

			return Properties.KDD_FRAMEWORK_PROPERTIES.getProperty(PropertyName
					.getOrCreatePropertyName(type));
		}
		return new String[] {};

	}

	public void propertyChange(PropertyChangeEvent evt) {

		if (evt.getPropertyName().equals(KDDTask.DATABASE_CONNECTION_P)) {
			dbConnection = (String[]) evt.getNewValue();
			algorithmPanel.select.setEnabled(true);
//			System.out.println(Arrays.toString(dbConnection));
		} else if (evt.getPropertyName().equals(KDDTask.ALGORITHM_P)) {
			algorithm = (String[]) evt.getNewValue();
//			System.out.println(Arrays.toString(algorithm));
			String[] completeParams = new String[dbConnection.length
					+ algorithm.length];
			System.arraycopy(algorithm, 0, completeParams, 0,
					algorithm.length);
			System.arraycopy(dbConnection, 0, completeParams,
					algorithm.length, dbConnection.length);
			runKDDTask(completeParams);
		}

	}

	private void runKDDTask(String[] args) {
		System.out.println(Arrays.toString(args));
		task = new KDDTask();
		try {
			task.setParameters(args);
		} catch (ParameterException e) {
			KDDDialog.showMessage(this, e.getMessage());
		}
	}

	private class SelectionPanel extends JPanel implements PopUpTreeListener,
			EditObjectChangeListener {

		/**
		 * the popup menu of this panel
		 */
		private PopUpTree menu;

		/**
		 * the select button
		 */
		private JButton select;

		/**
		 * the name label
		 */
		private JLabel nameLabel;

		/**
		 * the text field
		 */
		private JTextField textField;

		/**
		 * the panel to customize the edit object
		 */
		private CustomizerPanel editObjectCustomizer;

		/**
		 * the selected class includint the parameters
		 */
		private String selectedClass;

		private String oldValueSelectedClass;

		private String propName;

		public SelectionPanel(String buttonToolTip, String labelToolTip,
				String defaultClassName, Class parentClass) {

			// instantiate the popup-menu
			menu = new PopUpTree(getPropertyFileInfo(parentClass),
					defaultClassName);
			// don't forget to register as listener
			menu.addPopUpTreeListener(this);

			editObjectCustomizer = new CustomizerPanel(KDDTaskFrame.this,
					parentClass);
			editObjectCustomizer.addEditObjectChangeListener(this);

			layoutPanel(buttonToolTip, labelToolTip);
		}

		private void layoutPanel(final String buttonToolTip,
				final String labelToolTip) {

			// gridbaylayout
			setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();

			// name label
			nameLabel = new JLabel(labelToolTip);
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			gbc.insets = new Insets(10, 10, 10, 10);
			add(nameLabel, gbc);

			// select button
			createSelectButton(buttonToolTip);
			gbc.gridx = 2;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.LINE_END;
			add(select, gbc);

			// text field
			createTextField(buttonToolTip);
			gbc.gridy = 2;
			gbc.gridwidth = 4;
			gbc.gridx = 0;
			gbc.insets = new Insets(5, 10, 10, 10);
			gbc.anchor = GridBagConstraints.CENTER;
			add(textField, gbc);

			// border
			setBorder(BorderFactory.createEtchedBorder());
		}

		private void createSelectButton(String toolTipText) {
			select = new JButton("Select");

			select.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					menu.show(select, nameLabel.getLocation().x, select
							.getLocation().y);
				}
			});

			select.setEnabled(false);
			select.setToolTipText(toolTipText);
		}

		private void createTextField(final String toolTipText) {

			textField = new JTextField(20) {

				public void setText(String text) {
					if (text != null || !(text.equals(""))) {
						super.setText(text);
						this.setToolTipText("Click for details");
						this.setCaretPosition(0);
					} else {
						super.setText("");

						setToolTipText(toolTipText);
					}
				}
			};
			textField.setEditable(false);
			textField.setBackground(Color.white);
			textField.addMouseListener(new MouseAdapter() {

				public void mouseClicked(MouseEvent e) {
					if (editObjectCustomizer != null) {
						editObjectCustomizer.setVisible(true);
					}
				}

				public void mousePressed(MouseEvent e) {
					if (editObjectCustomizer != null) {
						editObjectCustomizer.setVisible(true);
					}
				}
			});
		}

		public void selectedClassChanged(String selectedClass) {
			editObjectCustomizer.setEditObjectClass(selectedClass);
		}

		public void editObjectChanged(String editObjectName, String[] parameters) {

			// update textField
			textField.setText(editObjectName);

			// update TaskFrame
			String[] completeParams = new String[parameters.length + 2];
			completeParams[0] = "-" + propName;
			completeParams[1] = editObjectName;
			System.arraycopy(parameters, 0, completeParams, 2,
					parameters.length);
			firePropertyChange(propName, "", completeParams);

			// if(selectedClass != null){
			// oldValueSelectedClass = selectedClass;
			// }
			// StringBuilder builder = new StringBuilder();
			// builder.append(editObjectName);
			// builder.append(" ");
			// builder.append(parameters);
			//			
			// selectedClass = builder.toString();
			// System.out.println("selectedClass: " + selectedClass);
			// textField.setText(editObjectName);

			// update KDDTaskFrame
			// firePropertyChange(propName, oldValueSelectedClass,
			// selectedClass);
		}

	}

}
