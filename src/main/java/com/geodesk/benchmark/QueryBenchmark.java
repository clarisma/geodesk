package com.geodesk.benchmark;

import com.clarisma.common.fab.FabReader;
import com.geodesk.feature.FeatureLibrary;
import com.geodesk.feature.Features;
import com.geodesk.feature.Tags;
import com.geodesk.feature.Way;
import com.geodesk.core.Box;
import com.geodesk.geom.Bounds;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.locationtech.jts.util.Stopwatch;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;

public class QueryBenchmark
{
    public static final Logger log = LogManager.getLogger();

    private static Map<String,Class<?>> taskClasses = new HashMap<>();

    static
    {
        taskClasses.put("count", CountTask.class);
        taskClasses.put("names", NameTask.class);
        taskClasses.put("measure", MeasureTask.class);
        taskClasses.put("tags", TagsTask.class);
    }

    private Path outputPath;
    private Features<?> features;
    private Map<String,Bounds> areas = new HashMap<>();
    private Map<String,BoxSpecs> boxSpecs = new HashMap<>();
    private Map<String,String> queryStrings = new HashMap<>();
    // private Map<String,String> querySpecs = new HashMap<>();
    private Set<String> querySpecs = new HashSet<>();
    private ExecutorService executor = Executors.newFixedThreadPool(5); // TODO

    private static class Benchmark
    {
        String name;
        int queries;
        long totalFeatures;
        MutableLongList times = LongLists.mutable.empty();
    }


    private static class BoxSpecs
    {
        int count;
        int minMeters;
        int maxMeters;
    }

    private class BenchmarkSpecReader extends FabReader
    {
        BiConsumer<String,String> kvConsumer;

        void areas(String k, String v)
        {
            String[] lonLat = v.split("[,\\s]\\s*");
            areas.put(k, Box.ofWSEN(
                Double.parseDouble(lonLat[0]),
                Double.parseDouble(lonLat[1]),
                Double.parseDouble(lonLat[2]),
                Double.parseDouble(lonLat[3])));
        }

        void boxes(String k, String v)
        {
            String[] va = v.split("[,\\s]\\s*");
            BoxSpecs b = new BoxSpecs();
            b.count = Integer.parseInt(va[0]);
            b.minMeters = Integer.parseInt(va[1]);
            b.maxMeters = Integer.parseInt(va[2]);
            boxSpecs.put(k, b);
        }

        /*
        void script(String k, String v)
        {
            // querySpecs.put(k,v);
        }
         */

        void queries(String k, String v)
        {
            queryStrings.put(k,v);
        }

        @Override protected void beginKey(String key)
        {
            switch(key)
            {
            case "areas":
                kvConsumer = this::areas;
                break;
            case "boxes":
                kvConsumer = this::boxes;
                break;
            case "queries":
                kvConsumer = this::queries;
                break;
            default:
                log.warn("Skipping unknown section: {}", key);
            }
        }

        protected void keyValue(String key, String value)
        {
            if(key.equals("script"))
            {
                for(String s: value.split("[,\\s]\\s*"))
                {
                    querySpecs.add(s);
                }
                return;
            }
            if(kvConsumer != null) kvConsumer.accept(key, value);
        }

        protected void endKey()
        {
            kvConsumer = null;
        }
    }

    public void readSpecs(Path path) throws IOException
    {
        new BenchmarkSpecReader().readFile(path);
    }

    private abstract static class BenchmarkTask implements Callable<Double>
    {
        Features<?> features;
        String queryString;
        Bounds[] boxes;
        int boxStartIndex;
        int boxEndIndex;
        long count;
        double result;

        public BenchmarkTask()
        {
        }

        public void init(Features<?> features, String queryString,
            Bounds[] boxes, int boxStartIndex, int boxEndIndex)
        {
            this.features = features;
            this.queryString = queryString;
            this.boxes = boxes;
            this.boxStartIndex = boxStartIndex;
            this.boxEndIndex = boxEndIndex;
        }

        protected abstract void perform(Features<?> view);
        public abstract String result();
        public abstract String resultKey();

        @Override public Double call() throws Exception
        {
            for(int i=boxStartIndex; i<boxEndIndex; i++)
            {
                perform(features.features(queryString).in(boxes[i]));
            }
            // return result;
            return (double)count;
        }
    }

    private static class CountTask extends BenchmarkTask
    {
        public CountTask() {}

        @Override public void perform(Features<?> view)
        {
            long c = view.count();
            count += c;
            result += c;
        }

