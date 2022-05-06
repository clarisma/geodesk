package com.geodesk.feature.store;

import com.clarisma.common.math.Decimal;
import com.clarisma.common.util.Bytes;
import com.geodesk.core.Mercator;
import com.geodesk.feature.*;
import com.geodesk.core.Box;
import com.geodesk.feature.query.EmptyView;
import com.geodesk.feature.query.ParentRelationView;

import java.nio.ByteBuffer;
import java.util.*;


public abstract class StoredFeature implements Feature
{
	protected final FeatureStore store;
	protected final ByteBuffer buf;
	protected final int ptr;
	protected String role;

	private static final int TAG_POINTER_OFFSET = 8;

	public static final String[] EMPTY_TAGS = new String[0];
	public static final Iterable<Node> EMPTY_NODES = new ArrayList<>(0);
	public static final Iterable<Feature> EMPTY_MEMBERS = new ArrayList<>(0);


	public StoredFeature(FeatureStore store, ByteBuffer buf, int ptr)
	{
		this.store = store;
		this.buf = buf;
		this.ptr = ptr;
	}

	public FeatureStore store()
	{
		return store;
	}

	protected ByteBuffer buffer()
	{
		return buf;
	}

	@Override public long id()
	{
		// ByteBuffer buf = buffer.buffer();
		return id(buf, ptr);
	}

	public static long id(ByteBuffer buf, int ptr)
	{
		return ((long) (buf.getInt(ptr) >>> 8) << 32) |
			((long) (buf.getInt(ptr + 4)) & 0xffff_ffffL);
	}

	protected int getFlags()
	{
		return buf.getInt(ptr);
	}

	/*
	public static int type(ByteBuffer buf, int ptr)
	{
		return (buf.getInt(ptr) >> 3) & 3;
	}
	 */

	public int flags()
	{
		return buf.get(ptr);	// get single byte
	}

	@Override public int x()
	{
		return (buf.getInt(ptr - 16) + buf.getInt(ptr - 8)) / 2;
	}

	@Override public int y()
	{
		return (buf.getInt(ptr - 12) + buf.getInt(ptr - 4)) / 2;
	}

	@Override public boolean equals(Object other)
	{
		if(!(other instanceof Feature)) return false;
		Feature o = (Feature)other;
		return type()==o.type() && id() == o.id();
	}

	@Override public int hashCode()
	{
		return Long.hashCode(id());
	}

	/**
	 * Checks if the feature's tagtable contains the given
	 * common key, and returns its value in the following form:
	 *
	 *   Bit 0:		  type (0=number, 1=string)
	 *   Bit 1:		  size flag (0=narrow, 1=wide)
	 *   Bit 2-15:	  undefined
	 *   Bits 16-31:  narrow value
	 *   Bits 32-63:  pointer to wide value
	 *
	 * If the value is narrow, bits 32-63 are undefined.
	 * If the value is wide, bits 16-31 are undefined.
	 * If the key is not found, this method returns 0.
	 *
	 * @param pTags pointer to tagtable
	 * @param key   common key
	 * @return the tag value (see above) or 0 if not found
	 */
	// TODO: Use -1 to signify not found, vs. 0?
	//  maybe not necessary, since return value is nonzero
	//  even if the tag value is narrow number 0 because
	//  we grab the key value as well
	//  (but should document this)
	protected long getCommonKeyValue(int pTags, int key)
	{
		int keyBits = key << 2;
		int p = pTags;
		for (; ; )
		{
			int tag = buf.getInt(p);
			if ((char) tag >= keyBits)
			{
				if ((tag & 0x7ffc) != keyBits) return 0;
				return ((long) (p + 2) << 32) | ((long) tag & 0xffff_ffffL);
				// careful, sign extension when Or-ing long and int
			}
			p += 4 + (tag & 2);
		}
	}

