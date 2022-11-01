/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.geodesk.io.osm;

public class HeaderData 
{
	public String[] requiredFeatures;
	public String[] optionalFeatures;
	public String writingProgram;
	public String source;
	public long replicationTimestamp;
	public long replicationSequence;
	public String replicationUrl;
}
