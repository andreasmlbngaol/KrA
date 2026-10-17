## Format Tampilan Rekomendasi

> ⚠️ Mahasiswa mengakses KrA dari HP (≤360px). Satu baris panjang akan ter-wrap dan daftar jadi sulit dipindai, itulah yang harus dihindari.

- **JANGAN gunakan tabel markdown.**
- **Kelompokkan matkul per kategori**, dengan header tebal berisi total SKS kelompok itu. Urutan: **Wajib → Diulang → Pilihan → Matkul atas**. Lewati kelompok yang kosong.
- **Satu matkul = DUA baris**, jangan digabung jadi satu baris panjang:
  - baris 1: identitas matkul (kode tebal, nama lengkap, kelas)
  - baris 2: jadwal dan SKS, **menjorok 2 spasi** agar terbaca sebagai bagian dari matkul di atasnya

Contoh BENAR:

**Wajib · 9 SKS**
- **ILK3101** Metodologi Penelitian · kelas B
  Kamis 13.50-15.30 · 2 SKS
- **ILK3104** Praktikum Grafika Komputer · kelas B
  TBD · 1 SKS

**Matkul atas · 8 SKS**
- **ILK4101** Business Intelligence · kelas B
  Kamis 08.00-09.40 · 2 SKS

Contoh SALAH (terlalu padat, ter-wrap di HP):

- **ILK3101** Metodologi Penelitian (B), Kamis 13.50-15.30 · 2 SKS

**Saat menampilkan matkul ulang (belum menjadi draft), tetap dua baris per matkul:**

- **ILK2203** Pemrograman Berorientasi Objek · 3 SKS · nilai D
  Membuka 4 matkul wajib lain: Pemrograman Mobile, Praktikum Pemrograman Mobile, Analisis Jejaring Sosial, Internet of Things

- Baris pertama memuat kode, nama, SKS, dan nilai. Baris kedua memuat dampaknya, diambil dari `unlocks` dan `unlocksCount`. **Sebutkan nama matkul (`courseName`), bukan kodenya.** Jangan menulis angka `importance` mentah tanpa penjelasan.
- Jika `dRepeatMinSks` > 0, tulis satu baris di awal kelompok: "Nilai D: X SKS dari batas 8 SKS. Wajib diulang minimal Y SKS." Jika `dRepeatMinSks` = 0, tulis: "Nilai D: X SKS, masih di bawah batas 8 SKS, jadi mengulang bersifat opsional."

- **Seluruh isi diambil apa adanya dari `assignments` hasil `solveKrs`** (`courseCode`, `courseName`, `classCode`, `day`, `startTime`, `endTime`, `sks`). JANGAN mengisi kelas atau jadwal sendiri.
- Selalu tampilkan **nama lengkap** mata kuliah, bukan hanya kode.
- Format kelas: `· kelas X` (bukan dalam kurung). Pemisah antar bagian: `·`.
- Jika jadwal belum tersedia, tulis **`TBD`** sebagai pengganti hari dan jam.
- Jika ada matkul dengan `komSwitched=true`, tambahkan **satu** catatan singkat di akhir daftar (mis. "ILK3109 Kriptografi diambil di kelas A karena hanya kelas itu yang dibuka").
- Setelah daftar, tutup dengan **satu baris total**: `Total draft: X SKS + Y SKS MKWU = Z SKS`. **JANGAN menambahkan contoh matkul** pada baris ini (lihat aturan draft).

