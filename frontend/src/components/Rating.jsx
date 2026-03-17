import React, { useState } from 'react';

export default function Rating({ value = 0, onChange, readonly = false, size = 'md' }) {
  const [hover, setHover] = useState(0);
  
  const sizes = {
    sm: '16px',
    md: '24px',
    lg: '32px'
  };

  const starSize = sizes[size] || sizes.md;

  return (
    <div className="rating">
      {[1, 2, 3, 4, 5].map((star) => (
        <span
          key={star}
          className={`star ${star <= (hover || value) ? 'filled' : ''} ${!readonly ? 'clickable' : ''}`}
          style={{ 
            fontSize: starSize,
            cursor: readonly ? 'default' : 'pointer',
            color: star <= (hover || value) ? '#ffc107' : '#ddd',
            transition: 'color 0.2s'
          }}
          onClick={() => !readonly && onChange && onChange(star)}
          onMouseEnter={() => !readonly && setHover(star)}
          onMouseLeave={() => !readonly && setHover(0)}
        >
          ★
        </span>
      ))}
    </div>
  );
}
