/*
 * Copyright (c) Clarisma / GeoDesk contributors
 *
 * This source code is licensed under the Apache 2.0 license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.clarisma.common.text;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Table
{
    private final List<Column> columns = new ArrayList<>();
    private final List<String> values = new ArrayList<>();
    private int currentCol;
    private int totalWidth;
    private int maxWidth = 80;
    private boolean ready;

    public static class Column implements Comparable<Column>
    {
        int span = 1;
        int gap = 2;
        String header;
        DecimalFormat format;
        int minWidth = 8;
        int width;
        int averageWidth;
        int widthVariance;

        public Column format(String format)
        {
            this.format = new DecimalFormat(format);
            return this;
        }

        public Column gap(int gap)
        {
            this.gap = gap;
            return this;
        }

        @Override public int compareTo(Column other)
        {
            return Integer.compare(other.widthVariance, this.widthVariance);
        }
    }

    public Column column()
    {
        Column c = new Column();
        columns.add(c);
        return c;
    }

    public Column column(String header)
    {
        Column c = column();
        c.header = header;
        return c;
    }

    public Column column(String header, String format)
    {
        Column c = column();
        c.header = header;
        c.format = new DecimalFormat(format);
        return c;
    }

    public void skipColumn()
    {

    }

    private void beginRow()
    {
        values.add("");
    }

    public void add(String s)
    {
        ready = false;
        if (currentCol == 0) beginRow();
        values.add(s);
        Column c = columns.get(currentCol);
        int len = s.length();
        if(c.width < len) c.width = len;
        currentCol++;
        if (currentCol == columns.size()) currentCol = 0;
    }

    public void add(double v)
    {
        Column c = columns.get(currentCol);
        add(c.format.format(v));
    }

    public void newRow()
    {
        if(currentCol==0) return;
        while (currentCol < columns.size()) add("");
    }

    public void divider(String div)
    {
        newRow();
        for(int i=0; i<=columns.size(); i++) values.add(div);
    }

    protected void layout()
    {
        for(Column col: columns)
        {
            totalWidth += col.width + col.gap;
        }
        totalWidth -= columns.get(columns.size()-1).gap;
        if(totalWidth > maxWidth)
        {
            shrinkColumns();
        }
        ready = true;
    }

    private void shrinkColumns()
    {
        int rowCount = 0;
        int currentCol = -1;
        for(String value : values)
        {
            if(currentCol >= 0)
            {
                Column col = columns.get(currentCol);
                col.averageWidth += value.length();
            }
            currentCol++;
            if(currentCol == columns.size())
            {
                currentCol = -1;
                rowCount++;
            }
        }

        // Gather all non-numeric columns (We cannot shrink columns
        // with numeric values)

        List<Column> elasticColumns = new ArrayList<>();
        for(Column col: columns)
        {
            if(col.format == null)
            {
                elasticColumns.add(col);
                col.averageWidth /= rowCount;
                col.widthVariance = col.width - col.averageWidth;
            }
        }
        if(elasticColumns.size() == 0) return;

        int needToTrim = totalWidth - maxWidth;

        // When shrinking columns, we'll focus on those that have the highest
        // difference between with and the average width of their cells,
        // because these are the columns that will be least degraded (usually,
        // there are one or two outliers that are far wider than the other
        // cells, and only these will appear trimmed)
        // first, we sort the columns, highest variance first

        Collections.sort(elasticColumns);
        Column startCol = elasticColumns.get(0);
        while(needToTrim > 0)
        {
            int excess = startCol.widthVariance;
            if(excess <= 0)
            {
                // As final step, trim all columns by equal amount
                trimColumns(elasticColumns, elasticColumns.size(), needToTrim);
                break;
            }
            int trimColCount = 1;
            for (; trimColCount < elasticColumns.size(); trimColCount++)
            {
                Column col = elasticColumns.get(trimColCount);
                if (col.widthVariance < excess) break;
            }
            excess *= trimColCount;
            int trimNow = Math.min(excess, needToTrim);
            needToTrim -= trimColumns(elasticColumns, trimColCount, trimNow);
        }
    }

    /**
     * Shrinks a set of columns by trimming each width. The width of a column
     * will never be reduced to less than Column.minWidth.
     *
     * @param elasticColumns    the columns that can be trimmed
     * @param colCount          the number of columns to actually trim (counted
     *                          from the start)
     * @param trimNow           the total number of characters to trim
     * @return                  the actual number of characters trimmed
     */
    private int trimColumns(List<Column> elasticColumns, int colCount, int trimNow)
    {
        int leftToTrim = trimNow;
        for (int i = colCount - 1; i >= 0; i--)
        {
            Column col = elasticColumns.get(i);
            int trimThisCol = (i == 0) ? leftToTrim : (trimNow / colCount);
            trimThisCol = Math.min(trimThisCol, col.width - col.minWidth);
            col.width -= trimThisCol;
            col.widthVariance -= trimThisCol;
            leftToTrim -= trimThisCol;
        }
        int trimmed = trimNow - leftToTrim;
        totalWidth -= trimmed;
        return trimmed;
    }

    public void print(Appendable out) throws IOException
    {
        if(!ready) layout();
        int n = 0;
        while(n < values.size())
        {
            String rowType = values.get(n++);
            if(!rowType.isEmpty())
            {
                out.append(rowType.repeat(totalWidth));
                out.append("\n");
                n += columns.size();
                continue;
            }
            for(Column col: columns)
            {
                String value = values.get(n++);
                int len = value.length();
                int width = col.width;
                if(len < width)
                {
                    String padding = " ".repeat(width-len);
                    if(col.format != null)
                    {
                        out.append(padding);
                        out.append(value);
                    }
                    else
                    {
                        out.append(value);
                        out.append(padding);
                    }
                }
                else if(len > width)
                {
                    out.append(value.substring(0, width-3));
                    out.append("...");
                }
                else
                {
                    out.append(value);
                }
                out.append(" ".repeat(col.gap));
            }
            out.append("\n");
        }
    }

    @Override public String toString()
    {
        StringBuilder buf = new StringBuilder();
        try
        {
            print(buf);
        }
        catch(IOException ex)
        {
            // This should never happen
            throw new RuntimeException(ex);
        }
        return buf.toString();
    }
}
