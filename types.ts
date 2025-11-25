
export interface FormData {
  namaSatuanPendidikan: string;
  namaGuru: string;
  nipGuru: string;
  namaKepalaSekolah: string;
  nipKepalaSekolah: string;
  jenjang: string;
  kelas: string;
  mapel: string;
  cp: string;
  materi: string;
  jumlahPertemuan: number;
  durasi: string;
  praktikPedagogis: { [key: number]: string };
  dimensiLulusan: string[];
  generateLampiran: boolean;
}

export interface RPMData {
  identifikasi: {
    siswa: string;
    materiPelajaran: string;
    capaianDimensiLulusan: string;
  };
  desainPembelajaran: {
    capaianPembelajaran: string;
    lintasDisiplinIlmu: string;
    tujuanPembelajaran: string[];
    topikPembelajaran: string;
    praktikPedagogis: string;
    kemitraanPembelajaran: string;
    lingkunganPembelajaran: string;
    pemanfaatanDigital: string;
  };
  pengalamanBelajar: {
    pertemuan: number;
    awal: string;
    inti: string;
    refleksi: string;
    penutup: string;
  }[];
  asesmenPembelajaran: {
    asesmenAwal: string;
    asesmenProses: string;
    asesmenAkhir: string;
  };
  lampiran?: {
    bahanAjar: string; // HTML content
    mediaPembelajaran: string; // HTML content
    asesmen: string; // HTML content
    lkpd: string; // HTML content
  };
}