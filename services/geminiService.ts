
import { GoogleGenAI, Type } from "@google/genai";
import { FormData, RPMData } from "../types";

const ai = new GoogleGenAI({ apiKey: process.env.API_KEY as string });

const responseSchema = {
  type: Type.OBJECT,
  properties: {
    identifikasi: {
      type: Type.OBJECT,
      properties: {
        siswa: { type: Type.STRING, description: "Deskripsi singkat profil siswa target." },
        materiPelajaran: { type: Type.STRING, description: "Materi pelajaran yang diberikan oleh pengguna." },
        capaianDimensiLulusan: { type: Type.STRING, description: "Dimensi lulusan yang dipilih oleh pengguna." }
      },
      required: ["siswa", "materiPelajaran", "capaianDimensiLulusan"]
    },
    desainPembelajaran: {
      type: Type.OBJECT,
      properties: {
        capaianPembelajaran: { type: Type.STRING, description: "Capaian pembelajaran yang diberikan pengguna." },
        lintasDisiplinIlmu: { type: Type.STRING, description: "Contoh koneksi lintas disiplin ilmu yang relevan dengan materi." },
        tujuanPembelajaran: { type: Type.ARRAY, items: { type: Type.STRING }, description: "3-5 tujuan pembelajaran yang spesifik, terukur, dan selaras dengan Capaian Pembelajaran." },
        topikPembelajaran: { type: Type.STRING, description: "Topik utama pembelajaran, di-breakdown dari materi." },
        praktikPedagogis: { type: Type.STRING, description: "Praktik pedagogis per pertemuan yang dipilih pengguna, dirangkum." },
        kemitraanPembelajaran: { type: Type.STRING, description: "Saran 1-2 kemitraan pembelajaran yang bisa dijalin (misal: orang tua, komunitas, profesional)." },
        lingkunganPembelajaran: { type: Type.STRING, description: "Deskripsi lingkungan belajar yang mendukung (fisik dan non-fisik)." },
        pemanfaatanDigital: { type: Type.STRING, description: "Sebutkan 2-3 ide pemanfaatan teknologi digital dalam pembelajaran ini." }
      },
      required: ["capaianPembelajaran", "lintasDisiplinIlmu", "tujuanPembelajaran", "topikPembelajaran", "praktikPedagogis", "kemitraanPembelajaran", "lingkunganPembelajaran", "pemanfaatanDigital"]
    },
    pengalamanBelajar: {
      type: Type.ARRAY,
      description: "Array pengalaman belajar, satu objek untuk setiap pertemuan.",
      items: {
        type: Type.OBJECT,
        properties: {
          pertemuan: { type: Type.INTEGER },
          awal: { type: Type.STRING, description: "Aktivitas awal yang berkesadaran, bermakna, dan menggembirakan." },
          inti: { type: Type.STRING, description: "Aktivitas inti (memahami, mengaplikasi) yang berkesadaran dan bermakna." },
          refleksi: { type: Type.STRING, description: "Aktivitas refleksi yang berkesadaran dan menggembirakan." },
          penutup: { type: Type.STRING, description: "Aktivitas penutup yang berkesadaran." }
        },
        required: ["pertemuan", "awal", "inti", "refleksi", "penutup"]
      }
    },
    asesmenPembelajaran: {
      type: Type.OBJECT,
      properties: {
        asesmenAwal: { type: Type.STRING, description: "Deskripsi asesmen awal (diagnostik/apersepsi)." },
        asesmenProses: { type: Type.STRING, description: "Deskripsi asesmen proses (observasi, rubrik, diskusi)." },
        asesmenAkhir: { type: Type.STRING, description: "Deskripsi asesmen akhir (produk, tugas, presentasi, portofolio)." }
      },
      required: ["asesmenAwal", "asesmenProses", "asesmenAkhir"]
    },
    lampiran: {
        type: Type.OBJECT,
        description: "Lampiran pendukung pembelajaran. Hanya dibuat jika diminta.",
        properties: {
            bahanAjar: { type: Type.STRING, description: "Konten HTML untuk Lampiran 1: Bahan Ajar. Berisi ringkasan materi yang jelas dan terstruktur." },
            mediaPembelajaran: { type: Type.STRING, description: "Konten HTML untuk Lampiran 2: Media Pembelajaran. Berisi saran media seperti link video, gambar, dll." },
            asesmen: { type: Type.STRING, description: "Konten HTML untuk Lampiran 3: Asesmen. Berisi 10 soal formatif pilihan ganda, kunci jawaban, dan rubrik penilaian diskusi/presentasi dalam format tabel HTML." },
            lkpd: { type: Type.STRING, description: "Konten HTML untuk Lampiran 4: Lembar Kerja Peserta Didik (LKPD). Berisi judul, petunjuk, dan kegiatan siswa." }
        },
    }
  },
  required: ["identifikasi", "desainPembelajaran", "pengalamanBelajar", "asesmenPembelajaran"]
};

