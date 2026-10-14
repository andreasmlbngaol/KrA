import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { ChatLauncher } from './components/ChatLauncher';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { KhsPage } from './pages/KhsPage';
import { ManageKrsPage } from './pages/ManageKrsPage';
import { TokenUsagePage } from './pages/TokenUsagePage';
import { ChatHistoryPage } from './pages/ChatHistoryPage';
import { GooeyToaster } from 'goey-toast';
import 'goey-toast/styles.css';

export function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/khs"
            element={
              <ProtectedRoute>
                <KhsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/kelola-krs"
            element={
              <ProtectedRoute>
                <ManageKrsPage />
              </ProtectedRoute>
            }
          />
          {/* Public (no auth) — internal monitoring dashboards */}
          <Route path="/monitoring" element={<TokenUsagePage />} />
          <Route path="/monitoring/chat" element={<ChatHistoryPage />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        <ChatLauncher />
        <GooeyToaster position="bottom-left" theme="light" closeButton bounce={0.45} />
      </BrowserRouter>
    </AuthProvider>
  );
}
