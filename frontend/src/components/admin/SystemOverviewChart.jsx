import React from 'react';
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

const BAR_COLORS = {
  users: '#0284c7',
  admins: '#6d28d9',
  centers: '#d97706',
  drives: '#dc2626',
  slots: '#059669'
};

export default function SystemOverviewChart({ data = [], onItemClick }) {
  return (
    <div style={{ width: '100%', height: 320 }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 12, right: 12, left: -18, bottom: 4 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="rgba(148, 163, 184, 0.24)" />
          <XAxis dataKey="label" tickLine={false} axisLine={false} />
          <YAxis allowDecimals={false} tickLine={false} axisLine={false} />
          <Tooltip
            cursor={{ fill: 'rgba(14, 165, 233, 0.08)' }}
            contentStyle={{
              borderRadius: '0.85rem',
              border: '1px solid rgba(148, 163, 184, 0.2)',
              boxShadow: '0 16px 40px rgba(15, 23, 42, 0.08)'
            }}
          />
          <Bar
            dataKey="value"
            radius={[12, 12, 0, 0]}
            barSize={42}
            onClick={(payload) => onItemClick?.(payload)}
            style={{ cursor: onItemClick ? 'pointer' : 'default' }}
          >
            {data.map((entry) => (
              <Cell key={entry.key} fill={BAR_COLORS[entry.key] || '#0284c7'} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
