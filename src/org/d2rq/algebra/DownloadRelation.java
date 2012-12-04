package org.d2rq.algebra;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.nodes.NodeMaker;
import org.d2rq.nodes.TypedNodeMaker;
import org.d2rq.values.ColumnValueMaker;
import org.d2rq.values.ValueMaker;

import com.hp.hpl.jena.sparql.core.Var;


public class DownloadRelation extends NodeRelation {
	
	public final static Var RESOURCE = Var.alloc("resource");
	public final static Var MEDIA_TYPE = Var.alloc("mediaType");
	public final static Var CONTENT = Var.alloc("content");

	public final static Set<Var> HEADER = new HashSet<Var>(Arrays.asList(
			new Var[]{RESOURCE, MEDIA_TYPE, CONTENT}));
	
	private final ColumnName contentDownloadColumn;
	
	public DownloadRelation(
			SQLConnection sqlConnection, 
			DatabaseOp tabular, 
			final NodeMaker nodeMaker, 
			final ValueMaker mediaTypeMaker, 
			final ColumnName contentDownloadColumn) {
		super(sqlConnection, tabular, new HashMap<Var,NodeMaker>() {{
			put(RESOURCE, nodeMaker);
			put(MEDIA_TYPE, new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, mediaTypeMaker));
			put(CONTENT, new TypedNodeMaker(TypedNodeMaker.PLAIN_LITERAL, new ColumnValueMaker(contentDownloadColumn)));
		}});
		this.contentDownloadColumn = contentDownloadColumn;
	}
	
	public ColumnName getContentDownloadColumn() {
		return contentDownloadColumn;
	}
}