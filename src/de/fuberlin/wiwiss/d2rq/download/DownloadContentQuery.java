package de.fuberlin.wiwiss.d2rq.download;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hsqldb.types.Types;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.map.DownloadMap;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.QueryExecutionIterator;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/**
 * A helper that evaluates a {@link DownloadMap} for a particular
 * URI, returning either the content, or <tt>null</tt> if the
 * URI isn't applicable for the download map or there is nothing
 * in the table for the value.
 * 
 * This directly runs its own SQL query because the handling of
 * BLOBs here requires returning an InputStream, and that's not
 * easily supported by {@link QueryExecutionIterator}.
 *   
 * @author RichardCyganiak
 */
public class DownloadContentQuery {
	private static final Log log = LogFactory.getLog(DownloadContentQuery.class);
	
	private final DownloadMap downloadMap;
	private final String uri;
	private Statement statement = null;
	private ResultSet resultSet = null;
	private InputStream resultStream = null;
	
	/**
	 * @param downloadMap The download map to be queried
	 * @param uri The URI whose content is desired
	 */
	public DownloadContentQuery(DownloadMap downloadMap, String uri) {
		this.downloadMap = downloadMap;
		this.uri = uri;
		execute();
	}
	
	public boolean hasContent() {
		return resultStream != null;
	}
	
	public InputStream getContentStream() {
		return resultStream;
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
		MutableRelation newRelation = new MutableRelation(downloadMap.getRelation());
		NodeMaker x = downloadMap.nodeMaker().selectNode(Node.createURI(uri), newRelation);
		// URI didn't fit the node maker
		if (x.equals(NodeMaker.EMPTY)) return;
		newRelation.project(Collections.singleton(downloadMap.getContentColumn()));
		newRelation.limit(1);
		Relation filteredRelation = newRelation.immutableSnapshot();
		String sql = new SelectStatementBuilder(filteredRelation).getSQLStatement();
    	ConnectedDB db = filteredRelation.database();
		Connection conn = db.connection();
		try {
			statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			log.debug(sql);
			resultSet = statement.executeQuery(sql);
			if (!resultSet.next()) {
				close();
				return;	// 0 results
			}
			int type = resultSet.getMetaData().getColumnType(1);
			if (type == Types.BINARY || type == Types.VARBINARY || type == Types.LONGVARBINARY || type == Types.BLOB) {
				resultStream = resultSet.getBinaryStream(1);
				if (resultSet.wasNull()) {
					resultStream = null;
				}
			} else {
				String s = resultSet.getString(1);
				if (!resultSet.wasNull()) {
					resultStream = new ByteArrayInputStream(s.getBytes());
				}
			}
		} catch (SQLException ex) {
			throw new D2RQException(ex);
		}
	}
}
