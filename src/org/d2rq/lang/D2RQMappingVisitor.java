package org.d2rq.lang;

public interface D2RQMappingVisitor {
	boolean visitEnter(Mapping mapping);
	void visitLeave(Mapping mapping);
	void visit(Configuration configuration);
	void visit(Database database);
	void visit(TranslationTable translationTable);
	boolean visitEnter(ClassMap classMap);
	void visitLeave(ClassMap classMap);
	void visit(PropertyBridge propertyBridge);
	void visit(DownloadMap downloadMap);
	
	public static class Default implements D2RQMappingVisitor {
		public boolean visitEnter(Mapping mapping) { return true; }
		public void visitLeave(Mapping mapping) {}
		public void visit(Configuration configuration) {}
		public void visit(Database database) {}
		public void visit(TranslationTable translationTable) {}
		public boolean visitEnter(ClassMap classMap) { return true; }
		public void visitLeave(ClassMap classMap) {}
		public void visit(PropertyBridge propertyBridge) {}
		public void visit(DownloadMap downloadMap) {}
	}
}
