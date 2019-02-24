package io.github.blaney83;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

public class ScatterPlot3DSettings {

	// static internal config keys
	static final String CFGKEY_COUNT = "count";
	static final String CFGKEY_COL_NAME = "columnName";
	static final String CFGKEY_COL_FILTER = "colFilter";
	static final String CFGKEY_X_AXIS_VAR_COLUMN = "xAxisVarColumn";
	static final String CFGKEY_Y_AXIS_VAR_COLUMN = "yAxisVarColumn";
	static final String CFGKEY_APPEND_CALCULATED_TARGET = "calculatedTarget";
	static final String CFGKEY_NUM_APPROX_COLUMN = "numApproxColumns";
	static final String CFGKEY_SHOW_ALL = "showAllData";
	static final String CFGKEY_SHOW_REG_MODEL = "showRegressionModel";
	static final String CFGKEY_FILTER_TYPE = "filterType";
	static final String CFGKEY_INCLUDED_COLUMNS = "includedColumns";
	static final String CFGKEY_EXCLUDED_COLUMNS = "excludedColumns";
	static final String CFGKEY_FILTER_ENFORCE = "enforceOption";
	static final String CFGKEY_FILTER_TYPE_LIST = "typeList";
	static final String CFGKEY_DEFAULT_SETTINGS_NAME = "defaultSettings";
	static final String CFGKEY_FILTER_SETTINGS_NAME = "filterSettings";
	static final String CFGKEY_SUB_SETTINGS_DATA_TYPE = "dataType"; 
	

	// static internal defaults
	static final int DEFAULT_COUNT = 100;
	static final boolean DEFAULT_APPEND_CALCULATED_TARGET = false;
	static final boolean DEFAULT_IS_H2O_NODE = false;
	static final boolean DEFAULT_SHOW_ALL_DATA = false;
	static final boolean DEFAULT_SHOW_REG_MODEL = true;
	static final String DEFAULT_FILTER_TYPE = "STANDARD";

	// settings model declarations
	private final SettingsModelIntegerBounded m_count = new SettingsModelIntegerBounded(CFGKEY_COUNT,
			DEFAULT_COUNT, 0, Integer.MAX_VALUE);
	protected final SettingsModelColumnName m_colName = new SettingsModelColumnName(CFGKEY_COL_NAME, "");
	protected final SettingsModelColumnName m_xAxisVarColumn = new SettingsModelColumnName(CFGKEY_X_AXIS_VAR_COLUMN,
			"");
	protected final SettingsModelColumnName m_yAxisVarColumn = new SettingsModelColumnName(CFGKEY_Y_AXIS_VAR_COLUMN,
			"");
	protected final SettingsModelBoolean m_appendCalculatedTarget = new SettingsModelBoolean(
			CFGKEY_APPEND_CALCULATED_TARGET, DEFAULT_APPEND_CALCULATED_TARGET);
	protected final SettingsModelBoolean m_showAllData = new SettingsModelBoolean(CFGKEY_SHOW_ALL,
			DEFAULT_SHOW_ALL_DATA);
	protected final SettingsModelBoolean m_showRegressionModel = new SettingsModelBoolean(CFGKEY_SHOW_REG_MODEL,
			DEFAULT_SHOW_REG_MODEL);

	@SuppressWarnings("unchecked")
	protected final DataColumnSpecFilterConfiguration m_filterConfiguration = new DataColumnSpecFilterConfiguration(
			CFGKEY_COL_FILTER, new DataTypeColumnFilter(DoubleValue.class),
			DataColumnSpecFilterConfiguration.FILTER_BY_DATATYPE | NameFilterConfiguration.FILTER_BY_NAMEPATTERN);
	
