package com.nikhil.taskscheduler.controller;

import com.nikhil.taskscheduler.dao.Job;
import com.nikhil.taskscheduler.dto.JobRequestDto;
import com.nikhil.taskscheduler.dto.JobResponseDto;
import com.nikhil.taskscheduler.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService){
        this.jobService=jobService;
    }
    @PostMapping
    public ResponseEntity<JobResponseDto> postJob(@RequestBody @Valid JobRequestDto jobRequestDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.postJob(jobRequestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDto> getJobStatus(@PathVariable UUID id){
        JobResponseDto response = jobService.getJobStatus(id);
        if(response != null){
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }
}
