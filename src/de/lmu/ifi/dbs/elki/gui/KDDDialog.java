package de.lmu.ifi.dbs.elki.gui;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.border.CompoundBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

@SuppressWarnings("serial")
public class KDDDialog extends JDialog implements ActionListener {
	public static final String CLOSE_COMMAND = "CLOSE";

	public static final String DETAILS = "DETAILS";

	public static final String NO_DETAILS = "NO_DETAILS";

	private Exception exception;

	private String message;

	private KDDDialog(Window owner, String message, Exception ex) {

		super(owner);
		setModal(true);

		this.exception = ex;
		this.message = message;

		if (KDDTaskFrame.CENTERED_LOCATION) {
			setLocationRelativeTo(null);
		} else {
			setLocationRelativeTo(owner);
		}
	}

	private KDDDialog(Window owner) {
		super(owner);
		setModal(true);

		setLocationRelativeTo(owner);
	}

	private KDDDialog(Window owner, String message) {
		super(owner);
		setModal(true);
		this.message = message;
	}

	private void createParameterDialog(boolean showDetails) {

		JPanel base = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		JTextArea textArea = new JTextArea(message);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(400, 100));
		scrollPane.setBorder(new CompoundBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), BorderFactory.createLineBorder(Color.GRAY)));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 5;
		gbc.gridheight = 3;
		gbc.insets = new Insets(5, 20, 10, 20);
		base.add(scrollPane, gbc);

		JButton close = new JButton("Close");
		close.addActionListener(this);
		close.setActionCommand(CLOSE_COMMAND);

		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.insets = new Insets(5, 20, 10, 5);
		gbc.anchor = (GridBagConstraints.WEST);
		base.add(close, gbc);

		if (showDetails) {

			JButton details = new JButton("No details");
			details.addActionListener(this);
			details.setActionCommand(NO_DETAILS);
			// details.addActionListener(new ActionListener() {
			//
			// public void actionPerformed(ActionEvent e) {
			// update(false);
			// }
			// });

			gbc.gridx = 3;
			gbc.gridy = 4;
			gbc.anchor = (GridBagConstraints.EAST);
			gbc.insets = new Insets(5, 5, 10, 20);
			base.add(details, gbc);

			JLabel detail = new JLabel("Details");
			gbc.gridx = 2;
			gbc.gridy = 5;
			gbc.gridwidth = 3;
			gbc.anchor = GridBagConstraints.CENTER;
			gbc.insets = new Insets(10, 10, 5, 10);
			base.add(detail, gbc);

			StringBuffer buffer = new StringBuffer();
			
			if (exception != null) {
			  for (StackTraceElement el : exception.getStackTrace()) {
			    buffer.append(el.toString() + "\n");
			  }
			}

			JTextArea detailArea = new JTextArea(buffer.toString());
			textArea.setLineWrap(true);
			textArea.setEditable(false);
			textArea.setWrapStyleWord(true);
			JScrollPane detailsPane = new JScrollPane(detailArea);
			detailsPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			detailsPane.setPreferredSize(new Dimension(400, 100));
			gbc.gridx = 0;
			gbc.gridy = 6;
			gbc.gridwidth = 5;
			gbc.gridheight = 3;
			base.add(detailsPane, gbc);
		} else {
			JButton details = new JButton("Details");
			details.addActionListener(this);
			details.setActionCommand(DETAILS);
			// details.addActionListener(new ActionListener() {
			//
			// public void actionPerformed(ActionEvent e) {
			// update(true);
			// }
			// });
			gbc.gridx = 3;
			gbc.gridy = 4;
			gbc.anchor = (GridBagConstraints.EAST);
			gbc.insets = new Insets(5, 5, 10, 20);
			base.add(details, gbc);

		}

		add(base);
	}

	private void update(boolean showDetails) {
		getContentPane().removeAll();
		createParameterDialog(showDetails);
		pack();
	}

  private void createAboutPanel(EditObject editObject) {

		JPanel base = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 3;
		gbc.gridx = 0;
		gbc.gridy = 0;

		JTextPane area = new JTextPane() {

      @Override
      public Dimension getPreferredScrollableViewportSize() {
				Dimension dim = getPreferredSize();
				dim.width = 400;
				dim.height = 300;
				return dim;
			}

			@Override
      public void setSize(Dimension d) {

				if (d.width < getParent().getSize().width) {
					d.width = getParent().getSize().width;

				}

				super.setSize(d);
			}

			@Override
      public boolean getScrollableTracksViewportWidth() {

				return false;
			}

		};
		area.setEditable(false);

		Dimension dim = area.getPreferredSize();
		dim.height = 300;
		area.setMaximumSize(dim);
		StyledDocument doc = area.getStyledDocument();
		SimpleAttributeSet set = new SimpleAttributeSet();

		StyleConstants.setBold(set, true);
		try {
			// name information
			doc.insertString(doc.getLength(), "NAME\n", set);
			StyleConstants.setBold(set, false);
			doc.insertString(doc.getLength(), editObject.getName() + "\n\n", set);

			// algorithm?
			if (editObject.isAlgorithm()) {
				StyleConstants.setBold(set, true);
				doc.insertString(doc.getLength(), "SYNOPSIS\n", set);
				StyleConstants.setBold(set, false);
				doc.insertString(doc.getLength(), editObject.getAlgorithmInfo() + "\n\n", set);
			}

			if (editObject.isParameterizable()) {
				StyleConstants.setBold(set, true);
				doc.insertString(doc.getLength(), "USAGE\n", set);
				StyleConstants.setBold(set, false);
				doc.insertString(doc.getLength(), editObject.getDescription(), set);
			}

		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		JScrollPane scroller = new JScrollPane();

		scroller.setViewportView(area);
		area.setCaretPosition(0);
		base.add(scroller, gbc);

		// usage information
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.insets = new Insets(10, 5, 5, 5);
		JButton close = new JButton("Close");
		getRootPane().setDefaultButton(close);
		close.addActionListener(this);
		close.setActionCommand(CLOSE_COMMAND);
		base.add(close, gbc);

		getContentPane().add(base);
	}

	private void createMessageDialog() {

		JPanel base = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		JTextArea area = new JTextArea(message);
		area.setEditable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setColumns(25);
		area.setRows(8);

		JScrollPane scroller = new JScrollPane();
		scroller.setViewportView(area);
		area.setCaretPosition(0);
		gbc.gridwidth = 3;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.insets = new Insets(20, 20, 10, 20);
		base.add(scroller, gbc);

		JButton close = new JButton("OK");
		close.addActionListener(this);
		close.setActionCommand(CLOSE_COMMAND);
		gbc.gridwidth = 1;
		gbc.gridy = 1;
		gbc.gridx = 2;
		gbc.insets = new Insets(5, 10, 10, 10);
		base.add(close, gbc);
		getContentPane().add(base);
		setResizable(false);
	}
	
//	private void createSaveSettingsDialog(){
//		JPanel base = new JPanel(new GridBagLayout());
//		GridBagConstraints gbc = new GridBagConstraints();
//		
//		
//	}

	public static void showParameterMessage(Window owner, String message, ParameterException e) {

		KDDDialog dialog = new KDDDialog(owner, message, e);
		dialog.setTitle("Parameter Value Error");
		dialog.createParameterDialog(false);

		dialog.pack();
		dialog.setVisible(true);
	}

	public static void showAboutMessage(Window owner, EditObject editObject) {

		KDDDialog dialog = new KDDDialog(owner);
		dialog.setTitle("Information");

		dialog.createAboutPanel(editObject);
		dialog.pack();
		dialog.setResizable(false);
		dialog.setVisible(true);
	}

	public static void showMessage(Window owner, String message) {
		KDDDialog dialog = new KDDDialog(owner, message);
		dialog.setTitle("Information");

		dialog.createMessageDialog();
		dialog.pack();
		dialog.setVisible(true);
	}

	public static void showErrorMessage(Window owner, String message, @SuppressWarnings("unused") Exception e) {
		KDDDialog dialog = new KDDDialog(owner, message);
		dialog.setTitle("Error");
		dialog.createParameterDialog(false);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	public static void showSaveSettingsDialog(Window owner){
//		KDDDialog dialog = new KDDDialog(owner);
//		dialog.setTitle("Save settings...");
//		dialog.createSaveSettingsDialog();
//		dialog.pack();
//		dialog.setVisible(true);
		
		JFileChooser choosy = new JFileChooser();
		int returnValue = choosy.showSaveDialog(owner);
		if(returnValue == JFileChooser.APPROVE_OPTION){
			//
		}
	}

	public void actionPerformed(ActionEvent e) {

		String actionCommand = e.getActionCommand();

		if (actionCommand.equalsIgnoreCase("CLOSE")) {
			setVisible(false);
		} else if (actionCommand.equalsIgnoreCase(NO_DETAILS)) {
			update(false);
		} else if (actionCommand.equals(DETAILS)) {
			update(true);
		}
	}
}
