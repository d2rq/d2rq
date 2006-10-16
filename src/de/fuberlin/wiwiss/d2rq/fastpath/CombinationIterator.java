package de.fuberlin.wiwiss.d2rq.fastpath;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterates over combinations of elements from several
 * arrays. The input is an array of arrays. Each iteration
 * step will return an array that contains exactly one
 * item from each of the input arrays. The iterator will
 * step through all possible combinations.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: CombinationIterator.java,v 1.1 2006/10/16 12:46:00 cyganiak Exp $
 */
public class CombinationIterator implements Iterator {
	private final Object[][] elements;
	private final int n;
	private final int[] positions;
	private boolean finished = false;
	
	public CombinationIterator(Object[][] elements) {
		this.elements = elements;
		this.n = elements.length;
		this.positions = new int[this.n];
		for (int i = 0; i < this.n; i++) {
			if (this.elements[i].length == 0) {
				this.finished = true;
			}
		}
		if (this.n == 0) {
			this.finished = true;
		}
	}
	
	public boolean hasNext() {
		return !this.finished;
	}

	public Object next() {
		if (this.finished) {
			throw new NoSuchElementException();
		}
		Object[] result = new Object[this.n];
		for (int i = 0; i < this.n; i++) {
			result[i] = this.elements[i][this.positions[i]];
		}
		this.positions[0]++;
		int i = 0;
		while (this.positions[i] >= this.elements[i].length) {
			this.positions[i] = 0;
			i++;
			if (i >= this.n) {
				this.finished = true;
				break;
			}
			this.positions[i]++;
		}
		return result;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
