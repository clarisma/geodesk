package com.geodesk.geom;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.geodesk.core.Box;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class RTree
{
	protected Node root;

	public static class Node extends Box
	{
		private List<Bounds> children;
		private boolean isLeaf;
		
		public Node(List<Bounds> children, boolean isLeaf)
		{
			if(children==null)
			{
				children = new ArrayList<>();
			}
			else
			{
				for(Bounds b: children) expandToInclude(b);
			}
			this.children = children;
			this.isLeaf = isLeaf;
		}
		
		public boolean isLeaf()  
		{
			return isLeaf;
		}
		
		public List<Bounds> children()
		{
			return children;
		}
		
		// may only be called before tree is built 
		protected void add(Bounds child)
		{
			children.add(child);
			expandToInclude(child);
		}
		
		@SuppressWarnings("unchecked")
		public <T extends Bounds> void visit(Bounds bbox, Consumer<T> consumer)
		{
			if(isLeaf)
			{
				for(Bounds child: children)
				{
					if(child.intersects(bbox)) consumer.accept((T)child);
				}
			}
			else
			{
				for(Bounds child: children)
				{
					if(child.intersects(bbox)) ((Node)child).visit(bbox, consumer);
				}
			}
		}

		/*
		public void dumpToMap(String name, int levels, Bounds parentEnv, String color) throws IOException
		{
			DebugMap map = new DebugMap(name);
			if(parentEnv != null) map.addRectangle(parentEnv);
			for (int i=0; i<children.size(); i++)
			{
				Bounds b = children.get(i);
				boolean isLeaf = (b instanceof BoundedItem<?>);
				String tooltip = isLeaf ? ((BoundedItem<?>)b).get().toString() : Integer.toString(i);
				String url = isLeaf ? "https://www.openstreetmap.org/" + tooltip : 
					String.format("%s-%d.html", name, i);
				Marker rect = map.addRectangle(b).tooltip(tooltip).url(url);
				if(color != null) rect.option("color", color);
				if(levels > 1 && b instanceof Node)
				{
					((Node)b).dumpToMap(name + "-" + i, levels-1, this, "orange");
				}
			}
			map.save();
		}
		*/
	}
	
	public Node root()
	{
		return root;
	}
	
	public <T extends Bounds> void query(Bounds bbox, Consumer<T> consumer)
	{
		root.visit(bbox, consumer);
	}

	/*
	public void dumpToMap(String name, int levels)
	{
		try 
		{
			root.dumpToMap(name, levels, null, null);
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	*/

}
