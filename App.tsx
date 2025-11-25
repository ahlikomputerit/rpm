
import React, { useState, useMemo } from 'react';
import { RPMForm } from './components/RPMForm';
import { RPMOutput } from './components/RPMOutput';
import { generateRPM } from './services/geminiService';
import { FormData, RPMData } from './types';

const App: React.FC = () => {
  const [formData, setFormData] = useState<FormData | null>(null);
  const [rpmData, setRpmData] = useState<RPMData | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const initialFormData = useMemo<FormData>(() => ({
    namaSatuanPendidikan: '',
    namaGuru: '',
    nipGuru: '',
    namaKepalaSekolah: '',
    nipKepalaSekolah: '',
    jenjang: '',
    kelas: '',
    mapel: '',
    cp: '',
    materi: '',
    jumlahPertemuan: 1,
    durasi: '',
    praktikPedagogis: {},
    dimensiLulusan: [],
    generateLampiran: true,
  }), []);

  const handleFormSubmit = async (data: FormData) => {
    setIsLoading(true);
    setError(null);
    setRpmData(null);
    setFormData(data); // Store the submitted form data for use in the output component

    try {
      const result = await generateRPM(data);
      setRpmData(result);
    } catch (err: any) {
      setError(err.message || 'Terjadi kesalahan yang tidak diketahui.');
    } finally {
      setIsLoading(false);
    }
  };
  
  const teacherName = formData?.namaGuru || 'Anda';
  const appTitle = `Generator RPM dibuat oleh ${teacherName}`;

  return (
    <div className="min-h-screen bg-gray-50 p-4 sm:p-6 lg:p-8">
      <div className="max-w-7xl mx-auto">
        <header className="text-center mb-10">
          <h1 className="text-4xl font-extrabold text-gray-900 tracking-tight">
            {appTitle}
          </h1>
          <p className="mt-2 text-lg text-gray-600">
            Buat Rencana Pembelajaran Mendalam (RPM) untuk semua jenjang dan mata pelajaran dengan mudah.
          </p>
        </header>

        <main className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
          <div>
            <RPMForm onSubmit={handleFormSubmit} isLoading={isLoading} initialData={initialFormData} />
          </div>
          <div className="lg:sticky top-8">
             {error && (
              <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg relative mb-6" role="alert">
                <strong className="font-bold">Error!</strong>
                <span className="block sm:inline ml-2">{error}</span>
              </div>
            )}
            
            {isLoading && (
              <div className="flex flex-col items-center justify-center bg-white p-8 rounded-lg shadow-md h-96">
                  <svg className="animate-spin h-12 w-12 text-indigo-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  <p className="mt-4 text-lg font-medium text-gray-700">AI sedang merancang RPM Anda...</p>
                  <p className="text-gray-500">Mohon tunggu sebentar.</p>
              </div>
            )}

            {!isLoading && rpmData && formData && (
              <RPMOutput data={rpmData} formData={formData} />
            )}

            {!isLoading && !rpmData && !error && (
               <div className="flex flex-col items-center justify-center bg-white p-8 rounded-lg shadow-md h-96 text-center">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <p className="mt-4 text-lg font-medium text-gray-700">Hasil RPM akan muncul di sini.</p>
                  <p className="text-gray-500">Lengkapi formulir di sebelah kiri dan klik "Hasilkan RPM" untuk memulai.</p>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
};

export default App;