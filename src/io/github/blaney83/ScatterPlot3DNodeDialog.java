package io.github.blaney83;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;

/**
 * <code>NodeDialog</code> for the "ScatterPlot3D" Node.
 * A 3 Dimensional representation of points in space for a given data set.
 * 
 * @author Benjamin Laney
 */
public class ScatterPlot3DNodeDialog extends NodeDialogPane {

	private final ScatterPlot3DSettings m_settings = new ScatterPlot3DSettings();
	
	private final DataColumnSpecFilterPanel m_colSelectionPanel = new DataColumnSpecFilterPanel();
	private final JCheckBox m_isClustered = new JCheckBox();
	private JComboBox<String[]> m_clusterType = new JComboBox<String[]>();
	private JSpinner m_numClusters = new JSpinner(new SpinnerNumberModel(2, 2, Integer.MAX_VALUE, 1));
	private final JCheckBox m_prototypesProvided = new JCheckBox();
//	m_clusterType

	private JCheckBox m_showAllData = new JCheckBox();
	private final JSpinner m_count = new JSpinner(new SpinnerNumberModel(ScatterPlot3DSettings.DEFAULT_COUNT, 1, Integer.MAX_VALUE, 1));
	private final JSpinner m_dataPointSize = new JSpinner(new SpinnerNumberModel(ScatterPlot3DSettings.DEFAULT_DATA_POINT_SIZE, 1, Integer.MAX_VALUE, 1));
	private final JSpinner m_prototypePointSize = new JSpinner(new SpinnerNumberModel(ScatterPlot3DSettings.DEFAULT_PROTOTYPE_POINT_SIZE, 1, Integer.MAX_VALUE, 1));
	private final JColorChooser m_prototypePointColor = new JColorChooser(ScatterPlot3DSettings.DEFAULT_PROTOTYPE_POINT_COLOR);
	
	protected ScatterPlot3DNodeDialog() {

		m_clusterType.addItem(ScatterPlot3DSettings.DEFAULT_CLUSTER_TYPES_ARRAY);
		
		m_colSelectionPanel.setIncludeTitle("X and Y Axis Variables (exactly 2)");
		m_colSelectionPanel.setExcludeTitle("Set to Mean/Excluded from Model");

		JPanel panel = new JPanel(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.gridx = 0;
		constraints.gridy = 0;

		constraints.insets = new Insets(4, 0, 0, 0);
		constraints.gridy++;
		constraints.gridx = 0;
		constraints.gridwidth = 2;

		panel.add(new JLabel("Select the three independent variables to model"), constraints);
		constraints.gridy++;

		panel.add(m_colSelectionPanel, constraints);
		constraints.gridy++;
		constraints.gridx = 0;

		panel.add(new JSeparator(SwingConstants.HORIZONTAL), constraints);

		constraints.gridwidth = 1;
		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("Has the data been clustered?"), constraints);
		constraints.gridx = 1;
		panel.add(m_isClustered, constraints);
		
		m_isClustered.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {

				if (m_isClustered.isSelected()) {
					m_clusterType.setEnabled(true);
					m_numClusters.setEnabled(true);
				} else {
					m_clusterType.setEnabled(false);
					m_numClusters.setEnabled(false);
				}

			}
		});

		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("What method of clustering has been used?"), constraints);
		constraints.gridx = 1;
		panel.add(m_clusterType, constraints);
		
		m_clusterType.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(m_clusterType.getSelectedItem().toString().equals(ScatterPlot3DSettings.DEFAULT_CLUSTER_TYPE)) {
					m_prototypesProvided.setEnabled(true);
				}else {
					m_prototypesProvided.setEnabled(false);
				}
			}
		});
		
		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("Number of clusters"), constraints);
		constraints.gridx = 1;
		panel.add(m_numClusters, constraints);
		m_numClusters.setEnabled(false);
		
		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("Prototype table provided (In-Port 2)?"), constraints);
		constraints.gridx = 1;
		panel.add(m_prototypesProvided, constraints);
		m_prototypesProvided.setEnabled(false);
		
		m_prototypesProvided.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if(m_prototypesProvided.isSelected()) {
					m_prototypePointSize.setEnabled(true);
					m_prototypePointColor.setEnabled(true);
				}else {
					m_prototypePointSize.setEnabled(false);
					m_prototypePointColor.setEnabled(false);
				}
			}
		});

		addTab("General", panel);
		