const buildPrompt = (data: FormData): string => {
  const praktikPedagogisText = Object.entries(data.praktikPedagogis)
    .map(([pertemuan, praktik]) => `Pertemuan ${pertemuan}: ${praktik}`)
    .join('\n');

  const lampiranInstruction = data.generateLampiran
    ? `
Selain bagian utama RPM, Anda WAJIB membuat bagian 'lampiran' yang sangat lengkap dan siap pakai dalam format HTML sederhana. Gunakan tag seperti <h4>, <p>, <ul>, <ol>, <li>, <table>, <thead>, <tbody>, <tr>, <th>, <td>, dan <b>.
Bagian lampiran harus berisi:
1.  'bahanAjar': Buat ringkasan materi yang detail, jelas, dan terstruktur untuk siswa.
2.  'mediaPembelajaran': Berikan saran media pembelajaran yang konkret, misalnya menyertakan link video YouTube yang relevan.
3.  'asesmen':
    - Buat 10 soal asesmen formatif bentuk pilihan ganda (A, B, C, D) yang relevan dengan materi.
    - Sertakan Kunci Jawaban dan Pedoman Penskoran yang jelas.
    - Buat Rubrik Penilaian untuk Diskusi dan Presentasi dalam format tabel HTML yang rapi.
4.  'lkpd': Buat satu Lembar Kerja Peserta Didik (LKPD) yang lengkap, mencakup Judul, Petunjuk Pengerjaan, dan Kegiatan/Soal yang harus dikerjakan siswa.
`
    : "Pengguna tidak meminta untuk membuat lampiran. Jangan membuat properti 'lampiran' dalam output JSON.";

  return `
Anda adalah seorang ahli perancang kurikulum dan pedagogi yang sangat berpengalaman di Indonesia.
Tugas Anda adalah membuat Rencana Pembelajaran Mendalam (RPM) yang komprehensif, terstruktur, dan inovatif berdasarkan informasi yang diberikan.
Pastikan output yang Anda hasilkan relevan dengan konteks pendidikan di Indonesia, kreatif, dan berpusat pada siswa.

Berikut adalah data untuk pembuatan RPM:
- Jenjang Pendidikan: ${data.jenjang}
- Kelas: ${data.kelas}
- Mata Pelajaran: ${data.mapel}
- Materi Pelajaran: ${data.materi}
- Capaian Pembelajaran (CP): ${data.cp}
- Jumlah Pertemuan: ${data.jumlahPertemuan}
- Durasi Setiap Pertemuan: ${data.durasi}
- Praktik Pedagogis per Pertemuan:
${praktikPedagogisText}
- Dimensi Lulusan yang dituju: ${data.dimensiLulusan.join(', ')}

Berdasarkan data di atas, tolong hasilkan RPM dalam format JSON yang terstruktur sesuai dengan skema yang telah ditentukan.
- Untuk 'lintasDisiplinIlmu', berikan contoh konkret bagaimana materi ini bisa terhubung dengan 1-2 mata pelajaran lain.
- Untuk 'tujuanPembelajaran', rumuskan tujuan yang SMART (Specific, Measurable, Achievable, Relevant, Time-bound).
- Untuk 'pengalamanBelajar', rancang aktivitas yang menarik dan sesuai dengan praktik pedagogis yang dipilih untuk setiap pertemuan.
- Untuk semua bagian yang perlu di-generate, berikan jawaban yang mendalam, praktis, dan dapat diimplementasikan di kelas.
- Pastikan ada satu objek dalam array 'pengalamanBelajar' untuk setiap pertemuan dari 1 hingga ${data.jumlahPertemuan}.

${lampiranInstruction}
`;
};


export const generateRPM = async (formData: FormData): Promise<RPMData> => {
  try {
    const prompt = buildPrompt(formData);
    
    // Adjust schema based on whether lampiran is requested
    // FIX: Deep copy responseSchema to prevent mutation of the original object and to work around strict type inference.
    // The original shallow copy caused a type error on line 126 when trying to add the 'required' property to the 'lampiran' schema.
    const finalSchema = JSON.parse(JSON.stringify(responseSchema));
    if (formData.generateLampiran) {
        finalSchema.properties.lampiran.required = ["bahanAjar", "mediaPembelajaran", "asesmen", "lkpd"];
        if (!finalSchema.required.includes('lampiran')) {
            finalSchema.required.push('lampiran');
        }
    } else {
        delete finalSchema.properties.lampiran;
        finalSchema.required = finalSchema.required.filter(item => item !== 'lampiran');
    }

    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: prompt,
      config: {
        responseMimeType: "application/json",
        responseSchema: finalSchema,
        temperature: 0.7,
      },
    });

    const jsonString = response.text;
    const parsedData = JSON.parse(jsonString);

    // Ensure all required fields from input are present in the final object for rendering
    parsedData.identifikasi.materiPelajaran = formData.materi;
    parsedData.identifikasi.capaianDimensiLulusan = formData.dimensiLulusan.join(', ');
    parsedData.desainPembelajaran.capaianPembelajaran = formData.cp;
    parsedData.desainPembelajaran.praktikPedagogis = Object.entries(formData.praktikPedagogis)
      .map(([pertemuan, praktik]) => `Pertemuan ${pertemuan}: ${praktik}`)
      .join('; ');
      
    return parsedData as RPMData;

  } catch (error) {
    console.error("Error generating RPM:", error);
    throw new Error("Gagal menghasilkan RPM. Silakan coba lagi.");
  }
};
