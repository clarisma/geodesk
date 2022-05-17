package com.clarisma.common.soar;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import static java.nio.file.StandardOpenOption.*;

import com.clarisma.common.io.ByteBufferOutputStream;
import com.clarisma.common.pbf.PbfOutputStream;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

// TODO: do we need this?
//  work is done by a Layout class

public class Archive 
{
	private Struct header;
	private Struct beforeCurrentPage;
	private Struct last;
	private int pos;
	private int pageSpaceRemaining;
	private int pageSizeShift = 12;
	private int pageSize = 4096;
	private int pageSizeMask = 0xffff_ffff >>> (32-pageSizeShift);
	private MutableIntObjectMap<Hole> holesByPage = new IntObjectHashMap<>();
	private TreeMap<Integer, Hole> holesBySize = new TreeMap<>(); 

	public int size()
	{
		return pos;
	}
	
	private static class Hole
	{
		Struct prev;
		int pos;
		int size;
		
		Hole(Struct prev, int pos, int size)
		{
			this.prev = prev;
			this.pos = pos;
			this.size = size;
		}
	}

	public Struct header()
	{
		return header;
	}

	public Struct last()
	{
		return last;
	}
	
	public void setHeader(Struct header)
	{
		assert this.header == null;
		this.header = header;
		beforeCurrentPage = header;
		last = header;
		pos = header.size();
		pageSpaceRemaining = pageSize-pos;
	}
	
	public void newPage()
	{
		if(pageSpaceRemaining > 0)
		{
			if(pageSpaceRemaining == pageSize) return;
			Hole hole = new Hole(last, pos, pageSpaceRemaining);
			holesByPage.put(pos >>> pageSizeShift, hole);
			holesBySize.put(pageSpaceRemaining, hole);
		}
		pos = (pos + pageSize - 1) & (~pageSizeMask);
		pageSpaceRemaining = pageSize;
	}

	public int pageSize()
	{
		return pageSize;
	}

	public int pageSpaceRemaining()
	{
		return pageSpaceRemaining;
	}
	
	public void place(Struct s)
	{
		assert header != null: "Must set header before adding other structs";
		assert s.location() <= 0: "Struct has already been placed: " + s;
		// TODO
		
		//while(s != null)
		//{
			pos = s.alignedLocation(pos);
			s.setLocation(pos);
			last.setNext(s);
			pos += s.size();
			last = s;
			// s = s.next();
			pageSpaceRemaining = pageSize - (pos & pageSizeMask);
		// }
	}
	
	/**
	 * Attempts to place the given Struct, along with its siblings,
	 * into the current page. 
	 *  
	 * @param s		the Struct to place
	 * @return		true if the struct has been placed successfully,
	 * 				otherwise false
	 */
	public boolean fit(Struct s)
	{
		// TODO
		return false;
	}
	
	/**
	 * Attempts to place the given Struct, along with its siblings,
	 * into a specific page. In order to avoid the method having to
	 * recalculate the size of the structs, it can be provided if
	 * it is known. 
	 * 
	 * @param s			a Struct (or chain of structs) 
	 * @param sizeHint	the minimum size (assuming no gaps) of the 
	 * 		  			structs, or zero 
	 * @param page		the page into which to place the structs
	 * @return
	 */
	public boolean stuffIntoPage(Struct s, int sizeHint, int page)
	{
		// TODO
		return false;
	}
	
	public void writeGzipFile(Path path) throws IOException
	{
		try(FileOutputStream fout = new FileOutputStream(path.toFile()))
		{
			GZIPOutputStream zip = new GZIPOutputStream(fout);
			StructOutputStream out = new StructOutputStream(zip);
			out.writeChain(header);
			out.close();
			zip.close();
		}
	}
	
	public void writeFile(Path path) throws IOException
	{
		try(FileOutputStream fout = new FileOutputStream(path.toFile()))
		{
			StructOutputStream out = new StructOutputStream(fout);
			out.writeChain(header);
			out.flush();
			fout.flush();
			out.close();
		}
	}

	public void writeSparseFile(Path path) throws IOException
	{
		// SPARSE only applies to CREATE_NEW, so we'll have to explicitly
		// delete any existing file

		if(Files.exists(path)) Files.delete(path);
		FileChannel channel = (FileChannel)Files.newByteChannel(path,
			CREATE_NEW,WRITE,SPARSE);
		try(OutputStream fout = Channels.newOutputStream(channel))
		{
			StructOutputStream out = new StructOutputStream(fout);
			out.writeChain(header);
			out.flush();
			fout.flush();
			out.close();
			channel.close();
		}
	}



	public void writeToBuffer(ByteBuffer buf, int pos, PbfOutputStream imports) throws IOException
	{
		StructOutputStream out = new StructOutputStream(
			new ByteBufferOutputStream(buf, pos));
		out.setLinks(imports);
		out.writeChain(header);
	}

	public void dump(Path path) throws IOException
	{
		PrintWriter out = new PrintWriter(new FileWriter(path.toFile()));
		dumpStructs(out);
		out.close();
	}
	
	public void dumpStructs(PrintWriter out)
	{
		Struct s = header;
		int pos = 0;
		int totalGaps = 0;
		while(s != null)
		{
			int gap = s.location() - pos;
			if(gap > 0) 
			{
				out.format("     ---  %d-byte gap\n", gap);
				totalGaps += gap;
			}
			s.dump(out);
			pos = s.location() + s.size();
			s = s.next();
		}
		out.format("\n%d bytes wasted\n", totalGaps);
	}
}