	/**
	 * Checks if the feature's tagtable contains the given
	 * key, and returns its value in the following form:
	 *
	 * 	   Bit 0:		type (0=number, 1=string)
	 *     Bit 1:		size flag (0=narrow, 1=wide)
	 * 	   Bit 2-15:	undefined
	 * 	   Bits 16-31:	narrow value
	 * 	   Bits 32-63:  pointer to wide value
	 *
	 * If the value is narrow, bits 32-63 are undefined.
	 * If the value is wide, bits 16-31 are undefined.
	 * If the key is not found, this method returns 0.
	 *
	 * @param keyString
	 * @return
	 */
	protected long getKeyValue(String keyString)
	{
		int key = store.codeFromString(keyString);
		int ppTags = ptr + 8;
		int pTags = buf.getInt(ppTags);
		int uncommonKeysFlag = pTags & 1;
		pTags = ppTags + (pTags ^ uncommonKeysFlag);
		int p = pTags;
		if (key != 0 && key <= TagValues.MAX_COMMON_KEY)
		{
			return getCommonKeyValue(pTags, key);
		}
		if (uncommonKeysFlag == 0) return 0;
		int origin = pTags & 0xffff_fffc;
		p -= 6;
		for (; ; )
		{
			long tag = buf.getLong(p);
			int rawPointer = (int) (tag >> 16);
			int flags = rawPointer & 7;
			int pKey = ((rawPointer ^ flags) >> 1) + origin;    // preserve sign
			// uncommon keys are relative to the 4-byte-aligned tagtable address
			// if (Bytes.stringEqualsAscii(buf, pKey, keyString))
				// TODO: can't assume keys to be ASCII-only, use this slower method for now:
			if (Bytes.stringEquals(buf, pKey, keyString))
			{
				return (((long) (p - 2) << 32) | flags |
					(((long) ((char) tag)) << 16));
				// careful, sign extension when Or-ing long and int
			}
			if ((flags & 4) != 0) return 0;
			p -= 6 + (flags & 2);
		}
	}

	/**
	 * Returns the string representation of a tag value.
	 *
	 * @param value The tag value in the following form:
	 *
	 *              Bit 0:		type (0=number, 1=string)
	 *              Bit 1:		size flag (0=narrow, 1=wide)
	 *              Bit 2-15:	unused
	 *              Bits 16-31:	narrow value (if size flag =0)
	 *              Bits 32-63: pointer to wide value (if size flag =1)
	 *
	 *              If value is 0, an empty string is returned.
	 * @return
	 */
	private String valueAsString(long value)
	{
		if (value == 0) return "";
		int typeAndSize = (int) value & 3;
		if (typeAndSize == 1)
		{
			// narrow string
			return store.stringFromCode((char) (value >> 16));
		}
		if (typeAndSize == 3)
		{
			// wide string
			int ppValue = (int) (value >> 32);    // preserve sign? (TODO: should be absolute ptr)
			int pValueString = buf.getInt(ppValue) + ppValue;
			return Bytes.readString(buf, pValueString);
		}
		if (typeAndSize == 0)
		{
			// narrow number
			int number = (int) ((char) (value >> 16)) + TagValues.MIN_NUMBER;
			return Integer.toString(number);
		}
		// wide number
		int number = buf.getInt((int) (value >> 32));    // preserve sign? (TODO: should be absolute ptr)
		int mantissa = (number >>> 2) + TagValues.MIN_NUMBER;
		int scale = number & 3;
		if (scale == 0) return Integer.toString(mantissa);
		return Decimal.toString(Decimal.of(mantissa, scale));
	}

	private int valueAsInt(long value)
	{
		if (value == 0) return 0;
		int typeAndSize = (int) value & 3;
		if (typeAndSize == 0)
		{
			// narrow number
			return (int) ((char) (value >> 16)) + TagValues.MIN_NUMBER;
		}
		if (typeAndSize == 2)
		{
			// wide number
			int number = buf.getInt((int) (value >> 32));    // preserve sign? (TODO: should be absolute ptr)
			int mantissa = (number >>> 2) + TagValues.MIN_NUMBER;
			int scale = number & 3;
			return Decimal.toInt(Decimal.of(mantissa, scale));
		}
		if (typeAndSize == 3)
		{
			// wide string
			int ppValue = (int) (value >> 32);    // preserve sign? (TODO: should be absolute ptr)
			int pValueString = buf.getInt(ppValue) + ppValue;
			String s = Bytes.readString(buf, pValueString);
			// TODO: should be: return MathUtils.doubleFromString(s);
			return TagValues.toInt(s);
		}
		// narrow string
		String s = store.stringFromCode((char) (value >> 16));
		// TODO: should be: return MathUtils.doubleFromString(s);
		return TagValues.toInt(s);
	}

