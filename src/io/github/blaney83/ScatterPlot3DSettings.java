package io.github.blaney83;

import java.awt.Color;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColor;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataTypeColumnFilter;

public class ScatterPlot3DSettings {

	// port settings
	public static final int MAIN_DATA_TABLE_IN_PORT = 0;
	public static final int PROTOTYPE_TABLE_IN_PORT = 1;

	// externals file config
	protected static final String FILE_NAME = "scatter_plot_3_d.xml";

	// static internal config keys
	static final String CFGKEY_X_AXIS_VAR_COLUMN = "xAxisVarColumn";
	static final String CFGKEY_Y_AXIS_VAR_COLUMN = "yAxisVarColumn";
	static final String CFGKEY_Z_AXIS_VAR_COLUMN = "zAxisVarColumn";
	static final String CFGKEY_PRE_CLUSTERED_DATA = "isClustered";
	static final String CFGKEY_NUMBER_CLUSTERS = "numClusters";
	static final String CFGKEY_CLUSTER_TYPE = "typeCluster";
	static final String CFGKEY_PROTOTYPES_PROVIDED = "prototypesProvided";


	// filter cfg keys
	static final String CFGKEY_COL_FILTER = "colFilter";
	static final String CFGKEY_FILTER_TYPE = "filterType";
	static final String CFGKEY_FILTER_ENFORCE = "enforceOption";
	static final String CFGKEY_FILTER_TYPE_LIST = "typeList";
	static final String CFGKEY_DEFAULT_SETTINGS_NAME = "defaultSettings";
	static final String CFGKEY_FILTER_SETTINGS_NAME = "filterSettings";
	static final String CFGKEY_SUB_SETTINGS_DATA_TYPE = "dataType";
	static final String CFGKEY_INCLUDED_COLUMNS = "includedColumns";
	static final String CFGKEY_EXCLUDED_COLUMNS = "excludedColumns";

	// view configs
	static final String CFGKEY_SHOW_ALL = "showAllData";
	static final String CFGKEY_COUNT = "count";
	static final String CFGKEY_DBSCAN_PLOT_NOISE_MEMBERS = "dbNoiseMembers";
	static final String CFGKEY_DATA_POINT_SIZE = "pointSize";
	static final String CFGKEY_PROTOTYPE_POINT_SIZE = "protoSize";
	static final String CFGKEY_PROTOTYPE_POINT_COLOR = "protoColor";
	static final String CFGKEY_DBSCAN_NOISE_MEMBER_COLOR = "dbNoiseColor";

	// static internal defaults
	static final boolean DEFAULT_PRE_CLUSTERED_DATA = false;
	static final String DEFAULT_CLUSTER_TYPE = "K-Means";
	static final String[] DEFAULT_CLUSTER_TYPES_ARRAY = new String[]{"K-Means", "Fuzzy C-Means", "DBSCAN"};
	static final String DEFAULT_FILTER_TYPE = "STANDARD";
	static final boolean DEFAULT_PROTOTYPES_PROVIDED = false;

	// view defaults
	static final boolean DEFAULT_SHOW_ALL_DATA = false;
	static final int DEFAULT_COUNT = 100;
	static final boolean DEFAULT_DBSCAN_PLOT_NOISE_MEMBERS = true;
	static final int DEFAULT_DATA_POINT_SIZE = 5;
	static final int DEFAULT_PROTOTYPE_POINT_SIZE = 10;
	static final Color DEFAULT_PROTOTYPE_POINT_COLOR = new Color(255, 255, 255);
	static final Color DEFAULT_DBSCAN_NOISE_MEMBER_COLOR = new Color(255, 255, 255);
	static final int PANEL_WIDTH = 800;
	static final int PANEL_HEIGHT = 650;

