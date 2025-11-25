
import React, { useRef, useState } from 'react';
import { RPMData, FormData } from '../types';

interface RPMOutputProps {
  data: RPMData;
  formData: FormData;
}

export const RPMOutput: React.FC<RPMOutputProps> = ({ data, formData }) => {
  const tableRef = useRef<HTMLDivElement>(null);
  const [copyStatus, setCopyStatus] = useState<'idle' | 'success' | 'error'>('idle');
  
  const handleCopy = async () => {
    if (!tableRef.current) return;

    try {
      const html = tableRef.current.outerHTML;
      const blob = new Blob([html], { type: 'text/html' });
      const clipboardItem = new ClipboardItem({ 'text/html': blob });
      await navigator.clipboard.write([clipboardItem]);
      setCopyStatus('success');
      setTimeout(() => setCopyStatus('idle'), 2000);
    } catch (err) {
      console.error('Failed to copy content: ', err);
      setCopyStatus('error');
      setTimeout(() => setCopyStatus('idle'), 2000);
    }
  };

  const getCopyButtonText = () => {
    switch (copyStatus) {
      case 'success': return 'Berhasil Disalin!';
      case 'error': return 'Gagal Menyalin';
      default: return 'Salin atau Unduh RPM';
    }
  };
  
  return (
    <div className="bg-white p-8 rounded-lg shadow-md mt-8 lg:mt-0">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-bold text-gray-800">Hasil Rencana Pembelajaran Mendalam</h2>
        <button
          onClick={handleCopy}
          className={`px-4 py-2 text-sm font-medium text-white rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 ${
            copyStatus === 'success' ? 'bg-green-600 hover:bg-green-700 focus:ring-green-500' : 
            copyStatus === 'error' ? 'bg-red-600 hover:bg-red-700 focus:ring-red-500' :
            'bg-indigo-600 hover:bg-indigo-700 focus:ring-indigo-500'
          }`}
        >
          {getCopyButtonText()}
        </button>
      </div>
      
      <div ref={tableRef} className="overflow-x-auto">
        <h3 className="text-center text-xl font-bold mb-2">RENCANA PEMBELAJARAN MENDALAM (RPM)</h3>
        <h4 className="text-center text-lg font-semibold mb-4">{formData.namaSatuanPendidikan.toUpperCase()}</h4>

        <table className="min-w-full border-collapse border border-gray-400">
          <tbody>
            {/* --- IDENTIFIKASI --- */}
            <tr className="bg-indigo-100">
              <th colSpan={2} className="border border-gray-300 px-4 py-2 text-left text-lg font-bold text-indigo-800">1. IDENTIFIKASI</th>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold w-1/3">Siswa</td>
              <td className="border border-gray-300 px-4 py-2">{data.identifikasi.siswa}</td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Materi Pelajaran</td>
              <td className="border border-gray-300 px-4 py-2">{data.identifikasi.materiPelajaran}</td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Capaian Dimensi Lulusan</td>
              <td className="border border-gray-300 px-4 py-2">{data.identifikasi.capaianDimensiLulusan}</td>
            </tr>

            {/* --- DESAIN PEMBELAJARAN --- */}
            <tr className="bg-indigo-100">
              <th colSpan={2} className="border border-gray-300 px-4 py-2 text-left text-lg font-bold text-indigo-800">2. DESAIN PEMBELAJARAN</th>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Capaian Pembelajaran</td>
              <td className="border border-gray-300 px-4 py-2">{data.desainPembelajaran.capaianPembelajaran}</td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Lintas Disiplin Ilmu</td>
              <td className="border border-gray-300 px-4 py-2">{data.desainPembelajaran.lintasDisiplinIlmu}</td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Tujuan Pembelajaran</td>
              <td className="border border-gray-300 px-4 py-2">
                <ul className="list-disc pl-5">
                  {data.desainPembelajaran.tujuanPembelajaran.map((tujuan, index) => <li key={index}>{tujuan}</li>)}
                </ul>
              </td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Topik Pembelajaran</td>
              <td className="border border-gray-300 px-4 py-2">{data.desainPembelajaran.topikPembelajaran}</td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Praktik Pedagogis per Pertemuan</td>
              <td className="border border-gray-300 px-4 py-2">{data.desainPembelajaran.praktikPedagogis}</td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Kemitraan Pembelajaran</td>
              <td className="border border-gray-300 px-4 py-2">{data.desainPembelajaran.kemitraanPembelajaran}</td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Lingkungan Pembelajaran</td>
              <td className="border border-gray-300 px-4 py-2">{data.desainPembelajaran.lingkunganPembelajaran}</td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Pemanfaatan Digital</td>
              <td className="border border-gray-300 px-4 py-2">{data.desainPembelajaran.pemanfaatanDigital}</td>
            </tr>

            {/* --- PENGALAMAN BELAJAR --- */}
            <tr className="bg-indigo-100">
              <th colSpan={2} className="border border-gray-300 px-4 py-2 text-left text-lg font-bold text-indigo-800">3. PENGALAMAN BELAJAR</th>
            </tr>
            {data.pengalamanBelajar.map((item, index) => (
              <React.Fragment key={index}>
                <tr><td colSpan={2} className="border border-gray-300 px-4 py-2 font-bold bg-gray-100">Pertemuan {item.pertemuan}</td></tr>
                <tr><td className="border border-gray-300 px-4 py-2 font-semibold">Awal</td><td className="border border-gray-300 px-4 py-2">{item.awal}</td></tr>
                <tr><td className="border border-gray-300 px-4 py-2 font-semibold">Inti</td><td className="border border-gray-300 px-4 py-2">{item.inti}</td></tr>
                <tr><td className="border border-gray-300 px-4 py-2 font-semibold">Refleksi</td><td className="border border-gray-300 px-4 py-2">{item.refleksi}</td></tr>
                <tr><td className="border border-gray-300 px-4 py-2 font-semibold">Penutup</td><td className="border border-gray-300 px-4 py-2">{item.penutup}</td></tr>
              </React.Fragment>
            ))}

            {/* --- ASESMEN PEMBELAJARAN --- */}
            <tr className="bg-indigo-100">
              <th colSpan={2} className="border border-gray-300 px-4 py-2 text-left text-lg font-bold text-indigo-800">4. ASESMEN PEMBELAJARAN</th>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Asesmen Awal</td>
              <td className="border border-gray-300 px-4 py-2">{data.asesmenPembelajaran.asesmenAwal}</td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Asesmen Proses</td>
              <td className="border border-gray-300 px-4 py-2">{data.asesmenPembelajaran.asesmenProses}</td>
            </tr>
            <tr>
              <td className="border border-gray-300 px-4 py-2 font-semibold">Asesmen Akhir</td>
              <td className="border border-gray-300 px-4 py-2">{data.asesmenPembelajaran.asesmenAkhir}</td>
            </tr>

            {/* --- LAMPIRAN --- */}
            {data.lampiran && (
              <>
                <tr className="bg-indigo-100">
                  <th colSpan={2} className="border border-gray-300 px-4 py-2 text-left text-lg font-bold text-indigo-800">5. LAMPIRAN</th>
                </tr>
                <tr>
                  <td colSpan={2} className="border border-gray-300 px-4 py-2">
                    <h4 className="font-bold text-lg mt-2 mb-2">Lampiran 1: Bahan Ajar</h4>
                    <div className="ai-content" dangerouslySetInnerHTML={{ __html: data.lampiran.bahanAjar }} />
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} className="border border-gray-300 px-4 py-2">
                    <h4 className="font-bold text-lg mt-2 mb-2">Lampiran 2: Media Pembelajaran</h4>
                    <div className="ai-content" dangerouslySetInnerHTML={{ __html: data.lampiran.mediaPembelajaran }} />
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} className="border border-gray-300 px-4 py-2">
                    <h4 className="font-bold text-lg mt-2 mb-2">Lampiran 3: Asesmen</h4>
                    <div className="ai-content" dangerouslySetInnerHTML={{ __html: data.lampiran.asesmen }} />
                  </td>
                </tr>
                <tr>
                  <td colSpan={2} className="border border-gray-300 px-4 py-2">
                    <h4 className="font-bold text-lg mt-2 mb-2">Lampiran 4: Lembar Kerja Peserta Didik (LKPD)</h4>
                    <div className="ai-content" dangerouslySetInnerHTML={{ __html: data.lampiran.lkpd }} />
                  </td>
                </tr>
              </>
            )}

          </tbody>
        </table>

        <div className="mt-12 flex justify-between text-center">
            <div className="w-1/3">
                <p>Mengetahui,</p>
                <p>Kepala Sekolah</p>
                <br /><br /><br />
                <p className="font-bold underline">{formData.namaKepalaSekolah}</p>
                <p>NIP. {formData.nipKepalaSekolah}</p>
            </div>
            <div className="w-1/3">
                {/* Spacer */}
            </div>
            <div className="w-1/3">
                <p>Guru Mata Pelajaran</p>
                <br /><br /><br />
                <p className="font-bold underline">{formData.namaGuru}</p>
                <p>NIP. {formData.nipGuru}</p>
            </div>
        </div>

      </div>
    </div>
  );
};