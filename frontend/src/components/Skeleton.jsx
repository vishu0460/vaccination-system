import React from 'react';

// Skeleton loader for cards
export const SkeletonCard = ({ height = '200px', width = '100%', borderRadius = '12px' }) => {
  return (
    <div 
      className="skeleton-card"
      style={{ 
        height, 
        width, 
        borderRadius,
        background: 'linear-gradient(90deg, var(--bg-secondary) 25%, var(--bg-primary) 50%, var(--bg-secondary) 75%)',
        backgroundSize: '200% 100%',
        animation: 'skeleton-loading 1.5s infinite'
      }}
    />
  );
};

// Skeleton loader for text lines
export const SkeletonText = ({ lines = 3, spacing = '10px' }) => {
  return (
    <div className="skeleton-text">
      {Array.from({ length: lines }).map((_, index) => (
        <div 
          key={index}
          className="skeleton-line"
          style={{ 
            height: '12px', 
            width: index === lines - 1 ? '60%' : '100%',
            marginBottom: index < lines - 1 ? spacing : '0',
            background: 'linear-gradient(90deg, var(--bg-secondary) 25%, var(--bg-primary) 50%, var(--bg-secondary) 75%)',
            backgroundSize: '200% 100%',
            animation: 'skeleton-loading 1.5s infinite',
            borderRadius: '4px'
          }}
        />
      ))}
    </div>
  );
};

// Skeleton loader for table rows
export const SkeletonTable = ({ rows = 5, columns = 4 }) => {
  return (
    <div className="skeleton-table">
      {/* Table Header */}
      <div className="d-flex" style={{ gap: '10px', marginBottom: '10px' }}>
        {Array.from({ length: columns }).map((_, index) => (
          <div 
            key={`header-${index}`}
            style={{ 
              flex: 1, 
              height: '20px', 
              background: 'linear-gradient(90deg, var(--bg-secondary) 25%, var(--bg-primary) 50%, var(--bg-secondary) 75%)',
              backgroundSize: '200% 100%',
              animation: 'skeleton-loading 1.5s infinite',
              borderRadius: '4px'
            }}
          />
        ))}
      </div>
      {/* Table Rows */}
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <div 
          key={`row-${rowIndex}`}
          className="d-flex" 
          style={{ gap: '10px', marginBottom: '10px' }}
        >
          {Array.from({ length: columns }).map((_, colIndex) => (
            <div 
              key={`cell-${rowIndex}-${colIndex}`}
              style={{ 
                flex: 1, 
                height: '16px', 
                background: 'linear-gradient(90deg, var(--bg-secondary) 25%, var(--bg-primary) 50%, var(--bg-secondary) 75%)',
                backgroundSize: '200% 100%',
                animation: 'skeleton-loading 1.5s infinite',
                borderRadius: '4px'
              }}
            />
          ))}
        </div>
      ))}
    </div>
  );
};

// Skeleton loader for avatar
export const SkeletonAvatar = ({ size = '40px' }) => {
  return (
    <div 
      className="skeleton-avatar"
      style={{ 
        width: size, 
        height: size, 
        borderRadius: '50%',
        background: 'linear-gradient(90deg, var(--bg-secondary) 25%, var(--bg-primary) 50%, var(--bg-secondary) 75%)',
        backgroundSize: '200% 100%',
        animation: 'skeleton-loading 1.5s infinite'
      }}
    />
  );
};

// Skeleton loader for button
export const SkeletonButton = ({ width = '100px', height = '38px' }) => {
  return (
    <div 
      className="skeleton-button"
      style={{ 
        width, 
        height, 
        borderRadius: '8px',
        background: 'linear-gradient(90deg, var(--bg-secondary) 25%, var(--bg-primary) 50%, var(--bg-secondary) 75%)',
        backgroundSize: '200% 100%',
        animation: 'skeleton-loading 1.5s infinite'
      }}
    />
  );
};

// Dashboard skeleton
export const DashboardSkeleton = () => {
  return (
    <div className="dashboard-skeleton">
      {/* Stats Cards */}
      <div className="row mb-4">
        {[1, 2, 3].map((i) => (
          <div className="col-md-4 mb-3" key={i}>
            <SkeletonCard height="120px" />
          </div>
        ))}
      </div>
      
      {/* Drives Section */}
      <h4 className="mb-3" style={{ width: '200px', height: '24px', borderRadius: '4px' }}></h4>
      <div className="row">
        {[1, 2, 3].map((i) => (
          <div className="col-md-4 mb-3" key={i}>
            <SkeletonCard height="250px" />
          </div>
        ))}
      </div>
      
      {/* Table Section */}
      <h4 className="mb-3 mt-4" style={{ width: '150px', height: '24px', borderRadius: '4px' }}></h4>
      <SkeletonTable rows={5} columns={5} />
    </div>
  );
};

// Page skeleton for general pages
export const PageSkeleton = () => {
  return (
    <div className="page-skeleton">
      <SkeletonText lines={1} spacing="20px" />
      <div style={{ height: '20px' }} />
      <SkeletonCard height="300px" />
      <div style={{ height: '20px' }} />
      <SkeletonTable rows={5} columns={3} />
    </div>
  );
};

// Add CSS animation for skeleton loading
const skeletonStyles = `
  @keyframes skeleton-loading {
    0% {
      background-position: 200% 0;
    }
    100% {
      background-position: -200% 0;
    }
  }
  
  .skeleton-fade {
    animation: skeleton-pulse 2s ease-in-out infinite;
  }
  
  @keyframes skeleton-pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.5;
    }
  }
`;

// Inject styles
if (typeof document !== 'undefined') {
  const styleSheet = document.createElement('style');
  styleSheet.textContent = skeletonStyles;
  document.head.appendChild(styleSheet);
}

export default {
  SkeletonCard,
  SkeletonText,
  SkeletonTable,
  SkeletonAvatar,
  SkeletonButton,
  DashboardSkeleton,
  PageSkeleton
};