	private Object valueAsObject(long value)
	{
		if (value == 0) return "";
		int typeAndSize = (int) value & 3;
		if (typeAndSize == 1)
		{
			// narrow string
			return store.stringFromCode((char) (value >> 16));
		}
		if (typeAndSize == 3)
		{
			// wide string
			int ppValue = (int) (value >> 32);    // preserve sign
			int pValueString = buf.getInt(ppValue) + ppValue;
			return Bytes.readString(buf, pValueString);
		}
		if (typeAndSize == 0)
		{
			// narrow number
			return (int) ((char) (value >> 16)) + TagValues.MIN_NUMBER;
		}
		// wide number
		int number = buf.getInt((int) (value >> 32));    // preserve sign
		int scale = number & 3;
		if (scale == 0)
		{
			return (number >>> 2) + TagValues.MIN_NUMBER;
		}
		// return Double.valueOf(Decimal.toDouble(Decimal.of(mantissa, scale)));
		return TagValues.wideNumberToDouble(number);
		// We return a Double since it is more familiar to users
	}


	/**
	 * Returns the value of the given key as a String.
	 *
	 * @param key
	 * @return the key's value, or an empty string
	 */
	@Override public String stringValue(String key)
	{
		long value = getKeyValue(key);
		return valueAsString(value);
	}

	/**
	 * Returns the value of the given key as a integer.
	 *
	 * @param key
	 * @return the key's value, or 0 if the key does not
	 * exist, or its value is not a valid number
	 */
	@Override public int intValue(String key)
	{
		long value = getKeyValue(key);
		return valueAsInt(value);
	}

	@Override public String tag(String key)
	{
		return stringValue(key);
	}

	// TODO: should we have concept of a "null map" to avoid null check and branching?

	@Override public boolean hasTag(String key)
	{
		return getKeyValue(key) != 0;
	}

	@Override public boolean hasTag(String key, String value)
	{
		return stringValue(key).equals(value);
	}

	@Override public boolean isArea()
	{
		return (buf.getInt(ptr) & FeatureFlags.AREA_FLAG) != 0;
	}

	@Override public Box bounds()
	{
		// bbox for Way/Relation
		return new Box(
			buf.getInt(ptr-16), buf.getInt(ptr-12),
			buf.getInt(ptr-8), buf.getInt(ptr-4));
	}

	@Override public String role()
	{
		return role;
	}

	public void setRole(String role)
	{
		this.role = role;
	}

	@Override public boolean belongsTo(Feature parent)
	{
		if(parent instanceof Relation rel)
		{
			return parentRelations().contains(rel);
		}

		// TODO: parent way
		return false;
	}

	// TODO: optimize, building Geometry is not needed
	@Override public double area()
	{
		if(!isArea()) return 0;
		int avgY = (buf.getInt(ptr - 12) + buf.getInt(ptr - 4)) / 2;
		double scale = Mercator.metersAtY(avgY);
		return toGeometry().getArea() * scale * scale;
	}

	/*
	protected int getTagTable()
	{
		int ppTags = ptr+8;
		return (buf.getInt(ppTags) & 0xfffe) + ppTags;
	}
	
	protected int getBody()
	{
		int ppBody = ptr+12;
		return buf.getInt(ppBody) + ppBody;
	}
	*/
	

	/*
	public static FeatureHandle of(FeatureFile file, ByteBuffer buf, int p)
	{
		int type = (buf.getInt(p) >>> 3) & 3;
		if(type==0) return new NodeHandle(file, buf, p);
		if(type==1) return new WayHandle(file, buf, p);
		if(type==2) return new RelationHandle(file, buf, p);
		assert false;
		return null;
	}
	*/

	@Override
	public Tags tags()
	{
		return new TagIterator(ptr + 8);
	}

	private class TagIterator implements Tags
	{
		private final int pTagTable;
		private final int uncommonKeysFlag;
		private int pNextTag;
		private String key;
		private long value;

		public TagIterator(int ppTags)
		{
			int rawTagsPtr = buf.getInt(ppTags);
			uncommonKeysFlag = rawTagsPtr & 1;
			pTagTable = (rawTagsPtr ^ uncommonKeysFlag) + ppTags;
			reset();
		}

		private void reset()
		{
			pNextTag = pTagTable;
			if (buf.getInt(pNextTag) == TagValues.EMPTY_TABLE_MARKER)
			{
				// If there are no common keys in the tagtable,
				// set pointer to first uncommon key, or -1 if table
				// is completely empty
				pNextTag = (uncommonKeysFlag != 0) ? (pTagTable - 6) : -1;
			}
		}

