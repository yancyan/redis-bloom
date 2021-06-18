package com.example.redisbloom.enums;

public enum SocialChannelTransactionDataSourceType {

    /**
     * 正常交易
     */
    NORMAL,

    /**
     * 接口冲正
     */
    INTERFACE_REVERSE,

    /**
     * BOSS冲正
     */
    BOSS_REVERSE,

    /**
     * BOSS重做
     */
    BOSS_REDO,

    /**
     * 金额修正
     */
    AMEND_REFUND,

    /**
     * 补做
     */
    BOSS_REPAIR,

    /**
     * 托收充值
     */
    DEBIT,

    /**
     * 托收冲正
     */
    DEBIT_REVERSE,

    /**
     * 后台自动补做（对账）
     */
    AUTO_REPAIR;

    public static SocialChannelTransactionDataSourceType valueOf(int ordinal) {
        SocialChannelTransactionDataSourceType[] values = SocialChannelTransactionDataSourceType.values();
        if (ordinal >= values.length) {
            return null;
        } else {
            return values[ordinal];
        }
    }
}
