package io.github.blaney83;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This is the model implementation of ScatterPlot3D. A 3 Dimensional
 * representation of points in space for a given data set.
 *
 * @author Benjamin Laney
 */
public class ScatterPlot3DNodeModel extends NodeModel {
	// considerations:
	// maybe include key for cluster colors
	// maybe include settings for point transparency (alpha)
	// maybe add settings for prototype color and size
	// maybe use color mapper for points w/o clustering

	ScatterPlot3DSettings m_settings = new ScatterPlot3DSettings();

	// save/load cfg keys
	static final String INTERNAL_MODEL_NAME_KEY = "internalModel";
	static final String INTERNAL_MODEL_NUM_FUNCTION_TERM_KEY = "numFnTerms";
	static final String INTERNAL_MODEL_NUM_CALC_POINT_KEY = "numCalcPoints";
	static final String INTERNAL_MODEL_TERM_KEY = "fnTerm";
	static final String INTERNAL_MODEL_POINT_KEY = "calcPoint";

	// view dependent fields
	private Coord3d[] m_dataPoints;
	private Color[] m_dataPointColors;
	private short[] m_dataPointColorIndicies;
	private Coord3d[] m_protoTypePoints;


	private int m_xColIndex = -1;
	private int m_yColIndex = -1;
	private int m_zColIndex = -1;
	private int m_clusterColumnIndex = -1;
	
	private int m_xColProtoIndex = -1;
	private int m_yColProtoIndex = -1;
	private int m_zColProtoIndex = -1;

	protected ScatterPlot3DNodeModel() {
		super(2, 1);
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		coordPointFactory(inData);
		BufferedDataTable bufferedOutput;
		bufferedOutput = exec.createBufferedDataTable(inData[ScatterPlot3DSettings.MAIN_DATA_TABLE_IN_PORT], exec);
		return new BufferedDataTable[] { bufferedOutput };
	}