        @Override public String result()
        {
            return String.format("Counted %d features", (long)result);
        }

        @Override public String resultKey() { return "should_be_1"; }
    }

    /**
     * A BenchmarkTask that retrieves the names of the selected features
     * and adds their lengths to the result.
     */
    private static class NameTask extends BenchmarkTask
    {
        public NameTask() {}

        @Override public void perform(Features<?> view)
        {
            view.forEach(f ->
            {
                result += f.tag("name").length();
                count++;
            });
        }

        @Override public String result()
        {
            return String.format("Average name has %.1f characters", result / count);
        }

        @Override public String resultKey() { return "avg_name_length"; }
    }

    /**
     * A BenchmarkTask that adds the length of the feature (if it is a way),
     * or the length of its perimeter (if it is an area-way) to the result.
     */
    private static class MeasureTask extends BenchmarkTask
    {
        public MeasureTask() {}

        @Override public void perform(Features<?> view)
        {
            view.forEach(f ->
            {
                if(f instanceof Way)
                {
                    Way way = (Way)f;
                    result += way.length();
                    count++;
                }
            });
        }

        @Override public String result()
        {
            return String.format("Average Way is %.2f meters long", result / count);
        }

        @Override public String resultKey() { return "avg_length_meters"; }
    }


    private static class TagsTask extends BenchmarkTask
    {
        public TagsTask()
        {
        }

        @Override public void perform(Features<?> view)
        {
            view.forEach(f ->
            {
                Tags tags = f.tags();
                while(tags.next())
                {
                    result += tags.key().length();
                    result += tags.stringValue().length();
                }
                count++;
            });
        }

        @Override public String result()
        {
            return String.format("Average feature has %.1f tag characters", result / count);
        }

        @Override public String resultKey() { return "avg_tags_length"; }
    }

    void benchmark(String queryString, Class<?> taskClass, Bounds[] boxes,
        Benchmark bm, boolean parallel) throws Exception
    {
        Stopwatch timer = new Stopwatch();
        Constructor taskConstructor = taskClass.getConstructor();
        long count = 0;
        long time;
        double result;
        String resultKey;

        if(parallel)
        {
            int batchSize;
            switch(boxes.length)
            {
            case 1000:
                batchSize = 25;
                break;
            case 10000:
                batchSize = 250;
                break;
            default:
                batchSize = 500;
                break;
            }

            int batchCount = (boxes.length + batchSize - 1) / batchSize;
            List<BenchmarkTask> tasks = new ArrayList<>(batchCount);
            int startBox = 0;
            int endBox;
            for(int i=0; i<batchCount; i++)
            {
                endBox = Math.min(startBox + batchSize, boxes.length);
                BenchmarkTask task = (BenchmarkTask)taskConstructor.newInstance();
                task.init(features, queryString, boxes, startBox, endBox);
                tasks.add(task);
                startBox = endBox;
                // log.debug("Batched boxes {} to {}", startBox, endBox);
            }
            timer.start();
            for(Future<Double> val: executor.invokeAll(tasks)) count += val.get();
            time = timer.stop();
            result = 0;
            for(BenchmarkTask task : tasks)
            {
                result += task.result;
            }
            resultKey = tasks.get(0).resultKey();
        }
        else
        {
            BenchmarkTask task = (BenchmarkTask)taskConstructor.newInstance();
            task.init(features, queryString, boxes, 0, boxes.length);
            timer.start();
            log.debug("Executing...");
            count = task.call().longValue();
            time = timer.stop();
            result = task.result;
            resultKey = task.resultKey();
        }

        bm.queries = boxes.length;
        bm.totalFeatures = count;
        bm.times.add(time);

        log.debug("{} for {} boxes ({}): {} features in {} ms, {} = {}",
            queryString, boxes.length, parallel ? "parallel" : "sequential",
            count, time, resultKey, result / count);
    }


    private Map<String, RandomBoxes> makeBoxes(String area) throws IOException
    {
        Map<String, RandomBoxes> boxes = new HashMap<>();
        Bounds areaBounds = areas.get(area);
        for(Map.Entry<String,BoxSpecs> e: boxSpecs.entrySet())
        {
            String name = e.getKey();
            BoxSpecs spec = e.getValue();
            log.info("Preparing boxes for {} - size {}", area, name);
            Path path = outputPath.resolve(String.format("%s-%s.boxes", area, name));
            boxes.put(name, RandomBoxes.loadOrCreate(path, features, areaBounds,
                spec.count, spec.minMeters, spec.maxMeters, 10));
        }
        return boxes;
    }

