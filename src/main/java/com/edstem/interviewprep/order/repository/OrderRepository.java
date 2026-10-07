package com.edstem.interviewprep.order.repository;

import java.util.Optional;
import java.util.UUID;

import com.edstem.interviewprep.order.entity.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

	@EntityGraph(attributePaths = "items")
	Optional<CustomerOrder> findWithItemsById(UUID id);

	@EntityGraph(attributePaths = "items")
	Optional<CustomerOrder> findByIdempotencyKey(String idempotencyKey);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update CustomerOrder o set o.status = com.edstem.interviewprep.order.entity.OrderStatus.CANCELLED "
			+ "where o.id = :id and o.status = com.edstem.interviewprep.order.entity.OrderStatus.PLACED")
	int markCancelled(@Param("id") UUID id);
}
