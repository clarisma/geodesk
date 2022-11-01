/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.feature;

import com.clarisma.common.util.Consumable;
import com.geodesk.feature.store.TagValues;

import java.util.Map;

// TODO: should "value" always return strings?
//  If a number is too large or too precise to store in binary form,
//  it is stored as a string. This makes the API unpredictable.
//  But we could have doubleValue, intValue that return exact type
// TODO: implement as regular Iterable as well?

/**
 * A {@link Consumable} that can be used to iterate a feature's tags.
 */
public interface Tags extends Consumable
{
	boolean isEmpty();
	int size();
	boolean next();
	String key();
	Object value();
	String stringValue();
	default int intValue() { return TagValues.toInt(stringValue()); }

	// TODO: Decide if to... methods must preserve current iterator pos
	//  no, they consume all remaining elements
	Map<String,Object> toMap();	// TODO: just make it String,String?
}