//		view tab
		panel = new JPanel(new GridBagLayout());
		
		constraints = new GridBagConstraints();

		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.gridx = 0;
		constraints.gridy = 0;
		
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
		m_count.setEnabled(false);
		
		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("Point size (in pixels)"), constraints);
		constraints.gridx = 1;
		panel.add(m_dataPointSize, constraints);
		
		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("Prototype point size"), constraints);
		constraints.gridx = 1;
		panel.add(m_prototypePointSize, constraints);
		
		constraints.gridy++;
		constraints.gridx = 0;
		panel.add(new JLabel("Prototype point color"), constraints);
		constraints.gridx = 1;
		panel.add(m_prototypePointColor, constraints);
		
		addTab("View", panel);
	}

	
	
	@Override
	protected void loadSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
		//rework w/ testing
//		DataTableSpec tableSpec = specs[ScatterPlot3DNodeModel.DATA_TABLE_IN_PORT];
//		try {
//			m_settings.loadSettingsInDialog(settings, tableSpec);
//		}catch(Exception e) {
//			LinkedHashSet<String> inclSet = new LinkedHashSet<String>();
//			for(DataColumnSpec colSpec : tableSpec) {
//				if(colSpec.getType().isCompatible(DoubleValue.class)) {
//					inclSet.add(colSpec.getName());
//				}
//			}
//			NodeSettings defaultSettings = ScatterPlot3DSettings.createDefaults(CFGKEY_COL_FILTER, inclSet.toArray(new String[0]), new String[0], false);
//			DataColumnSpecFilterConfiguration filterConfig = new DataColumnSpecFilterConfiguration(CFGKEY_COL_FILTER);
//			filterConfig.loadConfigurationInDialog(defaultSettings, tableSpec);
//			m_settings.getFilterConfiguration().loadConfigurationInDialog(defaultSettings, tableSpec);
//		}
//		
//		m_colSelectionPanel.loadConfiguration(m_settings.getFilterConfiguration(), tableSpec);
//		m_colSelectionPanel.resetHiding();
//		m_count.setValue(m_settings.getCount());
//		m_showAllData.setSelected(m_settings.getShowAllData());
		
	}



	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
		
		String[] nameArr = new String[2];
		try {
		nameArr = m_colSelectionPanel.getIncludedNamesAsSet().toArray(nameArr);
		} catch(Exception e) {
			throw new InvalidSettingsException("You must have three columns included: first for the X-Axis, second for the Y-Axis, third for the Z-Axis"
					+ " Please exclude/include additional columns until there are exactly 2 on the right hand side.");
		}

		m_settings.setXAxisVarColumn(nameArr[0]);
		m_settings.setYAxisVarColumn(nameArr[1]);
		m_settings.setZAxisVarColumn(nameArr[2]);
		m_settings.setIsClustered(m_isClustered.isSelected());
		m_settings.setClusterType(m_clusterType.getSelectedItem().toString());
		m_settings.setNumClusters((int) m_numClusters.getValue());
		m_settings.setPrototypesProvided(m_prototypesProvided.isSelected());

		m_settings.setShowAllData(m_showAllData.isSelected());
		m_settings.setCount((int)m_count.getModel().getValue());
		m_settings.setDataPointSize((int)m_dataPointSize.getValue());
		m_settings.setPrototypePointSize((int)m_prototypePointSize.getValue());
		m_settings.setPrototypePointColor(m_prototypePointColor.getColor());
		
		m_colSelectionPanel.saveConfiguration(m_settings.getFilterConfiguration());
		
		m_settings.saveSettingsTo(settings);

	}
}

