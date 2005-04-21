/*
 * Created on 18.04.2005 by Joerg Garbers, FU-Berlin
 *
 */
package de.fuberlin.wiwiss.d2rq.rdql;

import junit.framework.TestCase;

public class OverloadTest extends TestCase {

    public OverloadTest() {
        super();
    }

    public OverloadTest(String arg0) {
        super(arg0);
    }

    
    public interface A {
        public void doIt();
    }
    
    public class B implements A {
        public void doIt() {
            System.out.println("do");
        }
    }
    
    public void overload(B b) {
        System.out.println("B");
    }
    public void overload(A a) {
        System.out.println("A");
    }
    public void overload(Object x) {
        System.out.println("Object");        
    }
    
    public void overloadX(Object x) {
        overload(x);
    }
    
    public void testOverload() {
        A a=new B();
        B b=new B();
        Object x=a;
 
        // why on earth does Java not choose the more specific method B?
        // see output
        overload(a); // "A" 
        overload(b); // "B"
        overload(x); // "Object"
        overloadX(a); // "Object" 
        overloadX(b); // "Object"
        overloadX(x); // "Object"
    }
}

