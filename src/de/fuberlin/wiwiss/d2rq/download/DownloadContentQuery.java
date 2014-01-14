package de.fuberlin.wiwiss.d2rq.download;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.graph.Node;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.algebra.MutableRelation;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.algebra.Relation;
import de.fuberlin.wiwiss.d2rq.map.DownloadMap;
import de.fuberlin.wiwiss.d2rq.nodes.NodeMaker;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;
import de.fuberlin.wiwiss.d2rq.sql.ResultRowMap;
import de.fuberlin.wiwiss.d2rq.sql.SQLIterator;
import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;
import de.fuberlin.wiwiss.d2rq.values.ValueMaker;

/**
 * A helper that evaluates a {@link DownloadMap} for a particular
 * URI, returning either the content, or <tt>null</tt> if the
 * URI isn't applicable for the download map or there is nothing
 * in the table for the value.
 * 
 * This directly runs its own SQL query because the handling of
 * BLOBs here requires returning an InputStream, and that's not
 * easily supported by {@link SQLIterator}.
 *   
 * @author RichardCyganiak
 */
public class DownloadContentQuery {
	private static final Log log = LogFactory.getLog(DownloadContentQuery.class);
	
	private final DownloadMap downloadMap;
	private final ValueMaker mediaTypeValueMaker;
	private final String uri;
	private Statement statement = null;
	private ResultSet resultSet = null;
	private InputStream resultStream = null;
	private String mediaType = null;
	private ConnectedDB db = null;
	
	/**
	 * @param downloadMap The download map to be queried
	 * @param uri The URI whose content is desired
	 */
	public DownloadContentQuery(DownloadMap downloadMap, String uri) {
		this.downloadMap = downloadMap;
		this.mediaTypeValueMaker = downloadMap.getMediaTypeValueMaker();
		this.uri = uri;
		execute();
	}
	
	public boolean hasContent() {
		return resultStream != null;
	}
	
	public InputStream getContentStream() {
		return resultStream;
	}
	
	public String getMediaType() {
		return mediaType;
	}
	
	public void close() {
		try {
			if (this.db != null) {
				this.db.vendor().beforeClose(db.connection());
			}
			if (this.statement != null) {
				this.statement.close();
				this.statement = null;
			}
			if (this.db != null) {
				this.db.vendor().afterClose(db.connection());
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
		Set<ProjectionSpec> requiredProjections = new HashSet<ProjectionSpec>();
		requiredProjections.add(downloadMap.getContentDownloadColumn());
		requiredProjections.addAll(mediaTypeValueMaker.projectionSpecs());
		newRelation.project(requiredProjections);
		newRelation.limit(1);
		Relation filteredRelation = newRelation.immutableSnapshot();
		SelectStatementBuilder builder = new SelectStatementBuilder(filteredRelation);
		String sql = builder.getSQLStatement();
		int contentColumn = builder.getColumnSpecs().indexOf(downloadMap.getContentDownloadColumn()) + 1;
    	db = filteredRelation.database();
		Connection conn = db.connection();
		try {
			statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			log.debug(sql);

			db.vendor().beforeQuery(conn);
			resultSet = statement.executeQuery(sql);
			db.vendor().afterQuery(conn);
			
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
			mediaType = mediaTypeValueMaker.makeValue(
					ResultRowMap.fromResultSet(resultSet, builder.getColumnSpecs(), db));
		} catch (SQLException ex) {
			throw new D2RQException(ex);
		}
	}
}
