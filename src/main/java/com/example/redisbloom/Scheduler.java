package com.example.redisbloom;

import com.example.redisbloom.dto.SocialChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowCountCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ZhuYX
 * @date 2021/06/15
 */
@Configuration
public class Scheduler {

    /**
     * @throws IOException
     */
    // @Scheduled(fixedDelay = 10_000)
    public void b() throws IOException {
        // var client = redissonClient();
        //
        // var bf = client.<String>getBloomFilter(
        //         filter_name + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        //
        // bf.tryInit(1_000_000, 0.00001);
        // bf.add("{}");
        // bf.expire(30, TimeUnit.SECONDS);
        //
        // var transactionNO = "abcccasldf";
        // for (; ; ) {
        //     bf.add(UUID.randomUUID().toString().replace("_", ""));
        //
        //     if (bf.count() > 10_000_000) {
        //         break;
        //     }
        // }

    }

    public static final String filter_name = "bf-13:";

    @Scheduled(fixedDelay = 10_000)
    public void autoCheck() throws IOException {
        System.out.println("######### start...............");
        // TransBloomFilter.getShareInstance().enable(true);
        AtomicLong count = new AtomicLong(0);

        for (; ; ) {
            StopWatch watch = new StopWatch();
            watch.start();
            var trans = UUID.randomUUID().toString().replace("_", "");
            var check = TransBloomFilter.getShareInstance().check(trans);
            count.incrementAndGet();
            watch.stop();
            if (!check) {
                System.out.println("######### " + trans + ": " + check + "(" + watch.getTotalTimeMillis() + "/millis)");
            }
            if (count.get() % 100 == 0) {
                System.out.println("############ " + count.get());
            }

        }
    }

    @Autowired
    NamedParameterJdbcTemplate template;

    // @Scheduled(fixedDelay = 10_000)
    public void initBloom() {

        var sql = "select sct.company_id, sct.TRANSACTION_NO, sct.PARTNER_STAFF_ID from boss_collection.social_channel_transaction sct " +
                "where sct.create_Instant >= :start_create_instant and sct.create_Instant <= :end_create_instant";


        // DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss xxx").withZone(ZoneId.systemDefault()).format(Instant.now())
            var start = LocalDateTime.of(LocalDate.now().minusMonths(7), LocalTime.MIN).toInstant(ZoneOffset.UTC);
            var end = LocalDateTime.of(LocalDate.now().minusMonths(7), LocalTime.MAX).toInstant(ZoneOffset.UTC);


        var params = Map.of("start_create_instant",  Timestamp.from(start),
                "end_create_instant", Timestamp.from(end));

        var mapper = BeanPropertyRowMapper.newInstance(SocialChannel.class);
        template.query(sql, params,
                new RowCountCallbackHandler() {
                    @Override
                    protected void processRow(ResultSet rs, int rowNum) throws SQLException {
                        var sc = mapper.mapRow(rs, rowNum);
                        System.out.println(sc);
                    }
                });



    }
}
