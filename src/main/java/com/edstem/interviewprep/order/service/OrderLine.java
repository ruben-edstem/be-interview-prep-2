package com.edstem.interviewprep.order.service;

import java.util.UUID;

public record OrderLine(UUID productId, long quantity) {
}
