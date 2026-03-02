import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Breadcrumb as BsBreadcrumb } from 'react-bootstrap';

const Breadcrumb = () => {
  const location = useLocation();
  
  // Define breadcrumb paths
  const getBreadcrumbs = () => {
    const paths = location.pathname.split('/').filter(path => path);
    const breadcrumbs = [{ label: 'Home', path: '/' }];
    
    let currentPath = '';
    paths.forEach((path) => {
      currentPath += `/${path}`;
      const label = path.charAt(0).toUpperCase() + path.slice(1).replace(/-/g, ' ');
      breadcrumbs.push({ label, path: currentPath });
    });
    
    return breadcrumbs;
  };

  const breadcrumbs = getBreadcrumbs();

  // Don't show breadcrumb on home page
  if (location.pathname === '/') {
    return null;
  }

  return (
    <BsBreadcrumb className="breadcrumb-nav mb-3">
      {breadcrumbs.map((crumb, index) => (
        <BsBreadcrumb.Item
          key={crumb.path}
          linkAs={Link}
          linkProps={{ to: crumb.path }}
          active={index === breadcrumbs.length - 1}
        >
          {index === 0 && '🏠 '}
          {crumb.label}
        </BsBreadcrumb.Item>
      ))}
    </BsBreadcrumb>
  );
};

// Custom breadcrumb styles
const breadcrumbStyles = `
  .breadcrumb-nav {
    background-color: var(--bg-secondary);
    padding: 0.75rem 1rem;
    border-radius: 8px;
    margin-bottom: 1rem;
  }
  
  .breadcrumb-nav .breadcrumb-item {
    font-size: 0.9rem;
  }
  
  .breadcrumb-nav .breadcrumb-item a {
    color: var(--accent-color);
    text-decoration: none;
    transition: color 0.2s ease;
  }
  
  .breadcrumb-nav .breadcrumb-item a:hover {
    color: #6610f2;
    text-decoration: underline;
  }
  
  .breadcrumb-nav .breadcrumb-item.active {
    color: var(--text-secondary);
    font-weight: 500;
  }
  
  .breadcrumb-nav .breadcrumb-item + .breadcrumb-item::before {
    content: "›";
    color: var(--text-secondary);
    font-size: 1.2rem;
  }
`;

if (typeof document !== 'undefined') {
  const styleSheet = document.createElement('style');
  styleSheet.textContent = breadcrumbStyles;
  document.head.appendChild(styleSheet);
}

export default Breadcrumb;
