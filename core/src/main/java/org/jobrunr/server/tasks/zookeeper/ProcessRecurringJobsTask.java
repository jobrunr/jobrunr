package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;

public class ProcessRecurringJobsTask extends AbstractJobZooKeeperTask {

    private final Map<String, Instant> recurringJobRuns;
    private RecurringJobsResult recurringJobs;
    Map<String,Long> existingById;


    public ProcessRecurringJobsTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.recurringJobRuns = new HashMap<>();
        this.recurringJobs = new RecurringJobsResult();
    }

    @Override
    protected void runTask() {
        LOGGER.trace("Looking for recurring jobs... ");

        Instant from = runStartTime();
        Instant upUntil = runStartTime().plus(backgroundJobServerConfiguration().getPollInterval());
        List<RecurringJob> recurringJobs = getRecurringJobs();
        existingById = fetchExistingCounts();
        convertAndProcessManyJobs(recurringJobs,
                recurringJob -> toScheduledJobs(recurringJob, from, upUntil),
                totalAmountOfJobs -> LOGGER.debug("Found {} jobs to schedule from {} recurring jobs", totalAmountOfJobs, recurringJobs.size()));
    }


    private long calculateHash(List<RecurringJob> jobs) {
        return jobs.stream()
                .map(recurringJob -> recurringJob.getCreatedAt().toEpochMilli())
                .reduce(Long::sum)
                .orElse(0L);
    }

    private List<RecurringJob> getRecurringJobs() {
        if (recurringJobs == null || recurringJobs.isEmpty()) {
            // first time, just fetch all
            this.recurringJobs = storageProvider.getRecurringJobs();
            return recurringJobs;
        }

        // Added this logic to avoid refetching even the hash. 
        if (!storageProvider.recurringJobsUpdated(recurringJobs.getLastModifiedHash())) {
            System.out.println("🏳️ NO NEED TO FETCH");
            return recurringJobs;
        }
    
        // make a mutable copy and sort by createdAt ascending
        List<RecurringJob> mutable = new ArrayList<>(recurringJobs);
        // no need to do as it is already sorted from SQL
        // mutable.sort(Comparator.comparingLong(j -> j.getCreatedAt().toEpochMilli()));
    
        // determine our paging window: from the earliest job we know about…
        long windowStart = mutable.get(0).getCreatedAt().toEpochMilli();
        // …up to now, in 5‑minute increments
        long now      = System.currentTimeMillis();
        long interval = 10 * 60 * 1000; // 10 minutes
        // Boolean fetchJobs = true;
        // I'm not sure if this is the best way to do this, unable to check if it works, but check once
        while (windowStart < now ) {
            long windowEnd = Math.min(windowStart + interval, now);
    
            System.out.println("🏳️ Window: " + windowStart + " to " + windowEnd);
            // pick out only the local jobs in this time slice
            // copy into final locals for the lambda
            final long ws = windowStart;
            final long we = windowEnd;

            List<RecurringJob> localSlice = mutable.stream()
                .filter(j -> { 
                    long ts = j.getCreatedAt().toEpochMilli();
                    return ts >= ws && ts < we;
                })
                .collect(Collectors.toList());
    
            System.out.println("🏳️ LOCAL page size: " + localSlice  .size());
            long localHash = calculateHash(localSlice);
            long dbHash    = storageProvider.recurringJobsUpdatedHash(windowStart, windowEnd);
            if (localHash != dbHash) {
                System.out.println("====== Hash at offset: " + localHash);
                System.out.println("====== Hash from db: " + dbHash);
                System.out.println("🚨 Hash mismatch at offset " + windowStart + ": will fetch fresh page.");
            } else {
                System.out.println("====== Hash at offset: " + localHash);
                System.out.println("====== Hash from db: " + dbHash);
                System.out.println("✅ Hash matches at offset " + windowStart + ": no need to fetch.");
            }

            if (localHash != dbHash) {

                // fetch only that 5‑minute batch
                List<RecurringJob> fresh = storageProvider.getRecurringJobsPage(windowStart, windowEnd);
                System.out.println("🏳️ Fresh page size: " + fresh.size());
                // replace in existing recurringJobs list
                    // find the range in the current list that belongs to [ws,we)
                int startIdx = firstIndexOfTimestamp(mutable, windowStart);
                int endIdx   = firstIndexOfTimestamp(mutable, windowEnd);

                // remove the old slice
                for (int i = startIdx; i < endIdx; i++) {
                    mutable.remove(startIdx); // each removal shifts the rest left
                }
                // insert all the fresh jobs at startIdx
                mutable.addAll(startIdx, fresh);
            }
    
            windowStart = windowEnd;
        }
    
        this.recurringJobs = new RecurringJobsResult(mutable);
        return mutable;
    }
    
    /** 
     * Find the first index in list where createdAt ≥ target.
     * If all < target, returns list.size().
     */
    private int firstIndexOfTimestamp(List<RecurringJob> list, long target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getCreatedAt().toEpochMilli() >= target) {
                return i;
            }
        }
        return list.size();
    }
    
    
    // private List<RecurringJob> getRecurringJobs() {
    //     if (recurringJobs == null || recurringJobs.isEmpty()) {
    //         // first time - fetch everything
    //         System.out.println("🏳️ 🏳️ 🏳️ 🏳️ First time fetching all recurring jobs");
    //         this.recurringJobs = storageProvider.getRecurringJobs();
    //         System.out.println("🏳️ First time fetch size: " + recurringJobs.size());
    //         return this.recurringJobs;
    //     }
    
    //     // A MUTABLE COPY
    //     List<RecurringJob> mutableRecurringJobs = new ArrayList<>(recurringJobs);

    //     int pageSize = 100;
    //     int offset = 0;
    //     int totalJobs = mutableRecurringJobs.size();
    
    //     while (offset < totalJobs) {            

    //         List<RecurringJob> subList = mutableRecurringJobs.subList(offset, Math.min(offset + pageSize, totalJobs));
    //         System.out.println("🏳️ Offset: " + offset + ", PageSize: " + pageSize + ", TotalJobs: " + totalJobs);
    //         System.out.println("🏳️ Sublist size: " + subList.size());
            
    //         long localSubListHash = calculateHash(subList);
    //         // Last page special case to check if we need to fetch fresh jobs
    //         if (offset + pageSize >= totalJobs) {
    //             // last page, remove limit
    //             pageSize = 10000000; //We handle this as no limit in the SQL query - 10 million jobs, increase this if needed
    //         }
    //         long dbSubListHash = storageProvider.recurringJobsUpdatedHash(offset, pageSize);
            
    //         if (localSubListHash != dbSubListHash) {
    //             System.out.println("🚨 Hash mismatch at offset " + offset + ": will fetch fresh page.");
    //         } else {
    //             System.out.println("✅ Hash matches at offset " + offset + ": no need to fetch.");
    //         }

    //         if (localSubListHash != dbSubListHash) {
    //             // Fetch only this page fresh
    //             List<RecurringJob> freshPage = storageProvider.getRecurringJobsPage(offset, pageSize); //pagesize is limit basically
    
    //             System.out.println("🏳️ Fresh page size: " + freshPage.size());
    //             // replace in existing recurringJobs list
    //             for (int i = 0; i < freshPage.size(); i++) {
    //                 if (offset + i < mutableRecurringJobs.size()) {
    //                     System.out.println("🏳️ Replacing job at index " + (offset + i));
    //                     mutableRecurringJobs.set(offset + i, freshPage.get(i));
    //                 } else {
    //                     System.out.println("🏳️ Adding job at index " + (offset + i));
    //                     mutableRecurringJobs.add(freshPage.get(i));
    //                 }
    //             }
    //         }
    
    //         offset += pageSize;
    //     }
        
    //     this.recurringJobs = new RecurringJobsResult(mutableRecurringJobs);
    //     return mutableRecurringJobs;
    // }
    
    // private List<RecurringJob> getRecurringJobs() {
    //     if (storageProvider.recurringJobsUpdated(recurringJobs.getLastModifiedHash())) {
    //         this.recurringJobs = storageProvider.getRecurringJobs();
    //     }
    //     return this.recurringJobs;
    // }

    List<Job> toScheduledJobs(RecurringJob recurringJob, Instant from, Instant upUntil) {
        List<Job> jobsToSchedule = getJobsToSchedule(recurringJob, from, upUntil);
        if (jobsToSchedule.isEmpty()) {
            LOGGER.trace("Recurring job '{}' resulted in 0 scheduled job.", recurringJob.getJobName());
        } else if (jobsToSchedule.size() > 1) {
            LOGGER.info("Recurring job '{}' resulted in {} scheduled jobs. This means a long GC happened and JobRunr is catching up.", recurringJob.getJobName(), jobsToSchedule.size());
        } else if (isAlreadyScheduledEnqueuedOrProcessing(recurringJob)) {
            // if the job is already scheduled, enqueued or processing, we skip this run
            LOGGER.info("Recurring job '{}' is already scheduled, enqueued or processing. Run will be skipped as job is taking longer than given CronExpression or Interval.", recurringJob.getJobName());
            jobsToSchedule.clear();
        } else if (jobsToSchedule.size() == 1) {
            System.out.println("🏳️ Recurring job '" + recurringJob.getId() + "has intances in jobrunr_jobs? - " + isAlreadyScheduledEnqueuedOrProcessing(recurringJob));
            LOGGER.debug("Recurring job '{}' resulted in 1 scheduled job.", recurringJob.getJobName());
        }
        registerRecurringJobRun(recurringJob, upUntil);
        return jobsToSchedule;
    }

    private List<Job> getJobsToSchedule(RecurringJob recurringJob, Instant runStartTime, Instant upUntil) {
        Instant lastRun = recurringJobRuns.getOrDefault(recurringJob.getId(), runStartTime);
        return recurringJob.toScheduledJobs(lastRun, upUntil);
    }

    private Map<String, Long> fetchExistingCounts() {
        Map<String, Long> existingById = new HashMap<>();
        System.out.println("🏳️ Fetching existing counts...");
        existingById = storageProvider.recurringJobsExists(SCHEDULED, ENQUEUED, PROCESSING);
        System.out.println("🏳️ Existing counts: " + existingById);
        return existingById;
    }

    // private boolean isAlreadyScheduledEnqueuedOrProcessing(RecurringJob recurringJob) {
    //     return storageProvider.recurringJobExists(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING);
    // }

    private boolean isAlreadyScheduledEnqueuedOrProcessing(RecurringJob recurringJob) {
        // true if we saw ≥1 existing job for this ID
        return existingById.getOrDefault(recurringJob.getId(), 0L) > 0;
    }
    

    private void registerRecurringJobRun(RecurringJob recurringJob, Instant upUntil) {
        recurringJobRuns.put(recurringJob.getId(), upUntil);
    }
}
