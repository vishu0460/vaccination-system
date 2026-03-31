import React, { useMemo, useState } from "react";
import moment from "moment";
import { Calendar, Views, momentLocalizer } from "react-big-calendar";
import "react-big-calendar/lib/css/react-big-calendar.css";

const localizer = momentLocalizer(moment);

const statusCopy = {
  ACTIVE: "Available now",
  UPCOMING: "Upcoming",
  FULL: "Fully booked",
  EXPIRED: "Expired"
};

const getEventTone = (event) => {
  const status = String(event?.resource?.status || "").toUpperCase();
  const availableSlots = Number(event?.resource?.availableSlots || 0);

  if (status === "EXPIRED") {
    return {
      backgroundColor: "#94a3b8",
      borderColor: "#64748b",
      color: "#0f172a"
    };
  }
  if (status === "FULL" || availableSlots <= 0) {
    return {
      backgroundColor: "#ef4444",
      borderColor: "#dc2626",
      color: "#ffffff"
    };
  }
  if (availableSlots <= 5) {
    return {
      backgroundColor: "#f59e0b",
      borderColor: "#d97706",
      color: "#111827"
    };
  }
  return {
    backgroundColor: "#10b981",
    borderColor: "#059669",
    color: "#ffffff"
  };
};

function SlotEvent({ event }) {
  const resource = event?.resource || {};
  const tooltip = [
    event.title,
    `${statusCopy[String(resource.status || "UPCOMING").toUpperCase()] || resource.status || "Upcoming"}`,
    `Center: ${resource.centerName || "N/A"}`,
    `Vaccine: ${resource.vaccineType || "General Vaccination"}`,
    `Available: ${resource.availableSlots ?? 0}`
  ].join("\n");

  return (
    <div className="slot-calendar-event" title={tooltip}>
      <div className="slot-calendar-event__title">{event.title}</div>
      <div className="slot-calendar-event__meta">{resource.centerName || "Center unavailable"}</div>
    </div>
  );
}

export default function SlotCalendar({ events = [], onSelectEvent }) {
  const [view, setView] = useState(Views.WEEK);
  const [date, setDate] = useState(new Date());

  const components = useMemo(() => ({
    event: SlotEvent
  }), []);

  return (
    <div className="slot-calendar-shell">
      <div className="slot-calendar-shell__legend">
        <span><i className="bi bi-circle-fill text-success me-2" />Available</span>
        <span><i className="bi bi-circle-fill text-warning me-2" />Limited</span>
        <span><i className="bi bi-circle-fill text-danger me-2" />Full</span>
        <span><i className="bi bi-circle-fill text-secondary me-2" />Expired</span>
      </div>

      <div className="slot-calendar-shell__frame">
        <Calendar
          localizer={localizer}
          events={events}
          startAccessor="start"
          endAccessor="end"
          view={view}
          onView={setView}
          date={date}
          onNavigate={setDate}
          views={[Views.DAY, Views.WEEK]}
          step={30}
          timeslots={2}
          popup
          selectable={false}
          tooltipAccessor={(event) => event.title}
          eventPropGetter={(event) => ({
            style: {
              ...getEventTone(event),
              borderRadius: "14px",
              borderWidth: "1px",
              boxShadow: "0 12px 28px rgba(15, 23, 42, 0.08)",
              padding: "0.15rem 0.35rem"
            }
          })}
          components={components}
          onSelectEvent={(event) => onSelectEvent?.(event.resource)}
        />
      </div>
    </div>
  );
}
