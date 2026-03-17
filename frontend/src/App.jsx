import React from "react";
import AppRoutes from "./routes/AppRoutes";
import Navbar from "./components/Navbar";
import Footer from "./components/Footer";
import { ThemeProvider } from "./context/ThemeContext";

export default function App() {
  return (
    <ThemeProvider>
      <div className="app-shell d-flex flex-column min-vh-100">
        <Navbar />
        <main className="flex-grow-1" style={{ paddingTop: '76px' }}>
          <AppRoutes />
        </main>
        <Footer />
      </div>
    </ThemeProvider>
  );
}
