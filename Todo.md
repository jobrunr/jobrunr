# Todo

- [ ] In case of too much logging, truncate
- [ ] Pro only: allow to set amount of cycles if CPUAllocationInfo problem occurs
- [ ] Use code below to provide more info about stop the world GC in dashboard and github SevereJobRunrException
- [ ] JobFilters in different threads


```java
try{
        List<GarbageCollectorMXBean> gcMxBeans=ManagementFactory.getGarbageCollectorMXBeans();

        for(GarbageCollectorMXBean gcMxBean:gcMxBeans){
        System.out.println(gcMxBean.getName());
        System.out.println(gcMxBean.getObjectName());
        }
        }catch(RuntimeException re){
        throw re;
        }catch(Exception exp){
        throw new RuntimeException(exp);
        }
```
