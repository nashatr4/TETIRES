import json
import re
from typing import Any

# ============================================================================
# MODEL KALIBRASI (dari hasil training Colab terbaik)
# ============================================================================

# Model DALAM: untuk kedalaman >= 8mm
model_dalam = {
    "min": 1717.81055814,
    "max": 2642.29232265,
    "slope": -1.80321265,
    "intercept": 11.879767975539409
}

# Model DANGKAL: untuk kedalaman 0-5mm
model_dangkal = {
    "min": 1503.08228732,
    "max": 2954.71073448,
    "slope": -0.29599108,
    "intercept": 0.9643865647697497
}

# Koefisien Filter Butterworth
b_coef = [0.0674553, 0.134911, 0.0674553]
a_coef = [-1.14298, 0.412801]

# ============================================================================
# FUNGSI FILTER BUTTERWORTH
# ============================================================================

def butter_lowpass_filter(data, b, a):
    """
    Forward pass filter Butterworth order-2
    """
    n = len(data)
    if n == 0:
        return []
    if n < 3:
        return data[:]

    y = [0.0] * n
    y[0] = b[0] * data[0]
    y[1] = b[0] * data[1] + b[1] * data[0] - a[0] * y[0]

    for i in range(2, n):
        y[i] = (
            b[0] * data[i]
            + b[1] * data[i - 1]
            + b[2] * data[i - 2]
            - a[0] * y[i - 1]
            - a[1] * y[i - 2]
        )
    return y


def butter_filtfilt(data, b, a):
    """
    Zero-phase filtering: forward + backward pass
    """
    if not data:
        return []
    if len(data) < 3:
        return data[:]

    forward = butter_lowpass_filter(data, b, a)
    backward = butter_lowpass_filter(forward[::-1], b, a)
    return backward[::-1]


# ============================================================================
# CONVERTER UNTUK CHAQUOPY (Java ArrayList)
# ============================================================================

def to_python_list(obj: Any):
    """
    Convert Java ArrayList atau iterable ke Python list
    """
    if isinstance(obj, list):
        return obj

    try:
        return list(obj)
    except Exception:
        pass

    try:
        size = obj.size()
        return [obj.get(i) for i in range(size)]
    except Exception:
        pass

    try:
        it = obj.iterator()
        out = []
        while it.hasNext():
            out.append(it.next())
        return out
    except Exception:
        pass

    return []


# ============================================================================
# PARSER CCD DATA (MULTI-SENSOR)
# ============================================================================

def parse_ccd_raw_lines(raw_lines):
    """
    Parse log CCD multi-sensor.
    Format:
      --- SENSOR 1 ---
      Pixel[   0]: 1234.56 mV
      ...

    Returns:
      dict {sensor_id: [voltages]}
    """
    lines = to_python_list(raw_lines)
    sensor_pattern = re.compile(r"---\s*SENSOR\s+(\d+)\s*---", re.IGNORECASE)
    pixel_pattern = re.compile(r"Pixel\[\s*(\d+)\s*\]:\s*([\d\.]+)\s*mV", re.IGNORECASE)

    sensors = {i: [] for i in range(1, 7)}
    current_sensor = None

    for raw in lines:
        if raw is None:
            continue
        line = str(raw).strip()
        if not line:
            continue

        # Deteksi marker sensor
        m_sensor = sensor_pattern.match(line)
        if m_sensor:
            current_sensor = int(m_sensor.group(1))
            continue

        # Parse pixel data
        m_pixel = pixel_pattern.search(line)
        if m_pixel and current_sensor is not None:
            try:
                pixel_idx = int(m_pixel.group(1))
                voltage = float(m_pixel.group(2))
            except Exception:
                continue

            # PENTING: Filter pixel range yang valid (280-1080)
            # Range ini harus konsisten dengan training!
            if 280 <= pixel_idx <= 1080:
                sensors.setdefault(current_sensor, []).append(voltage)

    return sensors


