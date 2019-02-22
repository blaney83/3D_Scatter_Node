package io.github.blaney83;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "ScatterPlot3D" Node.
 * A 3 Dimensional representation of points in space for a given data set.
 *
 * @author Benjamin Laney
 */
public class ScatterPlot3DNodeFactory 
        extends NodeFactory<ScatterPlot3DNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ScatterPlot3DNodeModel createNodeModel() {
        return new ScatterPlot3DNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<ScatterPlot3DNodeModel> createNodeView(final int viewIndex,
            final ScatterPlot3DNodeModel nodeModel) {
        return new ScatterPlot3DNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new ScatterPlot3DNodeDialog();
    }

}