	public void loadSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_count.setIntValue(settings.getInt(CFGKEY_COUNT));
		m_colName.setStringValue(settings.getString(CFGKEY_COL_NAME));
		m_filterConfiguration.loadConfigurationInModel(settings);
		m_xAxisVarColumn.setStringValue(settings.getString(CFGKEY_X_AXIS_VAR_COLUMN));
		m_yAxisVarColumn.setStringValue(settings.getString(CFGKEY_Y_AXIS_VAR_COLUMN));
		m_appendCalculatedTarget.setBooleanValue(settings.getBoolean(CFGKEY_APPEND_CALCULATED_TARGET));
		m_showAllData.setBooleanValue(settings.getBoolean(CFGKEY_SHOW_ALL));
		m_showRegressionModel.setBooleanValue(settings.getBoolean(CFGKEY_SHOW_REG_MODEL));
		
	}

	public void loadSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec spec)
			throws InvalidSettingsException {
		loadSettingsFrom(settings);
		m_filterConfiguration.loadConfigurationInDialog(settings, spec);
	}

	public void saveSettingsTo(final NodeSettingsWO settings) {
		if (m_colName != null) {
			settings.addInt(CFGKEY_COUNT, m_count.getIntValue());
			settings.addString(CFGKEY_COL_NAME, m_colName.getStringValue());
			settings.addString(CFGKEY_X_AXIS_VAR_COLUMN, m_xAxisVarColumn.getStringValue());
			settings.addString(CFGKEY_Y_AXIS_VAR_COLUMN, m_yAxisVarColumn.getStringValue());
			settings.addBoolean(CFGKEY_APPEND_CALCULATED_TARGET, m_appendCalculatedTarget.getBooleanValue());
			settings.addBoolean(CFGKEY_SHOW_ALL, m_showAllData.getBooleanValue());
			settings.addBoolean(CFGKEY_SHOW_REG_MODEL, m_showRegressionModel.getBooleanValue());
			m_filterConfiguration.saveConfiguration(settings);
		}
	}

	static NodeSettings createDefaults(final String configName, final String[] included, final String[] excluded, final boolean includeAll) {
		NodeSettings defaultSettings = new NodeSettings(CFGKEY_DEFAULT_SETTINGS_NAME);
		NodeSettings filterSettings = (NodeSettings)defaultSettings.addNodeSettings(CFGKEY_FILTER_SETTINGS_NAME);
		filterSettings.addString(CFGKEY_FILTER_TYPE, DEFAULT_FILTER_TYPE);
		filterSettings.addStringArray(CFGKEY_INCLUDED_COLUMNS, included);
		filterSettings.addStringArray(CFGKEY_EXCLUDED_COLUMNS, excluded);
		filterSettings.addString(CFGKEY_FILTER_ENFORCE, (includeAll ? EnforceOption.EnforceExclusion : EnforceOption.EnforceInclusion).name());
		NodeSettings dataTypeSettings = (NodeSettings)filterSettings.addNodeSettings(CFGKEY_SUB_SETTINGS_DATA_TYPE);
		NodeSettingsWO typeList = dataTypeSettings.addNodeSettings(CFGKEY_FILTER_TYPE_LIST);
		typeList.addBoolean(DoubleValue.class.getName(), true);
		return defaultSettings;
	}
	
	
	public void setColName(final String name) {
		this.m_colName.setStringValue(name);
	}

	public void setXAxisVarColumn(final String xName) {
		this.m_xAxisVarColumn.setStringValue(xName);
	}

	public void setYAxisVarColumn(final String yName) {
		this.m_yAxisVarColumn.setStringValue(yName);
	}

	public void setAppendColumn(final boolean append) {
		this.m_appendCalculatedTarget.setBooleanValue(append);
	}

	public void setCount(final int count) {
		this.m_count.setIntValue(count);
	}

	public void setShowAllData(final boolean showData) {
		this.m_showAllData.setBooleanValue(showData);
	}

	public void setShowRegModel(final boolean showRegModel) {
		this.m_showRegressionModel.setBooleanValue(showRegModel);
	}

	public String getColName() {
		return this.m_colName.getStringValue();
	}

	public String getXAxisVarColumn() {
		return this.m_xAxisVarColumn.getStringValue();
	}

	public String getYAxisVarColumn() {
		return this.m_yAxisVarColumn.getStringValue();
	}

	public boolean getAppendColumn() {
		return this.m_appendCalculatedTarget.getBooleanValue();
	}

	public int getCount() {
		return this.m_count.getIntValue();
	}

	public boolean getShowAllData() {
		return this.m_showAllData.getBooleanValue();
	}

	public boolean getShowRegModel() {
		return this.m_showRegressionModel.getBooleanValue();
	}

	public DataColumnSpecFilterConfiguration getFilterConfiguration() {
		return m_filterConfiguration;
	}
}
