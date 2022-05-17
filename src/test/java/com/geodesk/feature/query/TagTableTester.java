package com.geodesk.feature.query;

import com.clarisma.common.soar.Archive;
import com.clarisma.common.soar.SString;
import com.clarisma.common.soar.Struct;
import com.clarisma.common.soar.StructOutputStream;
import com.geodesk.gol.compiler.STagTable;
import com.clarisma.common.fab.FabException;
import com.clarisma.common.fab.FabReader;
import com.clarisma.common.parser.TagsParser;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class TagTableTester
{
	private Map<String, Map<String,Object>> cases = new HashMap<>();
	private ObjectIntMap<String> stringTable;
	private Random random = new Random(); 
	
	public TagTableTester() throws FabException, IOException
	{
		ClassLoader cl = getClass().getClassLoader();
		loadStringTable(cl.getResourceAsStream("feature/strings.txt"));
		loadCases(cl.getResourceAsStream("feature/tags.fab"));
	}
	
	void loadCases(InputStream in) throws FabException, IOException
	{
		ClassLoader cl = getClass().getClassLoader();
		loadStringTable(cl.getResourceAsStream("feature/strings.txt"));
		new CaseReader().read(cl.getResourceAsStream("feature/tags.fab"));
	}
	
	void loadStringTable(InputStream in) throws IOException
	{
		MutableObjectIntMap<String> st = ObjectIntMaps.mutable.empty();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in)); 
	    for (;;) 
        {
            String s = reader.readLine();
            if (s == null) break;
            st.put(s, st.size()+1);	// 1-based index
        }
	    stringTable = st;
    }
	
	private class CaseReader extends FabReader
	{
		TagsParser parser = new TagsParser();
		protected void keyValue(String key, String value)
		{
			// log.debug("{}",  key);
			parser.parse(value);
			cases.put(key, parser.tags());
		}
	}

	public Map<String,Object> getTags(String name)
	{
		return cases.get(name);
	}

	public static String[] tagsAsStringArray(Map<String,Object> map)
	{
		String[] tags = new String[map.size() * 2];
		int i=0;
		for(Map.Entry<String,Object> e: map.entrySet())
		{
			tags[i++] = e.getKey();
			tags[i++] = e.getValue().toString();
		}
		return tags;
	}

	private String randomUncommonKey()
	{
		int len = random.nextInt(256)+1;
		char[] chars = new char[len];
		boolean colon = true;
		for(int i=0; i<len; i++)
		{
			char c;
			int code = random.nextInt(colon ? 52 : 53);
			if(code < 26)
			{
				c = (char)('a' + code);
				colon = false;
			}
			else if(code < 52)
			{
				c = (char)('A' + code - 26);
				colon = false;
			}
			else
			{
				c = ':';
				colon = true;
			}
			chars[i] = c;
		}
		return new String(chars);
	}
	
	private String randomString()
	{
		int len = random.nextInt(256)+1;
		char[] chars = new char[len];
		int i=0;
		while(i<len)
		{
			char c = (char)random.nextInt(1 << 16);
			if(c != ' ' && !Character.isLetterOrDigit(c)) continue;
			chars[i++] = c;
		}
		return new String(chars);
	}
	
	/**
	 * 
	 * @param name
	 * @param maxRandomTags
	 * @param excludeTags
	 */
	public ByteBuffer makeCase(String name, int maxRandomTags, Set<String> excludeTags)
	{
		Map<String,Object> tags = getTags(name);
		assert tags != null: String.format("TagTable case \"%s\" not found", name);
		TagTestArchive archive = new TagTestArchive(tagsAsStringArray(tags));
		return archive.create(name);
	}

	private class TagTestArchive extends Archive
	{
		public TagTestArchive(String[] tags)
		{
			Map<String, SString> localStrings = new HashMap<>();
			STagTable tagTable = new STagTable(tags, stringTable, localStrings);
			STestFeature feature = new STestFeature(tagTable);
			setHeader(feature);
			place(tagTable);
			for(SString str: localStrings.values())
			{
				place(str);
			}
		}

		public ByteBuffer create(String name)
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StructOutputStream out = new StructOutputStream(baos);
			try
			{
				out.writeChain(header());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			// TODO: fix
			try
			{
				writeFile(Path.of("c:\\velojoe\\debug\\tags", name + ".bin"));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());
			buf.order(ByteOrder.LITTLE_ENDIAN);
			return buf;
		}
	}

	private static class STestFeature extends Struct
	{
		STagTable tagTable;

		public STestFeature(STagTable tagTable)
		{
			this.tagTable = tagTable;
			setSize(16);
		}

		@Override public void writeTo(StructOutputStream out) throws IOException
		{
			out.writeLong(0);
			out.writePointer(tagTable, tagTable.uncommonKeyCount() > 0 ? 1 : 0);
			out.writeInt(0);
		}
	}
	
	public static void main(String[] args) throws Exception
	{
		TagTableTester tester = new TagTableTester();
		/*
		for(int i=0; i<100; i++)
		{
			log.debug("{}", tester.randomUncommonKey());
			log.debug("{}", tester.randomString());
		}
		*/
	}
}
