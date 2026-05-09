package com.nikhil.taskscheduler.service;

import com.nikhil.taskscheduler.dao.Job;
import com.nikhil.taskscheduler.dao.Status;
import com.nikhil.taskscheduler.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class JobHeartbeat {
    private final JobRepository jobRepository;
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public JobHeartbeat(JobRepository jobRepository){
        this.jobRepository=jobRepository;
    }

    @Value("${worker.id}")
    private String workerId;

    @Scheduled(fixedDelay = 10000)
    public void heartbeat(){
        List<Job> runningJobs = jobRepository.findRunningJobsByWorkedId(Status.RUNNING, workerId);
        try{
            for(Job job: runningJobs){
                job.setLastHeartbeat(Instant.now());
                jobRepository.save(job);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void reap(){
        Instant threshold = Instant.now().minus(Duration.ofSeconds(60));
        List<Job> stuckJobs = jobRepository.findStuckJobs(Status.RUNNING, threshold);
        try{
            for(Job job: stuckJobs){
                job.setStatus(Status.PENDING);
                jobRepository.save(job);
            }
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }
}
