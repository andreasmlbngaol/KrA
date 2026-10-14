/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Warna khas portal SATU MAHASISWA USU: hijau cerah + emas, dominan putih.
        usu: {
          DEFAULT: '#13a05b',
          dark: '#0e8a4d',
          light: '#e9f8f0',
        },
        gold: {
          DEFAULT: '#f5b800',
          dark: '#8a6500',
          light: '#fff7d6',
        },
      },
    },
  },
  plugins: [],
};
