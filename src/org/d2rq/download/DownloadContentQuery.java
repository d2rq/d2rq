package org.d2rq.download;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.d2rq.D2RQException;
import org.d2rq.algebra.DownloadRelation;
import org.d2rq.algebra.NodeRelation;
import org.d2rq.algebra.NodeRelationUtil;
import org.d2rq.db.ResultRow;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.SelectStatementBuilder;
import org.d2rq.db.op.util.OpUtil;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.engine.binding.Binding;


/**
 * A helper that evaluates a {@link DownloadRelation} for a particular
 * URI, returning either the content, or <tt>null</tt> if the
 * URI isn't applicable for the download map or there is nothing
 * in the table for the value.
 * 
 * This directly runs its own SQL query because the handling of
 * BLOBs here requires returning an InputStream, and that's not
 * easily supported by our usual SQL query class.
 *   
 * @author RichardCyganiak
 */
public class DownloadContentQuery {
	private static final Log log = LogFactory.getLog(DownloadContentQuery.class);
	
	private final DownloadRelation downloadRelation;
	private final String uri;
	private Statement statement = null;
	private ResultSet resultSet = null;
	private InputStream resultStream = null;
	private String mediaType = null;
	
	/**
	 * @param downloadRelation The download map to be queried
	 * @param uri The URI whose content is desired
	 */
	public DownloadContentQuery(DownloadRelation downloadRelation, String uri) {
		this.downloadRelation = downloadRelation;
		this.uri = uri;
		execute();
	}
	
	public boolean hasContent() {
		return resultStream != null;
	}
	
	/**
	 * A stream over the downloadable content. Undefined if
	 * {@link #hasContent()} is <code>false</code>.
	 */
	public InputStream getContentStream() {
		return resultStream;
	}
	
	/**
	 * The media type of the downloadable content. Undefined if
	 * {@link #hasContent()} is <code>false</code>.
	 */
	public String getMediaType() {
		return mediaType;
	}
	
	public void close() {
		try {
			if (this.statement != null) {
				this.statement.close();
				this.statement = null;
			}
			if (this.resultSet != null) {
				this.resultSet.close();
				this.resultSet = null;
			}
		} catch (SQLException ex) {
			throw new D2RQException(ex);
		}
	}
	
	private void execute() {
		NodeRelation r = downloadRelation;
		r = NodeRelationUtil.select(r, DownloadRelation.RESOURCE, Node.createURI(uri));
		// URI didn't fit the node maker
		if (OpUtil.isEmpty(r.getBaseTabular())) return;
		r = NodeRelationUtil.project(r, DownloadRelation.HEADER);
		r = NodeRelationUtil.limit(r, 1);
		SQLConnection db = r.getSQLConnection();
		SelectStatementBuilder builder = new SelectStatementBuilder(r.getBaseTabular(), db.vendor());
		String sql = builder.getSQL();
		int contentColumn = builder.getColumns().indexOf(downloadRelation.getContentDownloadColumn()) + 1;
		Connection conn = db.connection();
		try {
			statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			log.debug(sql);
			resultSet = statement.executeQuery(sql);
			if (!resultSet.next()) {
				close();
				return;	// 0 results
			}
			int type = resultSet.getMetaData().getColumnType(contentColumn);
			// TODO Handle Oracle BFILE type; there's some code for that already in ResultRowMap
			if (type == Types.BINARY || type == Types.VARBINARY || type == Types.LONGVARBINARY || type == Types.BLOB) {
				resultStream = resultSet.getBinaryStream(contentColumn);
				if (resultSet.wasNull()) {
					resultStream = null;
				}
			} else {
				String s = resultSet.getString(contentColumn);
				if (!resultSet.wasNull()) {
					resultStream = new ByteArrayInputStream(s.getBytes());
				}
			}
			Binding row = r.getBindingMaker().makeBinding(
					ResultRow.fromResultSet(resultSet, builder.getColumns(), db));
			if (row != null) {
				mediaType = row.get(DownloadRelation.MEDIA_TYPE).getLiteralLexicalForm();
			}
		} catch (SQLException ex) {
			throw new D2RQException(ex);
		}
	}
}
