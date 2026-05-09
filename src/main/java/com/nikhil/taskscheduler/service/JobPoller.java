package com.nikhil.taskscheduler.service;

import com.nikhil.taskscheduler.dao.Job;
import com.nikhil.taskscheduler.dao.Status;
import com.nikhil.taskscheduler.repository.JobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JobPoller {
    private final JobRepository jobRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(JobPoller.class);

    @Value("${worker.id}")
    private String workerId;
    private final Counter jobCompletedCounter;
    private final Counter jobFailedCounter;

    public JobPoller(JobRepository jobRepository, RedisTemplate<String, String> redisTemplate, MeterRegistry meterRegistry) {
        this.jobRepository=jobRepository;
        this.redisTemplate=redisTemplate;
        this.jobCompletedCounter = Counter.builder("jobs.completed.total")
                .description("Total number of jobs completed.")
                .register(meterRegistry);
        this.jobFailedCounter = Counter.builder("jobs.failed.total")
                .description("Total number of jobs failed.")
                .register(meterRegistry);
        Gauge.builder("jobs.queue.depth", jobRepository, repo -> repo.countByStatus(Status.PENDING))
                .description("Number of jobs in queue or PENDING state.")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 5000)
    public void poll(){
        List<Job> readyJobs = jobRepository.findReadyJobs(Status.PENDING, Instant.now());
        for(Job job: readyJobs){
            boolean accquired = Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent("lock:job:" + job.getId(),
                            workerId, 30,
                            TimeUnit.SECONDS));
            if(accquired){
                job.setStatus(Status.RUNNING);
                job.setWorkerId(workerId);
                job.setStartedAt(Instant.now());
                jobRepository.save(job);
                try{
                    logger.info("Executing job");
                    job.setStatus(Status.COMPLETED);
                    job.setCompletedAt(Instant.now());
                    this.jobCompletedCounter.increment();
                    jobRepository.save(job);
                }catch (Exception e){
                    job.setRetryCount(job.getRetryCount() + 1);
                    if (job.getRetryCount() >= job.getMaxRetries()) {
                        job.setStatus(Status.FAILED);
                        job.setCompletedAt(Instant.now());
                        jobFailedCounter.increment();
                    } else {
                        job.setStatus(Status.PENDING); // back to queue for retry
                        job.setWorkerId(null);
                        job.setStartedAt(null);
                    }
                    jobRepository.save(job);
                }finally {
                    redisTemplate.delete("lock:job:"+job.getId());
                }
            }
        }
    }
}
