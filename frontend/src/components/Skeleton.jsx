export default function Skeleton({ width, height, borderRadius = '4px' }) {
  return (
    <div 
      className="skeleton"
      style={{
        width: width || '100%',
        height: height || '20px',
        borderRadius: borderRadius,
        background: 'linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%)',
        backgroundSize: '200% 100%',
        animation: 'skeleton-loading 1.5s infinite'
      }}
    />
  );
}

export function SkeletonCard() {
  return (
    <div className="card skeleton-card">
      <Skeleton height="200px" borderRadius="4px 4px 0 0" />
      <div className="card-body">
        <Skeleton width="70%" height="24px" />
        <Skeleton width="100%" height="16px" />
        <Skeleton width="60%" height="16px" />
      </div>
    </div>
  );
}

export function SkeletonTable({ rows = 5 }) {
  return (
    <div className="table-responsive">
      <table className="table">
        <thead>
          <tr>
            <th><Skeleton width="80px" /></th>
            <th><Skeleton width="120px" /></th>
            <th><Skeleton width="100px" /></th>
            <th><Skeleton width="80px" /></th>
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: rows }).map((_, i) => (
            <tr key={i}>
              <td><Skeleton width="80px" /></td>
              <td><Skeleton width="120px" /></td>
              <td><Skeleton width="100px" /></td>
              <td><Skeleton width="80px" /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
