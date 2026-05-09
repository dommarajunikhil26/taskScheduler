# Distributed Task Scheduler

A production-grade distributed task scheduling system built with Java 21 and Spring Boot. Supports scheduling background jobs across multiple worker nodes with guaranteed exactly-once execution, automatic failure recovery, and real-time observability.

---

## Architecture

```
┌─────────────────┐         ┌──────────────────────┐
│   Client Apps   │──POST──▶│   Scheduler API      │
│ (submit jobs)   │         │   (Spring Boot)      │
└─────────────────┘         └──────────┬───────────┘
                                        │ writes job
                                        ▼
                             ┌──────────────────────┐
                             │     PostgreSQL        │
                             │  (jobs table with     │
                             │   status + schedule)  │
                             └──────────┬───────────┘
                                        │ polls ready jobs
                            ┌───────────▼────────────┐
                            │          Redis          │
                            │   (distributed locks)   │
                            └───────────┬────────────┘
                                        │
              ┌─────────────────────────┼──────────────────────┐
              ▼                         ▼                       ▼
     ┌────────────────┐      ┌────────────────┐      ┌────────────────┐
     │   Worker Node 1│      │   Worker Node 2│      │   Worker Node 3│
     │  (executes job)│      │  (skips - lock)│      │  (skips - lock)│
     └────────────────┘      └────────────────┘      └────────────────┘
```

---

## Key Technical Decisions

### Why Redis for Distributed Locking?
Each worker attempts to acquire a Redis lock using the atomic `SET NX EX` command before claiming a job. Since this operation is atomic, only one worker can win the race — guaranteeing exactly-once execution even when multiple nodes poll simultaneously. The lock has a 30-second TTL as a safety net in case a worker crashes mid-execution.

### Why PostgreSQL for Job Persistence?
Jobs need durable storage with queryable state. PostgreSQL's JSONB support allows flexible job payloads without schema changes, while indexed queries on `status` and `scheduled_at` keep polling efficient at scale.

### Heartbeat + Reaper Pattern
If a worker crashes mid-job, the job would be stuck in `RUNNING` forever. To prevent this:
- Every worker sends a **heartbeat** every 10 seconds, updating `lastHeartbeat` for all jobs it owns
- A **reaper** runs every 30 seconds across all workers, finds jobs whose `lastHeartbeat` is older than 60 seconds, and resets them to `PENDING` for retry

---

## Features

- **Exactly-once execution** across multiple worker nodes via Redis distributed locking
- **Configurable retries** — jobs retry up to `maxRetries` times before being marked `FAILED`
- **Dead worker recovery** — heartbeat + reaper pattern automatically recovers stuck jobs
- **Flexible payloads** — JSONB storage allows any job structure without schema changes
- **Real-time observability** — Prometheus metrics for queue depth, completed, failed, and submitted jobs
- **Horizontally scalable** — add worker nodes by scaling the Docker service

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4 |
| Database | PostgreSQL 16 |
| Cache / Locks | Redis 8 |
| ORM | Spring Data JPA + Hibernate |
| Mapping | MapStruct |
| Metrics | Micrometer + Prometheus |
| Containers | Docker + Docker Compose |

---

## API Reference

### Submit a Job
```
POST /jobs
```
```json
{
  "name": "send-welcome-email",
  "scheduledAt": "2026-05-09T17:30:00Z",
  "maxRetries": 3,
  "payload": {
    "userId": "abc123",
    "email": "user@example.com"
  }
}
```

**Response `201 Created`**
```json
{
  "uuid": "2de5dc90-619a-4d32-b2db-096921fceabf",
  "name": "send-welcome-email",
  "status": "PENDING",
  "scheduledAt": "2026-05-09T17:30:00Z",
  "createdAt": "2026-05-09T17:46:41.547190Z",
  "maxRetries": 3,
  "payload": {
    "userId": "abc123",
    "email": "user@example.com"
  }
}
```

### Get Job Status
```
GET /jobs/{id}
```

**Response `200 OK`**
```json
{
  "uuid": "2de5dc90-619a-4d32-b2db-096921fceabf",
  "name": "send-welcome-email",
  "status": "COMPLETED",
  "scheduledAt": "2026-05-09T17:30:00Z",
  "createdAt": "2026-05-09T17:46:41.547190Z",
  "maxRetries": 3,
  "payload": {
    "userId": "abc123",
    "email": "user@example.com"
  }
}
```

**Job Status Values**

| Status | Meaning |
|---|---|
| `PENDING` | Waiting to be picked up by a worker |
| `RUNNING` | Currently being executed |
| `COMPLETED` | Successfully executed |
| `FAILED` | Exhausted all retries |

---

## Prometheus Metrics

| Metric | Type | Description |
|---|---|---|
| `jobs_submitted_total` | Counter | Total jobs submitted via API |
| `jobs_completed_total` | Counter | Total jobs successfully completed |
| `jobs_failed_total` | Counter | Total jobs that exhausted retries |
| `jobs_queue_depth` | Gauge | Current number of PENDING jobs |

Metrics available at `GET /actuator/prometheus`

---

## Running Locally

### Prerequisites
- Docker + Docker Compose
- Java 21
- Gradle

### Start with 3 Worker Nodes

```bash
# Build the application
./gradlew build -x test

# Start PostgreSQL, Redis, and 3 worker replicas
docker compose up --scale app=3 --build -d

# Verify all containers are running
docker ps
```

You should see 5 containers: 3 app workers, 1 PostgreSQL, 1 Redis.

### Submit a Test Job

```bash
curl -X POST http://localhost:8080/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "test-job",
    "scheduledAt": "2026-05-09T17:30:00Z",
    "maxRetries": 3,
    "payload": {"message": "hello"}
  }'
```

### Verify Exactly-Once Execution

Check logs across all workers — only one should show `Executing job`:

```bash
docker logs task-scheduler-app-1 | grep "Executing"
docker logs task-scheduler-app-2 | grep "Executing"
docker logs task-scheduler-app-3 | grep "Executing"
```

### Check Job Status

```bash
curl http://localhost:8080/jobs/{uuid}
```

---

## How the Distributed Lock Works

```
Worker polls PostgreSQL for PENDING jobs due now
    │
    ▼
For each job:
    Try: SET lock:job:{id} {workerId} NX EX 30
         │
         ├── Got lock (true)  → mark RUNNING → execute → mark COMPLETED → release lock
         │                                                    │
         │                                              on failure:
         │                                              increment retryCount
         │                                              if retryCount < maxRetries → PENDING
         │                                              else → FAILED
         │
         └── No lock (false) → skip, another worker owns it
```

---

## Project Structure

```
src/main/java/com/nikhil/taskscheduler/
├── config/
│   └── RedisConfig.java          # RedisTemplate bean configuration
├── controller/
│   └── JobController.java        # REST endpoints
├── dao/
│   ├── Job.java                  # JPA entity
│   └── Status.java               # Job status enum
├── dto/
│   ├── JobMapper.java            # MapStruct mapper
│   ├── JobRequestDto.java        # Incoming job request
│   └── JobResponseDto.java       # Outgoing job response
├── repository/
│   └── JobRepository.java        # JPA repository + custom queries
└── service/
    ├── JobHeartbeat.java         # Heartbeat + reaper scheduled tasks
    ├── JobPoller.java            # Job polling + distributed lock execution
    └── JobService.java           # Job submission + status lookup
```