import React, { useState, useEffect } from 'react';
import { newsAPI, unwrapApiData } from '../api/client';
import Skeleton from '../components/Skeleton';
import EmptyState from '../components/EmptyState';

export default function NewsPage() {
  const [news, setNews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedNews, setSelectedNews] = useState(null);

  const formatNewsDate = (item) => {
    const value = item?.publishedAt || item?.updatedAt || item?.createdAt;
    if (!value) {
      return "";
    }

    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? "" : parsed.toLocaleDateString();
  };

  useEffect(() => {
    fetchNews();
  }, []);

  useEffect(() => {
    const handleDataUpdated = () => {
      fetchNews();
    };

    window.addEventListener('vaxzone:data-updated', handleDataUpdated);
    return () => window.removeEventListener('vaxzone:data-updated', handleDataUpdated);
  }, []);

  const fetchNews = async () => {
    try {
      const response = await newsAPI.getAllNews(0, 20);
      const payload = unwrapApiData(response) || [];
      setNews(Array.isArray(payload) ? payload : []);
    } catch (err) {
      console.error('Failed to fetch news');
      setNews([]);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container py-5">
        <div className="row">
          {[1, 2, 3].map((i) => (
            <div key={i} className="col-md-4 mb-4">
              <Skeleton height="250px" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (news.length === 0) {
    return (
      <div className="container py-5">
        <EmptyState
          title="No News Available"
          description="Check back later for updates and announcements."
        />
      </div>
    );
  }

  return (
    <div className="container py-5">
      <h2 className="mb-4">Latest News & Announcements</h2>
      <div className="row">
        {news.map((item) => (
          <div key={item.id} className="col-md-4 mb-4">
            <div className="card h-100 shadow-sm news-card" onClick={() => setSelectedNews(item)}>
              {item.imageUrl && (
                <img src={item.imageUrl} className="card-img-top" alt={item.title} style={{ height: '180px', objectFit: 'cover' }} />
              )}
              <div className="card-body">
                <span className="badge bg-primary mb-2">{item.category}</span>
                <h5 className="card-title">{item.title}</h5>
                <p className="card-text text-muted">{item.content?.substring(0, 100)}...</p>
                <small className="text-muted">
                  {item.createdAt ? new Date(item.createdAt).toLocaleString() : formatNewsDate(item)}
                </small>
              </div>
            </div>
          </div>
        ))}
      </div>

      {selectedNews && (
        <div className="modal show d-block" tabIndex="-1" onClick={() => setSelectedNews(null)}>
          <div className="modal-dialog modal-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">{selectedNews.title}</h5>
                <button type="button" className="btn-close" onClick={() => setSelectedNews(null)}></button>
              </div>
              <div className="modal-body">
                {selectedNews.imageUrl && (
                  <img src={selectedNews.imageUrl} className="img-fluid mb-3" alt={selectedNews.title} />
                )}
                <p>{selectedNews.content}</p>
                <small className="text-muted">
                  Published: {selectedNews.createdAt ? new Date(selectedNews.createdAt).toLocaleString() : formatNewsDate(selectedNews)}
                </small>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
