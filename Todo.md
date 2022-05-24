# Todo

- [ ] In case of too much logging, truncate

- [x] signalBackgroundJobServerStopped
- [x] Id for recurring job
- [x] When dashboard server is stopped, stop sse handlers so that timer is stopped
- [ ] Test JobStats for Gson and JsonB
- [ ] Website:
  - [x] Explain difference between static BackgroundJob and jobscheduler access
  - [x] Fix typo Alexander: BackgroundJob.enqueue should be jobScheduler.enqueue
  - [ ] Add examples about Quarkus
- [ ] GraalVM
  - [ ] Add support for JobDetails
  - [ ] What about platformmbean?
  - [ ] Cleanup StaticUrl stuff