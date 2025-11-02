import pandas as pd
import numpy as np
from scipy.signal import butter, filtfilt

# ===== Butterworth Filter Parameters =====
order = 2
cutoff_hz = 10
fs = 548
nyquist = fs / 2
cutoff_fraction = cutoff_hz / nyquist
b, a = butter(N=order, Wn=cutoff_fraction, btype='low', analog=False)

# ===== ADC to mV Conversion Parameters =====
adc_bits = 12
adc_max = (2 ** adc_bits) - 1
vref_mV = 3300

def parse_lines_to_df(lines_list):
    """
    Mem-parse List<String> dari Kotlin, bukan file.
    """
    sensors_data = {}
    current_sensor = None

    for line in lines_list:
        line = line.strip()

        if line.startswith("--- SENSOR"):
            try:
                sensor_num = int(line.split()[2])
                current_sensor = sensor_num
                if current_sensor not in sensors_data:
                    sensors_data[current_sensor] = {'pixel': [], 'adc_value': []}
            except (ValueError, IndexError):
                current_sensor = None

        elif line.startswith("Pixels[") and current_sensor is not None:
            try:
                parts = line.split(']')
                pixel = int(parts[0].replace('Pixels[', '').strip())
                adc = int(parts[1].replace(':', '').strip())
                sensors_data[current_sensor]['pixel'].append(pixel)
                sensors_data[current_sensor]['adc_value'].append(adc)
            except (ValueError, IndexError):
                pass

    sensor_dfs = {}
    for sensor_num, data in sensors_data.items():
        if data['pixel']:
            sensor_dfs[sensor_num] = pd.DataFrame(data)

    return sensor_dfs


def process_data_batch(lines_list, storage_path):
    """
    Fungsi utama yang dipanggil Kotlin.
    Menerima List<String> dan path penyimpanan.
    TANPA plotting. TANPA menyimpan CSV.
    Mengembalikan satu nilai float (mean dari semua mean sensor).
    """
    sensor_dfs = parse_lines_to_df(lines_list)

    if not sensor_dfs:
        return 0.0  # Tidak ada data

    all_filtered_voltage_means = []

    for sensor_num in sorted(sensor_dfs.keys()):
        data = sensor_dfs[sensor_num].copy()

        # STEP 1: Apply Butterworth filter
        data["adc_filtered"] = filtfilt(b, a, data["adc_value"].values)

        # STEP 2: Convert to mV
        data["voltage_mV_filtered"] = (data["adc_filtered"] / adc_max) * vref_mV

        # Kumpulkan rata-rata voltage dari sensor ini
        all_filtered_voltage_means.append(data["voltage_mV_filtered"].mean())

    # ===== Kalkulasi Akhir =====
    if not all_filtered_voltage_means:
        return 0.0

    overall_mean = np.mean(all_filtered_voltage_means)

    # Kembalikan hanya satu nilai float
    return float(overall_mean)