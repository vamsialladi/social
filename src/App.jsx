import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './layouts/MainLayout';
import HomePage from './pages/HomePage';
import EntertainmentsPage from './pages/EntertainmentsPage';
import TalentsPage from './pages/TalentsPage';
import TalentDetailsPage from './pages/TalentDetailsPage';
import ProfilePage from './pages/ProfilePage';
import JobsPage from './pages/JobsPage';
import JobDetailsPage from './pages/JobDetailsPage';
import InvestorsConnectPage from './pages/InvestorsConnectPage';
import LcmAppPage from './pages/LcmAppPage';
import GetSupportPage from './pages/GetSupportPage';
import SignInPage from './pages/SignInPage';
import JoinNowPage from './pages/JoinNowPage';
import PrivacyPolicyPage from './pages/PrivacyPolicyPage';
import TermsPage from './pages/TermsPage';
import ProtectProducerLandingPage from './pages/ProtectProducerLandingPage';
import ProtectCategoryPage from './pages/ProtectCategoryPage';
import ProjectDashboardPage from './pages/ProjectDashboardPage';
import ContactUsPage from './pages/ContactUsPage';
import AboutUsPage from './pages/AboutUsPage';
import NotFoundPage from './pages/NotFoundPage';
import ScrollToTop from './components/common/ScrollToTop';
import { AuthProvider } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';

export default function App() {
  return (
    <BrowserRouter>
      <ScrollToTop />
      <NotificationProvider>
        <AuthProvider>
          <Routes>
            <Route path="/" element={<MainLayout />}>
              <Route index element={<HomePage />} />
              <Route path="entertainments" element={<EntertainmentsPage />} />
              <Route path="talents" element={<TalentsPage />} />
              <Route path="talents/:id" element={<ProfilePage />} />
              <Route path="profile" element={<ProfilePage />} />
              <Route path="profile/:id" element={<ProfilePage />} />
              <Route path="jobs" element={<JobsPage />} />
              <Route path="jobs/:id" element={<JobDetailsPage />} />
              <Route path="investors-connect" element={<InvestorsConnectPage />} />
              
              {/* Protect Producer Module Routes */}
              <Route path="protect-producer" element={<ProtectProducerLandingPage />} />
              <Route path="protect-producer/:category" element={<ProtectCategoryPage />} />
              <Route path="protect-producer/:category/projects/:projectId" element={<ProjectDashboardPage />} />
              <Route path="protect-producer/:category/projects/:projectId/:stage" element={<ProjectDashboardPage />} />

              <Route path="lcm-app" element={<LcmAppPage />} />
              <Route path="get-support" element={<GetSupportPage />} />
              <Route path="contact-us" element={<ContactUsPage />} />
              <Route path="contact" element={<ContactUsPage />} />
              <Route path="about-us" element={<AboutUsPage />} />
              <Route path="about" element={<AboutUsPage />} />
              <Route path="sign-in" element={<SignInPage />} />
              <Route path="join-now" element={<JoinNowPage />} />
              <Route path="privacy-policy" element={<PrivacyPolicyPage />} />
              <Route path="terms-and-conditions" element={<TermsPage />} />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </AuthProvider>
      </NotificationProvider>
    </BrowserRouter>
  );
}