	// settings model declarations
	protected final SettingsModelColumnName m_xAxisVarColumn = new SettingsModelColumnName(CFGKEY_X_AXIS_VAR_COLUMN,
			"");
	protected final SettingsModelColumnName m_yAxisVarColumn = new SettingsModelColumnName(CFGKEY_Y_AXIS_VAR_COLUMN,
			"");
	protected final SettingsModelColumnName m_zAxisVarColumn = new SettingsModelColumnName(CFGKEY_Z_AXIS_VAR_COLUMN,
			"");
	protected final SettingsModelBoolean m_isClustered = new SettingsModelBoolean(CFGKEY_PRE_CLUSTERED_DATA,
			DEFAULT_PRE_CLUSTERED_DATA);
	protected final SettingsModelIntegerBounded m_numClusters = new SettingsModelIntegerBounded(CFGKEY_NUMBER_CLUSTERS,
			0, 0, Integer.MAX_VALUE);
	protected final SettingsModelString m_clusterType = new SettingsModelString(CFGKEY_CLUSTER_TYPE,
			DEFAULT_CLUSTER_TYPE);
	protected final SettingsModelBoolean m_prototypesProvided = new SettingsModelBoolean(CFGKEY_PROTOTYPES_PROVIDED, DEFAULT_PROTOTYPES_PROVIDED);
	
	// view settings model declarations
	protected final SettingsModelBoolean m_showAllData = new SettingsModelBoolean(CFGKEY_SHOW_ALL,
			DEFAULT_SHOW_ALL_DATA);
	private final SettingsModelIntegerBounded m_count = new SettingsModelIntegerBounded(CFGKEY_COUNT, DEFAULT_COUNT, 0,
			Integer.MAX_VALUE);
	private final SettingsModelBoolean m_dbscanPlotNoise = new SettingsModelBoolean(CFGKEY_DBSCAN_PLOT_NOISE_MEMBERS, DEFAULT_DBSCAN_PLOT_NOISE_MEMBERS);
	protected final SettingsModelIntegerBounded m_dataPointSize = new SettingsModelIntegerBounded(
			CFGKEY_DATA_POINT_SIZE, DEFAULT_DATA_POINT_SIZE, 1, Integer.MAX_VALUE);
	protected final SettingsModelIntegerBounded m_prototypePointSize = new SettingsModelIntegerBounded(
			CFGKEY_PROTOTYPE_POINT_SIZE, DEFAULT_PROTOTYPE_POINT_SIZE, 1, Integer.MAX_VALUE);
	protected final SettingsModelColor m_prototypePointColor = new SettingsModelColor(CFGKEY_PROTOTYPE_POINT_COLOR, DEFAULT_PROTOTYPE_POINT_COLOR);
	protected final SettingsModelColor m_dbNoiseMemberColor = new SettingsModelColor(CFGKEY_DBSCAN_NOISE_MEMBER_COLOR, DEFAULT_DBSCAN_NOISE_MEMBER_COLOR);
	

	@SuppressWarnings("unchecked")
	protected final DataColumnSpecFilterConfiguration m_filterConfiguration = new DataColumnSpecFilterConfiguration(
			CFGKEY_COL_FILTER, new DataTypeColumnFilter(DoubleValue.class),
			DataColumnSpecFilterConfiguration.FILTER_BY_DATATYPE | NameFilterConfiguration.FILTER_BY_NAMEPATTERN);

