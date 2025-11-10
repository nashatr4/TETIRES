import json
import re

# ======================================================
# 🔹 BUTTERWORTH LOWPASS FILTER (Manual Implementation)
# ======================================================

def butter_lowpass_filter(data, b, a):
    """Forward pass of Butterworth filter"""
    n = len(data)
    y = [0.0] * n

    if n == 0:
        return y
    if n == 1:
        y[0] = b[0] * data[0]
        return y

    y[0] = b[0] * data[0]
    y[1] = b[0] * data[1] + b[1] * data[0] - a[0] * y[0]

    for i in range(2, n):
        y[i] = (b[0] * data[i] +
                b[1] * data[i-1] +
                b[2] * data[i-2] -
                a[0] * y[i-1] -
                a[1] * y[i-2])
    return y


def butter_filtfilt(data, b, a):
    """Zero-phase filtering (forward + backward pass)"""
    forward = butter_lowpass_filter(data, b, a)
    backward = butter_lowpass_filter(forward[::-1], b, a)
    return backward[::-1]


# ======================================================
# 🔹 SAFE ARRAYLIST CONVERTER
# ======================================================

def to_python_list(obj):
    """
    Safely convert Kotlin/Java ArrayList to Python list
    Handles multiple types of Java collections
    """
    if isinstance(obj, list):
        return obj
    try:
        return list(obj)
    except TypeError:
        pass
    try:
        size = obj.size()
        return [str(obj.get(i)) for i in range(size)]
    except Exception:
        pass
    try:
        iterator = obj.iterator()
        result = []
        while iterator.hasNext():
            result.append(str(iterator.next()))
        return result
    except Exception:
        pass
    return []


# =================================================================
# 🔹 SINGLE TIRE PROCESSOR (4-ALUR) - FIXED VERSION
# =================================================================

def process_single_sensor(raw_lines):
    """
    Process single TIRE (4 alur/grooves) data
    Return JSON dengan struktur:
    {
      "success": true,
      "message": "...",
      "result": {
        "alur1": 2.0,
        "alur2": 1.5,
        "alur3": 1.8,
        "alur4": 1.9,
        "is_worn": true,
        "adc_mean": ...,
        "adc_std": ...,
        "voltage_mV": ...,
        "pixel_count": ...
      }
    }

    ATURAN BISNIS:
    - Ban AUS jika ADA (any) alur < 1.6 mm
    - Semua 4 alur harus dikembalikan
    """

    try:
        lines = to_python_list(raw_lines)
    except Exception as e:
        return json.dumps({
            "success": False,
            "message": f"Conversion error: {str(e)}",
            "result": None
        })

    if not lines:
        return json.dumps({
            "success": False,
            "message": "Empty data",
            "result": None
        })

    # ✅ Step 1: Parse data untuk 4 alur
    sensor_pattern = re.compile(r"--- SENSOR (\d+) ---")
    pixel_pattern = re.compile(r"Pixel\[\s*(\d+)\]:\s*([\d\.]+)\s*mV", re.IGNORECASE)

    sensors = {i: [] for i in range(1, 5)}
    current_sensor = None

    for line in lines:
        try:
            line = str(line).strip()
        except:
            continue

        match_sensor = sensor_pattern.match(line)
        if match_sensor:
            sensor_id = int(match_sensor.group(1))
            if sensor_id in sensors:
                current_sensor = sensor_id
            else:
                current_sensor = None
            continue

        match_pixel = pixel_pattern.search(line)
        if match_pixel and current_sensor is not None:
            try:
                sensors[current_sensor].append(float(match_pixel.group(2)))
            except ValueError:
                continue

    # ✅ Step 2: Proses setiap alur dengan filter
    b_coef = [0.0674553, 0.134911, 0.0674553]
    a_coef = [-1.14298, 0.412801]

    groove_thicknesses = {}
    all_voltages_filtered = []
    all_voltages_raw = []

    for sensor_id in range(1, 5):
        data = sensors[sensor_id]
        all_voltages_raw.extend(data)

        if len(data) < 50:
            groove_thicknesses[sensor_id] = 0.0
            continue

        filtered = butter_filtfilt(data, b_coef, a_coef)
        all_voltages_filtered.extend(filtered)

        voltage_mean = sum(filtered) / len(filtered)

        # Rumus kalibrasi (sesuaikan dengan hasil kalibrasi Anda)
        a = 0.00422
        b = 0.0
        thickness_mm = a * voltage_mean + b
        thickness_mm = max(0.0, thickness_mm)

        groove_thicknesses[sensor_id] = thickness_mm

    # ✅ Step 3: Tentukan status 'is_worn'
    if not all_voltages_raw:
        return json.dumps({
            "success": False,
            "message": "No valid pixel data found",
            "result": None
        })

    valid_grooves = [g for g in groove_thicknesses.values() if g > 0]

    if not valid_grooves:
        is_worn = True  # Default ke 'aus' jika tidak ada data valid
    else:
        # ✅ ATURAN: Aus jika ADA alur < 1.6 mm
        is_worn = any(g < 1.6 for g in valid_grooves)

    # Hitung statistik gabungan
    if all_voltages_filtered:
        voltage_mean_total = sum(all_voltages_filtered) / len(all_voltages_filtered)
        voltage_std_total = (sum((x - voltage_mean_total)**2 for x in all_voltages_filtered) / len(all_voltages_filtered)) ** 0.5
    else:
        voltage_mean_total = 0.0
        voltage_std_total = 0.0

    adc_mean = (voltage_mean_total / 3300.0) * 4095
    adc_std = (voltage_std_total / 3300.0) * 4095

    # Buat pesan detail
    min_groove = min(valid_grooves) if valid_grooves else 0.0
    alur_aus = [i for i, g in groove_thicknesses.items() if g < 1.6 and g > 0]

    if alur_aus:
        message_detail = f"Min groove: {min_groove:.1f}mm → AUS (Alur {alur_aus})"
    else:
        message_detail = f"Min groove: {min_groove:.1f}mm → AMAN"

    # ✅ Step 4: Kembalikan JSON dengan struktur yang benar
    result_data = {
        "alur1": round(groove_thicknesses.get(1, 0.0), 2),
        "alur2": round(groove_thicknesses.get(2, 0.0), 2),
        "alur3": round(groove_thicknesses.get(3, 0.0), 2),
        "alur4": round(groove_thicknesses.get(4, 0.0), 2),
        "is_worn": is_worn,
        "adc_mean": round(adc_mean, 2),
        "adc_std": round(adc_std, 2),
        "voltage_mV": round(voltage_mean_total, 2),
        "pixel_count": len(all_voltages_raw)
    }

    return json.dumps({
        "success": True,
        "message": message_detail,
        "result": result_data
    })


