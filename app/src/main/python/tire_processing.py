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

    y[0] = b[0] * data[0]

    if n > 1:
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
    # Already a Python list
    if isinstance(obj, list):
        return obj

    # Try direct conversion
    try:
        return list(obj)
    except TypeError:
        pass

    # Try Java ArrayList .size() method
    try:
        size = obj.size()
        return [str(obj.get(i)) for i in range(size)]
    except Exception:
        pass

    # Try Java iterator
    try:
        iterator = obj.iterator()
        result = []
        while iterator.hasNext():
            result.append(str(iterator.next()))
        return result
    except Exception:
        pass

    # Fallback: empty list
    return []


# ======================================================
# 🔹 CCD MULTI-SENSOR PARSER (6 Sensors × 3700 Pixels)
# ======================================================

def process_ccd_raw_lines(raw_lines):
    """
    Process 6-sensor CCD data from Kotlin

    Expected format:
        --- SENSOR 1 ---
        Pixel[   0]: 3277.44 mV
        Pixel[   1]: 3280.12 mV
        ...
        --- SENSOR 2 ---
        ...

    Returns: JSON string with processing results
    """

    # ✅ Step 1: Convert to Python list safely
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
            "message": "No data received (empty list)",
            "result": None
        })

    # ✅ Step 2: Parse sensor data
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

        # Detect sensor header
        match_sensor = sensor_pattern.match(line)
        if match_sensor:
            current_sensor = int(match_sensor.group(1))
            continue

        # Detect pixel data
        match_pixel = pixel_pattern.match(line)
        if match_pixel and current_sensor is not None:
            try:
                voltage = float(match_pixel.group(2))
                sensors[current_sensor].append(voltage)
            except ValueError:
                continue

    # ✅ Step 3: Check if we got any data
    total_pixels = sum(len(sensors[i]) for i in sensors)
    if total_pixels == 0:
        return json.dumps({
            "success": False,
            "message": "No valid sensor data found in input",
            "result": None
        })

    # ✅ Step 4: Apply Butterworth filter
    b_coef = [0.0674553, 0.134911, 0.0674553]
    a_coef = [-1.14298, 0.412801]

    processed = {}

    for sensor_id in range(1, 7):
        data = sensors[sensor_id]
        pixel_count = len(data)

        if pixel_count == 0:
            processed[sensor_id] = {
                "success": False,
                "message": f"No data for sensor {sensor_id}",
                "pixel_count": 0
            }
            continue

        if pixel_count < 50:
            processed[sensor_id] = {
                "success": False,
                "message": f"Sensor {sensor_id}: insufficient data ({pixel_count} pixels)",
                "pixel_count": pixel_count
            }
            continue

        # Apply zero-phase filtering
        filtered = butter_filtfilt(data, b_coef, a_coef)

        # Calculate statistics
        raw_mean = sum(data) / len(data)
        filtered_mean = sum(filtered) / len(filtered)

        # Calculate std deviation
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

    # ✅ Step 5: Return results
    successful_sensors = sum(1 for s in processed.values() if s.get("success", False))

    return json.dumps({
        "success": True,
        "message": f"Processed {successful_sensors}/6 sensors successfully",
        "total_lines": len(lines),
        "total_pixels": total_pixels,
        "sensors": processed
    }, indent=2)


# ======================================================
# 🔹 SINGLE SENSOR PROCESSOR (untuk state machine CekBan)
# ======================================================

def process_single_sensor(raw_lines):
    """
    Process single sensor data (untuk satu posisi ban)
    Return JSON flat yang langsung dibaca Kotlin
    """

    try:
        lines = to_python_list(raw_lines)
    except Exception as e:
        return json.dumps({
            "success": False,
            "message": f"Conversion error: {str(e)}"
        })

    if not lines:
        return json.dumps({
            "success": False,
            "message": "Empty data"
        })

    # Parse voltage values
    pixel_pattern = re.compile(r"Pixel\[\s*(\d+)\]:\s*([\d\.]+)\s*mV", re.IGNORECASE)
    voltages = []

    for line in lines:
        try:
            line = str(line).strip()
        except:
            continue

        match = pixel_pattern.search(line)
        if match:
            try:
                voltages.append(float(match.group(2)))
            except ValueError:
                continue

    if len(voltages) < 50:
        return json.dumps({
            "success": False,
            "message": f"Insufficient data: {len(voltages)} pixels (need ≥ 50)"
        })

    # Apply Butterworth filter
    b_coef = [0.0674553, 0.134911, 0.0674553]
    a_coef = [-1.14298, 0.412801]
    filtered = butter_filtfilt(voltages, b_coef, a_coef)

    # Calculate mean & std
    voltage_mean = sum(filtered) / len(filtered)
    voltage_std = (sum((x - voltage_mean)**2 for x in filtered) / len(filtered)) ** 0.5

    # ADC conversion (12-bit, 3.3V)
    adc_mean = (voltage_mean / 3300.0) * 4095
    adc_std = (voltage_std / 3300.0) * 4095

    # ✅ Gunakan rumus kalibrasi real kamu di sini
    # Contoh: linear regression hasil eksperimen
    # thickness_mm = a * voltage_mean + b
    # Ganti a dan b sesuai hasil kalibrasi real kamu
    a = 0.00422
    b = 0.0
    thickness_mm = a * voltage_mean + b

    # Threshold keausan
    is_worn = thickness_mm < 1.6

    # ✅ Return flat JSON (tanpa "result")
    return json.dumps({
        "success": True,
        "message": f"Processed {len(voltages)} pixels",
        "adc_mean": round(adc_mean, 2),
        "adc_std": round(adc_std, 2),
        "voltage_mv": round(voltage_mean, 2),
        "thickness_mm": round(thickness_mm, 2),
        "is_worn": is_worn,
        "dataCount": len(voltages)
    })
