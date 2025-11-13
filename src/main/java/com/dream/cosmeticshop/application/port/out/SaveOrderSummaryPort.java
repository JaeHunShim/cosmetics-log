package com.dream.cosmeticshop.application.port.out;

import com.dream.cosmeticshop.domain.Order;

public interface SaveOrderSummaryPort
{
    void save(Order order);

}
