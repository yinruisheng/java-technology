package com.littlefxc.example.quartz.service.impl;

import com.littlefxc.example.quartz.component.JobScheduleCreator;
import com.littlefxc.example.quartz.enitiy.SchedulerJob;
import com.littlefxc.example.quartz.repository.SchedulerRepository;
import com.littlefxc.example.quartz.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author fengxuechao
 * @date 12/19/2018
 */
@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
public class SchedulerServiceImpl implements SchedulerService {

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    private SchedulerRepository schedulerRepository;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JobScheduleCreator scheduleCreator;

    /**
     * 启动所有的在表scheduler_job_info中记录的job
     */
    @Override
    public void startAllSchedulers() {
        List<SchedulerJob> jobInfoList = schedulerRepository.findAll();
        if (jobInfoList != null) {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            jobInfoList.forEach(jobInfo -> {
                try {
                    JobDetail jobDetail = JobBuilder.newJob((Class<? extends QuartzJobBean>) Class.forName(jobInfo.getJobClass()))
                            .withIdentity(jobInfo.getJobName(), jobInfo.getJobGroup()).build();
                    if (!scheduler.checkExists(jobDetail.getKey())) {
                        Trigger trigger;
                        jobDetail = scheduleCreator.createJob((Class<? extends QuartzJobBean>) Class.forName(jobInfo.getJobClass()),
                                false, context, jobInfo.getJobName(), jobInfo.getJobGroup());

                        if (jobInfo.getCronJob() && CronExpression.isValidExpression(jobInfo.getCronExpression())) {
                            trigger = scheduleCreator.createCronTrigger(jobInfo.getJobName(), new Date(),
                                    jobInfo.getCronExpression(), CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
                        } else {
                            trigger = scheduleCreator.createSimpleTrigger(jobInfo.getJobName(), new Date(),
                                    jobInfo.getRepeatTime(), SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
                        }

                        scheduler.scheduleJob(jobDetail, trigger);

                    }
                } catch (ClassNotFoundException e) {
                    log.error("Class Not Found - {}", jobInfo.getJobClass(), e);
                } catch (SchedulerException e) {
                    log.error(e.getMessage(), e);
                }
            });
        }
    }

    @Override
    public void scheduleNewJob(SchedulerJob jobInfo) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();

            JobDetail jobDetail = JobBuilder.newJob((Class<? extends QuartzJobBean>) Class.forName(jobInfo.getJobClass()))
                    .withIdentity(jobInfo.getJobName(), jobInfo.getJobGroup()).build();
            if (!scheduler.checkExists(jobDetail.getKey())) {

                jobDetail = scheduleCreator.createJob((Class<? extends QuartzJobBean>) Class.forName(jobInfo.getJobClass()),
                        false, context, jobInfo.getJobName(), jobInfo.getJobGroup());

                Trigger trigger;
                if (jobInfo.getCronJob()) {
                    trigger = scheduleCreator.createCronTrigger(jobInfo.getJobName(), new Date(), jobInfo.getCronExpression(),
                            CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
                } else {
                    trigger = scheduleCreator.createSimpleTrigger(jobInfo.getJobName(), new Date(), jobInfo.getRepeatTime(),
                            SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
                }

                scheduler.scheduleJob(jobDetail, trigger);
                jobInfo.setSchedulerName(schedulerFactoryBean.getScheduler().getSchedulerName());
                schedulerRepository.save(jobInfo);
            } else {
                log.error("scheduleNewJobRequest.jobAlreadyExist");
            }
        } catch (ClassNotFoundException e) {
            log.error("Class Not Found - {}", jobInfo.getJobClass(), e);
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void updateScheduleJob(SchedulerJob jobInfo) {
        Trigger newTrigger;
        if (jobInfo.getCronJob()) {
            newTrigger = scheduleCreator.createCronTrigger(jobInfo.getJobName(), new Date(), jobInfo.getCronExpression(),
                    CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        } else {
            newTrigger = scheduleCreator.createSimpleTrigger(jobInfo.getJobName(), new Date(), jobInfo.getRepeatTime(),
                    SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
        }
        try {
            schedulerFactoryBean.getScheduler().rescheduleJob(TriggerKey.triggerKey(jobInfo.getJobName()), newTrigger);
            jobInfo.setSchedulerName(schedulerFactoryBean.getScheduler().getSchedulerName());
            schedulerRepository.save(jobInfo);
        } catch (SchedulerException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * unscheduleJob(TriggerKey triggerKey)只是不再调度触发器，所以，当其他的触发器引用了这个Job，它们不会被改变
     *
     * @param jobName
     * @return
     */
    @Override
    public boolean unScheduleJob(String jobName) {
        try {
            return schedulerFactoryBean.getScheduler().unscheduleJob(new TriggerKey(jobName));
        } catch (SchedulerException e) {
            log.error("Failed to un-schedule job - {}", jobName, e);
            return false;
        }
    }

    /**
     * deleteJob(JobKey jobKey):<br>
     * 1.循环遍历所有引用此Job的触发器，以取消它们的调度(to unschedule them)<br>
     * 2.从jobstore中删除Job
     *
     * @param jobName  job name
     * @param jobGroup job group
     * @return
     */
    @Override
    public boolean deleteJob(String jobName, String jobGroup) {
        try {
            JobKey jobKey = new JobKey(jobName, jobGroup);
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            boolean deleteJob = scheduler.deleteJob(jobKey);
            if (deleteJob) {
                scheduler.pauseJob(jobKey);
                String schedulerName = scheduler.getSchedulerName();
                SchedulerJob job = schedulerRepository.findSchedulerJob(schedulerName, jobName);
                schedulerRepository.delete(job);
            }
            return deleteJob;
        } catch (SchedulerException e) {
            log.error("Failed to delete job - {}", jobName, e);
            return false;
        }
    }

    /**
     * 暂停
     *
     * @param jobName  job name
     * @param jobGroup job group
     * @return
     */
    @Override
    public boolean pauseJob(String jobName, String jobGroup) {
        try {
            schedulerFactoryBean.getScheduler().pauseJob(new JobKey(jobName, jobGroup));
            return true;
        } catch (SchedulerException e) {
            log.error("Failed to pause job - {}", jobName, e);
            return false;
        }
    }

    /**
     * 恢复
     *
     * @param jobName  job name
     * @param jobGroup job group
     * @return
     */
    @Override
    public boolean resumeJob(String jobName, String jobGroup) {
        try {
            schedulerFactoryBean.getScheduler().resumeJob(new JobKey(jobName, jobGroup));
            return true;
        } catch (SchedulerException e) {
            log.error("Failed to resume job - {}", jobName, e);
            return false;
        }
    }

    @Override
    public boolean startJobNow(String jobName, String jobGroup) {
        try {
            schedulerFactoryBean.getScheduler().triggerJob(new JobKey(jobName, jobGroup));
            return true;
        } catch (SchedulerException e) {
            log.error("Failed to start new job - {}", jobName, e);
            return false;
        }
    }

    /**
     * 分页查询
     *
     * @param pageable
     * @param cron     true: cron trigger, false: simple trigger
     * @return
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)// 方法上注解属性会覆盖类注解上的相同属性
    @Override
    public Page<Map<String, Object>> findAll(Pageable pageable, Boolean cron) {
        try {
            String schedulerName = schedulerFactoryBean.getScheduler().getSchedulerName();
            if (cron) {
                return schedulerRepository.getJobWithCronTrigger(schedulerName, pageable);
            } else {
                return schedulerRepository.getJobWithSimpleTrigger(schedulerName, pageable);
            }
        } catch (SchedulerException e) {
            log.error("Failed to get scheduler name", e);
            return Page.empty();
        }
    }

    /**
     * 根据jobName查询单条记录
     *
     * @param jobName
     * @return
     */
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    @Override
    public SchedulerJob findOne(String jobName) {
        try {
            String schedulerName = schedulerFactoryBean.getScheduler().getSchedulerName();
            return schedulerRepository.findSchedulerJob(schedulerName, jobName);
        } catch (SchedulerException e) {
            log.error("Failed to get scheduler name", e);
            return null;
        }

    }

}
