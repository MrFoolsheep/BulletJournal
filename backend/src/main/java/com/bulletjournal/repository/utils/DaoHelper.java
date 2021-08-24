package com.bulletjournal.repository.utils;

import com.bulletjournal.controller.utils.ZonedDateTimeHelper;
import com.bulletjournal.daemon.models.ReminderRecord;
import com.bulletjournal.repository.models.Task;
import com.bulletjournal.repository.models.Transaction;
import com.bulletjournal.util.BuJoRecurrenceRule;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;

public class DaoHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DaoHelper.class);

    public static <T> void updateIfPresent(Boolean isPresent, T value, Consumer<T> getter) {
        if (isPresent) {
            getter.accept(value);
        }
    }

    /**
     * Get all reminder records from given task
     * - For recurring task, return list of ReminderRecord in [startTime, endTime]
     * - For one-time task, return list of single or zero ReminderRecord
     *
     * @param task      the given task
     * @param startTime the ZonedDateTime object of start time
     * @param endTime   the ZonedDateTime object of end time
     * @return List<ReminderRecord> - a list of reminder record
     */
    public static List<ReminderRecord> getReminderRecords(Task task, ZonedDateTime startTime, ZonedDateTime endTime) {
        return new ArrayList<>(getReminderRecordMap(task, startTime, endTime).keySet());
    }

    public static Map<ReminderRecord, Task> getReminderRecordMap(Task task, ZonedDateTime startTime, ZonedDateTime endTime) {
        Map<ReminderRecord, Task> map = new HashMap<>();
        if (Objects.isNull(task.getRecurrenceRule())) {
            LOGGER.info("Get reminder record map: task id " + task.getId()
                    + " reminder date time " + task.getReminderDateTime());
            if (task.getReminderDateTime() != null) {
                map.put(new ReminderRecord(task.getId(), task.getReminderDateTime().getTime()), task);
            }
        } else {
            List<Task> recurringTasks = getRecurringTask(task, startTime, endTime);
            recurringTasks.forEach(t -> {
                if (t == null || !t.hasReminderDateTime()) {
                    LOGGER.error("getReminderRecordMap error on {}", t);
                } else {
                    map.put(new ReminderRecord(t.getId(), t.getReminderDateTime().getTime()), t);
                }
            });
        }
        return map;
    }

    /**
     * Fetch all recurring within [startTime, endTime] based on transaction's recurrence rule
     *
     * @param transactions the target list of transaction contains recurrence rule
     * @param startTime    the requested time range starting time
     * @param endTime      the requested time range ending time
     * @return List<Task> - a list of task based on recurrence rule
     */
    public static List<Transaction> getRecurringTransactions(List<Transaction> transactions, ZonedDateTime startTime, ZonedDateTime endTime) {
        List<Transaction> result = new ArrayList<>();
        transactions.forEach(ts -> result.addAll(getRecurringTransaction(ts, startTime, endTime)));
        return result;
    }

    /**
     * Fetch all recurring within [startTime, endTime] based on transaction's recurrence rule
     *
     * @param transaction the target transaction contains recurrence rule
     * @param startTime   the requested time range starting time
     * @param endTime     the requested time range ending time
     * @return List<Task> - a list of task based on recurrence rule
     */
    public static List<Transaction> getRecurringTransaction(Transaction transaction, ZonedDateTime startTime, ZonedDateTime endTime) {
        try {
            DateTime startDateTime = ZonedDateTimeHelper.getDateTime(startTime);
            DateTime endDateTime = ZonedDateTimeHelper.getDateTime(endTime);

            List<Transaction> recurringTransactionsBetween = new ArrayList<>();
            String recurrenceRule = transaction.getRecurrenceRule();
            String timezone = transaction.getTimezone();
            Set<String> deletedSlots = ZonedDateTimeHelper.parseDateTimeSet(transaction.getDeletedSlots());

            BuJoRecurrenceRule rule = new BuJoRecurrenceRule(recurrenceRule, timezone);
            RecurrenceRuleIterator it = rule.getIterator();

            while (it.hasNext()) {
                DateTime currDateTime = it.nextDateTime();
                if (currDateTime.after(endDateTime)) {
                    break;
                }
                if (currDateTime.before(startDateTime) || deletedSlots.contains(currDateTime.toString())) {
                    continue;
                }
                Transaction cloned = cloneTransactionWithDateTime(transaction, timezone, currDateTime);
                recurringTransactionsBetween.add(cloned);
            }
            return recurringTransactionsBetween;
        } catch (InvalidRecurrenceRuleException | NumberFormatException e) {
            throw new IllegalArgumentException("Recurrence rule format invalid");
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone new Task failed");
        }
    }

    /**
     * Fetch all recurring within [startTime, endTime] based on task's recurrence rule
     *
     * @param task      the target task contains recurrence rule
     * @param startTime the requested time range starting time
     * @param endTime   the requested time range ending time
     * @return List<Task> - a list of task based on recurrence rule
     */
    public static List<Task> getRecurringTask(Task task, ZonedDateTime startTime, ZonedDateTime endTime) {
        try {
            DateTime startDateTime = ZonedDateTimeHelper.getDateTime(startTime);
            DateTime endDateTime = ZonedDateTimeHelper.getDateTime(endTime);

            List<Task> recurringTasksBetween = new ArrayList<>();
            String recurrenceRule = task.getRecurrenceRule();
            String timezone = task.getTimezone();
            Set<String> completedSlots = ZonedDateTimeHelper.parseDateTimeSet(task.getCompletedSlots());

            BuJoRecurrenceRule rule = new BuJoRecurrenceRule(recurrenceRule, timezone);
            RecurrenceRuleIterator it = rule.getIterator();

            while (it.hasNext()) {
                DateTime currDateTime = it.nextDateTime();
                LOGGER.info("Recurring Task id: "
                        + task.getId() + " rule: " + recurrenceRule
                        + " current time: " + currDateTime.toString()
                        + " start time: " + startDateTime.toString()
                        + " end time: " + startDateTime.toString()
                );
                if (currDateTime.after(endDateTime)) {
                    break;
                }
                if (currDateTime.before(startDateTime) || completedSlots.contains(currDateTime.toString())) {
                    continue;
                }
                Task cloned = cloneTaskWithDateTime(task, timezone, currDateTime);
                recurringTasksBetween.add(cloned);
            }
            return recurringTasksBetween;
        } catch (InvalidRecurrenceRuleException | NumberFormatException e) {
            throw new IllegalArgumentException("Recurrence rule format invalid");
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone new Task failed");
        }
    }

    /**
     * Clone a new task and set input timestamp to [DueDate, DueTime] and [StartTime, EndTime]
     *
     * @param task            the target task needs to be cloned
     * @param timestampMillis the input timestamp for task Due DateTime
     * @return Task a task cloned from original task with new Due DateTime and Timezone
     */
    @VisibleForTesting
    public static Task cloneTaskWithDueDateTime(Task task, Long timestampMillis) {
        try {
            DateTime taskDateTime = ZonedDateTimeHelper.getDateTime(timestampMillis, task.getTimezone());
            return cloneTaskWithDateTime(task, task.getTimezone(), taskDateTime);
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone new Task failed");
        }
    }

    /**
     * Clone a new transaction and set RFC 5545 DateTime as its [DueDate, DueTime] and [StartTime, EndTime]
     * Only used for recurring transaction
     *
     * @param transaction  the target transaction needs to cloned
     * @param timezone     the target timezone for cloned task's due date and due time
     * @param currDateTime the RFC 5545 DateTime Object contains timing information
     * @return Transaction a transaction cloned from original task with new Due DateTime and Timezone
     * @throws CloneNotSupportedException
     */
    private static Transaction cloneTransactionWithDateTime(Transaction transaction, String timezone, DateTime currDateTime) throws CloneNotSupportedException {
        if (StringUtils.isBlank(transaction.getRecurrenceRule())) {
            LOGGER.error("Transaction {} does not have RecurrenceRule", transaction);
            throw new IllegalArgumentException("Transaction " + transaction.getId() + " does not have RecurrenceRule");
        }
        Transaction cloned = (Transaction) transaction.clone();

        String defaultDate = ZonedDateTimeHelper.getDate(currDateTime);
        String defaultTime = ZonedDateTimeHelper.getTime(currDateTime);

        // Shift to task's timezone
        ZonedDateTime targetDate = ZonedDateTimeHelper.getStartTime(defaultDate, defaultTime, timezone);
        ZonedDateTime targetTime = ZonedDateTimeHelper.getEndTime(defaultDate, defaultTime, timezone);

        // Set due date and time
        cloned.setDate(ZonedDateTimeHelper.getDate(targetDate));
        cloned.setTime(ZonedDateTimeHelper.getTime(targetTime));

        // Set start time and end time
        cloned.setStartTime(Timestamp.from(targetDate.toInstant()));
        cloned.setEndTime(Timestamp.from(targetTime.toInstant()));

        // Set timezone
        cloned.setTimezone(timezone);
        return cloned;
    }

    /**
     * Clone a new task and set RFC 5545 DateTime as its [DueDate, DueTime] and [StartTime, EndTime]
     * Only used for recurring task
     *
     * @param task         the target task needs to cloned
     * @param timezone     the target timezone for cloned task's due date and due time
     * @param currDateTime the RFC 5545 DateTime Object contains timing information
     * @return Task a task cloned from original task with new Due DateTime and Timezone
     * @throws CloneNotSupportedException
     */
    private static Task cloneTaskWithDateTime(Task task, String timezone, DateTime currDateTime) throws CloneNotSupportedException {
        if (StringUtils.isBlank(task.getRecurrenceRule())) {
            LOGGER.error("Task {} does not have RecurrenceRule", task);
            throw new IllegalArgumentException("Task " + task.getId() + " does not have RecurrenceRule");
        }
        Task cloned = (Task) task.clone();

        String defaultDate = ZonedDateTimeHelper.getDate(currDateTime);
        String defaultTime = ZonedDateTimeHelper.getTime(currDateTime);

        // Shift to task's timezone
        ZonedDateTime targetDate = ZonedDateTimeHelper.getStartTime(defaultDate, defaultTime, timezone);
        ZonedDateTime targetTime = ZonedDateTimeHelper.getEndTime(defaultDate, defaultTime, timezone);

        // Set due date and time
        cloned.setDueDate(ZonedDateTimeHelper.getDate(targetDate));
        cloned.setDueTime(ZonedDateTimeHelper.getTime(targetTime));

        // Set start time and end time
        cloned.setStartTime(Timestamp.from(targetDate.toInstant()));
        cloned.setEndTime(Timestamp.from(targetTime.toInstant()));

        // Set timezone
        cloned.setTimezone(timezone);

        cloned.setReminderSetting(task.getReminderSetting()); // Set reminding setting to cloned

        if (!cloned.hasReminderDateTime()) {
            LOGGER.warn("Task {} ReminderDateTime is null", task.getReminderSetting());
        }
        return cloned;
    }
}
