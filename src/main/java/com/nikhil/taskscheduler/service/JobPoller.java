package com.nikhil.taskscheduler.service;

import com.nikhil.taskscheduler.config.RedisConfig;
import com.nikhil.taskscheduler.dao.Job;
import com.nikhil.taskscheduler.dao.Status;
import com.nikhil.taskscheduler.repository.JobRepository;
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
    @Value("${worker.id}")
    private String workerId;
    public JobPoller(JobRepository jobRepository, RedisTemplate<String, String> redisTemplate){
        this.jobRepository=jobRepository;
        this.redisTemplate=redisTemplate;
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
                    System.out.println("Executing job");
                    job.setStatus(Status.COMPLETED);
                    job.setCompletedAt(Instant.now());
                    jobRepository.save(job);
                }catch (Exception e){
                    job.setRetryCount(job.getRetryCount() + 1);
                    if (job.getRetryCount() >= job.getMaxRetries()) {
                        job.setStatus(Status.FAILED);
                        job.setCompletedAt(Instant.now());
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
