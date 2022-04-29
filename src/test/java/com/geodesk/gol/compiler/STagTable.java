package com.geodesk.gol.compiler;

// ===========================
//  T E S T   U S E   O N L Y
// ===========================

import com.clarisma.common.math.Decimal;
import com.clarisma.common.soar.SString;
import com.clarisma.common.soar.SharedStruct;
import com.clarisma.common.soar.StructOutputStream;
import com.geodesk.feature.store.TagValues;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

//
//  This is a stripped-down version used for testing of the Query Compiler
//

// TODO: If tagtable contains only uncommon keys, and no common keys,
// we need to write an end marker entry!!

// TODO: cut down memory consumption; instead of Entry array, maybe use:
//  encoded array of long (one for each k/v pair)
//  array of Object (one each for each k and each v:
//    GlobalString, SString, Character, Integer

public class STagTable extends SharedStruct implements Iterable<Map.Entry<String,String>>, Comparable<STagTable>
{
	private int hashCode;
	private int uncommonKeyCount;	// TODO: needed?
	private final Entry[] entries;
	
	public static final String EMPTY_ID = "<empty>";

	private enum ValueType
	{
		GLOBAL_STRING(true, false),
		LOCAL_STRING(true, true),
		NARROW_NUMBER(false, false),
		WIDE_NUMBER(false, true);
		
		private final boolean string;
		private final boolean wide;
	
		public boolean isString()
		{
			return string; 
		}
		
		public boolean isWide()
		{
			return wide; 
		}
		
		ValueType(boolean string, boolean wide)
		{
			this.string = string;
			this.wide = wide;
		}
	}

	// TODO: can we condense this to use less memory?
	public static class Entry implements Comparable<Entry>, Map.Entry<String, String>
	{
		// String tag;
		String key;
		int keyCode;
		SString keyString;
		ValueType type;
		String value;
		int valueCode;
		SString valueString;
		
		/**
		 * Common Keys are sorted in ascending order of string index.
		 * Uncommon Keys are sorted in reverse alphabetical order.
		 * Uncommon Keys are placed ahead of Common Keys
		 */
		public int compareTo(Entry o) 
		{
			if(keyString != null)
			{
				if(o.keyString==null) return -1;
				return o.keyString.compareTo(keyString);
			}
			else
			{
				if(o.keyString != null) return 1;
			}
			return Integer.compare(keyCode, o.keyCode);
		}
		
		int size()
		{
			int size = (keyCode==0) ? 4 : 2;
			switch(type)
			{
			case GLOBAL_STRING:
			case NARROW_NUMBER:
				return size + 2;
			case LOCAL_STRING:
			case WIDE_NUMBER:
				return size + 4;
			default:
				throw new IllegalStateException();
			}
		}
		
		boolean setDecimalValue(long d)
		{
			if(d == Decimal.INVALID) return false;
			int scale = Decimal.scale(d);
			if(scale > 3) return false;
			long dLong = Decimal.mantissa(d);
			if(dLong < TagValues.MIN_NUMBER || dLong > TagValues.MAX_WIDE_NUMBER) return false;
			if(scale > 0 || dLong > TagValues.MAX_NARROW_NUMBER)
			{
				type = ValueType.WIDE_NUMBER;
				valueCode = (((int)dLong - TagValues.MIN_NUMBER) << 2) | scale;
				return true;
			}
			type = ValueType.NARROW_NUMBER;
			valueCode = (int)dLong - TagValues.MIN_NUMBER;
			return true;
		}

		public String getKey() 
		{
			return key;
		}

		public String getValue() 
		{
			return value;
		}

		public String setValue(String value) 
		{
			// Does nothing, required by Map.Entry interface
			return null;
		}

		public SString keyString()
		{
			return keyString;
		}

		public SString valueString()
		{
			return valueString;
		}
	}

	private static SString getLocalString(Map<String,SString> localStrings, String s)
	{
		SString str = localStrings.get(s);
		if(str == null)
		{
			str = new SString(s);
			localStrings.put(s, str);
		}
		return str;
	}

