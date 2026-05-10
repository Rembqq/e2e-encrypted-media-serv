import axios from 'axios'
import toast from 'react-hot-toast'

export function handleApiError(error: unknown, fallback = 'Something went wrong') {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status
    const message = error.response?.data?.message
    const details = error.response?.data?.details

    if (status === 404) {
      toast.error('Resource not found')
      return
    }
    if (status === 403) {
      toast.error('Access denied')
      return
    }
    if (status === 500) {
      toast.error('Server error, please try again later')
      return
    }
    if (Array.isArray(details)) {
      toast.error(details[0])
      return
    }
    if (message) {
      toast.error(message)
      return
    }
  }
  toast.error(fallback)
}