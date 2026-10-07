package com.edstem.interviewprep.order.repository;

import java.util.UUID;

import com.edstem.interviewprep.order.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update InventoryItem i set i.stock = i.stock - :quantity where i.id = :id and i.stock >= :quantity")
	int reserve(@Param("id") UUID id, @Param("quantity") long quantity);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update InventoryItem i set i.stock = i.stock + :quantity where i.id = :id")
	int release(@Param("id") UUID id, @Param("quantity") long quantity);
}
