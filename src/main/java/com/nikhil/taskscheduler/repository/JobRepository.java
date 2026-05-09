package com.nikhil.taskscheduler.repository;

import com.nikhil.taskscheduler.dao.Job;
import com.nikhil.taskscheduler.dao.Status;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.scheduledAt <= :now")
    List<Job> findReadyJobs(@Param("status") Status status, @Param("now") Instant now);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.workerId = :workerId")
    List<Job> findRunningJobsByWorkedId(@Param("status") Status status, @Param("workerId") String workerId);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.lastHeartbeat < :threshold")
    List<Job> findStuckJobs(@Param("status") Status status, @Param("threshold") Instant threshold);

    long countByStatus(Status status);
}
