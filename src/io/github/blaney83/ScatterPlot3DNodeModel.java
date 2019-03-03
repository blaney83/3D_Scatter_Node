package io.github.blaney83;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.node.io.filereader.InterruptedExecutionException;
import org.knime.base.node.preproc.double2int.WarningMessage;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
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

	ScatterPlot3DSettings m_settings = new ScatterPlot3DSettings();

	// save/load cfg keys
	static final String INTERNAL_MODEL_NAME_KEY = "internalModel";
	static final String INTERNAL_MODEL_NUM_FUNCTION_TERM_KEY = "numFnTerms";
	static final String INTERNAL_MODEL_NUM_CALC_POINT_KEY = "numCalcPoints";
	static final String INTERNAL_MODEL_TERM_KEY = "fnTerm";
	static final String INTERNAL_MODEL_POINT_KEY = "calcPoint";

	// INTERNAL NODE FIELDS
	// model inter-method variables
	private List<String> m_variableColNames;
	private List<String> m_dataTableColumnNames;
	private int m_inPort1VariableColumnIndex;
	private int m_inPort1CoeffColumnIndex;
	private int m_inPort1ExponentIndex = -1;
	private boolean m_isH2ONode = false;

	// view dependent fields
	protected Set<FunctionTerm> m_termSet;
	protected CalculatedPoint[] m_calcPoints;

	protected ScatterPlot3DNodeModel() {
		super(2, 1);
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {

		m_variableColNames = new ArrayList<String>();
		m_termSet = new LinkedHashSet<FunctionTerm>();

		BufferedDataTable coeffTable = inData[COEFFICIENT_IN_PORT];
		for (DataRow row : coeffTable) {
			m_termSet.add(validateCoeffVariables(row));
		}
		BufferedDataTable dataTable = inData[DATA_TABLE_IN_PORT];
		Set<String> correctRegressionColumnsSelected = new LinkedHashSet<String>();

		for (FunctionTerm fnTerm : m_termSet) {
			if (fnTerm.getVarName().equals(m_settings.getXAxisVarColumn())
					|| fnTerm.getVarName().equals(m_settings.getYAxisVarColumn())) {
				correctRegressionColumnsSelected.add(fnTerm.getVarName());
			}
			processColumn(dataTable, fnTerm);
		}
		if (correctRegressionColumnsSelected.size() != 2) {
			throw new InvalidSettingsException(
					"The columns you chose do not match with the columns modeled by the Regression Model. "
							+ "Please ensure the two selected columns were used in the creation of the Regression Equation.");
		}

		if (m_settings.getCount() > dataTable.size() || m_settings.getShowAllData()) {
			m_settings.setCount((int) dataTable.size());
		}

		m_calcPoints = new CalculatedPoint[m_settings.getCount()];
		int iterations = 0;
		for (DataRow dataRow : dataTable) {
			if (iterations >= m_settings.getCount()) {
				break;
			}
			m_calcPoints[iterations] = pointFactory(dataRow, dataTable.getDataTableSpec());
			iterations++;
		}

		BufferedDataTable bufferedOutput;
		// calculate values anyways, so as to color points on graph accordingly
		CellFactory cellFactory = new MVLRGraphCellFactory(createCalcValsOutputColumnSpec(),
				inData[DATA_TABLE_IN_PORT].getDataTableSpec(), m_termSet, m_calcPoints);
		ColumnRearranger outputTable = new ColumnRearranger(inData[DATA_TABLE_IN_PORT].getDataTableSpec());
		outputTable.append(cellFactory);
		bufferedOutput = exec.createColumnRearrangeTable(inData[DATA_TABLE_IN_PORT], outputTable, exec);
		if (!m_settings.getAppendColumn()) {
			bufferedOutput = exec.createBufferedDataTable(inData[DATA_TABLE_IN_PORT], exec);
		}
		return new BufferedDataTable[] { bufferedOutput };
	}

	private FunctionTerm validateCoeffVariables(final DataRow dataRow) throws InterruptedExecutionException {
		if (dataRow != null) {
			double coeff = Double.parseDouble(dataRow.getCell(m_inPort1CoeffColumnIndex).toString());
			int exponent = 1;
			if (m_inPort1ExponentIndex != -1) {
				exponent = Integer.parseInt(dataRow.getCell(m_inPort1ExponentIndex).toString());
			}
			if (!m_isH2ONode) {
				String varName = dataRow.getCell(m_inPort1VariableColumnIndex).toString();
				if (m_dataTableColumnNames.contains(varName) || varName.toLowerCase().trim().equals("intercept")) {
					m_variableColNames.add(varName);
					return new FunctionTerm(varName, coeff, exponent);
				}
			} else {
				String rowKeyValue = dataRow.getKey().getString();
				if (m_dataTableColumnNames.contains(rowKeyValue) || rowKeyValue.toLowerCase().trim() == "intercept") {
					m_variableColNames.add(rowKeyValue);
					return new FunctionTerm(rowKeyValue, coeff, exponent);
				}
			}
			throw new InterruptedExecutionException(
					"The coefficient table you provided does not match with the columns of the data table. Please ensure that the column names and variables fields have not been altered by a previous node AND your selected columns were used in the previous regression learner.");
		}
		return new FunctionTerm();
	}

	// may switch to iterating through columns 10 times for every 1000 rows, instead
	// of
	// 1000 rows for each of 10 columns
	private void processColumn(final BufferedDataTable input, final FunctionTerm fnTerm) {
		String colName = fnTerm.getVarName();
		int colIndex = input.getDataTableSpec().findColumnIndex(colName);
		if (colIndex > -1) {
			double meanSum = 0;
			double lowerBound = Double.MAX_VALUE;
			double upperBound = Double.MIN_VALUE;
			int totalRows = 0;
			for (DataRow row : input) {
				DataCell cell = row.getCell(colIndex);
				double value = ((DoubleValue) cell).getDoubleValue();
				meanSum += value;
				lowerBound = Math.min(lowerBound, value);
				upperBound = Math.max(upperBound, value);

				totalRows++;
			}
			DataColumnDomainCreator colDomainCreator = new DataColumnDomainCreator();
			colDomainCreator.setLowerBound(new DoubleCell(lowerBound));
			colDomainCreator.setUpperBound(new DoubleCell(upperBound));
			fnTerm.setDomain(colDomainCreator.createDomain());
			if (!colName.equals(m_settings.getColName()) && !colName.equals(m_settings.getXAxisVarColumn())
					&& !colName.equals(m_settings.getYAxisVarColumn())) {
				fnTerm.setValue(meanSum / totalRows);
			}
		} else {
			// handle intercept
			fnTerm.setValue(1);
			DataColumnDomainCreator colDomainCreator = new DataColumnDomainCreator();
			colDomainCreator.setLowerBound(new DoubleCell(fnTerm.getCoefficient()));
			colDomainCreator.setUpperBound(new DoubleCell(fnTerm.getCoefficient()));
			fnTerm.setDomain(colDomainCreator.createDomain());
		}
	}

	private CalculatedPoint pointFactory(final DataRow dataRow, final DataTableSpec tableSpec) {
		int targetIndex = tableSpec.findColumnIndex(m_settings.getColName());
		int xIndex = tableSpec.findColumnIndex(m_settings.getXAxisVarColumn());
		int yIndex = tableSpec.findColumnIndex(m_settings.getYAxisVarColumn());
		double xValue = ((DoubleValue) dataRow.getCell(xIndex)).getDoubleValue();
		double yValue = ((DoubleValue) dataRow.getCell(yIndex)).getDoubleValue();
		double outPutValue = ((DoubleValue) dataRow.getCell(targetIndex)).getDoubleValue();
		return new CalculatedPoint(xValue, yValue, outPutValue, dataRow.getKey());
	}

	@Override
	protected void reset() {
		if (m_termSet != null) {
			m_termSet = null;
		}
		if (m_calcPoints != null) {
			m_calcPoints = null;
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
						}
						break;
					case "Fuzzy C-Means":
						if (mainTableSpec.getColumnSpec(i).getName().equals("Winner Cluster")) {
							clusterColumnIdentified = true;
						}
					default:
						break;
					}
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
								+ "In-Port #2");
			}
			
			if (m_settings.getIsClustered() && m_settings.getClusterType().equals("K-Means") && m_settings.getPrototypesProvided() && inSpecs.length == 1) {
				throw new InvalidSettingsException("You have indicated that you are providing a prototype table at In-Port 2, but one is not present. "
						+ "Please provide the table generated by the K-Means node or change your preferences to reflect the absence of this table.");
			}

			if (m_settings.getIsClustered() && m_settings.getClusterType().equals("K-Means") && inSpecs.length > 1) {
				DataTableSpec prototypeTableSpec = inSpecs[ScatterPlot3DSettings.PROTOTYPE_TABLE_IN_PORT];
				if(!m_settings.getPrototypesProvided()) {
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
					switch (prototypeTableSpec.getColumnSpec(i).getName()) {
						case m_settings.getXAxisVarColumn():
							containsXVar = true;
							break;
						case m_settings.getYAxisVarColumn():
							containsYVar = true;
							break;
						case m_settings.getZAxisVarColumn():
							containsZVar = true;
							break;
						default:
								break;
					}
				}
				if(doubleCompatColCount < 3) {
					throw new InvalidSettingsException("The prototype table provided does not contain three numeric columns for plotting.");
				}
				if(!containsXVar ||
						!containsYVar ||
						!containsZVar) {
					throw new InvalidSettingsException("The prototype table provided does not match the columns you selected to be plotted on "
							+ "the graph. Please correct your column selections, your prototype settings, or change your settings to reflect the "
							+ "absence of a compatible prototype table.");
				}
			}
			return new DataTableSpec[] {inSpecs[ScatterPlot3DSettings.MAIN_DATA_TABLE_IN_PORT]};
		}
	}

	private DataColumnSpec createCalcValsOutputColumnSpec() {
		DataColumnSpecCreator newColSpecCreator = new DataColumnSpecCreator("Calculated " + m_settings.getColName(),
				DoubleCell.TYPE);
		DataColumnSpec newColSpec = newColSpecCreator.createSpec();
		return newColSpec;
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
				int numFnTerms = modelContent.getInt(INTERNAL_MODEL_NUM_FUNCTION_TERM_KEY);
				int numCalcPoints = modelContent.getInt(INTERNAL_MODEL_NUM_CALC_POINT_KEY);
				m_termSet = new LinkedHashSet<FunctionTerm>();
				m_calcPoints = new CalculatedPoint[numCalcPoints];
				for (int i = 0; i < numFnTerms; i++) {
					FunctionTerm newTerm = new FunctionTerm();
					ModelContentRO subContent = modelContent.getModelContent(INTERNAL_MODEL_TERM_KEY + i);
					newTerm.loadFrom(subContent);
					m_termSet.add(newTerm);
				}

				for (int i = 0; i < numCalcPoints; i++) {
					CalculatedPoint newPoint = new CalculatedPoint();
					ModelContentRO subContent = modelContent.getModelContent(INTERNAL_MODEL_POINT_KEY + i);
					newPoint.loadFrom(subContent);
					m_calcPoints[i] = newPoint;
				}

			} catch (InvalidSettingsException e) {
				throw new IOException("There was a problem loading the internal state of this node.");
			}

		}
	}

	@Override
	protected void saveInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		if (m_termSet != null && m_calcPoints != null) {
			ModelContent modelContent = new ModelContent(INTERNAL_MODEL_NAME_KEY);
			modelContent.addInt(INTERNAL_MODEL_NUM_FUNCTION_TERM_KEY, m_termSet.size());
			modelContent.addInt(INTERNAL_MODEL_NUM_CALC_POINT_KEY, m_calcPoints.length);
			int count = 0;
			for (FunctionTerm fnTerm : m_termSet) {
				ModelContentWO subContentWO = modelContent.addModelContent(INTERNAL_MODEL_TERM_KEY + count);
				fnTerm.saveTo(subContentWO);
				count++;
			}
			count = 0;
			for (CalculatedPoint calcPoint : m_calcPoints) {
				ModelContentWO subContentWO = modelContent.addModelContent(INTERNAL_MODEL_POINT_KEY + count);
				calcPoint.saveTo(subContentWO);
				count++;
			}
			File file = new File(internDir, FILE_NAME);
			FileOutputStream fos = new FileOutputStream(file);
			modelContent.saveToXML(fos);
		}
	}

	public ScatterPlot3DSettings getSettings() {
		return m_settings;
	}

}
