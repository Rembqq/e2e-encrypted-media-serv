export function useAuth() {
    const token = localStorage.getItem('token')
  
    const isAuthenticated = !!token
  
    const getUsername = (): string | null => {
      if (!token) return null
      try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        return payload.sub ?? null
      } catch {
        return null
      }
    }
  
    const logout = () => {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
  
    return { isAuthenticated, getUsername, logout }
  }