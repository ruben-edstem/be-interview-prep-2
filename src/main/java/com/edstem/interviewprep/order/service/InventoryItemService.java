package com.edstem.interviewprep.order.service;

import java.util.UUID;

import com.edstem.interviewprep.order.entity.InventoryItem;
import com.edstem.interviewprep.order.exception.InventoryItemNotFoundException;
import com.edstem.interviewprep.order.repository.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryItemService {

	private final InventoryItemRepository repository;

	public InventoryItemService(InventoryItemRepository repository) {
		this.repository = repository;
	}

	public InventoryItem create(String name, long stock) {
		return repository.save(new InventoryItem(name, stock));
	}

	@Transactional(readOnly = true)
	public InventoryItem get(UUID id) {
		return repository.findById(id).orElseThrow(() -> new InventoryItemNotFoundException(id));
	}
}
