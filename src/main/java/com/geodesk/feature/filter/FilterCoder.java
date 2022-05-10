package com.geodesk.feature.filter;

import com.clarisma.common.ast.*;
import com.geodesk.feature.store.TagValues;
import org.objectweb.asm.Label;

import java.nio.charset.StandardCharsets;

import static org.objectweb.asm.Opcodes.*;

/**
 * A bytecode generator that produces a TagFilter class for a given query
 * string. Note that the filter code does not verify the feature type; it is
 * the caller's responsibility to only apply the TagFilter to features
 * of the appropriate type.
 *
 * This class relies on the ExpressionCoder base class to encode the
 * expression tree of each TagClause. Only a subset of expressions are
 * supported:
 *
 * BinaryExpression:
 * 	   - AND/OR
 * 	   - value EQ/NE to String, GlobalString or Double
 * 	   - value LT/LE/GT/GE Double
 * 	   - value StartsWith/EndsWith/MatchesRegex String
 * 	   - value IN/NOT_IN List of GlobalString
 *
 * UnaryExpression:
 * 	   - NOT
 *
 *
 */

/*
	IMPLEMENTATION NOTES
   	════════════════════

   	1. Layout of Tag Tables
   	━━━━━━━━━━━━━━━━━━━━━━━

	Tag Tables must follow the layout described in the Tile File Format
	Specification. To recap, tags are stored as key/value combos. Tags
	with a frequently used key (found in the Global String Table)
	are Global-Key Tags, which are stored in ascending order of their
	numeric key value, starting at the anchor address of the Tag Table.
	Tags with uncommon key values use a pointer to locally-stored UTF-8
	string (Local-Key Tags), and are placed in reverse alphabetical order
	ahead of the Tag Table's anchor point.

	Each tag has a value, which takes up 2 or 4 bytes, accommodating four
	different types: narrow string (a number from 1 to 64K-1 which refers to
	an entry in the Global String Table), wide string (a pointer to a local
	string), narrow number (-256 to ~64K), or wide number (a decimal with a
	mantissa -256 to ~1M, and 0 to 3 digits after the decimal point).
	Numerical values that cannot be represented as narrow or wide numbers
	are stored as strings.

	Values of Global-Key Tags are stored immediately after their key, values
	of Local-Key Tags are stored ahead of their key (This allows us to scan
	the Tag Table from the Tag-Table Pointer -- forward for global, backward
	for local).

         String pointer                          Global string value
         or wide number                          or narrow number
	     │                                       │
	     │         Local key      Local key      │    Global key
         │         │              │              │    │
	  ═╦═════════╤═════════╦════╤═════════╦════╤════╦════╤═════════╦═
	...║ XX ╎ XX │  "dog"  ║ XX │ "apple" ║ 13 │ XX ║ 42 │ XX ╎ XX ║...
      ═╩═════════╧═════════╩════╧═════════╩════╧════╩════╧═════════╩═
                             │              ┃              │
                     Global string value    Global key     String pointer
                     or narrow number       ┃              or wide number
                                            ┃
                                            Tag-table pointer
                                            points here

                ⯇── keys increase backward  ┆  keys increase forward ──⯈


    Global Keys are 16 bits wide and have the following format:

    ┏━━━━┳━━━━━━┳━━━┳━━━┓
    ┃ 15 ┃ 2-14 ┃ 1 ┃ 0 ┃
    ┗━━┯━┻━━━┯━━┻━┯━┻━┯━┛
       │     │    │   ╰── type bit: 1 = string, 0 = number
       │     │    ╰────── size bit: 1 = wide, 0 = narrow
       │     ╰─────────── entry in the Global String Table (first 8K entries)
       ╰───────────────── 1 = last global-key tag

    Local Keys are 32 bits wide and have the following format:

    ┏━━━━━━┳━━━┳━━━┳━━━┓
    ┃ 3-31 ┃ 2 ┃ 1 ┃ 0 ┃
    ┗━━━┯━━┻━┯━┻━┯━┻━┯━┛
        │    │   │   ╰── type bit: 1 = string, 0 = number
        │    │   ╰────── size bit: 1 = wide, 0 = narrow
        │    ╰────────── 1 = last local-key tag
        ╰─────────────── relative pointer to the key string (see note)

    Since we only have 29 bits to represent the string pointer, it can only
    refer to 4-byte aligned strings inside a 1-GB tile file. However, Tag
    Tables (and hence the position of a Local Key) are only guaranteed to be
    2-byte aligned, so we use a little trick: We don't base the pointer off
    the address where it is stored (as we do for all other pointers), but
    we add it to the address of the Tab Table, with its lower 2 bits set to
    zero.

	If a Tag Table contains Local-Key Tags, bit 0 of the Tag-Table Pointer
	is set to 1.

	2. Queries
	━━━━━━━━━━

	A Query is composed of one or more Selectors. A Selector applies to one
	or more feature types (node, way, area, relation) and contains one or
	more Tag Clauses. Tag Clauses are ordered: global keys, in ascending
	order, then local keys, in ascending alphabetical order (This allows
	scanning of keys in the same order in which they are laid out in the
	Tag Table). There is never more than one Tag Clause per key (multiple
	clauses in a query string, such as "[maxspeed>20][maxspeed<70]" are
	consolidated by the Query Parser).

	A Tag Clause is "Positive" if its key must be present and not be "no".
	A Tag Clause is "Negative" if its key can (or must) be absent, or be "no".

	A Tag Clause can contain an Expression used to check a tag's value
	(without an Expression, the generated TagFilter merely checks for
	presence or absence of a key).

 */

public class FilterCoder extends ExpressionCoder
{
	private static final String
		FILTER_BASE_CLASS = "com/geodesk/feature/filter/TagFilter",
		BYTES_CLASS = "com/clarisma/common/util/Bytes",
		TAG_VALUES_CLASS = "com/geodesk/feature/store/TagValues";

	private static final String
		FILTER_KEYMASK_FIELD = "keyMask",
		FILTER_KEYMIN_FIELD = "keyMin";

	private static final int $this = 0;
	/**
	 * Reference to the ByteBuffer
	 */
	private static final int $buf = 1;
	/**
	 * Pointer to the current tag
	 */
	private static final int $pos = 2;
	/**
	 * The current tag value
	 */
	private static final int $tag = 3;
	/**
	 * Pointer to the tag-table
	 */
	private static final int $tagtable_ptr = 4;
	/**
	 * The current tag's value as a global string code
	 */
	private static final int $val_global_string = 5;
	/**
	 * The current tag's value as a local string pointer
	 * (also used to store a pointer to a local key when
	 * checking for key presence)
	 */
	private static final int $val_string_ptr = 6;
	/**
	 * The current tag's value as a String object
	 */
	private static final int $val_string = 7;
	/**
	 * The current tag's value as a double
	 */
	private static final int $val_double = 8;    // takes 2 slots
	/**
	 * A variable to store a copy of $pos, so it can be reset after
	 * checking local-key tags for a negative clause
	 */
	private static final int $saved_pos = 10;
	/**
	 * A variable to store a copy of $val_string_ptr, which is used
	 * (and modified) by the string-matching code
	 */
	private static final int $temp_string_ptr = 11;
	/**
	 * The flag that indicates whether the feature's tag-table
	 * contains local keys (from Bit 0 of the tag-table pointer)
	 */
	private static final int $local_key_flag = 12;
	/**
	 * For "contains" string matching: The next position where
	 * a match will be attempted.
	 */
	private static final int $next_match_ptr = 13;
	/**
	 * For "contains" string matching: The position where no other
	 * match attempts will be performed.
	 */
	private static final int $end_match_ptr = 13;

	private static final int NONE = 0;
	private static final int REQUIRED = 1;
	private static final int ONLY_OPTIONAL = 2;

	private static final int MULTIPLE_TAG_TYPES = -1;

	/**
	 * The code of the global string "no"
	 */
	private final int valueNo;

	/**
	 * The Tag Clause that is currently being encoded.
	 */
	private TagClause clause;
	/**
	 * Where to jump if the current Selector is matched.
	 */
	private Label selector_success;
	/**
	 * Where to jump if the current Selector is NOT matched.
	 */
	private Label selector_failed;
	/**
	 * What kinds of clauses follow the current one: NONE, REQUIRED,
	 * or ONLY_OPTIONAL.
	 */
	private int trailingClauses;
	/**
	 * What type of tag value is required for a value-based match of
	 * the current tag: NARROW_NUMBER (0), GLOBAL_STRING (1), WIDE_NUMBER (2),
	 * LOCAL_STRING (3) -- from TagValues; or MULTIPLE_TAG_TYPES (-1)
	 */
	private int acceptedTagType;

