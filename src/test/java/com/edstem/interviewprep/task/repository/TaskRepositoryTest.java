package com.edstem.interviewprep.task.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import com.edstem.interviewprep.task.entity.Task;
import com.edstem.interviewprep.task.entity.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class TaskRepositoryTest {

	@Autowired
	private TaskRepository taskRepository;

	@Test
	void savePopulatesIdAndCreatedAt() {
		Task task = new Task("Write report", "Quarterly numbers", TaskStatus.TODO, LocalDate.now().plusDays(3));

		Task saved = taskRepository.saveAndFlush(task);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void findByStatusReturnsOnlyMatchingTasks() {
		taskRepository.save(new Task("Todo task", null, TaskStatus.TODO, null));
		taskRepository.save(new Task("Done task", null, TaskStatus.DONE, null));
		taskRepository.flush();

		List<Task> found = taskRepository.findByStatus(TaskStatus.DONE, Sort.by("createdAt"));

		assertThat(found).extracting(Task::getTitle).containsExactly("Done task");
	}
}
