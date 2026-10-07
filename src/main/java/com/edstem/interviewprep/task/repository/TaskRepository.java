package com.edstem.interviewprep.task.repository;

import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.task.entity.Task;
import com.edstem.interviewprep.task.entity.TaskStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	List<Task> findByStatus(TaskStatus status, Sort sort);
}
