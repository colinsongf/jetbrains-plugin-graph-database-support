package com.neueda.jetbrains.plugin.graphdb.visualization;

import com.neueda.jetbrains.plugin.graphdb.database.api.GraphNode;
import com.neueda.jetbrains.plugin.graphdb.database.api.GraphRelationship;
import com.neueda.jetbrains.plugin.graphdb.visualization.decorators.CenteredLayout;
import com.neueda.jetbrains.plugin.graphdb.visualization.events.EventType;
import com.neueda.jetbrains.plugin.graphdb.visualization.events.NodeCallback;
import com.neueda.jetbrains.plugin.graphdb.visualization.events.RelationshipCallback;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.ControlAdapter;
import prefuse.controls.DragControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.render.ShapeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.PrefuseLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import static prefuse.Constants.EDGE_TYPE_LINE;
import static prefuse.Constants.SHAPE_ELLIPSE;

public class GraphDisplay extends Display {

    private static final String GRAPH = "graph";
    private static final String NODES = "graph.nodes";
    private static final String EDGES = "graph.edges";
    private static final boolean DIRECTED = true;
    private static final int NODE_DIAMETER = 25;
    private static final String NODE_LABEL = "nodelabel";
    private static final String LAYOUT = "layout";
    private static final String FONT = "Tahoma";
    private static final int FONT_SIZE = 12;
    private static final int FONT_COLOR = ColorLib.rgb(15, 15, 45);

    private Graph graph;

    private Map<String, Node> nodeMap = new HashMap<>();
    private Map<String, GraphNode> graphNodeMap = new HashMap<>();
    private Map<String, GraphRelationship> graphRelationshipMap = new HashMap<>();

    public GraphDisplay() {
        super(new Visualization());
        graph = new Graph(DIRECTED);
        graph.addColumn("id", String.class);

        m_vis.addGraph(GRAPH, graph);
        m_vis.setInteractive(EDGES, null, false);
        m_vis.setValue(NODES, null, VisualItem.SHAPE, SHAPE_ELLIPSE);

        setupRenderer();
        createLayout();
        setHighQuality(true);
        addControlListener(new DragControl());
    }

    public void clearGraph() {
        graph.clear();
    }

    public void addNodeListener(EventType type, NodeCallback callback) {
        ControlAdapter listener = new ControlAdapter() {
            @Override
            public void itemEntered(VisualItem item, MouseEvent e) {
                if (type == EventType.HOVER_START && item instanceof NodeItem) {
                    callback.accept(graphNodeMap.get(item.get("id")), item, e);
                }
            }

            @Override
            public void itemExited(VisualItem item, MouseEvent e) {
                if (type == EventType.HOVER_END && item instanceof NodeItem) {
                    callback.accept(graphNodeMap.get(item.get("id")), item, e);
                }
            }

            @Override
            public void itemClicked(VisualItem item, MouseEvent e) {
                if (type == EventType.CLICK && item instanceof NodeItem) {
                    callback.accept(graphNodeMap.get(item.get("id")), item, e);
                }
            }
        };
        addControlListener(listener);
    }

    public void addEdgeListener(EventType type, RelationshipCallback action) {
        ControlAdapter listener = new ControlAdapter() {
            @Override
            public void itemEntered(VisualItem item, MouseEvent e) {
                if (type == EventType.HOVER_START && item instanceof EdgeItem) {
                    action.accept(graphRelationshipMap.get(item.get("id")), item, e);
                }
            }

            @Override
            public void itemExited(VisualItem item, MouseEvent e) {
                if (type == EventType.HOVER_END && item instanceof EdgeItem) {
                    action.accept(graphRelationshipMap.get(item.get("id")), item, e);
                }
            }

            @Override
            public void itemClicked(VisualItem item, MouseEvent e) {
                if (type == EventType.CLICK && item instanceof EdgeItem) {
                    action.accept(graphRelationshipMap.get(item.get("id")), item, e);
                }
            }
        };
        addControlListener(listener);
    }

    public void addNode(GraphNode graphNode) {
        Node node = graph.addNode();
        node.set("id", graphNode.getId());
        nodeMap.put(graphNode.getId(), node);
        graphNodeMap.put(graphNode.getId(), graphNode);
    }

    public void addRelationship(GraphRelationship graphRelationship) {
        String start = graphRelationship.getStart().getId();
        String end = graphRelationship.getEnd().getId();

        Edge edge = graph.addEdge(nodeMap.get(start), nodeMap.get(end));
        edge.set("id", graphRelationship.getId());
        graphRelationshipMap.put(graphRelationship.getId(), graphRelationship);
    }

    private void setupRenderer() {
        ShapeRenderer nodeRenderer = new ShapeRenderer();

        nodeRenderer.setBaseSize(NODE_DIAMETER);
        DefaultRendererFactory rf = new DefaultRendererFactory(nodeRenderer, new EdgeRenderer(EDGE_TYPE_LINE));
        rf.add(new InGroupPredicate(NODE_LABEL), new LabelRenderer("id"));

        final Schema decoratorSchema = PrefuseLib.getVisualItemSchema();
        decoratorSchema.setDefault(VisualItem.INTERACTIVE, false);
        decoratorSchema.setDefault(VisualItem.TEXTCOLOR, FONT_COLOR);
        decoratorSchema.setDefault(VisualItem.FONT, FontLib.getFont(FONT, FONT_SIZE));
        m_vis.addDecorators(NODE_LABEL, NODES, decoratorSchema);

        m_vis.setRendererFactory(rf);
    }

    private void createLayout() {
        ActionList layout = new ActionList(Activity.INFINITY);
        layout.add(getColors());
        layout.add(new ForceDirectedLayout(GRAPH, true));
        layout.add(new RepaintAction());
        layout.add(new CenteredLayout(NODE_LABEL));

        m_vis.putAction(LAYOUT, layout);
    }

    private ActionList getColors() {
        ActionList colors = new ActionList();

        ColorAction nStroke = new ColorAction(NODES, VisualItem.STROKECOLOR);
        nStroke.setDefaultColor(ColorLib.gray(100));
        nStroke.add("_hover", ColorLib.gray(50));
        colors.add(nStroke);

        ColorAction nFill = new ColorAction(NODES, VisualItem.FILLCOLOR);
        nFill.setDefaultColor(ColorLib.gray(255));
        nFill.add("_hover", ColorLib.gray(200));
        colors.add(nFill);

        ColorAction nEdges = new ColorAction(EDGES, VisualItem.STROKECOLOR);
        nEdges.setDefaultColor(ColorLib.gray(100));
        colors.add(nEdges);

        ColorAction arrow = new ColorAction(EDGES, VisualItem.FILLCOLOR);
        nEdges.setDefaultColor(ColorLib.gray(100));
        colors.add(arrow);

        return colors;
    }

    public void startLayout() {
        m_vis.run(LAYOUT);
    }

    public void stopLayout() {
        m_vis.cancel(LAYOUT);
    }
}