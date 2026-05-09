package com.nikhil.taskscheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

@SpringBootApplication
@EnableScheduling
public class TaskSchedulerApplication {
	private static Logger logger = LoggerFactory.getLogger(TaskSchedulerApplication.class);

	public static void main(String[] args) {
		String workerId = "worker-" + UUID.randomUUID();
		System.setProperty("worker.id", workerId);
		logger.info("Starting worker with ID: {}", workerId);
		SpringApplication.run(TaskSchedulerApplication.class, args);
	}

}