	private void coordPointFactory(final BufferedDataTable[] inData)
			throws InvalidSettingsException, IndexOutOfBoundsException {
		BufferedDataTable mainDataTable = inData[ScatterPlot3DSettings.MAIN_DATA_TABLE_IN_PORT];
		int numPoints = m_settings.getCount();
		int totalPointsCreated = 0;
		if (m_settings.getShowAllData()) {
			numPoints = (int) mainDataTable.size();
		}
		m_dataPoints = new Coord3d[numPoints];
		m_dataPointColorIndicies = new short[numPoints];
		int numColors = 1;
		if (m_settings.getIsClustered()) {
			numColors = m_settings.getNumClusters();
			if (m_settings.getClusterType().equals("DBSCAN") && m_settings.getDBSCANPlotNoise()) {
				numColors++;
			}
		}
		m_dataPointColors = new Color[numColors];
		int dynamicColorValue = 255 / numColors + 1;
		for (int i = 0; i < numColors; i++) {
			if (i == numColors - 1 && m_settings.getClusterType().equals("DBSCAN")) {
				m_dataPointColors[i] = new Color(m_settings.getDBNoiseMemberColor().getRed(),
						m_settings.getDBNoiseMemberColor().getBlue(), m_settings.getDBNoiseMemberColor().getGreen(),
						m_settings.getDBNoiseMemberColor().getAlpha());
				break;
			}
			m_dataPointColors[i] = new Color(dynamicColorValue * (i + 1), dynamicColorValue * (i + 1),
					dynamicColorValue * (i + 1));
		}

		for (DataRow row : mainDataTable) {
			if (totalPointsCreated >= numPoints) {
				break;
			}
			if (!m_settings.getIsClustered()) {
				m_dataPoints[totalPointsCreated] = new Coord3d(Double.valueOf(row.getCell(m_xColIndex).toString()),
						Double.valueOf(row.getCell(m_yColIndex).toString()),
						Double.valueOf(row.getCell(m_zColIndex).toString()));
				m_dataPointColorIndicies[totalPointsCreated] = 0;
			}else {
				if (m_settings.getIsClustered() && m_clusterColumnIndex > -1) {
					String[] clusterMembership = row.getCell(m_clusterColumnIndex).toString().toLowerCase().split("_");
					if(m_settings.getClusterType().equals("DBSCAN")) {
						if(!m_settings.getDBSCANPlotNoise()) {
							if(clusterMembership.length == 1) {
								totalPointsCreated ++;
								continue;
							}
							m_dataPoints[totalPointsCreated] = new Coord3d(Double.valueOf(row.getCell(m_xColIndex).toString()),
									Double.valueOf(row.getCell(m_yColIndex).toString()),
									Double.valueOf(row.getCell(m_zColIndex).toString()));
							m_dataPointColorIndicies[totalPointsCreated] = Short.valueOf(clusterMembership[1]);
						} else {
							if(clusterMembership.length == 1) {
								m_dataPointColorIndicies[totalPointsCreated] = Short.valueOf(String.valueOf((m_dataPointColorIndicies.length-1)));
							} else {
								m_dataPointColorIndicies[totalPointsCreated] = Short.valueOf(clusterMembership[1]);
							}
							m_dataPoints[totalPointsCreated] = new Coord3d(Double.valueOf(row.getCell(m_xColIndex).toString()),
									Double.valueOf(row.getCell(m_yColIndex).toString()),
									Double.valueOf(row.getCell(m_zColIndex).toString()));
						}
					}else {
						m_dataPoints[totalPointsCreated] = new Coord3d(Double.valueOf(row.getCell(m_xColIndex).toString()),
								Double.valueOf(row.getCell(m_yColIndex).toString()),
								Double.valueOf(row.getCell(m_zColIndex).toString()));
						m_dataPointColorIndicies[totalPointsCreated] = Short.valueOf(clusterMembership[1]);
					}
				} else if (m_settings.getIsClustered() && m_clusterColumnIndex == -1) {
					throw new InvalidSettingsException(
							"Cluster memberships could not be determined at runtime. Please reconfigure node");
				}
			}
			totalPointsCreated++;
		}
		
		if(inData.length > 1 && m_settings.getPrototypesProvided()) {
			BufferedDataTable prototypeTable = inData[ScatterPlot3DSettings.PROTOTYPE_TABLE_IN_PORT];
			//could use user provided cluster number and catch errors, but using ProtoTable size instead
			int trueClusterNumber = (int) prototypeTable.size();
			m_protoTypePoints = new Coord3d[trueClusterNumber];
			int count = 0;
			for(DataRow row : prototypeTable) {
				m_protoTypePoints[count] = new Coord3d(Double.valueOf(row.getCell(m_xColProtoIndex).toString()),
						Double.valueOf(row.getCell(m_yColProtoIndex).toString()),
						Double.valueOf(row.getCell(m_zColProtoIndex).toString()));
			}
		}
	}