	public STagTable(String[] tags, ObjectIntMap<String> globalStrings,
					 Map<String, SString> localStrings)
	{
		setAlignment(1);	// always 2-byte aligned

		if(tags==null || tags.length==0)
		{
			entries = new Entry[0];
			setSize(4);
			return;
		}
		
		int size = 0;
		int uncommonSize = 0;
		entries = new Entry[tags.length / 2];
		for(int i=0; i<tags.length; i+=2)
		{
			String k = tags[i];
			String v = tags[i+1];
			Entry e = new Entry();
			e.key = k;
			e.value = v;
			e.keyCode = globalStrings.get(k);
			if(e.keyCode == 0  || e.keyCode > TagValues.MAX_COMMON_KEY)
			{
				e.keyCode = 0;
				e.keyString = getLocalString(localStrings, k);
				e.keyString.setAlignment(2);   
					// strings used as keys must be 4-byte aligned
				uncommonKeyCount++;
			}
			e.valueCode = globalStrings.get(v);
			if(e.valueCode != 0)
			{
				e.type = ValueType.GLOBAL_STRING;
			}
			else
			{
				long d = Decimal.parse(v, true);	
					// strict=true (formatting the decimal value must
					// produce the same string)
				if(!e.setDecimalValue(d))
				{
					e.type = ValueType.LOCAL_STRING;
					e.valueString = getLocalString(localStrings, v);
				}
			}
			entries[i / 2] = e;
			int entrySize = e.size();
			size += entrySize;
			if(e.keyString != null) uncommonSize += entrySize;
		}
	
		Arrays.sort(entries);
		if(uncommonKeyCount == entries.length)
		{
			// A tag table that only has uncommon keys must have
			// an empty table marker where the global keys would
			// normally be
			assert size == uncommonSize;
			size += 4;
		}
		setSize(size);
		setAnchor(uncommonSize);
	}
	
	public void writeTo(StructOutputStream out) throws IOException 
	{
		if(entries.length == 0)
		{
			out.writeInt(TagValues.EMPTY_TABLE_MARKER);
			return;
		}
	
		int origin = (location() + anchor()) & 0xffff_fffc;
		for(int i=0; i<entries.length; i++)
		{
			Entry e = entries[i];
			boolean isUncommonKey = e.keyString != null;
					
			if(!isUncommonKey)
			{
				int key = e.keyCode << 2;
				if(e.type.isString()) key |= 1;
				if(e.type.isWide()) key |= 2;
				if(i == entries.length - 1) key |= 0x8000;
				out.writeShort(key);
			}
			
			if(e.type == ValueType.LOCAL_STRING)
			{
				out.writePointer(e.valueString);
			}
			else if(e.type.isWide())
			{
				out.writeInt(e.valueCode);
			}
			else
			{
				out.writeShort(e.valueCode);
			}
			
			if(isUncommonKey)
			{
				int ptr = e.keyString.location() - origin;
				assert (ptr & 3) == 0;
				ptr <<= 1;
				if(e.type.isString()) ptr |= 1;
				if(e.type.isWide()) ptr |= 2;
				if(i == 0) ptr |= 4;
				out.writeInt(ptr);
					// don't use writePointer, pointers to uncommon
					// keys require special handling
			}
		}

		if(uncommonKeyCount == entries.length)
		{
			// Write an empty marker in case the tagtable
			// consists purely of uncommon keys
			out.writeInt(TagValues.EMPTY_TABLE_MARKER);
			return;
		}
	}

	public int hashCode() 
	{
		if(hashCode == 0)
		{
			hashCode = 17;
			for(Entry e: entries)
			{
				hashCode = 37 * hashCode + e.getKey().hashCode(); 
				hashCode = 37 * hashCode + e.getValue().hashCode();
			}
		}
		return hashCode;
	}
	
	public boolean equals(Object other)
	{
		if(!(other instanceof STagTable)) return false;
		STagTable o = (STagTable)other;
		if(entries.length != o.entries.length) return false;
		for(int i=0; i<entries.length; i++)
		{
			Entry a = entries[i];
			Entry b = o.entries[i];
			if(!a.key.equals(b.key)) return false;
			if(!a.value.equals(b.value)) return false;
		}
		return true;
	}
	
	public int uncommonKeyCount()
	{
		return uncommonKeyCount;
	}

	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("TAGS ");
		
		for(int i=0; i<entries.length; i++)
		{
			if(i > 0) sb.append('/');
			Entry e = entries[i];
			if(e.keyCode == 0) 
			{
				sb.append('\"');
				sb.append(e.key);
				sb.append('\"');
			}
			else
			{
				sb.append(e.key);
			}
			sb.append("=");
			if(e.type == ValueType.LOCAL_STRING)
			{
				sb.append('\"');
				sb.append(e.value);
				sb.append('\"');
			}
			else
			{
				sb.append(e.value);
			}
		}
		
		// sb.append(sharedSuffix());
		return sb.toString();
	}

	public Iterator<Map.Entry<String, String>> iterator()
	{
		return new Iterator<>()
		{
			int i;

			@Override public boolean hasNext()
			{
				return i < entries.length;
			}

			@Override public Entry next()
			{
				return entries[i++];
			}
		};
	}

	// TODO: this is inefficient, check len first!
	@Override public int compareTo(STagTable other)
	{
		int thisLen = entries.length;
		int otherLen = other.entries.length;

		int len = Math.min(thisLen, otherLen);
		for(int i=0; i<len; i++)
		{
			int comp = Integer.compare(entries[i].keyCode, other.entries[i].keyCode);
			if(comp != 0) return comp;
			comp = Integer.compare(entries[i].valueCode, other.entries[i].valueCode);
			if(comp != 0) return comp;
		}
		return Integer.compare(thisLen, otherLen);
	}
}