		/**
		 * Checks if there are more tags, and if so, retrieves the next.
		 *
		 * @return true if the next tag has been retrieved,
		 * otherwise false
		 */
		public boolean next()
		{
			if (pNextTag < 0) return false;
			if (pNextTag < pTagTable)
			{
				// Uncommon keys (located ahead of the tagtable pointer)
				// pNextTag points 2 bytes ahead of the key pointer, so
				// we scoop up the narrow value as well
				// (Remember, for uncommon tags, the value is located
				// ahead of the key). We could theoretically read a
				// wide value as well, but we risk a buffer overrun
				// if we're at the last tag and it has a narrow value.
				long tag = buf.getLong(pNextTag);
				int rawPointer = (int) (tag >> 16);
				int flags = rawPointer & 7;
				int origin = pTagTable & 0xffff_fffc;
				// uncommon keys are relative to the 4-byte-aligned tagtable address
				int pKey = ((rawPointer ^ flags) >> 1) + origin;    // preserve sign
				key = Bytes.readString(buf, pKey);
				value = ((((long) (pNextTag - 2)) << 32) | flags | (((long) ((char) tag)) << 16));
				// careful, sign extension when Or-ing long and int
				if ((flags & 4) != 0)
				{
					// This was the last uncommon key
					pNextTag = -1;
				}
				else
				{
					// Move pointer to next uncommon key
					// (either by 6 or 8 bytes)
					pNextTag -= 6 + (flags & 2);
				}
			}
			else
			{
				// Common keys
				int tag = buf.getInt(pNextTag);
				key = store.stringFromCode((tag >> 2) & 0x1fff);
				value = (((long) (pNextTag + 2)) << 32) | ((long) tag & 0xffff_ffffl);
				if ((tag & 0x8000) != 0)
				{
					// last common key reached:
					// If there are uncommon keys, move pointer to first one,
					// else we are done (set pointer < 0)
					pNextTag = (uncommonKeysFlag == 0) ? -1 : (pTagTable - 6);
				}
				else
				{
					// Move to next common key
					pNextTag += 4 + (tag & 2);
				}
			}
			return true;
		}

		@Override public String key()
		{
			return key;
		}

		@Override public Object value()
		{
			return valueAsObject(value);
		}

		@Override public String stringValue()
		{
			return valueAsString(value);
		}

		@Override public int intValue()
		{
			return valueAsInt(value);
		}

		@Override public Map<String, Object> toMap()
		{
			Map<String, Object> map = new HashMap<>();
			int pOld = pNextTag;
			reset();
			while (next())
			{
				// assert !map.containsKey(key()): String.format("Duplicate key %s in %s", key(), FeatureHandle.this);
				map.put(key(), value());
			}
			pNextTag = pOld;
			return map;
		}

		@Override public boolean isEmpty()
		{
			return buf.getInt(pTagTable) == TagValues.EMPTY_TABLE_MARKER &&
				uncommonKeysFlag == 0;
		}

		// TODO: could be much more efficient
		@Override public int size()
		{
			int pOld = pNextTag;
			reset();
			int count = 0;
			while (next()) count++;
			pNextTag = pOld;
			return count;
		}

		@Override public String toString()
		{
			StringBuilder buf = new StringBuilder();
			boolean first = true;
			int pOld = pNextTag;
			reset();
			while (next())
			{
				if (first)
				{
					first = false;
				}
				else
				{
					buf.append(',');
				}
				buf.append(key());
				buf.append('=');
				buf.append(value());	// TODO: escape?
			}
			pNextTag = pOld;
			return buf.toString();
		}
	}

	/*
	public void dumpTags()
	{
		TagIterator iter = new TagIterator(ptr + 8);
		while (iter.next())
		{
			FeatureStoreBase.log.debug("  {}={}", iter.key(), iter.value());
			// FeatureStore.log.debug("  Key {}", iter.key());
			// FeatureStore.log.debug("    = {}", iter.value());
		}
	}
	 */

	@Override public boolean belongsToRelation()
	{
		return (buf.getInt(ptr) & FeatureFlags.RELATION_MEMBER_FLAG) != 0;
	}

	@Override public Features<Relation> parentRelations()
	{
		return belongsToRelation() ? new ParentRelationView(store, buf, getRelationTablePtr()) :
			(Features<Relation>) EmptyView.ANY;
	}

	/**
	 * Retrieves the pointer to the feature's relation table.
	 * The feature must be a member of at least one relation, otherwise
	 * the result of this method will be invalid.
	 *
	 * @return a pointer to the feature's relation table
	 */
	protected int getRelationTablePtr()
	{
		// Default implementation for Ways and Relations:
		// reltable pointer is placed just ahead of the body anchor
		int ppBody = ptr+12;
		int pBody = buf.getInt(ppBody) + ppBody;
		int ppRelTable = pBody-4;
		return buf.getInt(ppRelTable) + ppRelTable;
	}

	public boolean matches(Filter filter)
	{
		return filter.accept(buf, ptr);
	}
}

