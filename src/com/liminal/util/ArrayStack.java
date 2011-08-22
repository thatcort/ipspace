/*
 * Created on 29-Dec-2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.liminal.util;

import java.util.ArrayList;
import java.util.Collection;

public class ArrayStack <T> extends ArrayList<T> {

	public ArrayStack() {
	}

	public ArrayStack(Collection c) {
		super(c);
	}

	public T peek() {
		return get(size()-1);
	}
	
	public T pop() {
		return remove(size()-1);
	}
	
	public void push(T obj) {
		add(obj);
	}
	
}
