package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.Node;

public class NodeConstraint {
    public boolean possible=true;
    boolean infoAdded = false;

    public Node fixedNode;
    public int nodeType=NotFixedNodeType;
    public LiteralMaker literalMaker; // used by LiteralMaker only

    public static final int NotFixedNodeType = 0;
    public static final int BlankNodeType = 1;
    public static final int UriNodeType = 2;
    public static final int LiteralNodeType = 3;    
    
    public void matchLiteralMaker(LiteralMaker m) {
        if (!possible)
            return;
        if (literalMaker == null) {
            literalMaker=m;
        } else if (possible){
            possible=m.matchesOtherLiteralMaker(literalMaker);
        }
    }

    public void matchFixedNode(Node node) {
        if (!possible)
            return;
       if (fixedNode == null) {
            fixedNode = node;
            infoAdded = true;
            if (node.isLiteral())
                matchNodeType(LiteralNodeType);
            return;
        }
        possible = fixedNode.equals(node);
    }
 
    public void matchNodeType(int t) {
        if (!possible)
            return;
       if (nodeType == NotFixedNodeType) {
            nodeType = t;
            infoAdded = true;
            return;
        }
        possible = (nodeType == t);
    }
    
    public void matchValueSource(ValueSource s) {
        if (!possible)
            return;
       
    }
}