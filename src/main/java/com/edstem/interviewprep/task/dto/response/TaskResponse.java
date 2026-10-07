package com.edstem.interviewprep.task.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.edstem.interviewprep.task.entity.Task;
import com.edstem.interviewprep.task.entity.TaskStatus;

public record TaskResponse(
		UUID id,
		String title,
		String description,
		TaskStatus status,
		LocalDate dueDate,
		Instant createdAt) {

	public static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getDueDate(),
				task.getCreatedAt());
	}
}
