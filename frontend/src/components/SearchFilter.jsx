import React, { useState } from 'react';

export default function SearchFilter({ 
  onSearch, 
  onFilter, 
  searchPlaceholder = 'Search...',
  filters = [] 
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [activeFilters, setActiveFilters] = useState({});

  const handleSearch = (e) => {
    const value = e.target.value;
    setSearchTerm(value);
    onSearch && onSearch(value);
  };

  const handleFilterChange = (filterKey, value) => {
    const newFilters = { ...activeFilters, [filterKey]: value };
    setActiveFilters(newFilters);
    onFilter && onFilter(newFilters);
  };

  const clearFilters = () => {
    setSearchTerm('');
    setActiveFilters({});
    onSearch && onSearch('');
    onFilter && onFilter({});
  };

  const hasActiveFilters = Object.values(activeFilters).some(v => v && v !== '');

  return (
    <div className="search-filter-container mb-4">
      <div className="row g-3">
        <div className="col-md-6">
          <div className="input-group">
            <span className="input-group-text">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
            </span>
            <input
              type="text"
              className="form-control"
              placeholder={searchPlaceholder}
              value={searchTerm}
              onChange={handleSearch}
            />
            {searchTerm && (
              <button className="btn btn-outline-secondary" type="button" onClick={() => setSearchTerm('')}>
                ×
              </button>
            )}
          </div>
        </div>

        {filters.map((filter) => (
          <div key={filter.key} className="col-md-3">
            <select
              className="form-select"
              value={activeFilters[filter.key] || ''}
              onChange={(e) => handleFilterChange(filter.key, e.target.value)}
            >
              <option value="">{filter.label}</option>
              {filter.options.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        ))}

        {hasActiveFilters && (
          <div className="col-md-2">
            <button className="btn btn-outline-danger w-100" onClick={clearFilters}>
              Clear Filters
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