# ======================================================
# 🔹 CCD MULTI-SENSOR PARSER (6 Sensors × 3700 Pixels)
# ======================================================

def process_ccd_raw_lines(raw_lines):
    """
    Process 6-sensor CCD data from Kotlin (untuk Terminal mode)
    """
    try:
        lines = to_python_list(raw_lines)
    except Exception as e:
        return json.dumps({
            "success": False,
            "message": f"Failed to convert input: {str(e)}",
            "result": None
        })

    if not lines:
        return json.dumps({
            "success": False,
            "message": "Empty input data",
            "result": None
        })

    sensor_pattern = re.compile(r"--- SENSOR (\d+) ---")
    pixel_pattern = re.compile(r"Pixel\[\s*(\d+)\]:\s*([\d\.]+)\s*mV", re.IGNORECASE)

    sensors = {i: [] for i in range(1, 7)}
    current_sensor = None

    for line in lines:
        try:
            line = str(line).strip()
        except:
            continue
        if not line:
            continue
        match_sensor = sensor_pattern.match(line)
        if match_sensor:
            current_sensor = int(match_sensor.group(1))
            continue
        match_pixel = pixel_pattern.match(line)
        if match_pixel and current_sensor is not None:
            try:
                voltage = float(match_pixel.group(2))
                sensors[current_sensor].append(voltage)
            except ValueError:
                continue

    b_coef = [0.0674553, 0.134911, 0.0674553]
    a_coef = [-1.14298, 0.412801]
    processed = {}

    for sensor_id in range(1, 7):
        data = sensors[sensor_id]
        pixel_count = len(data)
        if pixel_count < 50:
             processed[sensor_id] = {
                 "success": False,
                 "pixel_count": pixel_count
             }
             continue

        filtered = butter_filtfilt(data, b_coef, a_coef)
        raw_mean = sum(data) / len(data)
        filtered_mean = sum(filtered) / len(filtered)
        raw_std = (sum((x - raw_mean)**2 for x in data) / len(data)) ** 0.5
        filtered_std = (sum((x - filtered_mean)**2 for x in filtered) / len(filtered)) ** 0.5

        processed[sensor_id] = {
            "success": True,
            "pixel_count": pixel_count,
            "raw_mean_mV": round(raw_mean, 2),
            "filtered_mean_mV": round(filtered_mean, 2),
            "raw_std_mV": round(raw_std, 2),
            "filtered_std_mV": round(filtered_std, 2),
            "first_20_raw": [round(x, 2) for x in data[:20]],
            "first_20_filtered": [round(x, 2) for x in filtered[:20]]
        }

    successful_sensors = sum(1 for s in processed.values() if s.get("success", False))
    total_pixels = sum(len(sensors[i]) for i in sensors)

    return json.dumps({
        "success": True,
        "message": f"Processed {successful_sensors}/6 sensors successfully",
        "total_lines": len(lines),
        "total_pixels": total_pixels,
        "sensors": processed
    }, indent=2)