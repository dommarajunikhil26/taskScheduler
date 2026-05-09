package com.nikhil.taskscheduler.service;

import com.nikhil.taskscheduler.dao.Job;
import com.nikhil.taskscheduler.dto.JobMapper;
import com.nikhil.taskscheduler.dto.JobRequestDto;
import com.nikhil.taskscheduler.dto.JobResponseDto;
import com.nikhil.taskscheduler.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    public JobService(JobRepository jobRepository, JobMapper jobMapper){
        this.jobRepository=jobRepository;
        this.jobMapper=jobMapper;
    }

    public JobResponseDto postJob(JobRequestDto jobRequestDto){
        Job savedJob = jobRepository.save(jobMapper.toEntity(jobRequestDto));
        return jobMapper.toDto(savedJob);
    }

    public JobResponseDto getJobStatus(UUID id){
        Optional<Job> job = jobRepository.findById(id);
        return job.map(jobMapper::toDto).orElse(null);
    }
}
