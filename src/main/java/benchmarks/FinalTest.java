package benchmarks;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

/**
 * Purpose of this experiment:
 *
 * Is the compiler smart enough to elide a chain of calls that produces final elements?
 * There is nothing to calculate, both links and end values are final.
 *
 * Conclusion:
 *
 * No, apparently not, execution time scales with the depth of the chain.
 * Marking classes and/or methods final makes no difference.
 */

public class FinalTest
{
    interface Value
    {
        int value();
    }

    static class End implements Value
    {
        private final int value;
        public End(int val)
        {
            value = val;
        }

        public int value()
        {
            return value;
        }
    }

    static class Link implements Value
    {
        private final Value value;
        public Link(Value val)
        {
            value = val;
        }

        public int value()
        {
            return value.value();
        }
    }

    static Value[] make(int count, int depth)
    {
        Value[] values = new Value[count];
        for(int i=0; i<values.length; i++)
        {
            Value prev = new End(i);
            for(int j=0; j<depth; j++)
            {
                prev = new Link(prev);
            }
            values[i] = prev;
        }
        return values;
    }

    static void test(int count, int depth)
    {
        Value[] values = make(count, depth);
        int sum = 0;
        long start = currentTimeMillis();
        for(int i=0; i<1000; i++)
        {
            for(int j=0; j<values.length; j++)
            {
                sum += values[j].value();
            }
        }
        long end = currentTimeMillis();
        out.format("%d items with %d depth = sum(%d) in %d ms\n", count, depth, sum, end-start);
    }

    public static void main(String[] args)
    {
        test(100_000, 1);
        test(100_000, 2);
        test(100_000, 1);
        test(100_000, 2);
        test(100_000, 1);
        test(100_000, 2);
        test(100_000, 4);
        test(100_000, 2);
        test(100_000, 4);
        test(100_000, 2);
        test(100_000, 1);
        test(100_000, 1);
    }

}
