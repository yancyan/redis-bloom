package com.example.redisbloom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 预计数据量     误差率            长度(长度转换容量)        最优哈希数       容量（redis客户端显示）
 * 千万级
 * 10_000_000 + 0.001   ======> size:143775875（17.97MB） hashIter: 10   || 17.10M
 * 10_000_000 + 0.00001 ======> size:239626495（29.9MB） hashIter: 17    || 26.13M
 * 亿级
 * 100_000_000 + 0.001 ======>  size:1437758756（179.72MB） hashIter: 10   || 161.83M
 * 100_000_000 + 0.00001 ====>  size:2396264594（299.53MB） hashIter: 17   || 300.83M
 *
 * @author ZhuYX
 * @date 2021/06/11
 */
@Slf4j
public class TransBloomFilter {

    @Resource
    RedissonClient redissonClient;

    private static final String TRANS_PREFIX = "bf:payer-trans-no:";
    public static final String TRANS_STATE = TRANS_PREFIX + "state";
    public static final String TRANS_TASK_SET = TRANS_PREFIX + "task-set";


    public static final long expectedInsertions = 10_000_000;
    public static final double falseProbability = 0.0001;

    public static final DateTimeFormatter monthKeyFormatter = DateTimeFormatter.ofPattern("yyyyMM");

    // on or off.
    private volatile boolean enableUse = false;
    private final AtomicInteger bC = new AtomicInteger(0);

    private static TransBloomFilter instance;
    public static final ObjectMapper mapper = new ObjectMapper();

    private TransBloomFilter() {
        BeanFactoryProvider.autowire(this);

    }

    public static TransBloomFilter getShareInstance() {
        if (instance == null) {
            synchronized (TransBloomFilter.class) {
                if (instance == null) {
                    instance = new TransBloomFilter();
                }
            }
        }
        return instance;
    }

    // current (first.) -> prev(last.)
    private static final ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<>() {
        {
            offerLast(LocalDate.now().format(monthKeyFormatter));
            offerLast(LocalDate.now().minusMonths(1).format(monthKeyFormatter));
            offerLast(LocalDate.now().minusMonths(2).format(monthKeyFormatter));
            offerLast(LocalDate.now().minusMonths(3).format(monthKeyFormatter));
            offerLast(LocalDate.now().minusMonths(4).format(monthKeyFormatter));
            offerLast(LocalDate.now().minusMonths(5).format(monthKeyFormatter));
        }
    };


    private boolean check(String transNo) {
        try {
            smartKey();
            var name = "";
            for (String fl : deque) {
                var rBloomFilter = getBloomFilter(fl);
                if (rBloomFilter.contains(transNo)) {
                    return false;
                }
                name = fl;
            }
            var addResult = getBloomFilter(name).add(transNo);
            if (!enableUse) {
                return false;
            }
            return addResult;
        } catch (Exception e) {
            if (e instanceof RedissonShutdownException) {
                log.info("redisson shutdown, switch to database check.");
            } else {
                log.error("the bloom filter error. msg " + e.getMessage(), e);
            }
        }
        return false;
    }

    // -> path: yyyyMM
    public <T> void append(String path, T sc) {
        try {
            var format = deque.stream().filter(fn -> fn.startsWith(path)).findFirst()
                    .orElseThrow(() -> new RuntimeException("Cannot find the valid bloom filter name: " + path));
            getBloomFilter(format).add(mapper.writeValueAsString(sc));
        } catch (JsonProcessingException e) {
            log.warn("write sc error." + e.getMessage());
        }
    }

    public <T> boolean check(T param) {
        try {
            return check(mapper.writeValueAsString(param));
        } catch (JsonProcessingException e) {
            log.info("write sc error." + e.getMessage());
        }
        return false;
    }

    // cache ???
    private RBloomFilter<String> getBloomFilter(String path) {
        var bf = redissonClient.<String>getBloomFilter(TRANS_PREFIX + path);
        if (!bf.isExists()) {
            bf.tryInit(expectedInsertions, falseProbability);
            bf.add("{}");
            bf.expire(180, TimeUnit.DAYS);
        }
        return bf;
    }

    private void smartKey() {
        var cur = LocalDate.now().format(monthKeyFormatter);
        var first = deque.peekFirst();
        if (StringUtils.hasText(first) && !cur.equals(first.substring(0, 6))) {
            var appendBF = LocalDate.now().format(monthKeyFormatter);
            deque.offerFirst(appendBF);
            var deleteBF = deque.pollLast();
            log.info("############## append filter: " + appendBF + ", delete bloom filter: " + deleteBF);
        }
    }

    public void enable(Boolean en) {
        log.debug("trans_bloom_filter -> enable: " + en);
        enableUse = Objects.requireNonNullElse(en, false);
    }

    public static boolean validDate(LocalDate cur) {
        if (cur == null) {
            return false;
        }
        return deque.stream().anyMatch(s -> s.equals(cur.format(monthKeyFormatter)));
    }
}
