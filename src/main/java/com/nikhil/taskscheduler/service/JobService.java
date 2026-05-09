package com.nikhil.taskscheduler.service;

import com.nikhil.taskscheduler.dao.Job;
import com.nikhil.taskscheduler.dto.JobMapper;
import com.nikhil.taskscheduler.dto.JobRequestDto;
import com.nikhil.taskscheduler.dto.JobResponseDto;
import com.nikhil.taskscheduler.repository.JobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final Counter jobSubmittedCounter;

    public JobService(JobRepository jobRepository, JobMapper jobMapper, MeterRegistry meterRegistry) {
        this.jobRepository=jobRepository;
        this.jobMapper=jobMapper;
        this.jobSubmittedCounter = Counter.builder("jobs.submitted.total")
                .description("Total Jobs Submitted")
                .register(meterRegistry);
    }

    public JobResponseDto postJob(JobRequestDto jobRequestDto){
        Job savedJob = jobRepository.save(jobMapper.toEntity(jobRequestDto));
        this.jobSubmittedCounter.increment();
        return jobMapper.toDto(savedJob);
    }

    public JobResponseDto getJobStatus(UUID id){
        Optional<Job> job = jobRepository.findById(id);
        return job.map(jobMapper::toDto).orElse(null);
    }
}
