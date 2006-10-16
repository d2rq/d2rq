package de.fuberlin.wiwiss.d2rq.fastpath;

import java.util.Arrays;
import java.util.Iterator;

import junit.framework.TestCase;

public class CombinationIteratorTest extends TestCase {

	public void testZeroArraysHasNoElements() {
		assertFalse(new CombinationIterator(new String[][]{}).hasNext());
	}
	
	public void testOneZeroLengthArrayHasNoElements() {
		assertFalse(new CombinationIterator(
				new String[][]{new String[]{}}).hasNext());
	}
	
	public void testNoElementsIfOneArrayIsEmpty() {
		assertFalse(new CombinationIterator(
				new String[][]{new String[]{"a"}, new String[]{}}).hasNext());
	}
	
	public void testSingleOneElementArray() {
		Iterator it = new CombinationIterator(
				new String[][]{new String[]{"a"}});
		assertTrue(it.hasNext());
		assertArrayEquals(new String[]{"a"}, it.next());
		assertFalse(it.hasNext());
	}
	
	public void testSingleTwoElementArray() {
		Iterator it = new CombinationIterator(
				new String[][]{new String[]{"a", "b"}});
		assertTrue(it.hasNext());
		assertArrayEquals(new String[]{"a"}, it.next());
		assertTrue(it.hasNext());
		assertArrayEquals(new String[]{"b"}, it.next());
		assertFalse(it.hasNext());
	}

	public void testTwoOneElementArrays() {
		Iterator it = new CombinationIterator(
				new String[][]{new String[]{"a"}, new String[]{"b"}});
		assertTrue(it.hasNext());
		assertArrayEquals(new String[]{"a", "b"}, it.next());
		assertFalse(it.hasNext());
	}
	
	public void testTwoTwoElementArrays() {
		Iterator it = new CombinationIterator(
				new String[][]{
						new String[]{"a", "b"}, 
						new String[]{"c", "d"}});
		assertTrue(it.hasNext());
		assertArrayEquals(new String[]{"a", "c"}, it.next());
		assertTrue(it.hasNext());
		assertArrayEquals(new String[]{"b", "c"}, it.next());
		assertTrue(it.hasNext());
		assertArrayEquals(new String[]{"a", "d"}, it.next());
		assertTrue(it.hasNext());
		assertArrayEquals(new String[]{"b", "d"}, it.next());
		assertFalse(it.hasNext());
	}

	private void assertArrayEquals(Object a1, Object a2) {
		assertEquals(Arrays.asList((Object[]) a1), Arrays.asList((Object[]) a2));
	}
}
