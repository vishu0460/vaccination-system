import React from "react";
import AppRoutes from "./routes/AppRoutes";
import Navbar from "./components/Navbar";
import Footer from "./components/Footer";
import { ThemeProvider } from "./context/ThemeContext";
import { PublicCatalogProvider } from "./context/PublicCatalogContext";

export default function App() {
  return (
    <ThemeProvider>
      <PublicCatalogProvider>
        <div className="app-shell d-flex flex-column min-vh-100">
          <Navbar />
          <main className="app-main flex-grow-1">
            <AppRoutes />
          </main>
          <Footer />
        </div>
      </PublicCatalogProvider>
    </ThemeProvider>
  );
}
