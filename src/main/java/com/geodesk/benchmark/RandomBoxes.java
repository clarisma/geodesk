package com.geodesk.benchmark;

import com.geodesk.core.Mercator;
import com.geodesk.core.Tile;
import com.geodesk.core.Box;
import com.geodesk.feature.FeatureLibrary;
import com.geodesk.feature.Features;
import com.geodesk.geom.Bounds;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class RandomBoxes
{
    public static final Logger log = LogManager.getLogger();

    private String name;
    private Bounds outer;
    private Bounds[] boxes;
    private int minMeters;
    private int maxMeters;
    private int minDensity;

    private RandomBoxes(String name, Bounds outer, Bounds[] boxes,
        int minMeters, int maxMeters, int minDensity)
    {
        this.name = name;
        this.outer = outer;
        this.boxes = boxes;
        this.minMeters = minMeters;
        this.maxMeters = maxMeters;
        this.minDensity = minDensity;
    }

    public Bounds[] boxes()
    {
        return boxes;
    }

    public static RandomBoxes load(Path path) throws IOException
    {
        FileInputStream fin = new FileInputStream(path.toFile());
        DataInputStream in = new DataInputStream(fin);
        Bounds outer = new Box(
            in.readInt(), in.readInt(), in.readInt(), in.readInt());
        int minMeters = in.readInt();
        int maxMeters = in.readInt();
        int minDensity = in.readInt();
        int count = in.readInt();
        Bounds[] boxes = new Bounds[count];
        for(int i=0; i<count; i++)
        {
            boxes[i] = new Box(
                in.readInt(), in.readInt(), in.readInt(), in.readInt());
        }
        in.close();
        fin.close();
        return new RandomBoxes(null, outer, boxes,
            minMeters, maxMeters, minDensity);
    }

    public static RandomBoxes loadOrCreate(Path path,
        Features<?> features, Bounds outer, int count,
        int minMeters, int maxMeters, int minDensity) throws IOException
    {
        if(Files.exists(path)) return load(path);

        Box[] boxes = new Box[count];
        Random random = new Random();
        int minExtent = (int) Mercator.deltaFromMeters(minMeters, outer.centerY());
        int maxExtent = (int) Mercator.deltaFromMeters(maxMeters, outer.centerY());
        int extentRange = maxExtent - minExtent;
        int outerWidth = (int)outer.width();
        int outerHeight = (int)outer.height();
        for (int i = 0; i < count; i++)
        {
            for (; ; )
            {
                int width = minExtent + random.nextInt(extentRange);
                int height = minExtent + random.nextInt(extentRange);
                int x = outer.minX() + random.nextInt(outerWidth - width);
                int y = outer.minY() + random.nextInt(outerHeight - height);
                Box box = Box.ofXYWidthHeight(x, y, width, height);
                if (features.in(box).count() < minDensity) continue;
                boxes[i] = box;
                if ((i + 1) % 1000 == 0) log.debug("{} boxes generated", i + 1);
                break;
            }
        }
        RandomBoxes rb = new RandomBoxes(null, outer, boxes,
            minMeters, maxMeters, minDensity);
        rb.save(path);
        return rb;
    }

    private void save(Path path) throws IOException
    {
        FileOutputStream fout = new FileOutputStream(path.toFile());
        DataOutputStream out = new DataOutputStream(fout);
        out.writeInt(outer.minX());
        out.writeInt(outer.minY());
        out.writeInt(outer.maxX());
        out.writeInt(outer.maxY());
        out.writeInt(minMeters);
        out.writeInt(maxMeters);
        out.writeInt(minDensity);
        out.writeInt(boxes.length);
        for(Bounds b: boxes)
        {
            out.writeInt(b.minX());
            out.writeInt(b.minY());
            out.writeInt(b.maxX());
            out.writeInt(b.maxY());
        }
        out.flush();
        fout.flush();
        out.close();
        fout.close();
    }

    public static void main(String[] args) throws IOException
    {
        Features<?> features = new FeatureLibrary("C:\\geodesk\\local");
        loadOrCreate(
            Paths.get("C:\\geodesk\\random-boxes.bin"),
            features, Tile.bounds(Tile.fromString("4/8/5")),
            100_000, 250, 1000, 2);
        /*
        DebugMap map = new DebugMap("boxes");
        for(Bounds b: boxes) map.addRectangle(b);
        map.save();
         */
    }
}
