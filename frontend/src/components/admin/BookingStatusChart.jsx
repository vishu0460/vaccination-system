import React from 'react';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';

export default function BookingStatusChart({ data = [], onItemClick }) {
  const total = data.reduce((sum, item) => sum + Number(item?.value || 0), 0);

  return (
    <div style={{ width: '100%', height: 320 }} className="dashboard-donut-chart">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="label"
            cx="50%"
            cy="50%"
            innerRadius={72}
            outerRadius={112}
            paddingAngle={4}
            onClick={(payload) => onItemClick?.(payload)}
            style={{ cursor: onItemClick ? 'pointer' : 'default' }}
          >
            {data.map((entry) => (
              <Cell key={entry.key} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip
            formatter={(value, name) => [value, name]}
            contentStyle={{
              borderRadius: '0.85rem',
              border: '1px solid rgba(148, 163, 184, 0.2)',
              boxShadow: '0 16px 40px rgba(15, 23, 42, 0.08)'
            }}
          />
        </PieChart>
      </ResponsiveContainer>

      <div className="dashboard-donut-chart__center">
        <span>Total</span>
        <strong>{total}</strong>
      </div>

      <div className="dashboard-donut-chart__legend">
        {data.map((item) => (
          <button
            key={item.key}
            type="button"
            className="dashboard-donut-chart__legend-item"
            onClick={() => onItemClick?.(item)}
          >
            <span className="dashboard-donut-chart__legend-dot" style={{ backgroundColor: item.color }} />
            <span>{item.label}</span>
            <strong>{item.value}</strong>
          </button>
        ))}
      </div>
    </div>
  );
}
