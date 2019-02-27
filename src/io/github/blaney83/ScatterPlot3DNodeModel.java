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
 * This is the model implementation of ScatterPlot3D.
 * A 3 Dimensional representation of points in space for a given data set.
 *
 * @author Benjamin Laney
 */
public class ScatterPlot3DNodeModel extends NodeModel {
    
	ScatterPlot3DSettings m_settings = new ScatterPlot3DSettings();

	// inport fields
	public static final int MAIN_DATA_TABLE_IN_PORT = 0;

	// Model Content File
	private static final String FILE_NAME = "ScatterPlot.xml";

	// save/load cfg keys
	static final String INTERNAL_MODEL_NAME_KEY = "internalModel";
	static final String INTERNAL_MODEL_NUM_FUNCTION_TERM_KEY = "numFnTerms";
	static final String INTERNAL_MODEL_NUM_CALC_POINT_KEY = "numCalcPoints";
	static final String INTERNAL_MODEL_TERM_KEY = "fnTerm";
	static final String INTERNAL_MODEL_POINT_KEY = "calcPoint";

	// view options
	// color rainbow- under, on, over
	// opacity of planar field
	// point size

	// default settings fields
	static final int DEFAULT_COUNT = 100;
	static final boolean DEFAULT_APPEND_CALCULATED_TARGET = false;
	static final boolean DEFAULT_IS_H2O_NODE = false;
	static final boolean DEFAULT_SHOW_ALL_DATA = false;
	static final boolean DEFAULT_SHOW_REG_MODEL = true;

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
		// two in-ports, one out-port
		super(1, 1);
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
			if(fnTerm.getVarName().equals(m_settings.getXAxisVarColumn()) || fnTerm.getVarName().equals(m_settings.getYAxisVarColumn())) {
				correctRegressionColumnsSelected.add(fnTerm.getVarName());
			}
			processColumn(dataTable, fnTerm);
		}
		if(correctRegressionColumnsSelected.size() != 2) {
			throw new InvalidSettingsException("The columns you chose do not match with the columns modeled by the Regression Model. "
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
		//calculate values anyways, so as to color points on graph accordingly
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

		
		if (inSpecs[COEFFICIENT_IN_PORT] == null || inSpecs[DATA_TABLE_IN_PORT] == null) {
			throw new InvalidSettingsException(
					"Please provide a regression coefficient table to In-Port #1 and a data table to In-Port #2.");
		} else {
			if (!inSpecs[COEFFICIENT_IN_PORT].containsCompatibleType(StringValue.class)
					&& !inSpecs[COEFFICIENT_IN_PORT].containsCompatibleType(DoubleValue.class)) {
				throw new InvalidSettingsException(
						"The coefficient table provided does not meet the requirements for this node. It should contain at least a column containing the row keys (variables of the regression equation) in the associated data column and a numeric coefficients column.");
			}
			
			if(m_settings.getColName() == null) {
				throw new InvalidSettingsException("No target column selected");
			}
			// checking input ind. 0
			boolean hasCoefficientColumn = false;
			boolean hasVariableColumn = false;
			String[] coeffTableColumns = inSpecs[COEFFICIENT_IN_PORT].getColumnNames();
			// to be taken out if node expanded for polynomial regression
			if (coeffTableColumns != null && coeffTableColumns.length > 1) {
				for (String colName : coeffTableColumns) {
					if (colName.toLowerCase().contains("exponent")) {
						m_inPort1ExponentIndex = inSpecs[COEFFICIENT_IN_PORT].findColumnIndex(colName);
					}
					// alternately check for "coeff."
					if (colName.toLowerCase().trim().equals("coeff.") && inSpecs[COEFFICIENT_IN_PORT]
							.getColumnSpec(colName).getType().isCompatible(DoubleValue.class)) {
						m_inPort1CoeffColumnIndex = inSpecs[COEFFICIENT_IN_PORT].findColumnIndex(colName);
						hasCoefficientColumn = true;
					}
					if (colName.toLowerCase().trim().contains("variable") && inSpecs[COEFFICIENT_IN_PORT]
							.getColumnSpec(colName).getType().isCompatible(StringValue.class)) {
						m_inPort1VariableColumnIndex = inSpecs[COEFFICIENT_IN_PORT].findColumnIndex(colName);
						hasVariableColumn = true;
					}
				}
			} else if (coeffTableColumns != null && // checking for H20 regression output
					coeffTableColumns.length == 1 && coeffTableColumns[0].toLowerCase().trim().equals("beta")
					&& inSpecs[COEFFICIENT_IN_PORT].getColumnSpec(coeffTableColumns[0]).getType()
							.isCompatible(DoubleValue.class)) {
				
				m_inPort1CoeffColumnIndex = inSpecs[COEFFICIENT_IN_PORT].findColumnIndex(coeffTableColumns[0]);
				m_isH2ONode = true;
				hasCoefficientColumn = true;
				hasVariableColumn = true;
			}
			if (!hasCoefficientColumn) {
				throw new InvalidSettingsException(
						"Cannot find coefficient's from the provided table. Please make sure the coefficient table is connected to the top In-Port (0) and that the column containing coefficients is correctly named 'Coeff.'");
			}
			if (!hasVariableColumn) {
				throw new InvalidSettingsException(
						"Cannot find coefficient variables from the provided table. Please make sure the coefficient table is connected to the top In-Port (0) and that the column containing the associated column names from the data table (bottom In-Port (1)) is correctly named 'Variable'");
			}
			// checking input ind 1
			boolean containsTargetColumn = false;
			boolean containsXCol = false;
			boolean containsYCol = false;
			int numDataColumns = inSpecs[DATA_TABLE_IN_PORT].getNumColumns();
			int numNumericColumns = 0;

			m_dataTableColumnNames = new ArrayList<String>();

			if (numDataColumns > 0) {
				for (int i = 0; i < numDataColumns; i++) {
					DataColumnSpec columnSpec = inSpecs[DATA_TABLE_IN_PORT].getColumnSpec(i);
					String colName = columnSpec.getName();
					m_dataTableColumnNames.add(colName);
					if (columnSpec.getType().isCompatible(DoubleValue.class)) {
						numNumericColumns++;
						if (m_settings.getColName() != null && colName.contentEquals(m_settings.getColName())) {
							containsTargetColumn = true;
						}
						if (m_settings.getXAxisVarColumn() != null && colName.contentEquals(m_settings.getXAxisVarColumn())) {
							containsXCol = true;
						}
						if (m_settings.getYAxisVarColumn() != null && colName.contentEquals(m_settings.getYAxisVarColumn())) {
							containsYCol = true;
						}
					}
				}
			}
			if (numNumericColumns < 3) {
				throw new InvalidSettingsException(
						"The data table provided must contain 2 or more numeric columns which correspond to the variables in the regression equation and 1 numeric target column (same as was selected during regression learning).");
			}

			if (!containsTargetColumn) {
				throw new InvalidSettingsException(
						"You must select the column targeted in the Regression Learning Node for the target column.");
			}
			if (!containsXCol || !containsYCol) {
				throw new InvalidSettingsException("You must select a numeric column for both axis.");
			}
			if (m_settings.getAppendColumn()) {
				DataColumnSpec calcValsColumnSpec = createCalcValsOutputColumnSpec();
				DataTableSpec appendSpec = new DataTableSpec(calcValsColumnSpec);
				DataTableSpec outputSpec = new DataTableSpec(inSpecs[DATA_TABLE_IN_PORT], appendSpec);
				return new DataTableSpec[] { outputSpec };
			} else {
				return new DataTableSpec[] { inSpecs[DATA_TABLE_IN_PORT] };
			}
		}
	}

	private DataColumnSpec createCalcValsOutputColumnSpec() {
		DataColumnSpecCreator newColSpecCreator = new DataColumnSpecCreator("Calculated " + m_settings.getColName(), DoubleCell.TYPE);
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
		MVLRGraphSettings s = new MVLRGraphSettings();
		
		s.loadSettingsFrom(settings);
		
		if(s.getColName() == null) {
			throw new InvalidSettingsException("No target column selected");
		}
	}

	@Override
	protected void loadInternals(final File internDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		File file = new File(internDir, FILE_NAME);
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
	
	public MVLRGraphSettings getSettings() {
		return m_settings;
	}

}