# ============================================================================
# DETEKSI VALLEY (NILAI MINIMUM SETELAH FILTERING)
# ============================================================================

def detect_valleys_from_sensors(sensors: dict):
    """
    Deteksi valley (nilai minimum) dari setiap sensor setelah filtering.

    Returns:
      valleys: list[float or None] - 6 nilai valley (None jika data tidak cukup)
      details: dict dengan info detail per sensor
    """
    valleys = []
    details = {}

    for sid in range(1, 7):
        data = sensors.get(sid, [])

        # Minimal 50 data point untuk filtering yang reliable
        if len(data) < 50:
            valleys.append(None)
            details[sid] = {
                "filtered": [],
                "valley_index": None,
                "valley_value": None,
                "pixel_count": len(data)
            }
            continue

        # Filter data
        filtered = butter_filtfilt(data, b_coef, a_coef)

        # Cari nilai minimum (valley)
        min_val = min(filtered)
        min_idx = filtered.index(min_val)

        valleys.append(min_val)
        details[sid] = {
            "filtered": filtered,
            "valley_index": min_idx,
            "valley_value": min_val,
            "pixel_count": len(data)
        }

    return valleys, details


# ============================================================================
# PEMILIHAN MODEL OTOMATIS
# ============================================================================

def choose_model_from_sensors(sensors: dict):
    """
    Logika pemilihan model berdasarkan karakteristik sensor 1 dan 6.

    KRITERIA:
    - Jika sensor 1 dan 6 KEDUA-DUANYA memiliki >2 pixel dengan voltage >2801 mV
      setelah filtering → gunakan model DANGKAL
    - Selain itu → gunakan model DALAM

    Returns:
      (model_dict, label_string)
    """
    def safe_filter(sig):
        if len(sig) < 3:
            return []
        return butter_filtfilt(sig, b_coef, a_coef)

    s1 = sensors.get(1, [])
    s6 = sensors.get(6, [])

    f1 = safe_filter(s1)
    f6 = safe_filter(s6)

    # Threshold untuk deteksi kondisi dangkal
    th_high = 2801  # mV

    # Hitung berapa pixel di atas threshold
    c1 = sum(1 for v in f1 if v > th_high)
    c6 = sum(1 for v in f6 if v > th_high)

    # Logika keputusan
    if c1 > 2 and c6 > 2:
        return model_dangkal, "DANGKAL"
    return model_dalam, "DALAM"


# ============================================================================
# MIN-MAX SCALER & LINEAR PREDICTION
# ============================================================================

def transform_minmax(values, mn, mx):
    """
    Normalisasi nilai ke range [0, 1]
    """
    out = []
    for v in values:
        if v is None:
            out.append(None)
        else:
            if mx == mn:
                out.append(0.0)
            else:
                out.append((v - mn) / (mx - mn))
    return out


def predict_linear(slope, intercept, x):
    """
    Prediksi linear: y = slope * x + intercept
    """
    if x is None:
        return None
    return slope * x + intercept


# ============================================================================
# PIPELINE UTAMA: MULTI-SENSOR PROCESSING
# ============================================================================

