import { Navigate } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth'
import { hasMasterKey } from '@/crypto/keyStore'

interface Props {
  children: React.ReactNode
  requireKey?: boolean
}

export default function PrivateRoute({ children, requireKey = false }: Props) {
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (requireKey && !hasMasterKey()) {
    return <Navigate to="/master-key" replace />
  }

  return <>{children}</>
}