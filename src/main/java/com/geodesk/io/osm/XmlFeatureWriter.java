package com.geodesk.io.osm;

import java.io.FileOutputStream;
import java.io.OutputStream;

import com.clarisma.common.xml.XmlWriter;
import com.geodesk.feature.FeatureId;

import com.geodesk.feature.FeatureType;

public class XmlFeatureWriter extends XmlWriter
{
	public XmlFeatureWriter(OutputStream out)
	{
		super(out);
	}
	
	public void tag(String k, String v)
	{
		empty("tag k=\"%s\" v=\"%s\"", escape(k), escape(v)); 
	}
	
	public void tags(String[] tags)
	{
		for(int i=0; i<tags.length; i+=2) tag(tags[i], tags[i+1]);
	}
	
	public void wayNodes(long[] nodes)
	{
		for(int i=0; i<nodes.length; i++)
		{
			begin("nd");
			attr("ref", nodes[i]);
			end();
		}
	}

	public void beginNode(long id)
	{
		begin("node");
		attr("id", id);
	}

	public void beginWay(long id)
	{
		begin("way");
		attr("id", id);
	}
	
	public void way(long id, long[] nodes, String[] tags)
	{
		beginWay(id);
		wayNodes(nodes);
		tags(tags);
		end();
	}
	
	public void member(FeatureType type, long id, String role)
	{
		begin("member");
		attr("type", FeatureType.toString(type));
		attr("ref", id);
		attr("role", role);
		end();
	}
	
	public void members(long[] members, String[] roles)
	{
		for(int i=0; i<members.length; i++)
		{
			long m = members[i];
			member(FeatureId.type(m), FeatureId.id(m), roles[i]);
		}
	}
	
	public void beginRelation(long id)
	{
		begin("relation");
		attr("id", id);
	}
	
	public void relation(long id, long[] members, String[] roles, String[] tags)
	{
		beginRelation(id);
		members(members, roles);
		tags(tags);
		end();
	}
	
	public static void main(String[] args) throws Exception
	{
		XmlFeatureWriter out = new XmlFeatureWriter(new FileOutputStream("c:\\velojoe\\xml-test.xml"));
		out.begin("group");
		out.way(123, new long[]{ 456, 789 }, new String[]{ "test", "abc", "more", "def" });
		out.way(2000300, new long[]{ 1122, 33456, 22789 }, new String[]{ "test2", "abc", "more2", "def" });
		out.end();
		out.begin("test");
		out.begin("empty");
		out.end();
		out.end();
		out.close();
	}
}
