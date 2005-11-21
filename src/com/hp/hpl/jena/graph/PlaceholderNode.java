/*
 * Created on 14.04.2005 by Joerg Garbers, FU-Berlin
 *
 */
package com.hp.hpl.jena.graph;
// Note: this is not original Jena code!
// This class must be put in this package to be allowed to call the super constructor.

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeVisitor;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.shared.PrefixMapping;

/**
 * A PlaceholderNode gives information about a node, mich may not be fully instanciated.
 * In contrast to standard nodes a PlaceholderNode is mutable. 
 * It can be used to replace nodes within a larger structure.
 * By inheriting from Node, it can be used in all Node-typed places.
 * @author jgarbers
 *
 */
public class PlaceholderNode extends Node {

    /** if not null, we allready know the value of the variable, but still have to check for 
     * other occurrences. */
    protected Node node=null;
    /** What is the type, an URI, a blank node or a literal? */
    protected int nodeType=NotFixedNodeType;
    
    public static Node unwrapNode(Node n) {
        if (n instanceof PlaceholderNode)
            return ((PlaceholderNode)n).givenNode();
        return n;
    }

    public static final int NotFixedNodeType = 0;
    public static final int BlankNodeType = 1;
    public static final int UriNodeType = 2;
    public static final int LiteralNodeType = 4; 
    public static final int VariableNodeType = 8;

    static public int nodeType(Node n) {
        if (n.isLiteral()) 
            return LiteralNodeType;
        if (n.isBlank())
            return BlankNodeType;
        if (n.isURI())
            return UriNodeType;
        if (n.isVariable())
            return VariableNodeType;
        return NotFixedNodeType;
    }

    public Node givenNode() {
        if (node==null)
            throw new RuntimeException("Empty PlaceholderNode asked to reveal its node.");
        return node;
    }
    public int givenNodeType() {
        if (nodeType==NotFixedNodeType)
            throw new RuntimeException("Typeless PlaceholderNode asked to reveal its nodeType.");       
        return nodeType;
    }
     public void setNode(Node n) {
        node=n;
        if (n==null)
            nodeType=NotFixedNodeType;
        else 
            nodeType=nodeType(n);
    }

     ///////////////////////////////////// Node interface /////////////////////////
     
    public PlaceholderNode(Object label) {
        super(label);
    }
    
    public Object visitWith(NodeVisitor v) {
        return node.visitWith(v);
    }

    public boolean isConcrete() {
        return givenNodeType()!=VariableNodeType;
    }

    public boolean equals(Object o) {
        return node.equals(o);
    }

    /** is this a literal node - overridden in Node_Literal */
    public boolean isLiteral() 
        { return givenNodeType()==LiteralNodeType; }
    
    /** is this a blank node - overridden in Node_Blank */
    public boolean isBlank()
        { return givenNodeType()==BlankNodeType; }
    
    /** is this a URI node - overridden in Node_URI */
    public boolean isURI()
        { return givenNodeType()==UriNodeType; }
        
    /** is this a variable node - overridden in Node_Variable */
    public boolean isVariable()
        { return givenNodeType()==VariableNodeType; }

    /** get the blank node id if the node is blank, otherwise die horribly */    
    public AnonId getBlankNodeId() 
        { return node.getBlankNodeId(); }
    
    /** get the literal value of a literal node, otherwise die horribly */
    public LiteralLabel getLiteral()
    	{ return node.getLiteral(); }
    
    /** get the URI of this node if it has one, else die horribly */
    public String getURI()
	{ return node.getURI(); }
    
    /** get the namespace part of this node if it's a URI node, else die horribly */
    public String getNameSpace()
	{ return node.getNameSpace(); }
    
    /** get the localname part of this node if it's a URI node, else die horribly */
    public String getLocalName()
	{ return node.getLocalName(); }

    /** get a variable nodes name, otherwise die horribly */
    public String getName()
	{ return node.getName(); }
    
    /** answer true iff this node is a URI node with the given URI */
    public boolean hasURI( String uri )
    { return givenNodeType()==UriNodeType; }

    /**
     * Test that two nodes are semantically equivalent.
     * In some cases this may be the sames as equals, in others
     * equals is stricter. For example, two xsd:int literals with
     * the same value but different language tag are semantically
     * equivalent but distinguished by the java equality function
     * in order to support round tripping.
     * <p>Default implementation is to use equals, subclasses should
     * override this.</p>
     */
    public boolean sameValueAs(Object o) {
        return node.sameValueAs(o);
    }

    // same as super
    public int hashCode() {
    	return label.hashCode();
    }
    
    /**
        Answer true iff this node accepts the other one as a match.
        The default is an equality test; it is over-ridden in subclasses to
        provide the appropriate semantics for literals, ANY, and variables.
        
        @param other a node to test for matching
        @return true iff this node accepts the other as a match
    */
    public boolean matches( Node other )
    { 
        if (nodeType==VariableNodeType)
            return true;
        return node.matches( other ); 
    }

    /** 
        Answer a human-readable representation of this Node. It will not compress URIs, 
        nor quote literals (because at the moment too many places use toString() for 
        something machine-oriented).
    */   
    public String toString()
    	{ return node.toString(); }
    
    /**
         Answer a human-readable representation of this Node where literals are
         quoted according to <code>quoting</code> but URIs are not compressed.
    */
    public String toString( boolean quoting )
        { return node.toString( quoting ); }
    
    /**
        Answer a human-readable representation of the Node, quoting literals and
        compressing URIs.
    */
    public String toString( PrefixMapping pm )
        { return node.toString( pm ); }
        
    /**
        Answer a human readable representation of this Node, quoting literals if specified,
        and compressing URIs using the prefix mapping supplied.
    */
    public String toString( PrefixMapping pm, boolean quoting )
        { return node.toString(pm, quoting); }    
  
}
