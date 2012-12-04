package org.d2rq.lang;

import org.d2rq.D2RQException;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.vocab.D2RQ;

import com.hp.hpl.jena.rdf.model.Resource;


/**
 * A d2rq:DownloadMap instance. This is a d2rq:ResourceMap
 * that must produce URIs, can refer to a d2rq:ClassMap to
 * provide further relation elements (joins, aliases, conditions),
 * and additionally has a d2rq:mediaType and d2rq:contentColumn.
 * 
 * Results can be retrieved via {@link #getContentDownloadColumn()},
 * {@link #getMediaTypeValueMaker()} (for the media type value make),
 * {@link #nodeMaker()} (for the URI spec),
 * and {@link #getRelation()}.
 * 
 * @author RichardCyganiak
 */
public class DownloadMap extends ResourceMap {
	private ClassMap belongsToClassMap = null;
	private Database database = null;
	private String mediaType = null;
	private ColumnName contentDownloadColumn = null;
	
	public DownloadMap(Resource downloadMapResource) {
		super(downloadMapResource, false);
	}
	
	public void setBelongsToClassMap(ClassMap classMap) {
		assertNotYetDefined(belongsToClassMap, D2RQ.belongsToClassMap, 
				D2RQException.DOWNLOADMAP_DUPLICATE_BELONGSTOCLASSMAP);
		assertArgumentNotNull(classMap, D2RQ.belongsToClassMap, 
				D2RQException.DOWNLOADMAP_INVALID_BELONGSTOCLASSMAP);
		belongsToClassMap = classMap;
	}
	
	public ClassMap getBelongsToClassMap() {
		return belongsToClassMap;
	}
	
	public void setDatabase(Database database) {
		assertNotYetDefined(this.database, D2RQ.dataStorage, 
				D2RQException.DOWNLOADMAP_DUPLICATE_DATABASE);
		assertArgumentNotNull(database, D2RQ.dataStorage, 
				D2RQException.DOWNLOADMAP_INVALID_DATABASE);
		this.database = database;
	}
	
	public Database getDatabase() {
		return database;
	}
	
	public void setMediaType(String mediaType) {
		assertNotYetDefined(this.mediaType, D2RQ.mediaType, 
				D2RQException.DOWNLOADMAP_DUPLICATE_MEDIATYPE);
		this.mediaType = mediaType;
	}
	
	public String getMediaType() {
		return mediaType;
	}
	
	public void setContentDownloadColumn(String contentColumn) {
		assertNotYetDefined(this.contentDownloadColumn, D2RQ.contentDownloadColumn, 
				D2RQException.DOWNLOADMAP_DUPLICATE_CONTENTCOLUMN);
		this.contentDownloadColumn = Microsyntax.parseColumn(contentColumn);
	}

	public ColumnName getContentDownloadColumn() {
		return contentDownloadColumn;
	}
	
	public void accept(D2RQMappingVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Returns the d2rq:dataStorage of this d2rq:DownloadMap,
	 * or if none is defined, then check if there's a d2rq:belongsToClassMap
	 * that has one
	 */
	public Database getDatabaseFromHereOrClassMap() {
		if (database != null) return database;
		if (belongsToClassMap == null) return null;
		return belongsToClassMap.getDatabase();
	}
}
