package com.bulletjournal.calendars.google;

import com.bulletjournal.clients.UserClient;
import com.bulletjournal.controller.models.Content;
import com.bulletjournal.controller.models.ReminderSetting;
import com.bulletjournal.controller.models.Task;
import com.bulletjournal.controller.models.User;
import com.bulletjournal.controller.models.params.CreateTaskParams;
import com.bulletjournal.controller.utils.ZonedDateTimeHelper;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.dmfs.rfc5545.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Converter {
    private static final int DEFAULT_REMINDER_SETTING = 30;
    private static final String INSERT_STR_FORMAT = "{\"insert\":\"%s\"},";
    private static final String INSERT_LINE_BREAK = "{\"insert\":\"\\n\"},";
    public static final Logger LOGGER = LoggerFactory.getLogger(Converter.class);
    private static final Document.OutputSettings HTML_DOC_OUTPUT_SETTINGS = new Document.OutputSettings();

    public static GoogleCalendarEvent toTask(Event event, String timezone) {
        String username = MDC.get(UserClient.USER_NAME_KEY);
        LOGGER.info("GoogleCalendarEvent: {}", event);
        Task task = new Task();
        task.setOwner(new User(username));
        task.setAssignees(ImmutableList.of(new User(username)));
        task.setName(event.getSummary());
        task.setTimezone(timezone);

        EventDateTime startEventDateTime = event.getStart();
        EventDateTime endEventDateTime = event.getEnd();
        Long startDateTimeValue = getValue(startEventDateTime);
        if (startDateTimeValue != null) {
            setTaskRecurrence(task, timezone, event.getRecurrence(), startDateTimeValue);

            Long endDateTimeValue = getValue(endEventDateTime);
            if (endDateTimeValue != null) {
                task.setDuration((int) TimeUnit.MILLISECONDS.toMinutes(endDateTimeValue - startDateTimeValue));
            }

            if (startEventDateTime.getDate() != null) {
                task.setDueDate(startEventDateTime.getDate().toString());
            } else if (startEventDateTime.getDateTime() != null) {
                task.setDueDate(startEventDateTime.getDateTime().toString().substring(0, 10));
                task.setDueTime(startEventDateTime.getDateTime().toString().substring(11, 16));
            }

            setTaskReminder(task, timezone, event.getReminders(), startDateTimeValue);
        }

        Content content = new Content();
        content.setText(getText(event, task));
        content.setBaseText(getBaseText(event, task));
        content.setOwner(new User(username));
        content.setEditable(true);
        content.setDeletable(true);

        return new GoogleCalendarEvent(task, content, event.getId(),
                processHtmlTags(event.getDescription()));
    }

    private static String processHtmlTags(String strHTML) {
        if (StringUtils.isBlank(strHTML)) {
            return strHTML;
        }
        //create Jsoup document from HTML
        Document jsoupDoc = Jsoup.parse(strHTML);

        //set pretty print to false, so \n is not removed
        jsoupDoc.outputSettings(HTML_DOC_OUTPUT_SETTINGS.prettyPrint(false));

        //select all <br> tags and append \n after that
        jsoupDoc.select("br").after("\\n");

        //select all <p> tags and prepend \n before that
        jsoupDoc.select("p").before("\\n");

        //get the HTML from the document, and retaining original new lines
        String str = jsoupDoc.html().replaceAll("\\\\n", "\n").replaceAll("&nbsp;", "\t");

        String strWithNewLines =
                Jsoup.clean(str, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false)).replaceAll("&lt;", "<").replaceAll("&gt;", ">");

        LOGGER.info("Html Tags processed: {}", strWithNewLines);
        return strWithNewLines;
    }

    private static String getBaseText(Event event, Task task) {
        StringBuilder baseText = new StringBuilder("[");

        appendDescription(baseText, event.getDescription());

        appendLocation(baseText, event.getLocation());

        appendAttendees(baseText, event.getAttendees());

        baseText.append("{\"insert\":\"\\n\"}]");
        return baseText.toString();
    }

    private static void appendDescription(StringBuilder baseText, String description) {
        if (description != null) {
            String processedDescription = processHtmlTags(description);
            String[] lines = processedDescription.split("[\\r\\n]+");

            for (String line : lines) {
                if (!line.isEmpty()) {
                    baseText.append(String.format(INSERT_STR_FORMAT, line.replace("\"", "\\\"")))
                            .append(INSERT_LINE_BREAK);
                }
            }
        }
    }

    private static void appendLocation(StringBuilder baseText, String location) {
        if (location != null) {
            baseText.append(INSERT_LINE_BREAK)
                    .append(String.format(INSERT_STR_FORMAT, "Location: "))
                    .append(String.format(INSERT_STR_FORMAT, location))
                    .append(INSERT_LINE_BREAK);
        }
    }

    private static void appendAttendees(StringBuilder baseText, List<EventAttendee> attendeeList) {
        attendeeList = attendeeList != null ?
                attendeeList.stream().filter((a) -> StringUtils.isNotBlank(a.getDisplayName()))
                        .collect(Collectors.toList()) : Collections.emptyList();

        if (!attendeeList.isEmpty()) {
            baseText.append(INSERT_LINE_BREAK)
                    .append(String.format(INSERT_STR_FORMAT, "Attendees:"))
                    .append(INSERT_LINE_BREAK);

            for (EventAttendee attendee : attendeeList) {
                baseText.append(INSERT_LINE_BREAK)
                        .append(String.format(INSERT_STR_FORMAT, attendee.getDisplayName()));
            }
        }
    }


    private static String getText(Event event, Task task) {
        StringBuilder text = new StringBuilder();
        if (event.getDescription() != null) {
            text.append(event.getDescription().replace(System.lineSeparator(), "<br/>")
                    .replace("\"", "\\\""))
                    .append("<br/>").append(System.lineSeparator());
        }
        if (event.getLocation() != null) {
            task.setLocation(event.getLocation());
            text.append(System.lineSeparator()).append("<br/>")
                    .append("<b>Location:</b> ").append(event.getLocation())
                    .append(System.lineSeparator()).append("<br/>");
        }
        List<EventAttendee> attendeeList = event.getAttendees();
        attendeeList = attendeeList != null ?
                attendeeList.stream().filter((a) -> StringUtils.isNotBlank(a.getDisplayName()))
                        .collect(Collectors.toList()) : Collections.emptyList();
        if (!attendeeList.isEmpty()) {
            text.append(System.lineSeparator()).append("<br/>")
                    .append("<b>Attendees:</b>").append("<br/>");
            for (EventAttendee attendee : attendeeList) {
                text.append(System.lineSeparator()).append("<br/>");
                if (StringUtils.isBlank(attendee.getEmail())) {
                    text.append(attendee.getDisplayName());
                } else {
                    text.append("<a href=\\\"mailto:").append(attendee.getEmail()).append("\\\" target=\\\"_blank\\\">")
                            .append(attendee.getDisplayName()).append("</a>");
                }
                text.append(System.lineSeparator()).append("<br/>");
            }
        }

        return text.toString();
    }

    private static Long getValue(EventDateTime eventDateTime) {
        if (eventDateTime == null) {
            return null;
        }

        if (eventDateTime.getDateTime() != null) {
            return eventDateTime.getDateTime().getValue();
        }

        if (eventDateTime.getDate() != null) {
            return eventDateTime.getDate().getValue();
        }

        return null;
    }

    private static void setTaskReminder(Task task, String timezone, Event.Reminders reminders, long startDateTimeValue) {
        if (reminders == null) {
            return;
        }
        ReminderSetting reminderSetting = new ReminderSetting();
        if (reminders.getUseDefault()) {
            DateTime reminderDateTime = ZonedDateTimeHelper.getDateTime(startDateTimeValue - TimeUnit.MINUTES.toMillis(DEFAULT_REMINDER_SETTING), timezone);
            reminderSetting.setDate(ZonedDateTimeHelper.getDate(reminderDateTime));
            reminderSetting.setTime(ZonedDateTimeHelper.getTime(reminderDateTime));
        } else if (reminders.getOverrides() != null && !reminders.getOverrides().isEmpty()) {
            List<EventReminder> eventReminderList = reminders.getOverrides();
            int minutes = 0;
            for (EventReminder eventReminder : eventReminderList) {
                minutes = Math.max(minutes, eventReminder.getMinutes());
            }
            DateTime reminderDateTime = ZonedDateTimeHelper.getDateTime(startDateTimeValue - TimeUnit.MINUTES.toMillis(minutes), timezone);
            reminderSetting.setDate(ZonedDateTimeHelper.getDate(reminderDateTime));
            reminderSetting.setTime(ZonedDateTimeHelper.getTime(reminderDateTime));
        }
        task.setReminderSetting(reminderSetting);
    }

    /**
     * Set recurrence of event to task, rRule complies with rfc5545
     * rRule:
     * RRULE:FREQ=DAILY;UNTIL=20200724T065959Z
     */
    private static void setTaskRecurrence(Task task, String timezone, List<String> rRule, long startDateTimeValue) {
        if (rRule != null && !rRule.isEmpty()) {
            DateTime startDateTime = ZonedDateTimeHelper.getDateTime(startDateTimeValue, timezone);
            task.setRecurrenceRule("DTSTART:" + startDateTime.toString() + " " + rRule.get(0));
        }
    }

    public static CreateTaskParams toCreateTaskParams(GoogleCalendarEvent event) {
        Task task = event.getTask();
        return new CreateTaskParams(task.getName(), task.getDueDate(),
                task.getDueTime(), task.getDuration(), task.getReminderSetting(),
                task.getAssignees().stream().map(a -> a.getName()).collect(Collectors.toList()),
                task.getTimezone(), task.getRecurrenceRule(), Collections.emptyList(), task.getLocation());
    }
}
