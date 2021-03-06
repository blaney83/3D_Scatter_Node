package io.github.blaney83;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
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
	// should be handling or catching mis-matched numClusters before they get to
	// Graph
	// allow color picking for non-clustered data
	// make input 2 optional
	// option for prototype to be same color as cluster (with outline)
	// allow for seeding the color (using mixing algorithm)
	// or just build a better "'n' unique color's algorithm" to increase spread
	// (usefulcodesnips)

	ScatterPlot3DSettings m_settings = new ScatterPlot3DSettings();

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
			if (m_settings.getClusterType().equals("DBSCAN") && m_settings.getDBSCANPlotNoise()
					&& m_settings.getDBSCANPlotNoise()) {
				numColors++;
			}
		}
		m_dataPointColors = new Color[numColors];
		int[] colorVals = new int[] { 246, 0, 0, 246, 246, 0, 246, 123, 0, 246, 123, 123, 123, 123, 0, 246, 164, 82,
				164, 82, 0 };
		int firstCount = 0;
		int secondCount = 1;
		int thirdCount = 2;
		int masterCount = 0;
		double shader = .3;
		float opacity = (float) .6;
		for (int i = 0; i < numColors; i++) {
			if (i > 0 && i % 3 == 0) {
				masterCount += 3;
			}
			if ((i + 1) % 18 == 0) {
				masterCount = 0;
				shader += (1 - shader) / 2;
			}
			m_dataPointColors[i] = new Color((int) Math.round((colorVals[firstCount % 3 + masterCount] * shader)),
					(int) Math.round((colorVals[secondCount % 3 + masterCount] * shader)),
					(int) Math.round((colorVals[thirdCount % 3 + masterCount] * shader)), opacity);
			firstCount++;
			secondCount++;
			thirdCount++;
		}
		int dbNonNoiseCount = 0;
		for (DataRow row : mainDataTable) {
			if (totalPointsCreated >= numPoints) {
				break;
			}
			if (!m_settings.getIsClustered()) {
				m_dataPoints[totalPointsCreated] = new Coord3d(Double.valueOf(row.getCell(m_xColIndex).toString()),
						Double.valueOf(row.getCell(m_yColIndex).toString()),
						Double.valueOf(row.getCell(m_zColIndex).toString()));
				m_dataPointColorIndicies[totalPointsCreated] = 0;
			} else {
				if (m_settings.getIsClustered() && m_clusterColumnIndex > -1) {
					String[] clusterMembership = row.getCell(m_clusterColumnIndex).toString().toLowerCase().split("_");
					if (m_settings.getClusterType().equals("DBSCAN")) {
						if (!m_settings.getDBSCANPlotNoise()) {
							if (clusterMembership.length == 1) {
								totalPointsCreated++;
								continue;
							}
							m_dataPoints[totalPointsCreated] = new Coord3d(
									Double.valueOf(row.getCell(m_xColIndex).toString()),
									Double.valueOf(row.getCell(m_yColIndex).toString()),
									Double.valueOf(row.getCell(m_zColIndex).toString()));
							m_dataPointColorIndicies[totalPointsCreated] = Short.valueOf(clusterMembership[1]);
							dbNonNoiseCount++;
						} else {
							if (clusterMembership.length == 1) {
								m_dataPointColorIndicies[totalPointsCreated] = Short
										.valueOf(String.valueOf((m_dataPointColors.length - 1)));
							} else {
								m_dataPointColorIndicies[totalPointsCreated] = Short.valueOf(clusterMembership[1]);
							}
							m_dataPoints[totalPointsCreated] = new Coord3d(
									Double.valueOf(row.getCell(m_xColIndex).toString()),
									Double.valueOf(row.getCell(m_yColIndex).toString()),
									Double.valueOf(row.getCell(m_zColIndex).toString()));
							// update the final color
							m_dataPointColors[m_dataPointColors.length - 1] = new Color(
									m_settings.getDBNoiseMemberColor().getRed(),
									m_settings.getDBNoiseMemberColor().getBlue(),
									m_settings.getDBNoiseMemberColor().getGreen(),
									m_settings.getDBNoiseMemberColor().getAlpha());
						}
					} else {
						m_dataPoints[totalPointsCreated] = new Coord3d(
								Double.valueOf(row.getCell(m_xColIndex).toString()),
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

		// restructure data arrays to handle DBSCAN no-noise plots
		if (m_settings.getClusterType().equals("DBSCAN") && !m_settings.getDBSCANPlotNoise()
				&& m_settings.getShowAllData() && dbNonNoiseCount > 0) {
			System.out.println("Fired");
			Coord3d[] placeHolderCoords = new Coord3d[dbNonNoiseCount];
			short[] placeHolderIndicies = new short[dbNonNoiseCount];
			int resizeCount = 0;
			for (int j = 0; j < numPoints; j++) {
				if (m_dataPoints[j] != null) {
					placeHolderCoords[resizeCount] = m_dataPoints[j];
					placeHolderIndicies[resizeCount] = m_dataPointColorIndicies[j];
					resizeCount++;
				}
			}
			m_dataPoints = placeHolderCoords;
			m_dataPointColorIndicies = placeHolderIndicies;
		}

		if (inData.length > 1 && m_settings.getPrototypesProvided()) {
			BufferedDataTable prototypeTable = inData[ScatterPlot3DSettings.PROTOTYPE_TABLE_IN_PORT];
			// could use user provided cluster number and catch errors, but using ProtoTable
			// size instead
			int trueClusterNumber = (int) prototypeTable.size();
			m_protoTypePoints = new Coord3d[trueClusterNumber];
			int count = 0;
			for (DataRow row : prototypeTable) {
				m_protoTypePoints[count] = new Coord3d(Double.valueOf(row.getCell(m_xColProtoIndex).toString()),
						Double.valueOf(row.getCell(m_yColProtoIndex).toString()),
						Double.valueOf(row.getCell(m_zColProtoIndex).toString()));
				count++;
			}
		}
	}

	@Override
	protected void reset() {
		if (m_dataPoints != null) {
			m_dataPoints = null;
		}
		if (m_dataPointColorIndicies != null) {
			m_dataPointColorIndicies = null;
		}
		if (m_dataPointColors != null) {
			m_dataPointColors = null;
		}
		if (m_protoTypePoints != null) {
			m_protoTypePoints = null;
		}
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
			if (m_settings.getIsClustered() && !clusterColumnIdentified) {
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
			ModelContentRO modelContent = ModelContent.loadFromXML(fis);
			try {
				int numDataPoints = modelContent.getInt(ScatterPlot3DSettings.INTERNAL_NUM_PLOTTED_POINT);
				m_dataPoints = new Coord3d[numDataPoints];
				m_dataPointColorIndicies = new short[numDataPoints];
				for (int i = 0; i < numDataPoints; i++) {
					System.out.println("x" + modelContent.getDouble(ScatterPlot3DSettings.INTERNAL_X_VAL + i));
					System.out.println("y" + modelContent.getDouble(ScatterPlot3DSettings.INTERNAL_Y_VAL + i));
					System.out.println("z" + modelContent.getDouble(ScatterPlot3DSettings.INTERNAL_Z_VAL + i));
					m_dataPoints[i] = new Coord3d(modelContent.getDouble(ScatterPlot3DSettings.INTERNAL_X_VAL + i),
							modelContent.getDouble(ScatterPlot3DSettings.INTERNAL_Y_VAL + i),
							modelContent.getDouble(ScatterPlot3DSettings.INTERNAL_Z_VAL + i));
					m_dataPointColorIndicies[i] = modelContent.getShort(ScatterPlot3DSettings.INTERNAL_COLOR_INDEX + i);
				}
				int numColors = modelContent.getInt(ScatterPlot3DSettings.INTERNAL_NUM_COLORS);
				m_dataPointColors = new Color[numColors];
				for (int j = 0; j < numColors; j++) {
					m_dataPointColors[j] = new Color(modelContent.getFloat(ScatterPlot3DSettings.INTERNAL_RED_VAL + j),
							modelContent.getFloat(ScatterPlot3DSettings.INTERNAL_BLUE_VAL + j),
							modelContent.getFloat(ScatterPlot3DSettings.INTERNAL_GREEN_VAL + j),
							modelContent.getFloat(ScatterPlot3DSettings.INTERNAL_ALPHA_VAL + j));
				}

				boolean hasStoredProtos = modelContent
						.getBoolean(ScatterPlot3DSettings.INTERNAL_PROTO_POINTS_STORED_PROPERLY);
				if (hasStoredProtos) {
					int numProtoPoints = modelContent.getInt(ScatterPlot3DSettings.INTERNAL_NUM_PROTO_POINTS);
					m_protoTypePoints = new Coord3d[numProtoPoints];
					for (int k = 0; k < numProtoPoints; k++) {
						m_protoTypePoints[k] = new Coord3d(
								modelContent.getDouble(ScatterPlot3DSettings.INTERNAL_PROTO_X_VAL + k),
								modelContent.getDouble(ScatterPlot3DSettings.INTERNAL_PROTO_Y_VAL + k),
								modelContent.getDouble(ScatterPlot3DSettings.INTERNAL_PROTO_Z_VAL + k));
					}
				}

			} catch (InvalidSettingsException e) {
				throw new IOException("There was a problem loading the internal state of this node." + e);
			}

		}
	}

	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// could potentiall store column names, row keys, colors and indicies (or each
		// color with its row keys)
		if (m_dataPoints != null) {
			System.out.println("Saving");
			ModelContent modelContent = new ModelContent(ScatterPlot3DSettings.INTERNAL_MODEL_NAME_KEY);
			modelContent.addInt(ScatterPlot3DSettings.INTERNAL_NUM_PLOTTED_POINT, m_dataPoints.length);
			modelContent.addInt(ScatterPlot3DSettings.INTERNAL_NUM_COLORS, m_dataPointColors.length);
			int count = 0;
			for (Coord3d dataPoint : m_dataPoints) {
				// saving data points
				modelContent.addDouble(ScatterPlot3DSettings.INTERNAL_X_VAL + count, dataPoint.x);
				modelContent.addDouble(ScatterPlot3DSettings.INTERNAL_Y_VAL + count, dataPoint.y);
				modelContent.addDouble(ScatterPlot3DSettings.INTERNAL_Z_VAL + count, dataPoint.z);
				count++;
			}
			count = 0;
			for (Color pointColor : m_dataPointColors) {
				modelContent.addFloat(ScatterPlot3DSettings.INTERNAL_RED_VAL + count, pointColor.r);
				modelContent.addFloat(ScatterPlot3DSettings.INTERNAL_BLUE_VAL + count, pointColor.b);
				modelContent.addFloat(ScatterPlot3DSettings.INTERNAL_GREEN_VAL + count, pointColor.g);
				modelContent.addFloat(ScatterPlot3DSettings.INTERNAL_ALPHA_VAL + count, pointColor.a);
				count++;
			}
			count = 0;
			for (short pointIndex : m_dataPointColorIndicies) {
				modelContent.addShort(ScatterPlot3DSettings.INTERNAL_COLOR_INDEX + count, pointIndex);
				count++;
			}
			modelContent.addBoolean(ScatterPlot3DSettings.INTERNAL_PROTO_POINTS_STORED_PROPERLY, false);
			if (m_settings.getPrototypesProvided() && m_protoTypePoints != null && m_protoTypePoints.length > 0) {
				modelContent.addBoolean(ScatterPlot3DSettings.INTERNAL_PROTO_POINTS_STORED_PROPERLY, true);
				count = 0;
				for (Coord3d protoPoint : m_dataPoints) {
					// saving data points
					modelContent.addDouble(ScatterPlot3DSettings.INTERNAL_PROTO_X_VAL + count, protoPoint.x);
					modelContent.addDouble(ScatterPlot3DSettings.INTERNAL_PROTO_Y_VAL + count, protoPoint.y);
					modelContent.addDouble(ScatterPlot3DSettings.INTERNAL_PROTO_Z_VAL + count, protoPoint.z);
					count++;
				}
				modelContent.addInt(ScatterPlot3DSettings.INTERNAL_NUM_PROTO_POINTS, count + 1);
			}

			File file = new File(internDir, ScatterPlot3DSettings.FILE_NAME);
			FileOutputStream fos = new FileOutputStream(file);
			modelContent.saveToXML(fos);
		}
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

	protected Coord3d[] getPrototypePoints() {
		return m_protoTypePoints;
	}
}
