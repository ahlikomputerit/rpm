
import React, { useState, useEffect } from 'react';
import { FormData } from '../types';
import { JENJANG_PENDIDIKAN, KELAS_OPTIONS, PRAKTIK_PEDAGOGIS_OPTIONS, DIMENSI_LULUSAN_OPTIONS } from '../constants';

interface RPMFormProps {
  onSubmit: (data: FormData) => void;
  isLoading: boolean;
  initialData: FormData;
}

export const RPMForm: React.FC<RPMFormProps> = ({ onSubmit, isLoading, initialData }) => {
  const [formData, setFormData] = useState<FormData>(initialData);
  const [kelasOptions, setKelasOptions] = useState<string[]>([]);

  useEffect(() => {
    if (formData.jenjang) {
      setKelasOptions(KELAS_OPTIONS[formData.jenjang] || []);
    } else {
      setKelasOptions([]);
    }
  }, [formData.jenjang]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleCheckboxChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, checked } = e.target;
    setFormData(prev => ({...prev, [name]: checked}));
  }

  const handleJumlahPertemuanChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = parseInt(e.target.value, 10);
    const newJumlah = isNaN(value) || value < 1 ? 1 : value;
    setFormData(prev => ({
      ...prev,
      jumlahPertemuan: newJumlah,
      praktikPedagogis: {} // Reset praktik when count changes
    }));
  };
  
  const handlePraktikChange = (pertemuan: number, value: string) => {
    setFormData(prev => ({
      ...prev,
      praktikPedagogis: {
        ...prev.praktikPedagogis,
        [pertemuan]: value
      }
    }));
  };

  const handleDimensiChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { value, checked } = e.target;
    setFormData(prev => {
      const currentDimensi = prev.dimensiLulusan;
      if (checked) {
        return { ...prev, dimensiLulusan: [...currentDimensi, value] };
      } else {
        return { ...prev, dimensiLulusan: currentDimensi.filter(d => d !== value) };
      }
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(formData);
  };
  
  const renderPraktikInputs = () => {
    return Array.from({ length: formData.jumlahPertemuan }, (_, i) => i + 1).map(pertemuan => (
      <div key={pertemuan} className="mb-2">
        <label htmlFor={`praktik-${pertemuan}`} className="block text-sm font-medium text-gray-700 mb-1">
          Praktik Pedagogis Pertemuan {pertemuan}
        </label>
        <select
          id={`praktik-${pertemuan}`}
          name={`praktik-${pertemuan}`}
          value={formData.praktikPedagogis[pertemuan] || ''}
          onChange={(e) => handlePraktikChange(pertemuan, e.target.value)}
          className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md"
          required
        >
          <option value="" disabled>Pilih Praktik</option>
          {PRAKTIK_PEDAGOGIS_OPTIONS.map(opt => <option key={opt} value={opt}>{opt}</option>)}
        </select>
      </div>
    ));
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6 bg-white p-8 rounded-lg shadow-md">
      <h2 className="text-2xl font-bold text-gray-800">Masukkan Data Pembelajaran</h2>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label htmlFor="namaSatuanPendidikan" className="block text-sm font-medium text-gray-700">Nama Satuan Pendidikan</label>
          <input type="text" name="namaSatuanPendidikan" id="namaSatuanPendidikan" value={formData.namaSatuanPendidikan} onChange={handleChange} required className="mt-1 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" />
        </div>
         <div>
          <label htmlFor="namaGuru" className="block text-sm font-medium text-gray-700">Nama Guru</label>
          <input type="text" name="namaGuru" id="namaGuru" value={formData.namaGuru} onChange={handleChange} required className="mt-1 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" />
        </div>
        <div>
          <label htmlFor="nipGuru" className="block text-sm font-medium text-gray-700">NIP Guru</label>
          <input type="text" name="nipGuru" id="nipGuru" value={formData.nipGuru} onChange={handleChange} required className="mt-1 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" />
        </div>
        <div>
          <label htmlFor="namaKepalaSekolah" className="block text-sm font-medium text-gray-700">Nama Kepala Sekolah</label>
          <input type="text" name="namaKepalaSekolah" id="namaKepalaSekolah" value={formData.namaKepalaSekolah} onChange={handleChange} required className="mt-1 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" />
        </div>
        <div>
          <label htmlFor="nipKepalaSekolah" className="block text-sm font-medium text-gray-700">NIP Kepala Sekolah</label>
          <input type="text" name="nipKepalaSekolah" id="nipKepalaSekolah" value={formData.nipKepalaSekolah} onChange={handleChange} required className="mt-1 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" />
        </div>
        <div>
          <label htmlFor="jenjang" className="block text-sm font-medium text-gray-700">Jenjang Pendidikan</label>
          <select id="jenjang" name="jenjang" value={formData.jenjang} onChange={handleChange} required className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md">
            <option value="">Pilih Jenjang</option>
            {JENJANG_PENDIDIKAN.map(j => <option key={j} value={j}>{j}</option>)}
          </select>
        </div>
         <div>
          <label htmlFor="kelas" className="block text-sm font-medium text-gray-700">Kelas</label>
          <select id="kelas" name="kelas" value={formData.kelas} onChange={handleChange} required disabled={!formData.jenjang} className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm rounded-md disabled:bg-gray-200">
            <option value="">Pilih Kelas</option>
            {kelasOptions.map(k => <option key={k} value={k}>{k}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="mapel" className="block text-sm font-medium text-gray-700">Mata Pelajaran</label>
          <input type="text" name="mapel" id="mapel" value={formData.mapel} onChange={handleChange} required className="mt-1 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" />
        </div>
        <div>
          <label htmlFor="cp" className="block text-sm font-medium text-gray-700">Capaian Pembelajaran (CP)</label>
          <textarea name="cp" id="cp" value={formData.cp} onChange={handleChange} required rows={3} className="mt-1 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"></textarea>
        </div>
        <div>
          <label htmlFor="materi" className="block text-sm font-medium text-gray-700">Materi Pelajaran</label>
          <textarea name="materi" id="materi" value={formData.materi} onChange={handleChange} required rows={3} className="mt-1 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"></textarea>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label htmlFor="jumlahPertemuan" className="block text-sm font-medium text-gray-700">Jumlah Pertemuan</label>
          <input type="number" name="jumlahPertemuan" id="jumlahPertemuan" value={formData.jumlahPertemuan} onChange={handleJumlahPertemuanChange} required min="1" className="mt-1 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" />
        </div>
        <div>
          <label htmlFor="durasi" className="block text-sm font-medium text-gray-700">Durasi Setiap Pertemuan</label>
          <input type="text" name="durasi" id="durasi" value={formData.durasi} onChange={handleChange} required placeholder="Contoh: 2 x 45 menit" className="mt-1 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" />
        </div>
      </div>
      
      <div>
        <h3 className="text-lg font-medium text-gray-900">Praktik Pedagogis</h3>
        <div className="mt-4 p-4 border border-gray-200 rounded-md">
          {renderPraktikInputs()}
        </div>
      </div>
      
      <div>
        <h3 className="text-lg font-medium text-gray-900">Dimensi Lulusan</h3>
        <div className="mt-4 grid grid-cols-2 md:grid-cols-4 gap-4">
          {DIMENSI_LULUSAN_OPTIONS.map(dimensi => (
            <div key={dimensi} className="flex items-start">
              <div className="flex items-center h-5">
                <input id={dimensi} name="dimensiLulusan" type="checkbox" value={dimensi} onChange={handleDimensiChange} checked={formData.dimensiLulusan.includes(dimensi)} className="focus:ring-indigo-500 h-4 w-4 text-indigo-600 border-gray-300 rounded" />
              </div>
              <div className="ml-3 text-sm">
                <label htmlFor={dimensi} className="font-medium text-gray-700">{dimensi}</label>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div>
        <h3 className="text-lg font-medium text-gray-900">Opsi Tambahan</h3>
          <div className="mt-4 relative flex items-start">
            <div className="flex items-center h-5">
              <input
                id="generateLampiran"
                name="generateLampiran"
                type="checkbox"
                checked={formData.generateLampiran}
                onChange={handleCheckboxChange}
                className="focus:ring-indigo-500 h-4 w-4 text-indigo-600 border-gray-300 rounded"
              />
            </div>
            <div className="ml-3 text-sm">
              <label htmlFor="generateLampiran" className="font-medium text-gray-700">Sertakan Lampiran</label>
              <p className="text-gray-500">Membuat Bahan Ajar, Media, Asesmen, dan LKPD. (Proses mungkin sedikit lebih lama)</p>
            </div>
          </div>
      </div>

      <div>
        <button
          type="submit"
          disabled={isLoading}
          className="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:bg-indigo-300"
        >
          {isLoading ? (
             <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          ) : null}
          {isLoading ? 'Menghasilkan...' : 'Hasilkan RPM'}
        </button>
      </div>
    </form>
  );
};