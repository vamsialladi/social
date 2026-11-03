import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from '../components/navigation/Navbar';
import Footer from '../components/layout/Footer';

export default function MainLayout() {
  return (
    <div className="page-wrapper" style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar />
      <main style={{ flex: '1 0 auto', display: 'flex', flexDirection: 'column' }}>
        <Outlet />
      </main>
      <Footer />
    </div>
  );
}
