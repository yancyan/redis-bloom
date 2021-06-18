package com.example.redisbloom.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author ZhuYX
 * @date 2021/06/15
 */
@Getter @Setter @ToString
public class SocialChannel {
    private String transactionNo;
    private Long partnerStaffId;
    private Long companyId;
}
