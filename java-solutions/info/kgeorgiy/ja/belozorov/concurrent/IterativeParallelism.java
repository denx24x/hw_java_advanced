package info.kgeorgiy.ja.belozorov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.Math.min;


public class IterativeParallelism implements ListIP {
    private final ParallelMapper mapper;
    public IterativeParallelism(){ this.mapper = null; }
    public IterativeParallelism(ParallelMapper mapper){
        this.mapper = mapper;
    }

    private <T, R> R calculate(int threads, List<? extends T> values, Collector<? super T, ?, R> collector, Collector<R, ?, R> finalizer) throws InterruptedException {
        if (threads <= 0) {
            throw new IllegalArgumentException("Thread count should greater when zero");
        }
        ParallelMapper parallelMapper = mapper;
        if(parallelMapper == null){
            parallelMapper = new ParallelMapperImpl(threads);
        }
        List<List<? extends T>> data = new ArrayList<>(threads);
        threads = min(threads, values.size());
        int count = values.size() / threads;
        int ost = values.size() % threads;
        int pos = 0;
        for(int i = 0; i < threads;i++){
            int ln = count + (i < ost ? 1 : 0);
            data.add(values.subList(pos, pos + ln));
            pos += ln;
        }
        R result = parallelMapper.map((val) -> val.stream().collect(collector), data).stream().collect(finalizer);
        if(mapper == null){
            parallelMapper.close();
        }
        return result;
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return comparatorPredicate(threads, values, Collectors.maxBy(comparator));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    private <T> T comparatorPredicate(int threads, List<? extends T> values, Collector<T, ?, Optional<T>> comp) throws InterruptedException {
        return calculate(
                threads,
                values,
                Collectors.collectingAndThen(comp, Optional::get),
                Collectors.collectingAndThen(comp, Optional::get)
        );
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !any(threads, values, predicate.negate());
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return boolPredicate(threads, values, predicate, Boolean::logicalOr, false);
    }

    private <T> boolean boolPredicate(int threads, List<? extends T> values, Predicate<? super T> predicate, BinaryOperator<Boolean> op, Boolean init) throws InterruptedException {
        return calculate(threads,
                values,
                Collectors.reducing(init, predicate::test, op),
                Collectors.reducing(init, op)
        );
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return calculate(
                threads, values,
                Collectors.reducing(0, (T val) -> (predicate.test(val) ? 1 : 0), Integer::sum),
                Collectors.reducing(0, Integer::sum)
        );
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return calculate(threads,
                values,
                Collectors.mapping(Object::toString, Collectors.joining()),
                Collectors.mapping(Object::toString, Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return calculate(
                threads,
                values,
                Collectors.filtering(predicate, Collectors.<T>toList()),
                Collectors.flatMapping(Collection::stream, Collectors.toList())
        );
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return calculate(threads,
                values,
                Collectors.mapping(f, Collectors.toList()),
                Collectors.flatMapping(Collection::stream, Collectors.toList())
        );
    }
}
