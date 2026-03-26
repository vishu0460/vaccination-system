import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { getErrorMessage, publicAPI, unwrapApiData } from "../api/client";
import { debugDataSync, subscribeToDataUpdates } from "../utils/dataSync";

const PublicCatalogContext = createContext(null);

const DRIVE_PAGE_SIZE = 200;

const EMPTY_SUMMARY = {
  totalCenters: 0,
  activeDrives: 0,
  availableSlots: 0
};

export function PublicCatalogProvider({ children }) {
  const [drives, setDrives] = useState([]);
  const [summary, setSummary] = useState(EMPTY_SUMMARY);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const refreshCatalog = async () => {
    setLoading(true);

    try {
      const [drivesRes, summaryRes] = await Promise.all([
        publicAPI.getDrives({ page: 0, size: DRIVE_PAGE_SIZE }),
        publicAPI.getSummary()
      ]);

      const drivesPayload = unwrapApiData(drivesRes) || {};
      const driveItems = Array.isArray(drivesPayload)
        ? drivesPayload
        : (drivesPayload.drives || []);
      debugDataSync("public catalog drives", driveItems);
      setDrives(driveItems);

      const summaryPayload = unwrapApiData(summaryRes) || {};
      setSummary({
        totalCenters: summaryPayload.totalCenters || summaryPayload.centersCount || 0,
        activeDrives: summaryPayload.activeDrives || summaryPayload.drivesCount || 0,
        availableSlots: summaryPayload.availableSlots || 0
      });
      setError("");
    } catch (requestError) {
      setError(getErrorMessage(requestError, "Unable to load drive catalog right now."));
      setDrives([]);
      setSummary(EMPTY_SUMMARY);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshCatalog();
    const unsubscribe = subscribeToDataUpdates(() => refreshCatalog());

    return () => {
      unsubscribe();
    };
  }, []);

  const value = useMemo(() => ({
    drives,
    summary,
    loading,
    error,
    refreshCatalog
  }), [drives, summary, loading, error]);

  return (
    <PublicCatalogContext.Provider value={value}>
      {children}
    </PublicCatalogContext.Provider>
  );
}

export function usePublicCatalog() {
  const context = useContext(PublicCatalogContext);
  if (!context) {
    throw new Error("usePublicCatalog must be used within PublicCatalogProvider");
  }
  return context;
}
