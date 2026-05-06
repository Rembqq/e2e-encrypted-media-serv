import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import LoginPage from '@/pages/LoginPage'
import RegisterPage from '@/pages/RegisterPage'
import MasterKeyPage from '@/pages/MasterKeyPage'
import DashboardPage from '@/pages/DashboardPage'
import BackupPage from '@/pages/BackupPage'
import RestorePage from '@/pages/RestorePage'
import PrivateRoute from '@/components/PrivateRoute'

function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-right" />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/master-key"
          element={
            <PrivateRoute>
              <MasterKeyPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/dashboard"
          element={
            <PrivateRoute requireKey>
              <DashboardPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/backup"
          element={
            <PrivateRoute requireKey>
              <BackupPage />
            </PrivateRoute>
          }
        />
        <Route
          path="/restore/:id"
          element={
            <PrivateRoute requireKey>
              <RestorePage />
            </PrivateRoute>
          }
        />
        <Route path="*" element={<Navigate to="/login" />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App