	@Override
	protected void reset() {
		// do nothing
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

		if (inSpecs[ScatterPlot3DSettings.MAIN_DATA_TABLE_IN_PORT] == null) {
			throw new InvalidSettingsException("Please provide a data table to In-Port #1.");
		} else {
			// validate three columns of double-compatible values inport 1
			// validate only three matching columns
			// if isClustered == true, then validate clusters column (throw error if not
			// cluster column found for given type)
			// maybe throw warning about correct numClusters and crash if incorrect...
			// if clusterType = "k-means" and setProvidePrototypes = true, but inport 2 DNE,
			// throw error
			// else if k-means and inport 2 exists, setProvidedPrototypes = true
			// if inport 2 exists, validate three columns of doubles
			// validate matching columns selected and matching prototype columns
			// if inport 2 exists, throw warning if not K-means (maybe)

			int doubleCompatColCount = 0;
			DataTableSpec mainTableSpec = inSpecs[ScatterPlot3DSettings.MAIN_DATA_TABLE_IN_PORT];
			String clusterType = m_settings.getClusterType();
			boolean clusterColumnIdentified = false;
			for (int i = 0; i < mainTableSpec.getNumColumns(); i++) {
				if (mainTableSpec.getColumnSpec(i).getType().isCompatible(DoubleValue.class)) {
					doubleCompatColCount++;
				}
				if (m_settings.getIsClustered()) {
					switch (clusterType) {
					case "K-Means":
					case "DBSCAN":
						if (mainTableSpec.getColumnSpec(i).getName().equals("Cluster")) {
							clusterColumnIdentified = true;
							m_clusterColumnIndex = i;
						}
						break;
					case "Fuzzy C-Means":
						if (mainTableSpec.getColumnSpec(i).getName().equals("Winner Cluster")) {
							clusterColumnIdentified = true;
							m_clusterColumnIndex = i;
						}
					default:
						break;
					}
				}
				if (mainTableSpec.getColumnSpec(i).getName().equals(m_settings.getXAxisVarColumn())) {
					m_xColIndex = i;
				} else if (mainTableSpec.getColumnSpec(i).getName().equals(m_settings.getYAxisVarColumn())) {
					m_yColIndex = i;
				} else if (mainTableSpec.getColumnSpec(i).getName().equals(m_settings.getZAxisVarColumn())) {
					m_zColIndex = i;
				}
			}
			if (doubleCompatColCount < 3) {
				throw new InvalidSettingsException(
						"The data table provided does not contain three numeric columns for plotting.");
			}
			if (!clusterColumnIdentified) {
				throw new InvalidSettingsException(
						"You have indicated that the data table has been previous clustered, but this node cannot"
								+ " detect a cluster membership column. Please make sure you have correctly chosen the clustering method used during"
								+ " that process. Currently this node supports K-Means, Fuzzy C-Means and DBSCAN clustered data from KNIME developed "
								+ "clustering nodes. Compatiblity with other clustered data is currently not assured and should be used at your own "
								+ "risk.");
			}
			// should never happen, but check for good practice
			if (m_xColIndex == -1 || m_yColIndex == -1 || m_zColIndex == -1) {
				throw new InvalidSettingsException(
						"The columns you selected could not be found in the table. Please reset the node or delete, recreate, and "
								+ "reconfigure the node.");
			}
			if (m_settings.getIsClustered()) {
				setWarningMessage(
						"You have indicated that the data has been pre-clustered. Please ensure you correctly enter the number of clusters "
								+ "calculated for the data set. If you are using DBSCAN clustering, do NOT include the noise cluster in your count.");
			}
			if ((!m_settings.getIsClustered()
					|| (m_settings.getIsClustered() && !m_settings.getClusterType().equals("K-Means")))
					&& inSpecs.length > 1) {
				throw new InvalidSettingsException(
						"You have provided a secondary prototype table for a data set that has not been identified as "
								+ "having been clustered using a K-Means node. This is currently not supported. Please disconnect the data table from "
								+ "In-Port #2 or correctly select the 'K-Means' cluster type from the options.");
			}

			if (m_settings.getIsClustered() && m_settings.getClusterType().equals("K-Means")
					&& m_settings.getPrototypesProvided() && inSpecs.length == 1) {
				throw new InvalidSettingsException(
						"You have indicated that you are providing a prototype table at In-Port 2, but one is not present. "
								+ "Please provide the table generated by the K-Means node or change your preferences to reflect the absence of this table.");
			}

			if (m_settings.getIsClustered() && m_settings.getClusterType().equals("K-Means") && inSpecs.length > 1) {
				DataTableSpec prototypeTableSpec = inSpecs[ScatterPlot3DSettings.PROTOTYPE_TABLE_IN_PORT];
				if (!m_settings.getPrototypesProvided()) {
					m_settings.setPrototypesProvided(true);
				}
				if (prototypeTableSpec.getNumColumns() < 3) {
					throw new InvalidSettingsException(
							"The prototype table provided does not contain the minimum three columns for plotting.");
				}
				doubleCompatColCount = 0;
				boolean containsXVar = false;
				boolean containsYVar = false;
				boolean containsZVar = false;
				for (int i = 0; i < prototypeTableSpec.getNumColumns(); i++) {
					if (prototypeTableSpec.getColumnSpec(i).getType().isCompatible(DoubleValue.class)) {
						doubleCompatColCount++;
					}
					if (prototypeTableSpec.getColumnSpec(i).getName().equals(m_settings.getXAxisVarColumn())) {
						containsXVar = true;
						m_xColProtoIndex = i;
					} else if (prototypeTableSpec.getColumnSpec(i).getName().equals(m_settings.getYAxisVarColumn())) {
						containsYVar = true;
						m_yColProtoIndex = i;
					} else if (prototypeTableSpec.getColumnSpec(i).getName().equals(m_settings.getZAxisVarColumn())) {
						containsZVar = true;
						m_zColProtoIndex = i;
					}
				}
				if (doubleCompatColCount < 3) {
					throw new InvalidSettingsException(
							"The prototype table provided does not contain three numeric columns for plotting.");
				}
				if (!containsXVar || !containsYVar || !containsZVar) {
					throw new InvalidSettingsException(
							"The prototype table provided does not match the columns you selected to be plotted on "
									+ "the graph. Please correct your column selections, your prototype settings, or change your settings to reflect the "
									+ "absence of a compatible prototype table.");
				}
			}
			return new DataTableSpec[] { inSpecs[ScatterPlot3DSettings.MAIN_DATA_TABLE_IN_PORT] };
		}
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_settings.saveSettingsTo(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_settings.loadSettingsFrom(settings);
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		ScatterPlot3DSettings s = new ScatterPlot3DSettings();

		s.loadSettingsFrom(settings);

		if (s.getXAxisVarColumn() == null || s.getYAxisVarColumn() == null || s.getZAxisVarColumn() == null) {
			throw new InvalidSettingsException("No target column selected");
		}
	}

	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		File file = new File(internDir, ScatterPlot3DSettings.FILE_NAME);
		try (FileInputStream fis = new FileInputStream(file)) {
//			ModelContentRO modelContent = ModelContent.loadFromXML(fis);
//			try {
//				int numFnTerms = modelContent.getInt(INTERNAL_MODEL_NUM_FUNCTION_TERM_KEY);
//				int numCalcPoints = modelContent.getInt(INTERNAL_MODEL_NUM_CALC_POINT_KEY);
//				m_termSet = new LinkedHashSet<FunctionTerm>();
//				m_calcPoints = new CalculatedPoint[numCalcPoints];
//				for (int i = 0; i < numFnTerms; i++) {
//					FunctionTerm newTerm = new FunctionTerm();
//					ModelContentRO subContent = modelContent.getModelContent(INTERNAL_MODEL_TERM_KEY + i);
//					newTerm.loadFrom(subContent);
//					m_termSet.add(newTerm);
//				}
//
//				for (int i = 0; i < numCalcPoints; i++) {
//					CalculatedPoint newPoint = new CalculatedPoint();
//					ModelContentRO subContent = modelContent.getModelContent(INTERNAL_MODEL_POINT_KEY + i);
//					newPoint.loadFrom(subContent);
//					m_calcPoints[i] = newPoint;
//				}
//
//			} catch (InvalidSettingsException e) {
//				throw new IOException("There was a problem loading the internal state of this node.");
//			}

		}
	}

	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
//		if (m_termSet != null && m_calcPoints != null) {
//			ModelContent modelContent = new ModelContent(INTERNAL_MODEL_NAME_KEY);
//			modelContent.addInt(INTERNAL_MODEL_NUM_FUNCTION_TERM_KEY, m_termSet.size());
//			modelContent.addInt(INTERNAL_MODEL_NUM_CALC_POINT_KEY, m_calcPoints.length);
//			int count = 0;
//			for (FunctionTerm fnTerm : m_termSet) {
//				ModelContentWO subContentWO = modelContent.addModelContent(INTERNAL_MODEL_TERM_KEY + count);
//				fnTerm.saveTo(subContentWO);
//				count++;
//			}
//			count = 0;
//			for (CalculatedPoint calcPoint : m_calcPoints) {
//				ModelContentWO subContentWO = modelContent.addModelContent(INTERNAL_MODEL_POINT_KEY + count);
//				calcPoint.saveTo(subContentWO);
//				count++;
//			}
//			File file = new File(internDir, FILE_NAME);
//			FileOutputStream fos = new FileOutputStream(file);
//			modelContent.saveToXML(fos);
//		}
	}

	protected ScatterPlot3DSettings getSettings() {
		return m_settings;
	}

	protected Coord3d[] getDataPoints() {
		return m_dataPoints;
	}
	
	protected Color[] getDataPointColors() {
		return m_dataPointColors;
	}
	
	protected short[] getDataPointColorIndicies() {
		return m_dataPointColorIndicies;
	}
	
	protected Coord3d[] getProtoTypePoints() {
		return m_protoTypePoints;
	}
}
