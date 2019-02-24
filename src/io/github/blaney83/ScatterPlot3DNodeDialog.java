package io.github.blaney83;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.util.ColumnSelectionComboxBox;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

import io.github.blaney83.mvlrgraph.MVLRGraphNodeModel;
import io.github.blaney83.mvlrgraph.MVLRGraphSettings;

/**
 * <code>NodeDialog</code> for the "ScatterPlot3D" Node.
 * A 3 Dimensional representation of points in space for a given data set.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Benjamin Laney
 */
public class ScatterPlot3DNodeDialog extends NodeDialogPane {

	private final ScatterPlot3DSettings m_settings = new ScatterPlot3DSettings();
	
	private static String CFGKEY_COL_FILTER = "colFilter";
	
	@SuppressWarnings("unchecked")
	private final ColumnSelectionComboxBox m_colName = new ColumnSelectionComboxBox(DoubleValue.class);
	
	private final DataColumnSpecFilterPanel m_colSelectionPanel = new DataColumnSpecFilterPanel();
	
	private JCheckBox m_appendColumn = new JCheckBox();
	private JCheckBox m_showRegModel = new JCheckBox();
	private JCheckBox m_showAllData = new JCheckBox();

	private final JSpinner m_count = new JSpinner(new SpinnerNumberModel(ScatterPlot3DNodeModel.DEFAULT_COUNT, 1, Integer.MAX_VALUE, 1));
	
	protected ScatterPlot3DNodeDialog() {

		m_colSelectionPanel.setIncludeTitle("X and Y Axis Variables (exactly 2)");
		m_colSelectionPanel.setExcludeTitle("Set to Mean/Excluded from Model");

		JPanel panel = new JPanel(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.gridx = 0;
		constraints.gridy = 0;

		panel.add(new JLabel("Target Column (same as in Regression Learner; z-axis)"), constraints);
		constraints.gridx = 1;
		panel.add(m_colName);
		m_colName.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				if (m_colName.getSelectedItem() != null) {
					m_colSelectionPanel.resetHiding();
					m_colSelectionPanel.hideNames((DataColumnSpec) m_colName.getSelectedItem());
				}

			}
		});

		constraints.insets = new Insets(4, 0, 0, 0);
		constraints.gridy++;
		constraints.gridx = 0;
		constraints.gridwidth = 2;

		panel.add(new JLabel("Select the two independent variables to model"), constraints);
		constraints.gridy++;

		panel.add(m_colSelectionPanel, constraints);
		constraints.gridy++;
		constraints.gridx = 0;

		panel.add(new JSeparator(SwingConstants.HORIZONTAL), constraints);

		constraints.gridwidth = 1;
		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("Append Column of Calculated Values"), constraints);
		constraints.gridx = 1;
		panel.add(m_appendColumn, constraints);

		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("Plot all real data points"), constraints);
		constraints.gridx = 1;
		panel.add(m_showAllData, constraints);
		m_showAllData.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				if (m_showAllData.isSelected()) {
					m_count.setEnabled(false);
				} else {
					m_count.setEnabled(true);
				}

			}
		});

		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("Number of data points to plot"), constraints);
		constraints.gridx = 1;
		panel.add(m_count, constraints);

		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("Display Regression model?"), constraints);
		constraints.gridx = 1;
		panel.add(m_showRegModel, constraints);

		addTab("General", panel);
		
//		future view tab
//		panel = new JPanel(new GridBagLayout());

	}

	
	
	@Override
	protected void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
		DataTableSpec tableSpec = specs[ScatterPlot3DNodeModel.DATA_TABLE_IN_PORT];
		try {
			m_settings.loadSettingsInDialog(settings, tableSpec);
		}catch(Exception e) {
			LinkedHashSet<String> inclSet = new LinkedHashSet<String>();
			for(DataColumnSpec colSpec : tableSpec) {
				if(colSpec.getType().isCompatible(DoubleValue.class)) {
					inclSet.add(colSpec.getName());
				}
			}
			NodeSettings defaultSettings = ScatterPlot3DSettings.createDefaults(CFGKEY_COL_FILTER, inclSet.toArray(new String[0]), new String[0], false);
			DataColumnSpecFilterConfiguration filterConfig = new DataColumnSpecFilterConfiguration(CFGKEY_COL_FILTER);
			filterConfig.loadConfigurationInDialog(defaultSettings, tableSpec);
			m_settings.getFilterConfiguration().loadConfigurationInDialog(defaultSettings, tableSpec);
		}
		
		m_colName.update(tableSpec, m_settings.getColName());
		m_colSelectionPanel.loadConfiguration(m_settings.getFilterConfiguration(), tableSpec);
		m_colSelectionPanel.resetHiding();
		m_colSelectionPanel.hideNames((DataColumnSpec)m_colName.getSelectedItem());
		m_appendColumn.setSelected(m_settings.getAppendColumn());
		m_count.setValue(m_settings.getCount());
		m_showAllData.setSelected(m_settings.getShowAllData());
		m_showRegModel.setSelected(m_settings.getShowRegModel());
		
	}



	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		
		String[] nameArr = new String[2];
		try {
		nameArr = m_colSelectionPanel.getIncludedNamesAsSet().toArray(nameArr);
		} catch(Exception e) {
			throw new InvalidSettingsException("You must have two columns included: first for the X-Axis, second for the Y-Axis."
					+ " Please exclude/include additional columns until there are exactly 2 on the right hand side.");
		}
		
		m_settings.setColName(m_colName.getSelectedColumn());
		m_settings.setXAxisVarColumn(nameArr[0]);
		m_settings.setYAxisVarColumn(nameArr[1]);
		m_colSelectionPanel.saveConfiguration(m_settings.getFilterConfiguration());
		m_settings.setAppendColumn(m_appendColumn.isSelected());
		m_settings.setCount((Integer)m_count.getModel().getValue());
		m_settings.setShowAllData(m_showAllData.isSelected());
		m_settings.setShowRegModel(m_showRegModel.isSelected());
		
		m_settings.saveSettingsTo(settings);

	}
}

