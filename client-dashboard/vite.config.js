import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  // Tell Vite to map Node's 'global' to the Browser's 'window'
  define: {
    global: 'window',
  },
})