def process_file(raw_lines):
    """
    Pipeline lengkap untuk prediksi kedalaman tapak ban dari data CCD multi-sensor.

    Returns:
      JSON string dengan hasil prediksi
    """
    try:
        # 1. Parse data CCD
        sensors = parse_ccd_raw_lines(raw_lines)

        # Validasi: apakah ada data?
        total_pixels = sum(len(v) for v in sensors.values())
        if total_pixels == 0:
            return json.dumps({
                "success": False,
                "message": "No CCD data found in valid pixel range (280-1080)"
            })

        # 2. Deteksi valley dari setiap sensor
        valleys, details = detect_valleys_from_sensors(sensors)

        # 3. Pilih model kalibrasi yang sesuai
        model, label = choose_model_from_sensors(sensors)

        # 4. Normalisasi valley values
        scaled = transform_minmax(valleys, model["min"], model["max"])

        # 5. Prediksi kedalaman
        depths = [predict_linear(model["slope"], model["intercept"], s) for s in scaled]

        # 6. Susun data per sensor
        data = []
        for i in range(6):
            data.append({
                "sensor": i + 1,
                "valley": valleys[i],
                "scaled": scaled[i],
                "depth": depths[i],
                "pixel_count": details[i+1]["pixel_count"]
            })

        # 7. Ambil 4 sensor dengan kedalaman terkecil (area paling tipis)
        valid = [d for d in data if d["depth"] is not None]

        if len(valid) < 4:
            # Jika kurang dari 4 sensor valid, gunakan sebanyak yang ada
            smallest4 = sorted(valid, key=lambda x: x["depth"])[:max(1, len(valid))]
            min_depth = smallest4[0]["depth"] if smallest4 else None
            avg_depth = (sum(x["depth"] for x in smallest4) / len(smallest4)) if smallest4 else None
        else:
            # Ambil 4 terkecil
            smallest4 = sorted(valid, key=lambda x: x["depth"])[:4]
            min_depth = smallest4[0]["depth"]
            avg_depth = sum(x["depth"] for x in smallest4) / 4

        # 8. Interpretasi kondisi ban
        condition_status = "UNKNOWN"
        condition_detail = ""
        if min_depth is not None:
            if min_depth < 1.6:
                condition_status = "AUS"
                condition_detail = "⚠️ Kedalaman < 1.6mm (batas legal). Ban WAJIB diganti!"
            elif min_depth < 2.0:
                condition_status = "HAMPIR_AUS"
                condition_detail = "⚡ Kedalaman mendekati batas. Persiapkan penggantian!"
            elif min_depth < 3.0:
                condition_status = "NORMAL"
                condition_detail = "✅ Kedalaman memadai. Pantau berkala."
            else:
                condition_status = "BAIK"
                condition_detail = "✅ Kondisi sangat baik."

        # 9. Return hasil dalam format JSON
        return json.dumps({
            "success": True,
            "model_used": label,
            "total_pixels": total_pixels,
            "data": data,
            "smallest_4": smallest4,
            "min_depth": min_depth,
            "avg_depth": avg_depth,
            "condition_status": condition_status,
            "condition_detail": condition_detail
        }, indent=2)

    except Exception as e:
        return json.dumps({
            "success": False,
            "message": f"process_file exception: {str(e)}"
        })


# ============================================================================
# SINGLE-SENSOR PROCESSING (untuk 4 grooves)
# ============================================================================

def process_single_sensor(raw_lines):
    """
    Proses data single sensor dengan asumsi 4 groove (alur ban).
    Ini digunakan untuk mode pengukuran lain.
    """
    try:
        lines = to_python_list(raw_lines)
        if not lines:
            return json.dumps({
                "success": False,
                "message": "Empty data",
                "result": None
            })

        # Parse voltages
        pixel_pattern = re.compile(r"Pixel\[\s*(\d+)\s*\]:\s*([\d\.]+)\s*mV", re.IGNORECASE)
        voltages = []
        for raw in lines:
            if raw is None:
                continue
            line = str(raw).strip()
            m = pixel_pattern.search(line)
            if m:
                voltages.append(float(m.group(2)))

        if len(voltages) < 50:
            return json.dumps({
                "success": False,
                "message": "Not enough pixels in single sensor",
                "result": None
            })

        # Filter data
        filtered = butter_filtfilt(voltages, b_coef, a_coef)

        # Split menjadi 4 segment (untuk 4 groove)
        n = len(filtered)
        seg = n // 4
        groove_thicknesses = {}

        # Kalibrasi sederhana (linear)
        a = 0.00422
        b = 0.0

        for i in range(4):
            start = i * seg
            end = (i + 1) * seg if i < 3 else n
            seg_vals = filtered[start:end]
            if not seg_vals:
                groove_thicknesses[i + 1] = 0.0
                continue
            mean_v = sum(seg_vals) / len(seg_vals)
            thickness_mm = max(0.0, a * mean_v + b)
            groove_thicknesses[i + 1] = round(thickness_mm, 2)

        # Cek kondisi aus
        valid = [g for g in groove_thicknesses.values() if g > 0]
        is_worn = False
        if valid:
            is_worn = any(g < 1.6 for g in valid)
            min_groove = min(valid)
        else:
            is_worn = True
            min_groove = 0.0

        # Statistik voltage
        voltage_mean = sum(filtered) / len(filtered)
        voltage_std = (sum((x - voltage_mean) ** 2 for x in filtered) / len(filtered)) ** 0.5
        adc_mean = (voltage_mean / 3300.0) * 4095
        adc_std = (voltage_std / 3300.0) * 4095

        result_data = {
            "alur1": groove_thicknesses.get(1, 0.0),
            "alur2": groove_thicknesses.get(2, 0.0),
            "alur3": groove_thicknesses.get(3, 0.0),
            "alur4": groove_thicknesses.get(4, 0.0),
            "is_worn": is_worn,
            "adc_mean": round(adc_mean, 2),
            "adc_std": round(adc_std, 2),
            "voltage_mV": round(voltage_mean, 2),
            "pixel_count": len(voltages)
        }

        message_detail = f"Min groove: {min_groove:.2f} mm → {'AUS' if is_worn else 'AMAN'}"

        return json.dumps({
            "success": True,
            "message": message_detail,
            "result": result_data
        })

    except Exception as e:
        return json.dumps({
            "success": False,
            "message": f"process_single_sensor exception: {str(e)}",
            "result": None
        })


