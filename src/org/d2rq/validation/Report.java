package org.d2rq.validation;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.d2rq.D2RQException;
import org.d2rq.r2rml.MappingTerm;
import org.d2rq.validation.Message.Level;
import org.d2rq.validation.Message.Problem;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;


public class Report {
	private final List<Message> messages = new ArrayList<Message>();
	private boolean ignoreInfo = false;
	private boolean throwExceptionOnError = false;
	
	public void setIgnoreInfo(boolean ignoreInfo) {
		this.ignoreInfo = ignoreInfo;
	}
	
	public void setThrowExceptionOnError(boolean flag) {
		this.throwExceptionOnError = flag;
	}
	
	public String toString() {
		return messages.toString();
	}
	
	public boolean hasError() {
		return countErrors() > 0;
	}
	
	public int countWarnings() {
		int result = 0;
		for (Message message: messages) {
			if (message.getLevel() == Message.Level.Warning) result++;
		}
		return result;
	}

	public int countErrors() {
		int result = 0;
		for (Message message: messages) {
			if (message.getLevel() == Message.Level.Error) result++;
		}
		return result;
	}
	public List<Message> getMessages() {
		return messages;
	}
	
	public void report(Problem problem) {
		report(new Message(problem));
	}

	public void report(Problem problem, String details) {
		report(new Message(problem, details));
	}
	
	public void report(Problem problem, Resource subject, Property property, MappingTerm term) {
		report(new Message(problem, term, null, null, subject, property));
	}
	
	public void report(Problem problem, Resource subject, Property property, MappingTerm term, String detailCode, String details) {
		report(new Message(problem, term, detailCode, details, subject, property));
	}
	
	public void report(Problem problem, Resource subject) {
		report(new Message(problem, subject, null, null));
	}
	
	public void report(Problem problem, Resource subject, Property predicate) {
		report(new Message(problem, subject, 
				predicate == null ? null : new Property[]{predicate}, null));
	}
	
	public void report(Problem problem, Resource subject, Property[] predicates) {
		report(new Message(problem, subject, predicates, null));
	}
	
	public void report(Problem problem, Resource subject, Property predicate, RDFNode object) {
		report(new Message(problem, subject, 
				predicate == null ? null : new Property[]{predicate}, new RDFNode[]{object}));
	}
	
	public void report(Problem problem, Resource subject, Property predicate, RDFNode[] objects) {
		report(new Message(problem, subject, 
				predicate == null ? null : new Property[]{predicate}, objects));
	}
	
	public void report(Message message) {
		if (ignoreInfo && message.getLevel() == Level.Info) return;
		if (throwExceptionOnError && message.getLevel() == Level.Error) {
			StringWriter out = new StringWriter();
			new PlainTextMessageRenderer(out).render(message);
			throw new D2RQException(out.toString(), D2RQException.VALIDATION_EXCEPTION);
		}
		messages.add(message);
	}
}
