package example.utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * StreamTest：
 *
 * @Description:Stream方法练习
 * @author: yatao.ma
 * @date: 2020/6/30 2:56 下午
 */
public class StreamTest {
    public static void main(String[] args) {
        ArrayList<Integer> list = new ArrayList();
        for (int i = -1; i < 10; i++) {
            list.add(i);
        }
        list.add(8);
        boolean anyMatch = list.stream().allMatch(integer -> integer > 0);
        System.out.println("anyMatch: integer > 0 ;result " + anyMatch);
        boolean noneMatch = list.stream().noneMatch(integer -> integer < 0);
        System.out.println("noneMatch: integer > 0 ;result " + noneMatch);
        boolean allMatch = list.stream().allMatch(integer -> integer > 4);
        System.out.println("allMatch: integer > 4 ;result " + allMatch);
        System.out.println("findFirst=   " + list.stream().findFirst().get());
        ;
        System.out.println("findAny=   " + list.stream().findAny().get());
        ;
        Comparator<Integer> comparator = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {  //负数,0,正数 表示第一参数小于等于大于第二参数
                return o1 - o2;
            }
        };
        int max = list.stream().max(comparator).get();
        System.out.println("max: " + max);
        int min = list.stream().min(comparator).get();
        System.out.println("min: " + min);
        long count = list.stream().count();
        System.out.println("count:  " + count);
        System.out.println(" ----------------我是一个分割线---------------------- ");
        //返回一个元素各异的流，说白了就是去重
        list.stream().distinct().forEach(i -> System.out.print(i + "  "));
        /**
         这个方法的主要作用是把 Stream 元素组合起来。它提供一个起始值（种子），
         然后依照运算规则（BinaryOperator），和前面 Stream 的第一个、第二个、第 n 个元素组合
         */
        long reduce = list.stream().reduce(0, (a, b) -> a + b).longValue();
        System.out.println("reduce: " + reduce);
        System.out.print("forEachOrdered:");
        list.stream().forEachOrdered(i -> System.out.print(i + "  "));
        System.out.println();
        System.out.println(" ----------------我是一个分割线---------------------- ");
        Stream<Integer> filter = list.stream().filter(n -> n % 2 == 0);
        System.out.print("filter:");
        filter.forEach(i -> System.out.print(i + "  "));
        System.out.println();
        System.out.println(" ----------------我是一个分割线---------------------- ");
        System.out.print("sorted: ");
        list.stream().sorted().forEach(i -> System.out.print(i + "  "));
        System.out.println();
        System.out.println(" ----------------我是一个分割线---------------------- ");
        System.out.print("limit4个 :");
        list.stream().limit(4).forEach(i -> System.out.print(i + "  "));
        System.out.println();
        System.out.println(" ----------------我是一个分割线---------------------- ");
        System.out.print("skip4个  :");
        list.stream().skip(4).forEach(i -> System.out.print(i + "  "));
        System.out.println();
        System.out.println(" ----------------我是一个分割线---------------------- ");
        Stream<String> stream = Stream.of("2", "4", "5");
        //第一种 直接调用stream.Collectors里的方法
        List ll = stream.collect(Collectors.toList());
        System.out.print("collect:");
        ll.stream().forEach(s -> System.out.print(s + " "));
        System.out.println();
        System.out.println(" ----------------我是一个分割线---------------------- ");
        /**
         * stream.collect() 的本质由三个参数构成,
         * <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);
         * supplier：动态的提供初始化的值；创建一个可变的结果容器（JAVADOC）；对于并行计算，这个方法可能被调用多次，每次返回一个新的对象；
         * accumulator：类型为BiConsumer，注意这个接口是没有返回值的；它必须将一个元素放入结果容器中（JAVADOC）。
         * combiner：类型也是BiConsumer，因此也没有返回值。它与三参数的Reduce类型，只是在并行计算时汇总不同线程计算的结果。
         *          它的输入是两个结果容器，必须将第二个结果容器中的值全部放入第一个结果容器中（JAVADOC）。
         *   list装map
         */
        Map<String, Integer> siMap = list.stream().collect(() -> new HashMap<>(),
                (map, p) -> map.put(String.valueOf(p), p), (m, n) -> m.putAll(n));
        siMap.forEach((k, v) -> System.out.print("key:" + k + "   vulaue: " + v + "\t"));

        System.out.println(" ----------------我是一个分割线---------------------- ");
        /**
         * map接受一个函数作为参数。这个函数会被应用到每个元素上，并映射成新的元素
         */
        List<Double> output = list.stream().
                map(Double::valueOf).collect(Collectors.toList());
        output.stream().forEach(i -> System.out.print(i + "  "));
        System.out.println();

        System.out.println(" ----------------我是一个分割线---------------------- ");
        /**
         * 普通流转换成数值流。
         * StreamAPI提供了三种数值流：IntStream、DoubleStream、LongStream，
         * 也提供了将普通流转换成数值流的三种方法：mapToInt、mapToDouble、mapToLong。
         */
        OptionalDouble x = list.stream().mapToDouble(Integer::doubleValue).max();
        System.out.println("OptionalDouble: " + x);

        /**
         * 返回由该流的元素组成的流，并在所提供的流中执行所提供的每个元素上的动作。
         * 这是一个intermediate operation。
         * 对于并行流管道，操作可以在任何时间和任何线程中调用元素，由上游操作提供。如果操作修改共享状态，则负责提供所需的同步。
         */
        Stream.of("one", "two", "three", "four")
                .filter(e -> e.length() > 3)
                .peek(e -> System.out.println("Filtered value: " + e))
                .map(String::toUpperCase)
                .peek(e -> System.out.println("Mapped value: " + e))
                .collect(Collectors.toList());


        /**
         *flatMap各个数组并不是分别映射成一个流，而是映射成流的内容，说白了就是把几个小的list转换到一个大的list
         * 流扁平化，让你把一个流中的每个值都换成另一个流，然后把所有的流连接起来成为一个流
         * 一个2维的集合映射成一个一维,相当于他映射的深度比map深了一层
         */
        String[] words = new String[]{"Hello", "World"};
        List<String> collect = Stream.of(words).map(i -> i.split("")).flatMap(Stream::of).collect(Collectors.toList());


        //参数Function函数式接口提供由T到IntStream的转化，方法返回值是IntStream
        List<List<String>> listOfLists = Arrays.asList(
                Arrays.asList("1", "2"),
                Arrays.asList("5", "6"),
                Arrays.asList("3", "4")
        );
        IntStream intStream = listOfLists.stream()
                        .flatMapToInt(childList -> childList.stream().mapToInt(Integer::new));

        int sum = intStream.peek(e -> System.out.print(" "+e+" ")).sum();
        System.out.println();
        System.out.println("sum: " + sum);

    }

}
