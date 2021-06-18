package com.example.redisbloom;

import com.example.redisbloom.dto.SocialChannel;
import com.example.redisbloom.enums.SocialChannelTransactionDataSourceType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * stateless provider...
 *
 * @author ZhuYX
 * @date 2021/06/16
 */
@Slf4j
public class BloomFilterProvider implements Runnable {

    @Resource
    RedissonClient redissonClient;
    @Resource
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public static final int STARTED = 1;
    public static final int PROCESSED = 2;
    public static final int END = 3;

    // public static final List<SocialChannelTransactionDataSourceType> reverseDateSourceTypes = Arrays.asList(
    //         SocialChannelTransactionDataSourceType.INTERFACE_REVERSE,
    //         SocialChannelTransactionDataSourceType.BOSS_REVERSE, SocialChannelTransactionDataSourceType.DEBIT_REVERSE);

    BloomFilterProvider() {
        BeanFactoryProvider.autowire(this);
    }

    @Override
    public void run() {
        var state = redissonClient.getAtomicLong(TransBloomFilter.TRANS_STATE);
        var taskSet = redissonClient.<LocalDate>getSet(TransBloomFilter.TRANS_TASK_SET);
        for (; ; ) {
            // phase pre.
            if (!state.isExists()) {
                state.set(0);
            }
            if (state.get() == END) {
                return;
            }

            // phase #1: provider tasks.
            if (state.get() == 0 && state.compareAndSet(0, STARTED)) {
                log.info("init bloom filter start-time is " + System.currentTimeMillis());

                var dates = LongStream.rangeClosed(0, 180)
                        .mapToObj(this::getValidLocalDate).filter(Objects::nonNull).collect(Collectors.toList());
                log.info("init bloom filter valid date is [" + dates + "]");

                taskSet.addAll(dates);
                state.compareAndSet(STARTED, PROCESSED);
            }
            // phase #2: task consumer.
            else if (state.get() == PROCESSED) {

                var taskLocalDate = taskSet.removeRandom();
                try {
                    doProvider(taskLocalDate);
                } catch (Exception e) {
                    log.error("############# ", e);

                    taskSet.add(taskLocalDate);
                    state.set(PROCESSED);
                }

                if (taskSet.isEmpty()) {
                    state.compareAndSet(PROCESSED, END);
                    log.info("init bloom filter end-time is " + System.currentTimeMillis());
                }
            }
            // phase #3: end.
            else if (state.get() == END) {
                TransBloomFilter.getShareInstance().enable(true);
                return;
            }
        }
    }

    private LocalDate getValidLocalDate(long s) {
        var date = LocalDate.now().minusDays(s);
        if (TransBloomFilter.validDate(date)) {
            return date;
        }
        log.debug("the date invalid and skipped. " + date);
        return null;
    }

    public static final String sql = "select sct.company_id, sct.transaction_no, sct.partner_staff_id " +
            "from boss_collection.social_channel_transaction sct " +
            "where sct.create_instant >= :startCreateInstant and sct.create_instant <= :endCreateInstant and sct.status = 1 " +
            "and sct.data_source_type not in (1, 2, 7) and sct.partner_staff_id is not null and sct.transaction_no is not null ";


    private void doProvider(LocalDate cur) {
        if (cur == null) {
            return;
        }

        var yearMonth = cur.format(DateTimeFormatter.ofPattern("yyyyMM"));

        var start = LocalDateTime.of(cur, LocalTime.MIN).toInstant(ZoneOffset.UTC);
        var end = LocalDateTime.of(cur, LocalTime.MAX).toInstant(ZoneOffset.UTC);
        var params = Map.of("startCreateInstant", Timestamp.from(start),
                "endCreateInstant", Timestamp.from(end));
        log.info("process local-date " + cur + " ->  params " + params);

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger countTotal = new AtomicInteger(0);
        // fixme: parse upgrade.(解析性能是否需要优化？？？)
        var mapper = BeanPropertyRowMapper.newInstance(SocialChannelMapper.class);
        namedParameterJdbcTemplate.query(sql, params, rs -> {
            var sc = mapper.mapRow(rs, 0);
            if (sc != null) {
                TransBloomFilter.getShareInstance().append(yearMonth, sc);
                count.incrementAndGet();
            }
            countTotal.incrementAndGet();
        });
        log.info("process local-date " + cur + " -> query total: " + countTotal.get() + ", insert bloom-filter total: " + count.get());
    }

    @Getter
    @Setter
    @ToString(callSuper = true)
    static class SocialChannelMapper extends SocialChannel {

        private Long id;
        /**
         * 数据来源类型
         */
        private SocialChannelTransactionDataSourceType dataSourceType;
    }

}
