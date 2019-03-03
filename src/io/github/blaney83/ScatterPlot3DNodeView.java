package io.github.blaney83;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "ScatterPlot3D" Node.
 * A 3 Dimensional representation of points in space for a given data set.
 *
 * @author Benjamin Laney
 */
public class ScatterPlot3DNodeView extends NodeView<ScatterPlot3DNodeModel> {
	
	private ScatterPlot3DNodeViewPanel m_borderLayout;

	protected ScatterPlot3DNodeView(final ScatterPlot3DNodeModel nodeModel) {
		super(nodeModel);
		m_borderLayout = new ScatterPlot3DNodeViewPanel(nodeModel);
		setComponent(m_borderLayout);
	}

	@Override
	protected void modelChanged() {
		ScatterPlot3DNodeModel updatedModel = getNodeModel();
		if(updatedModel != null) {
			m_borderLayout.updateView(updatedModel);
			setComponent(m_borderLayout);
		}

	}

	@Override
	protected void onClose() {
		//nothing
	}

	@Override
	protected void onOpen() {
		//nothing
	}

}

