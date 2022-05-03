package com.geodesk.map;

import com.geodesk.util.JavaScript;
import org.locationtech.jts.geom.Envelope;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class Marker 
{
	protected Map<String,Object> options = new HashMap<>();
	protected String tooltip;
	protected String url;
	protected SlippyMap map;
	
	public Marker tooltip(String content)
	{
		tooltip = content;
		return this;
	}
	
	public Marker url(String url)
	{
		this.url = url;
		return this;
	}
	
	public Marker addTo(SlippyMap map)
	{
		map.add(this);
		return this;
	}
	
	public void setMap(SlippyMap map)
	{
		this.map = map;
	}

	public Marker options(Map<String,Object> moreOptions)
	{
		options.putAll(moreOptions);
		return this;
	}

	public Marker option(String key, Object value)
	{
		options.put(key, value);
		return this;
	}
	
	public abstract Envelope envelope();
	
	protected abstract void emitPart(Appendable out) throws IOException;
	
	public void emit(Appendable out) throws IOException
	{
		emitPart(out);
		if(options.size() > 0)
		{
			out.append(',');
			JavaScript.writeMap(out, options);
		}
		out.append(')');
		if(tooltip != null && tooltip.length() > 0)
		{
			out.append(".bindTooltip(");
			JavaScript.writeString(out, tooltip);
			out.append(")");
		}
		if(url != null && url.length() > 0)
		{
			out.append(".on('click', function(){window.location=");
			JavaScript.writeString(out, url);
			out.append(";})");
		}
		out.append(".addTo(map);\n");
	}
	
}
