package com.edstem.interviewprep.order.service;

import com.edstem.interviewprep.order.entity.CustomerOrder;

public record PlacedOrder(CustomerOrder order, boolean replayed) {
}
