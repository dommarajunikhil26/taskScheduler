package com.nikhil.taskscheduler.dto;

import com.nikhil.taskscheduler.dao.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface JobMapper {
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "retryCount", ignore = true)
    @Mapping(target = "workerId", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "lastHeartbeat", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Job toEntity(JobRequestDto jobRequestDto);
    JobResponseDto toDto(Job job);
}