	public FilterCoder(int valueNo)
	{
		this.valueNo = valueNo;
		setTypeChecker(new TypeChecker());
	}

	/**
	 * Writes a debug message (in log4j format, using {} as
	 * parameter placeholder), with one int parameter.
	 *
	 * @param msg      the debug message (e.g. "Pos = {}")
	 * @param localVar the local var to use as a parameter
	 */
	private void debug(String msg, int localVar)
	{
		mv.visitLdcInsn(msg);
		mv.visitVarInsn(ILOAD, localVar);
		mv.visitMethodInsn(INVOKESTATIC, FILTER_BASE_CLASS,
			"debug", "(Ljava/lang/String;I)V", false);
	}

	/**
	 * Emits code to call ByteBuffer.getInt(pos).
	 */
	private void getInt()
	{
		// debug("Pos = {}", $pos);
		mv.visitVarInsn(ALOAD, $buf);    // buf
		mv.visitVarInsn(ILOAD, $pos);    // pos
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/nio/ByteBuffer",
			"getInt", "(I)I", false);
	}


	/**
	 * Emits code to match a UTF-8 string in the ByteBuffer against
	 * a test string. The candidate string must be prefixed with a
	 * varint-encoded length.
	 *
	 * - Either f or t must be non-null (but not both)
	 * - The generated code modifies pointerVar.
	 *
	 * @param op         EQ, NE, STARTS_WITH, ENDS_WITH
	 * @param pointerVar the variable that holds the offset
	 *                   where the candidate string is stored
	 *                   in the ByteBuffer ($buf)
	 * @param match      the comparison string
	 * @param t          where to jump if string is matched
	 * @param f          where to jump if string is NOT matched
	 */

	// TODO: does not work for matching strings of 128 bytes (not chars!) or greater
	//  fixed, but verify

	private void matchString(Operator op, int pointerVar, String match, Label t, Label f)
	{
		// Instead of the "traditional" way of string matching, which requires
		// checking individual characters and looping, we take the test string
		// (which is a literal known at compile time) and break it up into
		// individual "runs" of up to 8 characters. This way, we check a 24-byte
		// candidate using 3 long comparisons, rather than comparing 24 individual
		// bytes. If the string length is not divisible by 8, we use smaller run
		// lengths for the tail: 4, 2, or 1 bytes (int, short or byte).
		// Instead of checking a 3-byte tail using a short and byte comparison,
		// we can also "step back" by 1 byte and use a single int comparison;
		// The same applies to tails with 7, 6 or 5 bytes: if possible, we
		// compare a long value which overlaps with the previous run.

		if (op == Operator.NE)
		{
			// If Operator is NE, we set it to EQ and switch jump targets
			// (STARTS_WITH, ENDS_WITH and IN don't have opposite operators,
			// they are negated using a unary NOT expression, for which
			// the ExpressionCoder already swaps the jump targets)
			op = Operator.EQ;
			Label swap = t;
			t = f;
			f = swap;
		}
		Label fx = f == null ? new Label() : f;
		byte[] matchBytes = match.getBytes(StandardCharsets.UTF_8);
		int len = matchBytes.length;

		// Retrieve the lower-most length byte of the candidate string
		mv.visitVarInsn(ALOAD, $buf);        // buf
		mv.visitVarInsn(ILOAD, pointerVar);    // string ptr
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/nio/ByteBuffer",
			"get", "(I)B", false);

		byte lenByte;
		int remaining = len;
		int n = 0;
		int matched = 0;
		if (op == Operator.EQ)
		{
			// For a full string match, we check if the lower length
			// byte matches; if not, the match fails
			int lenByteToCheck;
			if (len < 128)
			{
				lenByteToCheck = lenByte = (byte) len;
				matched = 1;
			}
			else
			{
				lenByteToCheck = (byte) ((len & 0x7f) | 128);
				lenByte = (byte)(len >> 7);
				remaining++;
				n--;

				// If the string length is 128 or more bytes,
				// we must also match the high-byte of the string
				// (TODO: assumes max string length of 16K,
				//  do we need to check more than 2 length bytes?)
			}
			loadIntConstant(lenByteToCheck);
			mv.visitJumpInsn(IF_ICMPNE, fx);

			// debug("Matched string length, p = {}", pointerVar);
			// TODO: allow comparison with zero-length test string

			if(len != 3 && len != 7)
			{
				// Move pointer to first string char, unless
				// the length is 3 or 7, which means we're going
				// to match an int or long value and are therefore
				// including the length byte
				mv.visitIincInsn(pointerVar, 1);
			}
		}
		else
		{
			lenByte = 0;    // not used for partial matches

			// TODO: partial match of zero-length test string
			// is pointless, it is always a match
			// TODO: This works for string lengths up to 16K bytes
			Label single_byte_length = new Label();
			mv.visitInsn(DUP);
			mv.visitJumpInsn(IFGE, single_byte_length);
			mv.visitIntInsn(BIPUSH, 0x7f);
			mv.visitInsn(IAND);
			mv.visitIincInsn(pointerVar, 1);
			mv.visitVarInsn(ALOAD, $buf);        // buf
			mv.visitVarInsn(ILOAD, pointerVar);    // string ptr
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/nio/ByteBuffer",
				"get", "(I)B", false);
			mv.visitIntInsn(BIPUSH, 7);
			mv.visitInsn(ISHL);
			mv.visitInsn(IOR);
			mv.visitLabel(single_byte_length);

			if (op == FilterParser.ENDS_WITH)
			{
				// for ENDS_WITH, we adjust the string pointer
				// to point at the potential substring of the candidate
				// (We'll do this before the length comparison,
				// because otherwise we would have to store a
				// copy of the candidate length)
				mv.visitInsn(DUP);
					// duplicate the candidate's length
					// (includes one length byte)
				loadIntConstant(len-1);
					// -1 to account for the fact that we haven't
					// skipped the length byte
				mv.visitInsn(ISUB);
				mv.visitVarInsn(ILOAD, pointerVar);
				mv.visitInsn(IADD);
				mv.visitVarInsn(ISTORE, pointerVar);

				// TODO: subtract match_len from candidate_len,
				//  then apply comparision (-1 --> no match possible)
			}
			else
			{
				// Move the pointer to the first character of the
				// candidate string
				mv.visitIincInsn(pointerVar, 1);
			}

			// If the candidate is shorter than the test string,
			// it can't possibly match, so we jump to "false"
			loadIntConstant(len);
			mv.visitJumpInsn(IF_ICMPLT, fx);
		}

		int prevRun = 0;
		for (; ; )
		{
			int run;
			int stepBack = 0;

			switch (Math.min(remaining, 8))
			{
			case 1:
				run = 1;
				break;
			case 2:
				run = 2;
				break;
			case 3:
				if (matched > 0)
				{
					run = 4;
					stepBack = 1;
				}
				else
				{
					run = 2;
				}
				break;
			case 4:
				run = 4;
				break;
			case 5:
				if (matched >= 3)
				{
					run = 8;
					stepBack = 3;
				}
				else
				{
					run = 4;
				}
				break;
			case 6:
				if (matched >= 2)
				{
					run = 8;
					stepBack = 2;
				}
				else
				{
					run = 4;
				}
				break;
			case 7:
				if (matched >= 1)
				{
					run = 8;
					stepBack = 1;
				}
				else
				{
					run = 4;
				}
				break;
			case 8:
				run = 8;
				break;
			default:
				assert false;
				return;
			}

			n -= stepBack;
			long sample = 0;
			for (int i = 0; i < run; i++)
			{
				byte b = (n == -1) ? lenByte : matchBytes[n];
				sample |= (((long) b) & 0xff) << (8 * i);
				n++;
			}

			// extend the sign (This is important for properly matching non-ASCII
			// chars, which have the high-most bit set)
			int shift = 8 * (8-run);
			sample = (sample << shift) >> shift;

			remaining -= run;
			matched += run;

			String method;
			String descriptor;

			switch (run)
			{
			case 1:
				method = "get";
				descriptor = "(I)B";
				break;
			case 2:
				method = "getShort";
				descriptor = "(I)S";
				break;
			case 4:
				method = "getInt";
				descriptor = "(I)I";
				break;
			case 8:
				method = "getLong";
				descriptor = "(I)J";
				break;
			default:
				assert false;
				return;
			}

			if(prevRun > 0)	mv.visitIincInsn(pointerVar, prevRun - stepBack);

			mv.visitVarInsn(ALOAD, $buf);
			mv.visitVarInsn(ILOAD, pointerVar);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/nio/ByteBuffer",
				method, descriptor, false);

			int jumpInsn;
			Label jumpTo;
			if (t != null && remaining <= 0)
			{
				// If we are supposed to branch in case of successful match,
				// do so only if the final run matches
				jumpTo = t;
				jumpInsn = run == 8 ? IFEQ : IF_ICMPEQ;
			}
			else
			{
				// In all other cases, jump to the explicit "false" label
				// (or the point after the instructions) if the run does
				// not match
				jumpTo = fx;
				jumpInsn = run == 8 ? IFNE : IF_ICMPNE;
			}

			// When comparing samples, we need to be mindful of sign extension
			// (Caused a problem if last character of a run was non-ASCII (bit 7 set)

			if (run == 8)
			{
				mv.visitLdcInsn(Long.valueOf(sample));
				mv.visitInsn(LCMP);
			}
			else
			{
				loadIntConstant((int) sample);
			}
			mv.visitJumpInsn(jumpInsn, jumpTo);

			// debug("Matched run, p = {}", pointerVar);

			if (remaining <= 0)
			{
				// debug("String match completed, p = {}", pointerVar);
				if (f == null) mv.visitLabel(fx);
				return;
			}
			prevRun = run;
		}
	}

	/**
	 * Emits code to check if the String object on the operand stack
	 * starts with the given String.
	 *
	 * - Either f or t must be non-null (but not both)
	 *
	 * @param prefix the test string
	 * @param t      where to jump if true
	 * @param f      where to jump if false
	 */
	private void stringStartsWith(String prefix, Label t, Label f)
	{
		mv.visitLdcInsn(prefix);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
			"startsWith", "(Ljava/lang/String;)Z", false);
		mv.visitJumpInsn(t != null ? IFNE : IFEQ, t != null ? t : f);
	}

	/**
	 * Emits code to check if the String object on the operand stack
	 * ends with the given String.
	 *
	 * - Either f or t must be non-null (but not both)
	 *
	 * @param suffix the test string
	 * @param t      where to jump if true
	 * @param f      where to jump if false
	 */
	private void stringEndsWith(String suffix, Label t, Label f)
	{
		mv.visitLdcInsn(suffix);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
			"endsWith", "(Ljava/lang/String;)Z", false);
		mv.visitJumpInsn(t != null ? IFNE : IFEQ, t != null ? t : f);
	}

	/**
	 * Emits code to read and decode a UTF-8 string from the ByteBuffer ($buf).
	 * The resulting String object is placed onto the operand stack.
	 *
	 * @param pointerVar the variable that holds the offset where the
	 *                   string starts
	 */
	private void readString(int pointerVar)
	{
		mv.visitVarInsn(ALOAD, $buf);
		mv.visitVarInsn(ILOAD, pointerVar);
		mv.visitMethodInsn(INVOKESTATIC, BYTES_CLASS, "readString",
			"(Ljava/nio/ByteBuffer;I)Ljava/lang/String;", false);
	}

	/**
	 * Emits code to convert a narrow number (on the stack) to a double.
	 */
	private void narrowNumberToDouble()
	{
		loadIntConstant(TagValues.MIN_NUMBER);
		mv.visitInsn(IADD);
		mv.visitInsn(I2D);
	}

	/**
	 * Emits code to convert a narrow number (on the stack) to a String.
	 */
	private void narrowNumberToString()
	{
		loadIntConstant(TagValues.MIN_NUMBER);
		mv.visitInsn(IADD);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf",
			"(I)Ljava/lang/String;", false);
	}

	/**
	 * Emits code to convert a wide number (on the stack) to a double.
	 */
	private void wideNumberToDouble()
	{
		mv.visitMethodInsn(INVOKESTATIC, TAG_VALUES_CLASS,
			"wideNumberToDouble", "(I)D", false);
	}

	/**
	 * Emits code to convert a wide number (on the stack) to a String.
	 */
	private void wideNumberToString()
	{
		mv.visitMethodInsn(INVOKESTATIC, TAG_VALUES_CLASS,
			"wideNumberToString", "(I)Ljava/lang/String;", false);
	}

	/**
	 * Emits code to convert a double (on the stack) to a String.
	 */
	private void doubleToString()
	{
		mv.visitMethodInsn(INVOKESTATIC, FILTER_BASE_CLASS,
			"doubleToString", "(D)Ljava/lang/String;", false);
	}

	/**
	 * Emits code to convert a String (on the stack) to a double.
	 */
	private void stringToDouble()
	{
		// TODO: could call MathUtils.doubleFromString(s) directly
		mv.visitMethodInsn(INVOKESTATIC, FILTER_BASE_CLASS,
			"stringToDouble", "(Ljava/lang/String;)D", false);
	}

	/**
	 * Emits code to perform a STARTS_WITH, ENDS_WITH, or MATCH operation.
	 * <p>
	 * TODO
	 *
	 * @param op
	 * @param match
	 * @param t
	 * @param f
	 */
	private void matchPatternValue(Operator op, String match, Label t, Label f)
	{
		if (op == Operator.MATCH)
		{
			Label string_valid = new Label();
			mv.visitVarInsn(ALOAD, $val_string);
			mv.visitJumpInsn(IFNONNULL, string_valid);
			readString($val_string_ptr);
			mv.visitVarInsn(ASTORE, $val_string);
			mv.visitVarInsn(ALOAD, $val_string);
			mv.visitLabel(string_valid);
			// TODO
			return;
		}

		// startsWith and endsWith can operate on either a string pointer
		// or a String object

		assert op == FilterParser.STARTS_WITH || op == FilterParser.ENDS_WITH;
		Label use_string = new Label();
		Label done = new Label();
		// First, we check if the string pointer is non-null
		mv.visitVarInsn(ILOAD, $val_string_ptr);
		mv.visitJumpInsn(IFEQ, use_string);
		// If we have a valid string pointer, use fast matching
		matchString(op, $val_string_ptr, match, t, f);
		mv.visitJumpInsn(GOTO, done);
		// Otherwise, a valid String value must have been provided
		mv.visitLabel(use_string);
		mv.visitVarInsn(ALOAD, $val_string);
		// execute one of the basic String methods
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
			op == FilterParser.STARTS_WITH ? "startsWith" : "endsWith",
			"(Ljava/lang/String;)Z", false);
		mv.visitJumpInsn(t != null ? IFNE : IFEQ, t != null ? t : f);
		mv.visitLabel(done);
	}

	/**
	 * Emits code to load the narrow value associated with a local tag.
	 * $pos must be the address of the local tag's key.
	 */
	private void loadLocalTagNarrowValue()
	{
		mv.visitVarInsn(ALOAD, $buf);
		mv.visitVarInsn(ILOAD, $pos);
		// A local tag's value is placed ahead of its key
		mv.visitInsn(ICONST_2);
		mv.visitInsn(ISUB);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/nio/ByteBuffer",
			"getChar", "(I)C", false);
	}

	/**
	 * Emits code to:
	 *
	 * - load the current tag's value (which must be of a given type and size)
	 * - store the value (or its converted equivalents) in $val_global_string,
	 *   $val_string_ptr, or $val_double (according to the needs of the
	 *   tag-clause expression)
	 * - optionally move $pos to the next tag (Cannot transition from
	 *   global-key to local-key tags)
	 *
	 * Prerequisites:
	 *
	 * - $buf the ByteBuffer
	 * - $pos must point to the key of the current tag
	 * - If the current tag is a global-key tag with a narrow value,
	 *   $tag must contain the tag's value in its upper word
	 *
	 * Required fields: clause, trailingClauses
	 *
	 * @param valueType the type of value to fetch:
	 *                  0 = narrow number, 1 = narrow string,
	 *                  2 = wide number, 3 = wide string
	 */
	private void fetchTagValue(int valueType)
	{
		int valuesRequired = clause.flags();
		int valueFulfilled;

		if ((valueType & 2) == 0)	// Narrow type?
		{
			if (clause.isGlobalKey())
			{
				// Load the 16-bit value (which is stored in the upper
				// 16 bits of $tag) onto the stack

				mv.visitVarInsn(ILOAD, $tag);
				mv.visitIntInsn(BIPUSH, 16);
				mv.visitInsn(IUSHR);
			}
			else
			{
				loadLocalTagNarrowValue();
			}
			if (valueType == TagValues.NARROW_NUMBER)
			{
				valueFulfilled = 0;
				if((valuesRequired & TagClause.VALUE_ANY_STRING) != 0)
				{
					if((valuesRequired & TagClause.VALUE_DOUBLE) != 0) mv.visitInsn(DUP);
					narrowNumberToString();
					mv.visitVarInsn(ASTORE, $val_string);
					valueFulfilled |= TagClause.VALUE_ANY_STRING;
				}
				if((valuesRequired & TagClause.VALUE_DOUBLE) != 0)
				{
					narrowNumberToDouble();
					mv.visitVarInsn(DSTORE, $val_double);
					valueFulfilled |= TagClause.VALUE_DOUBLE;
				}

				// TODO: This is stupid, if the value is not actually needed,
				//  it should not be fetched in the first place
				if(valueFulfilled == 0) mv.visitInsn(POP);
			}
			else // can only be GLOBAL_STRING
			{
				valueFulfilled = TagClause.VALUE_GLOBAL_STRING;
				mv.visitVarInsn(ISTORE, $val_global_string);
				if ((valuesRequired & (TagClause.VALUE_ANY_STRING |
					TagClause.VALUE_DOUBLE)) != 0)
				{
					mv.visitVarInsn(ALOAD, $this);
					mv.visitVarInsn(ILOAD, $val_global_string);
					mv.visitMethodInsn(INVOKEVIRTUAL, FILTER_BASE_CLASS,
						"globalString", "(I)Ljava/lang/String;", false);

					if ((valuesRequired & TagClause.VALUE_DOUBLE) != 0)
					{
						mv.visitInsn(DUP);
						stringToDouble();
						mv.visitVarInsn(DSTORE, $val_double);
					}
					mv.visitVarInsn(ASTORE, $val_string);
				}
			}
			if (trailingClauses != NONE)
			{
				mv.visitIincInsn($pos, clause.isGlobalKey() ? 4 : -6);
			}
		}
		else // Wide type
		{
			mv.visitIincInsn($pos, clause.isGlobalKey() ? 2 : -4);
			getInt();
			if (valueType == TagValues.WIDE_NUMBER)
			{
				valueFulfilled = 0;
				if ((valuesRequired & TagClause.VALUE_ANY_STRING) != 0)
				{
					if((valuesRequired & TagClause.VALUE_DOUBLE) != 0) mv.visitInsn(DUP);
					wideNumberToString();
					mv.visitVarInsn(ASTORE, $val_string);
					valueFulfilled |= TagClause.VALUE_ANY_STRING;
				}
				if((valuesRequired & TagClause.VALUE_DOUBLE) != 0)
				{
					wideNumberToDouble();
					mv.visitVarInsn(DSTORE, $val_double);
					valueFulfilled |= TagClause.VALUE_DOUBLE;
				}

				// TODO: This is stupid, if the value is not actually needed,
				//  it should not be fetched in the first place
				if(valueFulfilled == 0) mv.visitInsn(POP);
			}
			else // can only be LOCAL_STRING
			{
				mv.visitVarInsn(ILOAD, $pos);
				mv.visitInsn(IADD);
				mv.visitVarInsn(ISTORE, $val_string_ptr);
				valueFulfilled = TagClause.VALUE_LOCAL_STRING;

				if ((valuesRequired & TagClause.VALUE_DOUBLE) != 0)
				{
					// We need to decode the actual String object
					// and attempt to convert it into a number

					readString($val_string_ptr);
					if ((valuesRequired & TagClause.VALUE_ANY_STRING) != 0)
					{
						// If the expression requires the actual String
						// object as well, store it now

						mv.visitInsn(DUP);
						mv.visitVarInsn(ASTORE, $val_string);
					}
					stringToDouble();
					mv.visitVarInsn(DSTORE, $val_double);
				}
				else if ((valuesRequired & TagClause.VALUE_ANY_STRING) != 0)
				{
					// Unless we've already decoded the local string
					// as a String object, we'll let the expression term
					// that needs the String perform this step; we'll
					// set $val_string to null to signal this

					mv.visitInsn(ACONST_NULL);
					mv.visitVarInsn(ASTORE, $val_string);
				}

			}
			if (trailingClauses != NONE)
			{
				mv.visitIincInsn($pos, clause.isGlobalKey() ? 4 : -4);
			}
		}

		valuesRequired &= ~valueFulfilled;
		if ((valuesRequired & TagClause.VALUE_GLOBAL_STRING) != 0)
		{
			// set global string code to 0
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE, $val_global_string);
		}
		if ((valuesRequired & TagClause.VALUE_LOCAL_STRING) != 0)
		{
			// set local string pointer to 0
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ISTORE, $val_string_ptr);
		}

		/*
		// TODO: create numeric strings based on type to preserve
		//  exact string representation
		if (valueFulfilled == TagClause.VALUE_DOUBLE &&
			(valuesRequired & TagClause.VALUE_ANY_STRING) != 0)
		{
			mv.visitVarInsn(DLOAD, $val_double);
			doubleToString();
			mv.visitVarInsn(ASTORE, $val_string);
		}
		 */
	}

	/**
	 * Emits instructions to test the value expression of the current
	 * Tag Clause. If the tag value does not match the clause expression,
	 * the code branches to selector_failed.
	 *
	 * $tag must contain the current tag (key/value combo for global-key-with-
	 * narrow-value tags, key only for local-key tags)
	 *
	 * Required fields: acceptedTagType, selector_failed, clause, trailingClauses
	 */

	// TODO: Resolve: can a number equate to a string?
	// Would be useful for strict checks:
	// maxspeed="35 mph": [maxspeed=35] -> true
	// addr:housenumber="12A": [addr:housenumber="12"] -> false
	// Would need custom number parsing that stops at first non-number char
	private void checkExpression()
	{
		if (acceptedTagType != MULTIPLE_TAG_TYPES)
		{
			fetchTagValue(acceptedTagType);
		}
		else
		{
			// Multiple types are accepted by the tag clause expression

			Label check_local_string = new Label();
			Label check_narrow_number = new Label();
			Label check_wide_number = new Label();
			Label evaluate_exp = new Label();

			// Branch based on the type of value the tag has
			// (We could use a tableswitch instruction here, but probably
			// not worth it for just 4 cases)

			mv.visitVarInsn(ILOAD, $tag);
			mv.visitInsn(ICONST_3);
			mv.visitInsn(IAND);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitJumpInsn(IF_ICMPNE, check_local_string);

			// === Narrow String === //

			mv.visitInsn(POP);
			fetchTagValue(TagValues.GLOBAL_STRING);
			mv.visitJumpInsn(GOTO, evaluate_exp);

			mv.visitLabel(check_local_string);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_3);
			mv.visitJumpInsn(IF_ICMPNE, check_narrow_number);

			// === Wide String === //

			mv.visitInsn(POP);
			fetchTagValue(TagValues.LOCAL_STRING);
			mv.visitJumpInsn(GOTO, evaluate_exp);

			mv.visitLabel(check_narrow_number);
			mv.visitJumpInsn(IFNE, check_wide_number);

			// === Narrow Number === //

			fetchTagValue(TagValues.NARROW_NUMBER);
			mv.visitJumpInsn(GOTO, evaluate_exp);

			// === Wide Number === //

			mv.visitLabel(check_wide_number);
			fetchTagValue(TagValues.WIDE_NUMBER);

			mv.visitLabel(evaluate_exp);
		}
		logicalExpression(clause.expression(), null, selector_failed);
	}

	/**
	 * Writes instructions to advance the tag pointer ($pos) to the next tag
	 * with a global key. To determine whether to advance by 4 or 6 bytes,
	 * we use the size flag from the current tag ($tag).
	 */
	private void nextGlobalKeyTag()
	{
		mv.visitVarInsn(ILOAD, $pos);
		mv.visitInsn(ICONST_4);
		mv.visitVarInsn(ILOAD, $tag);
		mv.visitInsn(ICONST_2);            // mask of wide-value flag
		mv.visitInsn(IAND);                // isolate flag
		mv.visitInsn(IADD);                // add it to 4
		mv.visitInsn(IADD);                // add 4 or 6 to pos
		mv.visitVarInsn(ISTORE, $pos);     // store pos of next tag
	}

	/**
	 * Writes instructions to advance the tag pointer ($pos) to the next tag
	 * with a local key. To determine whether to advance by 4 or 6 bytes,
	 * we use the size flag from the current tag ($tag)
	 */
	private void nextLocalKeyTag()
	{
		mv.visitVarInsn(ILOAD, $pos);
		mv.visitIntInsn(BIPUSH, 6);
		mv.visitVarInsn(ILOAD, $tag);
		mv.visitInsn(ICONST_2);            // mask of wide-value flag
		mv.visitInsn(IAND);                // isolate flag
		mv.visitInsn(IADD);                // add it to 4
		mv.visitInsn(ISUB);                // subtract 6 or 8 from pos
		mv.visitVarInsn(ISTORE, $pos);     // store pos of next tag
	}

	/**
	 * Emits code to check if a local key exists in the Tag Table.
	 *
	 * - $pos must point to the first local key to check
	 * - If key is required but is not found, code branches to selector_failed
	 * - If key is optional and is not found, code branches to matched_clause_no_key
	 *   (If more clauses follow, $pos will be reset)
	 * - If key is found, $pos will point to it and code continues
	 * - $tag will be set to the key's value (of interest here are the lower 3
	 *   bits, which contain the type, size and last-item flags)
	 *
	 * Required fields: clause, selector_failed
	 *
	 * @param matched_clause_no_key
	 */
	private void scanLocalKeys(Label matched_clause_no_key)
	{
		// Local (uncommon) keys are sorted in alphabetical order, so we
		// would in theory only need to scan half of them (on average) to
		// check for a match. However, this would require comparing keys
		// character by character, instead of the much faster check for
		// identity. Further, there are usually only one or two local keys
		// (if any). Therefore, we run a simple check of all local keys
		// in the TagTable.

		// If this clause matches if the key (or key-value combo) is NOT
		// present, and other clauses follow, we stash a copy of the pointer
		// to the current tag so we can reset it afterwards

		if (clause.next() != null && !clause.isKeyRequired())
		{
			mv.visitVarInsn(ILOAD, $pos);
			mv.visitVarInsn(ISTORE, $saved_pos);
		}

		Label check_next_tag = new Label();
		Label matched_key = new Label();
		mv.visitLabel(check_next_tag);    // the loop returns to this point
		// Retrieve relative tagged pointer
		getInt();                             // ⯇─────────────────╮
		mv.visitInsn(DUP);				// Store a copy of the tagged pointer,
		mv.visitVarInsn(ISTORE, $tag);	// since we will need the flag bits later
		mv.visitIntInsn(BIPUSH, 0xfff8); // Clear the 3 lower bits
		mv.visitInsn(IAND);						// (type, size, and last-tag flags)
		mv.visitInsn(ICONST_1);			// right-shift by 1 bit, preserving sign
		mv.visitInsn(ISHR);
		// Now we have the relative pointer to the key string; this pointer
		// is relative to the 4-byte aligned tag table pointer
		mv.visitVarInsn(ILOAD, $tagtable_ptr);
		mv.visitIntInsn(BIPUSH, 0xfffc);
		mv.visitInsn(IAND);
		mv.visitInsn(IADD);
		// Store the resulting pointer to the local key string
		// TODO: use temp_string_ptr instead for consistency?
		mv.visitVarInsn(ISTORE, $val_string_ptr);
		// Match the key string, branch to matched_key if successful
		matchString(Operator.EQ, $val_string_ptr, clause.name(),
			matched_key, null);   // ─────⯈─────╮
		// Key does not match:
		mv.visitVarInsn(ILOAD, $tag);
		// Check bit #2 (last-tag flag)
		mv.visitInsn(ICONST_4);
		mv.visitInsn(IAND);
		// If we've reached the end of the table without finding the key:
		// - If key is required, the clause (and hence the selector) fails
		// - If key is NOT required, the clause succeeds
		if(clause.isKeyRequired())
		{
			mv.visitJumpInsn(IFNE, selector_failed); // ───⯈╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
			// If not at end, advance $pos to the next local-key tag
			nextLocalKeyTag();
			mv.visitJumpInsn(GOTO, check_next_tag); // ──────⯈─────╯
		}
		else // optional-key clause
		{
			if (clause.next() != null)
			{
				Label move_to_next_tag = new Label();
				mv.visitJumpInsn(IFEQ, move_to_next_tag); // ─⯈─╮
				mv.visitVarInsn(ILOAD, $saved_pos);		// reset pointer
				mv.visitVarInsn(ISTORE, $pos);			// to previous position
				mv.visitJumpInsn(GOTO, matched_clause_no_key); // ───────────⯈╌╌
				mv.visitLabel(move_to_next_tag);
				nextLocalKeyTag();						// ⯇────╯
				mv.visitJumpInsn(GOTO, check_next_tag); // ────⯈───╯
			}
			else
			{
				mv.visitJumpInsn(IFNE, matched_clause_no_key); // ───────────⯈╌╌
				nextLocalKeyTag();
				mv.visitJumpInsn(GOTO, check_next_tag); // ────⯈───╯
			}

		}

		// Control flow arrives here if a key match has been found:
		mv.visitLabel(matched_key);  // ⯇─────────╯
	}

	/**
	 * Emits code to check if a global key exists in the Tag Table.
	 *
	 * - $pos must point to the first global key to check
	 * - Unlike the search for local keys, the code generated by this method
	 *   continues until a key value equal to or greater than the clause key
	 *   is found; it is up to the caller to examine $tag
	 * - $tag is set to the key value: The global key along with the size, type
	 *   and last-item flags is stored in the lower 16 bits o $tag, and the
	 *   lower word of the tag value in the upper 16 bits (We speculatively
	 *   retrieve an int value instead of a char value, since most values
	 *   examined by queries are narrow, i.e. a global string or a small number)
	 *
	 *   Required fields: clause
	 */
	private void scanGlobalKeys()
	{
		int key = clause.keyCode();
		Label check_next_tag = new Label();
		Label matched_key_or_end = new Label();

		mv.visitLabel(check_next_tag);  // the loop returns to this point
		getInt();                       // ⯇────────────╮
		mv.visitVarInsn(ISTORE, $tag);  // store tag
		mv.visitVarInsn(ILOAD, $tag);   // load it again
		mv.visitInsn(I2C);              // isolate the lower word (key)
		loadIntConstant(key << 2); // compare it to the predicate key

		// If the tag's key is greater or equal, we either have a match,
		// or the table does not contain it

		mv.visitJumpInsn(IF_ICMPGE, matched_key_or_end); // ───⯈───╮

		// If the current tag in the tagtable is less than the key
		// we are looking for, keep scanning

		nextGlobalKeyTag();
		mv.visitJumpInsn(GOTO, check_next_tag);  // ──⯈─╯

		mv.visitLabel(matched_key_or_end);  // ⯇───────────────────╯
	}

	/**
	 * If the clause is a simple [k=v] where both k and v are global
	 * strings (a very common case), this method emits code to perform
	 * a fast and simple check based on $tag, which must contain the
	 * global key, flags, and global string value. The generated code
	 * branches to selector_failed if the string value does not match;
	 * otherwise, it continues ($pos is not changed).
	 *
	 * Required fields: clause, trailingClauses, selector_failed
	 *
	 * @return true if fast-match code has been generated, otherwise false
	 */
	private boolean matchGlobalKeyGlobalStringValue()
	{
		Expression exp = clause.expression();
		if(!(exp instanceof BinaryExpression)) return false;
		BinaryExpression binary = (BinaryExpression) exp;
		if (binary.operator() != Operator.EQ) return false;
		Object val = ((Literal) binary.right()).value();
		if (!(val instanceof GlobalString)) return false;

		int valueCode = ((GlobalString) val).value();
		if (trailingClauses != REQUIRED)
		{
			// If the clause is the last one, or it is followed only by optional
			// clauses (true if key is NOT present), we need to mask off the
			// last-tag flag

			loadIntConstant(0xffff_7fff); // mask excluding last-tag flag
			mv.visitInsn(IAND);                // mask out the flag
		}
		// key, value and type flag (01 = common string)
		loadIntConstant((clause.keyCode() << 2) | (valueCode << 16) | 1);
		mv.visitJumpInsn(IF_ICMPNE, selector_failed);	// ──────⯈╌╌╌╌╌╌╌╌╌╌╌╌╌
		return true;
	}

	/**
	 * If the current clause is followed only by optional clauses, this method
	 * emits code to check if we've reached the end of the tag table, in which
	 * case we jump to selector_success.
	 *
	 * Required fields: trailingClauses, selector_success
	 */
	private void skipOptionalClauses()
	{
		if(trailingClauses == ONLY_OPTIONAL)
		{
			// If all remaining clauses are optional, we check for the
			// last-item flag; if it is set, our work is done, the entire
			// Selector has been successfully matched
			// (This is also important to avoid a buffer overrun)

			// TODO: This is wrong, because it does not consider that
			//  optional global clauses could be followed by local clauses
			//  The currently produced code fails to consider these
			//  But is this true? Test case for skipOptionalClauses
			//  is passing

			mv.visitVarInsn(ILOAD, $tag);
			loadIntConstant(clause.isLocalKey() ? 4 : 0x8000);
			mv.visitInsn(IAND);
			mv.visitJumpInsn(IFNE, selector_success);
		}
	}


	/**
	 * Emits code to match a Tag Clause.
	 *
	 * For a required-key clause to succeed:
	 *
	 * - Its key must be found
	 * - If the clause is followed by other required-key clauses, the tag
	 *   cannot be the last (otherwise, the other clauses will fail)
	 * - If the clause only accepts a single value type, and this type is
	 *   global string or local string, the tag's value must be of this type
	 *   (see exception below for explicitly required keys)
	 * - If the key is explicitly required (but not implicitly), its value
	 *   cannot be "no" (E.g. [bridge] or [bridge][bridge!=retractable]);
	 *   if only accepted value is local string, the clause implicitly accepts
	 *   global string as well (since "no" is always a global string), and
	 *   we cannot fail early
	 * - The clause expression (if any) must be true
	 *
	 * An optional-key clause succeeds if:
	 *
	 * - Its key is not found; OR
	 * - If the clause has an expression:
	 * 		- If it only accepts a single value type, AND this type is global
	 * 	 	  string or local string, AND the tag's value is of another type; OR
	 * 	 	- The expression is true
	 * - If it has no expression:
	 * 		- the tag's value type is not global string; OR
	 *      - the tag's value is global string "no"
	 *
	 * However, an optional-key clause fails in any case if it is followed by
	 * required-key clause, and the tag is the last (which means the required-key
	 * clause has no chance of matching).
	 *
	 * If the clause does not match, the code branches to selector_failed
	 * Otherwise:
	 * - If the clause is followed only by optional-key clauses (of the same
	 *   type), and this tag is the last, the code branches to selector_success
	 * Otherwise, the code continues. If there are further clauses of the same
	 * type, $pos is moved to the next key in the table.
	 */
	private void matchClause()
	{
		Label matched_clause = new Label();
		Label matched_clause_no_key = new Label();

		// Determine the current clause's trailing clauses
		// of the same local/global type

		boolean isLocalKey = clause.isLocalKey();
		TagClause sibling = clause.next();
		trailingClauses = NONE;
		while (sibling != null)
		{
			if (sibling.isLocalKey() && !isLocalKey) break;
			trailingClauses = ONLY_OPTIONAL;
			if (sibling.isKeyRequired())
			{
				trailingClauses = REQUIRED;
				break;
			}
			sibling = sibling.next();
		}

		boolean failIfLast = trailingClauses == REQUIRED;

		// Let's see if we can match or fail based solely on the type of the
		// tag value. If the clause accepts a) only global strings or b) only
		// local strings, a required-key clause will fail (and an optional-key
		// clause will succeed) if the tag value is of any other type
		// If a key is explicitly required, but not implicitly, we'll also
		// have to examine if the value is "no" (always a global string)

		boolean requiredExplicitlyOnly =
			((clause.flags() & (TagClause.KEY_REQUIRED_EXPLICITLY |
				TagClause.KEY_REQUIRED_IMPLICITLY)) ==
				TagClause.KEY_REQUIRED_EXPLICITLY);
			// Match clause if key present unless it has certain value(s)

		boolean failIfWrongType;
		boolean matchedIfWrongType;

		if(requiredExplicitlyOnly ||
			(!clause.isKeyRequired() && clause.expression() != null))
		{
			// [k], [k!=v] and [k][k!=v] succeed if wrong type
			matchedIfWrongType = true;
			failIfWrongType = false;
		}
		else
		{
			// [!k], [k=v] and [k][k=v] fail if wrong type
			matchedIfWrongType = false;
			failIfWrongType = true;
		}

		int clauseValueTypes = clause.flags() & TagClause.VALUE_ANY;
		/*
		if(requiredExplicitlyOnly || clause.expression() == null)
		{
			// [k], [!]k and [k][k!=v] require "no" check, therefore
			// need to look at global string value
			clauseValueTypes |= TagClause.VALUE_GLOBAL_STRING;
		}
		*/
		if(clauseValueTypes == TagClause.VALUE_GLOBAL_STRING)
		{
			acceptedTagType = TagValues.GLOBAL_STRING;
		}
		else if(clauseValueTypes == TagClause.VALUE_LOCAL_STRING)
		{
			acceptedTagType = TagValues.LOCAL_STRING;
		}
		else
		{
			acceptedTagType = MULTIPLE_TAG_TYPES;
			matchedIfWrongType = false;
			failIfWrongType = false;
		}

		boolean advancePos = trailingClauses != NONE;
		int advanceBy = 0;


		if(isLocalKey)
		{
			scanLocalKeys(matched_clause_no_key);

			// At this point in the generated code, we've found the local key
		}
		else
		{
			scanGlobalKeys();
			mv.visitVarInsn(ILOAD, $tag);
			if(matchGlobalKeyGlobalStringValue())
			{
				// We can do simple & fast [ns=ns] check

				skipOptionalClauses();
				if(trailingClauses != NONE) mv.visitIincInsn($pos, 4);
				return;
			}

			// Otherwise, check key and possibly type. For required-key
			// clauses, we can simultaneously perform a check for the
			// last-entry flag, as well

			int key = clause.keyCode();
			if(clause.isKeyRequired())
			{
				int checkBits = key << 2;
				int maskBits = 0x7ffc;
				if(failIfLast)
				{
					maskBits |= 0x8000;
					failIfLast = false;
				}
				if(failIfWrongType)
				{
					maskBits |= 3;
					checkBits |= acceptedTagType;
					failIfWrongType = false;
				}
				if(maskBits == 0xffff)
				{
					mv.visitInsn(I2C);	// convert to char is same as AND 0xffff
				}
				else
				{
					loadIntConstant(maskBits);
					mv.visitInsn(IAND);
				}
				loadIntConstant(checkBits);
				mv.visitJumpInsn(IF_ICMPNE, selector_failed); // ──────⯈╌╌╌╌╌╌╌
			}
			else
			{
				// If we don't match key, optional clause succeeds, but we don't advance $pos
				loadIntConstant(0x7ffc);
				mv.visitInsn(IAND);
				loadIntConstant(key << 2);
				mv.visitJumpInsn(IF_ICMPNE, matched_clause_no_key); // ──⯈──╮
			}

			if(clause.expression() == null)
			{
				// [k] or [!k]: Check for "no"
				mv.visitVarInsn(ILOAD, $tag);
				if(trailingClauses != REQUIRED)
				{
					loadIntConstant(0xffff_7fff);
					mv.visitInsn(IAND);
				}
				loadIntConstant((key << 2) | 1 | (valueNo << 16));
				if(clause.isKeyRequired())
				{
					// [k] fails of value = "no"
					mv.visitJumpInsn(IF_ICMPEQ, selector_failed); // ───⯈╌╌╌╌╌╌
					matchedIfWrongType = false;
				}
				else
				{
					// [!k] fails UNLESS value = "no"
					mv.visitJumpInsn(IF_ICMPNE, selector_failed); // ───⯈╌╌╌╌╌╌
					// At this point, we don't have to check the last-entry
					// and type flags, since we've done so implicitly
					// Since the only possible matching value is value="no",
					// the advanceBy value can only be 4
					failIfLast = false;
					failIfWrongType = false;
					advanceBy = 4;
				}
			}
		}

		if(failIfWrongType)
		{
			int maskBits = 3;
			if(failIfLast)
			{
				maskBits |= isLocalKey ? 4 : 0x8000;
					// TODO: should not process global keys here, so can only be 4
				failIfLast = false;
			}
			mv.visitVarInsn(ILOAD, $tag);
			loadIntConstant(maskBits);
			mv.visitInsn(IAND);
			loadIntConstant(acceptedTagType);
			mv.visitJumpInsn(IF_ICMPNE, selector_failed); // ───⯈╌╌╌╌╌╌╌╌╌╌╌╌╌╌
		}
		if(failIfLast)
		{
			// If the clause fails if the matched tag is at end of table,
			// and we haven't encoded a check yet, generate it now:
			// Isolate the flag (Bit 2 for local, bit 15 for global),
			// and fail the selector if the flag is set

			mv.visitVarInsn(ILOAD, $tag);
			loadIntConstant(isLocalKey ? 4 : 0x8000);
			mv.visitInsn(IAND);
			mv.visitJumpInsn(IFNE, selector_failed); // ───⯈╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
		}

		if(matchedIfWrongType)
		{
			mv.visitVarInsn(ILOAD, $tag);
			mv.visitInsn(ICONST_3);
			mv.visitInsn(IAND);
			loadIntConstant(acceptedTagType);
			mv.visitJumpInsn(IF_ICMPNE, matched_clause); // ──⯈──╮
		}

		if(clause.expression() != null)
		{
			// Clauses other than [k] or [!k]
			checkExpression();                           // ───⯈╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
			if (requiredExplicitlyOnly)
			{
				// [k][k!=v] fails if value is "no"
				mv.visitVarInsn(ILOAD, $val_global_string);
				loadIntConstant(valueNo);
				mv.visitJumpInsn(IF_ICMPEQ, selector_failed); // ───⯈╌╌╌╌╌╌╌╌╌╌
			}
			advancePos = false; // because checkExpression already did it
		}
		else
		{
			if(isLocalKey)
			{
				// For local-key [k] and [!k], check for "no" here

				// TODO: check???

				loadLocalTagNarrowValue();
				loadIntConstant(valueNo);
				if(clause.isKeyRequired())
				{
					mv.visitJumpInsn(IF_ICMPEQ, selector_failed); // ───⯈╌╌╌╌╌╌
				}
				else
				{
					mv.visitJumpInsn(IF_ICMPNE, selector_failed); // ───⯈╌╌╌╌╌╌
					advanceBy = -6;
				}
			}
		}

		mv.visitLabel(matched_clause); // ⯇──────────────────────╯
		skipOptionalClauses();
		if(advancePos)
		{
			// If more clauses follow, we need to advance $pos

			// TODO: wrong, might have advanced already in checkExpression

			if(advanceBy != 0)
			{
				mv.visitIincInsn($pos, advanceBy);
			}
			else
			{
				if (isLocalKey)
				{
					nextLocalKeyTag();
				}
				else
				{
					nextGlobalKeyTag();
				}
			}
		}

		// We jump here if the clause matched even if we didn't find a tag
		// [!k] or [k!=v]
		mv.visitLabel(matched_clause_no_key); // ⯇──────────────────────────╯
	}

	/**
	 * Writes code to execute a single Selector. The following local
	 * variables must be initialized:
	 *
	 * $buf    			ByteBuffer
	 * $pos    			tag-table pointer
	 * $tagtable_ptr	a copy of the tag-table pointer (only needed
	 * 					if the selector contains a mix of local and
	 * 					global keys)
	 * $local_key_flag	flag indicating whether the tag-table contains
	 * 					tags with local keys (only needed if
	 * 					checkLocalKeyFlag is true)
	 *
	 * @param sel               the Selector
	 * @param checkLocalKeyFlag if true, code should be generated to perform
	 *                          a fast check based on $local_key_flag if the
	 *                          Selector has certain local-key clauses
	 */
	private void selector(Selector sel, boolean checkLocalKeyFlag)
	{
		selector_success = new Label();
		selector_failed = new Label();

		int ct = sel.clauseTypes();

		if (checkLocalKeyFlag)
		{
			if (ct == Selector.CLAUSE_LOCAL_OPTIONAL)
			{
				// If this Selector *only* has optional-local-key clauses,
				// it will always succeed if the local-key flag is false
				// (in this case, the whose Matcher is successful)

				mv.visitVarInsn(ILOAD, $local_key_flag);
				mv.visitJumpInsn(IFEQ, selector_success);
			}
			else if ((ct & Selector.CLAUSE_LOCAL_REQUIRED) != 0)
			{
				// If this Selector has *any* required-local-key clauses,
				// it will always fail if the local-key flag is false
				// (In this case, we skip the Selector)

				mv.visitVarInsn(ILOAD, $local_key_flag);
				mv.visitJumpInsn(IFEQ, selector_failed);
			}
		}

		// Generate matcher code for global-key clauses

		clause = sel.firstClause();
		if(clause != null && clause.isGlobalKey())
		{
			while (clause != null)
			{
				if (clause.isLocalKey()) break;
				matchClause();
				clause = clause.next();
			}
			if (clause != null)
			{
				// local-key clauses follow

				// If we hit the end of the global-key section of the tag-table
				// while checking a clause, and only optional clauses followed,
				// execution jumps to this intermediate success-point

				mv.visitLabel(selector_success);
				selector_success = new Label();

				if ((ct & (Selector.CLAUSE_LOCAL_REQUIRED
					| Selector.CLAUSE_LOCAL_OPTIONAL))
					== Selector.CLAUSE_LOCAL_OPTIONAL)
				{
					// If the Selector has only optional local-key clauses,
					// check if there are no local keys present; if so,
					// the Selector succeeds at this point

					mv.visitVarInsn(ILOAD, $local_key_flag);
					mv.visitJumpInsn(IFEQ, selector_success);
				}

				// set $pos to the first local key
				mv.visitVarInsn(ILOAD, $tagtable_ptr);
				mv.visitInsn(ICONST_4);
				mv.visitInsn(ISUB);
				mv.visitVarInsn(ISTORE, $pos);
			}
		}
		else
		{
			// If the selector only checks uncommon keys,
			// move $pos to the first uncommon key,
			// stored directly ahead of the table start
			mv.visitIincInsn($pos, -4);
		}

		while(clause != null)
		{
			matchClause();
			clause = clause.next();
		}

		// matched all clauses, return true
		mv.visitLabel(selector_success);
		mv.visitInsn(ICONST_1);
		mv.visitInsn(IRETURN);

		// common exit point when we've failed to match a clause
		mv.visitLabel(selector_failed);
	}

	private void createConstructor(int keyMask, int keyMin)
	{
		beginConstructor("([Ljava/lang/String;)V");
		mv.visitVarInsn(ALOAD, $this);
		mv.visitVarInsn(ALOAD, 1); // first argument: String[]
		loadIntConstant(keyMask);
		loadIntConstant(keyMin);
		callBaseClassConstructor("([Ljava/lang/String;II)V");
		mv.visitInsn(RETURN);
		// force auto-calculation of maxStack and maxLocals
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private void createAcceptMethod(String methodName, Selector first)
	{
		Selector sel;

		// The local-keys flag gives us a shortcut to determine if certain
		// Selectors will succeed or fail, without having to scan the tags.
		//
		// A) If a Selector contains *only* optional-local-key clauses,
		//    (e.g. [!uncommon_key] or [uncommon_key!=value]), the
		//    Selector always SUCCEEDS if the local-keys flag is false.
		//
		// B) If a Selector contains *any* required-local-key clauses
		//    (e.g. [common][uncommon_key] or [uncommon_key=value]), the
		//    Selector always FAILS if the local-keys flag is false.
		//
		// If the Query has multiple Selectors:
		//
		// - If either A or B applies to all, we can perform the local-key
		//   flag check upfront
		// - If A or B applies to none, we ignore the local-key flag
		// - Otherwise, we need to check the local-key flag for each
		//   Selector for which A or B applies

		boolean anyLocalKeys = false;
		boolean mustCheckLocalsFlag = false;
		boolean queryTrueIfNoLocals = true;
		boolean queryFalseIfNoLocals = true;

		sel = first;
		while (sel != null)
		{
			int ct = sel.clauseTypes();
			if ((ct & (Selector.CLAUSE_LOCAL_OPTIONAL |
				Selector.CLAUSE_LOCAL_REQUIRED)) != 0)
			{
				anyLocalKeys = true;
				mustCheckLocalsFlag = true;
			}
			if (ct != Selector.CLAUSE_LOCAL_OPTIONAL)
			{
				queryTrueIfNoLocals = false;
			}
			if ((ct & Selector.CLAUSE_LOCAL_REQUIRED) == 0)
			{
				queryFalseIfNoLocals = false;
			}
			sel = sel.next();
		}

		mv = cw.visitMethod(ACC_PUBLIC, methodName, "(Ljava/nio/ByteBuffer;I)Z", null, null);
		// Advance the feature pointer to the tagtable pointer
		mv.visitIincInsn($pos, 8);
		// Load the relative tag-table pointer
		getInt();

		if (mustCheckLocalsFlag)
		{
			// Isolate the local-keys flag from the tagged pointer
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitInsn(IAND);
		}
		if (queryTrueIfNoLocals || queryFalseIfNoLocals)
		{
			// If all selectors have *only* optional-local-key clauses
			// (e.g. [!uncommon_key] or [uncommon_key!=value]), the
			// Matcher always SUCCEEDS if the local-keys flag is false.
			// If all selectors have *any* required-local-key clauses
			// (e.g. [common][uncommon] or [uncommon=value]), the
			// Matcher always FAILS if the local-keys flag is false.

			Label must_check_selectors = new Label();
			// The local-keys flag is on the stack here
			// If the flag is set, we have to do a full tag-table check
			mv.visitJumpInsn(IFNE, must_check_selectors);
			// Otherwise, return true or false
			mv.visitInsn(POP);        // remove pointer from stack
			mv.visitInsn(queryTrueIfNoLocals ? ICONST_1 : ICONST_0);
			mv.visitInsn(IRETURN);
			mv.visitLabel(must_check_selectors);
			// Since we're checking the local-keys flag upfront,
			// we don't have to check it for each individual Selector
			mustCheckLocalsFlag = false;
		}
		if (mustCheckLocalsFlag)
		{
			// If we must check the local-key flag before evaluating
			// each Selector, we must store the flag
			mv.visitIntInsn(ISTORE, $local_key_flag);
		}

		// At this point, the tagged tag-table pointer is on the stack
		// Mask off the uncommon-keys flag and get the absolute pointer
		loadIntConstant(0xffff_fffe);
		mv.visitInsn(IAND);
		mv.visitVarInsn(ILOAD, $pos);
		mv.visitInsn(IADD);

		if (anyLocalKeys || first.next() != null)
		{
			// If there are multiple selectors, or we need to check
			// uncommon keys, stash the tag-table pointer for re-use
			// mv.visitInsn(DUP);
			mv.visitVarInsn(ISTORE, $tagtable_ptr);
			mv.visitVarInsn(ILOAD, $tagtable_ptr);
		}
		mv.visitVarInsn(ISTORE, $pos);

		sel = first;
		for (; ; )
		{
			selector(sel, mustCheckLocalsFlag);
			sel = sel.next();
			if (sel == null) break;
			// If there are more selectors, reset pos to start of tagtable
			mv.visitVarInsn(ILOAD, $tagtable_ptr);
			mv.visitVarInsn(ISTORE, $pos);
		}

		// None of the selectors matched: return false
		mv.visitInsn(ICONST_0);
		mv.visitInsn(IRETURN);

		// force auto-calculation of maxStack and maxLocals
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	public byte[] createFilterClass(String className, Selector first)
	{
		// TODO
		int keyMask = first.indexBits();
		beginClass(className, FILTER_BASE_CLASS, null);
		createConstructor(keyMask, keyMask);	// TODO: keyMin
		createAcceptMethod("accept", first);
		endClass();
		return cw.toByteArray();
	}

	@Override public Void visitLiteral(Literal exp)
	{
		Object v = exp.value();
		if (v instanceof GlobalString)
		{
			loadIntConstant(((GlobalString) v).value());
			return null;
		}
		return super.visitLiteral(exp);
	}

	@Override protected void evaluate(Expression exp, Class<?> type)
	{
		if (exp instanceof Variable)
		{
			if (type == Double.TYPE || type == Double.class)
			{
				mv.visitVarInsn(DLOAD, $val_double);
				return;
			}
			if (type == GlobalString.class)
			{
				mv.visitVarInsn(ILOAD, $val_global_string);
				return;
			}
		}
		exp.accept(this);
	}

	/**
	 * If the provided BinaryExpression represents a comparison against a
	 * string value (NOT a GlobalString, which is represented by a number),
	 * emits string-matching code.
	 *
	 * One of the labels (but not both) must be non-zero.
	 *
	 * @param exp the expression
	 * @param t		jump target if true
	 * @param f		jump target if false
	 * @return true if string-matching code has been emitted, otherwise false
	 */
	private boolean matchStringValue(BinaryExpression exp, Label t, Label f)
	{
		// TODO: regex pattern matching
		// TODO: contained string matching

		Expression right = exp.right();
		if (!(right instanceof Literal)) return false;
		Literal literal = (Literal) right;
		Object val = literal.value();
		if (!(val instanceof String)) return false;
		String matchString = (String) val;
		Operator op = exp.operator();
		// TagClause clause = (TagClause)exp.left();
		if (op == Operator.NE)
		{
			// Turn != into = (and swap the labels)
			op = Operator.EQ;
			Label swap = t;
			t = f;
			f = swap;
		}
		/*		// TODO: enable for regex
		else if (op == Operator.NOT_MATCH)
		{
			// Turn !~ into ~ (and swap the labels)
			op = Operator.MATCH;
			Label swap = t;
			t = f;
			f = swap;
		}
		 */

		if(matchString.isEmpty())
		{
			// "*" (wildcard with empty string) matches anything
			if(op == Operator.IN || op == FilterParser.STARTS_WITH || op == FilterParser.ENDS_WITH)
			{
				if(t != null)
				{
					mv.visitJumpInsn(GOTO, t);
				}
				return true;
			}
		}

		assert op == Operator.EQ || op == FilterParser.STARTS_WITH ||
			op == FilterParser.ENDS_WITH;

		Label done = new Label();
		Label fx = f != null ? f : done;

		mv.visitVarInsn(ILOAD, $val_string_ptr);
		if (op != Operator.EQ)
		{
			// STARTS_WITH and ENDS_WITH can use string pointer or String object

			Label use_string_ptr = new Label();
			mv.visitJumpInsn(IFNE, use_string_ptr);
			// If the string pointer value is zero, this means the tag value
			// is not a wide string value. Narrow string and numeric values
			// are always turned into a String representation if the tag-clause
			// expression contains pattern-matching operations
			// In this case, we use the basic startsWith or endsWith methods
			// of the String class
			mv.visitVarInsn(ALOAD, $val_string);
			mv.visitLdcInsn(matchString);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
				op == FilterParser.STARTS_WITH ? "startsWith" : "endsWith",
				"(Ljava/lang/String;)Z", false);
			if (t != null)
			{
				mv.visitJumpInsn(IFNE, t);  // jump if true
			}
			else
			{
				mv.visitJumpInsn(IFEQ, f);  // jump if false
			}
			// skip the string-pointer matching code
			mv.visitJumpInsn(GOTO, done);
			// continue here if we have a valid string pointer
			mv.visitLabel(use_string_ptr);
		}
		else
		{
			if((clause.flags() & TagClause.VALUE_ANY_STRING) != 0)
			{
				// If the clause accepts a String converted from a numeric
				// tag value, we check that String if there is no local string
				// value present

				// TODO: should consolidate with above

				Label use_string_ptr = new Label();
				mv.visitJumpInsn(IFNE, use_string_ptr);
				mv.visitLdcInsn(matchString);
				mv.visitVarInsn(ALOAD, $val_string);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
					"equals", "(Ljava/lang/Object;)Z", false);
				if (t != null)
				{
					mv.visitJumpInsn(IFNE, t);  // jump if true
				}
				else
				{
					mv.visitJumpInsn(IFEQ, f);  // jump if false
				}
				// skip the string-pointer matching code
				mv.visitJumpInsn(GOTO, done);
				// continue here if we have a valid string pointer
				mv.visitLabel(use_string_ptr);
			}
			else
			{
				// Otherwise, value="string" always fails if string pointer is zero
				mv.visitJumpInsn(IFEQ, fx);
			}
		}
		// The code produced by matchString leaves the string-pointer
		// argument modified, so we use a temporary variable
		// TODO: don't need this if we're only matching one single value
		// (maybe not worth optimizing)
		mv.visitVarInsn(ILOAD, $val_string_ptr);
		mv.visitVarInsn(ISTORE, $temp_string_ptr);
		// TODO: let matchString stash this temp code, always use
		// $val_string_ptr as argument
		matchString(op, $temp_string_ptr, matchString, t, f);    // not fx
		mv.visitLabel(done);
		return true;
	}

	@Override protected void binaryLogicalExpression(BinaryExpression exp, Label t, Label f)
	{
		if (matchStringValue(exp, t, f)) return;
		super.binaryLogicalExpression(exp, t, f);
	}

	/**
	 * Emits code to compare two objects, which must already be
	 * placed on the stack.
	 *
	 * @param type   the class of the objects
	 * @param opcode the comparison opcode (IF_ICMPEQ, etc.)
	 * @param label  where to jump if comparison is true
	 */
	@Override protected void objectComparison(Class<?> type, int opcode, Label label)
	{
		if (type == GlobalString.class)
		{
			// Since global strings are simple integer values, we can apply
			// the comparison directly

			mv.visitJumpInsn(opcode, label);
			return;
		}
		super.objectComparison(type, opcode, label);
	}
}