# ============================================================================
# DISPATCHER: ENTRYPOINT UTAMA
# ============================================================================

def predict_file(raw_input, model_dalam_in=None, model_dangkal_in=None, b_in=None, a_in=None):
    """
    Fungsi utama yang dipanggil dari APK.
    Otomatis mendeteksi tipe input dan memproses sesuai.

    Parameters:
      raw_input: bisa berupa:
        - Java ArrayList dari Chaquopy
        - Python list of strings
        - String path ke file
        - Raw string berisi data log

    Returns:
      JSON string dengan hasil prediksi
    """
    global model_dalam, model_dangkal, b_coef, a_coef

    # Override model/koefisien jika diperlukan
    if model_dalam_in is not None:
        model_dalam = model_dalam_in
    if model_dangkal_in is not None:
        model_dangkal = model_dangkal_in
    if b_in is not None:
        b_coef = b_in
    if a_in is not None:
        a_coef = a_in

    # Normalize input menjadi list of lines
    lines = None
    if isinstance(raw_input, str):
        try:
            # Coba buka sebagai file
            with open(raw_input, "r", encoding="utf-8") as f:
                content = f.read()
            lines = content.splitlines()
        except Exception:
            # Treat sebagai raw text
            lines = raw_input.splitlines()
    else:
        # Convert dari ArrayList atau iterable
        lines = to_python_list(raw_input)

    # Prioritas 1: Coba multi-sensor CCD
    try:
        res_multi = process_file(lines)
        res_obj = json.loads(res_multi)
        if res_obj.get("success"):
            return res_multi
    except Exception:
        pass

    # Prioritas 2: Coba single-sensor
    try:
        return process_single_sensor(lines)
    except Exception as e:
        return json.dumps({
            "success": False,
            "message": f"predict_file fallback exception: {str(e)}"
        })


# Wrapper untuk kompatibilitas
def process_single_sensor_file(file_path):
    """Wrapper untuk backward compatibility"""
    return predict_file(file_path)


# ============================================================================
# EXPORT MODEL PARAMETERS (untuk debugging/logging)
# ============================================================================

def get_model_info():
    """
    Return informasi model yang sedang digunakan
    """
    return json.dumps({
        "model_dalam": model_dalam,
        "model_dangkal": model_dangkal,
        "filter_coefficients": {
            "b": b_coef,
            "a": a_coef
        },
        "pixel_range": {
            "min": 280,
            "max": 1080
        }
    }, indent=2)