    /*
    private List<String> makeScript(int runs) throws IOException
    {
        List<String> script = new ArrayList<>();
        List<String> tasks = new ArrayList<>();
        List<String> sizes = new ArrayList<>();
        for(Map.Entry<String,String> e: querySpecs.entrySet())
        {
            String query = e.getKey();
            String[] specs = e.getValue().split("[,\\s]\\s*");
            for(String s: specs)
            {
                if(taskClasses.containsKey(s))
                {
                    tasks.add(s);
                }
                else if (boxSpecs.containsKey(s))
                {
                    sizes.add(s);
                }
                else
                {
                    throw new RuntimeException(String.format(
                        "%s: '%s' is neither a task nor box size", query, s));
                }
            }
            if(tasks.isEmpty() || sizes.isEmpty())
            {
                throw new RuntimeException(String.format(
                    "%s: Must specify at least one task and one box size", query));
            }

            for(String task: tasks)
            {
                for(String size: sizes)
                {
                    String step = String.format("%s-%s-%s", query, task, size);
                    for(int i=0; i<runs; i++)
                    {
                        script.add(step);
                    }
                }
            }
            tasks.clear();
            sizes.clear();
        }
        return script;
    }
     */

    private List<String> makeScript(int runs) throws IOException
    {
        List<String> script = new ArrayList<>();
        for(String s: querySpecs)
        {
            for(int i=0; i<runs; i++) script.add(s);
        }
        Collections.shuffle(script);
        return script;
    }


    void performOne(Map<String,Benchmark> benchmarks, String benchmark, Map<String, RandomBoxes> boxes) throws Exception
    {
        log.debug(benchmark);
        String[] parts = benchmark.split("-");
        String query = parts[0];
        String task = parts[1];
        String size = parts[2];
        String queryString = queryStrings.get(query);
        Benchmark bm = benchmarks.get(benchmark);
        if(bm == null)
        {
            bm = new Benchmark();
            bm.name = benchmark;
            benchmarks.put(benchmark, bm);
        }
        benchmark(queryString, taskClasses.get(task),
            boxes.get(size).boxes(), bm, true);
    }

    private void writeBenchmarks(Path path, String name, Map<String,Benchmark> benchmarks) throws FileNotFoundException
    {
        List<Benchmark> list = new ArrayList<>(benchmarks.size());
        list.addAll(benchmarks.values());
        list.sort(Comparator.comparing(a -> a.name));
        PrintWriter out = new PrintWriter(new FileOutputStream(path.toFile()));
        out.format("name: %s\n", name);
        for(Benchmark bm: list)
        {
            out.format("\n%s:\n", bm.name);
            out.format("    queries:     %d\n", bm.queries);
            out.format("    features:    %d\n", bm.totalFeatures);
            bm.times.sortThis();
            out.format("    best-time:   %d\n", bm.times.getFirst());
            out.format("    worst-time:  %d\n", bm.times.getLast());
            out.format("    avg-time:    %d\n", (long)bm.times.average());
            out.format("    median-time: %d\n", (long)bm.times.median());
        }
        out.close();
    }

    private List<String> loadOrCreateScript(Path path, int runs) throws IOException
    {
        if(Files.exists(path))
        {
            return Files.readAllLines(path);
        }
        List<String> script = makeScript(runs);
        Files.write(path, script);
        return script;
    }

    public void perform(Path storePath, String area) throws Exception
    {
        features = new FeatureLibrary("/home/md/geodesk/benchmarks");
        new BenchmarkSpecReader().read(
            getClass().getClassLoader().getResourceAsStream("benchmarks/benchmark.fab"));
        Map<String, RandomBoxes> boxes = makeBoxes(area);
        List<String> script = loadOrCreateScript(outputPath.resolve("script.txt"), 5);
        Map<String,Benchmark> benchmarks = new HashMap<>();
        for(String benchmark: script)
        {
            performOne(benchmarks, benchmark, boxes);
        }
        writeBenchmarks(outputPath.resolve("results.fab"), "test", benchmarks);
        executor.shutdown();
    }

    public static void main(String[] args) throws Exception
    {
        // new QueryBenchmark().perform(Paths.get("C:\\geodesk\\tests\\de.gol"), "germany");
        new QueryBenchmark().perform(Paths.get("/home/md/geodesk/tests/de4.gol"), "germany");
    }
}