	public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		try {
			m_filterConfiguration.loadConfigurationInModel(settings);
			m_xAxisVarColumn.setStringValue(settings.getString(CFGKEY_X_AXIS_VAR_COLUMN));
			m_yAxisVarColumn.setStringValue(settings.getString(CFGKEY_Y_AXIS_VAR_COLUMN));
			m_zAxisVarColumn.setStringValue(settings.getString(CFGKEY_Z_AXIS_VAR_COLUMN));
			m_isClustered.setBooleanValue(settings.getBoolean(CFGKEY_PRE_CLUSTERED_DATA));
			m_numClusters.setIntValue(settings.getInt(CFGKEY_NUMBER_CLUSTERS));
			m_clusterType.setStringValue(settings.getString(CFGKEY_CLUSTER_TYPE));
			m_prototypesProvided.setBooleanValue(settings.getBoolean(CFGKEY_PROTOTYPES_PROVIDED));
			
			m_showAllData.setBooleanValue(settings.getBoolean(CFGKEY_SHOW_ALL));
			m_count.setIntValue(settings.getInt(CFGKEY_COUNT));
			m_dbscanPlotNoise.setBooleanValue(settings.getBoolean(CFGKEY_DBSCAN_PLOT_NOISE_MEMBERS));
			m_dataPointSize.setIntValue(settings.getInt(CFGKEY_DATA_POINT_SIZE));
			m_prototypePointSize.setIntValue(settings.getInt(CFGKEY_PROTOTYPE_POINT_SIZE));
			m_prototypePointColor.setColorValue(new Color(settings.getInt(CFGKEY_PROTOTYPE_POINT_COLOR)));
			m_dbNoiseMemberColor.setColorValue(new Color(settings.getInt(CFGKEY_DBSCAN_NOISE_MEMBER_COLOR)));
			
		} catch (InvalidSettingsException e) {
			throw new InvalidSettingsException("The internal settings for 3D Scatter Plot Node could not be successfully loaded. Please check the"
					+ "configuration before execution.");
		}
	}

	public void loadSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec spec)
			throws InvalidSettingsException {
		loadSettingsFrom(settings);
		m_filterConfiguration.loadConfigurationInDialog(settings, spec);
	}

	public void saveSettingsTo(final NodeSettingsWO settings) {
		if (m_xAxisVarColumn != null && m_yAxisVarColumn != null && m_zAxisVarColumn != null) {
			settings.addString(CFGKEY_X_AXIS_VAR_COLUMN, m_xAxisVarColumn.getStringValue());
			settings.addString(CFGKEY_Y_AXIS_VAR_COLUMN, m_yAxisVarColumn.getStringValue());
			settings.addString(CFGKEY_Z_AXIS_VAR_COLUMN, m_zAxisVarColumn.getStringValue());
			settings.addBoolean(CFGKEY_PRE_CLUSTERED_DATA, m_isClustered.getBooleanValue());
			settings.addInt(CFGKEY_NUMBER_CLUSTERS, m_numClusters.getIntValue());
			settings.addString(CFGKEY_CLUSTER_TYPE, m_clusterType.getStringValue());
			settings.addBoolean(CFGKEY_PROTOTYPES_PROVIDED, m_prototypesProvided.getBooleanValue());
			
			settings.addBoolean(CFGKEY_SHOW_ALL, m_showAllData.getBooleanValue());
			settings.addInt(CFGKEY_COUNT, m_count.getIntValue());
			settings.addBoolean(CFGKEY_DBSCAN_PLOT_NOISE_MEMBERS, DEFAULT_DBSCAN_PLOT_NOISE_MEMBERS);
			settings.addInt(CFGKEY_DATA_POINT_SIZE, m_dataPointSize.getIntValue());
			settings.addInt(CFGKEY_PROTOTYPE_POINT_SIZE, m_prototypePointSize.getIntValue());
			settings.addInt(CFGKEY_PROTOTYPE_POINT_COLOR, m_prototypePointColor.getColorValue().getRGB());
			settings.addInt(CFGKEY_DBSCAN_NOISE_MEMBER_COLOR, m_dbNoiseMemberColor.getColorValue().getRGB());
			m_filterConfiguration.saveConfiguration(settings);
		}
	}

	static NodeSettings createDefaults(final String configName, final String[] included, final String[] excluded,
			final boolean includeAll) {
		NodeSettings defaultSettings = new NodeSettings(CFGKEY_DEFAULT_SETTINGS_NAME);
		NodeSettings filterSettings = (NodeSettings) defaultSettings.addNodeSettings(CFGKEY_FILTER_SETTINGS_NAME);
		filterSettings.addString(CFGKEY_FILTER_TYPE, DEFAULT_FILTER_TYPE);
		filterSettings.addStringArray(CFGKEY_INCLUDED_COLUMNS, included);
		filterSettings.addStringArray(CFGKEY_EXCLUDED_COLUMNS, excluded);
		filterSettings.addString(CFGKEY_FILTER_ENFORCE,
				(includeAll ? EnforceOption.EnforceExclusion : EnforceOption.EnforceInclusion).name());
		NodeSettings dataTypeSettings = (NodeSettings) filterSettings.addNodeSettings(CFGKEY_SUB_SETTINGS_DATA_TYPE);
		NodeSettingsWO typeList = dataTypeSettings.addNodeSettings(CFGKEY_FILTER_TYPE_LIST);
		typeList.addBoolean(DoubleValue.class.getName(), true);
		return defaultSettings;
	}

	// Setters
	public void setXAxisVarColumn(final String xName) {
		this.m_xAxisVarColumn.setStringValue(xName);
	}

	public void setYAxisVarColumn(final String yName) {
		this.m_yAxisVarColumn.setStringValue(yName);
	}
	
	public void setZAxisVarColumn(final String yName) {
		this.m_zAxisVarColumn.setStringValue(yName);
	}
	
	public void setIsClustered(final boolean isClustered) {
		this.m_isClustered.setBooleanValue(isClustered);
	}
	
	public void setNumClusters(final int numClusters) {
		this.m_numClusters.setIntValue(numClusters);
	}

	public void setClusterType(final String clusterType) {
		this.m_clusterType.setStringValue(clusterType);
	}
	
	public void setPrototypesProvided(final boolean prototypesProvided) {
		this.m_prototypesProvided.setBooleanValue(prototypesProvided);
	}
	
	public void setShowAllData(final boolean showData) {
		this.m_showAllData.setBooleanValue(showData);
	}

	public void setCount(final int count) {
		this.m_count.setIntValue(count);
	}
	
	public void setDBSCANPlotNoise(final boolean dbPlotNoise) {
		this.m_dbscanPlotNoise.setBooleanValue(dbPlotNoise);
	}
	
	public void setDataPointSize(final int dataPointSize) {
		this.m_dataPointSize.setIntValue(dataPointSize);
	}
	
	public void setPrototypePointSize(final int prototypePointSize) {
		this.m_prototypePointSize.setIntValue(prototypePointSize);
	}
	
	public void setPrototypePointColor(final Color prototypePointColor) {
		this.m_prototypePointColor.setColorValue(prototypePointColor);
	}
	
	public void setDBNoiseMemberColor(final Color dbNoiseMemberColor) {
		this.m_dbNoiseMemberColor.setColorValue(dbNoiseMemberColor);
	}
	
	// Getters
	public String getXAxisVarColumn() {
		return this.m_xAxisVarColumn.getStringValue();
	}

	public String getYAxisVarColumn() {
		return this.m_yAxisVarColumn.getStringValue();
	}

	public String getZAxisVarColumn() {
		return this.m_zAxisVarColumn.getStringValue();
	}
	
	public boolean getIsClustered() {
		return this.m_isClustered.getBooleanValue();
	}

	public int getNumClusters() {
		return this.m_numClusters.getIntValue();
	}
	
	public String getClusterType() {
		return this.m_clusterType.getStringValue();
	}
	
	public boolean getPrototypesProvided() {
		return this.m_prototypesProvided.getBooleanValue();
	}
	
	public boolean getShowAllData() {
		return this.m_showAllData.getBooleanValue();
	}
	
	public int getCount() {
		return this.m_count.getIntValue();
	}
	
	public boolean getDBSCANPlotNoise() {
		return this.m_dbscanPlotNoise.getBooleanValue();
	}
	
	public int getDataPointSize() {
		return this.m_dataPointSize.getIntValue();
	}
	
	public int getPrototypePointSize() {
		return this.m_prototypePointSize.getIntValue();
	}
	
	public Color getPrototypePointColor() {
		return this.m_prototypePointColor.getColorValue();
	}
	
	public Color getDBNoiseMemberColor() {
		return this.m_dbNoiseMemberColor.getColorValue();
	}
	
	public DataColumnSpecFilterConfiguration getFilterConfiguration() {
		return m_filterConfiguration;
	}
}
